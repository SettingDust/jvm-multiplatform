package net.msrandom.stub.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.*
import org.jetbrains.kotlin.fir.resolve.transformers.*
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatchingContextImpl
import org.jetbrains.kotlin.fir.scopes.*
import org.jetbrains.kotlin.fir.scopes.impl.*
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.mpp.*
import org.jetbrains.kotlin.resolve.calls.mpp.AbstractExpectActualMatcher
import org.jetbrains.kotlin.resolve.calls.mpp.ExpectActualMatchingContext
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualMatchingCompatibility
import org.jetbrains.kotlin.types.model.KotlinTypeMarker

/** Resolves detached headers only; neither registers future FIR nor advances live declarations. */
@OptIn(SymbolInternals::class, DirectDeclarationsAccess::class)
internal class CallableHeaders(
    private val inputs: CallableInputs,
) {
    private class Unavailable(
        message: String,
    ) : RuntimeException(message)

    private fun cone(ref: FirTypeRef): ConeKotlinType =
        (ref as? FirResolvedTypeRef)?.takeUnless { it is FirErrorTypeRef }?.coneType
            ?: throw Unavailable("unresolved callable header")

    fun matches(
        expect: FirCallableDeclaration,
        candidate: CallableInputs.Declaration,
        session: FirSession,
    ): Boolean? {
        try {
            val baseModule = inputs.source(expect, session)?.module ?: throw Unavailable("expect source identity")
            val detached = CallableHeaderFiles(inputs, baseModule, session)
            detached.session(candidate.source.module)
            val file = detached.files.getValue(candidate.source)
            val actual =
                file.declarations.filterIsInstance<FirCallableDeclaration>().singleOrNull { it.source?.startOffset == candidate.start }
                    ?: throw Unavailable("detached declaration identity")
            if ((expect is FirNamedFunction) != (actual is FirNamedFunction)) return false
            if ((expect.receiverParameter == null) != (actual.receiverParameter == null)) return false
            if ((expect as? FirNamedFunction)?.valueParameters?.size != (actual as? FirNamedFunction)?.valueParameters?.size) return false
            if (expect.typeParameters.size != actual.typeParameters.size) return false
            val aliases =
                detached.files.values
                    .flatMap { owner ->
                        owner.declarations.filterIsInstance<FirTypeAlias>().map { it to owner }
                    }.toMap()
            val importingScopes =
                detached.files.values.associateWith { owner ->
                    val ownerSession = owner.moduleData.session
                    FirImportResolveTransformer(ownerSession).transformFile(owner, null)
                    createImportingScopes(owner, ownerSession, ScopeSession(), useCaching = false).asReversed()
                }
            val scopes = listOf(FirMemberTypeParameterScope(actual)) + importingScopes.getValue(file)
            lateinit var resolveAlias: (FirTypeAlias) -> ConeClassLikeType
            val supplier =
                object : SupertypeSupplier() {
                    override fun forClass(
                        firClass: FirClass,
                        useSiteSession: FirSession,
                    ): List<ConeClassLikeType> = throw Unavailable("future source supertype")

                    override fun expansionForTypeAlias(
                        typeAlias: FirTypeAlias,
                        useSiteSession: FirSession,
                    ): ConeClassLikeType? =
                        if (typeAlias in aliases) {
                            resolveAlias(typeAlias)
                        } else {
                            (typeAlias.expandedTypeRef as? FirResolvedTypeRef)?.coneType as? ConeClassLikeType
                                ?: throw Unavailable("future source alias expansion")
                        }
                }
            // Expand through our read-only supplier, not fullyExpandedType's default lazy phase advancement.
            val config = TypeResolutionConfiguration(scopes, emptyList(), file, actual)

            fun resolveIn(
                ref: FirTypeRef,
                configuration: TypeResolutionConfiguration,
            ): FirResolvedTypeRef {
                val ownerSession = configuration.useSiteFile!!.moduleData.session
                val resolver = FirSpecificTypeResolverTransformer(ownerSession, supertypeSupplier = supplier, expandTypeAliases = false)
                val resolved = ref.transform<FirTypeRef, TypeResolutionConfiguration>(resolver, configuration)
                val expanded = cone(resolved).fullyExpandedType(ownerSession) { supplier.expansionForTypeAlias(it, ownerSession) }
                return (resolved as FirResolvedTypeRef).withReplacedConeType(expanded)
            }
            val expanding = mutableSetOf<FirTypeAlias>()
            resolveAlias = { alias ->
                if (!expanding.add(alias)) throw Unavailable("recursive detached alias")
                try {
                    val owner = aliases.getValue(alias)
                    val resolved =
                        resolveIn(
                            alias.expandedTypeRef,
                            TypeResolutionConfiguration(
                                listOf(FirMemberTypeParameterScope(alias)) + importingScopes.getValue(owner),
                                emptyList(),
                                owner,
                                alias,
                            ),
                        )
                    alias.replaceExpandedTypeRef(resolved)
                    cone(resolved) as? ConeClassLikeType ?: throw Unavailable("non-class alias expansion")
                } finally {
                    expanding.remove(alias)
                }
            }

            fun resolve(ref: FirTypeRef): FirResolvedTypeRef = resolveIn(ref, config)
            actual.receiverParameter?.let { it.replaceTypeRef(resolve(it.typeRef)) }
            (actual as? FirNamedFunction)?.valueParameters?.forEach { it.replaceReturnTypeRef(resolve(it.returnTypeRef)) }
            actual.typeParameters.forEach { p ->
                p.symbol.fir.replaceBounds(
                    p.symbol.fir.bounds
                        .map(::resolve),
                )
            }
            // Read only already resolved expect refs. Unknown is not evidence of absence.
            expect.receiverParameter?.let { cone(it.typeRef) }
            (expect as? FirNamedFunction)?.valueParameters?.forEach { cone(it.returnTypeRef) }
            expect.typeParameters.forEach { p ->
                p.symbol.fir.bounds
                    .forEach(::cone)
            }
            val native = FirExpectActualMatchingContextImpl.Factory.create(session, ScopeSession(), false)
            val context =
                object : ExpectActualMatchingContext<FirBasedSymbol<*>> by native {
                    override val CallableSymbolMarker.extensionReceiverType: KotlinTypeMarker?
                        get() =
                            (this as FirCallableSymbol<*>)
                                .fir.receiverParameter
                                ?.typeRef
                                ?.let(::cone)
                    override val CallableSymbolMarker.returnType: KotlinTypeMarker
                        get() = cone((this as FirCallableSymbol<*>).fir.returnTypeRef)
                    override val TypeParameterSymbolMarker.bounds: List<KotlinTypeMarker>
                        get() = (this as FirTypeParameterSymbol).fir.bounds.map(::cone)
                    override val CallableSymbolMarker.isExpect: Boolean
                        get() = (this as FirCallableSymbol<*>).fir.status.isExpect
                }
            return AbstractExpectActualMatcher.getCallablesMatchingCompatibility(expect.symbol, actual.symbol, null, null, context) ==
                ExpectActualMatchingCompatibility.MatchedSuccessfully
        } catch (_: Unavailable) {
            return null
        }
    }
}
