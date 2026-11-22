package com.trinitybytes.minka.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class TopAppBarApiTest {
    @Test
    fun `top app bar exposes one composable with optional scroll behavior`() {
        val api = Class.forName("com.trinitybytes.minka.core.designsystem.component.TopAppBarKt")
        val methods = api.declaredMethods.filter { it.name == "MinkaTopAppBar" }
        assertEquals(1, methods.size)
        assertEquals(1, methods.single().parameterTypes.count { it.simpleName == "TopAppBarScrollBehavior" })
    }
}
