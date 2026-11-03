package com.trinitybytes.minka.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.trinitybytes.minka.core.designsystem.theme.MinkaSpacing
import com.trinitybytes.minka.core.designsystem.theme.MinkaTheme

/**
 * A shape drawn from SVG path data in a [viewportWidth] × [viewportHeight] box and stretched to
 * the bounds it is given, as Figma does with `preserveAspectRatio="none"`.
 */
class PathShape(pathData: String, private val viewportWidth: Float, private val viewportHeight: Float) : Shape {

    private val source: Path = PathParser().parsePathString(pathData).toPath()

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply { addPath(source) }
        path.transform(Matrix().apply { scale(size.width / viewportWidth, size.height / viewportHeight) })
        return Outline.Generic(path)
    }
}

/**
 * Expressive shapes of the Figma Shape component (`331:3028`). Material 3 1.4 has no public
 * `MaterialShapes`, so the outlines come from the Figma vectors.
 */
object MinkaExpressiveShapes {
    /** M3 Expressive 12-sided Cookie (splash art). */
    val Cookie12 = PathShape(
        "M96.9911 9.53871C104.734 3.86095 115.266 3.86095 123.009 9.53871L136.431 19.3805C139.809 21.8569 " +
            "143.823 23.3179 148.002 23.5918L164.61 24.6802C174.191 25.3081 182.259 32.0774 184.541 41.4042" +
            "L188.497 57.5712C189.492 61.6391 191.628 65.3384 194.653 68.2344L206.676 79.7438C213.612 86.3836 " +
            "215.441 96.7547 211.194 105.366L203.832 120.294C201.98 124.05 201.238 128.257 201.694 132.42" +
            "L203.507 148.965C204.552 158.509 199.286 167.63 190.498 171.497L175.263 178.2C171.43 179.887 " +
            "168.158 182.632 165.831 186.114L156.584 199.953C151.25 207.937 141.354 211.539 132.135 208.852" +
            "L116.156 204.194C112.136 203.023 107.864 203.023 103.844 204.194L87.8646 208.852C78.6463 211.539 " +
            "68.7503 207.937 63.4158 199.953L54.169 186.114C51.8424 182.632 48.5702 179.887 44.7369 178.2" +
            "L29.5024 171.497C20.7137 167.63 15.4481 158.509 16.4935 148.965L18.3056 132.42C18.7616 128.257 " +
            "18.0198 124.05 16.1675 120.294L8.80597 105.366C4.55909 96.7547 6.3878 86.3836 13.3239 79.7438" +
            "L25.347 68.2344C28.3722 65.3384 30.508 61.6391 31.5034 57.5712L35.4593 41.4042C37.7415 32.0774 " +
            "45.8088 25.3081 55.3902 24.6802L71.9985 23.5918C76.1774 23.3179 80.1914 21.8569 83.5687 19.3805" +
            "L96.9911 9.53871Z",
        viewportWidth = 220f,
        viewportHeight = 220f,
    )

    /** Cookie of the 1.11 confirmation art, the Figma vector drawn in a 200 × 200 box. */
    val Cookie12Compact = PathShape(
        "M86.9911 9.53871C94.7344 3.86095 105.266 3.86095 113.009 9.53871L123.558 17.274" +
            "C126.936 19.7503 130.95 21.2113 135.129 21.4852L148.182 22.3406" +
            "C157.763 22.9686 165.831 29.7379 168.113 39.0646L171.222 51.7712" +
            "C172.217 55.8391 174.353 59.5384 177.378 62.4344L186.828 71.4802" +
            "C193.764 78.12 195.593 88.4912 191.346 97.1029L185.56 108.835" +
            "C183.708 112.591 182.966 116.798 183.422 120.961L184.846 133.965" +
            "C185.892 143.509 180.626 152.63 171.837 156.497L159.864 161.765" +
            "C156.03 163.452 152.758 166.197 150.432 169.68L143.164 180.556" +
            "C137.83 188.54 127.933 192.142 118.715 189.455L106.156 185.794" +
            "C102.136 184.623 97.8642 184.623 93.8436 185.794L81.2848 189.455" +
            "C72.0666 192.142 62.1705 188.54 56.836 180.556L49.5685 169.68" +
            "C47.2418 166.197 43.9696 163.452 40.1363 161.765L28.1627 156.497" +
            "C19.3739 152.63 14.1083 143.509 15.1538 133.965L16.578 120.961" +
            "C17.0339 116.798 16.2922 112.591 14.4399 108.835L8.65404 97.1029" +
            "C4.40716 88.4912 6.23588 78.12 13.172 71.4802L22.6216 62.4344" +
            "C25.6468 59.5384 27.7826 55.8391 28.778 51.7712L31.8872 39.0646" +
            "C34.1693 29.7379 42.2367 22.9686 51.818 22.3406L64.8714 21.4852" +
            "C69.0504 21.2113 73.0644 19.7503 76.4417 17.2739L86.9911 9.53871Z",
        viewportWidth = 200f,
        viewportHeight = 200f,
    )

    /** Landing photo blob A. */
    val BlobA = PathShape(
        "M118.672 81.6672C113.513 97.6292 87.658 114.791 69.6012 118.692C51.4244 122.712 20.8898 117.252 " +
            "9.85173 105.25C-1.24631 93.2486 -2.20614 64.0849 3.19291 46.6227C8.59195 29.1605 25.9889 4.61742 " +
            "42.186 0.716927C58.4431 -3.18356 87.718 9.47803 100.436 23.0397C113.153 36.4814 123.832 65.7051 " +
            "118.672 81.6672Z",
        viewportWidth = 120f,
        viewportHeight = 120f,
    )

    /** Landing photo blob B. */
    val BlobB = PathShape(
        "M87.5981 112.093C72.9593 119.895 44.5216 123.136 30.5427 116.174C16.4438 109.212 7.44455 86.5858 " +
            "3.42487 70.4415C-0.594809 54.2371 -2.63465 30.7108 6.30464 19.0077C15.3039 7.24455 39.842 " +
            "0.642782 57.1206 0.0426217C74.5192 -0.497523 100.257 4.0637 110.396 15.5868C120.536 27.0498 " +
            "121.855 52.7967 118.016 68.881C114.236 84.9653 102.177 104.17 87.5981 112.093Z",
        viewportWidth = 120f,
        viewportHeight = 120f,
    )

    /** Landing photo blob C. */
    val BlobC = PathShape(
        "M113.039 98.8064C104.879 109.785 84.2391 112.185 69.059 115.185C53.999 118.064 33.6589 123.704 " +
            "22.3789 116.444C11.0988 109.245 4.07881 87.2877 1.4988 71.8094C-1.08121 56.3311 -0.961206 " +
            "34.9135 6.95882 23.5148C14.8788 12.0561 33.9589 6.47668 48.899 3.41702C63.719 0.23737 84.4791 " +
            "-2.88228 96.0591 4.73687C107.579 12.356 115.379 33.4137 118.199 49.072C121.079 64.7902 121.259 " +
            "87.8277 113.039 98.8064Z",
        viewportWidth = 120f,
        viewportHeight = 120f,
    )
}

@PreviewLightDark
@Composable
private fun MinkaExpressiveShapesPreview() {
    MinkaTheme {
        Surface {
            Row(
                modifier = Modifier.padding(MinkaSpacing.s16),
                horizontalArrangement = Arrangement.spacedBy(MinkaSpacing.s8),
            ) {
                listOf(
                    MinkaExpressiveShapes.Cookie12,
                    MinkaExpressiveShapes.Cookie12Compact,
                    MinkaExpressiveShapes.BlobA,
                    MinkaExpressiveShapes.BlobB,
                    MinkaExpressiveShapes.BlobC,
                ).forEach { Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.secondaryContainer, it)) }
            }
        }
    }
}
