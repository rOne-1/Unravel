package com.unravel.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class UnravelAndroidLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.library")
        }

        extensions.configure<LibraryExtension> {
            compileSdk = 37

            defaultConfig {
                minSdk = 26
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                val consumerRules = file("consumer-rules.pro")
                if (consumerRules.exists()) {
                    consumerProguardFiles(consumerRules)
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(21)
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit-jupiter-api").get())
            add("testImplementation", libs.findLibrary("junit-jupiter-params").get())
            add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
