package com.trinitybytes.minka.features.items.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil3.compose.AsyncImage

@Composable
fun ListingPhoto(uri: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        AsyncImage(
            model = rememberPhotoModel(uri),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }
}

private const val RESOURCE_PREFIX = "android.resource://"

/**
 * What Coil should load for a stored photo [uri]. Coil 3 only resolves `android.resource://<package>/<id>`,
 * but the mock API stores the stable `android.resource://<package>/drawable/<name>` (a resource id changes
 * with every build), so the name is turned into the current id here. Any other URI is returned as is.
 */
fun resolvePhotoModel(context: Context, uri: String?): Any? {
    if (uri == null || !uri.startsWith(RESOURCE_PREFIX)) return uri
    val parts = uri.removePrefix(RESOURCE_PREFIX).split('/')
    if (parts.size != 3) return uri
    val (pkg, type, name) = parts
    val id = context.resources.getIdentifier(name, type, pkg)
    return if (id == 0) uri else "$RESOURCE_PREFIX$pkg/$id"
}

@Composable
fun rememberPhotoModel(uri: String?): Any? {
    val context = LocalContext.current
    return remember(uri) { resolvePhotoModel(context, uri) }
}
