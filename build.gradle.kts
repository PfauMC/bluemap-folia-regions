plugins {
    java
    idea
    alias(libs.plugins.paper)
    alias(libs.plugins.runpaper)
}

group = project.properties["plugin.group"].toString()
version = project.properties["plugin.version"].toString()

repositories {
    mavenCentral()
    maven("https://maven.canvasmc.io/snapshots")
    maven("https://repo.bluecolored.de/releases")
    maven("https://folia-inquisitors.github.io/FoliaDevBundle")
}

dependencies {
    compileOnly("de.bluecolored:bluemap-api:2.7.3")
    compileOnly("io.canvasmc.canvas:canvas-api:1.21.11-R0.1-SNAPSHOT")
    paperweight.devBundle("io.canvasmc.canvas", "1.21.11-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf(
            "name" to project.properties["plugin.name"],
            "version" to project.version,
            "main" to project.properties["plugin.main"],
            "apiVersion" to project.properties["paper.api"],
        )
        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }
}
