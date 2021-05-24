import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.0"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.github.patrick-mc"
version = "1.0-SNAPSHOT"

val kebabRegex = "-[a-z]".toRegex()
val relocations = setOf(
    "net.bytebuddy"
)

repositories {
    maven("https://repo.maven.apache.org/maven2/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly("org.spigotmc:spigot-api:1.8-R0.1-SNAPSHOT")
    implementation("net.bytebuddy:byte-buddy:1.11.0")
    implementation("net.bytebuddy:byte-buddy-agent:1.11.0")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }

    withType<ShadowJar> {
        archiveClassifier.set("")

        val projectName = kebabRegex.replace(rootProject.name) { result ->
            result.value.drop(1)
        }

        relocations.forEach { pattern ->
            relocate(pattern, "com.github.patrick.$projectName.shaded.$pattern")
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