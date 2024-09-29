buildscript {
    dependencies {
        classpath(libs.bundletool)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jreleaser) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.vanniktech.maven.publish) apply false
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
