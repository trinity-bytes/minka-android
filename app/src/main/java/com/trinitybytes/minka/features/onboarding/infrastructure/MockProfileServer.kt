package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.features.auth.infrastructure.DEMO_USER_ID

import com.trinitybytes.minka.core.network.MockCall
import com.trinitybytes.minka.core.network.MockDocument
import com.trinitybytes.minka.core.network.MockReply
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockSessions
import com.trinitybytes.minka.core.network.MockStorage
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * The profile half of the in-app mock API: the district and interest catalogs (Figma 1.16, 1.19)
 * and each user's choices. There is no geocoding service, so the nearest district is the closest
 * centroid, and an unknown location falls back to [DEFAULT_DISTRICT_ID] (README).
 */
class MockProfileServer(storage: MockStorage, private val sessions: MockSessions) {
    private val document = MockDocument(storage, STORAGE_KEY, ProfileDb.serializer()) { ProfileDb.seeded() }

    fun routes(): Set<MockRoute> = setOf(
        MockRoute(HttpMethod.Get, "profile/districts") { call ->
            authorized(call) { MockReply.json(HttpStatusCode.OK, DISTRICTS.map { it.toDto() }) }
        },
        MockRoute(HttpMethod.Post, "profile/districts/nearest") { call ->
            authorized(call) { MockReply.json(HttpStatusCode.OK, nearest(call.body()).toDto()) }
        },
        MockRoute(HttpMethod.Get, "profile/interests") { call ->
            authorized(call) { MockReply.json(HttpStatusCode.OK, INTERESTS) }
        },
        MockRoute(HttpMethod.Put, "me/profile/district") { call ->
            val userId = userId(call) ?: return@MockRoute unauthorized()
            val request = call.body<DistrictChoiceRequest>()
            if (DISTRICTS.none { it.id == request.districtId }) {
                return@MockRoute MockReply.error(HttpStatusCode.UnprocessableEntity, "unknown_district")
            }
            update(userId) { it.copy(districtId = request.districtId) }
        },
        MockRoute(HttpMethod.Put, "me/profile/interests") { call ->
            val userId = userId(call) ?: return@MockRoute unauthorized()
            val request = call.body<InterestsChoiceRequest>()
            if (!INTERESTS.map { it.id }.containsAll(request.interestIds)) {
                return@MockRoute MockReply.error(HttpStatusCode.UnprocessableEntity, "unknown_interest")
            }
            update(userId) { it.copy(interestIds = request.interestIds.distinct()) }
        },
        MockRoute(HttpMethod.Get, "me/profile") { call ->
            val userId = userId(call) ?: return@MockRoute unauthorized()
            document.read { db -> profileReply(db.profiles[userId] ?: ProfileRecord()) }
        },
    )

    /** The district and interests [userId] chose, for the sibling mock servers that personalize their answers. */
    suspend fun profileOf(userId: String): ProfileDto =
        document.read { db -> profileDto(db.profiles[userId] ?: ProfileRecord()) }

    private suspend fun userId(call: MockCall): String? = call.bearerToken()?.let { sessions.userFor(it) }
    private fun unauthorized() = MockReply.error(HttpStatusCode.Unauthorized, "unauthorized")
    private suspend fun authorized(call: MockCall, block: () -> MockReply): MockReply =
        if (userId(call) == null) unauthorized() else block()

    private fun profileDto(profile: ProfileRecord) =
        ProfileDto(DISTRICTS.firstOrNull { it.id == profile.districtId }?.toDto(), profile.interestIds)

    private fun profileReply(profile: ProfileRecord) = MockReply.json(HttpStatusCode.OK, profileDto(profile))

    private fun nearest(request: NearestDistrictRequest): DistrictRecord {
        val latitude = request.latitude
        val longitude = request.longitude
        if (latitude == null || longitude == null) return DISTRICTS.first { it.id == DEFAULT_DISTRICT_ID }
        return DISTRICTS.minBy {
            (it.latitude - latitude).let { d -> d * d } + (it.longitude - longitude).let { d -> d * d }
        }
    }

    /** Applies [change] to the profile of [userId], persists it and answers with the result. */
    private suspend fun update(userId: String, change: (ProfileRecord) -> ProfileRecord): MockReply =
        document.update { db ->
            val profile = change(db.profiles[userId] ?: ProfileRecord())
            db.profiles[userId] = profile
            profileReply(profile)
        }

    companion object {
        private const val STORAGE_KEY = "profiles"

        /** Answer to "Usar mi ubicación" when the device has no approximate position (Figma 1.18 example). */
        const val DEFAULT_DISTRICT_ID = MockDistricts.DEFAULT_DISTRICT_ID

        private val DISTRICTS = MockDistricts.ALL

        /** The categories of Figma 1.19, in its order. */
        private val INTERESTS = listOf(
            InterestDto("hogar", "Hogar"),
            InterestDto("ropa", "Ropa"),
            InterestDto("libros", "Libros"),
            InterestDto("tecnologia", "Tecnología"),
            InterestDto("deportes", "Deportes"),
            InterestDto("ninos", "Niños"),
            InterestDto("herramientas", "Herramientas"),
            InterestDto("plantas", "Plantas"),
            InterestDto("servicios", "Servicios"),
            InterestDto("arte", "Arte"),
            InterestDto("musica", "Música"),
            InterestDto("cocina", "Cocina"),
        )
    }
}

private fun DistrictRecord.toDto() = DistrictDto(id, name)

@Serializable
private data class ProfileRecord(val districtId: String? = null, val interestIds: List<String> = emptyList())

@Serializable
private data class ProfileDb(val profiles: MutableMap<String, ProfileRecord> = mutableMapOf()) {
    companion object {
        /** The demo account already finished setup in Miraflores, so Inicio can show distances (README). */
        fun seeded() = ProfileDb(
            mutableMapOf(
                DEMO_USER_ID to ProfileRecord(
                    districtId = MockDistricts.DEFAULT_DISTRICT_ID,
                    interestIds = listOf("hogar", "libros", "plantas"),
                ),
            ),
        )
    }
}
