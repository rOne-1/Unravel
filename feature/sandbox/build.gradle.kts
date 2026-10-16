plugins {
    id("unravel.android.library")
    id("unravel.android.compose")
}

android {
    namespace = "com.unravel.feature.sandbox"
}

dependencies {
    implementation(project(":core:design-system"))
    implementation(project(":core:canvas-renderer"))
    implementation(project(":core:database"))
    implementation(project(":core:dsa-engine"))

    implementation(libs.snipme.highlights)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}
