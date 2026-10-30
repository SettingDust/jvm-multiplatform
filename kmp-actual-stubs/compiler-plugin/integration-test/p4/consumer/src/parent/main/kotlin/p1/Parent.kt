package p1
expect fun value(): Int
expect fun leafValue(): Int
expect fun missing(): Int
fun callValue(): Int = value()
fun callLeaf(): Int = leafValue()
fun callMissing(): Int = missing()
