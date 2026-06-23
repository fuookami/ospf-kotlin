package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.utils.functional.*

class SymbolicLinearInequality<V : Ring<V>>(val inequality: LinearInequality<V>)

class SymbolicQuadraticInequality<V : Ring<V>>(val inequality: QuadraticInequalityOf<V>)

interface Constraint<V, P> where V : RealNumber<V>, V : NumberField<V>, P : Category {
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

data class MetaDualSolution(
    val constraints: Map<MathConstraint, Flt64>,
    val symbols: Map<IntermediateSymbol<*>, List<Pair<Constraint<Flt64, *>, Flt64>>>
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
fun Map<Constraint<Flt64, Quadratic>, Flt64>.toMeta(): MetaDualSolution {
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

sealed class ConstraintImpl<V, P : Category>(
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
            relation: LinearRelation<V>,
            tokens: AbstractTokenTable<V>,
            converter: IntoValue<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): Ret<LinearConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
            val constraintRelation = when (val result = relation.constraintRelation()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) }
            val lhs = createLinearCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return Ok(LinearConstraintImpl(
                lhs = lhs,
                sign = constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            ))
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
            relation: QuadraticRelation<V>,
            tokens: AbstractTokenTable<V>,
            converter: IntoValue<V>,
            lazy: Boolean = false,
            name: String = "",
            origin: MathConstraint? = null,
            from: Pair<IntermediateSymbol<*>, Boolean>? = null,
        ): Ret<QuadraticConstraintImpl<V>> where V : RealNumber<V>, V : NumberField<V> {
            val constraintRelation = when (val result = relation.constraintRelation()) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val flattenData = relation.flattenData
            val flt64Monomials = flattenData.monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) }
            val lhs = createQuadraticCells(flt64Monomials, tokens, converter)
            val rhs: V = -flattenData.constant
            return Ok(QuadraticConstraintImpl(
                lhs = lhs,
                sign = constraintRelation,
                rhs = rhs,
                lazy = lazy,
                name = name ?: relation.name,
                origin = origin,
                from = from
            ))
        }
    }
}

internal fun <V> createLinearCells(
    monomials: List<LinearMonomial<Flt64>>,
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>
): ArrayList<LinearCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<LinearCell<V>>()
    for (monomial in monomials) {
        val variable = monomial.symbol as? AbstractVariableItem<*, *> ?: continue
        val token = tokens.find(variable)
        if (token != null && monomial.coefficient neq Flt64.zero) {
            cells.add(LinearCellImpl(tokens, monomial.coefficient, token, converter))
        }
    }
    return cells
}

internal fun <V> createQuadraticCells(
    monomials: List<QuadraticMonomial<Flt64>>,
    tokens: AbstractTokenTable<V>,
    converter: IntoValue<V>
): ArrayList<QuadraticCell<V>> where V : RealNumber<V>, V : NumberField<V> {
    val cells = ArrayList<QuadraticCell<V>>()
    for (monomial in monomials) {
        val variable1 = monomial.symbol1 as? AbstractVariableItem<*, *> ?: continue
        val token1 = tokens.find(variable1)
        val token2 = if (monomial.symbol2 != null) {
            tokens.find(monomial.symbol2 as? AbstractVariableItem<*, *> ?: continue) ?: continue
        } else {
            null
        }
        if (token1 != null && monomial.coefficient neq Flt64.zero) {
            cells.add(QuadraticCellImpl(tokens, monomial.coefficient, token1, token2, converter))
        }
    }
    return cells
}
