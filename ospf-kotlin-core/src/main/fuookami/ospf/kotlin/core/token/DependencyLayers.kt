package fuookami.ospf.kotlin.core.token

/** 按原始顺序构造拓扑层，未满足的外部依赖和环保持未就绪。 / Build stable topological layers, leaving cycles and unmet external dependencies unresolved. */
internal fun <S> buildDependencyLayers(dependencies: Map<S, Set<S>>): List<List<S>> {
    val remaining = HashMap<S, Int>()
    val dependents = HashMap<S, MutableList<S>>()
    val depths = HashMap<S, Int>()
    val completedDepths = HashMap<S, Int>()
    val queue = ArrayDeque<S>()
    for ((symbol, required) in dependencies) {
        remaining[symbol] = required.size
        if (required.isEmpty()) {
            queue.addLast(symbol)
        }
        for (dependency in required) {
            dependents.getOrPut(dependency) { ArrayList() }.add(symbol)
        }
    }
    var maximumDepth = -1
    while (queue.isNotEmpty()) {
        val symbol = queue.removeFirst()
        val depth = depths[symbol] ?: 0
        completedDepths[symbol] = depth
        maximumDepth = maxOf(maximumDepth, depth)
        for (dependent in dependents[symbol].orEmpty()) {
            depths[dependent] = maxOf(depths[dependent] ?: 0, depth + 1)
            val amount = remaining.getValue(dependent) - 1
            remaining[dependent] = amount
            if (amount == 0) {
                queue.addLast(dependent)
            }
        }
    }
    // 按输入顺序分桶而非按发现顺序输出，不需逐层排序或全量重扫。 / Bucket in input order rather than discovery order, without per-layer sorting or rescans.
    val layers = List(maximumDepth + 1) { ArrayList<S>() }
    for (symbol in dependencies.keys) {
        completedDepths[symbol]?.let { layers[it].add(symbol) }
    }
    return layers
}
