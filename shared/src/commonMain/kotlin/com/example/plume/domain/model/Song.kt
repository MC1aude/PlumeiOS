package com.example.plume.domain.model

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val streamEndpoint: String,
    val durationMs: Long = 0L
)
