package com.trinitybytes.minka.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.designsystem.component.initialsOf
import com.trinitybytes.minka.features.auth.application.ObserveSession
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    /** Up to two letters for the avatar ("AQ" for Ana Quispe). */
    val initials: String = "",
    /** Null while unknown; the line is then left out. */
    val district: String? = null,
)

/** The signed-in user's own profile (Figma 11.01): who they are, from the session and the setup profile. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val observeSession: ObserveSession,
    private val getSetupSummary: GetSetupSummary,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val name = observeSession().first()?.user?.name.orEmpty()
            mutableState.update { it.copy(name = name, initials = initialsOf(name)) }
            val district = getSetupSummary().districtName
            mutableState.update { it.copy(district = district) }
        }
    }
}
