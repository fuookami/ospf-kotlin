/**
 * 非支配排序与拥挤距离 / Non-dominated sorting and crowding distance
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 快速非支配排序（NSGA-II 的分层步骤）。 / Fast non-dominated sorting (the layering step of NSGA-II).
 *
 * 每一层（front）内部的解互不支配，层号越小越优。第 1 层就是整个种群的 Pareto 前沿。 / Solutions within a front are mutually non-dominated, and a smaller front index is better. Front 1 is the Pareto front of the
 * whole population.
*/
data object FastNonDominatedSort {

    /**
     * 对候选集分层 / Layer the candidate set
     *
     * @param T 候选类型 / Candidate type
     * @param items 候选集 / Candidate set
     * @param objectivesOf 目标向量提取器 / Objective vector extractor
     * @param dominance 支配判据 / Dominance predicate
     * @return 分层结果，按层号升序 / Layering result ordered by ascending front index
    */
    fun <T> sort(
        items: List<T>,
        objectivesOf: (T) -> List<Flt64>,
        dominance: Dominance
    ): List<List<T>> {
        val size = items.size
        if (size == 0) {
            return emptyList()
        }

        val objectives = items.map(objectivesOf)
        val dominated = List(size) { mutableListOf<Int>() }
        val dominatedAmount = IntArray(size)
        val fronts = mutableListOf<MutableList<Int>>()
        var current = mutableListOf<Int>()

        for (left in 0 until size) {
            for (right in 0 until size) {
                if (left == right) {
                    continue
                }
                if (dominance.dominates(objectives[left], objectives[right])) {
                    dominated[left].add(right)
                } else if (dominance.dominates(objectives[right], objectives[left])) {
                    dominatedAmount[left] += 1
                }
            }
            if (dominatedAmount[left] == 0) {
                current.add(left)
            }
        }

        val visited = BooleanArray(size)
        while (current.isNotEmpty()) {
            fronts.add(current)
            current.forEach { visited[it] = true }

            val next = mutableListOf<Int>()
            for (index in current) {
                for (dominatedIndex in dominated[index]) {
                    if (visited[dominatedIndex]) {
                        continue
                    }
                    dominatedAmount[dominatedIndex] -= 1
                    if (dominatedAmount[dominatedIndex] == 0) {
                        next.add(dominatedIndex)
                    }
                }
            }
            current = next
        }

        return fronts.map { front -> front.map { items[it] } }
    }
}

/**
 * 拥挤距离（NSGA-II 的多样性度量）。 / Crowding distance (the diversity measure of NSGA-II).
 *
 * 同一层内，每个解在每一维上取其左右邻居的归一化间隔之和；边界解取正无穷以强制保留。 / Within a front, each solution accumulates the normalised gap to its neighbours on every objective; boundary solutions get
 * positive infinity so that they are always retained.
*/
data object CrowdingDistance {

    /**
     * 计算拥挤距离 / Compute crowding distances
     *
     * @param objectives 同一层内各解的目标向量 / Objective vectors of the solutions within one front
     * @return 与输入同序的拥挤距离 / Crowding distances in input order
    */
    fun assign(objectives: List<List<Flt64>>): List<Double> {
        val size = objectives.size
        if (size == 0) {
            return emptyList()
        }
        if (size <= 2) {
            return List(size) { Double.POSITIVE_INFINITY }
        }

        val distance = DoubleArray(size)
        val dimension = objectives.first().size
        for (index in 0 until dimension) {
            val order = (0 until size).sortedBy { objectives[it][index].toDouble() }
            val minimum = objectives[order.first()][index].toDouble()
            val maximum = objectives[order.last()][index].toDouble()
            distance[order.first()] = Double.POSITIVE_INFINITY
            distance[order.last()] = Double.POSITIVE_INFINITY

            val span = maximum - minimum
            if (span <= 0.0) {
                continue
            }
            for (position in 1 until size - 1) {
                val current = objectives[order[position]][index].toDouble()
                val next = objectives[order[position + 1]][index].toDouble()
                val previous = objectives[order[position - 1]][index].toDouble()
                distance[order[position]] += (next - previous) / span
                if (!current.isFinite()) {
                    distance[order[position]] = Double.POSITIVE_INFINITY
                }
            }
        }
        return distance.toList()
    }
}
