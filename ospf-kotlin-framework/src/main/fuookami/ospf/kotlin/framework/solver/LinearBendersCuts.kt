package fuookami.ospf.kotlin.framework.solver

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/** 带原生变量边界贡献的割构造，不通过翻转符号修补无效证书。
 * / Cut construction including native bounds, without arbitrary certificate sign reversal.
 */
data object LinearBendersCuts {
    /** 最优点锚定的支撑平面，包含目标常数及边界贡献。 / Anchored supporting plane including constants and bounds.
     * @param theta 追索目标变量 / Recourse projection
     * @param fixed 当前主问题值 / Current master values
     * @param dual 原始约束对偶价格 / Original-row dual prices
     * @param objective 子问题完整目标 / Complete subproblem objective
     * @param value 当前最优追索值 / Current optimal recourse value
     * @return 最优性割 / Optimality cut
     */
    fun optimal(
        theta: AbstractVariableItem<*, *>,
        fixed: Map<AbstractVariableItem<*, *>, Flt64>,
        dual: Map<Constraint<Flt64, Linear>, Flt64>,
        objective: LinearPolynomial<Flt64>,
        value: Flt64
    ): LinearInequality<Flt64> {
        val gradient = fixed.keys.associateWith { variable ->
            objective.monomials.filter { it.symbol == variable }.fold(Flt64.zero) { acc, term -> acc + term.coefficient }
        }.toMutableMap()
        for ((row, price) in dual) {
            for (cell in row.lhs) {
                cell as LinearCell<Flt64>
                if (cell.token.variable in fixed) {
                    val variable = cell.token.variable
                    gradient[variable] = gradient.getValue(variable) - price * cell.coefficient
                }
            }
        }
        val intercept = value - gradient.entries.fold(Flt64.zero) { acc, (variable, coefficient) -> acc + coefficient * fixed.getValue(variable) }
        return LinearPolynomial(theta) geq LinearPolynomial(gradient.map { LinearMonomial(it.value, it.key) }, intercept)
    }

    /** 显式消去追索变量原生边界的 Farkas 投影（Gurobi 符号约定）。 / Farkas projection including native bounds (Gurobi convention).
     * @param fixed 当前主问题值 / Current master values
     * @param dual Farkas 行乘子 / Farkas row multipliers
     * @return 排除当前不可行点的有效割或错误 / Valid separating cut or failure
     */
    fun feasible(
        fixed: Map<AbstractVariableItem<*, *>, Flt64>,
        dual: Map<Constraint<Flt64, Linear>, Flt64>
    ): Ret<LinearInequality<Flt64>> {
        fun invalid(reason: String): Ret<LinearInequality<Flt64>> = Failed(ErrorCode.OREngineSolvingException, "无效 Farkas 证书 / Invalid Farkas certificate: $reason")
        val coefficients = linkedMapOf<AbstractVariableItem<*, *>, Flt64>()
        var rhs = Flt64.zero
        for ((row, price) in dual) {
            if (!price.isFinite() || (row.sign == ConstraintRelation.LessEqual && price < Flt64.zero) ||
                (row.sign == ConstraintRelation.GreaterEqual && price > Flt64.zero)) return invalid("row sign ${row.name}")
            rhs += price * row.rhs
            for (cell in row.lhs) {
                cell as LinearCell<Flt64>
                val variable = cell.token.variable
                coefficients[variable] = (coefficients[variable] ?: Flt64.zero) + price * cell.coefficient
            }
        }
        var minimum = Flt64.zero
        for ((variable, coefficient) in coefficients) {
            if (variable in fixed || coefficient == Flt64.zero) continue
            val bound = (if (coefficient > Flt64.zero) variable.lowerBound else variable.upperBound)?.value?.unwrapOrNull()
                ?: return invalid("missing supporting bound ${variable.name}")
            if (!bound.isFinite()) return invalid("nonfinite supporting bound ${variable.name}")
            minimum += coefficient * bound
        }
        val lhs = LinearPolynomial(coefficients.filterKeys { it in fixed }.map { LinearMonomial(it.value, it.key) }, minimum)
        val current = coefficients.filterKeys { it in fixed }.entries.fold(minimum) { acc, (variable, coefficient) -> acc + coefficient * fixed.getValue(variable) }
        if (!current.isFinite() || !rhs.isFinite() || current <= rhs) return invalid("certificate does not separate incumbent")
        return Ok(lhs leq rhs)
    }
}
