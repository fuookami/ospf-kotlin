@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModelFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.token.AbstractTokenTableFlt64
import fuookami.ospf.kotlin.core.token.QuadraticFlattenDataFlt64
import fuookami.ospf.kotlin.core.token.AbstractTokenListFlt64
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.Category
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Quadratic linear function: wraps a QuadraticPolynomial as a quadratic intermediate symbol.
 * If the polynomial is purely linear, no helper variable or constraint is needed.
 * If it contains quadratic terms, creates a helper variable y with constraint y = polynomial.
 */
class QuadraticLinearFunction<V>(
    private val _polynomial: QuadraticPolynomial<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {

    private val isLinear: Boolean by lazy {
        _polynomial.monomials.none { it.isQuadratic }
    }

    private val y: AbstractVariableItem<*, *>? by lazy {
        if (!isLinear) URealVar("${name}_y") else null
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = if (isLinear) Linear else Quadratic
    override val parent: IntermediateSymbol<*>? = null
    override val operationCategory: Category get() = category

    override val dependencies: Set<IntermediateSymbol<*>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<*>>()
            for (m in _polynomial.monomials) {
                if (m.symbol1 is IntermediateSymbol<*>) deps.add(m.symbol1 as IntermediateSymbol<*>)
                if (m.symbol2 != null && m.symbol2 is IntermediateSymbol<*>) deps.add(m.symbol2 as IntermediateSymbol<*>)
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<Flt64> get() = ExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    override fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null

    override fun toMathQuadraticInequality(): QuadraticInequality {
        return if (isLinear) {
            QuadraticInequality(
                QuadraticPolynomial(_polynomial.monomials.map { QuadraticMonomial.linear(it.coefficient.asFlt64(), it.symbol1) }, _polynomial.constant.asFlt64()),
                QuadraticPolynomial(emptyList(), Flt64.one),
                Comparison.EQ
            )
        } else {
            val flt64Poly = _polynomial.asFlt64QuadraticPoly()
            QuadraticInequality(
                flt64Poly,
                QuadraticPolynomial(emptyList(), Flt64.one),
                Comparison.EQ
            )
        }
    }

    override val flattenedMonomials: QuadraticFlattenDataFlt64
        get() = _polynomial.asFlt64QuadraticPoly().let { QuadraticFlattenDataFlt64(it.monomials, it.constant) }

    @Suppress("UNCHECKED_CAST")
    override val polynomial: QuadraticPolynomial<V>
        get() = _polynomial

    @Suppress("UNCHECKED_CAST")
    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), Flt64.zero as V)

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? = null

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register this function with the quadratic mechanism model.
     * If the polynomial contains quadratic terms, adds a constraint y = polynomial.
     * Variable registration is handled separately by the MetaModel.
     */
    fun register(model: AbstractQuadraticMechanismModelFlt64): Try {
        if (!isLinear && y != null) {
            val flt64Poly = _polynomial.asFlt64QuadraticPoly()
            val yMon = QuadraticMonomial.linear(Flt64.one, y!!)
            val lhs = QuadraticPolynomial(listOf(yMon) + flt64Poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }, -flt64Poly.constant)
            val rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
            val constraint = QuadraticInequality(lhs, rhs, Comparison.EQ, name)
            when (val result = model.addConstraint(relation = constraint, name = constraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticLinearFunction(polynomial, name, displayName)
    }
}
