package com.unravel.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class UnravelAndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        with(pluginManager) {
            apply("org.jetbrains.kotlin.plugin.compose")
        }

        val androidExtension = extensions.getByType(CommonExtension::class.java)
        androidExtension.apply {
            buildFeatures {
                compose = true
            }
        }

        extensions.findByType(ComposeCompilerGradlePluginExtension::class.java)?.apply {
            val stabilityConfigFile = rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
            if (stabilityConfigFile.asFile.exists()) {
                stabilityConfigurationFiles.add(stabilityConfigFile)
            }
        }

        val bom = libs.findLibrary("androidx-compose-bom").get()
        dependencies {
            add("implementation", platform(bom))
            add("androidTestImplementation", platform(bom))
            add("implementation", libs.findLibrary("androidx-compose-ui").get())
            add("implementation", libs.findLibrary("androidx-compose-ui-graphics").get())
            add("implementation", libs.findLibrary("androidx-compose-foundation").get())
            add("implementation", libs.findLibrary("androidx-compose-material3").get())
            add("implementation", libs.findLibrary("androidx-compose-animation").get())
            add("testImplementation", libs.findLibrary("roborazzi").get())
            add("testImplementation", libs.findLibrary("roborazzi-compose").get())
        }
    }
}
