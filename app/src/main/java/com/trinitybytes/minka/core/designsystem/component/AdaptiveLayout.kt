package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material window-size breakpoint where a layout gains a second pane (expanded and up). */
val ExpandedWidthBreakpoint: Dp = 840.dp

/** Widest a form column grows on any window (Figma "Ventanas ≥840dp", section B). */
val FormMaxWidth: Dp = 480.dp

/** True when the available width reaches the expanded breakpoint. */
val BoxWithConstraintsScope.isExpandedWidth: Boolean
    get() = maxWidth >= ExpandedWidthBreakpoint

/**
 * Exposes the available width to [content] so a screen can switch between its compact and
 * expanded layouts (two panes at [ExpandedWidthBreakpoint] and up).
 */
@Composable
fun MinkaAdaptiveBox(modifier: Modifier = Modifier, content: @Composable BoxWithConstraintsScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier, content = content)
}

/** Fills the width up to [FormMaxWidth]; center it with the parent's alignment. */
fun Modifier.formColumnWidth(): Modifier = this.widthIn(max = FormMaxWidth).fillMaxWidth()
