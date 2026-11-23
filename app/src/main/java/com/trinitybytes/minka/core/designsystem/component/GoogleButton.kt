package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.R
import com.trinitybytes.minka.core.designsystem.theme.MinkaShapes
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * "Continuar con Google" (Figma Button `Style=Google`): the outlined Large button with the official
 * multicolor G, which is drawn untinted as Google's sign-in branding requires. Always 56 dp tall.
 */
@Composable
fun MinkaGoogleButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = GoogleButtonHeight),
        enabled = enabled,
        shape = MinkaShapes.full,
        border = BorderStroke(1.dp, if (enabled) scheme.outline else scheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = scheme.primary),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_google_g),
            contentDescription = null,
            modifier = Modifier
                .size(ButtonDefaults.IconSize)
                .alpha(if (enabled) 1f else MinkaDisabled.ContentAlpha),
        )
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}

private val GoogleButtonHeight = 56.dp


@Preview
@Composable
private fun MinkaGoogleButtonPreview() {
    MinkaTheme { MinkaGoogleButton(text = "Continuar con Google", onClick = {}, modifier = Modifier.fillMaxWidth()) }
}
