package com.example.coupleswipe.api

import com.example.coupleswipe.model.MovieResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TMDBApiService {
    @GET("discover/movie")
    suspend fun getMovies(
        @Query("with_genres") genreId: String? = null,
        @Query("primary_release_year") year: String? = null
    ): Response<MovieResponse>

    @GET("discover/movie")
    suspend fun getFilteredMovies(
        @Query("with_genres") genres: String? = null,
        @Query("primary_release_date.gte") minDate: String? = null,
        @Query("primary_release_date.lte") maxDate: String? = null,
        @Query("vote_average.gte") minRating: String? = null,
    ): Response<MovieResponse>
}