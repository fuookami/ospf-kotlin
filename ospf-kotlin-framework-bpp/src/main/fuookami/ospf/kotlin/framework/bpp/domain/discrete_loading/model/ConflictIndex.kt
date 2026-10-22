package fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model

/** 经验证的团覆盖，未覆盖边仍保留；绝不把连通分量当作团。 / Verified clique cover, never a connected-component closure.
 * @property cliques 每个集合中的所有舱位逐对冲突 / Pairwise-conflicting position sets
 */
class ConflictIndex private constructor(val cliques: List<Set<String>>) {
    companion object {
        /** 构建确定性的贪心团覆盖。 / Build a deterministic greedy clique cover.
         * @param edges 已验证互斥边 / Verified edges
         * @return 完整边覆盖 / Complete edge cover
         */
        fun build(edges: Set<PositionConflict>): ConflictIndex {
            val adjacency = linkedMapOf<String, MutableSet<String>>()
            for ((a, b) in edges) {
                adjacency.getOrPut(a) { sortedSetOf() }.add(b)
                adjacency.getOrPut(b) { sortedSetOf() }.add(a)
            }
            val remaining = edges.map { setOf(it.first, it.second) }.toMutableSet()
            val cliques = arrayListOf<Set<String>>()
            while (remaining.isNotEmpty()) {
                val seed = remaining.first().sorted()
                val clique = seed.toMutableSet()
                val candidates = adjacency.getValue(seed[0]).intersect(adjacency.getValue(seed[1])).sorted()
                for (candidate in candidates) {
                    if (clique.all { candidate in adjacency.getValue(it) }) clique.add(candidate)
                }
                cliques.add(clique)
                remaining.removeAll { clique.containsAll(it) }
            }
            return ConflictIndex(cliques)
        }
    }
}
