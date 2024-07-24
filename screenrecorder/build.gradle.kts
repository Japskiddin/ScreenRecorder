import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.FileInputStream
import java.util.Properties

plugins {
  alias(libs.plugins.app.android.library)
  id("maven-publish")
}

kotlin {
  explicitApi = ExplicitApiMode.Strict
}

android {
  namespace = "io.github.japskiddin.screenrecorder"
}

dependencies {
  implementation(libs.androidx.core)
}

val propertiesName = "github.properties"
val githubProperties = Properties().apply {
  if (rootProject.file(propertiesName).exists()) {
    load(FileInputStream(rootProject.file(propertiesName)))
  }
}
val outputsDirectoryPath = layout.buildDirectory.dir("outputs").get().toString()

val sourceFiles = android.sourceSets.getByName("main").java.getSourceFiles()

tasks.register<Javadoc>("withJavadoc") {
  isFailOnError = false
  dependsOn(tasks.named("compileDebugSources"), tasks.named("compileReleaseSources"))

  // add Android runtime classpath
  android.bootClasspath.forEach { classpath += project.fileTree(it) }

  // add classpath for all dependencies
  android.libraryVariants.forEach { variant ->
    variant.javaCompileProvider.get().classpath.files.forEach { file ->
      classpath += project.fileTree(file)
    }
  }

  source = sourceFiles
}

tasks.register<Jar>("withJavadocJar") {
  archiveClassifier.set("javadoc")
  dependsOn(tasks.named("withJavadoc"))
  val destination = tasks.named<Javadoc>("withJavadoc").get().destinationDir
  from(destination)
}

tasks.register<Jar>("withSourcesJar") {
  archiveClassifier.set("sources")
  from(sourceFiles)
}

publishing {
  publications {
    create<MavenPublication>("ScreenRecorder") {
      groupId = "io.github.japskiddin"
      artifactId = "screenrecorder"
      version = libs.versions.library.version.name.get()
      artifact("${outputsDirectoryPath}/aar/${artifactId}-release.aar")
    }
  }

  repositories {
    maven {
      name = "GithubPackages"
      url = uri("https://maven.pkg.github.com/japskiddin/ScreenRecorder")
      credentials {
        username = if (githubProperties.hasProperty("gpr.usr")) {
          githubProperties.getProperty("gpr.usr")
        } else {
          System.getenv("GPR_USER")
        }
        password = if (githubProperties.hasProperty("gpr.key")) {
          githubProperties.getProperty("gpr.key")
        } else {
          System.getenv("GPR_API_KEY")
        }
      }
    }
  }
}
