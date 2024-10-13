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
            plugin("shadow", "com.gradleup.shadow").version("8.3.3")
            plugin("paper", "io.papermc.paperweight.userdev").version("1.7.3")
            plugin("runpaper", "xyz.jpenilla.run-paper").version("2.3.1")
        }
    }
}
