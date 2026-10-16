plugins {
    id("unravel.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.unravel.core.database"
}

dependencies {
    implementation(project(":core:dsa-engine"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore)
}
