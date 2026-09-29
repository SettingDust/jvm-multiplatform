package net.msrandom.stub.fir

import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataKey
import org.jetbrains.kotlin.fir.declarations.FirDeclarationDataRegistry

// FIR metadata carries this declaration-local identity to IR without a user annotation.
private object GeneratedStubKey : FirDeclarationDataKey()

internal var FirDeclaration.isGeneratedStub: Boolean? by FirDeclarationDataRegistry.data(GeneratedStubKey)
