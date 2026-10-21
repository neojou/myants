// KMP : Desktop (JVM) + JS (browser), rendered by kool engine.

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

val appGroup = providers.gradleProperty("app.group").get()
val appRootName = providers.gradleProperty("app.rootName").get()
val appDisplayName = providers.gradleProperty("app.displayName").get()
val appVersion = providers.gradleProperty("app.version").get()
val isMacOs = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

group = appGroup
version = appVersion

kotlin {
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        binaries {
            executable {
                mainClass.set("$appGroup.MainKt")
                applicationDefaultJvmArgs = buildList {
                    add("--add-opens=java.base/java.lang=ALL-UNNAMED")
                    add("--enable-native-access=ALL-UNNAMED")
                    if (isMacOs) {
                        // GLFW must run on the macOS main thread.
                        add("-XstartOnFirstThread")
                        add("-Dapple.awt.application.name=$appDisplayName")
                        add("-Xdock:name=$appDisplayName")
                    }
                }
            }
        }
    }
    jvmToolchain(25)

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    js {
        outputModuleName.set(appRootName)
        browser {
            commonWebpackConfig {
                // Fixed bundle name so index.html need not change when ./configure.sh proj_name renames the project.
                outputFileName = "koolApp.js"
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
            }
        }
        binaries.executable()
        compilerOptions {
            target.set("es2015")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kool.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

// The MSDF atlas lives in kool-core-desktop.jar. The JS klib does not ship it, and the
// browser loader requests ./assets/fonts/fira-sans-regular.png. Without that file every
// glyph is a solid quad.
val koolJsFonts by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    isTransitive = false
}

dependencies {
    koolJsFonts("de.fabmax.kool:kool-core-desktop:${libs.versions.kool.get()}")
}

val extractKoolJsFonts = tasks.register<Copy>("extractKoolJsFonts") {
    val fontJar = koolJsFonts.elements.map { files -> files.single().asFile }
    from(fontJar.map { zipTree(it) }) {
        include("fonts/fira-sans-regular.png", "fonts/fira-sans-license.txt")
        eachFile { path = "assets/$path" }
    }
    into(layout.buildDirectory.dir("kool-js-fonts"))
    includeEmptyDirs = false
}

tasks.named<ProcessResources>("jsProcessResources") {
    from(extractKoolJsFonts)
    from(layout.projectDirectory.dir("assets/models")) {
        into("assets/models")
    }
    from(layout.projectDirectory.dir("assets/icons")) {
        into("assets/icons")
    }
    from(layout.projectDirectory.dir("src/commonMain/resources/fonts")) {
        include("noto-sans-cjk.json", "noto-sans-cjk.png")
        into("assets/fonts")
    }
}
