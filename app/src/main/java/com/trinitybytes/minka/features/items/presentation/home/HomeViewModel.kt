package com.trinitybytes.minka.features.items.presentation.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trinitybytes.minka.core.domain.Outcome
import com.trinitybytes.minka.features.items.application.GetFeed
import com.trinitybytes.minka.features.items.domain.Category
import com.trinitybytes.minka.features.items.domain.Feed
import com.trinitybytes.minka.features.items.domain.ItemsError
import com.trinitybytes.minka.features.onboarding.application.GetSetupSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val feed: Feed? = null,
    val offline: Boolean = false,
    val error: Boolean = false,
    val selectedCategory: Category? = null,
    val firstName: String = "",
    val district: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeed: GetFeed,
    private val savedState: SavedStateHandle,
    private val getSetupSummary: GetSetupSummary,
) : ViewModel() {
    private val mutableState = MutableStateFlow(HomeUiState(selectedCategory = savedCategory()))
    val state = mutableState.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            val summary = getSetupSummary()
            mutableState.update { it.copy(firstName = summary.firstName, district = summary.districtName) }
        }
    }

    private fun savedCategory(): Category? = savedState.get<String>(CATEGORY_KEY)?.let { name ->
        Category.entries.firstOrNull { it.name == name }
    }

    fun selectCategory(category: Category) {
        val selected = category.takeUnless { it == mutableState.value.selectedCategory }
        savedState[CATEGORY_KEY] = selected?.name
        mutableState.update { it.copy(selectedCategory = selected) }
    }

    fun refresh() {
        if (mutableState.value.refreshing) return
        viewModelScope.launch {
            mutableState.update { it.copy(refreshing = it.feed != null, error = false) }
            when (val result = getFeed()) {
                is Outcome.Success -> mutableState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        feed = result.value.value,
                        offline = result.value.fromCache,
                        error = false,
                    )
                }
                is Outcome.Failure -> mutableState.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        offline = result.error == ItemsError.Offline,
                        error = true,
                    )
                }
            }
        }
    }

    private companion object { const val CATEGORY_KEY = "home_category" }
}
