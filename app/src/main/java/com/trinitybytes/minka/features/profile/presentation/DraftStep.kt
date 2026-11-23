package com.trinitybytes.minka.features.profile.presentation

import com.trinitybytes.minka.features.items.domain.ListingDraft
import com.trinitybytes.minka.features.items.domain.validateBasics
import com.trinitybytes.minka.features.items.domain.validateExchange

/** The publish step a draft stops at, for "Borrador · paso 1 de 3": the first one that still misses something. */
fun draftStep(draft: ListingDraft): Int = when {
    draft.validateBasics().isNotEmpty() -> 1
    draft.validateExchange().isNotEmpty() -> 2
    else -> 3
}
