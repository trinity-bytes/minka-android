package com.trinitybytes.minka.features.items.domain

/** Orders of the results (Figma 3.02); "Mejor calificado" waits for phase 4. */
enum class SearchSort {
    NEAREST,
    NEWEST,
}

/** What the search screen asks for (Figma 3.02–3.05). Blank text and null filters match everything. */
data class SearchQuery(
    val text: String = "",
    val category: Category? = null,
    /** Matches items in this condition or better. */
    val condition: Condition? = null,
    /** Largest distance, in kilometers, from the viewer's district. */
    val distanceKm: Double? = null,
    val sort: SearchSort = SearchSort.NEAREST,
)

/** The two shelves of Inicio (Figma 2.01, 2.02). */
data class Feed(
    /** Active listings of other people, nearest first. */
    val nearby: List<Listing>,
    /** Active listings in the categories the viewer chose as interests, newest first. */
    val byInterest: List<Listing>,
)

/** A [value] and whether it was read from the device cache because the network was unavailable (Figma 2.05). */
data class Loaded<out T>(
    val value: T,
    val fromCache: Boolean = false,
)
