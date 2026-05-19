@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVar
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
        val satisfied = when (sign) {
            Comparison.LE -> !(lhsValue gr rhs)
            Comparison.LT -> lhsValue ls rhs
            Comparison.GE -> !(lhsValue ls rhs)
            Comparison.GT -> lhsValue gr rhs
            Comparison.EQ -> lhsValue eq rhs
            Comparison.NE -> lhsValue neq rhs
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
        val mV = bigM
        val epsV = tolerance
        val rhsV = rhs
        val lhsMonos = lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        val allConstraints = mutableListOf<LinearInequality<V>>()

        when (sign) {
            Comparison.LE, Comparison.LT -> {
                // lhs <= rhs + M*(1-flag)  =>  lhs + M*flag <= rhs + M
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(mV, flagVar), lhs.constant),
                    LinearPolynomial(emptyList(), rhsV + mV),
                    Comparison.LE, "${name}_satisfied"
                )

                // lhs + M*(1-flag) >= rhs + eps  =>  lhs + M - M*flag >= rhs + eps
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(-mV, flagVar), lhs.constant + mV),
                    LinearPolynomial(emptyList(), rhsV + epsV),
                    Comparison.GE, "${name}_violated"
                )
            }

            Comparison.GE, Comparison.GT -> {
                // lhs >= rhs - M*(1-flag) => lhs + M - M*flag >= rhs
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(-mV, flagVar), lhs.constant + mV),
                    LinearPolynomial(emptyList(), rhsV),
                    Comparison.GE, "${name}_satisfied"
                )

                // lhs <= rhs - eps + M*flag => lhs - M*flag <= rhs - eps
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(mV, flagVar), lhs.constant),
                    LinearPolynomial(emptyList(), rhsV - epsV + mV),
                    Comparison.LE, "${name}_violated"
                )
            }

            Comparison.EQ -> {
                val diffMonos = lhsMonos
                val diffConst = lhs.constant - rhsV

                // diff <= M*(1-flag) + eps => diff + M*flag <= M + eps
                allConstraints += LinearInequality(
                    LinearPolynomial(diffMonos + LinearMonomial(mV, flagVar), diffConst),
                    LinearPolynomial(emptyList(), mV + epsV),
                    Comparison.LE, "${name}_eq_upper"
                )

                // diff >= -M*(1-flag) - eps => diff - M*flag >= -M - eps
                allConstraints += LinearInequality(
                    LinearPolynomial(diffMonos + LinearMonomial(-mV, flagVar), diffConst),
                    LinearPolynomial(emptyList(), -mV - epsV),
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

        addConstraints(model, allConstraints)?.let { return it }
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
    }
}
