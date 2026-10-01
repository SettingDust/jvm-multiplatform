package net.msrandom.stubs

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively

@CacheableTask
abstract class GenerateStubApi : DefaultTask() {
    abstract val classpaths: ListProperty<List<ResolvedArtifact>>
        @Nested get

    abstract val excludes: ListProperty<String>
        @Input get

    abstract val apiFileName: Property<String>
        @Input get

    abstract val preserveMethodBodies: Property<Boolean>
        @Input get

    abstract val outputDirectory: DirectoryProperty
        @OutputDirectory get

    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    init {
        preserveMethodBodies.convention(false)
        apiFileName.convention("api-stub.jar")
        outputDirectory.convention(project.layout.dir(project.provider { temporaryDir }))
    }

    @TaskAction
    fun generateStub() {
        @OptIn(ExperimentalPathApi::class)
        outputDirectory.asFile
            .get()
            .toPath()
            .deleteRecursively()

        val artifacts = classpaths.get().flatten()
        workerExecutor.noIsolation().submit(GenerateStubWorkAction::class.java) {
            classpathFiles.set(
                artifacts.map {
                    it.file.asFile
                        .get()
                        .absolutePath
                },
            )
            classpathSizes.set(classpaths.get().map { it.size })
            componentIds.set(artifacts.map { it.componentId.orNull ?: "" })
            types.set(artifacts.map { it.type.orNull?.name ?: "" })
            moduleIds.set(artifacts.map { it.moduleId.orNull?.toString() ?: "" })
            moduleVersions.set(artifacts.map { it.moduleVersion.orNull ?: "" })
            excludes.set(this@GenerateStubApi.excludes.get())
            outputDirectory.set(
                this@GenerateStubApi
                    .outputDirectory.asFile
                    .get()
                    .absolutePath,
            )
            apiFileName.set(this@GenerateStubApi.apiFileName.get())
            preserveMethodBodies.set(this@GenerateStubApi.preserveMethodBodies.get())
        }
    }

    abstract class GenerateStubWorkAction : WorkAction<GenerateStubWorkAction.Parameters> {
        interface Parameters : WorkParameters {
            val classpathFiles: ListProperty<String>
            val classpathSizes: ListProperty<Int>
            val componentIds: ListProperty<String>
            val types: ListProperty<String>
            val moduleIds: ListProperty<String>
            val moduleVersions: ListProperty<String>
            val excludes: ListProperty<String>
            val outputDirectory: Property<String>
            val apiFileName: Property<String>
            val preserveMethodBodies: Property<Boolean>
        }

        @OptIn(ExperimentalPathApi::class)
        override fun execute() {
            val files = parameters.classpathFiles.get().map(::File)
            val componentIds = parameters.componentIds.get()
            val types = parameters.types.get()
            val moduleIds = parameters.moduleIds.get()
            val moduleVersions = parameters.moduleVersions.get()
            val artifacts =
                files.indices.map { index ->
                    StubGenerator.ArtifactSpec(
                        file = files[index],
                        componentId = componentIds[index].ifEmpty { null },
                        type = types[index].takeUnless(String::isEmpty)?.let(GenerateStubApi.ResolvedArtifact.Type::valueOf),
                        moduleId = moduleIds[index].ifEmpty { null },
                        moduleVersion = moduleVersions[index].ifEmpty { null },
                    )
                }
            val groups = artifacts.chunkedBySizes(parameters.classpathSizes.get())
            val directory = File(parameters.outputDirectory.get()).toPath()
            @OptIn(ExperimentalPathApi::class)
            directory.deleteRecursively()
            directory.createDirectories()
            try {
                val extras =
                    StubGenerator.generateStub(
                        groups,
                        parameters.excludes.get(),
                        directory.resolve(parameters.apiFileName.get()),
                        parameters.preserveMethodBodies.get(),
                    )
                for (artifact in extras) {
                    val target = directory.resolve(artifact.componentId!!.replace(':', '_'))
                    target.createDirectories()
                    artifact.file.toPath().copyTo(target.resolve(artifact.file.name), StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (error: Throwable) {
                directory.deleteRecursively()
                throw error
            }
        }
    }

    interface ResolvedArtifact {
        val componentId: Property<String>
            @Optional
            @Input
            get

        val type: Property<Type>
            @Optional
            @Input
            get

        val moduleVersion: Property<String>
            @Optional
            @Input
            get

        val moduleId: Property<ModuleIdentifier>
            @Optional
            @Input
            get

        // Full bytes and the copied filename affect output, even with preservation disabled.
        val file: RegularFileProperty
            @PathSensitive(PathSensitivity.NAME_ONLY)
            @InputFile
            get

        fun setComponent(component: ComponentIdentifier) {
            componentId.set(component.toString())

            when (component) {
                is ModuleComponentIdentifier -> {
                    type.set(Type.Module)
                    moduleId.set(component.moduleIdentifier)
                    moduleVersion.set(component.version)
                }

                is ProjectComponentIdentifier -> {
                    type.set(Type.Project)
                }
            }
        }

        enum class Type {
            Module,
            Project,
        }
    }
}

private fun <T> List<T>.chunkedBySizes(sizes: List<Int>): List<List<T>> {
    var offset = 0
    return sizes.map { size ->
        subList(offset, offset + size).also { offset += size }
    }
}
