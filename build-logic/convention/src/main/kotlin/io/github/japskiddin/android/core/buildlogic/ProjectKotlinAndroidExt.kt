package io.github.japskiddin.android.core.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project

internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = libs.findVersion("androidSdk-compile").get().toString().toInt()

        defaultConfig {
            minSdk = libs.findVersion("androidSdk-min").get().toString().toInt()
        }

        compileOptions {
            val javaVersion = JavaVersion.toVersion(libs.findVersion("jvm").get())
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
        }
    }
}
