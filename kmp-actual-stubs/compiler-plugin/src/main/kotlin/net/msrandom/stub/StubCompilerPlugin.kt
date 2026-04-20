package net.msrandom.stub

import com.google.auto.service.AutoService
import net.msrandom.stub.fir.FirExpectEnumConstructorGenerator
import net.msrandom.stub.fir.FirExpectTransformer
import net.msrandom.stub.fir.ActualDeclarationIndex
import net.msrandom.stub.ir.IrStubBodyGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.KotlinSourceRoot
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
class StubCompilerPlugin : CompilerPluginRegistrar() {

    override val supportsK2: Boolean = true

    override val pluginId: String = "net.msrandom.stub"

    override fun ExtensionStorage.registerExtensions(
        configuration: CompilerConfiguration
    ) {
        // Collect canonical paths of all source files passed via -Xcommon-sources.
        // These files belong to "common" fragments whose `expect` declarations must
        // remain intact for K2's expect/actual matching in refining fragments.
        val commonSourcePaths = configuration
            .get(CLIConfigurationKeys.CONTENT_ROOTS, emptyList())
            .filterIsInstance<KotlinSourceRoot>()
            .filter { it.isCommon }
            .mapNotNullTo(HashSet()) { root ->
                try { File(root.path).canonicalPath } catch (_: Exception) { null }
            }

        val platformSourcePaths = configuration
            .get(CLIConfigurationKeys.CONTENT_ROOTS, emptyList())
            .filterIsInstance<KotlinSourceRoot>()
            .filter { !it.isCommon }
            .mapNotNullTo(HashSet()) { root ->
                try { File(root.path).canonicalPath } catch (_: Exception) { null }
            }

        val actualDeclarationIndex = ActualDeclarationIndex.fromSourceRoots(platformSourcePaths)

        FirExtensionRegistrarAdapter.registerExtension(StubFirExtensions(commonSourcePaths, actualDeclarationIndex))
        IrGenerationExtension.registerExtension(IrStubBodyGenerator)
    }
}

class StubFirExtensions(
    private val commonSourcePaths: Set<String>,
    private val actualDeclarationIndex: ActualDeclarationIndex,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +{ session: FirSession -> FirExpectTransformer(session, commonSourcePaths, actualDeclarationIndex) }
        +{ session: FirSession -> FirExpectEnumConstructorGenerator(session, commonSourcePaths, actualDeclarationIndex) }
    }
}