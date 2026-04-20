package net.msrandom.stub.fir

import net.msrandom.stub.STUB
import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.diagnostics.AbstractKtDiagnosticFactory
import org.jetbrains.kotlin.fakeElement
import org.jetbrains.kotlin.fir.FirAnnotationContainer
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getModifierList
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildLiteralExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildVarargArgumentsExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirStatusTransformerExtension
import org.jetbrains.kotlin.fir.extensions.transform
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.createArrayType
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.KtPsiSourceElement
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.types.ConstantValueKind
import java.io.File

/**
 * Transforms `expect` declarations so they can be compiled to JVM without actual implementations.
 *
 * Declarations in files passed via `-Xcommon-sources` are intentionally skipped: K2 uses those
 * files as "common" fragments whose `expect` declarations must be matched by `actual` in a
 * refining (non-common) fragment.  Stripping `isExpect` there would break K2's expect/actual
 * resolution and cause "actual has no corresponding expected declaration" errors in the refining
 * fragment.
 */
class FirExpectTransformer(
    session: FirSession,
    private val commonSourcePaths: Set<String>,
    private val actualDeclarationIndex: ActualDeclarationIndex,
) : FirStatusTransformerExtension(session) {

    /**
     * Returns `true` when [declaration] lives in a file that was passed as a common source
     * (i.e. has `isCommon=true` in CONTENT_ROOTS).
     *
     * K2 compiles with KtLightSourceElement (light-tree), so PSI is not available on the
     * individual declaration's source.  We instead obtain the containing FirFile via the
     * FirProvider and match it against the known common-source paths.
     */
    private fun isInCommonSource(declaration: FirDeclaration): Boolean {
        if (commonSourcePaths.isEmpty()) return false

        // Fast path: PSI-backed source (K1 or IDE-facing compilation)
        val psiFilePath = (declaration.source as? KtPsiSourceElement)
            ?.psi?.containingFile?.let { it as? KtFile }?.virtualFilePath
        if (psiFilePath != null) {
            val canonical = try { File(psiFilePath).canonicalPath } catch (_: Exception) { return false }
            return canonical in commonSourcePaths
        }

        // K2 light-tree mode: declaration.source is KtLightSourceElement with no PSI
        // Retrieve the containing FirFile and check its path
        val firFile = try {
            when (val sym = declaration.symbol) {
                is FirClassLikeSymbol<*> -> session.firProvider.getFirClassifierContainerFile(sym)
                is FirCallableSymbol<*> -> session.firProvider.getFirCallableContainerFile(sym)
                else -> null
            }
        } catch (_: Exception) { null } ?: return false

        // Try PSI from the FirFile's own source element
        val firFilePsiPath = (firFile.source as? KtPsiSourceElement)
            ?.psi?.let { it as? KtFile }?.virtualFilePath
        if (firFilePsiPath != null) {
            val canonical = try { File(firFilePsiPath).canonicalPath } catch (_: Exception) { return false }
            return canonical in commonSourcePaths
        }

        // Final fallback: match by package-path/FileName.kt tail
        // (assumes standard Maven/Gradle source layout: package mirrors directory structure)
        val packagePath = firFile.packageDirective.packageFqName.asString().replace('.', '/')
        val tail = if (packagePath.isEmpty()) firFile.name else "$packagePath/${firFile.name}"
        return commonSourcePaths.any { p -> p.replace('\\', '/').endsWith("/$tail") }
    }

    /**
     * Returns true when an `expect` declaration in a common-source file has a corresponding `actual`
     * in the refining fragment of the same compilation.
     *
     * For callables: queries `session.symbolProvider.getTopLevelCallableSymbols` and checks for a
     * sibling symbol that carries `actual` in its modifier list.
     * For class-like: uses `getClassLikeSymbolByClassId` returning a *different* symbol object as
     * a heuristic — when an `actual` is registered under the same ClassId, the provider may return
     * the actual symbol instead of the expect symbol.
     */
    private fun hasMatchingActual(declaration: FirDeclaration): Boolean {
        return try {
            when (val sym = declaration.symbol) {
                is FirCallableSymbol<*> -> {
                    val cid = sym.callableId ?: return false
                    // Only top-level expect callables have EXPECT_KEYWORD; skip member callables.
                    if (cid.classId != null) return false
                    if (actualDeclarationIndex.hasCallable(cid.packageName.asString(), cid.callableName.asString())) {
                        return true
                    }
                    session.symbolProvider
                        .getTopLevelCallableSymbols(cid.packageName, cid.callableName)
                        .any { other ->
                            other !== sym &&
                            other.source.getModifierList()?.let { KtTokens.ACTUAL_KEYWORD in it } == true
                        }
                }
                is FirClassLikeSymbol<*> -> {
                    if (actualDeclarationIndex.hasClassLike(sym.classId.packageFqName.asString(), sym.classId.shortClassName.asString())) {
                        return true
                    }
                    // If an actual exists, the symbol provider may return it (a different object)
                    // when queried by the same ClassId.
                    val other = session.symbolProvider.getClassLikeSymbolByClassId(sym.classId)
                    other != null && other !== sym
                }
                else -> false
            }
        } catch (_: Exception) { false }
    }

    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        val modifierList = declaration.source.getModifierList()
        val status = (declaration as? FirMemberDeclaration)?.status
        val isExpectDeclaration =
            (modifierList?.let { KtTokens.EXPECT_KEYWORD in it } == true) ||
                (status?.isExpect == true)
        val isActualDeclaration =
            (modifierList?.let { KtTokens.ACTUAL_KEYWORD in it } == true) ||
                (status?.isActual == true)

        if (isExpectDeclaration) {
            // Preserve `isExpect` only when the declaration is BOTH in a common-source file
            // AND has a matching `actual` in the refining fragment.
            // If there is no actual, strip `isExpect` as normal so K2 doesn't report
            // NO_ACTUAL_FOR_EXPECT.
            if (isInCommonSource(declaration) && hasMatchingActual(declaration)) return false
            return true
        }
        // Intercept `actual` declarations so that transformStatus can add ACTUAL_WITHOUT_EXPECT
        // suppression. Without this, IDE analysis (where commonSourcePaths is empty and all
        // `expect` have been stripped) would show false errors on `actual` declarations.
        if (isActualDeclaration) {
            return true
        }
        return false
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        function: FirSimpleFunction,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        if (status.isActual) {
            addSuppressionNames(function, "ACTUAL_WITHOUT_EXPECT")
            return status
        }
        addStubAnnotation(function)
        addSuppressions(function,
            FirErrors.NON_MEMBER_FUNCTION_NO_BODY,
            FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY
        )
        return status.transform { isExpect = false }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        property: FirProperty,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        if (status.isActual) {
            addSuppressionNames(property, "ACTUAL_WITHOUT_EXPECT")
            return status
        }
        addStubAnnotation(property)
        addSuppressions(property,
            FirErrors.MUST_BE_INITIALIZED,
            FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT,
        )
        return status.transform { isExpect = false }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        regularClass: FirRegularClass,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        if (status.isActual) {
            addSuppressionNames(regularClass, "ACTUAL_WITHOUT_EXPECT")
            return status
        }
        addStubAnnotation(regularClass)

        regularClass.transformDeclarations(object : FirTransformer<Nothing?>() {
            override fun <E : FirElement> transformElement(
                element: E,
                data: Nothing?
            ): E = element

            override fun transformDeclaration(declaration: FirDeclaration, data: Nothing?): FirDeclaration {
                addSuppressions(declaration,
                    FirErrors.NON_ABSTRACT_FUNCTION_WITH_NO_BODY,
                    FirErrors.MUST_BE_INITIALIZED,
                    FirErrors.EXTENSION_PROPERTY_MUST_HAVE_ACCESSORS_OR_BE_ABSTRACT,
                )

                return super.transformDeclaration(declaration, data)
            }
        }, null)

        return status.transform { isExpect = false }
    }

    override fun transformStatus(
        status: FirDeclarationStatus,
        typeAlias: FirTypeAlias,
        containingClass: FirClassLikeSymbol<*>?,
        isLocal: Boolean
    ): FirDeclarationStatus {
        if (status.isActual) {
            addSuppressionNames(typeAlias, "ACTUAL_WITHOUT_EXPECT")
            return status
        }
        return status.transform { isExpect = false }
    }

    private fun addStubAnnotation(declaration: FirDeclaration) {
        if (declaration.getAnnotationByClassId(STUB, session) == null) {
            declaration.replaceAnnotations(declaration.annotations + buildAnnotation {
                annotationTypeRef = STUB.createConeType(session).toFirResolvedTypeRef()
                argumentMapping = FirEmptyAnnotationArgumentMapping
                source = declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
            })
        }
    }

    private fun addSuppressions(declaration: FirDeclaration, vararg diagnostics: AbstractKtDiagnosticFactory) {
        addSuppressionNames(declaration, *diagnostics.map { it.name }.toTypedArray())
    }

    private fun addSuppressionNames(declaration: FirDeclaration, vararg names: String) {
        val existingSuppress = declaration.getAnnotationByClassId(StandardClassIds.Annotations.Suppress, session)
        val existingSuppressions = existingSuppress?.getStringArrayArgument(StandardClassIds.Annotations.ParameterNames.suppressNames, session)
        val newSuppress = buildAnnotation {
            annotationTypeRef = StandardClassIds.Annotations.Suppress.createConeType(session).toFirResolvedTypeRef()
            argumentMapping = buildAnnotationArgumentMapping {
                source = declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
                mapping[StandardClassIds.Annotations.ParameterNames.suppressNames] = buildVarargArgumentsExpression {
                    coneTypeOrNull = session.builtinTypes.stringType.coneType.createArrayType()
                    coneElementTypeOrNull = session.builtinTypes.stringType.coneType
                    source = declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)

                    for (name in names) {
                        arguments += buildLiteralExpression(
                            declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated),
                            ConstantValueKind.String,
                            name,
                            setType = true
                        )
                    }

                    existingSuppressions?.forEach {
                        arguments += buildLiteralExpression(
                            declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated),
                            ConstantValueKind.String,
                            it, setType = true
                        )
                    }
                }
            }
            source = declaration.source?.fakeElement(KtFakeSourceElementKind.PluginGenerated)
        }

        val mutable = declaration.annotations.toMutableList()
        existingSuppress?.let(mutable::remove)
        mutable.add(newSuppress)

        declaration.replaceAnnotations(mutable)
    }

}