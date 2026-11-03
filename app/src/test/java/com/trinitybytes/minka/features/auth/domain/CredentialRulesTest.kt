package com.trinitybytes.minka.features.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialRulesTest {

    @Test
    fun `a contact is an email or a peruvian mobile number`() {
        assertEquals(ContactKind.Email, contactKindOf(" ana.quispe@gmail.com "))
        assertEquals(ContactKind.Phone, contactKindOf("987654321"))
        assertEquals(ContactKind.Phone, contactKindOf("+51 987 654 321"))
        assertNull(contactKindOf("ana.quispe@"))
        assertNull(contactKindOf("12345"))
        assertNull(contactKindOf("887654321"))
        assertNull(contactKindOf(""))
    }

    @Test
    fun `contacts are normalized so email case and phone formatting do not matter`() {
        assertEquals("ana.quispe@gmail.com", normalizeContact(" Ana.Quispe@Gmail.com"))
        assertEquals("987654321", normalizeContact("+51 987-654-321"))
    }

    @Test
    fun `password strength grows with length and variety`() {
        assertEquals(PasswordStrength.Weak, passwordStrength("abc"))
        assertEquals(PasswordStrength.Weak, passwordStrength("abcdefgh"))
        assertEquals(PasswordStrength.Medium, passwordStrength("trueque26"))
        assertEquals(PasswordStrength.Strong, passwordStrength("Trueque2026!"))
    }

    @Test
    fun `a password is acceptable from 8 characters and medium strength`() {
        assertEquals(false, isAcceptablePassword("abcdefgh"))
        assertEquals(false, isAcceptablePassword("Tr26!"))
        assertEquals(true, isAcceptablePassword("trueque26"))
    }
}
