/**
 * 求解器间隙计算
 * Solver gap calculation
 */
package fuookami.ospf.kotlin.core.solver

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 计算目标值与可能最优值之间的相对间隙。
 * Compute the relative gap between the objective value and the possible best objective.
 *
 * @param obj 当前目标值 / Current objective value
 * @param possibleBestObj 可能的最优目标值 / Possible best objective value
 * @return 相对间隙 / Relative gap
 */
fun gap(obj: Flt64, possibleBestObj: Flt64): Flt64 {
    return (obj - possibleBestObj + Flt64.decimalPrecision) / (obj + Flt64.decimalPrecision)
}
