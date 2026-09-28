package net.msrandom.stubs

import org.objectweb.asm.*
import org.objectweb.asm.tree.*

/** Records Code presence independently of its executable instruction count. */
internal class CodeMethodNode(
    access: Int,
    name: String,
    descriptor: String,
    signature: String?,
    exceptions: Array<out String>?,
) : MethodNode(Opcodes.ASM9, access, name, descriptor, signature, exceptions) {
    var hasCode = false
        private set

    override fun visitCode() {
        hasCode = true
        super.visitCode()
    }
}

internal object MethodBodies {
    private fun constant(value: Any?): Any? {
        return when (value) {
            is Int, is Long, is String, is Type, is Handle -> {
                value
            }

            is Float -> {
                listOf("float", value.toRawBits())
            }

            is Double -> {
                listOf("double", value.toRawBits())
            }

            is ConstantDynamic -> {
                val arguments =
                    (0 until value.bootstrapMethodArgumentCount).map {
                        constant(value.getBootstrapMethodArgument(it)) ?: return null
                    }
                listOf("dynamic", value.name, value.descriptor, value.bootstrapMethod, arguments)
            }

            else -> {
                null
            }
        }
    }

    private fun shape(method: CodeMethodNode): List<Any?>? {
        if (!method.hasCode || method.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_NATIVE) != 0) return null
        // ponytail: unknown attributes and Code type annotations omit Code; add comparison when needed.
        if (!method.attrs.isNullOrEmpty() || !method.visibleLocalVariableAnnotations.isNullOrEmpty() ||
            !method.invisibleLocalVariableAnnotations.isNullOrEmpty()
        ) {
            return null
        }
        val positions = mutableMapOf<LabelNode, Int>()
        var position = 0
        for (instruction in method.instructions) {
            when (instruction) {
                is LabelNode -> positions[instruction] = position
                is FrameNode, is LineNumberNode -> Unit
                else -> position++
            }
        }

        fun target(label: LabelNode): Int = positions[label] ?: throw IllegalArgumentException("Unresolved Code label")
        return try {
            val instructions =
                method.instructions.mapNotNull { instruction ->
                    if (!instruction.visibleTypeAnnotations.isNullOrEmpty() ||
                        !instruction.invisibleTypeAnnotations.isNullOrEmpty()
                    ) {
                        return null
                    }
                    val operands: Any? =
                        when (instruction) {
                            is LabelNode, is FrameNode, is LineNumberNode -> {
                                return@mapNotNull null
                            }

                            is InsnNode -> {
                                emptyList<Any>()
                            }

                            is IntInsnNode -> {
                                instruction.operand
                            }

                            is VarInsnNode -> {
                                instruction.`var`
                            }

                            is TypeInsnNode -> {
                                instruction.desc
                            }

                            is FieldInsnNode -> {
                                listOf(instruction.owner, instruction.name, instruction.desc)
                            }

                            is MethodInsnNode -> {
                                listOf(instruction.owner, instruction.name, instruction.desc, instruction.itf)
                            }

                            is InvokeDynamicInsnNode -> {
                                listOf(
                                    instruction.name,
                                    instruction.desc,
                                    instruction.bsm,
                                    instruction.bsmArgs.map { constant(it) ?: return null },
                                )
                            }

                            is JumpInsnNode -> {
                                target(instruction.label)
                            }

                            is LdcInsnNode -> {
                                constant(instruction.cst) ?: return null
                            }

                            is IincInsnNode -> {
                                listOf(instruction.`var`, instruction.incr)
                            }

                            is TableSwitchInsnNode -> {
                                listOf(instruction.min, instruction.max, target(instruction.dflt), instruction.labels.map(::target))
                            }

                            is LookupSwitchInsnNode -> {
                                listOf(instruction.keys, target(instruction.dflt), instruction.labels.map(::target))
                            }

                            is MultiANewArrayInsnNode -> {
                                listOf(instruction.desc, instruction.dims)
                            }

                            else -> {
                                return null
                            }
                        }
                    listOf(instruction.type, instruction.opcode, operands)
                }
            val handlers =
                method.tryCatchBlocks.map {
                    if (!it.visibleTypeAnnotations.isNullOrEmpty() || !it.invisibleTypeAnnotations.isNullOrEmpty()) return null
                    listOf(target(it.start), target(it.end), target(it.handler), it.type)
                }
            listOf(instructions, handlers)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun equal(
        first: CodeMethodNode,
        second: CodeMethodNode,
    ): Boolean {
        val shape = shape(first) ?: return false
        return shape == shape(second)
    }

    fun preserve(
        target: MethodNode,
        originals: List<CodeMethodNode>,
    ) {
        val source = originals.first()
        if (target.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_NATIVE) != 0 ||
            originals.any { !equal(source, it) }
        ) {
            return
        }
        // Move one coherent Code tree; declaration annotations/exceptions remain the intersection's.
        target.instructions = source.instructions
        target.tryCatchBlocks = source.tryCatchBlocks
        target.localVariables = source.localVariables
        target.visibleLocalVariableAnnotations = source.visibleLocalVariableAnnotations
        target.invisibleLocalVariableAnnotations = source.invisibleLocalVariableAnnotations
        target.maxStack = source.maxStack
        target.maxLocals = source.maxLocals
    }
}
