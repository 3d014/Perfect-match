package com.example.coupleswipe.utils

import com.example.coupleswipe.fragments.FilterSelection

val genreMap = mapOf(
    "Action" to 28,
    "Comedy" to 35,
    "Drama" to 18,
    "Horror" to 27,
    "Sci-Fi" to 878
)

fun convertFiltersToTMDBParams(filters: List<FilterSelection>): Map<String, String> {
    val params = mutableMapOf<String, String>()

    filters.forEach { filter ->
        when (filter.filterName) {
            "Genre" -> {
                val genreIds = filter.selectedValues
                    .mapNotNull { genre -> genreMap[genre.toString()] }
                    .joinToString(",")
                if (genreIds.isNotEmpty()) params["with_genres"] = genreIds
            }
            "ReleaseYear" -> {
                if (filter.selectedValues.size == 2) {
                    params["primary_release_date.gte"] = "${filter.selectedValues[0]}-01-01"
                    params["primary_release_date.lte"] = "${filter.selectedValues[1]}-12-31"
                }
            }
            "MinimumRating" -> {
                params["vote_average.gte"] = filter.selectedValues.firstOrNull()?.toString() ?: "0"
            }
        }
    }

    return params
}