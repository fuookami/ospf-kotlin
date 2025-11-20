package fuookami.ospf.kotlin.utils.multi_array

import fuookami.ospf.kotlin.utils.functional.*

operator fun <T> List2<T>.get(i: AllDummyIndex, j: Int): Iterable<T> {
    return this.mapNotNull { it.getOrNull(j) }
}

operator fun <T> List2<T>.get(i: Int, j: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i) ?: emptyList()
}

operator fun <T> List2<T>.get(i: AllDummyIndex, j: AllDummyIndex): Iterable<T> {
    return this.flatten()
}

operator fun <T> List3<T>.get(i: AllDummyIndex, j: Int, k: Int): Iterable<T> {
    return this.map { it[j, k] }
}

operator fun <T> List3<T>.get(i: Int, j: AllDummyIndex, k: Int): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: Int, j: Int, k: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: AllDummyIndex, j: AllDummyIndex, k: Int): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List3<T>.get(i: AllDummyIndex, j: Int, k: AllDummyIndex): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List3<T>.get(i: Int, j: AllDummyIndex, k: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List3<T>.get(i: AllDummyIndex, j: AllDummyIndex, k: AllDummyIndex): Iterable<T> {
    return this.flatMap { it[j, k] }
}
