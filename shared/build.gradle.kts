import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // AGP 9 requires this plugin instead of com.android.library for KMP modules.
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    // ── Android target (single-variant AAR produced by the Android-KMP plugin) ──
    android {
        namespace = "com.example.plume.shared"
        compileSdk = 35
        minSdk = 24

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    // ── iOS targets ──────────────────────────────────────────────────────────
    // The default hierarchy template (Kotlin 1.9.20+) automatically creates the
    // shared `iosMain` source set for these three targets.
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform — multiplatform artifacts for common code.
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            // Extended icon set (GraphicEq in SplashScreen, library/search icons…).
            implementation(compose.materialIconsExtended)

            // StateFlow / coroutines are exposed through MusicPlayerController,
            // so they are part of the public API of this module.
            api(libs.kotlinx.coroutines.core)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        androidMain.dependencies {
            // Only Android-specific implementations need these.
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.common)
            // MediaController / SessionToken for talking to PlumeMediaService.
            implementation(libs.androidx.media3.session)
        }
    }
}
