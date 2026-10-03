package net.msrandom.stubs

import org.gradle.api.Plugin
import org.gradle.api.Project

/** Registers the public task exposed by the classpath API stubs plugin. */
class ClasspathApiStubsPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("generateStubApi", GenerateStubApi::class.java)
    }
}
