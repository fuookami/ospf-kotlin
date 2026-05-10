@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.URealVar
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

private val flt64Converter = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/**
 * Inequality satisfaction indicator function.
 *
 * Given a linear expression and a comparison type, returns:
 * - 1 if the inequality is satisfied
 * - 0 if the inequality is violated
 *
 * @param lhs the left-hand side linear polynomial
 * @param rhs the right-hand side constant value
 * @param sign the comparison type
 * @param bigM Big-M bound (default 1e6)
 * @param tolerance zero tolerance (default 1e-6)
 * @param strictBoundary strict boundary value (default 0.5)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class InequalityFunction<V>(
    val lhs: LinearPolynomial<V>,
    val rhs: V,
    val sign: Comparison,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "ineq",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    private val flagVar: AbstractVariableItem<*, *> by lazy { BinVar("${name}_flag") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(flagVar)

    val result: LinearPolynomial<V> by lazy {
        LinearPolynomial(listOf(LinearMonomial(converter.one, flagVar)), converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val lhsValue = lhs.evaluateWith(values) ?: return null
        val lhsDouble = converter.fromValue(lhsValue).toDouble()
        val rhsDouble = converter.fromValue(rhs).toDouble()
        val satisfied = when (sign) {
            Comparison.LE, Comparison.LT -> lhsDouble <= rhsDouble
            Comparison.GE, Comparison.GT -> lhsDouble >= rhsDouble
            Comparison.EQ -> kotlin.math.abs(lhsDouble - rhsDouble) < 1e-9
            Comparison.NE -> kotlin.math.abs(lhsDouble - rhsDouble) > 1e-9
        }
        return if (satisfied) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val mF = converter.fromValue(bigM)
        val epsF = converter.fromValue(tolerance)
        val rhsF = converter.fromValue(rhs)
        val lhsF = lhs.asFlt64Poly(converter)
        val lhsMonos = lhsF.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        val allConstraints = mutableListOf<LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>>()

        when (sign) {
            Comparison.LE, Comparison.LT -> {
                // lhs <= rhs + M*(1-flag)  =>  lhs + M*flag <= rhs + M
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(lhsMonos + LinearMonomial(mF, flagVar), lhsF.constant),
                    LinearPolynomial(emptyList(), rhsF + mF),
                    Comparison.LE, "${name}_satisfied"
                )

                // lhs + M*(1-flag) >= rhs + eps  =>  lhs + M - M*flag >= rhs + eps
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(lhsMonos + LinearMonomial(-mF, flagVar), lhsF.constant + mF),
                    LinearPolynomial(emptyList(), rhsF + epsF),
                    Comparison.GE, "${name}_violated"
                )
            }

            Comparison.GE, Comparison.GT -> {
                // lhs >= rhs - M*(1-flag) => lhs + M - M*flag >= rhs
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(lhsMonos + LinearMonomial(-mF, flagVar), lhsF.constant + mF),
                    LinearPolynomial(emptyList(), rhsF),
                    Comparison.GE, "${name}_satisfied"
                )

                // lhs <= rhs - eps + M*flag => lhs - M*flag <= rhs - eps
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(lhsMonos + LinearMonomial(mF, flagVar), lhsF.constant),
                    LinearPolynomial(emptyList(), rhsF - epsF + mF),
                    Comparison.LE, "${name}_violated"
                )
            }

            Comparison.EQ -> {
                val diffMonos = lhsMonos
                val diffConst = lhsF.constant - rhsF

                // diff <= M*(1-flag) + eps => diff + M*flag <= M + eps
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(diffMonos + LinearMonomial(mF, flagVar), diffConst),
                    LinearPolynomial(emptyList(), mF + epsF),
                    Comparison.LE, "${name}_eq_upper"
                )

                // diff >= -M*(1-flag) - eps => diff - M*flag >= -M - eps
                allConstraints += LinearInequality<fuookami.ospf.kotlin.math.algebra.number.Flt64>(
                    LinearPolynomial(diffMonos + LinearMonomial(-mF, flagVar), diffConst),
                    LinearPolynomial(emptyList(), -mF - epsF),
                    Comparison.GE, "${name}_eq_lower"
                )
            }

            Comparison.NE -> {
                return Failed(
                    fuookami.ospf.kotlin.utils.error.Err(
                        fuookami.ospf.kotlin.utils.error.ErrorCode.ApplicationFailed,
                        "InequalityFunction: NE comparison not supported for MIP encoding"
                    )
                )
            }
        }

        addConstraints(model, allConstraints, converter)?.let { return it }
        return ok
    }
    companion object {
        operator fun <V> invoke(
            lhs: LinearPolynomial<V>,
            rhs: V,
            sign: Comparison,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): InequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            InequalityFunction(lhs = lhs, rhs = rhs, sign = sign, converter = converter, bigM = bigM, name = name, displayName = displayName)

        operator fun invoke(
            lhs: LinearPolynomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            rhs: Flt64,
            sign: Comparison,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): InequalityFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = InequalityFunction(
            lhs = lhs,
            rhs = rhs,
            sign = sign,
            converter = flt64Converter,
            bigM = bigM,
            name = name,
            displayName = displayName
        )

        operator fun invoke(
            lhs: LinearMonomial<fuookami.ospf.kotlin.math.algebra.number.Flt64>,
            rhs: Flt64,
            sign: Comparison,
            bigM: Flt64? = null,
            name: String,
            displayName: String? = null
        ): InequalityFunction<fuookami.ospf.kotlin.math.algebra.number.Flt64> = InequalityFunction(
            lhs = LinearPolynomial(listOf(lhs), Flt64.zero),
            rhs = rhs,
            sign = sign,
            converter = flt64Converter,
            bigM = bigM,
            name = name,
            displayName = displayName
        )
    }
}
