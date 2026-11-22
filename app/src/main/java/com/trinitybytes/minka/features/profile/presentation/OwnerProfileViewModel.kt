package com.trinitybytes.minka.features.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.designsystem.component.initialsOf
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.GetOwnerProfile
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.items.domain.OwnerProfile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OwnerProfileUiState(
    val loading: Boolean = true,
    val profile: OwnerProfile? = null,
    val notFound: Boolean = false,
    val offline: Boolean = false,
    val error: Boolean = false,
) {
    /** Up to two letters for the avatar of the owner. */
    val initials: String get() = profile?.owner?.name?.let(::initialsOf).orEmpty()
}

/** The public profile of another user (Figma 11.02): name, district and their active listings. */
@HiltViewModel(assistedFactory = OwnerProfileViewModel.Factory::class)
class OwnerProfileViewModel @AssistedInject constructor(
    private val getOwnerProfile: GetOwnerProfile,
    @Assisted private val ownerId: String,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(ownerId: String): OwnerProfileViewModel
    }

    private val mutableState = MutableStateFlow(OwnerProfileUiState())
    val state = mutableState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            mutableState.update {
                it.copy(loading = it.profile == null, notFound = false, offline = false, error = false)
            }
            when (val result = getOwnerProfile(ownerId)) {
                is Outcome.Success -> mutableState.update { it.copy(loading = false, profile = result.value) }
                is Outcome.Failure -> mutableState.update {
                    it.copy(
                        loading = false,
                        notFound = result.error == ItemsError.NotFound,
                        offline = result.error == ItemsError.Offline,
                        error = result.error != ItemsError.NotFound && result.error != ItemsError.Offline,
                    )
                }
            }
        }
    }
}
