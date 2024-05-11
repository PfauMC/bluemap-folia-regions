rootProject.name = "paper-plugin-template"

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
            plugin("shadow", "com.github.johnrengelman.shadow").version("8.1.1")
            plugin("paper", "io.papermc.paperweight.userdev").version("1.5.9")
            plugin("runpaper", "xyz.jpenilla.run-paper").version("2.2.2")
        }
    }
}
