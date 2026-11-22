package com.trinitybytes.minka.features.onboarding

import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.auth.domain.Session
import com.trinitybytes.minka.features.auth.domain.User
import com.trinitybytes.minka.features.auth.presentation.FakeSessionStore
import com.trinitybytes.minka.features.onboarding.domain.Coordinates
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.Interest
import com.trinitybytes.minka.features.onboarding.domain.LocationProvider
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgress
import com.trinitybytes.minka.features.onboarding.domain.OnboardingProgressStore
import com.trinitybytes.minka.features.onboarding.domain.PermissionPolicy
import com.trinitybytes.minka.features.onboarding.domain.Profile
import com.trinitybytes.minka.features.onboarding.domain.ProfileOutcome
import com.trinitybytes.minka.features.onboarding.domain.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow

val ANA = Session("token-ana", User("user-ana", "Ana Quispe", "ana.quispe@gmail.com"))

val MIRAFLORES = District("miraflores", "Miraflores")
val SAN_ISIDRO = District("san-isidro", "San Isidro")
val BARRANCO = District("barranco", "Barranco")
val TEST_DISTRICTS = listOf(MIRAFLORES, SAN_ISIDRO, BARRANCO)
val TEST_INTERESTS = listOf(Interest("hogar", "Hogar"), Interest("libros", "Libros"), Interest("plantas", "Plantas"))

fun signedInSessionStore(session: Session? = ANA) = FakeSessionStore().apply { this.session.value = session }

class FakeProgressStore(initial: OnboardingProgress? = null) : OnboardingProgressStore {
    override val progress = MutableStateFlow(initial)
    override suspend fun save(progress: OnboardingProgress) {
        this.progress.value = progress
    }
}

/** Profile port scripted per test; records what was saved. */
class FakeProfileRepository : ProfileRepository {
    val savedDistricts = mutableListOf<String>()
    val savedInterests = mutableListOf<List<String>>()
    val locatedFrom = mutableListOf<Coordinates?>()

    var districtsAnswer: suspend () -> ProfileOutcome<List<District>> = { Outcome.Success(TEST_DISTRICTS) }
    var nearestAnswer: suspend () -> ProfileOutcome<District> = { Outcome.Success(BARRANCO) }
    var interestsAnswer: suspend () -> ProfileOutcome<List<Interest>> = { Outcome.Success(TEST_INTERESTS) }
    var saveAnswer: suspend () -> ProfileOutcome<Unit> = { Outcome.Success(Unit) }
    var profileAnswer: suspend () -> ProfileOutcome<Profile> = { Outcome.Success(Profile(MIRAFLORES, emptyList())) }

    override suspend fun districts() = districtsAnswer()
    override suspend fun nearestDistrict(location: Coordinates?): ProfileOutcome<District> {
        locatedFrom += location
        return nearestAnswer()
    }
    override suspend fun interests() = interestsAnswer()
    override suspend fun saveDistrict(districtId: String): ProfileOutcome<Unit> =
        saveAnswer().also { if (it is Outcome.Success) savedDistricts += districtId }
    override suspend fun saveInterests(interestIds: List<String>): ProfileOutcome<Unit> =
        saveAnswer().also { if (it is Outcome.Success) savedInterests += listOf(interestIds) }
    override suspend fun profile() = profileAnswer()
}

class FakeLocationProvider(var location: Coordinates? = Coordinates(-12.149, -77.022)) : LocationProvider {
    var calls = 0
    override suspend fun approximateLocation(): Coordinates? {
        calls++
        return location
    }
}

class FakePermissionPolicy(override val notificationsNeedRequest: Boolean) : PermissionPolicy
