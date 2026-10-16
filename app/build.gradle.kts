plugins {
    id("unravel.android.application")
    id("unravel.android.compose")
}

android {
    namespace = "com.unravel.app"
}

dependencies {
    implementation(project(":feature:skill-map"))
    implementation(project(":feature:sandbox"))
    implementation(project(":feature:race-mode"))
    implementation(project(":core:design-system"))
    implementation(project(":core:canvas-renderer"))
    implementation(project(":core:database"))
    implementation(project(":core:dsa-engine"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
}
