import io.github.japskiddin.android.core.buildlogic.detektPlugins
import io.github.japskiddin.android.core.buildlogic.libs
import io.github.japskiddin.android.core.buildlogic.plugins
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named

class AndroidDetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                apply(libs.plugins.detekt.get().pluginId)
            }

            tasks.named<Detekt>("detekt") {
                reports {
                    xml.required.set(true)
                    html.required.set(true)
                    txt.required.set(true)
                    sarif.required.set(true)
                    md.required.set(true)
                }
            }

            extensions.configure<DetektExtension> {
                config.setFrom(rootProject.files("default-detekt-config.yml"))
            }

            dependencies {
                detektPlugins(libs.detekt.formatting)
            }
        }
    }
}
