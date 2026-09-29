plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `maven-publish`
}

kotlin {
    jvmToolchain(8)
}

repositories {
    mavenCentral()
}

dependencies {
    ksp(group = "dev.zacsweers.autoservice", name = "auto-service-ksp", version = "1.2.0")
    compileOnly(group = "com.google.auto.service", name = "auto-service-annotations", version = "1.1.1")

    implementation(kotlin("compiler-embeddable"))

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    dependsOn(tasks.jar)
    systemProperty("callable.plugin", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("callable.evidence", layout.buildDirectory.dir("callable-evidence").get().asFile.absolutePath)
    systemProperty("p4.replay", providers.gradleProperty("p4Replay").orElse("false").get())
    systemProperty("p4.root", layout.buildDirectory.dir("p4-final-acceptance").get().asFile.absolutePath)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}