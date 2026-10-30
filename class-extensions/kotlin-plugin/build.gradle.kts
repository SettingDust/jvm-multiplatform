plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    ksp(group = "dev.zacsweers.autoservice", name = "auto-service-ksp", version = "1.2.0")
    compileOnly(group = "com.google.auto.service", name = "auto-service-annotations", version = "1.1.1")

    implementation(kotlin("compiler-embeddable"))

    implementation(projects.classExtensionAnnotations)

    testImplementation(kotlin("test"))
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(8)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.test {
    useJUnit()
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    dependsOn(tasks.jar)
    systemProperty("extensions.plugin.jar", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    doFirst {
        systemProperty("extensions.compiler.classpath", (sourceSets.test.get().output + configurations.testRuntimeClasspath.get()).asPath)
    }
}
