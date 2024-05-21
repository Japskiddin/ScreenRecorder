import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

buildscript {
    dependencies {
        classpath(libs.bundletool)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.jetbrains.dokka) apply false
    alias(libs.plugins.gradle.nexus.publish)
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set(
                gradleLocalProperties(rootDir, providers).getProperty("sonatype.username")
                    ?: System.getenv("OSSRH_USERNAME")
            )
            password.set(
                gradleLocalProperties(rootDir, providers).getProperty("sonatype.password")
                    ?: System.getenv("OSSRH_PASSWORD")
            )
            stagingProfileId.set(
                gradleLocalProperties(rootDir, providers).getProperty("sonatype.stagingProfileId")
                    ?: System.getenv("OSSRH_STAGING_PROFILE_ID")
            )
        }
    }
}