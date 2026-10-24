package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

internal fun <T> orderedPairs(values: List<T>, precedes: (T, T) -> Boolean): List<Pair<T, T>> {
    return buildList {
        for (i in values.indices) {
            for (j in i + 1 until values.size) {
                if (precedes(values[i], values[j])) {
                    add(values[i] to values[j])
                }
            }
        }
    }
}

internal fun <T> firstOccurrenceIndices(values: List<T>): Map<T, Int> {
    val indices = HashMap<T, Int>()
    for ((index, value) in values.withIndex()) {
        indices.putIfAbsent(value, index)
    }
    return indices
}
