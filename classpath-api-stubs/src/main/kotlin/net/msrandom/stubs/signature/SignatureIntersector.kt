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

    // Validate against the emitted declaration, not merely against another Signature.
    private fun scope(parameters: List<FormalTypeParameter>, outer: Map<String, FormalTypeParameter>) =
        (outer + parameters.associateBy { it.name }).also {
            require(parameters.map { it.name }.distinct().size == parameters.size)
            parameters.forEach { parameter ->
                val bounds = listOfNotNull(parameter.classBound) + parameter.interfaceBounds
                require(bounds.isNotEmpty())
                bounds.forEach { bound ->
                    require(bound is TypeSignature.Reference && bound !is TypeSignature.Reference.Array)
                    erase(bound, it)
                }
            }
        }

    private fun erase(type: TypeSignature, variables: Map<String, FormalTypeParameter>, visiting: Set<String> = emptySet()): String =
        when (type) {
            is TypeSignature.Primitive -> {
                require(type.identifier in "BCDFIJSZV")
                type.identifier.toString()
            }
            is TypeSignature.Reference.Array -> "[" + erase(type.typeSignature, variables, visiting).also { require(it != "V") }
            is TypeSignature.Reference.TypeVariable -> {
                require(type.name !in visiting)
                val parameter = requireNotNull(variables[type.name])
                erase(parameter.classBound ?: parameter.interfaceBounds.first(), variables, visiting + type.name)
            }
            is TypeSignature.Reference.Class -> {
                val segments = listOf(type.base) + type.innerClasses
                segments.forEach { segment ->
                    require(segment.name.isNotEmpty())
                    segment.typeArguments.filterIsInstance<TypeArgument.Bounded>().forEach { argument ->
                        require(argument.type is TypeSignature.Reference)
                        checkReferences(argument.type, variables)
                    }
                }
                "L" + segments.joinToString("$") { it.name } + ";"
            }
        }

    // Checking references separately permits legal recursive bounds such as T : Comparable<T>.
    private fun checkReferences(type: TypeSignature, variables: Map<String, FormalTypeParameter>) {
        when (type) {
            is TypeSignature.Reference.TypeVariable -> require(type.name in variables)
            is TypeSignature.Reference.Array -> checkReferences(type.typeSignature, variables)
            is TypeSignature.Reference.Class -> (listOf(type.base) + type.innerClasses).forEach { segment ->
                segment.typeArguments.filterIsInstance<TypeArgument.Bounded>().forEach { checkReferences(it.type, variables) }
            }
            is TypeSignature.Primitive -> Unit
        }
    }

    fun validClass(signature: String?, superName: String, interfaces: List<String>): String? =
        signature?.takeIf {
            runCatching {
                val parsed = parseClassSignature(it)
                require(write(parsed) == it)
                val variables = scope(parsed.typeParameters, emptyMap())
                require(erase(parsed.superClass, variables) == "L$superName;")
                require(parsed.superInterfaces.map { type -> erase(type, variables) } == interfaces.map { name -> "L$name;" })
            }.isSuccess
        }

    fun validMember(signature: String?, descriptor: String, classSignature: String?, method: Boolean): String? =
        signature?.takeIf {
            runCatching {
                val outer = classSignature?.let { value -> scope(parseClassSignature(value).typeParameters, emptyMap()) } ?: emptyMap()
                if (method) {
                    val parsed = parseMethodSignature(it)
                    require(write(parsed) == it)
                    val variables = scope(parsed.typeParameters, outer)
                    val parameters = parsed.parameterTypes.map { type -> erase(type, variables).also { value -> require(value != "V") } }
                    require("(" + parameters.joinToString("") + ")" + erase(parsed.returnType, variables) == descriptor)
                    parsed.exceptionTypes.forEach { type ->
                        require(type !is TypeSignature.Reference.Array)
                        erase(type, variables)
                    }
                } else {
                    val parsed = parseTypeSignature(it)
                    require(parsed is TypeSignature.Reference && write(parsed) == it)
                    require(erase(parsed, outer) == descriptor)
                }
            }.isSuccess
        }

    private fun write(signature: Signature): String {
        val writer = SignatureWriter()
        signature.accept(writer)
        return writer.toString()
    }
}
