import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.app.android.library)
    alias(libs.plugins.app.detekt)
    alias(libs.plugins.app.maven.publish)
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
}

android {
    namespace = "io.github.japskiddin.screenrecorder"
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
}

// Подробности как публиковать
// https://vanniktech.github.io/gradle-maven-publish-plugin/central
mavenPublishing {
    // Публикация в https://central.sonatype.com/
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
