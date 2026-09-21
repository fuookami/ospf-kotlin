@file:Suppress("unused")

/** 平衡三值化函数符号 / Balanced ternaryzation function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 平衡三值化函数符号 / Balance ternaryzation function symbol
 *
 * 提供 [BalanceTernaryzationFunction]，将输入映射到 {-1, 0, 1}。
 *
 * Provides [BalanceTernaryzationFunction] for mapping inputs to {-1, 0, 1}.
 */

/**
 * 平衡三值化函数：将 x 映射为 sign(x) 取值 {-1, 0, 1}。 / Balance Ternaryzation function: maps x to sign(x) in {-1, 0, 1}.
 *
 * 输出：
 * - y = 1  当 x >= epsilon + delta
 * - y = 0  当 -epsilon <= x <= epsilon
 * - y = -1 当 x <= -epsilon - delta
 * 零带外紧邻边界的开区间未定义（返回 `null`），以便 evaluator 与求解器约束
 * 使用相同的严格边界语义；其中 delta = strictBoundary，默认 1e-10。
 *
 * Output:
 * - y = 1  when x >= epsilon + delta
 * - y = 0  when -epsilon <= x <= epsilon
 * - y = -1 when x <= -epsilon - delta
 * Values in the open transition gaps immediately outside the zero band are
 * undefined (`null`) so that evaluator and solver rows share strict-boundary
 * semantics; delta = strictBoundary, defaulting to 1e-10.
 *
 * 使用正、负两个互斥指示变量的精确三状态线性化。
 * Uses an exact three-state linearization with mutually exclusive positive and negative indicators.
 *
 * @property x 输入线性多项式 / the input linear polynomial
 * @property epsilon 零阈值（默认 1e-6）/ zero threshold (default 1e-6)
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 * @property strictBoundary 双侧严格间隔 / Strict gap on both sides
 * @property fallbackBigM 输入无有限界时使用的回退 Big-M / fallback Big-M used when the input has no finite bounds
 * @property resultVar 结果变量 / result variable
 * @property positiveVar 正分支指示变量 / positive-branch indicator variable
 * @property negativeVar 负分支指示变量 / negative-branch indicator variable
 * @property result 结果线性多项式 / result linear polynomial
 * @property helperVariables 辅助变量集合 / helper variable collection
 */
class BalanceTernaryzationFunction<V>(
    val x: LinearPolynomial<V>,
    val epsilon: Flt64 = Flt64(1e-6),
    private val converter: IntoValue<V>,
    override var name: String = "bter",
    override var displayName: String? = null,
    val fallbackBigM: Flt64 = Flt64(1e6),
    val strictBoundary: Flt64 = Flt64(NONZERO_TOLERANCE)
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    init {
        require(epsilon geq Flt64.zero) {
            "BalanceTernaryzation epsilon must be non-negative"
        }
        require(fallbackBigM gr epsilon + strictBoundary) {
            "BalanceTernaryzation fallbackBigM must exceed epsilon plus the strict boundary"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_bter")
    val positiveVar: AbstractVariableItem<*, *> = BinVar("${name}_bter_positive")
    val negativeVar: AbstractVariableItem<*, *> = BinVar("${name}_bter_negative")

    val result: LinearPolynomial<V>
        get() = LinearPolynomial(
            listOf(LinearMonomial(converter.one, resultVar)), converter.zero
        )

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, positiveVar, negativeVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xValue = x.evaluateWith(values) ?: return null
        val epsilonValue = converter.intoValue(epsilon)
        val strictBoundary = converter.intoValue(strictBoundary)
        val minusOne = converter.intoValue(Flt64(-1.0))
        return when {
            xValue geq epsilonValue + strictBoundary -> converter.one
            xValue gr epsilonValue -> null
            xValue leq -epsilonValue - strictBoundary -> minusOne
            xValue ls -epsilonValue -> null
            else -> converter.zero
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        if (!strictBoundary.isFinite() || !(strictBoundary gr Flt64.zero)) {
            return Failed(ErrorCode.IllegalArgument, "严格间隔必须有限且为正。 / Strict gap must be finite and positive.")
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun deferredStructure(): IndicatorStructure<V>? {
        return try {
            val structure = indicatorStructure()
            if (structure.generateConstraints() is Ok) structure else null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun indicatorStructure(): IndicatorStructure<V> {
        val zero = converter.zero
        val eps = converter.intoValue(epsilon)
        val delta = converter.intoValue(strictBoundary)
        val bounds = x.finiteBounds(converter)
        val bigM = if (bounds != null) {
            val lowerMagnitude = if (bounds.lower ls zero) -bounds.lower else bounds.lower
            val upperMagnitude = if (bounds.upper ls zero) -bounds.upper else bounds.upper
            val magnitude = if (lowerMagnitude gr upperMagnitude) lowerMagnitude else upperMagnitude
            magnitude + eps + delta
        } else {
            converter.intoValue(fallbackBigM)
        }
        val conditionBounds = ConditionBounds(delta - bigM, bigM)
        val negative = IndicatorStructure(
            input = LinearPolynomial(
                monomials = x.monomials.map { LinearMonomial(-it.coefficient, it.symbol) },
                constant = -x.constant - eps
            ),
            resultVariable = negativeVar,
            bigM = bigM,
            tolerance = delta,
            converter = converter,
            name = "${name}_bter_negative",
            conditionBounds = conditionBounds
        )
        return IndicatorStructure(
            input = LinearPolynomial(x.monomials.toList(), x.constant - eps),
            resultVariable = positiveVar,
            bigM = bigM,
            tolerance = delta,
            converter = converter,
            name = "${name}_bter_positive",
            conditionBounds = conditionBounds,
            difference = DifferenceIndicator(
                condition = negative,
                resultVariable = resultVar,
                name = name
            )
        )
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return try {
            when (val generated = indicatorStructure().generateConstraints()) {
                is Ok -> addConstraints(model, generated.value) ?: ok
                is Failed -> Failed(generated.error)
                is Fatal -> Fatal(generated.errors)
            }
        } catch (_: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "三值化约束生成失败。 / Failed to generate ternary constraints.")
        }
    }

    companion object {
        /**
         * 创建平衡三值化函数实例 / Create a balance ternaryzation function instance
         *
         * @param x 输入线性多项式 / input linear polynomial
         * @param epsilon 零阈值 / zero threshold
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @param strictBoundary 双侧严格间隔 / Strict gap on both sides
         * @param fallbackBigM 输入无有限界时使用的回退 Big-M / fallback Big-M used when the input has no finite bounds
         * @return [BalanceTernaryzationFunction] 实例 / [BalanceTernaryzationFunction] instance
         */
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null,
            fallbackBigM: Flt64 = Flt64(1e6),
            strictBoundary: Flt64 = Flt64(NONZERO_TOLERANCE)
        ): BalanceTernaryzationFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BalanceTernaryzationFunction(
                x = x,
                epsilon = epsilon,
                converter = converter,
                name = name,
                displayName = displayName,
                fallbackBigM = fallbackBigM,
                strictBoundary = strictBoundary
            )
    }
}
