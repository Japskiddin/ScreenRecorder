import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  `kotlin-dsl`
}

group = "io.github.japskiddin.android.core.buildlogic"

java {
  val javaVersion = JavaVersion.toVersion(libs.versions.jvm.get())
  sourceCompatibility = javaVersion
  targetCompatibility = javaVersion
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.fromTarget(libs.versions.jvm.get())
  }
}

tasks {
  validatePlugins {
    enableStricterValidation = true
    failOnWarning = true
  }
}

dependencies {
  compileOnly(libs.android.gradle.plugin)
  compileOnly(libs.kotlin.gradle.plugin)
  compileOnly(libs.android.tools.common)
}

gradlePlugin {
  plugins {
    register("androidApplication") {
      id = "app.android.application"
      implementationClass = "AndroidApplicationConventionPlugin"
    }
    register("androidLibrary") {
      id = "app.android.library"
      implementationClass = "AndroidLibraryConventionPlugin"
    }
  }
}
