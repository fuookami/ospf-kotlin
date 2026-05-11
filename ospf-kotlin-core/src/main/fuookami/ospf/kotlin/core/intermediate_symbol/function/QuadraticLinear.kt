@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.intermediate_symbol.SolverBoundaryCasts
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
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
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AbstractTokenList
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Quadratic linear function: wraps a QuadraticPolynomial as a quadratic intermediate symbol.
 * If the polynomial is purely linear, no helper variable or constraint is needed.
 * If it contains quadratic terms, creates a helper variable y with constraint y = polynomial.
 */
class QuadraticLinearFunction<V>(
    private val _polynomial: QuadraticPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {

    private val isLinear: Boolean by lazy {
        _polynomial.monomials.none { it.isQuadratic }
    }

    private val y: AbstractVariableItem<*, *>? by lazy {
        if (!isLinear) URealVar("${name}_y") else null
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = if (isLinear) Linear else Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = category

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in _polynomial.monomials) {
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRangeV()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null

    internal fun toMathQuadraticInequality(): QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        return if (isLinear) {
            QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                QuadraticPolynomial(_polynomial.monomials.map { QuadraticMonomial.linear(converter.fromValue(it.coefficient), it.symbol1) }, converter.fromValue(_polynomial.constant)),
                QuadraticPolynomial(emptyList(), Flt64.one),
                Comparison.EQ
            )
        } else {
            val flt64Poly = _polynomial.asFlt64QuadraticPoly(converter)
            QuadraticInequalityOf<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                flt64Poly,
                QuadraticPolynomial(emptyList(), Flt64.one),
                Comparison.EQ
            )
        }
    }

    internal val flattenedMonomials: QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>
        get() = _polynomial.asFlt64QuadraticPoly(converter).let { QuadraticFlattenData<fuookami.ospf.kotlin.math.algebra.number.Flt64>(it.monomials, it.constant) }

    override val polynomial: QuadraticPolynomial<V>
        get() = _polynomial

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? = null

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? = null
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register helper variable y with the token collection (only if quadratic).
     */
    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        if (!isLinear && y != null) {
            return when (val result = tokens.add(listOf(y!!))) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * Register the quadratic equality constraint y = polynomial (only if quadratic).
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        if (!isLinear && y != null) {
            val yMon = QuadraticMonomial.linear(converter.one, y!!)
            val lhs = QuadraticPolynomial(
                listOf(yMon) + _polynomial.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                -_polynomial.constant
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), converter.zero)
            val constraint = QuadraticInequalityOf(lhs, rhs, Comparison.EQ, name)
            return when (val result = model.addConstraint(relation = constraint, name = name)) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticLinearFunction(polynomial, converter, name, displayName)

        operator fun invoke(
            polynomial: QuadraticPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = QuadraticLinearFunction(polynomial, flt64Converter, name, displayName)
    }
}
