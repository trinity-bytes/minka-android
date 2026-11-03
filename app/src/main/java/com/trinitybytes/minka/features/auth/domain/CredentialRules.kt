package com.trinitybytes.minka.features.auth.domain

enum class ContactKind { Email, Phone }

enum class PasswordStrength { Weak, Medium, Strong }

const val MIN_PASSWORD_LENGTH = 8

private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
private val PERU_MOBILE = Regex("^9\\d{8}$")

/** The kind of contact [raw] is, or null when it is neither an email nor a Peruvian mobile number. */
fun contactKindOf(raw: String): ContactKind? {
    val value = raw.trim()
    return when {
        EMAIL.matches(value) -> ContactKind.Email
        PERU_MOBILE.matches(normalizeContact(value)) -> ContactKind.Phone
        else -> null
    }
}

/** Canonical form of a contact: lowercase email, or the 9 digits of a phone without `+51`. */
fun normalizeContact(raw: String): String {
    val value = raw.trim()
    if ('@' in value) return value.lowercase()
    val digits = value.filter(Char::isDigit)
    return if (digits.length == 11 && digits.startsWith("51")) digits.drop(2) else digits
}

/** Live strength for the meter under the password field (Figma 1.12). */
fun passwordStrength(password: String): PasswordStrength {
    var score = 0
    if (password.length >= MIN_PASSWORD_LENGTH) score++
    if (password.length >= 12) score++
    if (password.any(Char::isLowerCase) && password.any(Char::isUpperCase)) score++
    if (password.any(Char::isDigit)) score++
    if (password.any { !it.isLetterOrDigit() }) score++
    return when {
        score >= 4 -> PasswordStrength.Strong
        score >= 2 -> PasswordStrength.Medium
        else -> PasswordStrength.Weak
    }
}

fun isAcceptablePassword(password: String): Boolean =
    password.length >= MIN_PASSWORD_LENGTH && passwordStrength(password) != PasswordStrength.Weak
