@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

/** 二元基数函数符号 / Binary cardinality function symbols */

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.*

private fun validateCardinalityInputs(
    indicators: List<AbstractVariableItem<*, *>>,
    amount: Int,
    operation: String
): Try {
    if (amount !in 0..indicators.size) {
        return Failed(
            ErrorCode.IllegalArgument,
            "$operation 数量必须在 0 到输入数之间。 / $operation count must be between zero and the input count."
        )
    }
    if (indicators.any { !it.type.isBinaryType }) {
        return Failed(
            ErrorCode.IllegalArgument,
            "$operation 输入必须全部为二元变量。 / $operation inputs must all be binary variables."
        )
    }
    return ok
}

private fun <V> countPolynomial(
    indicators: List<AbstractVariableItem<*, *>>,
    converter: IntoValue<V>
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> = LinearPolynomial(
    indicators.map { LinearMonomial(converter.one, it) },
    converter.zero
)

private fun <V> integerValue(amount: Int, converter: IntoValue<V>): V
    where V : RealNumber<V>, V : NumberField<V> {
    var value = converter.zero
    repeat(amount) { value += converter.one }
    return value
}

private fun <V> countRelation(
    indicators: List<AbstractVariableItem<*, *>>,
    amount: Int,
    comparison: Comparison,
    converter: IntoValue<V>,
    name: String
): LinearInequality<V> where V : RealNumber<V>, V : NumberField<V> = LinearInequality(
    lhs = countPolynomial(indicators, converter),
    rhs = LinearPolynomial(emptyList(), integerValue(amount, converter)),
    comparison = comparison,
    name = name
)

/**
 * 构造纯约束 `sum(indicators) <= limit`，不创建满足指示变量。
 * Builds the pure constraint `sum(indicators) <= limit` without a satisfaction indicator.
 *
 * @param indicators 二元变量列表 / list of binary variables
 * @param limit 最大计数 / maximum count
 * @param converter 数值转换器 / numeric value converter
 * @param name 约束名称 / constraint name
 * @return 单条线性约束或验证错误 / one linear constraint or validation error
 */
fun <V> atMostConstraints(
    indicators: List<AbstractVariableItem<*, *>>,
    limit: Int,
    converter: IntoValue<V>,
    name: String = "at_most"
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = validateCardinalityInputs(indicators, limit, "AtMost")) {
        is Ok -> try {
            Ok(listOf(countRelation(indicators, limit, Comparison.LE, converter, name)))
        } catch (error: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "AtMost 约束构造失败：${error.message ?: "无效参数"} / Failed to build AtMost constraint.")
        }
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 构造纯约束 `sum(indicators) == amount`，不创建满足指示变量。
 * Builds the pure constraint `sum(indicators) == amount` without a satisfaction indicator.
 *
 * @param indicators 二元变量列表 / list of binary variables
 * @param amount 精确计数 / exact count
 * @param converter 数值转换器 / numeric value converter
 * @param name 约束名称 / constraint name
 * @return 单条线性约束或验证错误 / one linear constraint or validation error
 */
fun <V> exactlyConstraints(
    indicators: List<AbstractVariableItem<*, *>>,
    amount: Int,
    converter: IntoValue<V>,
    name: String = "exactly"
): Ret<List<LinearInequality<V>>> where V : RealNumber<V>, V : NumberField<V> {
    return when (val validation = validateCardinalityInputs(indicators, amount, "Exactly")) {
        is Ok -> try {
            Ok(listOf(countRelation(indicators, amount, Comparison.EQ, converter, name)))
        } catch (error: RuntimeException) {
            Failed(ErrorCode.IllegalArgument, "Exactly 约束构造失败：${error.message ?: "无效参数"} / Failed to build Exactly constraint.")
        }
        is Failed -> Failed(validation.error)
        is Fatal -> Fatal(validation.errors)
    }
}

/**
 * 精确指示恰有至多 `limit` 个输入为 1。
 * Exact binary indicator that at most `limit` inputs equal 1.
 *
 * @property indicators 二元变量列表 / list of binary variables
 * @property limit 最大计数 / maximum count
 * @property resultVar 满足指示变量 / satisfaction indicator variable
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class AtMostFunction<V> private constructor(
    val indicators: List<AbstractVariableItem<*, *>>,
    val limit: Int,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    val resultVar: BinVar = BinVar("${name}_satisfied")

    override val helperVariables: List<AbstractVariableItem<*, *>> get() = listOf(resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (indicator in indicators) {
            val value = values[indicator] ?: return null
            when {
                value.compareTo(converter.zero) == 0 -> Unit
                value.compareTo(converter.one) == 0 -> count++
                else -> return null
            }
        }
        return if (count <= limit) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try =
        when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val sum = countPolynomial(indicators, converter)
        val one = converter.one
        val zero = converter.zero
        val output = LinearPolynomial(listOf(LinearMonomial(one, resultVar)), zero)
        val n = indicators.size
        val constraints = if (limit == n) {
            listOf(
                LinearInequality(output, LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_always_true")
            )
        } else {
            val upper = LinearInequality(
                lhs = LinearPolynomial(sum.monomials + LinearMonomial(integerValue(n - limit, converter), resultVar), sum.constant),
                rhs = LinearPolynomial(emptyList(), integerValue(n, converter)),
                comparison = Comparison.LE,
                name = "${name}_upper"
            )
            val lower = LinearInequality(
                lhs = LinearPolynomial(sum.monomials + LinearMonomial(integerValue(limit + 1, converter), resultVar), sum.constant),
                rhs = LinearPolynomial(emptyList(), integerValue(limit + 1, converter)),
                comparison = Comparison.GE,
                name = "${name}_lower"
            )
            listOf(upper, lower)
        }
        return addConstraints(model, constraints) ?: ok
    }

    companion object {
        /** 校验并创建 AtMost 指示函数。 / Validate and create an AtMost indicator function.
         *
         * @param indicators 二元变量列表 / list of binary variables
         * @param limit 最大计数 / maximum count
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            indicators: List<AbstractVariableItem<*, *>>,
            limit: Int,
            converter: IntoValue<V>,
            name: String = "at_most",
            displayName: String? = null
        ): Ret<AtMostFunction<V>> where V : RealNumber<V>, V : NumberField<V> = checkedFunctionConstruction(
            operation = "AtMostFunction",
            validation = validateCardinalityInputs(indicators, limit, "AtMost")
        ) {
            AtMostFunction(indicators.toList(), limit, converter, name, displayName)
        }
    }
}

/**
 * 精确指示恰有 `amount` 个输入为 1。
 * Exact binary indicator that exactly `amount` inputs equal 1.
 *
 * @property indicators 二元变量列表 / list of binary variables
 * @property amount 精确计数 / exact count
 * @property resultVar 满足指示变量 / satisfaction indicator variable
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ExactlyFunction<V> private constructor(
    val indicators: List<AbstractVariableItem<*, *>>,
    val amount: Int,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    val resultVar: BinVar = BinVar("${name}_satisfied")
    private val belowVar: BinVar? = if (amount > 0) BinVar("${name}_below") else null
    private val aboveVar: BinVar? = if (amount < indicators.size) BinVar("${name}_above") else null

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOfNotNull(resultVar, belowVar, aboveVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (indicator in indicators) {
            val value = values[indicator] ?: return null
            when {
                value.compareTo(converter.zero) == 0 -> Unit
                value.compareTo(converter.one) == 0 -> count++
                else -> return null
            }
        }
        return if (count == amount) converter.one else converter.zero
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try =
        when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val sum = countPolynomial(indicators, converter)
        val one = converter.one
        val zero = converter.zero
        val n = indicators.size
        val rows = mutableListOf<LinearInequality<V>>()
        val branchMonomials = mutableListOf(LinearMonomial(one, resultVar))
        belowVar?.let { branchMonomials += LinearMonomial(one, it) }
        aboveVar?.let { branchMonomials += LinearMonomial(one, it) }
        rows += LinearInequality(
            LinearPolynomial(branchMonomials, zero),
            LinearPolynomial(emptyList(), one),
            Comparison.EQ,
            "${name}_one_branch"
        )

        val exactUpper = LinearPolynomial(
            sum.monomials + LinearMonomial(integerValue(n - amount, converter), resultVar),
            sum.constant
        )
        rows += LinearInequality(
            exactUpper,
            LinearPolynomial(emptyList(), integerValue(n, converter)),
            Comparison.LE,
            "${name}_exact_upper"
        )
        val exactLower = LinearPolynomial(
            sum.monomials + LinearMonomial(-integerValue(amount, converter), resultVar),
            sum.constant
        )
        rows += LinearInequality(
            exactLower,
            LinearPolynomial(emptyList(), zero),
            Comparison.GE,
            "${name}_exact_lower"
        )

        belowVar?.let { variable ->
            rows += LinearInequality(
                LinearPolynomial(
                    sum.monomials + LinearMonomial(integerValue(n - amount + 1, converter), variable),
                    sum.constant
                ),
                LinearPolynomial(emptyList(), integerValue(n, converter)),
                Comparison.LE,
                "${name}_below_branch"
            )
        }
        aboveVar?.let { variable ->
            rows += LinearInequality(
                LinearPolynomial(
                    sum.monomials + LinearMonomial(-integerValue(amount + 1, converter), variable),
                    sum.constant
                ),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${name}_above_branch"
            )
        }
        return addConstraints(model, rows) ?: ok
    }

    companion object {
        /** 校验并创建 Exactly 指示函数。 / Validate and create an Exactly indicator function.
         *
         * @param indicators 二元变量列表 / list of binary variables
         * @param amount 精确计数 / exact count
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或验证错误 / function or validation error
         */
        operator fun <V> invoke(
            indicators: List<AbstractVariableItem<*, *>>,
            amount: Int,
            converter: IntoValue<V>,
            name: String = "exactly",
            displayName: String? = null
        ): Ret<ExactlyFunction<V>> where V : RealNumber<V>, V : NumberField<V> = checkedFunctionConstruction(
            operation = "ExactlyFunction",
            validation = validateCardinalityInputs(indicators, amount, "Exactly")
        ) {
            ExactlyFunction(indicators.toList(), amount, converter, name, displayName)
        }
    }
}
