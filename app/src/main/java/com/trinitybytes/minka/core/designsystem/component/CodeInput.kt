package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Figma code input (1.14–1.15): one 60 dp box per digit, 8 dp apart. The next box to fill gets a
 * 2 dp primary border while focused; in error every box gets a 2 dp error border. One hidden text
 * field takes the input, so pasting a whole code works.
 */
@Composable
fun MinkaCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    length: Int,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val scheme = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = { onValueChange(it.filter(Char::isDigit).take(length)) },
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = label
                if (isError) error(label)
            },
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        interactionSource = interaction,
        decorationBox = {
            Row(horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8)) {
                repeat(length) { index ->
                    val isNext = focused && index == value.length.coerceAtMost(length - 1) && !isError
                    val border = when {
                        isError -> 2.dp to scheme.error
                        isNext -> 2.dp to scheme.primary
                        else -> 1.dp to scheme.outline
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .background(MinkaTheme.colors.raised, MinkaShapes.control)
                            .border(border.first, border.second, MinkaShapes.control),
                        contentAlignment = Alignment.Center,
                    ) {
                        value.getOrNull(index)?.let { digit ->
                            Text(
                                text = digit.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                color = scheme.onSurface.let { if (enabled) it else it.copy(alpha = DisabledContentAlpha) },
                            )
                        }
                    }
                }
            }
        },
    )
}

@PreviewLightDark
@Composable
private fun MinkaCodeInputPreview() {
    MinkaTheme {
        Surface {
            Column(
                modifier = Modifier.padding(MinkaSpacing.s16),
                verticalArrangement = Arrangement.spacedBy(MinkaSpacing.s16),
            ) {
                var code by remember { mutableStateOf("482") }
                MinkaCodeInput(value = code, onValueChange = { code = it }, length = 6, label = "Código")
                MinkaCodeInput(value = "482915", onValueChange = {}, length = 6, label = "Código", isError = true)
            }
        }
    }
}
