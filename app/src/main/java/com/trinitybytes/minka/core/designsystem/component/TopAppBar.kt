package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/** Heights of the Figma Top app bar (`297:73`). */
enum class MinkaTopAppBarSize {
    /** 64 dp, Fraunces Title/Large: detail screens. */
    Small,

    /** 152 dp, Fraunces Headline/Medium: top-level screens. */
    Large,
}

/**
 * Top app bar on the page surface. With [onBack] it shows the back arrow; [actions] are 48 dp
 * icon buttons. The title styles come from the theme typography (titleLarge and headlineMedium).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinkaTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    size: MinkaTopAppBarSize = MinkaTopAppBarSize.Small,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val navigationIcon: @Composable () -> Unit = {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        }
    }
    // Figma draws every icon in the text color; Material defaults the actions to onSurfaceVariant.
    val onSurface = MaterialTheme.colorScheme.onSurface
    when (size) {
        MinkaTopAppBarSize.Small -> TopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(actionIconContentColor = onSurface),
        )
        MinkaTopAppBarSize.Large -> LargeTopAppBar(
            title = { Text(title) },
            modifier = modifier,
            navigationIcon = navigationIcon,
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(actionIconContentColor = onSurface),
        )
    }
}

@PreviewLightDark
@Composable
private fun MinkaTopAppBarPreview() {
    MinkaTheme {
        Column {
            MinkaTopAppBarSize.entries.forEach { size ->
                MinkaTopAppBar(
                    title = "Detalle",
                    size = size,
                    onBack = {},
                    actions = {
                        IconButton(onClick = {}) { Icon(Icons.Rounded.Favorite, contentDescription = null) }
                        IconButton(onClick = {}) { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
                    },
                )
            }
        }
    }
}
