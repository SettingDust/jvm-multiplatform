package net.msrandom.stubs

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.tree.*
import kotlin.test.*

class MethodBodiesTest {
    private fun body() =
        CodeMethodNode(ACC_PUBLIC or ACC_STATIC, "m", "()V", null, null).apply {
            visitCode()
            visitInsn(RETURN)
            visitMaxs(1, 1)
        }

    private fun checkDifferent(change: (CodeMethodNode) -> Unit) {
        val a = body()
        val b = body().apply(change)
        assertFalse(MethodBodies.equal(a, b))
        assertFalse(MethodBodies.equal(b, a))
    }

    @Test fun presenceAndConservativeOmissions() {
        val absent = CodeMethodNode(ACC_PUBLIC, "m", "()V", null, null)
        val empty = CodeMethodNode(ACC_PUBLIC, "m", "()V", null, null).apply { visitCode() }
        assertFalse(MethodBodies.equal(absent, absent))
        assertFalse(MethodBodies.equal(absent, empty))
        assertTrue(MethodBodies.equal(empty, empty))
        checkDifferent { it.access = ACC_ABSTRACT }
        checkDifferent { it.access = ACC_NATIVE }
        checkDifferent { it.visitAttribute(object : Attribute("UnknownCode") {}) }
        checkDifferent { it.instructions.first.visibleTypeAnnotations = mutableListOf(TypeAnnotationNode(0, null, "LA;")) }
        checkDifferent {
            it.visibleLocalVariableAnnotations =
                mutableListOf(LocalVariableAnnotationNode(0, null, emptyArray(), emptyArray(), intArrayOf(), "LA;"))
        }
        checkDifferent { it.instructions.insert(LdcInsnNode(Any())) }
    }

    @Test fun debugAndFramesDoNotAffectShape() {
        val a = body()
        val b =
            body().apply {
                val label = LabelNode()
                instructions.insert(label)
                instructions.insert(label, LineNumberNode(999, label))
                instructions.insert(FrameNode(F_SAME, 0, null, 0, null))
                val end = LabelNode()
                instructions.add(end)
                localVariables = mutableListOf(LocalVariableNode("debug", "I", null, label, end, 4))
                maxStack = 10
                maxLocals = 20
            }
        assertTrue(MethodBodies.equal(a, b))
        assertTrue(MethodBodies.equal(b, a))
    }

    @Test fun operandsAndConstants() {
        val bootstrap = Handle(H_INVOKESTATIC, "Owner", "bsm", "()V", false)

        fun dynamic(value: Int) = ConstantDynamic("c", "I", bootstrap, ConstantDynamic("nested", "I", bootstrap, value))
        val pairs =
            listOf(
                InsnNode(ICONST_0) to InsnNode(ICONST_1),
                IntInsnNode(BIPUSH, 1) to IntInsnNode(BIPUSH, 2),
                VarInsnNode(ILOAD, 1) to VarInsnNode(ILOAD, 2),
                IincInsnNode(1, 1) to IincInsnNode(1, 2),
                TypeInsnNode(NEW, "A") to TypeInsnNode(NEW, "B"),
                FieldInsnNode(GETSTATIC, "A", "x", "I") to FieldInsnNode(GETSTATIC, "A", "y", "I"),
                MethodInsnNode(INVOKESTATIC, "A", "m", "()V", false) to MethodInsnNode(INVOKESTATIC, "A", "m", "()V", true),
                MultiANewArrayInsnNode("[[I", 1) to MultiANewArrayInsnNode("[[I", 2),
                LdcInsnNode(1) to LdcInsnNode(1L),
                LdcInsnNode(0.0f) to LdcInsnNode(-0.0f),
                LdcInsnNode(dynamic(1)) to LdcInsnNode(dynamic(2)),
                InvokeDynamicInsnNode("m", "()V", bootstrap, dynamic(1)) to InvokeDynamicInsnNode("m", "()V", bootstrap, dynamic(2)),
            )
        for ((left, right) in pairs) {
            val a = body().apply { instructions.add(left) } // unreachable instructions also count
            val b = body().apply { instructions.add(right) }
            assertFalse(MethodBodies.equal(a, b), left.javaClass.name)
            assertFalse(MethodBodies.equal(b, a), left.javaClass.name)
        }
        val a = body().apply { instructions.insert(LdcInsnNode(dynamic(1))) }
        val b = body().apply { instructions.insert(LdcInsnNode(dynamic(1))) }
        assertTrue(MethodBodies.equal(a, b))
    }

    private fun flow() =
        body().apply {
            instructions.clear()
            val start = LabelNode()
            val middle = LabelNode()
            val end = LabelNode()
            instructions.add(start)
            instructions.add(JumpInsnNode(GOTO, middle))
            instructions.add(middle)
            instructions.add(TableSwitchInsnNode(0, 0, end, middle))
            instructions.add(LookupSwitchInsnNode(end, intArrayOf(7), arrayOf(middle)))
            instructions.add(InsnNode(RETURN))
            instructions.add(end)
            tryCatchBlocks.add(TryCatchBlockNode(start, middle, middle, "A"))
            tryCatchBlocks.add(TryCatchBlockNode(start, end, middle, null))
        }

    @Test fun controlFlowAndOrderedHandlers() {
        val changes: List<(CodeMethodNode) -> Unit> =
            listOf(
                { (it.instructions[1] as JumpInsnNode).label = it.instructions.last as LabelNode },
                { (it.instructions[3] as TableSwitchInsnNode).labels[0] = it.instructions.last as LabelNode },
                { (it.instructions[4] as LookupSwitchInsnNode).dflt = it.instructions.first as LabelNode },
                { (it.instructions[4] as LookupSwitchInsnNode).keys[0] = 8 },
                { it.tryCatchBlocks[0].end = it.instructions.last as LabelNode },
                { it.tryCatchBlocks[0].type = "B" },
                { it.tryCatchBlocks.reverse() },
                { it.tryCatchBlocks[0].handler = it.instructions.first as LabelNode },
                { it.tryCatchBlocks[0].visibleTypeAnnotations = mutableListOf(TypeAnnotationNode(0, null, "LA;")) },
            )
        assertTrue(MethodBodies.equal(flow(), flow()))
        for (change in changes) {
            val a = flow()
            val b = flow().apply(change)
            assertFalse(MethodBodies.equal(a, b))
            assertFalse(MethodBodies.equal(b, a))
        }
        val redundant = flow().apply { instructions.insert(LabelNode()) }
        assertTrue(MethodBodies.equal(flow(), redundant))
    }
}
