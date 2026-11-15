package com.example.plume.shared

/** Common contract implemented by every platform target of the shared module. */
interface Platform {
    val name: String
}

/** Returns the platform specific implementation of [Platform]. */
expect fun currentPlatform(): Platform
