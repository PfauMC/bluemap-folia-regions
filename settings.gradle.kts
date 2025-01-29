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
            plugin("shadow", "com.gradleup.shadow").version("9.0.0-beta6")
            plugin("paper", "io.papermc.paperweight.userdev").version("2.0.0-beta.14")
            plugin("runpaper", "xyz.jpenilla.run-paper").version("2.3.0")
        }
    }
}
