package com.trinitybytes.minka.core.database

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Stores a list of strings (photo URIs, wanted categories) in one column as a JSON array. */
class ListingConverters {
    @TypeConverter
    fun fromList(value: List<String>): String = Json.encodeToString(STRINGS, value)

    @TypeConverter
    fun toList(value: String): List<String> = Json.decodeFromString(STRINGS, value)

    private companion object {
        val STRINGS = ListSerializer(String.serializer())
    }
}
