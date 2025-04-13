package com.example.coupleswipe.model

import com.google.gson.annotations.SerializedName

data class Movie(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("vote_average") val rating: Double?,
    @SerializedName("overview") val description: String?,
)

data class MovieResponse(
    val results: List<Movie>
)