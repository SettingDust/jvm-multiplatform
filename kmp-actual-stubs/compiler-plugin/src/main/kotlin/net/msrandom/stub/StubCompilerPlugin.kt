@file:Suppress("DEPRECATION_ERROR")

package net.msrandom.stub

import com.google.auto.service.AutoService
import net.msrandom.stub.fir.CallableInputs
import net.msrandom.stub.fir.FirExpectEnumConstructorGenerator
import net.msrandom.stub.fir.FirExpectTransformer
import net.msrandom.stub.ir.IrStubBodyGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.extensionsStorage
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class StubCompilerPlugin : CompilerPluginRegistrar() {
    override val pluginId: String = PLUGIN_ID
    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val storage: CompilerPluginRegistrar.ExtensionStorage = this

        @Suppress("UNCHECKED_CAST")
        val extensions = storage.registeredExtensions as MutableMap<org.jetbrains.kotlin.extensions.ExtensionPointDescriptor<*>, List<Any>>
        extensions[FirExtensionRegistrarAdapter.Companion] = listOf(StubFirExtensions(CallableInputs.get(configuration)))
        extensions[IrGenerationExtension.Companion] = listOf(IrStubBodyGenerator)
    }
}

internal class StubFirExtensions(
    private val inputs: CallableInputs,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: org.jetbrains.kotlin.fir.FirSession -> FirExpectTransformer(session, inputs) }
        +{ session: org.jetbrains.kotlin.fir.FirSession -> FirExpectEnumConstructorGenerator(session, inputs) }
    }
}

/** Captures the invocation environment for collection after plugin configuration. */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(ComponentRegistrar::class)
class StubInputRegistrar : ComponentRegistrar {
    override val supportsK2 = true

    override fun registerProjectComponents(
        project: MockProject,
        configuration: CompilerConfiguration,
    ) {
        val inputs = CallableInputs.get(configuration)
        val projectEnvironment =
            VfsBasedProjectEnvironment(
                project,
                listOf(KotlinLocalFileSystem(), CoreJarFileSystem()),
            ) { PackagePartProvider.Empty }
        with(requireNotNull(configuration.extensionsStorage)) {
            FirAnalysisHandlerExtension.registerExtension(
                object : FirAnalysisHandlerExtension() {
                    override fun isApplicable(configuration: CompilerConfiguration): Boolean = !inputs.collect(projectEnvironment)

                    override fun doAnalysis(
                        project: Project,
                        configuration: CompilerConfiguration,
                    ): Boolean = false
                },
            )
        }
    }
}
