import org.gradle.internal.classpath.Instrumented.systemProperty
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.excludeCoroutines
import org.jetbrains.intellij.platform.gradle.extensions.excludeKotlinStdlib

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

val koogVersion = "1.1.1"

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2026.1.2")
        testFramework(TestFrameworkType.Platform)

        bundledPlugin("Git4Idea")
        bundledPlugin("org.intellij.plugins.markdown")
    }

    implementation("ai.koog:koog-agents:${koogVersion}") {
        excludeCoroutines()
        excludeKotlinStdlib()
    }
}

tasks {
    // Set up the additional debug/trace log categories if needed.
    runIde {
        systemProperty("idea.log.debug.categories", "com.github.yuonsa.commitgenius,ai.koog")
    }

    test {
        systemProperty("idea.log.debug.categories", "com.github.yuonsa.commitgenius,ai.koog")
    }
}

intellijPlatform {

    pluginConfiguration {
        ideaVersion {
            sinceBuild = "261"
            untilBuild = "261.*"
        }
    }
}
