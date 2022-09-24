package fuookami.ospf.kotlin.utils.math.ordinary

import fuookami.ospf.kotlin.utils.operator.Ord
import fuookami.ospf.kotlin.utils.functional.Extractor

fun <T : Ord<T>> min(lhs: T, rhs: T): T = if (lhs < rhs) lhs else rhs

fun <T : Ord<T>> minOf(lhs: T, vararg rhs: T): T {
    var min = lhs
    for (e in rhs) {
        if (e leq min) {
            min = e
        }
    }
    return min
}

fun <T : Ord<T>> minOf(list: Collection<T>): T? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var min = iterator.next()
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v leq min) {
            min = v
        }
    }
    return min
}

fun <T : Ord<T>, U> minOf(lhs: U, vararg rhs: U, extractor: Extractor<T, U>): T {
    var min = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v leq min) {
            min = v
        }
    }
    return min
}

fun <T : Ord<T>, U> minOf(list: Collection<U>, extractor: Extractor<T, U>): T? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var min = extractor(iterator.next())
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v leq min) {
            min = v
        }
    }
    return min
}

fun <T : Ord<T>> max(lhs: T, rhs: T): T = if (lhs > rhs) lhs else rhs

fun <T : Ord<T>> maxOf(lhs: T, vararg rhs: T): T {
    var max = lhs
    for (e in rhs) {
        if (e geq max) {
            max = e
        }
    }
    return max
}

fun <T : Ord<T>> maxOf(list: Collection<T>): T? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var max = iterator.next()
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v geq max) {
            max = v
        }
    }
    return max
}

fun <T : Ord<T>, U> maxOf(lhs: U, vararg rhs: U, extractor: Extractor<T, U>): T {
    var max = extractor(lhs)
    for (e in rhs) {
        val v = extractor(e)
        if (v geq max) {
            max = v
        }
    }
    return max
}

fun <T : Ord<T>, U> maxOf(list: Collection<U>, extractor: Extractor<T, U>): T? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var max = extractor(iterator.next())
    while (iterator.hasNext()) {
        val v = extractor(iterator.next())
        if (v geq max) {
            max = v
        }
    }
    return max
}

fun <T : Ord<T>> minMaxOf(lhs: T, vararg rhs: T): Pair<T, T> {
    var min = lhs
    var max = lhs
    for (e in rhs) {
        if (e leq min) {
            min = e
        }
        if (e geq max) {
            max = e
        }
    }
    return Pair(min, max)
}

fun <T : Ord<T>> minMaxOf(list: Collection<T>): Pair<T, T>? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var min = iterator.next()
    var max = min
    while (iterator.hasNext()) {
        val v = iterator.next()
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T : Ord<T>, U> minMaxOf(lhs: U, vararg rhs: U, extractor: Extractor<T, U>): Pair<T, T> {
    var min = extractor(lhs)
    var max = min
    for (e in rhs) {
        val v = extractor(e)
        if (v leq min) {
            min = v
        }
        if (v geq max) {
            max = v
        }
    }
    return Pair(min, max)
}

fun <T : Ord<T>, U> minMaxOf(list: Collection<U>, extractor: Extractor<T, U>): Pair<T, T>? {
    if (list.isEmpty()) {
        return null
    }
    val iterator = list.iterator()
    var min = extractor(iterator.next())
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
