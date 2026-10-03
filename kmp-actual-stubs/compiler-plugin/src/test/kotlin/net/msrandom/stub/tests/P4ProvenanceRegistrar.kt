package net.msrandom.stub.tests

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.languageVersionSettings
import java.io.File
import java.security.MessageDigest

/** Observation only: runs inside the product compiler's plugin classloader. */
@OptIn(ExperimentalCompilerApi::class)
class P4ProvenanceRegistrar : CompilerPluginRegistrar() {
    override val supportsK2 = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val loaded = configuration.getList(COMPILER_PLUGIN_REGISTRARS)
            .single { it.javaClass.name == "net.msrandom.stub.StubCompilerPlugin" }.javaClass
        val jar =
            File(
                loaded.protectionDomain.codeSource.location
                    .toURI(),
            )
        val hash = MessageDigest.getInstance("SHA-256").digest(jar.readBytes()).joinToString("") { "%02x".format(it) }
        val settings = configuration.languageVersionSettings
        configuration.getNotNull(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY).report(
            CompilerMessageSeverity.STRONG_WARNING,
            "P4_PROVENANCE|compiler=${KotlinCompilerVersion.VERSION}|language=${settings.languageVersion}|api=${settings.apiVersion}" +
                "|plugin=${jar.absolutePath}|sha256=$hash|compilerSource=${KotlinCompilerVersion::class.java.protectionDomain.codeSource.location}",
        )
    }
}
