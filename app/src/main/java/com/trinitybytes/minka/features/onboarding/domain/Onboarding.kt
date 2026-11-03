package com.trinitybytes.minka.features.onboarding.domain

import com.trinitybytes.minka.core.domain.Outcome
import kotlinx.coroutines.flow.Flow

/** A district the user trades in (Figma 1.16). Only the district is shared, never the address. */
data class District(val id: String, val name: String)

/** A category that personalizes Inicio (Figma 1.19). */
data class Interest(val id: String, val name: String)

/** An approximate position; coarse location is enough (Figma 1.17). */
data class Coordinates(val latitude: Double, val longitude: Double)

/** What the profile setup saved on the server. */
data class Profile(val district: District?, val interestIds: List<String>)

/** The steps of the profile setup after the account is verified (Figma 1.16–1.21), in order. */
enum class SetupStep {
    /** 1.16–1.18. */
    District,

    /** 1.19. */
    Interests,

    /** 1.20. */
    Notifications,

    /** 1.21. */
    AllSet,
}

/** How far [userId] got with the profile setup. Kept on the device next to the session (S4). */
sealed interface OnboardingProgress {
    val userId: String

    data class Pending(override val userId: String, val nextStep: SetupStep) : OnboardingProgress

    data class Completed(override val userId: String) : OnboardingProgress
}

sealed interface ProfileError {
    data object Offline : ProfileError
    data object Unexpected : ProfileError
}

typealias ProfileOutcome<T> = Outcome<T, ProfileError>

/** Port to the profile service: catalogs and the user's choices. */
interface ProfileRepository {
    suspend fun districts(): ProfileOutcome<List<District>>

    /** The district around [location]; the service picks a default when the location is unknown. */
    suspend fun nearestDistrict(location: Coordinates?): ProfileOutcome<District>
    suspend fun interests(): ProfileOutcome<List<Interest>>
    suspend fun saveDistrict(userId: String, districtId: String): ProfileOutcome<Unit>
    suspend fun saveInterests(userId: String, interestIds: List<String>): ProfileOutcome<Unit>
    suspend fun profile(userId: String): ProfileOutcome<Profile>
}

/** Port to the device storage that keeps the setup progress across launches. */
interface OnboardingProgressStore {
    /** The saved progress, or null when no setup was ever started on this device. */
    val progress: Flow<OnboardingProgress?>
    suspend fun save(progress: OnboardingProgress)
}

/** Port to the device's approximate location. Called only once the permission is granted. */
interface LocationProvider {
    /** The last approximate position, or null when the device has none. */
    suspend fun approximateLocation(): Coordinates?
}

/** Port to the platform rules about runtime permissions. */
interface PermissionPolicy {
    /** True when notifications need the system dialog (Android 13+, Figma 1.20). */
    val notificationsNeedRequest: Boolean
}
