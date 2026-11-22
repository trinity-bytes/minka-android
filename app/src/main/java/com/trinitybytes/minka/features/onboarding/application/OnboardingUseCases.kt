package com.trinitybytes.minka.features.onboarding.application

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.core.domain.map
import com.trinitybytes.minka.core.domain.onSuccess
import com.trinitybytes.minka.features.auth.domain.SessionStore
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Interest
import com.trinitybytes.minka.features.onboarding.domain.LocationProvider
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.PermissionPolicy
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.ProfileOutcome
import com.trinitybytes.minka.features.onboarding.domain.ProfileRepository
import com.trinitybytes.minka.features.onboarding.domain.SetupStep
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject

/** Where the app opens once the session is restored (Figma 1.01). */
sealed interface LaunchTarget {
    data object Welcome : LaunchTarget
    data object Home : LaunchTarget

    /** A signed-in user who left the profile setup at [step]. */
    data class Setup(val step: SetupStep) : LaunchTarget
}

/**
 * Signed out → Welcome. Signed in with the setup pending for that same user → the pending step.
 * Anything else (setup completed, or never started on this device, as for accounts that signed in) → Home.
 * Device storage that cannot be read → Welcome, where signing in again rebuilds the session.
 */
class ResolveLaunch @Inject constructor(
    private val sessionStore: SessionStore,
    private val progressStore: OnboardingProgressStore,
) {
    suspend operator fun invoke(): LaunchTarget = try {
        resolve()
    } catch (_: IOException) {
        LaunchTarget.Welcome
    }

    private suspend fun resolve(): LaunchTarget {
        val session = sessionStore.session.first() ?: return LaunchTarget.Welcome
        val progress = progressStore.progress.first()
        return if (progress is OnboardingProgress.Pending && progress.userId == session.user.id) {
            LaunchTarget.Setup(progress.nextStep)
        } else {
            LaunchTarget.Home
        }
    }
}

/** A verified registration starts the profile setup at the district step (1.16). */
class BeginOnboarding @Inject constructor(private val progressStore: OnboardingProgressStore) {
    suspend operator fun invoke(userId: String) =
        progressStore.save(OnboardingProgress.Pending(userId, SetupStep.District))
}

class GetDistricts @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(): ProfileOutcome<List<District>> = repository.districts()
}

/** 1.17: maps the approximate location to a district. Only call it once the permission is granted. */
class LocateDistrict @Inject constructor(
    private val locationProvider: LocationProvider,
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): ProfileOutcome<District> =
        repository.nearestDistrict(locationProvider.approximateLocation())
}

/** 1.16: saves the district on the profile; the setup then continues with the interests. */
class ChooseDistrict @Inject constructor(
    private val sessionStore: SessionStore,
    private val repository: ProfileRepository,
    private val progressStore: OnboardingProgressStore,
) {
    suspend operator fun invoke(districtId: String): ProfileOutcome<Unit> = storingOnDevice {
        val userId = sessionStore.session.first()?.user?.id ?: return@storingOnDevice Outcome.Failure(ProfileError.Unexpected)
        repository.saveDistrict(userId, districtId).onSuccess {
            progressStore.save(OnboardingProgress.Pending(userId, SetupStep.Interests))
        }
    }
}

class GetInterests @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(): ProfileOutcome<List<Interest>> = repository.interests()
}

/** 1.19: saves the chosen interests; the setup then continues with notifications. */
class SaveInterests @Inject constructor(
    private val sessionStore: SessionStore,
    private val repository: ProfileRepository,
    private val progressStore: OnboardingProgressStore,
) {
    suspend operator fun invoke(interestIds: List<String>): ProfileOutcome<Unit> = storingOnDevice {
        val userId = sessionStore.session.first()?.user?.id ?: return@storingOnDevice Outcome.Failure(ProfileError.Unexpected)
        repository.saveInterests(userId, interestIds).onSuccess {
            progressStore.save(OnboardingProgress.Pending(userId, SetupStep.Notifications))
        }
    }
}

/** 1.19 "Se puede omitir": nothing is saved on the profile, the setup moves on. */
class SkipInterests @Inject constructor(private val advance: AdvanceSetup) {
    suspend operator fun invoke() = advance(SetupStep.Notifications)
}

/** 1.20: whatever the answer to the system dialog, the setup moves on to 1.21. */
class FinishNotificationsStep @Inject constructor(private val advance: AdvanceSetup) {
    suspend operator fun invoke() = advance(SetupStep.AllSet)
}

/** Whether 1.20 has to show the system dialog (Android 13+) or can skip it. */
class NotificationsNeedRequest @Inject constructor(private val policy: PermissionPolicy) {
    operator fun invoke(): Boolean = policy.notificationsNeedRequest
}

/** Saves [SetupStep] as the next one for the signed-in user. */
class AdvanceSetup @Inject constructor(
    private val sessionStore: SessionStore,
    private val progressStore: OnboardingProgressStore,
) {
    suspend operator fun invoke(next: SetupStep) {
        val userId = sessionStore.session.first()?.user?.id ?: return
        progressStore.save(OnboardingProgress.Pending(userId, next))
    }
}

/** What 1.21 greets with: "¡Todo listo, Ana!" and the chosen district, when the profile has it. */
data class SetupSummary(val firstName: String, val districtName: String?)

class GetSetupSummary @Inject constructor(
    private val sessionStore: SessionStore,
    private val repository: ProfileRepository,
) {
    suspend operator fun invoke(): SetupSummary {
        val user = sessionStore.session.first()?.user ?: return SetupSummary("", null)
        val district = when (val profile = repository.profile(user.id).map { it.district?.name }) {
            is Outcome.Success -> profile.value
            is Outcome.Failure -> null
        }
        return SetupSummary(user.name.trim().substringBefore(' '), district)
    }
}

/** 1.21 "lleva a Inicio": the setup is marked complete, so the next launch opens Inicio. */
class CompleteOnboarding @Inject constructor(
    private val sessionStore: SessionStore,
    private val progressStore: OnboardingProgressStore,
) {
    suspend operator fun invoke() {
        val userId = sessionStore.session.first()?.user?.id ?: return
        progressStore.save(OnboardingProgress.Completed(userId))
    }
}

/** A device storage failure while reading the session or saving the step is [ProfileError.Unexpected], not a crash. */
private inline fun <T> storingOnDevice(block: () -> ProfileOutcome<T>): ProfileOutcome<T> = try {
    block()
} catch (_: IOException) {
    Outcome.Failure(ProfileError.Unexpected)
}
