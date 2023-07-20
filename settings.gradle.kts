pluginManagement {
    operator fun Settings.get(property: String): String {
        return org.gradle.api.internal.plugins.DslObject(this).asDynamicObject.getProperty(property) as String
    }

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            url = uri("https://libraries.minecraft.net")
        }

    }

    plugins {
        id("org.jetbrains.kotlin.jvm") version settings["kotlin_version"]
        id("fabric-loom") version settings["loom_version"]
    }
}
