import groovy.lang.MissingPropertyException
import org.gradle.jvm.tasks.Jar

plugins {
    `maven-publish`
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.github.patrick-mc"
version = "0.1.0"

repositories {
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")

    implementation("net.bytebuddy:byte-buddy:1.11.0")
    implementation("net.bytebuddy:byte-buddy-agent:1.11.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }

    jar {
        from("src/main/resources") {
            include("tools/attach/tools-min.jar")
        }
    }

    processResources {
        exclude("**/*.jar")
    }

    create<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets["main"].allSource)
    }

    create<Copy>("distJar") {
        if (System.getProperty("os.name").startsWith("Windows")) { // due to ci error
            from(shadowJar)

            val fileName = "${project.name.split("-").joinToString("") { it.capitalize() }}.jar"

            rename {
                fileName
            }

            var dest = file("W:/Servers/1.16.4/plugins")
            if (File(dest, fileName).exists()) dest = File(dest, "update")
            into(dest)
        }
    }
}

try {
    publishing {
        publications {
            create<MavenPublication>(rootProject.name) {
                from(components["java"])
                artifact(tasks["sourcesJar"])

                repositories {
                    mavenLocal()
                }
            }
        }
    }
} catch (ignored: MissingPropertyException) {}