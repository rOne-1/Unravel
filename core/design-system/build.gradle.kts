plugins {
    id("unravel.android.library")
    id("unravel.android.compose")
}

android {
    namespace = "com.unravel.core.designsystem"
}

dependencies {
    implementation(libs.chrisbanes.haze)
    implementation(libs.chrisbanes.haze.materials)
}
