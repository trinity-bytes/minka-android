package com.trinitybytes.minka.features.items.presentation.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing

/**
 * The search bar of Buscar (Figma 3.01-3.05): a 56 dp pill that shows a search icon at rest and a back arrow
 * once the search is active ([onBack] non-null), a clear button while it holds text, and a 2 dp primary border
 * while focused. The microphone of the mockup is left out: voice search is not part of the build.
 */
@Composable
internal fun BuscarSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onBack: (() -> Unit)?,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val placeholder = stringResource(R.string.search_placeholder)
    val textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .semantics { contentDescription = placeholder },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        decorationBox = { innerTextField ->
            val border = if (focused) {
                Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), MinkaShapes.full)
            } else {
                Modifier
            }
            Surface(
                modifier = Modifier.fillMaxWidth().height(56.dp).then(border),
                shape = MinkaShapes.full,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = MinkaSpacing.s4),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s4),
                ) {
                    FieldIcon(onBack)
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(placeholder, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        innerTextField()
                    }
                    if (value.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.search_clear_text),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        Box(Modifier.size(MinkaSpacing.s16))
                    }
                }
            }
        },
    )
}

@Composable
private fun FieldIcon(onBack: (() -> Unit)?) {
    if (onBack != null) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
    } else {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface)
        }
    }
}
