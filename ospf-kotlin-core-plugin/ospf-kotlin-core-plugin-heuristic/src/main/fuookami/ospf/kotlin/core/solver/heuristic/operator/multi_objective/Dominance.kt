/**
 * 支配关系算子 / Dominance relation operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.multi_objective

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 支配关系接口，多目标优化的最底层判据。 / Dominance relation interface, the bottom-most predicate of multi-objective optimization.
 *
 * 单目标优化用全序（`compareObjective`）比较任意两解；多目标优化只有偏序，
 * 而偏序的全部内容就是「谁支配谁」。因此所有 MO 算子都建立在本接口之上。 / Single-objective optimization uses a total order (`compareObjective`) over any two solutions; multi-objective optimization
 * only has a partial order, and the entire content of that partial order is "who dominates whom". Every MO operator is therefore
 * built on this interface.
*/
interface Dominance {

    /**
     * 判断左目标向量是否支配右目标向量 / Decide whether the left objective vector dominates the right one
     *
     * @param lhs 左目标向量 / Left objective vector
     * @param rhs 右目标向量 / Right objective vector
     * @return 是否支配 / Whether it dominates
    */
    fun dominates(lhs: List<Flt64>, rhs: List<Flt64>): Boolean
}

/**
 * Pareto 支配：所有目标都不差，且至少一个目标严格更好。 / Pareto dominance: no objective is worse and at least one is strictly better.
 *
 * @property directions 每个目标的优化方向，`true` 表示最小化 / Optimization direction per objective; `true` means minimisation
*/
class ParetoDominance(
    val directions: List<Boolean>
) : Dominance {

    override fun dominates(lhs: List<Flt64>, rhs: List<Flt64>): Boolean {
        var strictlyBetter = false
        for (index in directions.indices) {
            val left = lhs[index].toDouble()
            val right = rhs[index].toDouble()
            val better = if (directions[index]) left < right else left > right
            val worse = if (directions[index]) left > right else left < right
            if (worse) {
                return false
            }
            if (better) {
                strictlyBetter = true
            }
        }
        return strictlyBetter
    }
}

/** 全最小化的 Pareto 支配 / Pareto dominance with every objective minimised */
class MinimizationDominance(
    objectiveAmount: Int
) : Dominance {
    private val delegate = ParetoDominance(List(objectiveAmount) { true })

    override fun dominates(lhs: List<Flt64>, rhs: List<Flt64>): Boolean = delegate.dominates(lhs, rhs)
}
