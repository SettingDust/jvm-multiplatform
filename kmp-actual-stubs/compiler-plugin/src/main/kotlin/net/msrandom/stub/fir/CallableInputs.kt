@file:Suppress("DEPRECATION_ERROR")

package net.msrandom.stub.fir

import java.io.File
import org.jetbrains.kotlin.cli.common.allFiles
import org.jetbrains.kotlin.cli.common.collectSources
import org.jetbrains.kotlin.cli.jvm.compiler.VfsBasedProjectEnvironment
import org.jetbrains.kotlin.cli.common.localfs.KotlinLocalFileSystem
import org.jetbrains.kotlin.cli.jvm.compiler.jarfs.FastJarFileSystem
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.extensions.PreprocessedVirtualFileFactoryExtension
import org.jetbrains.kotlin.incremental.IncrementalJvmCache
import org.jetbrains.kotlin.modules.TargetId
import org.jetbrains.kotlin.parsing.KotlinLightParser
import org.jetbrains.kotlin.KtIoFileSourceFile
import org.jetbrains.kotlin.com.intellij.lang.LighterASTNode
import org.jetbrains.kotlin.com.intellij.openapi.util.Ref
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.session.sourcesToPathsMapper

/** One invocation's effective physical sources. Never persisted across compiler invocations. */
internal class CallableInputs(private val configuration: CompilerConfiguration) {
    data class Source(val path: String, val module: HmppCliModule, val text: String)
    data class Declaration(val source: Source, val start: Int, val pkg: List<String>, val owner: List<String>,
                           val name: String, val kind: String, val actual: Boolean, val expect: Boolean)
    private var graph: HmppCliModuleStructure? = null
    var sources: List<Source> = emptyList(); private set
    var declarations: List<Declaration> = emptyList(); private set
    private var ready = false

    fun collect(projectEnvironment: VfsBasedProjectEnvironment): Boolean {
        val project = projectEnvironment.project
        val messages = configuration.getNotNull(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY)
        try {
            require(PreprocessedVirtualFileFactoryExtension.getInstances(project).isEmpty()) {
                "unsupported source preprocessor; rejected before expect conversion"
            }
            val dirty = collectSources(configuration, projectEnvironment).allFiles.toList()
            require(!messages.hasErrors()) { "native source collection failed" }
            val structure = configuration.get(CommonConfigurationKeys.HMPP_MODULE_STRUCTURE)
            graph = structure
            val modules = structure?.modules ?: listOf(HmppCliModule(configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME), dirty.map { it.path!! }.toSet()))
            val membership = modules.flatMap { module -> module.sources.map { canonical(it) to module } }.groupBy({ it.first }, { it.second })
            require(membership.values.all { it.size == 1 }) { "source has ambiguous fragment ownership" }
            val components = configuration.get(JVMConfigurationKeys.INCREMENTAL_COMPILATION_COMPONENTS)
            val cache = components?.getIncrementalCache(TargetId(configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME), "java-production"))
            require(cache == null || cache is IncrementalJvmCache) { "unsupported incremental cache ${cache?.javaClass?.name}" }
            val selected = linkedMapOf<String, HmppCliModule>()
            for (source in dirty) {
                val path = canonical(requireNotNull(source.path) { "nonphysical source" })
                selected[path] = requireNotNull(membership[path]?.singleOrNull()) { "source has no fragment owner: $path" }
            }
            if (cache is IncrementalJvmCache) for ((path, owners) in membership) {
                if (path !in selected && cache.classesBySources(listOf(File(path))).any { it.internalName == ".kotlin_module" }) {
                    require(File(path).isFile) { "cached source no longer exists: $path" }
                    selected[path] = owners.single()
                }
            }
            val recovered = configuration.copy()
            recovered.put(CLIConfigurationKeys.CONTENT_ROOTS, mutableListOf())
            selected.forEach { (path, module) -> recovered.addKotlinSourceRoot(path, module !== modules.last(), module.name) }
            val effective = collectSources(recovered, projectEnvironment)
            sources = effective.allFiles.map { source ->
                val path = canonical(requireNotNull(source.path))
                Source(path, requireNotNull(selected[path]), StringUtil.convertLineSeparators(source.getContentsAsStream().bufferedReader().use { it.readText() }))
            }.toList()
            require(sources.map { it.path }.toSet() == selected.keys) { "recovered source collection changed membership" }
            require(!messages.hasErrors()) { "recovered source collection failed" }
            declarations = sources.flatMap(::parse)
            ready = true
            return true
        } catch (e: IllegalArgumentException) {
            messages.report(CompilerMessageSeverity.ERROR, "STUB_INPUT_UNAVAILABLE: ${e.message}")
        } catch (e: java.io.IOException) {
            messages.report(CompilerMessageSeverity.ERROR, "STUB_INPUT_UNAVAILABLE: ${e.message}")
        }
        return false
    }

    private fun parse(source: Source): List<Declaration> {
        var valid = true
        val tree = KotlinLightParser.buildLightTree(source.text, KtIoFileSourceFile(File(source.path))) { _, _, _ -> valid = false }
        fun children(node: LighterASTNode): List<LighterASTNode> {
            val ref = Ref<Array<LighterASTNode?>>()
            val count = tree.getChildren(node, ref)
            return ref.get()?.take(count)?.filterNotNull().orEmpty()
        }
        fun text(node: LighterASTNode) = source.text.substring(node.startOffset, node.endOffset).removeSurrounding("`")
        fun identifiers(node: LighterASTNode): List<String> = if (node.tokenType.toString() == "IDENTIFIER") listOf(text(node)) else children(node).flatMap(::identifiers)
        val top = children(tree.root)
        val pkg = top.firstOrNull { it.tokenType.toString() == "PACKAGE_DIRECTIVE" }?.let(::identifiers).orEmpty()
        val result = mutableListOf<Declaration>()
        fun visit(nodes: List<LighterASTNode>, owner: List<String>) {
            for (node in nodes) {
                val kind = node.tokenType.toString()
                if (kind !in setOf("FUN", "PROPERTY", "CLASS", "OBJECT_DECLARATION", "TYPEALIAS")) continue
                val parts = children(node)
                val name = parts.singleOrNull { it.tokenType.toString() == "IDENTIFIER" }
                require(name != null) { "unsupported declaration identity in ${source.path}" }
                val modifiers = parts.firstOrNull { it.tokenType.toString() == "MODIFIER_LIST" }?.let(::children).orEmpty().map { it.tokenType.toString() }
                result += Declaration(source, node.startOffset, pkg, owner, text(name), kind, "actual" in modifiers, "expect" in modifiers)
                if (kind in setOf("CLASS", "OBJECT_DECLARATION")) parts.firstOrNull { it.tokenType.toString() == "CLASS_BODY" }?.let { visit(children(it), owner + text(name)) }
            }
        }
        visit(top, emptyList())
        require(valid) { "source parse error in ${source.path}" }
        return result
    }

    fun source(declaration: FirDeclaration, session: FirSession): Source? {
        val path = declaration.source?.let(session.sourcesToPathsMapper::getSourceFilePath)?.let(::canonical) ?: return null
        return sources.singleOrNull { it.path == path }
    }

    fun dependencies(module: HmppCliModule): List<HmppCliModule> = graph?.sourceDependencies?.get(module).orEmpty()

    fun isCommon(module: HmppCliModule): Boolean = graph?.modules?.let { module !== it.last() } ?: false

    fun classMissing(declaration: FirRegularClass, session: FirSession): Boolean {
        if (!ready) return false
        val source = source(declaration, session) ?: return false
        val original = declarations.singleOrNull {
            it.source === source && it.start == declaration.source?.startOffset && it.kind in setOf("CLASS", "OBJECT_DECLARATION")
        } ?: return false
        if (!original.expect && original.owner.isEmpty()) return false
        // A retained enclosing class owns native checking of every nested declaration.
        for (depth in original.owner.indices) {
            val parents = declarations.filter {
                it.pkg == original.pkg && it.owner == original.owner.take(depth) &&
                    it.name == original.owner[depth] && it.expect
            }.map { it.source.module }.toSet()
            if (declarations.any {
                it.pkg == original.pkg && it.owner == original.owner.take(depth) &&
                    it.name == original.owner[depth] && it.actual &&
                    it.kind in setOf("CLASS", "OBJECT_DECLARATION", "TYPEALIAS") &&
                    source.module in firstWave(it.source.module, parents)
            }) return false
        }
        val bucket = declarations.filter {
            it.pkg == original.pkg && it.owner == original.owner && it.name == original.name &&
                it.actual && it.kind in setOf("CLASS", "OBJECT_DECLARATION", "TYPEALIAS")
        }
        val interests = declarations.filter {
            it.pkg == original.pkg && it.owner == original.owner && it.name == original.name && it.expect
        }.map { it.source.module }.toSet()
        return bucket.none { source.module in firstWave(it.source.module, interests) }
    }

    fun candidates(declaration: FirCallableDeclaration, session: FirSession): List<Declaration>? {
        if (!ready) return null
        val source = source(declaration, session) ?: return null
        val original = declarations.singleOrNull { it.source === source && it.start == declaration.source?.startOffset && it.expect } ?: return null
        if (original.owner.isNotEmpty()) return null
        val bucket = declarations.filter { it.pkg == original.pkg && it.owner == original.owner && it.name == original.name && it.kind in setOf("FUN", "PROPERTY") }
        val interests = bucket.filter { it.expect }.map { it.source.module }.toSet()
        return bucket.filter { candidate ->
            candidate.actual && source.module in firstWave(candidate.source.module, interests)
        }
    }

    private fun firstWave(actual: HmppCliModule, interests: Set<HmppCliModule>): Set<HmppCliModule> {
        val result = mutableSetOf<HmppCliModule>()
        val visited = mutableSetOf<HmppCliModule>()
        fun visit(module: HmppCliModule) {
            if (!visited.add(module)) return
            if (module in interests) result += module
            else graph?.sourceDependencies?.get(module).orEmpty().forEach(::visit)
        }
        graph?.sourceDependencies?.get(actual).orEmpty().forEach(::visit)
        return result
    }

    companion object {
        val KEY = CompilerConfigurationKey<CallableInputs>("invocation callable inputs")
        fun get(configuration: CompilerConfiguration): CallableInputs = configuration.get(KEY) ?: CallableInputs(configuration).also { configuration.put(KEY, it) }
        private fun canonical(path: String) = File(path).canonicalPath
    }
}
