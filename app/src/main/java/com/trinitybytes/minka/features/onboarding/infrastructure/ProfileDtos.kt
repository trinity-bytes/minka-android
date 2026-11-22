package com.trinitybytes.minka.features.onboarding.infrastructure

import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Interest
import com.trinitybytes.minka.features.onboarding.domain.Profile
import kotlinx.serialization.Serializable

/** Wire format of the profile endpoints, shared by the Ktor adapter and the mock server. */

@Serializable
data class DistrictDto(val id: String, val name: String) {
    fun toDomain() = District(id, name)
}

@Serializable
data class InterestDto(val id: String, val name: String) {
    fun toDomain() = Interest(id, name)
}

@Serializable
data class NearestDistrictRequest(val latitude: Double? = null, val longitude: Double? = null)

@Serializable
data class DistrictChoiceRequest(val districtId: String)

@Serializable
data class InterestsChoiceRequest(val interestIds: List<String>)

@Serializable
data class ProfileDto(val district: DistrictDto? = null, val interestIds: List<String> = emptyList()) {
    fun toDomain() = Profile(district?.toDomain(), interestIds)
}
