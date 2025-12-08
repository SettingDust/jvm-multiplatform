@file:OptIn(InternalKotlinGradlePluginApi::class)

package net.msrandom.virtualsourcesets

import net.msrandom.virtualsourcesets.model.VirtualSourceSetModelBuilder
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinSingleJavaTargetExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import javax.inject.Inject

private const val KOTLIN_JVM = "org.jetbrains.kotlin.jvm"

private val KotlinCompile.isK2: Provider<Boolean>
    get() = compilerOptions.languageVersion
        .orElse(KotlinVersion.DEFAULT)
        .map { it >= KotlinVersion.KOTLIN_2_0 }

fun KotlinCompile.addK2Argument(value: () -> String) {
    compilerOptions.freeCompilerArgs.addAll(isK2.map {
        if (it) {
            listOf(value())
        } else {
            emptyList()
        }
    })
}

@Suppress("unused")
open class JavaVirtualSourceSetsPlugin @Inject constructor(private val modelBuilderRegistry: ToolingModelBuilderRegistry) :
    Plugin<Project> {
    private fun Project.extend(base: String, dependency: String) = project.configurations.findByName(dependency)?.let {
        project.configurations.findByName(base)?.extendsFrom(it)
    }

    private fun SourceSet.addJavaCommonSources(task: JavaCompile) {
        task.source(java)

        extensions.getByType<SourceSetStaticLinkageInfo>().links.all {
            addJavaCommonSources(task)
        }
    }

    private fun SourceSet.addCommonResources(task: ProcessResources) {
        task.from(resources)

        extensions.getByType<SourceSetStaticLinkageInfo>().links.all {
            addCommonResources(task)
        }
    }

    private fun SourceSet.addKotlinCommonSources(
        kotlin: KotlinSourceSetContainer,
        objectFactory: ObjectFactory,
        providerFactory: ProviderFactory,
        dependency: SourceSet,
        info: SourceSetStaticLinkageInfo,
        compileTask: KotlinCompile,
        handledSourceSetNames: MutableSet<String> = hashSetOf(),
    ) {
        val kotlinSourceSet = kotlin.sourceSets.getByName(name)
        val kotlinDependency = kotlin.sourceSets.getByName(dependency.name)

        val newFragments = listOf(kotlinSourceSet, kotlinDependency).filter {
            handledSourceSetNames.add(it.name)
        }

        val weakLinks = info.weakTreeLinks(dependency)
        val emptyList: Provider<List<String>> = providerFactory.provider { emptyList() }

        compileTask.compilerOptions.freeCompilerArgs.addAll(compileTask.isK2.flatMap { isK2 ->
            if (isK2) {
                val k2Args = objectFactory.listProperty(String::class.java)

                k2Args.add("-Xfragment-refines=${kotlinSourceSet.name}:${kotlinDependency.name}")

                for (fragment in newFragments) {
                    k2Args.add("-Xfragments=${fragment.name}")

                    for (sourceDirectory in fragment.kotlin.sourceDirectories.asFileTree) {
                        k2Args.add("-Xfragment-sources=${fragment.name}:$sourceDirectory")
                    }
                }

                k2Args.addAll(weakLinks.map {
                    it.map {
                        "-Xfragment-refines=${kotlinDependency.name}:${it.name}"
                    }
                })

                k2Args
            } else {
                emptyList
            }
        })

        compileTask.compilerOptions.freeCompilerArgs.addAll(providerFactory.provider {
            kotlinDependency.kotlin.sourceDirectories.asFileTree.map {
                "-Xcommon-sources=${it}"
            }
        })

        compileTask.source(kotlinDependency.kotlin)

        dependency.extensions.getByType<SourceSetStaticLinkageInfo>().links.all {
            dependency.addKotlinCommonSources(
                kotlin,
                objectFactory,
                providerFactory,
                this,
                info,
                compileTask,
                handledSourceSetNames
            )
        }
    }

    private fun SourceSet.addDependency(dependency: SourceSet, info: SourceSetStaticLinkageInfo, project: Project) {
        if (System.getProperty("idea.sync.active")?.toBoolean() == true) {
            // TODO Temporary until an intellij plugin is complete
            compileClasspath += dependency.output
        }

        project.tasks.named(compileJavaTaskName, JavaCompile::class.java) {
            dependency.addJavaCommonSources(this)
        }

        project.tasks.named(processResourcesTaskName, ProcessResources::class.java) {
            dependency.addCommonResources(this)
        }

        project.plugins.withId(KOTLIN_JVM) {
            val kotlin = project.extensions.getByType<KotlinSingleJavaTargetExtension>()
            val kotlinCompilation = kotlin.target.compilations.getByName(name)

            kotlinCompilation.compileTaskProvider.configure {
                this as KotlinCompile

                compilerOptions.freeCompilerArgs.add("-Xmulti-platform")

                addKotlinCommonSources(
                    kotlin,
                    project.serviceOf(),
                    project.serviceOf(),
                    dependency,
                    info,
                    this,
                )
            }
        }
    }

    override fun apply(target: Project) {
        target.apply<JavaPlugin>()

        target.extensions.getByType<SourceSetContainer>().all {
            val sourceSet = this

            val staticLinkInfo =
                sourceSet.extensions.create(
                    "staticLinkage",
                    SourceSetStaticLinkageInfo::class,
                    sourceSet,
                    target.objects
                )

            staticLinkInfo.links.all {
                sourceSet.addDependency(this, staticLinkInfo, target)
            }
        }

        modelBuilderRegistry.register(VirtualSourceSetModelBuilder())
    }
}
