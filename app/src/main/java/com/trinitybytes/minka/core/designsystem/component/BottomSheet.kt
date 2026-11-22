package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaElevation
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme
import com.trinitybytes.minka.core.designsystem.theme.minkaShadow

private val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Figma Bottom sheet (`299:118`): modal, overlay surface with a top radius of 28 and the xl
 * shadow, a drag handle, a Fraunces Title/Large [title] and a [content] slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinkaBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier.minkaShadow(MinkaElevation.xl, SheetShape),
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = MinkaTheme.colors.overlay,
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() },
    ) {
        SheetBody(title, content)
    }
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = MinkaSpacing.s12)
            .size(width = 32.dp, height = 4.dp)
            .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun SheetBody(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MinkaSpacing.s24, end = MinkaSpacing.s24, top = MinkaSpacing.s16, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        content()
    }
}

/** The modal window cannot render in a preview, so this draws the sheet surface in place. */
@Preview
@Composable
private fun MinkaBottomSheetPreview() {
    MinkaTheme {
        Surface(shape = SheetShape, color = MinkaTheme.colors.overlay) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SheetHandle()
                SheetBody(title = "Filtrar resultados") {
                    MinkaListGroup {
                        MinkaListItem(headline = "Distancia", supporting = "Hasta 5 km", leadingIcon = Icons.Rounded.Tune)
                    }
                }
            }
        }
    }
}
