import java.util.UUID

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    `maven-publish`
}

gradlePlugin {
    plugins.create("classpathApiStubs") {
        id = "net.msrandom.classpath-api-stubs"

        displayName = "Classpath API Stubs"

        description = "A plugin that allows generating a JVM stub Jar based on the intersection of multiple classpaths"

        implementationClass = "net.msrandom.stubs.ClasspathApiStubsPlugin"
    }
}

java {
    withSourcesJar()
}

kotlin {
    jvmToolchain(8)
}

tasks.test {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
    })
    systemProperty("stubs.test.classpath", sourceSets.main.get().runtimeClasspath.asPath)
    environment("STUBS_TEST_JAVA_HOME", System.getProperty("java.home"))
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "org.ow2.asm", name = "asm-tree", version = "9.7.1")
    // implementation(group = "org.jetbrains.kotlin", name = "kotlin-metadata-jvm", version = "2.1.0")

    testImplementation(kotlin("test"))
    testImplementation(gradleTestKit())
}

tasks.test {
    // TestKit runs Gradle itself on the current JDK; production bytecode still targets Java 8.
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(JavaVersion.current().majorVersion))
    })
    systemProperty("stubs.test.classpath", sourceSets.main.get().runtimeClasspath.asPath)
}

// Test-only production publications; a new repository URL prevents same-GAV cache fallback.
val markerTestRepository =
    file(
        providers.gradleProperty("stubsMarkerTestDir").getOrElse(
            layout.buildDirectory.dir("marker-test-repositories").get().asFile.absolutePath,
        ),
    ).resolve(UUID.randomUUID().toString())

publishing.repositories.maven {
    name = "markerTest"
    url = uri(markerTestRepository)
}

tasks.test {
    // Publication depends on assemble, never check/test; this keeps the fixture graph acyclic.
    dependsOn(
        "publishPluginMavenPublicationToMarkerTestRepository",
        "publishClasspathApiStubsPluginMarkerMavenPublicationToMarkerTestRepository",
    )
    systemProperty("stubs.test.marker.repository", markerTestRepository.absolutePath)
    systemProperty("stubs.test.marker.version", project.version.toString())
    systemProperty("stubs.test.implementation.group", project.group.toString())
    systemProperty("stubs.test.gradle.user.home", gradle.gradleUserHomeDir.absolutePath)
}
