package a.b.c

class A<T> : I {
    val a: Int = 1
}

interface I {
    fun hi(): String
}

fun consume(): Int = A<Int>().injected()

open class ImportedTarget {
    val seed: Int = 10
    var state: Int = 3
}

class PackageTarget

interface Added {
    fun added(): Int
}

fun consumeAdded(): Int = ImportedTarget().added()
