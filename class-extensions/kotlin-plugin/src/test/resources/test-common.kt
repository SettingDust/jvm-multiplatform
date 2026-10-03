package a.b.c

class A<T> : I {
    val a: Int = 1
}

interface I {
    fun hi(): String
}

fun consume(): Int = A<Int>().injected()
