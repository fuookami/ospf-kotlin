package fuookami.ospf.kotlin.utils.functional

fun <T : Comparable<T>> Iterable<T>.minMax(): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T : Comparable<T>> Iterable<T>.minMaxOrNull(): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxBy(crossinline extractor: Extractor<R, T>): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
    }

    val v = iterator.next()
    val r = extractor(v)
    var min = Pair(v, r)
    var max = Pair(v, r)
    while (iterator.hasNext()) {
        val v0 = iterator.next()
        val r0 = extractor(v0)
        if (r0 < min.second) {
            min = Pair(v0, r0)
        }
        if (r0 > max.second) {
            max = Pair(v0, r0)
        }
    }
    return Pair(min.first, max.first)
}

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxByOrNull(crossinline extractor: Extractor<R, T>): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    val v = iterator.next()
    val r = extractor(v)
    var min = Pair(v, r)
    var max = Pair(v, r)
    while (iterator.hasNext()) {
        val v0 = iterator.next()
        val r0 = extractor(v0)
        if (r0 < min.second) {
            min = Pair(v0, r0)
        }
        if (r0 > max.second) {
            max = Pair(v0, r0)
        }
    }
    return Pair(min.first, max.first)
}

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOf(crossinline extractor: Extractor<R, T>): Pair<R, R> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
    }

    var min = extractor(iterator.next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}

inline fun <T, R : Comparable<R>> Iterable<T>.minMaxOfOrNull(crossinline extractor: Extractor<R, T>): Pair<R, R>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = extractor(iterator.next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v < min) {
            min = v
        }
        if (v > max) {
            max = v
        }
    }
    return Pair(min, max)
}
