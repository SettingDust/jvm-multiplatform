package net.msrandom.classextensions.kotlin.plugin.copiers

import net.msrandom.classextensions.kotlin.plugin.ClassExtensionFirDeclarations.Key
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.resolvePhase
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol

// Kotlin 2.2 no longer generates a constructor copy builder. Preserve both constructor kinds.
internal fun buildConstructorCopy(original: FirConstructor, init: FirAbstractConstructorBuilder.() -> Unit): FirConstructor {
    val builder = if (original.isPrimary) FirPrimaryConstructorBuilder() else FirConstructorBuilder()
    return builder.apply {
        source = original.source
        resolvePhase = original.resolvePhase
        moduleData = original.moduleData
        origin = original.origin
        attributes = original.attributes.copy()
        typeParameters.addAll(original.typeParameters)
        status = original.status
        returnTypeRef = original.returnTypeRef
        receiverParameter = original.receiverParameter
        deprecationsProvider = original.deprecationsProvider
        containerSource = original.containerSource
        dispatchReceiverType = original.dispatchReceiverType
        contextParameters.addAll(original.contextParameters)
        valueParameters.addAll(original.valueParameters)
        contractDescription = original.contractDescription
        annotations.addAll(original.annotations)
        symbol = original.symbol
        delegatedConstructor = original.delegatedConstructor
        body = original.body
        init()
    }.build()
}

fun copyConstructor(original: FirConstructor, ownerSymbol: FirClassSymbol<*>): FirConstructor {
    val constructorCopy = buildConstructorCopy(original) {
        symbol = FirConstructorSymbol(ownerSymbol.classId)
        origin = Key.origin
        this.moduleData = ownerSymbol.moduleData
    }

    return constructorCopy.transformChildren(FirFunctionCopier(original.symbol, ownerSymbol), Unit) as FirConstructor
}
