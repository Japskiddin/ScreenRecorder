package io.github.japskiddin.android.core.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

fun Project.configureJUnit(
    commonExtension: CommonExtension<*, *, *, *, *, *>
) {
    commonExtension.apply {
        defaultConfig {
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }

    dependencies {
        add("testImplementation", libs.findLibrary("junit").get())
    }
}
