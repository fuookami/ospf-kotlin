@file:Suppress("unused")

/** 二值化函数符号 / Binaryzation function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.IndicatorStructure
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

/**
 * 二值化函数符号 / Binaryzation function symbol
 *
 * 提供 [BinaryzationFunction]，使用 Big-M 方法将连续变量转换为二值变量。
 *
 * Provides [BinaryzationFunction] for converting continuous variables to binary using Big-M.
 */

/**
 * 二值化函数：使用 Big-M 方法将连续变量转换为二值变量。 / Binaryzation function: converts a continuous variable to binary using Big-M.
 *
 * 当 x >= tolerance 时 y = 1，x <= 0 时 y = 0，中间区间不可行。
 * y = 1 if x >= tolerance, y = 0 if x <= 0; the gap is infeasible.
 *
 * @property polynomial 输入线性多项式 / Input linear polynomial
 * @property resultVar 结果变量 / Result variable
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 * @param tolerance 正分支间隔，需结合求解器容差和模型尺度设置；默认保留 NONZERO_TOLERANCE。
 * Positive-branch gap, chosen for solver tolerances and model scaling; defaults to NONZERO_TOLERANCE.
 */
class BinaryzationFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "bin",
    override var displayName: String? = null,
    tolerance: V? = null
) : MathFunctionSymbol<V>, HasResultVariable, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val capturedInputBounds = if (bigM == null) polynomial.finiteBounds(converter) else null
    private val bigM: V = bigM ?: polynomial.defaultBigM(converter)
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))

    override val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_bin")

    override val resultPolynomial: LinearPolynomial<V> by lazy {
        LinearPolynomial(
            monomials = listOf(LinearMonomial(converter.one, resultVar)),
            constant = converter.zero
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val value = polynomial.evaluateWith(values) ?: return null
        return when (val classification = classify(
            d = value,
            relation = Comparison.GT,
            strictBoundary = tolerance
        )) {
            is Ok -> when (classification.value) {
                TruthValue.True -> converter.one
                TruthValue.False -> converter.zero
                TruthValue.Undefined -> null
            }
            is Failed, is Fatal -> null
        }
    }

    override fun deferredStructure(): IndicatorStructure<V> {
        return IndicatorStructure(
            input = LinearPolynomial(polynomial.monomials.toList(), polynomial.constant),
            resultVariable = resultVar,
            bigM = bigM,
            tolerance = tolerance,
            converter = converter,
            name = "${name}_bin",
            capturedInputBounds = capturedInputBounds
        )
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val constraints = deferredStructure().generateConstraints()) {
            is Ok -> Unit
            is Failed -> return Failed(constraints.error)
            is Fatal -> return Fatal(constraints.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val allConstraints = when (val result = deferredStructure().generateConstraints()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建二值化函数实例 / Create a binaryzation function instance
         *
         * @param polynomial 输入线性多项式 / input linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param tolerance 正分支间隔 / positive-branch gap
         * @return [BinaryzationFunction] 实例 / [BinaryzationFunction] instance
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null,
            tolerance: V? = null
        ): BinaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BinaryzationFunction(
                polynomial = polynomial,
                converter = converter,
                bigM = bigM,
                name = name,
                displayName = displayName,
                tolerance = tolerance
            )
    }
}
