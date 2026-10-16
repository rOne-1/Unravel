plugins {
    id("unravel.kotlin.jvm")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    testImplementation(libs.kotlinx.coroutines.test)
}
