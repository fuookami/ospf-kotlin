package fuookami.ospf.kotlin.utils.functional

inline fun <T, U> Iterable<U>.minMaxOfWith(
    comparator: kotlin.Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    val iterator = this.iterator()
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}

inline fun <T, U> Iterable<U>.minMaxOfWithOrNull(
    comparator: kotlin.Comparator<T>,
    crossinline extractor: Extractor<T, U>
): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}