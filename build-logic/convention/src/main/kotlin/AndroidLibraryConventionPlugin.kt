import io.github.japskiddin.android.core.buildlogic.configureJUnit
import io.github.japskiddin.android.core.buildlogic.configureKotlin
import io.github.japskiddin.android.core.buildlogic.configureKotlinAndroid
import io.github.japskiddin.android.core.buildlogic.libraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.run {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
            }

            requireNotNull(libraryExtension).apply {
                buildFeatures {
                    buildConfig = true
                }

                configureKotlin()
                configureKotlinAndroid(this)
                configureJUnit(this)
            }
        }
    }
}
