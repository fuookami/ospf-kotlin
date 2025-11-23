package fuookami.ospf.kotlin.utils.multi_array

operator fun <T> List<List<T>>.get(i: Int, j: Int): T {
    return this[i][j]
}

fun <T> List<List<T>>.getOrNull(i: Int, j: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)
}

operator fun <T> List<List<T>>.get(i: AllDummyIndex, j: Int): Iterable<T> {
    return this.mapNotNull { it.getOrNull(j) }
}

operator fun <T> List<List<T>>.get(i: Int, j: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i) ?: emptyList()
}

operator fun <T> List<List<T>>.get(i: AllDummyIndex, j: AllDummyIndex): Iterable<T> {
    return this.flatten()
}

operator fun <T> List<MutableList<T>>.set(i: Int, j: Int, value: T) {
    this[i][j] = value
}

operator fun <T> List<List<List<T>>>.get(i: Int, j: Int, k: Int): T {
    return this[i][j][k]
}

fun <T> List<List<List<T>>>.getOrNull(i: Int, j: Int, k: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)?.getOrNull(k)
}

operator fun <T> List<List<List<T>>>.get(i: AllDummyIndex, j: Int, k: Int): Iterable<T> {
    return this.map { it[j, k] }
}

operator fun <T> List<List<List<T>>>.get(i: Int, j: AllDummyIndex, k: Int): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List<List<List<T>>>.get(i: Int, j: Int, k: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List<List<List<T>>>.get(i: AllDummyIndex, j: AllDummyIndex, k: Int): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List<List<List<T>>>.get(i: AllDummyIndex, j: Int, k: AllDummyIndex): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List<List<List<T>>>.get(i: Int, j: AllDummyIndex, k: AllDummyIndex): Iterable<T> {
    return this.getOrNull(i)?.get(j, k) ?: emptyList()
}

operator fun <T> List<List<List<T>>>.get(i: AllDummyIndex, j: AllDummyIndex, k: AllDummyIndex): Iterable<T> {
    return this.flatMap { it[j, k] }
}

operator fun <T> List<List<MutableList<T>>>.set(i: Int, j: Int, k: Int, value: T) {
    this[i][j][k] = value
}
