package com.trinitybytes.minka.features.onboarding.infrastructure

import android.content.Context
import android.location.LocationManager
import android.os.Build
import com.trinitybytes.minka.features.onboarding.domain.Coordinates
import com.trinitybytes.minka.features.onboarding.domain.LocationProvider
import com.trinitybytes.minka.features.onboarding.domain.PermissionPolicy

/**
 * Reads the last approximate position the platform already has (network or passive provider), so
 * coarse location is enough and no Play Services are needed. Null when there is none or the
 * permission was not granted.
 */
class AndroidLocationProvider(private val context: Context) : LocationProvider {
    override suspend fun approximateLocation(): Coordinates? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        return try {
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
                .filter { it in manager.allProviders }
                .firstNotNullOfOrNull { manager.getLastKnownLocation(it) }
                ?.let { Coordinates(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }
}

/** POST_NOTIFICATIONS is a runtime permission only from Android 13 (API 33). */
class AndroidPermissionPolicy(private val sdkInt: Int = Build.VERSION.SDK_INT) : PermissionPolicy {
    override val notificationsNeedRequest: Boolean get() = sdkInt >= Build.VERSION_CODES.TIRAMISU
}
