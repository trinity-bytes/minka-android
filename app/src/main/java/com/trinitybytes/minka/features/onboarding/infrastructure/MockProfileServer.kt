package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.network.MockDocument
import com.trinitybytes.minka.core.network.MockReply
import com.trinitybytes.minka.core.network.MockRoute
import com.trinitybytes.minka.core.network.MockStorage
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import kotlinx.serialization.Serializable

/**
 * The profile half of the in-app mock API: the district and interest catalogs (Figma 1.16, 1.19)
 * and each user's choices. There is no geocoding service, so the nearest district is the closest
 * centroid, and an unknown location falls back to [DEFAULT_DISTRICT_ID] (README).
 */
class MockProfileServer(storage: MockStorage) {
    private val document = MockDocument(storage, STORAGE_KEY, ProfileDb.serializer()) { ProfileDb() }

    fun routes(): Set<MockRoute> = setOf(
        MockRoute(HttpMethod.Get, "profile/districts") {
            MockReply.json(HttpStatusCode.OK, DISTRICTS.map { it.toDto() })
        },
        MockRoute(HttpMethod.Post, "profile/districts/nearest") { call ->
            MockReply.json(HttpStatusCode.OK, nearest(call.body()).toDto())
        },
        MockRoute(HttpMethod.Get, "profile/interests") { MockReply.json(HttpStatusCode.OK, INTERESTS) },
        MockRoute(HttpMethod.Put, "users/{id}/profile/district") { call ->
            val request = call.body<DistrictChoiceRequest>()
            if (DISTRICTS.none { it.id == request.districtId }) {
                return@MockRoute MockReply.error(HttpStatusCode.UnprocessableEntity, "unknown_district")
            }
            update(call.pathParam("id")) { it.copy(districtId = request.districtId) }
        },
        MockRoute(HttpMethod.Put, "users/{id}/profile/interests") { call ->
            val request = call.body<InterestsChoiceRequest>()
            if (!INTERESTS.map { it.id }.containsAll(request.interestIds)) {
                return@MockRoute MockReply.error(HttpStatusCode.UnprocessableEntity, "unknown_interest")
            }
            update(call.pathParam("id")) { it.copy(interestIds = request.interestIds.distinct()) }
        },
        MockRoute(HttpMethod.Get, "users/{id}/profile") { call -> update(call.pathParam("id")) { it } },
    )

    private fun nearest(request: NearestDistrictRequest): DistrictRecord {
        val latitude = request.latitude
        val longitude = request.longitude
        if (latitude == null || longitude == null) return DISTRICTS.first { it.id == DEFAULT_DISTRICT_ID }
        return DISTRICTS.minBy { (it.latitude - latitude).let { d -> d * d } + (it.longitude - longitude).let { d -> d * d } }
    }

    /** Applies [change] to the profile of [userId], persists it and answers with the result. */
    private suspend fun update(userId: String, change: (ProfileRecord) -> ProfileRecord): MockReply = document.update { db ->
        val profile = change(db.profiles[userId] ?: ProfileRecord())
        db.profiles[userId] = profile
        MockReply.json(
            HttpStatusCode.OK,
            ProfileDto(DISTRICTS.firstOrNull { it.id == profile.districtId }?.toDto(), profile.interestIds),
        )
    }

    companion object {
        private const val STORAGE_KEY = "profiles"

        /** Answer to "Usar mi ubicación" when the device has no approximate position (Figma 1.18 example). */
        const val DEFAULT_DISTRICT_ID = "miraflores"

        /** Lima districts; the first five are the ones Figma 1.16 lists, in its order. */
        private val DISTRICTS = listOf(
            DistrictRecord("miraflores", "Miraflores", -12.1211, -77.0297),
            DistrictRecord("san-isidro", "San Isidro", -12.0977, -77.0365),
            DistrictRecord("barranco", "Barranco", -12.1494, -77.0219),
            DistrictRecord("santiago-de-surco", "Santiago de Surco", -12.1459, -76.9916),
            DistrictRecord("jesus-maria", "Jesús María", -12.0776, -77.0490),
            DistrictRecord("chorrillos", "Chorrillos", -12.1710, -77.0150),
            DistrictRecord("la-molina", "La Molina", -12.0790, -76.9420),
            DistrictRecord("lince", "Lince", -12.0838, -77.0360),
            DistrictRecord("magdalena-del-mar", "Magdalena del Mar", -12.0910, -77.0700),
            DistrictRecord("pueblo-libre", "Pueblo Libre", -12.0745, -77.0630),
            DistrictRecord("san-borja", "San Borja", -12.1086, -77.0010),
            DistrictRecord("surquillo", "Surquillo", -12.1120, -77.0190),
        )

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

private data class DistrictRecord(val id: String, val name: String, val latitude: Double, val longitude: Double) {
    fun toDto() = DistrictDto(id, name)
}

@Serializable
private data class ProfileRecord(val districtId: String? = null, val interestIds: List<String> = emptyList())

@Serializable
private data class ProfileDb(val profiles: MutableMap<String, ProfileRecord> = mutableMapOf())
