package io.github.japskiddin.android.core.buildlogic

import com.android.build.api.dsl.ApplicationExtension

internal fun ApplicationExtension.configureBuildTypes() {
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
