import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jreleaser.model.Active

plugins {
  alias(libs.plugins.app.android.library)
  alias(libs.plugins.jreleaser)
  id("maven-publish")
}

kotlin {
  explicitApi = ExplicitApiMode.Strict
}

android {
  namespace = "io.github.japskiddin.screenrecorder"

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

dependencies {
  implementation(libs.androidx.core)
}

// Deploy

android {
  publishing {
    singleVariant("release") {
      withSourcesJar()
      withJavadocJar()
    }
  }
}

version = project.properties["VERSION_NAME"].toString()
description = project.properties["POM_DESCRIPTION"].toString()

publishing {
  publications {
    create<MavenPublication>("release") {
      groupId = project.properties["GROUP"].toString()
      artifactId = project.properties["POM_ARTIFACT_ID"].toString()

      pom {
        name.set(project.properties["POM_NAME"].toString())
        description.set(project.properties["POM_DESCRIPTION"].toString())
        url.set("https://github.com/Japskiddin/ScreenRecorder")
        issueManagement {
          url.set("https://github.com/Japskiddin/ScreenRecorder/issues")
        }

        scm {
          url.set("https://github.com/Japskiddin/ScreenRecorder")
          connection.set("scm:git://github.com/Japskiddin/ScreenRecorder.git")
          developerConnection.set("scm:git://github.com/Japskiddin/ScreenRecorder.git")
        }

        licenses {
          license {
            name.set("The Apache Software License, Version 2.0")
            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            distribution.set("repo")
          }
        }

        developers {
          developer {
            id.set("Japskiddin")
            name.set("Nikita Lazarev")
            email.set("japskiddin@gmail.com")
          }
        }

        afterEvaluate {
          from(components["release"])
        }
      }
    }
  }
  repositories {
    maven {
      setUrl(layout.buildDirectory.dir("staging-deploy"))
    }
  }
}

jreleaser {
  project {
    inceptionYear = "2024"
    author("@Japskiddin")
  }
  gitRootSearch = true
  signing {
    active = Active.ALWAYS
    armored = true
    verify = true
  }
  release {
    github {
      skipTag = true
      sign = true
      branch = "main"
      branchPush = "main"
      overwrite = true
    }
  }
  deploy {
    maven {
      mavenCentral.create("sonatype") {
        active = Active.ALWAYS
        url = "https://central.sonatype.com/api/v1/publisher"
        stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
        setAuthorization("Basic")
        applyMavenCentralRules = false
        sign = true
        checksums = true
        sourceJar = true
        javadocJar = true
        retryDelay = 60
      }
    }
  }
}
