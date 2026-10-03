import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

buildscript {
    configurations.classpath {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.2.20")
        }
    }
}

plugins {
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"

    `kotlin-dsl`
    `java-gradle-plugin`

    `maven-publish`
    idea
}

gradlePlugin {
    plugins {
        val pluginName = "cloche"

        create(pluginName) {
            id = "$group.$pluginName"
            implementationClass = "earth.terrarium.cloche.ClochePlugin"
        }
    }
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://maven.msrandom.net/repository/cloche/")

    maven(url = "https://maven.fabricmc.net/")
    maven(url = "https://maven.neoforged.net/releases")



}

java {
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    implementation(group = "net.msrandom", name = "minecraft-codev-core", version = "0.7.2")
    implementation(group = "net.msrandom", name = "minecraft-codev-forge", version = "0.9.2")
    implementation(group = "net.msrandom", name = "minecraft-codev-fabric", version = "0.7.2")
    implementation(group = "net.msrandom", name = "minecraft-codev-mixins", version = "0.6.0")
    implementation(group = "net.msrandom", name = "minecraft-codev-runs", version = "0.7.2")
    implementation(group = "net.msrandom", name = "minecraft-codev-access-widener", version = "0.6.3")
    implementation(group = "net.msrandom", name = "minecraft-codev-remapper", version = "0.7.2")
    implementation(group = "net.msrandom", name = "minecraft-codev-decompiler", version = "0.6.0")
    implementation(group = "net.msrandom", name = "minecraft-codev-includes", version = "0.6.5")

    implementation(group = "net.msrandom", name = "class-extensions-gradle-plugin", version = "1.0.12")
    implementation(group = "net.msrandom", name = "jvm-virtual-source-sets", version = "1.3.6")
    implementation(group = "net.msrandom", name = "classpath-api-stubs", version = "0.1.12")

    implementation(group = "net.peanuuutz.tomlkt", name = "tomlkt", version = "0.5.0")
    implementation(group = "org.apache.groovy", name = "groovy-toml", version = "5.0.2")

    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.18.0")

    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20")

    ksp(group = "dev.zacsweers.autoservice", name = "auto-service-ksp", version = "1.2.0")
    compileOnly(group = "com.google.auto.service", name = "auto-service-annotations", version = "1.1.1")

    implementation(projects.clocheModels)

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.compileKotlin {
    compilerOptions {
        jvmDefault = JvmDefaultMode.ENABLE
        compilerOptions.freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks.jar {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "terrarium-earth",
        )
    }
}

tasks.test {
    useJUnitPlatform()

    dependsOn(tasks.pluginUnderTestMetadata)
}

kotlin {
    jvmToolchain(17)
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            if (requested.group == "org.jetbrains.kotlin") useVersion("2.2.20")
        }
    }
}


