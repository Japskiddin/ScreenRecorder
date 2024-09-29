package io.github.japskiddin.android.core.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.DependencyHandlerScope

fun DependencyHandlerScope.implementation(dependency: Provider<MinimalExternalModuleDependency>) {
    add("implementation", dependency)
}

fun DependencyHandlerScope.detektPlugins(provider: Provider<MinimalExternalModuleDependency>) {
    add("detektPlugins", provider)
}

fun DependencyHandlerScope.ksp(provider: Provider<MinimalExternalModuleDependency>) {
    add("ksp", provider)
}

fun DependencyHandlerScope.androidTestImplementation(provider: Provider<MinimalExternalModuleDependency>) {
    add("androidTestImplementation", provider)
}

fun DependencyHandlerScope.testImplementation(provider: Provider<MinimalExternalModuleDependency>) {
    add("testImplementation", provider)
}

fun DependencyHandlerScope.implementation(project: Project) {
    add("implementation", project)
}

fun DependencyHandlerScope.api(project: Project) {
    add("api", project)
}
