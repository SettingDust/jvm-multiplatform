package net.msrandom.stub.fir

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class FirExpectEnumConstructorGenerator(
    session: FirSession,
    private val commonSourcePaths: Set<String>,
    private val actualDeclarationIndex: ActualDeclarationIndex,
) : FirDeclarationGenerationExtension(session) {

    private fun isInCommonSource(classSymbol: FirClassSymbol<*>): Boolean {
        if (commonSourcePaths.isEmpty()) return false

        // Fast path: PSI-backed source
        val psiFilePath = (classSymbol.source as? KtPsiSourceElement)
            ?.psi?.containingFile?.let { it as? KtFile }?.virtualFilePath
        if (psiFilePath != null) {
            val canonical = try { File(psiFilePath).canonicalPath } catch (_: Exception) { return false }
            return canonical in commonSourcePaths
        }

        // K2 light-tree: retrieve FirFile via FirProvider
        val firFile = try { session.firProvider.getFirClassifierContainerFile(classSymbol) } catch (_: Exception) { null } ?: return false

        val firFilePsiPath = (firFile.source as? KtPsiSourceElement)
            ?.psi?.let { it as? KtFile }?.virtualFilePath
        if (firFilePsiPath != null) {
            val canonical = try { File(firFilePsiPath).canonicalPath } catch (_: Exception) { return false }
            return canonical in commonSourcePaths
        }

        // Tail matching fallback
        val packagePath = firFile.packageDirective.packageFqName.asString().replace('.', '/')
        val tail = if (packagePath.isEmpty()) firFile.name else "$packagePath/${firFile.name}"
        return commonSourcePaths.any { p -> p.replace('\\', '/').endsWith("/$tail") }
    }

    /**
     * Heuristic: if `getClassLikeSymbolByClassId` returns a *different* symbol object for the same
     * ClassId, an `actual` is registered alongside the `expect` in the session.
     */
    private fun hasMatchingActual(classSymbol: FirClassSymbol<*>): Boolean {
        return try {
            if (actualDeclarationIndex.hasClassLike(classSymbol.classId.packageFqName.asString(), classSymbol.classId.shortClassName.asString())) {
                return true
            }
            val other = session.symbolProvider.getClassLikeSymbolByClassId(classSymbol.classId)
            other != null && other !== classSymbol
        } catch (_: Exception) { false }
    }

    override fun getCallableNamesForClass(
        classSymbol: FirClassSymbol<*>,
        context: MemberGenerationContext
    ): Set<Name> {
        if ((classSymbol.classKind == ClassKind.ENUM_CLASS || classSymbol.classKind == ClassKind.ENUM_ENTRY)
            && classSymbol.source.getModifierList()?.contains(KtTokens.EXPECT_KEYWORD) == true
            && !(isInCommonSource(classSymbol) && hasMatchingActual(classSymbol))) {
            // Only generate synthetic constructor when the enum class is being stripped
            // (i.e. not in a common-source file paired with an actual).
            return setOf(Name.special("<init>"))
        }

        return emptySet()
    }

    override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> =
        listOf(createConstructor(context.owner, Key, true).symbol)

    private object Key : GeneratedDeclarationKey()
}