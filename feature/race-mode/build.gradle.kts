plugins {
    id("unravel.android.library")
    id("unravel.android.compose")
}

android {
    namespace = "com.unravel.feature.racemode"
}

dependencies {
    implementation(project(":core:design-system"))
    implementation(project(":core:canvas-renderer"))
    implementation(project(":core:dsa-engine"))

    implementation(libs.chrisbanes.haze)
    implementation(libs.chrisbanes.haze.materials)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
