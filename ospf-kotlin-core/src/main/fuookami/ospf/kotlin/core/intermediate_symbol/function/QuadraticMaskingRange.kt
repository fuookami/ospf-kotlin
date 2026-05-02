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
 * Quadratic masking range: when z = 1, y is forced to equal polynomial;
 * when z = 0, y is free (within bounds).
 * Uses Big-M formulation: y <= polynomial + M*(1-z), y >= polynomial - M*(1-z).
 *
 * @param polynomial the quadratic polynomial to mask
 * @param z the binary control variable
 * @param bigM Big-M constant (default 1e6)
 */
class QuadraticMaskingRangeFunction<V>(
    val _polynomial: QuadraticPolynomial<V>,
    val z: AbstractVariableItem<*, *>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<*>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<*>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<*>>()
            for (m in _polynomial.monomials) {
                if (m.symbol1 is IntermediateSymbol<*>) deps.add(m.symbol1 as IntermediateSymbol<*>)
                if (m.symbol2 != null && m.symbol2 is IntermediateSymbol<*>) deps.add(m.symbol2 as IntermediateSymbol<*>)
            }
            if (z is IntermediateSymbol<*>) deps.add(z as IntermediateSymbol<*>)
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
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, resultVar)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    override fun evaluate(tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenListFlt64, zeroIfNone: Boolean): Flt64? = null
    override fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenListFlt64?, zeroIfNone: Boolean): Flt64? = null

    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null
    override fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? = null

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * Register this masking range with the quadratic mechanism model.
     * Variable registration is handled separately by the MetaModel.
     */
    fun register(model: AbstractQuadraticMechanismModelFlt64): Try {
        val mF = bigM.asFlt64()
        val flt64Poly = _polynomial.asFlt64QuadraticPoly()
        val resultMon = QuadraticMonomial.linear(Flt64.one, resultVar)
        val bigMMon = QuadraticMonomial.linear(mF, z)

        // y <= polynomial + M*(1 - z)
        // => y - polynomial - M*z <= -M ... nope
        // => y - polynomial - M*(1 - z) <= 0
        // => y - polynomial - M + M*z <= 0
        // => y + M*z - polynomial <= M
        // LHS: resultMon + bigMMon + negatedPolyMonos, constant = -flt64Poly.constant + mF
        // Wait let me think about this more carefully.
        // y <= p + M*(1-z) => y - p <= M*(1-z) => y - p + M*z <= M
        // y >= p - M*(1-z) => y - p >= -M*(1-z) => y - p + M*z >= 0 ... no
        // y >= p - M*(1-z) => y - p >= -M + M*z => y - p - M*z >= -M

        val negatedPolyMonos = flt64Poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }

        // Constraint 1: y - polynomial + M*z <= M
        // LHS monomials: resultMon + negatedPolyMonos + bigMMon, LHS constant = -flt64Poly.constant
        // RHS monomials: none, RHS constant = mF
        val lhs1 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(bigMMon), -flt64Poly.constant)
        val rhs1 = QuadraticPolynomial(emptyList(), mF)
        val constraint1 = QuadraticInequality(lhs1, rhs1, Comparison.LE, "${name}_upper")
        when (val r = model.addConstraint(relation = constraint1, name = constraint1.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        // Constraint 2: y - polynomial - M*z >= -M
        // LHS monomials: resultMon + negatedPolyMonos + negBigMMon, LHS constant = -flt64Poly.constant
        // RHS monomials: none, RHS constant = -mF
        val negBigMMon = QuadraticMonomial.linear(-mF, z)
        val lhs2 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(negBigMMon), -flt64Poly.constant)
        val rhs2 = QuadraticPolynomial(emptyList(), -mF)
        val constraint2 = QuadraticInequality(lhs2, rhs2, Comparison.GE, "${name}_lower")
        when (val r = model.addConstraint(relation = constraint2, name = constraint2.name)) {
            is Ok -> {}
            is Failed -> return Failed(r.error)
            is Fatal -> return Fatal(r.errors)
        }

        return ok
    }

    companion object {
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            z: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticMaskingRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticMaskingRangeFunction(polynomial, z, bigM, converter, name, displayName)

        operator fun invoke(
            polynomial: QuadraticPolynomial<Flt64>,
            z: AbstractVariableItem<*, *>,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): QuadraticMaskingRangeFunction<Flt64> = QuadraticMaskingRangeFunction(polynomial, z, bigM, IntoValue.Flt64, name, displayName)
    }
}