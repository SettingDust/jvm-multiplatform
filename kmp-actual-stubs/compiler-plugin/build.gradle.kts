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

val p4CallableRoot = layout.buildDirectory.dir("p4-callable")
val p4CallableTest = tasks.register<Test>("p4CallableTest") {
    useJUnitPlatform()
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    javaLauncher.set(tasks.test.flatMap { it.javaLauncher })
    include("**/CallableMatchingTest.class")
    dependsOn(tasks.jar)
    systemProperty("callable.plugin", tasks.jar.get().archiveFile.get().asFile.absolutePath)
    systemProperty("callable.evidence", p4CallableRoot.get().dir("cases").asFile.absolutePath)
    reports.junitXml.outputLocation.set(p4CallableRoot.map { it.dir("xml") })
    outputs.upToDateWhen { false }
    doFirst { p4CallableRoot.get().dir("cases").asFile.deleteRecursively() }
}
val p4ObserverJar = tasks.register<Jar>("p4ObserverJar") {
    dependsOn(tasks.testClasses)
    archiveClassifier.set("p4-observer")
    from(sourceSets.test.get().output.classesDirs) { include("net/msrandom/stub/tests/P4ProvenanceRegistrar*.class") }
    from("src/test/resources/p4-observer")
}

tasks.test {
    useJUnitPlatform()
    val annotationsJar = project(":kmp-actual-stub-annotations").tasks.named<Jar>("jar")
    dependsOn(tasks.jar, annotationsJar)
    systemProperty("smoke.annotations", annotationsJar.get().archiveFile.get().asFile.absolutePath)
    doFirst {
        systemProperty("smoke.classpath", classpath.filter { it.exists() }.asPath)
    }
    systemProperty(
        "callable.plugin",
        tasks.jar
            .get()
            .archiveFile
            .get()
            .asFile.absolutePath,
    )
    systemProperty(
        "callable.evidence",
        layout.buildDirectory
            .dir("callable-evidence")
            .get()
            .asFile.absolutePath,
    )
    val p4Replay = providers.gradleProperty("p4Replay").map(String::toBoolean).orElse(false)
    systemProperty("p4.replay", p4Replay.get())
    systemProperty(
        "p4.root",
        layout.buildDirectory
            .dir("p4-final-acceptance")
            .get()
            .asFile.absolutePath,
    )
    if (!p4Replay.get()) {
        exclude("**/P4AcceptanceTest.class")
    } else {
        val vssJar = project(":jvm-virtual-source-sets").tasks.named<Jar>("jar")
        val classAnnotationsJar = project(":class-extension-annotations").tasks.named<Jar>("jar")
        dependsOn(p4CallableTest, p4ObserverJar, vssJar, classAnnotationsJar)
        systemProperty("p4.classAnnotations", classAnnotationsJar.get().archiveFile.get().asFile.absolutePath)
        outputs.upToDateWhen { false }
        systemProperty("p4.callableRoot", p4CallableRoot.get().asFile.absolutePath)
        systemProperty("p4.observer", p4ObserverJar.get().archiveFile.get().asFile.absolutePath)
        systemProperty("p4.vss", vssJar.get().archiveFile.get().asFile.absolutePath)
        systemProperty("p4.clocheSource", providers.gradleProperty("p4ClocheSource").orElse(rootProject.file("../cloche").absolutePath).get())
        systemProperty("p4.javaHome", System.getProperty("java.home"))
        providers.gradleProperty("p4JavaPaths").orNull?.let { systemProperty("p4.javaPaths", it) }
        providers.gradleProperty("p4GradleCommand").orNull?.let { systemProperty("p4.gradleCommand", it) }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
