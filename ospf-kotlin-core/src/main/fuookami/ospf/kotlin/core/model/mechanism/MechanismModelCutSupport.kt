/**
 * 机制模型割平面构造支持
 * Mechanism model cut-construction support
 */
package fuookami.ospf.kotlin.core.model.mechanism

import org.apache.logging.log4j.kotlin.KotlinLogger
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 机制模型 cut 构造支持。
 * Cut-construction support for mechanism models.
 *
 * 说明：基于 dual / farkas dual 与 fixed variable 投影构造线性或二次 cut。
 * Note: builds linear/quadratic cuts from dual/farkas-dual solutions with fixed-variable projection.
 */
private data class OrderedVariablePair(
    val first: AbstractVariableItem<*, *>,
    val second: AbstractVariableItem<*, *>
) {
    companion object {
        fun of(
            lhs: AbstractVariableItem<*, *>,
            rhs: AbstractVariableItem<*, *>
        ): OrderedVariablePair {
            return if (
                lhs.key.identifier < rhs.key.identifier ||
                (lhs.key.identifier == rhs.key.identifier && lhs.key.index <= rhs.key.index)
            ) {
                OrderedVariablePair(lhs, rhs)
            } else {
                OrderedVariablePair(rhs, lhs)
            }
        }
    }
}

/**
 * 构造线性最优性 cut。
 * Build linear optimality cuts.
 *
 * @param V 数值类型 / The number type
 * @param constraints 约束列表 / Constraint list
 * @param objectCategory 目标类型（最小化/最大化）/ Objective category
 * @param objectVariable 目标变量 / Objective variable
 * @param fixedVariables 固定变量及其值 / Fixed variables and their values
 * @param dualSolution 对偶解 / Dual solution
 * @param zero 零值 / Zero value
 * @param one 一值 / One value
 * @return 线性不等式列表 / List of linear inequalities
 */
internal fun <V> buildLinearOptimalCut(
    constraints: List<LinearConstraintImpl<V>>,
    objectCategory: ObjectCategory,
    objectVariable: AbstractVariableItem<*, *>,
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    dualSolution: Map<Constraint<V, Linear>, V>,
    zero: V,
    one: V
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    val constants = constraints.fold(zero) { acc, constraint ->
        acc + (dualSolution[constraint] ?: zero) * constraint.rhs
    }
    val polynomials = HashMap<AbstractVariableItem<*, *>, V>()
    for (constraint in constraints) {
        val dual = dualSolution[constraint] ?: continue
        if (dual eq zero) {
            continue
        }

        for (cell in constraint.lhs) {
            val variable = cell.token.variable
            if (variable in fixedVariables) {
                val coefficient = dual * cell.coefficient
                if (coefficient neq zero) {
                    polynomials[variable] = (polynomials[variable] ?: zero) - coefficient
                }
            }
        }
    }
    val rhs = LinearPolynomial(
        monomials = polynomials.map { LinearMonomial(it.value, it.key) },
        constant = constants
    )
    val lhs = LinearPolynomial(listOf(LinearMonomial(one, objectVariable)), zero)
    return when (objectCategory) {
        ObjectCategory.Maximum -> {
            listOf(lhs le rhs)
        }

        ObjectCategory.Minimum -> {
            listOf(lhs ge rhs)
        }
    }
}

/**
 * 构造线性可行性 cut（Farkas）。
 * Build linear feasibility cuts (Farkas).
 *
 * @param V 数值类型 / The number type
 * @param constraints 约束列表 / Constraint list
 * @param fixedVariables 固定变量及其值 / Fixed variables and their values
 * @param farkasDualSolution Farkas 对偶解 / Farkas dual solution
 * @param zero 零值 / Zero value
 * @param one 一值 / One value
 * @param logger 日志记录器 / Logger
 * @return 线性不等式列表 / List of linear inequalities
 */
internal fun <V> buildLinearFeasibleCut(
    constraints: List<LinearConstraintImpl<V>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    farkasDualSolution: Map<Constraint<V, Linear>, V>,
    zero: V,
    one: V,
    logger: KotlinLogger
): List<LinearInequality<V>> where V : RealNumber<V>, V : NumberField<V> {
    var value = zero
    var constants = zero
    val polynomials = HashMap<AbstractVariableItem<*, *>, V>()
    for (constraint in constraints) {
        val dual = farkasDualSolution[constraint] ?: continue
        if (dual eq zero) {
            continue
        }

        value += dual * constraint.rhs
        constants += dual * constraint.rhs
        for (cell in constraint.lhs) {
            val variable = cell.token.variable
            if (variable in fixedVariables) {
                val coefficient = dual * cell.coefficient
                if (coefficient neq zero) {
                    polynomials[variable] = (polynomials[variable] ?: zero) - coefficient
                }
                value -= dual * cell.coefficient * fixedVariables[variable]!!
            }
        }
    }
    if (value ls zero) {
        logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
        constants *= -one
        polynomials.replaceAll { _, v -> -v }
    }
    val lhs = LinearPolynomial(
        monomials = polynomials.map { LinearMonomial(it.value, it.key) },
        constant = constants
    )
    return listOf(lhs le zero)
}

/**
 * 构造二次最优性 cut；二次项为空时自动退化为线性 cut。
 * Build quadratic optimality cuts; degrades to linear cuts when no quadratic terms remain.
 *
 * @param V 数值类型 / The number type
 * @param constraints 约束列表 / Constraint list
 * @param objectCategory 目标类型 / Objective category
 * @param objectVariable 目标变量 / Objective variable
 * @param fixedVariables 固定变量及其值 / Fixed variables and their values
 * @param dualSolution 对偶解 / Dual solution
 * @param zero 零值 / Zero value
 * @param one 一值 / One value
 * @return cut 列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
 */
internal fun <V> buildQuadraticOptimalCut(
    constraints: List<QuadraticConstraintImpl<V>>,
    objectCategory: ObjectCategory,
    objectVariable: AbstractVariableItem<*, *>,
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    dualSolution: Map<Constraint<V, Quadratic>, V>,
    zero: V,
    one: V
): List<Any> where V : RealNumber<V>, V : NumberField<V> {
    val constants = constraints.fold(zero) { acc, constraint ->
        acc + (dualSolution[constraint] ?: zero) * constraint.rhs
    }
    val linearPolynomial = HashMap<AbstractVariableItem<*, *>, V>()
    val quadraticPolynomial = HashMap<OrderedVariablePair, V>()
    for (constraint in constraints) {
        val dual = dualSolution[constraint] ?: continue
        if (dual eq zero) {
            continue
        }

        for (cell in constraint.lhs) {
            val variable1 = cell.token1.variable
            val variable2 = cell.token2?.variable
            if (variable2 == null) {
                if (variable1 in fixedVariables) {
                    val projected = -dual * cell.coefficient
                    if (projected neq zero) {
                        linearPolynomial[variable1] = (linearPolynomial[variable1] ?: zero) + projected
                    }
                }
            } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
                val projected = -dual * cell.coefficient
                if (projected neq zero) {
                    val key = OrderedVariablePair.of(variable1, variable2)
                    quadraticPolynomial[key] = (quadraticPolynomial[key] ?: zero) + projected
                }
            }
        }
    }

    val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq zero }
    if (!hasQuadratic) {
        val rhs = LinearPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq zero }
                .map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        val lhs = LinearPolynomial(
            monomials = listOf(LinearMonomial(one, objectVariable)),
            constant = zero
        )
        val cut = when (objectCategory) {
            ObjectCategory.Maximum -> {
                lhs le rhs
            }

            ObjectCategory.Minimum -> {
                lhs ge rhs
            }
        }
        return listOf(cut)
    }

    val rhs = QuadraticPolynomial(
        monomials = linearPolynomial
            .filterValues { it neq zero }
            .map { QuadraticMonomial.linear(it.value, it.key) } +
            quadraticPolynomial
                .filterValues { it neq zero }
                .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
        constant = constants
    )
    val lhs = QuadraticPolynomial(
        monomials = listOf(QuadraticMonomial.linear(one, objectVariable)),
        constant = zero
    )
    val cut = when (objectCategory) {
        ObjectCategory.Maximum -> {
            lhs le rhs
        }

        ObjectCategory.Minimum -> {
            lhs ge rhs
        }
    }
    return listOf(cut)
}

/**
 * 构造二次可行性 cut（Farkas）；二次项为空时自动退化为线性 cut。
 * Build quadratic feasibility cuts (Farkas); degrades to linear cuts when no quadratic terms remain.
 *
 * @param V 数值类型 / The number type
 * @param constraints 约束列表 / Constraint list
 * @param fixedVariables 固定变量及其值 / Fixed variables and their values
 * @param farkasDualSolution Farkas 对偶解 / Farkas dual solution
 * @param zero 零值 / Zero value
 * @param one 一值 / One value
 * @param logger 日志记录器 / Logger
 * @return cut 列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
 */
internal fun <V> buildQuadraticFeasibleCut(
    constraints: List<QuadraticConstraintImpl<V>>,
    fixedVariables: Map<AbstractVariableItem<*, *>, V>,
    farkasDualSolution: Map<Constraint<V, Quadratic>, V>,
    zero: V,
    one: V,
    logger: KotlinLogger
): List<Any> where V : RealNumber<V>, V : NumberField<V> {
    var value = zero
    var constants = zero
    val linearPolynomial = HashMap<AbstractVariableItem<*, *>, V>()
    val quadraticPolynomial = HashMap<OrderedVariablePair, V>()
    for (constraint in constraints) {
        val dual = farkasDualSolution[constraint] ?: continue
        if (dual eq zero) {
            continue
        }

        value += dual * constraint.rhs
        constants += dual * constraint.rhs
        for (cell in constraint.lhs) {
            val variable1 = cell.token1.variable
            val variable2 = cell.token2?.variable
            if (variable2 == null) {
                if (variable1 in fixedVariables) {
                    val projected = -dual * cell.coefficient
                    if (projected neq zero) {
                        linearPolynomial[variable1] = (linearPolynomial[variable1] ?: zero) + projected
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable1]!!
                }
            } else if (variable1 in fixedVariables && variable2 in fixedVariables) {
                val projected = -dual * cell.coefficient
                if (projected neq zero) {
                    val key = OrderedVariablePair.of(variable1, variable2)
                    quadraticPolynomial[key] = (quadraticPolynomial[key] ?: zero) + projected
                }
                value -= dual * cell.coefficient * fixedVariables[variable1]!! * fixedVariables[variable2]!!
            }
        }
    }
    if (value ls zero) {
        logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
        constants *= -one
        linearPolynomial.replaceAll { _, coefficient -> -coefficient }
        quadraticPolynomial.replaceAll { _, coefficient -> -coefficient }
    }

    val hasQuadratic = quadraticPolynomial.any { (_, coefficient) -> coefficient neq zero }
    if (!hasQuadratic) {
        val lhs = LinearPolynomial(
            monomials = linearPolynomial
                .filterValues { it neq zero }
                .map { LinearMonomial(it.value, it.key) },
            constant = constants
        )
        return listOf(lhs le zero)
    }

    val lhs = QuadraticPolynomial(
        monomials = linearPolynomial
            .filterValues { it neq zero }
            .map { QuadraticMonomial.linear(it.value, it.key) } +
            quadraticPolynomial
                .filterValues { it neq zero }
                .map { QuadraticMonomial.quadratic(it.value, it.key.first, it.key.second) },
        constant = constants
    )
    val rhs = QuadraticPolynomial(emptyList(), zero)
    return listOf(lhs le rhs)
}
