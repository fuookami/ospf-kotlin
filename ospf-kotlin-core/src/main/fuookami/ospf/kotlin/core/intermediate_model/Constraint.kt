package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.Solution
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.intermediate_model.Sign
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

/**
 * Generic constraint interface skeleton - C2-2.6 declaration layer.
 * Phantom type parameter V for API signature; numerical kernel remains Flt64.
 */
interface ConstraintOf<V : RealNumber<V>> {
    val lhs: List<CellOf<V>>
    val sign: Sign
    val rhs: Flt64
    val lazy: Boolean
    val name: String
    val origin: MetaConstraint<*>?
    val from: Pair<IntermediateSymbol, Boolean>?
}

typealias DualSolution = Map<Constraint, Flt64>
typealias LinearDualSolution = Map<LinearConstraint, Flt64>
typealias QuadraticDualSolution = Map<QuadraticConstraint, Flt64>

data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol, List<Pair<Constraint, Flt64>>>
)

@JvmName("linearDualSolutionToMetaDualSolution")
fun LinearDualSolution.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!!.first }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

@JvmName("quadraticDualSolutionToMetaDualSolution")
fun QuadraticDualSolution.toMeta(): MetaDualSolution {
    return MetaDualSolution(
        constraints = this
            .filterKeys { it.origin != null }
            .mapKeys { it.key.origin!! },
        symbols = this
            .filterKeys { it.from != null }
            .entries
            .groupBy { it.key.from!!.first }
            .mapValues { prices -> prices.value.map { it.toPair() } }
    )
}

sealed class Constraint(
    open val lhs: List<Cell>,
    val sign: Sign,
    val rhs: Flt64,
    val lazy: Boolean,
    val name: String = "",
    open val origin: MetaConstraint<*>? = null,
    open val from: Pair<IntermediateSymbol, Boolean>? = null
) {
    fun isTrue(): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, rhs)
    }

    fun isTrue(results: Solution): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluate(results) ?: return null
        }
        return sign(lhsValue, rhs)
    }
}

class LinearConstraint(
    override val lhs: List<LinearCell>,
    sign: Sign,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MetaConstraint<*>? = null,
    from: Pair<IntermediateSymbol, Boolean>? = null,
) : Constraint(
    lhs = lhs,
    sign = sign,
    rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        /**
         * Create LinearConstraint from math LinearInequality
         */
        operator fun invoke(
            relation: MathLinearInequality,
            tokens: AbstractTokenTable,
            lazy: Boolean = false,
            name: String = "",
            origin: MetaConstraint<*>? = null,
            from: Pair<IntermediateSymbol, Boolean>? = null,
        ): LinearConstraint {
            val flattenData = relation.flattenData
            val lhs = createLinearCells(flattenData.monomials, tokens)
            return LinearConstraint(
                lhs = lhs,
                sign = Sign(relation.comparison),
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name,
                origin = origin,
                from = from
            )
        }
    }
}

class QuadraticConstraint(
    override val lhs: List<QuadraticCell>,
    sign: Sign,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MetaConstraint<*>? = null,
    from: Pair<IntermediateSymbol, Boolean>? = null
) : Constraint(
    lhs = lhs,
    sign = sign,
    rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        /**
         * Create QuadraticConstraint from math QuadraticInequality
         */
        operator fun invoke(
            relation: MathQuadraticInequality,
            tokens: AbstractTokenTable,
            lazy: Boolean = false,
            name: String = "",
            origin: MetaConstraint<*>? = null,
            from: Pair<IntermediateSymbol, Boolean>? = null,
        ): QuadraticConstraint {
            val flattenData = relation.flattenData
            val lhs = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticConstraint(
                lhs = lhs,
                sign = Sign(relation.comparison),
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name,
                origin = origin,
                from = from
            )
        }
    }
}

/**
 * Internal factory: create LinearCell ArrayList from linear monomials.
 * 内部工厂函数：从线性单项式创建 LinearCell ArrayList。
 *
 * Preserves filtering: token != null && coefficient != zero.
 * 保留过滤条件：token != null && coefficient != zero。
 */
internal fun createLinearCells(
    monomials: List<UtilsLinearMonomial<Flt64>>,
    tokens: AbstractTokenTable
): ArrayList<LinearCell> {
    val cells = ArrayList<LinearCell>()
    for (monomial in monomials) {
        val variable = monomial.symbol as AbstractVariableItem<*, *>
        val token = tokens.find(variable)
        if (token != null && monomial.coefficient neq Flt64.zero) {
            cells.add(LinearCell(tokens, monomial.coefficient, token))
        }
    }
    return cells
}

/**
 * Internal factory: create QuadraticCell ArrayList from quadratic monomials.
 * 内部工厂函数：从二次单项式创建 QuadraticCell ArrayList。
 *
 * Preserves filtering: token1 != null && coefficient != zero.
 * 保留过滤条件：token1 != null && coefficient != zero。
 *
 * Skips monomial when token2 lookup fails (continue behavior).
 * 当 token2 查找失败时跳过该单项式（continue 行为）。
 */
internal fun createQuadraticCells(
    monomials: List<UtilsQuadraticMonomial<Flt64>>,
    tokens: AbstractTokenTable
): ArrayList<QuadraticCell> {
    val cells = ArrayList<QuadraticCell>()
    for (monomial in monomials) {
        val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
        val token1 = tokens.find(variable1)
        val token2 = if (monomial.symbol2 != null) {
            tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
        } else {
            null
        }
        if (token1 != null && monomial.coefficient neq Flt64.zero) {
            cells.add(QuadraticCell(tokens, monomial.coefficient, token1, token2))
        }
    }
    return cells
}
