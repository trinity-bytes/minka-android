package com.trinitybytes.minka.features.auth.presentation

/** Groups a bare phone number in threes for display, as Figma 1.11b does ("987 654 321"). */
internal fun groupPhoneDigits(digits: String): String = digits.chunked(3).joinToString(" ")
