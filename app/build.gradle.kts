import java.security.MessageDigest
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.chrisbanes.haze)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

abstract class PackageBetaDistributionTask : DefaultTask() {
    @get:InputFile
    abstract val sourceApk: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun packageBeta() {
        val src = sourceApk.get().asFile
        if (!src.exists()) {
            throw GradleException("Release APK not found at: ${src.absolutePath}")
        }

        val outDir = outputDir.get().asFile
        if (!outDir.exists()) {
            outDir.mkdirs()
        }

        val targetApk = File(outDir, "unravel-v0.6.0-beta.0.apk")
        src.copyTo(targetApk, overwrite = true)

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(targetApk.readBytes())
        val sha256Hex = hashBytes.joinToString("") { byte -> "%02x".format(byte) }

        val checksumFile = File(outDir, "unravel-v0.6.0-beta.0.apk.sha256")
        checksumFile.writeText("$sha256Hex  ${targetApk.name}\n")

        val sizeMb = targetApk.length().toDouble() / (1024 * 1024)
        logger.lifecycle("==========================================================================")
        logger.lifecycle(" [Unravel Beta Distribution Packaging]")
        logger.lifecycle(" Target Artifact: ${targetApk.absolutePath}")
        logger.lifecycle(" Artifact Size:   ${"%.2f".format(sizeMb)} MB (${targetApk.length()} bytes)")
        logger.lifecycle(" SHA-256 Digest:  $sha256Hex")
        logger.lifecycle(" Checksum File:   ${checksumFile.absolutePath}")
        logger.lifecycle("==========================================================================")
    }
}

tasks.register<PackageBetaDistributionTask>("packageBetaDistribution") {
    group = "distribution"
    description = "Packages the signed, minified release APK into release-artifacts/ with companion SHA-256 checksum."
    dependsOn("assembleRelease")
    sourceApk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    outputDir.set(layout.projectDirectory.dir("../release-artifacts"))
}

