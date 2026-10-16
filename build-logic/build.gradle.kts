plugins {
    `kotlin-dsl`
}

group = "com.unravel.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("unravelKotlinJvm") {
            id = "unravel.kotlin.jvm"
            implementationClass = "com.unravel.buildlogic.UnravelKotlinJvmPlugin"
        }
        register("unravelAndroidLibrary") {
            id = "unravel.android.library"
            implementationClass = "com.unravel.buildlogic.UnravelAndroidLibraryPlugin"
        }
        register("unravelAndroidCompose") {
            id = "unravel.android.compose"
            implementationClass = "com.unravel.buildlogic.UnravelAndroidComposePlugin"
        }
        register("unravelAndroidApplication") {
            id = "unravel.android.application"
            implementationClass = "com.unravel.buildlogic.UnravelAndroidApplicationPlugin"
        }
    }
}
