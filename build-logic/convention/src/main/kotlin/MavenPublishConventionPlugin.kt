import com.android.build.api.dsl.LibraryExtension
import io.github.japskiddin.android.core.buildlogic.androidConfig
import io.github.japskiddin.android.core.buildlogic.configureMavenPublish
import io.github.japskiddin.android.core.buildlogic.configureSingleVariant
import io.github.japskiddin.android.core.buildlogic.plugins
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.configure

class MavenPublishConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            plugins {
                apply("maven-publish")
            }

            androidConfig {
                this as LibraryExtension
                configureSingleVariant()
            }

            extensions.configure<PublishingExtension> {
                configureMavenPublish(this)
            }
        }
    }
}
