package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.application.ChooseDistrict
import com.trinitybytes.minka.features.onboarding.application.GetDistricts
import com.trinitybytes.minka.features.onboarding.application.LocateDistrict
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
import com.trinitybytes.minka.features.onboarding.domain.ProfileOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer
import javax.inject.Inject

/** One-off feedback of the setup steps, shown in a snackbar. */
enum class SetupMessage {
    /** 1.18: "Sin permiso de ubicación. Elige tu distrito." with the "Ajustes" action. */
    LocationDenied,
    Unexpected,
}

sealed interface DistrictEvent {
    /** 1.17: the screen shows the system dialog for the approximate location. */
    data object RequestLocationPermission : DistrictEvent

    /** The district is saved; the setup continues with the interests. */
    data object Chosen : DistrictEvent
}

/**
 * Every state of Figma 1.16–1.18: the district list filtered by [query], the choice (by hand or
 * preselected from the approximate location), the denied-permission snackbar and no connection while
 * [isOffline] (1.22), which keeps the search and the choice.
 */
data class DistrictUiState(
    val districts: List<District> = emptyList(),
    val query: String = "",
    val selectedId: String? = null,
    val isLoading: Boolean = true,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val message: SetupMessage? = null,
    val isOffline: Boolean = false,
) {
    val visibleDistricts: List<District>
        get() = query.searchKey().let { key -> if (key.isEmpty()) districts else districts.filter { key in it.name.searchKey() } }

    /** 1.16: "Sin selección no se avanza." */
    val canContinue: Boolean get() = selectedId != null && !isSaving && !isLocating
}

/** Lowercase without accents, so "jesus" finds "Jesús María". */
private fun String.searchKey(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

@HiltViewModel
class DistrictViewModel @Inject constructor(
    private val getDistricts: GetDistricts,
    private val locateDistrict: LocateDistrict,
    private val chooseDistrict: ChooseDistrict,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistrictUiState())
    val uiState: StateFlow<DistrictUiState> = _uiState.asStateFlow()

    private val _events = Channel<DistrictEvent>(Channel.BUFFERED)
    val events: Flow<DistrictEvent> = _events.receiveAsFlow()

    /** The last request, which "Reintentar" (1.22) sends again. */
    private var lastRequest: (() -> Unit)? = null

    init {
        loadDistricts()
    }

    private fun loadDistricts() {
        lastRequest = ::loadDistricts
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getDistricts()
            _uiState.update { it.copy(isLoading = false, isOffline = result.isOffline()) }
            when (result) {
                is Outcome.Success -> _uiState.update { it.copy(districts = result.value) }
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value) }

    fun onDistrictSelected(id: String) = _uiState.update { if (it.isSaving) it else it.copy(selectedId = id) }

    /** "Usar mi ubicación": the location is read only after the system dialog grants it. */
    fun onUseMyLocation() {
        val state = _uiState.value
        if (state.isLocating || state.isSaving) return
        viewModelScope.launch { _events.send(DistrictEvent.RequestLocationPermission) }
    }

    /** 1.18: a denial keeps whatever was chosen by hand and offers the app settings. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _uiState.update { it.copy(message = SetupMessage.LocationDenied) }
            return
        }
        locate()
    }

    private fun locate() {
        lastRequest = ::locate
        _uiState.update { it.copy(isLocating = true, message = null) }
        viewModelScope.launch {
            val result = locateDistrict()
            _uiState.update { it.copy(isOffline = result.isOffline()) }
            when (result) {
                is Outcome.Success -> _uiState.update { state ->
                    val district = result.value
                    state.copy(
                        isLocating = false,
                        selectedId = district.id,
                        query = "",
                        districts = if (state.districts.any { it.id == district.id }) state.districts else state.districts + district,
                    )
                }
                is Outcome.Failure -> _uiState.update { it.copy(isLocating = false, message = result.error.toMessage()) }
            }
        }
    }

    fun onContinue() {
        val state = _uiState.value
        val districtId = state.selectedId
        if (!state.canContinue || districtId == null) return
        lastRequest = ::onContinue
        _uiState.update { it.copy(isSaving = true, message = null) }
        viewModelScope.launch {
            val result = chooseDistrict(districtId)
            _uiState.update { it.copy(isSaving = false, isOffline = result.isOffline()) }
            when (result) {
                is Outcome.Success -> _events.send(DistrictEvent.Chosen)
                // Offline (1.22) keeps the list, the search and the choice.
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** 1.22 "Reintentar": sends the last request again. */
    fun retry() {
        val state = _uiState.value
        if (state.isOffline && !state.isSaving && !state.isLocating) lastRequest?.invoke()
    }
}

/** Offline shows 1.22 instead of a message. */
internal fun ProfileError.toMessage(): SetupMessage? = when (this) {
    ProfileError.Offline -> null
    ProfileError.Unexpected -> SetupMessage.Unexpected
}

/** Whether the request failed for lack of connection, which the setup steps show as Figma 1.22. */
internal fun ProfileOutcome<*>.isOffline(): Boolean = this is Outcome.Failure && error == ProfileError.Offline
