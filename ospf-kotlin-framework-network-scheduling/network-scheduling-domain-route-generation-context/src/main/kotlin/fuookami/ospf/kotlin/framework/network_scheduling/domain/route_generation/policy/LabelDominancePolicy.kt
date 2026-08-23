package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.policy

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_generation.model.EspprcLabel

/**
 * 标签支配策略。 / Label dominance policy.
 *
 * 三资源支配：如果 label A 的 reduced cost <= label B 的 reduced cost，
 * A 的时间 <= B 的时间，A 的负载 <= B 的负载，
 * 且 A 的 visited 是 B 的子集，A 的 forbidden 是 B 的超集，
 * 则 A 支配 B。 / Three-resource dominance: label A dominates label B if
 * A's reduced cost <= B's, A's time <= B's, A's load <= B's,
 * A's visited is a subset of B's, and A's forbidden is a superset of B's.
 */
fun interface LabelDominancePolicy {

    /**
     * 判断 label a 是否支配 label b。 / Whether label a dominates label b.
     */
    fun dominates(a: EspprcLabel, b: EspprcLabel): Boolean

    companion object {
        /** 默认支配策略：三资源支配 + elementarity + 不可达超集 / Default: three-resource + elementarity + forbidden superset */
        val Default = LabelDominancePolicy { a, b ->
            if (a.currentNode != b.currentNode) return@LabelDominancePolicy false

            // 资源支配 / Resource dominance
            if (a.reducedCost gr b.reducedCost) return@LabelDominancePolicy false
            if (a.time gr b.time) return@LabelDominancePolicy false
            if (a.load gr b.load) return@LabelDominancePolicy false

            // Elementarity: a 的 visited 是 b 的子集（a 访问的客户不多于 b）
            // Elementarity: a's visited is a subset of b's (a visits no more customers than b)
            if (!isVisitedSubset(a, b)) return@LabelDominancePolicy false

            // 不可达超集：a 的 forbidden 是 b 的超集（a 标记了更多不可达客户）
            // Forbidden superset: a's forbidden is a superset of b's (a marks more unreachable)
            if (!b.forbidden.isSubsetOf(a.forbidden)) return@LabelDominancePolicy false

            // 至少有一个严格不等 / At least one strict inequality
            a.reducedCost ls b.reducedCost ||
                    a.time ls b.time ||
                    a.load ls b.load
        }

        private fun isVisitedSubset(a: EspprcLabel, b: EspprcLabel): Boolean {
            if (a.visited.size != b.visited.size) return false
            for (i in 0 until a.visited.size) {
                if (a.visited.contains(i) && !b.visited.contains(i)) return false
            }
            return true
        }
    }
}
