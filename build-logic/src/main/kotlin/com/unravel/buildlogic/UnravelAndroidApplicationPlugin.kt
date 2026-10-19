package com.unravel.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class UnravelAndroidApplicationPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("com.android.application")
        }

        extensions.configure<ApplicationExtension> {
            compileSdk = 37

            defaultConfig {
                applicationId = "com.unravel.app"
                minSdk = 26
                targetSdk = 36
                versionCode = 6
                versionName = "0.6.0-beta.0"
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }

            signingConfigs {
                create("beta") {
                    val betaKeystore = rootProject.file("signing/unravel-beta.keystore")
                    storeFile = betaKeystore
                    storePassword = (findProperty("unravel.beta.storePassword") as? String)
                        ?: System.getenv("UNRAVEL_BETA_STORE_PASSWORD")
                        ?: "unravel2026"
                    keyAlias = (findProperty("unravel.beta.keyAlias") as? String)
                        ?: System.getenv("UNRAVEL_BETA_KEY_ALIAS")
                        ?: "unravelBeta"
                    keyPassword = (findProperty("unravel.beta.keyPassword") as? String)
                        ?: System.getenv("UNRAVEL_BETA_KEY_PASSWORD")
                        ?: "unravel2026"
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                }
            }

            buildTypes {
                release {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                    signingConfig = signingConfigs.getByName("beta")
                }
                debug {
                    applicationIdSuffix = ".debug"
                    isDebuggable = true
                }
            }

            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }

            packaging {
                resources {
                    excludes += "/META-INF/{AL2.0,LGPL2.1}"
                }
            }
        }

        extensions.configure<KotlinAndroidProjectExtension> {
            jvmToolchain(21)
        }

        dependencies {
            add("testImplementation", libs.findLibrary("junit-jupiter-api").get())
            add("testImplementation", libs.findLibrary("junit-jupiter-params").get())
            add("testRuntimeOnly", libs.findLibrary("junit-jupiter-engine").get())
            add("testRuntimeOnly", libs.findLibrary("junit-platform-launcher").get())
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
