package fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.solver.heuristic.*

/** 显式违约与目标评估，不把放宽约束后的解标为可行 / Explicit violation/objective assessment, never labeling relaxed solutions feasible.
 * @property value 业务值 / Business value
 * @property objective 显式最小化标量 / Explicit minimization scalar
 * @property violation 非负归一化总违约 / Nonnegative normalized total violation
 */
data class ConstraintAssessment<T>(val value: T, val objective: Flt64, val violation: Flt64)

/** 逐步归零的 epsilon 约束排序器 / Epsilon constraint ranker tightening to zero.
 * @property initialEpsilon 初始归一化违约容差 / Initial normalized violation tolerance
 * @property horizon 收紧步数 / Tightening steps
 * @property exponent 收紧曲线幂 / Tightening exponent
 */
class EpsilonConstraintEvaluator(
    val initialEpsilon: Double,
    val horizon: Long,
    val exponent: Double = 2.0
) {
    /** 排序只影响私有工作种群，严格有效档案继续要求真实可行 / Rank private working populations only; valid archives still require genuine feasibility.
     * @param candidates 已计费评估的候选 / Already metered candidates
     * @param step 当前完整步 / Current complete step
     * @return 容差下的稳定优先序 / Stable order under epsilon tolerance
     */
    fun <T> rank(candidates: List<ConstraintAssessment<T>>, step: Long): Ret<List<ConstraintAssessment<T>>> {
        if (!initialEpsilon.isFinite() || initialEpsilon < 0 || horizon <= 0 || !exponent.isFinite() || exponent <= 0 || step < 0 || !valid(candidates)) return searchInvalid("invalid epsilon constraint inputs")
        val epsilon = initialEpsilon * Math.pow((1.0 - step.toDouble() / horizon).coerceIn(0.0, 1.0), exponent)
        return Ok(candidates.sortedWith { a, b ->
            val av = a.violation.toDouble()
            val bv = b.violation.toDouble()
            if (av <= epsilon && bv <= epsilon || av == bv) a.objective.toDouble().compareTo(b.objective.toDouble()) else av.compareTo(bv)
        })
    }
}

private fun <T> valid(candidates: List<ConstraintAssessment<T>>): Boolean = candidates.all {
    it.objective.toDouble().isFinite() && it.violation.toDouble().isFinite() && it.violation >= Flt64.zero
}

/** 随机排序采用有界冒泡扫描，不能使用非传递随机 Comparator / Stochastic ranking uses bounded bubble passes, never a non-transitive randomized Comparator.
 * @property objectiveProbability 双不可行解按目标比较的概率 / Objective-comparison probability for infeasible pairs
 */
class StochasticRanking(val objectiveProbability: Double = 0.45) {
    /** 进行随机排序 / Perform stochastic ranking.
     * @param candidates 已计费且有违约度的候选 / Metered candidates with violation measures
     * @param random 可恢复随机流 / Restorable random stream
     * @return 排序结果 / Ranked candidates
     */
    fun <T> rank(candidates: List<ConstraintAssessment<T>>, random: SearchRandom): Ret<List<ConstraintAssessment<T>>> {
        if (!objectiveProbability.isFinite() || objectiveProbability !in 0.0..1.0 || !valid(candidates)) return searchInvalid("invalid stochastic ranking inputs")
        val ranked = candidates.toMutableList()
        repeat(ranked.size) {
            var swapped = false
            for (index in 0 until ranked.size - 1) {
                val a = ranked[index]
                val b = ranked[index + 1]
                val useObjective = a.violation == Flt64.zero && b.violation == Flt64.zero || random().toDouble() < objectiveProbability
                val reverse = if (useObjective) a.objective > b.objective else a.violation > b.violation
                if (reverse) {
                    ranked[index] = b
                    ranked[index + 1] = a
                    swapped = true
                }
            }
            if (!swapped) return Ok(ranked)
        }
        return Ok(ranked)
    }
}
