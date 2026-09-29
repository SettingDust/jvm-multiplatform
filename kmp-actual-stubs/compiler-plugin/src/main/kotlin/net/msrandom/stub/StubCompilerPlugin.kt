@file:Suppress("DEPRECATION_ERROR")

package net.msrandom.stub

import com.google.auto.service.AutoService
import net.msrandom.stub.fir.CallableInputs
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.fir.extensions.FirAnalysisHandlerExtension
import net.msrandom.stub.fir.FirExpectEnumConstructorGenerator
import net.msrandom.stub.fir.FirExpectTransformer
import net.msrandom.stub.ir.IrStubBodyGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class StubCompilerPlugin : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        FirExtensionRegistrarAdapter.registerExtension(StubFirExtensions(CallableInputs.get(configuration)))
        IrGenerationExtension.registerExtension(IrStubBodyGenerator)
    }
}

internal class StubFirExtensions(private val inputs: CallableInputs) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: org.jetbrains.kotlin.fir.FirSession -> FirExpectTransformer(session, inputs) }
        +{ session: org.jetbrains.kotlin.fir.FirSession -> FirExpectEnumConstructorGenerator(session, inputs) }
    }
}

/** Fixed K2 2.2.20 entry, before sourcesProvider and FIR session construction. */
@OptIn(ExperimentalCompilerApi::class)
@AutoService(ComponentRegistrar::class)
class StubInputRegistrar : ComponentRegistrar {
    override val supportsK2 = true

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val inputs = CallableInputs.get(configuration)
        FirAnalysisHandlerExtension.registerExtension(project, object : FirAnalysisHandlerExtension() {
            override fun isApplicable(configuration: CompilerConfiguration): Boolean = !inputs.collect(project)
            override fun doAnalysis(project: Project, configuration: CompilerConfiguration): Boolean = false
        })
    }
}