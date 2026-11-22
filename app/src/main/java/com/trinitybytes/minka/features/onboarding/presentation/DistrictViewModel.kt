package com.trinitybytes.minka.features.onboarding.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.onboarding.application.ChooseDistrict
import com.trinitybytes.minka.features.onboarding.application.GetDistricts
import com.trinitybytes.minka.features.onboarding.application.LocateDistrict
import com.trinitybytes.minka.features.onboarding.domain.District
import com.trinitybytes.minka.features.onboarding.domain.ProfileError
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

    /** 1.22: "Sin conexión. Lo escrito sigue aquí." with "Reintentar"; the step stays as it was. */
    Offline,
    Unexpected,
}

/** Why the list of a setup step could not load: the step shows a full-screen error with "Reintentar". */
enum class LoadError { Offline, Unexpected }

sealed interface DistrictEvent {
    /** 1.17: the screen shows the system dialog for the approximate location. */
    data object RequestLocationPermission : DistrictEvent

    /** The district is saved; the setup continues with the interests. */
    data object Chosen : DistrictEvent
}

/**
 * Every state of Figma 1.16–1.18: the district list filtered by [query], the choice (by hand or
 * preselected from the approximate location), the denied-permission snackbar, and no connection
 * (1.22): a snackbar that keeps the search and the choice, or a full-screen error while [loadError]
 * says the list itself could not load.
 */
data class DistrictUiState(
    val districts: List<District> = emptyList(),
    val query: String = "",
    val selectedId: String? = null,
    val isLoading: Boolean = true,
    val isLocating: Boolean = false,
    val isSaving: Boolean = false,
    val message: SetupMessage? = null,
    val loadError: LoadError? = null,
) {
    /** Loading, locating or saving: "Reintentar" waits for it. */
    val isBusy: Boolean get() = isLoading || isLocating || isSaving

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
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistrictUiState(query = savedStateHandle["district_query"] ?: "", selectedId = savedStateHandle["district_selected"]))
    val uiState: StateFlow<DistrictUiState> = _uiState.asStateFlow()

    private val _events = Channel<DistrictEvent>(Channel.BUFFERED)
    val events: Flow<DistrictEvent> = _events.receiveAsFlow()

    /** The request "Reintentar" (1.22) sends again; each one reads the state when it runs. */
    private var lastRequest: (() -> Unit)? = null

    init {
        loadDistricts()
    }

    private fun loadDistricts() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val result = getDistricts()
            _uiState.update {
                when (result) {
                    is Outcome.Success -> it.copy(isLoading = false, loadError = null, districts = result.value)
                    is Outcome.Failure -> it.copy(isLoading = false, loadError = result.error.toLoadError())
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { savedStateHandle["district_query"] = value; it.copy(query = value) }

    fun onDistrictSelected(id: String) = _uiState.update { if (it.isSaving) it else { savedStateHandle["district_selected"] = id; it.copy(selectedId = id) } }

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
            when (val result = locateDistrict()) {
                is Outcome.Success -> _uiState.update { state ->
                    val district = result.value
                    savedStateHandle["district_selected"] = district.id
                    savedStateHandle["district_query"] = ""
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
            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Outcome.Success -> _events.send(DistrictEvent.Chosen)
                // Offline (1.22) keeps the list, the search and the choice.
                is Outcome.Failure -> _uiState.update { it.copy(message = result.error.toMessage()) }
            }
        }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    /** "Reintentar": loads the list again if it failed, else repeats the last request with the step as it is now. */
    fun retry() {
        val state = _uiState.value
        when {
            state.isBusy -> Unit
            state.loadError != null -> loadDistricts()
            else -> lastRequest?.invoke()
        }
    }
}

/** Snackbar for a failed request of a setup step that keeps its content. */
internal fun ProfileError.toMessage(): SetupMessage = when (this) {
    ProfileError.Offline -> SetupMessage.Offline
    ProfileError.Unexpected -> SetupMessage.Unexpected
}

/** Full-screen error for a setup list that could not load (1.22 when offline). */
internal fun ProfileError.toLoadError(): LoadError = when (this) {
    ProfileError.Offline -> LoadError.Offline
    ProfileError.Unexpected -> LoadError.Unexpected
}
