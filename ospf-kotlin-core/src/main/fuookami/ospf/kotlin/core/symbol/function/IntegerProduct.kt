@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import kotlin.math.ceil
import kotlin.math.floor
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

private data class IntegerProductPlan<V>(
    val lower: Long,
    val upper: Long,
    val width: Long,
    val lowerValue: V,
    val inputBounds: ConditionBounds<V>,
    val observedInputBounds: ConditionBounds<V>?,
    val resultBounds: ConditionBounds<V>,
    val bitVariables: List<BinVar>,
    val maskedVariables: List<RealVar>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 有界整数变量与有界线性表达式的乘积，采用整数偏移二进制编码精确转成 MILP。
 * [IntegerProductFunction] exactly linearizes a bounded integer variable times a bounded linear expression.
 *
 * 若整数变量范围为 [L, U]，函数令其等于 `L + sum(2^i * bit_i)`，并约束编码不超过 `U-L`；
 * 每个二进制位与输入表达式的乘积都用精确掩码约束表示。
 * The integer is represented as `L + sum(2^i * bit_i)` with an upper-width constraint;
 * each bit times the input expression is represented by exact masking constraints.
 *
 * @property integer 有界整数变量 / bounded integer variable
 * @property input 被乘的线性表达式 / linear expression being multiplied
 * @property resultVar 乘积结果变量 / product result variable
 * @param converter 数值类型转换器 / numeric value converter
 * @param inputBounds 可选的输入表达式有限界 / optional finite bounds for the input expression
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class IntegerProductFunction<V>(
    val integer: AbstractVariableItem<*, *>,
    input: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    private val inputBounds: ConditionBounds<V>? = null,
    override var name: String = "integer_product",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val input = LinearPolynomial(input.monomials.toList(), input.constant)
    private var plan: IntegerProductPlan<V>? = null

    private val resultVariable = RealVar("${name}_integer_product")
    val resultVar: RealVar
        get() {
            resolvePlan()
            return resultVariable
        }

    override val resultPolynomial: LinearPolynomial<V>
        get() {
            resolvePlan()
            return LinearPolynomial(listOf(LinearMonomial(converter.one, resultVariable)), converter.zero)
        }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() {
            val current = resolvePlan() ?: return listOf(resultVariable)
            return listOf(resultVariable) + current.bitVariables + current.maskedVariables
        }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val current = resolvePlan() ?: return null
        val integerValue = values[integer] ?: return null
        return try {
            val integerAsDouble = java.lang.Double.valueOf(converter.fromValue(integerValue).toString())
            if (!integerAsDouble.isFinite() || integerAsDouble != floor(integerAsDouble)) return null
            val integerAsLong = integerAsDouble.toLong()
            if (integerAsLong < current.lower || integerAsLong > current.upper) return null
            val inputValue = input.evaluateWith(values) ?: return null
            if (inputValue.compareTo(current.inputBounds.lower) < 0 || inputValue.compareTo(current.inputBounds.upper) > 0) return null
            integerValue * inputValue
        } catch (_: RuntimeException) {
            null
        }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val current = resolvePlan()
            ?: return integerProductFailure("整数乘积需要有限整数范围和有限输入范围。 / Integer product requires finite integer and input bounds.")
        if (!rangesRemainCovered(current)) {
            return integerProductFailure("整数乘积输入范围超出了已捕获范围。 / Integer product input bounds exceed the captured bounds.")
        }
        return when (val result = tokens.add(listOf(resultVariable) + current.bitVariables + current.maskedVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val current = resolvePlan()
            ?: return integerProductFailure("整数乘积需要有限整数范围和有限输入范围。 / Integer product requires finite integer and input bounds.")
        if (!rangesRemainCovered(current)) {
            return integerProductFailure("整数乘积输入范围超出了已捕获范围。 / Integer product input bounds exceed the captured bounds.")
        }

        val constraints = try {
            buildIntegerProductConstraints(current)
        } catch (_: RuntimeException) {
            return integerProductFailure("整数乘积约束系数无效或溢出。 / Integer product constraint coefficients are invalid or overflowed.")
        } ?: return integerProductFailure("整数乘积编码系数无法表示。 / Integer product encoding coefficients are not representable.")
        if (!constraints.all { row ->
                hasUsableConditionFlattenedValues(row.lhs, converter) &&
                    hasUsableConditionFlattenedValues(row.rhs, converter)
            }) {
            return integerProductFailure("整数乘积约束包含非有限系数。 / Integer product constraints contain non-finite coefficients.")
        }
        return addConstraints(model, constraints) ?: ok
    }

    private fun resolvePlan(): IntegerProductPlan<V>? {
        plan?.let { return it }
        val candidate = buildPlan() ?: return null
        try {
            resultVariable.range.geq(converter.fromValue(candidate.resultBounds.lower))
            resultVariable.range.leq(converter.fromValue(candidate.resultBounds.upper))
            candidate.maskedVariables.forEachIndexed { index, variable ->
                val lower = if (candidate.inputBounds.lower.compareTo(converter.zero) < 0) candidate.inputBounds.lower else converter.zero
                val upper = if (candidate.inputBounds.upper.compareTo(converter.zero) > 0) candidate.inputBounds.upper else converter.zero
                variable.range.geq(converter.fromValue(lower))
                variable.range.leq(converter.fromValue(upper))
            }
        } catch (_: RuntimeException) {
            return null
        }
        plan = candidate
        return candidate
    }

    private fun buildPlan(): IntegerProductPlan<V>? {
        return try {
            val domain = integerDomain() ?: return null
            val observed = input.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
            val expressionBounds = inputBounds ?: observed ?: return null
            if (!validBounds(expressionBounds)) {
                return null
            }
            val width = domain.second - domain.first
            if (width < 0L || width > MAX_EXACT_INTEGER) {
                return null
            }
            val lowerValue = convertInteger(domain.first) ?: return null
            val upperValue = convertInteger(domain.second) ?: return null
            val products = listOf(
                lowerValue * expressionBounds.lower,
                lowerValue * expressionBounds.upper,
                upperValue * expressionBounds.lower,
                upperValue * expressionBounds.upper
            )
            if (products.any { !isUsableConditionBound(it, converter) }) {
                return null
            }
            var resultLower = products.first()
            var resultUpper = products.first()
            for (product in products.drop(1)) {
                if (product.compareTo(resultLower) < 0) resultLower = product
                if (product.compareTo(resultUpper) > 0) resultUpper = product
            }
            val weights = bitWeights(width)
            val bitVariables = weights.indices.map { BinVar("${name}_integer_product_bit_$it") }
            val maskedVariables = weights.indices.map { RealVar("${name}_integer_product_mask_$it") }
            IntegerProductPlan(
                lower = domain.first,
                upper = domain.second,
                width = width,
                lowerValue = lowerValue,
                inputBounds = expressionBounds,
                observedInputBounds = observed,
                resultBounds = ConditionBounds(resultLower, resultUpper),
                bitVariables = bitVariables,
                maskedVariables = maskedVariables
            )
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun integerDomain(): Pair<Long, Long>? {
        return try {
            if (!integer.type.isIntegerType) return null
            val lower = integer.lowerBound?.value?.unwrapOrNull()?.let { java.lang.Double.valueOf(it.toString()) } ?: return null
            val upper = integer.upperBound?.value?.unwrapOrNull()?.let { java.lang.Double.valueOf(it.toString()) } ?: return null
            val maxExactInteger = java.lang.Double.valueOf(MAX_EXACT_INTEGER.toString())
            if (!lower.isFinite() || !upper.isFinite() || lower < -maxExactInteger || upper > maxExactInteger) {
                return null
            }
            val integerLower = ceil(lower)
            val integerUpper = floor(upper)
            if (integerLower > integerUpper) return null
            integerLower.toLong() to integerUpper.toLong()
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun convertInteger(value: Long): V? {
        return try {
            converter.intoValue(Flt64(java.lang.Double.valueOf(value.toString()))).takeIf { isUsableConditionBound(it, converter) }
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun validBounds(bounds: ConditionBounds<V>): Boolean {
        return bounds.lower.compareTo(bounds.upper) <= 0 &&
            isUsableConditionBound(bounds.lower, converter) &&
            isUsableConditionBound(bounds.upper, converter)
    }

    private fun contains(outer: ConditionBounds<V>, inner: ConditionBounds<V>): Boolean {
        return outer.lower.compareTo(inner.lower) <= 0 && outer.upper.compareTo(inner.upper) >= 0
    }

    private fun rangesRemainCovered(current: IntegerProductPlan<V>): Boolean {
        val domain = integerDomain() ?: return false
        if (domain.first < current.lower || domain.second > current.upper) return false
        val actual = input.finiteBounds(converter)?.let { ConditionBounds(it.lower, it.upper) }
        if (actual == null) {
            return current.observedInputBounds == null && inputBounds != null
        }
        return current.observedInputBounds == null || contains(current.observedInputBounds, actual)
    }

    private fun buildIntegerProductConstraints(current: IntegerProductPlan<V>): List<LinearInequality<V>>? {
        val one = converter.one
        val zero = converter.zero
        val weights = bitWeights(current.width)
        val constraints = mutableListOf<LinearInequality<V>>()

        val integerTerms = mutableListOf(LinearMonomial(one, integer))
        val productTerms = mutableListOf<LinearMonomial<V>>()
        val offsetBoundTerms = mutableListOf<LinearMonomial<V>>()

        for (index in weights.indices) {
            val weight = convertInteger(weights[index]) ?: return null
            val bit = current.bitVariables[index]
            val masked = current.maskedVariables[index]
            integerTerms += LinearMonomial(-weight, bit)
            offsetBoundTerms += LinearMonomial(weight, bit)
            productTerms += LinearMonomial(-weight, masked)
            constraints += maskingConstraints(
                input = input,
                lower = current.inputBounds.lower,
                upper = current.inputBounds.upper,
                bit = bit,
                result = masked,
                namePrefix = "${name}_integer_product_mask_$index"
            )
        }

        constraints += LinearInequality(
            lhs = input,
            rhs = LinearPolynomial(emptyList(), current.inputBounds.upper),
            comparison = Comparison.LE,
            name = "${name}_integer_product_input_upper"
        )
        constraints += LinearInequality(
            lhs = input,
            rhs = LinearPolynomial(emptyList(), current.inputBounds.lower),
            comparison = Comparison.GE,
            name = "${name}_integer_product_input_lower"
        )
        constraints += LinearInequality(
            lhs = LinearPolynomial(integerTerms, -current.lowerValue),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ,
            name = "${name}_integer_product_offset"
        )
        constraints += LinearInequality(
            lhs = LinearPolynomial(offsetBoundTerms, zero),
            rhs = LinearPolynomial(emptyList(), convertInteger(current.width) ?: return null),
            comparison = Comparison.LE,
            name = "${name}_integer_product_width"
        )

        val outputTerms = mutableListOf(LinearMonomial(one, resultVariable))
        outputTerms += input.monomials.map { LinearMonomial(-current.lowerValue * it.coefficient, it.symbol) }
        outputTerms += productTerms
        val outputConstant = -current.lowerValue * input.constant
        constraints += LinearInequality(
            lhs = LinearPolynomial(outputTerms, outputConstant),
            rhs = LinearPolynomial(emptyList(), zero),
            comparison = Comparison.EQ,
            name = "${name}_integer_product_result"
        )
        return constraints
    }

    private fun maskingConstraints(
        input: LinearPolynomial<V>,
        lower: V,
        upper: V,
        bit: AbstractVariableItem<*, *>,
        result: AbstractVariableItem<*, *>,
        namePrefix: String
    ): List<LinearInequality<V>> {
        val one = converter.one
        val zero = converter.zero
        val resultMonomial = LinearMonomial(one, result)
        val negativeInput = input.monomials.map { LinearMonomial(-it.coefficient, it.symbol) }
        return listOf(
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial, LinearMonomial(-upper, bit)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.LE,
                "${namePrefix}_zero_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial, LinearMonomial(-lower, bit)), zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${namePrefix}_zero_lower"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial) + negativeInput + LinearMonomial(-lower, bit), -input.constant),
                LinearPolynomial(emptyList(), -lower),
                Comparison.LE,
                "${namePrefix}_one_upper"
            ),
            LinearInequality(
                LinearPolynomial(listOf(resultMonomial) + negativeInput + LinearMonomial(-upper, bit), -input.constant),
                LinearPolynomial(emptyList(), -upper),
                Comparison.GE,
                "${namePrefix}_one_lower"
            )
        )
    }

    companion object {
        private const val MAX_EXACT_INTEGER = 9_007_199_254_740_992L

        /** 创建整数乘积函数符号。 / Create an integer-product function symbol.
         * @param integer 有界整数变量 / bounded integer variable
         * @param input 被乘的线性表达式 / linear expression being multiplied
         * @param converter 数值类型转换器 / numeric value converter
         * @param inputBounds 可选的输入表达式有限界 / optional finite bounds for the input expression
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 整数乘积函数符号 / integer-product function symbol
         */
        operator fun <V> invoke(
            integer: AbstractVariableItem<*, *>,
            input: LinearPolynomial<V>,
            converter: IntoValue<V>,
            inputBounds: ConditionBounds<V>? = null,
            name: String = "integer_product",
            displayName: String? = null
        ): IntegerProductFunction<V> where V : RealNumber<V>, V : NumberField<V> {
            return IntegerProductFunction(
                integer = integer,
                input = input,
                converter = converter,
                inputBounds = inputBounds,
                name = name,
                displayName = displayName
            )
        }

        private fun bitWeights(width: Long): List<Long> {
            if (width <= 0L) return emptyList()
            val count = 64 - java.lang.Long.numberOfLeadingZeros(width)
            return (0 until count).map { 1L shl it }
        }
    }
}

private fun <T> integerProductFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
