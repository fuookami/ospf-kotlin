package fuookami.ospf.kotlin.utils.functional

inline fun <R, T> Iterable<T>.lastNotNullOf(
    crossinline extractor: Extractor<R?, T>
): R {
    var result: R? = null

    val iterator = this.iterator()
    while (iterator.hasNext()) {
        result = extractor(iterator.next())
    }

    return result ?: throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}

inline fun <R, T> List<T>.lastNotNullOf(
    crossinline extractor: Extractor<R?, T>
): R {
    val iterator = this.listIterator()
    while (iterator.hasPrevious()) {
        val result = extractor(iterator.next())

        if (result != null) {
            return result
        }
    }

    throw NoSuchElementException("No element of the collection was transformed to a non-null value.")
}
