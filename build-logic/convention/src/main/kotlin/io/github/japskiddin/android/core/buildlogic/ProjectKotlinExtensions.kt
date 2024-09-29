package io.github.japskiddin.android.core.buildlogic

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

internal fun Project.configureKotlinAndroid(
    androidExtension: AndroidExtension
) {
    androidExtension.apply {
        compileSdk = libs.versions.androidSdk.compile.get().toString().toInt()

        defaultConfig {
            minSdk = libs.versions.androidSdk.min.get().toString().toInt()
        }

        compileOptions {
            sourceCompatibility = projectJavaVersion
            targetCompatibility = projectJavaVersion
        }
    }
}

internal fun Project.configureKotlinJvm() {
    kotlinJvmCompilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
    }
}
