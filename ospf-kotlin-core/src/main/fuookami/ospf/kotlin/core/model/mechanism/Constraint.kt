package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.Cell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCell
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCell
import fuookami.ospf.kotlin.core.model.intermediate.LinearCellImpl
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticCellImpl
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.model.mechanism.Linear
import fuookami.ospf.kotlin.core.model.mechanism.Quadratic
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

// ========== Polynomial Kind Marker Types ==========

sealed interface PolynomialKind
object Linear : PolynomialKind
object Quadratic : PolynomialKind

// ========== Symbolic Inequality Wrapper Types ==========

class SymbolicLinearInequality<V : Ring<V>>(val inequality: LinearInequality<V>)

class SymbolicQuadraticInequality<V : Ring<V>>(val inequality: QuadraticInequalityOf<V>)

// ========== Constraint<V, P> ==========

/**
 * Generic constraint with V-typed values.
 * Flt64 evaluation is handled by the solver adapter, not the core chain.
 */
interface Constraint<V, P> where V : RealNumber<V>, V : NumberField<V>, P : PolynomialKind {
    val lhs: List<Cell<V>>
    val sign: ConstraintRelation
    val rhs: V
    val lazy: Boolean
    val name: String
    val origin: MathConstraint?
    val from: Pair<IntermediateSymbol<*>, Boolean>?

    fun isTrue(): Boolean?
    fun isTrue(results: List<V>): Boolean?
}

typealias ConstraintFlt64<P> = Constraint<Flt64, P>

typealias DualSolution<P> = Map<ConstraintFlt64<P>, Flt64>

data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol<*>, List<Pair<ConstraintFlt64<*>, Flt64>>>
)

@JvmName("linearDualSolutionToMetaDualSolution")
fun kotlin.collections.Map<Constraint<Flt64, Linear>, Flt64>.toMeta(): MetaDualSolution {
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
fun kotlin.collections.Map<Constraint<Flt64, Quadratic>, Flt64>.toMeta(): MetaDualSolution {
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
    override val lazy: Boolean,
    override val name: String = "",
    override val origin: MathConstraint? = null,
    override val from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : Constraint<V, P> where V : RealNumber<V>, V : NumberField<V> {
    override val rhs: V get() = _rhs

    override fun isTrue(): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate() ?: return null
        }
        return sign(lhsValue, _rhs)
    }

    override fun isTrue(results: List<V>): Boolean? {
        var lhsValue = _rhs - _rhs
        for (cell in lhs) {
            lhsValue += cell.evaluate(results) ?: return null
        }
        return sign(lhsValue, _rhs)
    }
}

class LinearConstraintImpl<V>(
    override val lhs: List<LinearCell<V>>,
    sign: ConstraintRelation,
    rhs: V,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null,
) : ConstraintImpl<V, Linear>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            relation: LinearRelation,
            tokens: AbstractTokenTable<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): LinearConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
            val flattenData = relation.flattenData
            val lhs = createLinearCells(flattenData.monomials, tokens)
            // Adapter boundary: flattenData.constant is Flt64; safe when V=Flt64
            val rhs: V = (-flattenData.constant) as V
            return LinearConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

class QuadraticConstraintImpl<V>(
    override val lhs: List<QuadraticCell<V>>,
    sign: ConstraintRelation,
    rhs: V,
    lazy: Boolean = false,
    name: String = "",
    origin: MathConstraint? = null,
    from: Pair<IntermediateSymbol<*>, Boolean>? = null
) : ConstraintImpl<V, Quadratic>(
    lhs = lhs,
    sign = sign,
    _rhs = rhs,
    lazy = lazy,
    name = name,
    origin = origin,
    from = from
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        operator fun <V> invoke(
            relation: QuadraticRelation,
            tokens: AbstractTokenTable<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): QuadraticConstraintImpl<V> where V : RealNumber<V>, V : NumberField<V> {
            val flattenData = relation.flattenData
            val lhs = createQuadraticCells(flattenData.monomials, tokens)
            // Adapter boundary: flattenData.constant is Flt64; safe when V=Flt64
            val rhs: V = (-flattenData.constant) as V
            return QuadraticConstraintImpl(
                lhs = lhs,
                sign = relation.constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            )
        }
    }
}

// Type aliases for Constraint<V, P> with specific polynomial kinds

internal fun <V> createLinearCells(
    monomials: List<LinearMonomial<Flt64>>,
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
    monomials: List<QuadraticMonomial<Flt64>>,
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
