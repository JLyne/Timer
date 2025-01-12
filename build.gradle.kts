import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    java
    alias(libs.plugins.pluginYmlPaper)
}

group = "uk.co.notnull"
version = "3.1-SNAPSHOT"
description = "Timer"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
	mavenLocal()
	mavenCentral()
	maven {
		url = uri("https://repo.papermc.io/repository/maven-public/")
	}
}

dependencies {
	compileOnly(libs.paperApi)
}

paper {
    main = "com.leontg77.timer.Main"
    generateLibrariesJson = true
    apiVersion = libs.versions.paperApi.get().replace(Regex("\\-R\\d.\\d-SNAPSHOT"), "")
    authors = listOf("Jim (AnEnragedPigeon)", "LeonTG")
    description = "Allows creation and management of bossbar timers."

    permissions {
        register("timer.manage") {
            description = "Allows use of /timer command"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks {
    compileJava {
        options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing"))
        options.encoding = "UTF-8"
    }
}
