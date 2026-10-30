package a.b.c

import net.msrandom.classextensions.ClassExtension
import net.msrandom.classextensions.ExtensionInject
import net.msrandom.classextensions.ExtensionShadow

@ClassExtension(a.b.c.A::class)
class AExtension : I {
    @ExtensionShadow
    val a: Int = TODO()

    @ExtensionInject
    private var c = 5

    @ExtensionInject
    var mutable = 7

    @ExtensionInject
    override fun hi(): String = "hi"

    @ExtensionInject
    fun injected(): Int {
        val local = 36
        return a + c + local
    }
}

@ClassExtension(PackageTarget::class)
class PackageExtension {
    @ExtensionInject
    fun packageResolved(): Int = 23
}
