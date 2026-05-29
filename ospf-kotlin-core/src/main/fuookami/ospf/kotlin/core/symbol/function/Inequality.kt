/** 不等式函数符号 / Inequality function symbol */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 不等式满足指示函数符号 / Inequality satisfaction indicator function symbol
 *
 * 提供 [InequalityFunction]，判断不等式是否满足并返回二值指示变量。
 *
 * Provides [InequalityFunction] for checking inequality satisfaction and returning a binary indicator.
 */

/**
 * 不等式满足指示函数。
 * Inequality satisfaction indicator function.
 *
 * 给定线性表达式和比较类型，返回：
 * Given a linear expression and a comparison type, returns:
 * - 1 若不等式满足 / if the inequality is satisfied
 * - 0 若不等式违反 / if the inequality is violated
 *
 * @param lhs 左侧线性多项式 / the left-hand side linear polynomial
 * @param rhs 右侧常数值 / the right-hand side constant value
 * @param sign 比较类型 / the comparison type
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @param name 此函数的唯一名称 / unique name for this function
 * @param displayName 可选的人类可读显示名称 / optional human-readable display name
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

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val bigMValue = bigM
        val toleranceValue = tolerance
        val rhsValue = rhs
        val lhsMonos = lhs.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        val allConstraints = mutableListOf<LinearInequality<V>>()

        when (sign) {
            Comparison.LE, Comparison.LT -> {
                // lhs <= rhs + M*(1-flag)  =>  lhs + M*flag <= rhs + M
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(bigMValue, flagVar), lhs.constant),
                    LinearPolynomial(emptyList(), rhsValue + bigMValue),
                    Comparison.LE, "${name}_satisfied"
                )

                // lhs + M*(1-flag) >= rhs + eps  =>  lhs + M - M*flag >= rhs + eps
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(-bigMValue, flagVar), lhs.constant + bigMValue),
                    LinearPolynomial(emptyList(), rhsValue + toleranceValue),
                    Comparison.GE, "${name}_violated"
                )
            }

            Comparison.GE, Comparison.GT -> {
                // lhs >= rhs - M*(1-flag) => lhs + M - M*flag >= rhs
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(-bigMValue, flagVar), lhs.constant + bigMValue),
                    LinearPolynomial(emptyList(), rhsValue),
                    Comparison.GE, "${name}_satisfied"
                )

                // lhs <= rhs - eps + M*flag => lhs - M*flag <= rhs - eps
                allConstraints += LinearInequality(
                    LinearPolynomial(lhsMonos + LinearMonomial(bigMValue, flagVar), lhs.constant),
                    LinearPolynomial(emptyList(), rhsValue - toleranceValue + bigMValue),
                    Comparison.LE, "${name}_violated"
                )
            }

            Comparison.EQ -> {
                val diffMonos = lhsMonos
                val diffConst = lhs.constant - rhsValue

                // diff <= M*(1-flag) + eps => diff + M*flag <= M + eps
                allConstraints += LinearInequality(
                    LinearPolynomial(diffMonos + LinearMonomial(bigMValue, flagVar), diffConst),
                    LinearPolynomial(emptyList(), bigMValue + toleranceValue),
                    Comparison.LE, "${name}_eq_upper"
                )

                // diff >= -M*(1-flag) - eps => diff - M*flag >= -M - eps
                allConstraints += LinearInequality(
                    LinearPolynomial(diffMonos + LinearMonomial(-bigMValue, flagVar), diffConst),
                    LinearPolynomial(emptyList(), -bigMValue - toleranceValue),
                    Comparison.GE, "${name}_eq_lower"
                )
            }

            Comparison.NE -> {
                return Failed(
                    Err(
                        ErrorCode.ApplicationFailed,
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
