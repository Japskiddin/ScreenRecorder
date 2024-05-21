buildscript {
  dependencies {
    classpath(libs.bundletool)
  }
}

plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.jetbrains.kotlin.android) apply false
}

tasks.register("clean", Delete::class) {
  delete(layout.buildDirectory)
}