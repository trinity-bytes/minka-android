package com.trinitybytes.minka.features.items.presentation.component

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import com.trinitybytes.minka.features.items.infrastructure.seedPhotoUris
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Every photo URI the mock API serves must decode with Coil, or the feed shows grey boxes. */
@RunWith(AndroidJUnit4::class)
class SeedPhotosTest {
    @Test fun everySeedPhotoLoads() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val loader = ImageLoader(context)
        val uris = seedPhotoUris()
        assertTrue(uris.size >= 6)
        uris.forEach { uri ->
            val result = loader.execute(ImageRequest.Builder(context).data(resolvePhotoModel(context, uri)).build())
            assertTrue("$uri failed: $result", result is SuccessResult)
        }
    }
}
