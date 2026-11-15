package com.example.plume.data.api.soundcloud

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface SoundCloudApiService {

    @GET("search/tracks")
    suspend fun searchTracks(
        @Query("q") query: String,
        @Query("client_id") clientId: String,
        @Query("limit") limit: Int = 30
    ): SoundCloudSearchResponse

    @GET("charts")
    suspend fun getTrendingTracks(
        @Query("kind") kind: String = "trending",
        @Query("genre") genre: String = "soundcloud:genres:all-music",
        @Query("client_id") clientId: String,
        @Query("limit") limit: Int = 30
    ): SoundCloudChartsResponse

    @GET
    suspend fun getTrackStream(
        @Url streamUrl: String,
        @Query("client_id") clientId: String
    ): SoundCloudStreamResponse
}

data class SoundCloudSearchResponse(
    val collection: List<SoundCloudTrackDto>? = null
)

data class SoundCloudChartsResponse(
    val collection: List<SoundCloudChartEntryDto>? = null
)

data class SoundCloudChartEntryDto(
    val track: SoundCloudTrackDto? = null
)

data class SoundCloudTrackDto(
    val id: Long = 0,
    val title: String? = null,
    val user: SoundCloudUserDto? = null,
    val artwork_url: String? = null,
    val duration: Long? = null,
    val streamable: Boolean? = null,
    val media: SoundCloudMediaDto? = null
)

data class SoundCloudUserDto(
    val username: String? = null,
    val avatar_url: String? = null
)

data class SoundCloudMediaDto(
    val transcodings: List<SoundCloudTranscodingDto>? = null
)

data class SoundCloudTranscodingDto(
    val url: String? = null,
    val format: SoundCloudFormatDto? = null
)

data class SoundCloudFormatDto(
    val protocol: String? = null,
    val mime_type: String? = null
)

data class SoundCloudStreamResponse(
    val url: String? = null
)
