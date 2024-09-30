package io.github.japskiddin.android.core.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven

internal fun LibraryExtension.configureSingleVariant() {
    publishing {
        singleVariant("release") {
            withSourcesJar() // Обязательно надо для удобства использования

            // Javadoc отдельно публикуется только если нету исходников
            // withJavadocJar()
        }
    }
}

internal fun Project.configureMavenPublish(
    publishExt: PublishingExtension
) {
    with(publishExt) {
        publications {
            create("release", MavenPublication::class.java) {
                afterEvaluate {
                    // Добавляем компоненты в публикацию
                    from(components["release"])
                }

                groupId = project.properties["GROUP"].toString()
                artifactId = project.properties["POM_ARTIFACT_ID"].toString()
                version = project.properties["VERSION_NAME"].toString()

                pom {
                    name = project.properties["POM_NAME"].toString()
                    description = project.properties["POM_DESCRIPTION"].toString()
                    url = "https://github.com/Japskiddin/ScreenRecorder"

                    scm {
                        connection = "scm:git:git://github.com/Japskiddin/ScreenRecorder.git"
                        developerConnection = "scm:git:ssh://github.com/Japskiddin/ScreenRecorder.git"
                        url = "https://github.com/Japskiddin/ScreenRecorder"
                    }

                    ciManagement {
                        system = "GitHub Actions"
                        url = "https://github.com/Japskiddin/ScreenRecorder/actions"
                    }

                    issueManagement {
                        system = "GitHub"
                        url = "https://github.com/Japskiddin/ScreenRecorder/issues"
                    }

                    licenses {
                        license {
                            name = "The Apache Software License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                            distribution = "repo"
                        }
                    }

                    developers {
                        developer {
                            id = "Japskiddin"
                            name = "Nikita Lazarev"
                            email = "japskiddin@gmail.com"
                        }
                    }
                }
            }
        }

        // Список репозиториев куда публикуются артефакты
        repositories {
            // mavenCentral() // Публикация в Maven Central делается через REST API с помошью отдельного плагина
            mavenLocal() // Ищете файлы в директории ~/.m2/repository

            // Репозиторий в build папке корня проекта
            maven(url = uri(rootProject.layout.buildDirectory.file("maven-repo"))) {
                name = "BuildDir"
            }

            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/Japskiddin/ScreenRecorder")
                credentials {
                    username = project.properties["GITHUB_USERNAME"].toString()
                    password = project.properties["GITHUB_TOKEN"].toString()
                }
            }
        }
    }
}
