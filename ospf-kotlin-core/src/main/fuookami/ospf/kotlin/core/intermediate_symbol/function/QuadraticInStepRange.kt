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
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.BinVar
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
 * Quadratic in-step-range function: y = x if x in [lower, upper], else y = 0.
 * Uses binary variable z where z=1 means x is in range.
 *
 * Constraints:
 * - x - lower >= -M*(1-z)   =>   x - lower + M - M*z >= 0
 * - x - upper <= M*(1-z)    =>   x - upper - M + M*z <= 0
 * - y - x >= -M*(1-z)       =>   y - x + M - M*z >= 0
 * - y - x <= M*(1-z)        =>   y - x - M + M*z <= 0
 * - y <= M*z
 * - y >= -M*z
 *
 * @param x quadratic polynomial input
 * @param lower lower bound of the range
 * @param upper upper bound of the range
 * @param bigM Big-M constant (default 1e6)
 */
class QuadraticInStepRangeFunction<V>(
    val x: QuadraticPolynomial<V>,
    val lower: V,
    val upper: V,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    init {
        require(lower.asFlt64().toDouble() <= upper.asFlt64().toDouble()) {
            "QuadraticInStepRange lower bound must be <= upper bound"
        }
    }

    val z: AbstractVariableItem<*, *> = BinVar("${name}_z")
    val y: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Quadratic
    override val parent: IntermediateSymbol<*>? = null
    override val operationCategory: Category get() = Quadratic

    override val dependencies: Set<IntermediateSymbol<*>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<*>>()
            for (m in x.monomials) {
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
        return QuadraticInequality(
            QuadraticPolynomial(emptyList(), Flt64.zero),
            QuadraticPolynomial(emptyList(), Flt64.one),
            Comparison.EQ
        )
    }

    override val flattenedMonomials: QuadraticFlattenDataFlt64
        get() = QuadraticFlattenDataFlt64(emptyList(), Flt64.zero)

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, y)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? = null

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register this in-step-range function with the quadratic mechanism model.
     * Adds Big-M constraints. Variable registration is handled separately by the MetaModel.
     */
    fun register(model: AbstractQuadraticMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val lowerF = lower.asFlt64()
        val upperF = upper.asFlt64()
        val flt64X = x.asFlt64QuadraticPoly()
        val yMon = QuadraticMonomial.linear(Flt64.one, y)
        val zMon = QuadraticMonomial.linear(mF, z)
        val negZMon = QuadraticMonomial.linear(-mF, z)

        val negXMonos = flt64X.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
        val posXMonos = flt64X.monomials

        // Constraint 1: x >= lower - M*(1-z) => x + M*z >= lower + M - M => x + M*z >= lower ...
        // Let me redo this properly.
        // z=1 => x >= lower:  x + M*(1-z) >= lower  =>  x + M - M*z >= lower
        // LHS: posXMonos + negZMon, constant = flt64X.constant + mF
        // RHS: constant = lowerF
        // Wait: we need the inequality in the form LHS >= RHS or LHS <= RHS
        // The model uses QuadraticInequality(lhs, rhs, comparison) meaning lhs comparison rhs

        // C1: x + M*(1-z) >= lower  =>  x + M - M*z >= lower
        val c1Lhs = QuadraticPolynomial(posXMonos + listOf(negZMon), flt64X.constant + mF)
        val c1Rhs = QuadraticPolynomial(emptyList(), lowerF)
        val c1 = QuadraticInequality(c1Lhs, c1Rhs, Comparison.GE, "${name}_range_lb")
        when (val r = model.addConstraint(relation = c1, name = c1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // C2: x <= upper + M*(1-z)  =>  x - M + M*z <= upper
        val c2Lhs = QuadraticPolynomial(posXMonos + listOf(zMon), flt64X.constant - mF)
        val c2Rhs = QuadraticPolynomial(emptyList(), upperF)
        val c2 = QuadraticInequality(c2Lhs, c2Rhs, Comparison.LE, "${name}_range_ub")
        when (val r = model.addConstraint(relation = c2, name = c2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // C3: y - x + M*(1-z) >= 0  =>  y - x + M - M*z >= 0
        val c3Lhs = QuadraticPolynomial(listOf(yMon) + negXMonos + listOf(negZMon), -flt64X.constant + mF)
        val c3Rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
        val c3 = QuadraticInequality(c3Lhs, c3Rhs, Comparison.GE, "${name}_eq_lb")
        when (val r = model.addConstraint(relation = c3, name = c3.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // C4: y - x - M*(1-z) <= 0  =>  y - x - M + M*z <= 0
        val c4Lhs = QuadraticPolynomial(listOf(yMon) + negXMonos + listOf(zMon), -flt64X.constant - mF)
        val c4Rhs = QuadraticPolynomial(emptyList(), Flt64.zero)
        val c4 = QuadraticInequality(c4Lhs, c4Rhs, Comparison.LE, "${name}_eq_ub")
        when (val r = model.addConstraint(relation = c4, name = c4.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // C5: y <= M*z
        val c5Lhs = QuadraticPolynomial(listOf(yMon), Flt64.zero)
        val c5Rhs = QuadraticPolynomial(listOf(zMon), Flt64.zero)
        val c5 = QuadraticInequality(c5Lhs, c5Rhs, Comparison.LE, "${name}_zero_ub")
        when (val r = model.addConstraint(relation = c5, name = c5.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // C6: y >= -M*z
        val c6Lhs = QuadraticPolynomial(listOf(yMon), Flt64.zero)
        val c6Rhs = QuadraticPolynomial(listOf(negZMon), Flt64.zero)
        val c6 = QuadraticInequality(c6Lhs, c6Rhs, Comparison.GE, "${name}_zero_lb")
        when (val r = model.addConstraint(relation = c6, name = c6.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun <V> invoke(
            x: QuadraticPolynomial<V>,
            lower: V,
            upper: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticInStepRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticInStepRangeFunction(x, lower, upper, bigM, converter, name, displayName)

        operator fun invoke(
            x: QuadraticPolynomial<Flt64>,
            lower: Flt64,
            upper: Flt64,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): QuadraticInStepRangeFunction<Flt64> = QuadraticInStepRangeFunction(x, lower, upper, bigM, IntoValue.Flt64, name, displayName)
    }
}