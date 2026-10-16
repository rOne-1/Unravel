plugins {
    id("unravel.android.library")
    id("unravel.android.compose")
}

android {
    namespace = "com.unravel.core.canvasrenderer"
}

dependencies {
    implementation(project(":core:design-system"))
    implementation(project(":core:dsa-engine"))
}
