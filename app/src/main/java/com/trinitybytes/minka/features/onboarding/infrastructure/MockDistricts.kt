package com.trinitybytes.minka.features.onboarding.infrastructure

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** A Lima district of the mock catalog, located by its centroid. */
internal data class DistrictRecord(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
)

/** The district catalog shared by the profile and items mock servers, so the two never disagree. */
internal object MockDistricts {
    /** Answer to "Usar mi ubicación" when the device has no approximate position (Figma 1.18 example). */
    const val DEFAULT_DISTRICT_ID = "miraflores"

    private const val EARTH_RADIUS_KM = 6371.0

    /** Lima districts; the first five are the ones Figma 1.16 lists, in its order. */
    val ALL = listOf(
        DistrictRecord("miraflores", "Miraflores", -12.1211, -77.0297),
        DistrictRecord("san-isidro", "San Isidro", -12.0977, -77.0365),
        DistrictRecord("barranco", "Barranco", -12.1494, -77.0219),
        DistrictRecord("santiago-de-surco", "Santiago de Surco", -12.1459, -76.9916),
        DistrictRecord("jesus-maria", "Jesús María", -12.0776, -77.0490),
        DistrictRecord("chorrillos", "Chorrillos", -12.1710, -77.0150),
        DistrictRecord("la-molina", "La Molina", -12.0790, -76.9420),
        DistrictRecord("lince", "Lince", -12.0838, -77.0360),
        DistrictRecord("magdalena-del-mar", "Magdalena del Mar", -12.0910, -77.0700),
        DistrictRecord("pueblo-libre", "Pueblo Libre", -12.0745, -77.0630),
        DistrictRecord("san-borja", "San Borja", -12.1086, -77.0010),
        DistrictRecord("surquillo", "Surquillo", -12.1120, -77.0190),
    )

    /**
     * Approximate distance in kilometers between two districts given by name (case-insensitive), or null when
     * either is unknown. Only centroids are used: no exact location is ever exposed.
     */
    fun distanceKm(fromName: String?, toName: String?): Double? {
        val from = byName(fromName) ?: return null
        val to = byName(toName) ?: return null
        val dLat = Math.toRadians(to.latitude - from.latitude)
        val dLon = Math.toRadians(to.longitude - from.longitude)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(from.latitude)) * cos(Math.toRadians(to.latitude)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_KM * 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    private fun byName(name: String?): DistrictRecord? = name?.let { wanted ->
        ALL.firstOrNull { it.name.equals(wanted, ignoreCase = true) }
    }
}
