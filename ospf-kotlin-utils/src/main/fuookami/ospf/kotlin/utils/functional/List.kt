package fuookami.ospf.kotlin.utils.functional

typealias List2<T> = List<List<T>>
typealias MutableList2<T> = MutableList<MutableList<T>>
typealias List3<T> = List<List<List<T>>>
typealias MutableList3<T> = MutableList<MutableList<MutableList<T>>>

operator fun <T> List<List<T>>.get(i: Int, j: Int): T {
    return this[i][j]
}

fun <T> List<List<T>>.getOrNull(i: Int, j: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)
}

operator fun <T> List<MutableList<T>>.set(i: Int, j: Int, value: T) {
    this[i][j] = value
}

operator fun <T> List3<T>.get(i: Int, j: Int, k: Int): T {
    return this[i][j][k]
}

fun <T> List3<T>.getOrNull(i: Int, j: Int, k: Int): T? {
    return this.getOrNull(i)?.getOrNull(j)?.getOrNull(k)
}

operator fun <T> List<List<MutableList<T>>>.set(i: Int, j: Int, k: Int, value: T) {
    this[i][j][k] = value
}

data class ListFindResult<T>(
    val self: MutableList<T>,
    val result: T?
) {
    fun add(default: () -> T): T {
        return if (result != null) {
            result
        } else {
            val newValue = default()
            self.add(newValue)
            newValue
        }
    }

    fun default(default: () -> T): T {
        return result ?: default()
    }
}

fun <T> MutableList<T>.findOr(predicate: (T) -> Boolean): ListFindResult<T> {
    return ListFindResult(this, this.find(predicate))
}
