package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ObserveAsEventsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun eventsWaitUntilStartedAndResumeAfterStop() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this)
            override val lifecycle: Lifecycle get() = registry
        }
        val events = Channel<Int>(Channel.UNLIMITED)
        val flow = events.receiveAsFlow()
        val received = mutableListOf<Int>()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.setContent { ObserveAsEvents(flow, owner) { received.add(it) } }
        compose.runOnIdle { events.trySend(1) }
        compose.runOnIdle { assertEquals(emptyList<Int>(), received); owner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitUntil { received.size == 1 }
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED; events.trySend(2) }
        compose.runOnIdle { assertEquals(listOf(1), received); owner.registry.currentState = Lifecycle.State.STARTED }
        compose.waitUntil { received.size == 2 }
        compose.runOnIdle { assertEquals(listOf(1, 2), received) }
    }
}
