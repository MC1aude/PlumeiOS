// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false

    // ── Kotlin Multiplatform toolchain ──
    // Declared once at the root so future KMP modules (:shared, iosApp, desktop…)
    // can apply them without re-declaring versions. `apply false` keeps the
    // existing Android-only :app module completely unaffected.
    alias(libs.plugins.kotlin.multiplatform) apply false
    // Compose compiler plugin — version must always match the Kotlin version.
    alias(libs.plugins.kotlin.compose) apply false
    // Compose Multiplatform plugin (runtime, resources, desktop/iOS targets).
    alias(libs.plugins.compose.multiplatform) apply false

    alias(libs.plugins.ksp) apply false
}