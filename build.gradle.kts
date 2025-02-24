plugins {
    java
    id("com.gradleup.shadow") version ("9.0.0-beta6")
}

group = "me.xginko"
version = "2.0.0"
description = "Combat heavy villager lag by letting players optimize their trading halls."

repositories {
    mavenCentral()

    maven("https://ci.pluginwiki.us/plugin/repository/everything/") {
        name = "configmaster-repo"
    }

    maven("https://repo.codemc.io/repository/maven-releases/") {
        name = "codemc-repo"
    }

    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }

    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }

    maven("https://mvn-repo.arim.space/lesser-gpl3/") {
        name = "morepaperlib-repo"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("org.apache.logging.log4j:log4j-core:2.24.3")
    compileOnly("net.luckperms:api:5.4")

    implementation("com.github.thatsmusic99:ConfigurationMaster-API:v2.0.0-rc.3")
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")
    implementation("space.arim.morepaperlib:morepaperlib:0.4.3")
    implementation("com.github.cryptomorin:XSeries:13.0.0")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.bstats:bstats-bukkit:3.0.2")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }

    build.configure {
        dependsOn("shadowJar")
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(
                mapOf(
                    "name" to project.name,
                    "version" to project.version,
                    "description" to project.description!!.replace('"'.toString(), "\\\""),
                    "url" to "https://github.com/xGinko/VillagerOptimizer"
                )
            )
        }
    }

    shadowJar {
        archiveFileName.set("VillagerOptimizer-${version}.jar")

        relocate("io.github.thatsmusic99.configurationmaster", "me.xginko.newfriends.libs.configmaster")
        relocate("com.github.benmanes.caffeine", "me.xginko.newfriends.libs.caffeine")
        relocate("space.arim.morepaperlib", "me.xginko.newfriends.libs.morepaperlib")
        relocate("com.cryptomorin.xseries", "me.xginko.newfriends.libs.xseries")
        relocate("org.reflections", "me.xginko.newfriends.libs.reflections")
        relocate("org.bstats", "me.xginko.newfriends.libs.bstats")
    }
}