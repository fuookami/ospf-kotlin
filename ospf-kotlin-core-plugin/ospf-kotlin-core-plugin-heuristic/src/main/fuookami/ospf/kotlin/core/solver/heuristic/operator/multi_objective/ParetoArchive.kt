/**
 * Pareto 档案 / Pareto archive
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 有界 Pareto 档案，保存当前已知的非支配解集。 / Bounded Pareto archive holding the currently known non-dominated set.
 *
 * 与单目标档案 [fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.BestArchive] 的区别：单目标只需要记住最好的几个解，
 * 多目标需要记住**一整条前沿**——任何被支配的解都不值得保留，而互不支配的解都是合法的最终答案。 / Unlike the single-objective archive, which only remembers the best few solutions, the multi-objective case must remember an
 * entire front: any dominated solution is worthless, while every mutually non-dominated solution is a legitimate final answer.
 *
 * 超出容量时按拥挤距离从大到小裁剪，保留分布最稀疏的解——这保持前沿的覆盖度而不是让解聚成一团。 / When the capacity is exceeded, solutions are truncated from the largest crowding distance downwards, keeping the sparsest
 * solutions; this preserves the spread of the front instead of letting the members clump together.
 *
 * @param T 候选类型 / Candidate type
 * @property capacity 档案容量 / Archive capacity
 * @property objectivesOf 目标向量提取器 / Objective vector extractor
 * @property dominance 支配判据 / Dominance predicate
*/
class ParetoArchive<T>(
    val capacity: Int,
    val objectivesOf: (T) -> List<Flt64>,
    val dominance: Dominance
) {
    private val members = mutableListOf<T>()

    /** 档案内容 / Archive content */
    val items: List<T> get() = members.toList()

    /** 档案大小 / Archive size */
    val size: Int get() = members.size

    /**
     * 提交一个候选 / Offer one candidate
     *
     * @param candidate 候选 / Candidate
     * @return 提交后的档案内容 / Archive content after the offer
    */
    fun offer(candidate: T): List<T> = offer(listOf(candidate))

    /**
     * 提交一批候选 / Offer a batch of candidates
     *
     * @param candidates 候选列表 / Candidate list
     * @return 提交后的档案内容 / Archive content after the offer
    */
    fun offer(candidates: List<T>): List<T> {
        for (candidate in candidates) {
            val candidateObjectives = objectivesOf(candidate)

            // 已被档案中某个成员支配：直接丢弃。
            // Dominated by an archive member: discard outright.
            if (members.any { dominance.dominates(objectivesOf(it), candidateObjectives) }) {
                continue
            }
            // 目标向量完全相同的重复解：保留已有的一份。
            // Duplicate with an identical objective vector: keep the existing one.
            if (members.any { objectivesOf(it) == candidateObjectives }) {
                continue
            }

            // 新解支配了档案成员：淘汰它们。
            // The newcomer dominates some members: evict them.
            members.removeAll { dominance.dominates(candidateObjectives, objectivesOf(it)) }
            members.add(candidate)
        }

        trim()
        return items
    }

    /** 按拥挤距离裁剪到容量 / Truncate to capacity by crowding distance */
    private fun trim() {
        if (members.size <= capacity) {
            return
        }

        val distances = CrowdingDistance.assign(members.map(objectivesOf))
        val retained = members.indices
            .sortedWith(compareByDescending<Int> { distances[it] }.thenBy { it })
            .take(capacity)
            .toSortedSet()

        val survivors = members.filterIndexed { index, _ -> index in retained }
        members.clear()
        members.addAll(survivors)
    }
}
