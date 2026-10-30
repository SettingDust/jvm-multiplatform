package a.b.extensions

import a.b.c.Added
import a.b.c.ImportedTarget
import net.msrandom.classextensions.ClassExtension
import net.msrandom.classextensions.ExtensionInject
import net.msrandom.classextensions.ExtensionShadow

@ClassExtension(ImportedTarget::class)
open class ImportedExtension : Added {
    @ExtensionShadow
    val seed: Int = TODO()

    @ExtensionShadow
    var state: Int = TODO()

    @ExtensionShadow
    fun documentaryOnly(): Int = -1

    fun unmarked(): Int = -2

    val unmarkedProperty: Int = -3

    @ExtensionInject
    override fun added(): Int = seed

    @ExtensionInject
    val readOnly: Int = 0
        get() = field + seed + state

    @ExtensionInject
    var delegated: Int = 0
        get() = state
        set(value) {
            field = value
            state = value + seed
        }

    @ExtensionInject
    open fun echo(value: String): String = value + seed

    @ExtensionInject
    protected fun protectedValue(): Int = seed

    @ExtensionInject
    fun String.withTarget(): String = this + seed
}
