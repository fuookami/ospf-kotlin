package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.CellFlt64
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCellFlt64
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellFlt64
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.LinearFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as Flt64MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf as MathQuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as UtilsLinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial as UtilsQuadraticMonomial

// ========== Polynomial Kind Marker Types ==========

/**
 * Type parameter for Constraint<V, P> tracking polynomial kind.
 */
sealed interface PolynomialKind

/**
 * Marker for linear constraints: Constraint<V, Linear> holds SymbolicLinearInequality<V>.
 */
object Linear : PolynomialKind

/**
 * Marker for quadratic constraints: Constraint<V, Quadratic> holds SymbolicQuadraticInequality<V>.
 */
object Quadratic : PolynomialKind

// ========== Symbolic Inequality Wrapper Types ==========

/**
 * Symbolic wrapper for a math-layer LinearInequality<V>.
 * Holds the inequality without flattening, preserving the symbolic form.
 */
class SymbolicLinearInequality<V : Ring<V>>(val inequality: MathLinearInequality<V>)

typealias SymbolicLinearInequalityFlt64 = SymbolicLinearInequality<Flt64>

/**
 * Symbolic wrapper for a math-layer QuadraticInequalityOf<V>.
 * Holds the inequality without flattening, preserving the symbolic form.
 */
class SymbolicQuadraticInequality<V : Ring<V>>(val inequality: MathQuadraticInequalityOf<V>)

typealias SymbolicQuadraticInequalityFlt64 = SymbolicQuadraticInequality<Flt64>

// ========== Constraint<V, P> ==========

/**
 * Generic constraint with typed public values and Flt64 solver values.
 * 泛型约束：公开值使用 V，求解器边界值使用 Flt64。
 */
interface Constraint<V, P> where V : RealNumber<V>, V : NumberField<V>, P : PolynomialKind {
    val lhs: List<Cell<V>>
    val sign: ConstraintRelation
    /** V-typed rhs for public callers. / 面向调用方的 V 类型右端项。 */
    val rhs: V
    /** Flt64 rhs for solver-boundary callers. / 面向求解器边界的 Flt64 右端项。 */
    val rhsFlt64: Flt64
    val lazy: Boolean
    val name: String
    val origin: MathConstraint?
    val from: Pair<IntermediateSymbol<*>, Boolean>?
}

typealias ConstraintFlt64<P> = Constraint<Flt64, P>

typealias DualSolution<P> = Map<ConstraintFlt64<P>, Flt64>
typealias LinearDualSolution = Map<LinearConstraint, Flt64>
typealias QuadraticDualSolution = Map<QuadraticConstraint, Flt64>

data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol<*>, List<Pair<ConstraintFlt64<*>, Flt64>>>
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

sealed class ConstraintImpl<V, P : PolynomialKind>(
    override val lhs: List<Cell<V>>,
    override val sign: ConstraintRelation,
    private val _rhs: V,
    private val _rhsFlt64: Flt64,
    override val lazy: Boolean,
    override val name: String = "",
    override val origin: MathConstraint? = null,
    override val from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : Constraint<V, P> where V : RealNumber<V>, V : NumberField<V> {
    override val rhs: V get() = _rhs
    override val rhsFlt64: Flt64 get() = _rhsFlt64

    fun isTrue(): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, _rhs)
    }

    fun isTrue(results: List<V>): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate(results) ?: return null
        }
        return sign(lhsValue, _rhs)
    }

    fun isTrueFlt64(): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluateFlt64() ?: return null
        }
        return sign(lhsValue, _rhsFlt64)
    }

    fun isTrueFlt64(results: Solution): Boolean? {
        var lhsValue = Flt64.zero
        for (cell in lhs) {
            lhsValue += cell.evaluateFlt64(results) ?: return null
        }
        return sign(lhsValue, _rhsFlt64)
    }
}

class LinearConstraintImpl(
    override val lhs: List<LinearCellFlt64>,
    sign: ConstraintRelation,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
) : ConstraintImpl<Flt64, Linear>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    _rhsFlt64 = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        operator fun invoke(
            relation: LinearRelation,
            tokens: AbstractTokenTableFlt64,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): LinearConstraintImpl {
            val flattenData = relation.flattenData
            val lhs = createLinearCells(flattenData.monomials, tokens)
            return LinearConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

class QuadraticConstraintImpl(
    override val lhs: List<QuadraticCellFlt64>,
    sign: ConstraintRelation,
    rhs: Flt64,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : ConstraintImpl<Flt64, Quadratic>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    _rhsFlt64 = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) {
    companion object {
        operator fun invoke(
            relation: QuadraticRelation,
            tokens: AbstractTokenTableFlt64,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): QuadraticConstraintImpl {
            val flattenData = relation.flattenData
            val lhs = createQuadraticCells(flattenData.monomials, tokens)
            return QuadraticConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = -flattenData.constant,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

// Type aliases for Constraint<V, P> with specific polynomial kinds
typealias LinearConstraint = Constraint<Flt64, Linear>
typealias QuadraticConstraint = Constraint<Flt64, Quadratic>

internal fun <V> createLinearCells(
    monomials: List<UtilsLinearMonomial<Flt64>>,
    tokens: AbstractTokenTable<V>
): ArrayList<LinearCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<LinearCell<V>>()
    for (monomial in monomials) {
        val variable = monomial.symbol as AbstractVariableItem<*, *>
        val token = tokens.find(variable)
        if (token != null && monomial.coefficient neq Flt64.zero) {
            cells.add(LinearCellImpl(tokens, monomial.coefficient, token))
        }
    }
    return cells
}

internal fun <V> createQuadraticCells(
    monomials: List<UtilsQuadraticMonomial<Flt64>>,
    tokens: AbstractTokenTable<V>
): ArrayList<QuadraticCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<QuadraticCell<V>>()
    for (monomial in monomials) {
        val variable1 = monomial.symbol1 as AbstractVariableItem<*, *>
        val token1 = tokens.find(variable1)
        val token2 = if (monomial.symbol2 != null) {
            tokens.find(monomial.symbol2 as AbstractVariableItem<*, *>) ?: continue
        } else {
            null
        }
        if (token1 != null && monomial.coefficient neq Flt64.zero) {
            cells.add(QuadraticCellImpl(tokens, monomial.coefficient, token1, token2))
        }
    }
    return cells
}
