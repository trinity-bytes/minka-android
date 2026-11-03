package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * Indeterminate loading indicator of the Figma Loading indicator (`331:3029`), 48 dp. Material 3
 * 1.4 has no public Expressive `LoadingIndicator`, so this spins and breathes the Figma soft-burst
 * shape instead of morphing it.
 */
@Composable
fun MinkaLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val transition = rememberInfiniteTransition(label = "loading")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_400, easing = LinearEasing)),
        label = "rotation",
    )
    val scale by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 700), RepeatMode.Reverse),
        label = "scale",
    )
    Box(
        modifier = modifier
            .size(48.dp)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
            .graphicsLayer {
                rotationZ = rotation
                scaleX = scale
                scaleY = scale
            }
            .background(color, SoftBurst),
    )
}

private val SoftBurst = PathShape(
    "M20.7004 7.89801C22.5878 6.24031 25.4122 6.24031 27.2996 7.89801L29.3828 9.72771C30.068 10.3295 " +
        "30.9047 10.7325 31.8024 10.893L34.5318 11.3809C37.0046 11.8229 38.7656 14.0311 38.6463 16.5403" +
        "L38.5146 19.3098C38.4713 20.2207 38.678 21.1261 39.1122 21.9281L40.4325 24.3662C41.6287 26.5751 " +
        "41.0002 29.3287 38.9641 30.7999L36.7167 32.4237C35.9775 32.9578 35.3984 33.6839 35.0422 34.5234" +
        "L33.9592 37.0758C32.978 39.3882 30.4333 40.6136 28.0136 39.939L25.3428 39.1944C24.4644 38.9495 " +
        "23.5356 38.9495 22.6572 39.1944L19.9864 39.939C17.5667 40.6136 15.022 39.3882 14.0408 37.0758" +
        "L12.9578 34.5234C12.6016 33.6839 12.0225 32.9578 11.2833 32.4237L9.03594 30.7999C6.99982 29.3287 " +
        "6.37135 26.5751 7.5675 24.3662L8.88777 21.9281C9.32202 21.1261 9.52868 20.2207 9.48538 19.3098" +
        "L9.35372 16.5403C9.23445 14.0311 10.9954 11.8229 13.4682 11.3809L16.1976 10.893C17.0953 10.7325 " +
        "17.932 10.3295 18.6172 9.72771L20.7004 7.89801Z",
    viewportWidth = 48f,
    viewportHeight = 48f,
)

@PreviewLightDark
@Composable
private fun MinkaLoadingIndicatorPreview() {
    MinkaTheme {
        Surface { MinkaLoadingIndicator(Modifier.padding(MinkaSpacing.s16)) }
    }
}
