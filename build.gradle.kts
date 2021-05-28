import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import groovy.lang.MissingPropertyException
import org.gradle.internal.jvm.Jvm
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `maven-publish`
    kotlin("jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.github.patrick-mc"
version = "1.0-SNAPSHOT"

val kebabRegex = "-[a-z]".toRegex()
val relocations = setOf(
    "kotlin",
    "net.bytebuddy",
    "org.intellij.lang.annotations",
    "org.jetbrains.annotations"
)

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
    withType<JavaCompile> {
        sourceCompatibility = "1.8"
        targetCompatibility = "1.8"
    }

    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveClassifier.set("")

        val projectName = kebabRegex.replace(rootProject.name) { result ->
            result.value.drop(1)
        }

        from(files(Jvm.current().toolsJar))

        relocations.forEach { pattern ->
            relocate(pattern, "com.github.patrick.$projectName.shaded.$pattern")
        }

        manifest {
            attributes("Main-Class" to "com.github.patrick.attach.plugin.MainKt")
        }
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