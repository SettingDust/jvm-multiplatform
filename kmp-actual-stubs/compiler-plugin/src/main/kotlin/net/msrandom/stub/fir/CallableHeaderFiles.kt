package net.msrandom.stub.fir

import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.diagnostics.KtRegisteredDiagnosticFactoriesStorage
import org.jetbrains.kotlin.config.HmppCliModule
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.java.FirCliSession
import org.jetbrains.kotlin.fir.java.FirJavaVisibilityChecker
import org.jetbrains.kotlin.fir.resolve.FirJvmDefaultImportsProvider
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.lightTree.LightTree2Fir
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.session.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.toSourceLinesMapping
import java.io.File

/** A candidate's native import/provider view, isolated from every live FIR session. */
@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class)
internal class CallableHeaderFiles(
    private val inputs: CallableInputs,
    baseModule: HmppCliModule,
    private val base: FirSession,
) {
    val files = mutableMapOf<CallableInputs.Source, FirFile>()
    private val sessions = mutableMapOf(baseModule to base)

    fun session(module: HmppCliModule): FirSession =
        sessions.getOrPut(module) {
            val parents = inputs.dependencies(module).map(::session)
            val detached = FirCliSession(FirSession.Kind.Source)
            val moduleData =
                FirSourceModuleData(
                    Name.special("<${module.name}>"),
                    base.moduleData.dependencies,
                    parents.map { it.moduleData },
                    base.moduleData.friendDependencies,
                    base.moduleData.platform,
                    isCommon = inputs.isCommon(module),
                )
            moduleData.bindSession(detached)
            detached.registerModuleData(moduleData)
            detached.registerCliCompilerAndCommonComponents(base.languageVersionSettings, false)
            detached.registerResolveComponents(KtRegisteredDiagnosticFactoriesStorage())
            detached.register(FirKotlinScopeProvider::class, base.kotlinScopeProvider)
            detached.register(FirVisibilityChecker::class, FirJavaVisibilityChecker)
            detached.register(FirPlatformClassMapper::class, FirJavaClassMapper(detached))
            detached.register(FirDefaultImportsProviderHolder::class, FirDefaultImportsProviderHolder.of(FirJvmDefaultImportsProvider))
            FirSessionConfigurator(detached).configure()
            detached.registerCommonComponentsAfterExtensionsAreConfigured()
            val provider = FirProviderImpl(detached, detached.kotlinScopeProvider)
            detached.register(FirProvider::class, provider)
            val dependencies = FirCompositeSymbolProvider(
                detached, parents.map { it.symbolProvider } +
                    (base.moduleData.dependencies + base.moduleData.friendDependencies).map { it.session.symbolProvider }.distinct(),
            )
            detached.register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, dependencies)
            detached.register(FirSymbolProvider::class, FirCompositeSymbolProvider(detached, listOf(provider.symbolProvider, dependencies)))
            // Parse the effective P1 inputs for this module only. Imports stay native and can see
            // precisely these files and the directed dependsOn providers, never sibling/child files.
            for (source in inputs.sources.filter { it.module === module }) {
                val file =
                    LightTree2Fir(detached, detached.kotlinScopeProvider).buildFirFile(
                        source.text,
                        KtIoFileSourceFile(File(source.path)),
                        source.text.toSourceLinesMapping(),
                    )
                files[source] = file
                provider.recordFile(file)
            }
            detached
        }
}
