package io.github.japskiddin.android.core.buildlogic

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven

internal fun LibraryExtension.configureSingleVariant() {
    // На Android Gradle Plugin 8.7.0 вариант для публикации создаётся автоматически
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
                // Добавляем компоненты в публикацию
                afterEvaluate {
                    from(components["release"])
                }

                groupId = project.properties["GROUP"].toString()
                artifactId = project.properties["ARTIFACT_ID"].toString()
                version = project.properties["VERSION_NAME"].toString()

                pom {
                    name = project.properties["POM_NAME"].toString()
                    description = project.properties["POM_DESCRIPTION"].toString()
                    url = project.properties["POM_URL"].toString()

                    scm {
                        connection = project.properties["POM_SCM_CONNECTION"].toString()
                        developerConnection = project.properties["POM_SCM_DEV_CONNECTION"].toString()
                        url = project.properties["POM_SCM_URL"].toString()
                    }

                    ciManagement {
                        system = project.properties["POM_CI_SYSTEM"].toString()
                        url = project.properties["POM_CI_URL"].toString()
                    }

                    issueManagement {
                        system = project.properties["POM_ISSUE_SYSTEM"].toString()
                        url = project.properties["POM_ISSUE_URL"].toString()
                    }

                    licenses {
                        license {
                            name = project.properties["POM_LICENSE_NAME"].toString()
                            url = project.properties["POM_LICENSE_URL"].toString()
                            distribution = project.properties["POM_LICENSE_DIST"].toString()
                        }
                    }

                    developers {
                        developer {
                            id = project.properties["POM_DEVELOPER_ID"].toString()
                            name = project.properties["POM_DEVELOPER_NAME"].toString()
                            email = project.properties["POM_DEVELOPER_EMAIL"].toString()
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
