@file:Suppress("unused")

/** 绝对值函数符号 / Absolute value function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.AbsStructure
import fuookami.ospf.kotlin.core.model.intermediate.generateAbsConstraints
import fuookami.ospf.kotlin.core.model.intermediate.AbsHelperBoundsSnapshot
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

/**
 * 绝对值函数符号 / Absolute value function symbol
 *
 * 提供 [AbsFunction]，实现 y = |x| 的线性化建模。
 *
 * Provides [AbsFunction] for linearized modeling of y = |x|.
 */

/**
 * 绝对值函数 / Absolute value function
 *
 * 实现 y = |x|，其中 x = pos - neg（pos, neg >= 0），y = pos + neg，并使用二进制变量强制正负部互补。 / Implements y = |x| where x = pos - neg (pos, neg >= 0), y = pos + neg,
 * and uses a binary variable to enforce positive/negative-part complementarity.
 *
 * @property polynomial 输入线性多项式 / Input linear polynomial
 * @property resultVar 结果 / Result
 * @property posVar 正部 / Positive part
 * @property negVar 负部 / Negative part
 * @property signVar 正负部选择变量 / positive/negative-part selector
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class AbsFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    override var name: String = "abs",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val inputBounds = polynomial.finiteBounds(converter)
    private val positiveBigM: V = bigM ?: inputBounds?.let {
        ensurePositiveBigM(
            if (it.upper gr converter.zero) it.upper else converter.zero,
            converter
        )
    } ?: polynomial.defaultBigM(converter)
    private val negativeBigM: V = bigM ?: inputBounds?.let {
        ensurePositiveBigM(
            if (it.lower ls converter.zero) -it.lower else converter.zero,
            converter
        )
    } ?: polynomial.defaultBigM(converter)

    val resultVar: URealVar = URealVar("${name}_abs")
    val posVar: URealVar = URealVar("${name}_abs_pos")
    val negVar: URealVar = URealVar("${name}_abs_neg")
    val signVar: BinVar = BinVar("${name}_abs_sign")

    private val capturedHelperBounds: Map<VariableItemKey, AbsHelperBoundsSnapshot>

    init {
        inputBounds?.let {
            resultVar.range.leq(
                converter.fromValue(
                    if ((-it.lower) gr it.upper) -it.lower else it.upper
                )
            )
            posVar.range.leq(converter.fromValue(if (it.upper gr converter.zero) it.upper else converter.zero))
            negVar.range.leq(converter.fromValue(if (it.lower ls converter.zero) -it.lower else converter.zero))
        }
        capturedHelperBounds = mapOf(
            posVar.key to captureHelperBounds(posVar),
            negVar.key to captureHelperBounds(negVar),
            signVar.key to captureHelperBounds(signVar)
        )
    }

    private fun captureHelperBounds(variable: AbstractVariableItem<*, *>): AbsHelperBoundsSnapshot {
        return AbsHelperBoundsSnapshot(
            lower = variable.lowerBound?.value?.unwrap(),
            upper = variable.upperBound?.value?.unwrap()
        )
    }

    override fun deferredStructure(): DeferredFunctionStructure {
        val snapshotInput = LinearPolynomial(
            monomials = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol) },
            constant = polynomial.constant
        )
        return AbsStructure(
            input = snapshotInput,
            resultVariable = resultVar,
            positiveVariable = posVar,
            negativeVariable = negVar,
            signVariable = signVar,
            positiveBigM = positiveBigM,
            negativeBigM = negativeBigM,
            converter = converter,
            name = name,
            capturedHelperBounds = capturedHelperBounds.toMap()
        )
    }

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar, posVar, negVar, signVar)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v ls converter.zero) -v else v
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return when (val result = generateAbsConstraints(
            input = polynomial,
            resultVariable = resultVar,
            positiveVariable = posVar,
            negativeVariable = negVar,
            signVariable = signVar,
            positiveBigM = positiveBigM,
            negativeBigM = negativeBigM,
            converter = converter,
            name = name
        )) {
            is Ok -> addConstraints(model, result.value)?.let { it } ?: ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
    companion object {
        /**
         * 创建绝对值函数实例 / Create an absolute value function instance
         *
         * @param polynomial 输入线性多项式 / input linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [AbsFunction] 实例 / [AbsFunction] instance
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AbsFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AbsFunction(polynomial, converter, bigM, name = name, displayName = displayName)
    }
}
