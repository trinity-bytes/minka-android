package com.trinitybytes.minka.features.items.presentation.detail

/** What the detail screen can ask for; the stateless content composables receive it instead of a ViewModel. */
data class DetailActions(
    val onBack: () -> Unit = {},
    val onRetry: () -> Unit = {},
    val onShare: () -> Unit = {},
    val onHide: () -> Unit = {},
    val onOpenOwner: (String) -> Unit = {},
    val onPause: () -> Unit = {},
    val onResume: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onConfirmDelete: () -> Unit = {},
    val onDismissDelete: () -> Unit = {},
    val onFindSimilar: () -> Unit = {},
)
