import org.jetbrains.kotlin.konan.properties.hasProperty
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.dokka)
    id("maven-publish")
    id("signing")
}

android {
    namespace = "io.github.japskiddin.screenrecorder"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        jniLibs {
            excludes += listOf(
                "**/kotlin/**",
                "META-INF/androidx.*",
                "META-INF/proguard/androidx-*"
            )
        }
        resources {
            excludes += listOf(
                "/META-INF/*.kotlin_module",
                "**/kotlin/**",
                "**/*.txt",
                "**/*.xml",
                "**/*.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.version",
                "META-INF/androidx.*",
                "META-INF/proguard/androidx-*"
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        val release by getting {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core)
    implementation(libs.androidx.core.ktx)
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

val dokkaOutputDir = layout.buildDirectory.file("dokka")
tasks.dokkaHtml { outputDirectory.set(file(dokkaOutputDir)) }
val deleteDokkaOutputDir by tasks.register<Delete>("deleteDokkaOutputDirectory") {
    delete(
        dokkaOutputDir
    )
}
val javadocJar = tasks.create<Jar>("javadocJar") {
    archiveClassifier.set("javadoc")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(deleteDokkaOutputDir, tasks.dokkaHtml)
    from(dokkaOutputDir)
}

group = "io.github.japskiddin"
version = libs.versions.libVersionName.get()

publishing {
    publications {
        create<MavenPublication>("ScreenRecorder") {
            groupId = "io.github.japskiddin"
            artifactId = "screenrecorder"
            version = libs.versions.libVersionName.get()
            artifact("${outputsDirectoryPath}/aar/${artifactId}-release.aar")
        }
        publications.withType<MavenPublication> {
            artifact(javadocJar)

            pom {
                name.set("ScreenRecorder")
                description.set("Library for recording screen inside application.")
                url.set("https://github.com/Japskiddin/ScreenRecorder")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/Japskiddin/ScreenRecorder/issues")
                }

                developers {
                    developer {
                        id.set("Japskiddin")
                        name.set("Nikita Lazarev")
                        email.set("japskiddin@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Japskiddin/ScreenRecorder.git")
                    developerConnection.set("scm:git:ssh://github.com/Japskiddin/ScreenRecorder.git")
                    url.set("https://github.com/Japskiddin/ScreenRecorder")
                }
            }
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

signing {
    if (project.hasProperty("signing.gnupg.keyName")) {
        println("Signing lib...")
        useGpgCmd()
        sign(publishing.publications)
    }
}