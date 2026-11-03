package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.domain.Coordinates
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Interest
import com.trinitybytes.minka.features.onboarding.domain.Profile
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.ProfileOutcome
import com.trinitybytes.minka.features.onboarding.domain.ProfileRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

class KtorProfileRepository @Inject constructor(private val client: HttpClient) : ProfileRepository {

    override suspend fun districts(): ProfileOutcome<List<District>> =
        call<List<DistrictDto>, List<District>>({ list -> list.map { it.toDomain() } }) { client.get("profile/districts") }

    override suspend fun nearestDistrict(location: Coordinates?): ProfileOutcome<District> =
        call<DistrictDto, District>(DistrictDto::toDomain) {
            client.post("profile/districts/nearest") {
                contentType(ContentType.Application.Json)
                setBody(NearestDistrictRequest(location?.latitude, location?.longitude))
            }
        }

    override suspend fun interests(): ProfileOutcome<List<Interest>> =
        call<List<InterestDto>, List<Interest>>({ list -> list.map { it.toDomain() } }) { client.get("profile/interests") }

    override suspend fun saveDistrict(userId: String, districtId: String): ProfileOutcome<Unit> =
        call<ProfileDto, Unit>({ }) {
            client.put("users/$userId/profile/district") {
                contentType(ContentType.Application.Json)
                setBody(DistrictChoiceRequest(districtId))
            }
        }

    override suspend fun saveInterests(userId: String, interestIds: List<String>): ProfileOutcome<Unit> =
        call<ProfileDto, Unit>({ }) {
            client.put("users/$userId/profile/interests") {
                contentType(ContentType.Application.Json)
                setBody(InterestsChoiceRequest(interestIds))
            }
        }

    override suspend fun profile(userId: String): ProfileOutcome<Profile> =
        call<ProfileDto, Profile>(ProfileDto::toDomain) { client.get("users/$userId/profile") }

    private suspend inline fun <reified D, T> call(
        toDomain: (D) -> T,
        request: () -> HttpResponse,
    ): ProfileOutcome<T> = try {
        val response = request()
        if (response.status.isSuccess()) {
            Outcome.Success(toDomain(response.body<D>()))
        } else {
            Outcome.Failure(ProfileError.Unexpected)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: IOException) {
        Outcome.Failure(ProfileError.Offline)
    } catch (_: Exception) {
        Outcome.Failure(ProfileError.Unexpected)
    }
}
