plugins {
    id("unravel.android.application")
    id("unravel.android.compose")
    alias(libs.plugins.kotlin.serialization)
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
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.chrisbanes.haze)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
