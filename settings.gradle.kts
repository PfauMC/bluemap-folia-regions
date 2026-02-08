rootProject.name = "BlueMap-Folia-Regions"

pluginManagement {
    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugin("paper", "io.papermc.paperweight.userdev").version("2.0.0-beta.19")
            plugin("runpaper", "xyz.jpenilla.run-paper").version("3.0.2")
        }
    }
}
