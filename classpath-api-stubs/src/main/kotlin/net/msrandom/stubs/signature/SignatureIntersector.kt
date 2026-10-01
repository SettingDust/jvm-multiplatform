package net.msrandom.stubs.signature

import org.objectweb.asm.signature.SignatureWriter

internal object SignatureIntersector {
    private fun sameType(
        a: TypeSignature,
        b: TypeSignature,
        variables: Map<String, String> = emptyMap(),
    ): Boolean =
        when {
            a is TypeSignature.Primitive && b is TypeSignature.Primitive -> {
                a.identifier == b.identifier
            }

            a is TypeSignature.Reference.TypeVariable && b is TypeSignature.Reference.TypeVariable -> {
                variables[a.name]?.let { it == b.name } ?: (a.name == b.name)
            }

            a is TypeSignature.Reference.Array && b is TypeSignature.Reference.Array -> {
                sameType(a.typeSignature, b.typeSignature, variables)
            }

            a is TypeSignature.Reference.Class && b is TypeSignature.Reference.Class -> {
                a.base.name == b.base.name && sameArguments(a.base.typeArguments, b.base.typeArguments, variables) &&
                    a.innerClasses.size == b.innerClasses.size &&
                    a.innerClasses.zip(b.innerClasses).all { (left, right) ->
                        left.name == right.name && sameArguments(left.typeArguments, right.typeArguments, variables)
                    }
            }

            else -> {
                false
            }
        }

    private fun sameArguments(
        a: List<TypeArgument>,
        b: List<TypeArgument>,
        variables: Map<String, String>,
    ): Boolean =
        a.size == b.size &&
            a.zip(b).all { (left, right) ->
                when {
                    left === TypeArgument.Unbounded && right === TypeArgument.Unbounded -> {
                        true
                    }

                    left is TypeArgument.Bounded && right is TypeArgument.Bounded -> {
                        left.variance == right.variance && sameType(left.type, right.type, variables)
                    }

                    else -> {
                        false
                    }
                }
            }

    private fun sameClass(
        a: ClassSignature,
        b: ClassSignature,
    ): Boolean {
        if (a.typeParameters.size != b.typeParameters.size || a.superInterfaces.size != b.superInterfaces.size) return false
        val variables = a.typeParameters.mapIndexed { index, parameter -> parameter.name to b.typeParameters[index].name }.toMap()
        if (a.typeParameters.indices.any { index ->
                val left = a.typeParameters[index]
                val right = b.typeParameters[index]
                left.classBound != null != (right.classBound != null) ||
                    (left.classBound != null && !sameType(left.classBound, right.classBound!!, variables)) ||
                    left.interfaceBounds.size != right.interfaceBounds.size ||
                    left.interfaceBounds.zip(right.interfaceBounds).any { (x, y) -> !sameType(x, y, variables) }
            }
        ) {
            return false
        }
        if (!sameType(a.superClass, b.superClass, variables)) return false
        return a.superInterfaces.sortedBy { it.base.name }.zip(b.superInterfaces.sortedBy { it.base.name }).all { (x, y) ->
            sameType(x, y, variables)
        }
    }

    private fun sameMethod(
        a: MethodSignature,
        b: MethodSignature,
    ): Boolean {
        if (a.typeParameters.size != b.typeParameters.size || a.parameterTypes.size != b.parameterTypes.size ||
            a.exceptionTypes.size != b.exceptionTypes.size
        ) {
            return false
        }
        val variables = a.typeParameters.mapIndexed { index, parameter -> parameter.name to b.typeParameters[index].name }.toMap()
        return a.typeParameters.indices.all { index ->
            val left = a.typeParameters[index]
            val right = b.typeParameters[index]
            left.classBound != null == (right.classBound != null) &&
                (left.classBound == null || sameType(left.classBound, right.classBound!!, variables)) &&
                left.interfaceBounds.size == right.interfaceBounds.size &&
                left.interfaceBounds.zip(right.interfaceBounds).all { (x, y) -> sameType(x, y, variables) }
        } && a.parameterTypes.zip(b.parameterTypes).all { (x, y) -> sameType(x, y, variables) } &&
            sameType(a.returnType, b.returnType, variables) &&
            a.exceptionTypes.zip(b.exceptionTypes).all { (x, y) -> sameType(x, y, variables) }
    }

    fun intersectClassSignatures(signatures: List<String?>): String? {
        if (signatures.isEmpty() || signatures.any { it == null }) return null
        val parsed = runCatching { signatures.map { parseClassSignature(it!!) } }.getOrNull() ?: return null
        return parsed.first().takeIf { parsed.drop(1).all { other -> sameClass(it, other) } }?.let(::write)
    }

    fun intersectClassSignatures(
        a: String?,
        b: String?,
    ): String? = intersectClassSignatures(listOf(a, b))

    fun intersectMethodSignatures(signatures: List<String?>): String? {
        if (signatures.isEmpty() || signatures.any { it == null }) return null
        val parsed = runCatching { signatures.map { parseMethodSignature(it!!) } }.getOrNull() ?: return null
        return parsed.first().takeIf { parsed.drop(1).all { other -> sameMethod(it, other) } }?.let(::write)
    }

    fun intersectMethodSignature(
        a: String?,
        b: String?,
    ): String? = intersectMethodSignatures(listOf(a, b))

    fun intersectFieldSignatures(signatures: List<String?>): String? {
        if (signatures.isEmpty() || signatures.any { it == null }) return null
        val parsed = runCatching { signatures.map { parseTypeSignature(it!!) } }.getOrNull() ?: return null
        return parsed.first().takeIf { parsed.drop(1).all { other -> sameType(it, other) } }?.let(::write)
    }

    fun intersectFieldSignature(
        a: String?,
        b: String?,
    ): String? = intersectFieldSignatures(listOf(a, b))

    private fun write(signature: Signature): String {
        val writer = SignatureWriter()
        signature.accept(writer)
        return writer.toString()
    }
}
