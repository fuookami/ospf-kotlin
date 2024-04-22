package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

fun <T : Ord<T>> min(lhs: T, rhs: T): T = if (lhs < rhs) lhs else rhs

fun <T : Ord<T>> min(lhs: T, vararg rhs: T): T {
    var min = lhs
    for (e in rhs) {
        if (e leq min) {
            min = e
        }
    }
    return min
}

inline fun <T : Ord<T>, U> minOf(
    lhs: U, vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): T {
    var min = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v leq min) {
            min = v
        }
    }
    return min
}

fun <T : Ord<T>> max(lhs: T, rhs: T): T = if (lhs > rhs) lhs else rhs

fun <T : Ord<T>> max(lhs: T, vararg rhs: T): T {
    var max = lhs
    for (e in rhs) {
        if (e geq max) {
            max = e
        }
    }
    return max
}

inline fun <T : Ord<T>, U> maxOf(
    lhs: U,
    vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): T {
    var max = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v gr max) {
            max = v
        }
    }
    return max
}

fun <T : Ord<T>> minmax(lhs: T, rhs: T): Pair<T, T> = Pair(min(lhs, rhs), max(lhs, rhs))

fun <T : Ord<T>> minMax(lhs: T, vararg rhs: T): Pair<T, T> {
    var min = lhs
    var max = lhs
    for (e in rhs) {
        if (e ls min) {
            min = e
        }
        if (e gr max) {
            max = e
        }
    }
    return Pair(min, max)
}

inline fun <T : Ord<T>, U> minMaxOf(
    lhs: U,
    vararg rhs: U,
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    var min = extractor(lhs)
    var max = min
    for (e in rhs) {
        val v = extractor(e)
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T : Ord<T>> Iterable<T>.minMax(): Pair<T, T> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        throw NoSuchElementException()
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T : Ord<T>> Iterable<T>.minMaxOrNull(): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }

    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v ls min) {
            min = v
        }
        if (v gr max) {
            max = v
        }
    }
    return Pair(min, max)
}

inline fun <T : Ord<T>, U> Iterable<U>.minMaxBy(
    crossinline extractor: Extractor<T, U>
): Pair<U, U> {
    val iterator = this.iterator()
    var minE = iterator().next()
    var maxE = minE
    var min = extractor(minE)
    var max = min
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = extractor(e)
        if (v leq min) {
            minE = e
            min = v
        }
        if (v geq max) {
            maxE = e
            max = v
        }
    }
    return Pair(minE, maxE)
}

inline fun <T : Ord<T>, U> Iterable<U>.minMaxByOrNull(
    crossinline extractor: Extractor<T, U>
): Pair<U, U>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var minE = iterator().next()
    var maxE = minE
    var min = extractor(minE)
    var max = min
    while (iterator.hasNext()) {
        val e = iterator.next()
        val v = extractor(e)
        if (v leq min) {
            minE = e
            min = v
        }
        if (v geq max) {
            maxE = e
            max = v
        }
    }
    return Pair(minE, maxE)
}

inline fun <T : Ord<T>, U> Iterable<U>.minMaxOf(
    crossinline extractor: Extractor<T, U>
): Pair<T, T> {
    val iterator = this.iterator()
    var min = extractor(iterator().next())
    var max = min
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

inline fun <T : Ord<T>, U> Iterable<U>.minMaxOfOrNull(
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
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T> Iterable<T>.minMaxWith(comparator: kotlin.Comparator<T>): Pair<T, T> {
    val iterator = this.iterator()
    var min = iterator().next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T> Iterable<T>.minMaxWithOrNull(comparator: kotlin.Comparator<T>): Pair<T, T>? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var min = iterator().next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (comparator.compare(v, min) < 0) {
            min = v
        }
        if (comparator.compare(v, max) > 0) {
            max = v
        }
    }
    return Pair(min, max)
}

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
