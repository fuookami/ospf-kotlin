@file:Suppress("unused")

/** 满足数量不等式函数符号 / Satisfied amount inequality function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 不等式满足数量函数符号 / Satisfied amount inequality function symbols
 *
 * 提供 [SatisfiedAmountInequalityFunction]、[AnyFunction]、[AllFunction]、
 * [AtLeastInequalityFunction]、[NotAllFunction]、[NumeratorFunction] 等
 * 用于不等式满足数量的线性化建模。
 *
 * Provides [SatisfiedAmountInequalityFunction], [AnyFunction], [AllFunction],
 * [AtLeastInequalityFunction], [NotAllFunction], [NumeratorFunction] and more
 * for linearized modeling of inequality satisfaction counts.
 */

/**
 * 满足数量函数：统计列表中有多少不等式被满足。
 * Satisfied Amount function: counts how many inequalities in a list are satisfied.
 *
 * 给定线性约束列表，此函数：
 * Given a list of linear constraints, this function:
 * - 为每个约束创建二值标志 `u[i]` 表示是否满足
 * - Creates a binary flag `u[i]` for each constraint indicating satisfaction
 * - 返回 `y = sum(u[i])` 作为满足约束的计数
 * - Returns `y = sum(u[i])` as the count of satisfied constraints
 *
 * 当指定 `amount` 时，返回二值指示器：
 * When `amount` is specified, returns a binary indicator:
 * - 若满足约束的数量在 `amount` 范围内则 `y = 1`
 * - `y = 1` if the count of satisfied constraints is within `amount` range
 * - 否则 `y = 0`
 * - `y = 0` otherwise
 *
 * 约束满足度使用 PCT（百分比）公式编码：
 * The constraint satisfaction is encoded using the PCT (Percentage) formulation:
 * 对每个约束，创建 3 个百分比变量 [k0, k1, k2] 在 [lowerBound, 0, upperBound] 之间插值，
 * For each constraint, 3 percentage variables [k0, k1, k2] are created to interpolate
 * between [lowerBound, 0, upperBound], with a binary flag indicating whether 0 is in range.
 * 并用二值标志指示 0 是否在范围内。
 *
 * @property inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @property amount 可选的满足数量范围；若为 null，返回原始计数 / optional range of satisfied count; if null, returns raw count
 * @property epsilon 边界检查的容差 / tolerance for boundary checks
 * @property converter V 类型常量和 Flt64 <-> V 转换的值类型转换器 / value type converter for V-typed constants and Flt64 <-> V conversion
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
open class SatisfiedAmountInequalityFunction<V>(
    val inputs: List<LinearConstraintInput<V>>,
    open val amount: ValueRange<UInt64>? = null,
    val epsilon: V,
    val converter: IntoValue<V>,
    override var name: String = "satisfied_amount",
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    /** 二值标志：每个输入约束一个。 / Binary flags: one per input constraint. */
    private val flagVars: List<AbstractVariableItem<*, *>> by lazy {
        inputs.indices.map { i -> BinVar("${name}_u_$i") }
    }

    /** 指定 amount 时的单个二值输出。 / Single binary output when amount is specified. */
    private val amountFlagVar: AbstractVariableItem<*, *>? by lazy {
        val currentAmount = amount
        if (currentAmount != null) BinVar("${name}_y") else null
    }

    override val helperVariables: List<AbstractVariableItem<*, *>> by lazy {
        buildList {
            addAll(flagVars)
            amountFlagVar?.let { add(it) }
        }
    }

    /**
     * 结果：满足约束标志之和。
     * Result: sum of satisfied constraint flags.
     * 若指定了 amount，则为二值指示器（0 或 1）。
     * If amount is specified, this is a binary indicator (0 or 1).
     */
    val result: LinearPolynomial<V> by lazy {
        val currentAmount = amount
        if (currentAmount != null) {
            LinearPolynomial(
                listOf(LinearMonomial(converter.one, amountFlagVar!!)),
                converter.zero
            )
        } else {
            LinearPolynomial(
                flagVars.map { LinearMonomial(converter.one, it) },
                converter.zero
            )
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (input in inputs) {
            val satisfied = checkInputSatisfied(input, values) ?: return null
            if (satisfied) count++
        }
        val countUInt = UInt64(count)
        val currentAmount = amount
        return if (currentAmount != null) {
            if (currentAmount.contains(countUInt)) converter.one else converter.zero
        } else {
            repeatAdd(converter.one, count)
        }
    }

    /**
     * 检查给定当前值下单个输入约束是否满足。
     * Check whether a single input constraint is satisfied given the current values.
     */
    /**
     * 检查给定当前值下单个输入约束是否满足。
     * Check whether a single input constraint is satisfied given the current values.
     *
     * @param input 约束输入 / the constraint input
     * @param values 符号到值的映射 / symbol-to-value map
     * @return 若满足返回 true，若值缺失返回 null / true if satisfied, null if value missing
     */
    private fun checkInputSatisfied(
        input: LinearConstraintInput<V>,
        values: Map<Symbol, V>
    ): Boolean? {
        var lhsValue = input.flattenData.constant
        for (monomial in input.flattenData.monomials) {
            val symbolValue = values[monomial.symbol]
                ?: return null
            lhsValue += monomial.coefficient * symbolValue
        }
        return input.sign.compare(converter.fromValue(lhsValue), Flt64.zero)
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val eps = epsilon
        val nInputs = inputs.size
        val constraints = mutableListOf<LinearInequality<V>>()

        for ((i, input) in inputs.withIndex()) {
            val lb = input.lhsRange.lowerBound.value.unwrapOrNull()
            val ub = input.lhsRange.upperBound.value.unwrapOrNull()
            val flag = flagVars[i]

            if (lb != null && ub != null) {
                val inRange = zero geq lb && zero leq ub
                if (inRange) {
                    // Simple encoding: when 0 is within [lb, ub],
                    // 简单编码：当 0 在 [lb, ub] 范围内时，
                    // flag = 1 means constraint satisfied (lhs ~ 0 within bounds)
                    // flag = 1 表示约束满足（左侧在边界内接近 0）
                    // flag = 0 means violated
                    // flag = 0 表示违反
                    // Use Big-M: lhs <= M*flag and lhs >= -M*flag when flag=1
                    // 使用 Big-M：flag=1 时 左侧 <= M*flag 且 左侧 >= -M*flag
                    val absMax = if (lb.abs() geq ub.abs()) lb.abs() else ub.abs()
                    val m = ensurePositiveBigM(absMax, converter)

                    val polyMonos = input.flattenData.monomials.map { m2 ->
                        LinearMonomial(m2.coefficient, m2.symbol)
                    }
                    val polyConstant = input.flattenData.constant

                    // When flag=1: lhs <= epsilon and lhs >= -epsilon (satisfied)
                    // flag=1 时：左侧 <= epsilon 且 左侧 >= -epsilon（满足）
                    // When flag=0: lhs <= M and lhs >= -M (no constraint)
                    // flag=0 时：左侧 <= M 且 左侧 >= -M（无约束）
                    val upperLhs = LinearPolynomial(
                        polyMonos + listOf(LinearMonomial(m, flag)),
                        polyConstant
                    )
                    val upperRhs = LinearPolynomial(emptyList(), m + eps)
                    constraints += LinearInequality(
                        upperLhs, upperRhs, Comparison.LE, "${name}_i${i}_upper"
                    )

                    val lowerLhs = LinearPolynomial(
                        polyMonos + listOf(LinearMonomial(-m, flag)),
                        polyConstant
                    )
                    val lowerRhs = LinearPolynomial(emptyList(), -m - eps)
                    constraints += LinearInequality(
                        lowerLhs, lowerRhs, Comparison.GE, "${name}_i${i}_lower"
                    )
                } else {
                    // When 0 is NOT within [lb, ub], the constraint is trivially satisfied or violated
                    // 当 0 不在 [lb, ub] 范围内时，约束平凡满足或平凡违反
                    val triviallySatisfied = when (input.sign) {
                        Comparison.LE, Comparison.LT -> ub ls zero
                        Comparison.GE, Comparison.GT -> lb gr zero
                        Comparison.EQ -> false
                        Comparison.NE -> true
                    }
                    // Fix flag to the trivial value / 将标志固定为平凡值
                    val fixedValue = if (triviallySatisfied) one else zero
                    val poly = LinearPolynomial(listOf(LinearMonomial(one, flag)), zero)
                    val rhs = LinearPolynomial(emptyList(), fixedValue)
                    constraints += LinearInequality(
                        poly, rhs, Comparison.EQ, "${name}_i${i}_flag"
                    )
                }
            }
        }

        // Amount range constraint: lb <= sum(u) <= ub, with binary y indicator
        // 数量范围约束：下界 <= sum(u) <= 上界，带二值 y 指示变量
        val currentAmount = amount
        if (currentAmount != null) {
            val y = amountFlagVar!!
            val sumPoly = LinearPolynomial(
                flagVars.map { LinearMonomial(converter.one, it) },
                converter.zero
            )
            val nAsValue = repeatAdd(one, nInputs)
            val lbValue = repeatAdd(one, currentAmount.lowerBound.value.unwrap().toInt())
            val ubValue = repeatAdd(one, currentAmount.upperBound.value.unwrap().toInt())

            // sum(u) >= amount.lowerBound - n*(1-y) / sum(u) >= amount 下界 - n*(1-y)
            val lbPoly = LinearPolynomial(
                sumPoly.monomials + listOf(LinearMonomial(nAsValue, y)),
                sumPoly.constant
            )
            val lbRhs = LinearPolynomial(
                emptyList(),
                lbValue + nAsValue
            )
            constraints += LinearInequality(
                lbPoly, lbRhs, Comparison.GE, "${name}_amount_lb"
            )

            // sum(u) <= amount.upperBound + n*(1-y) / sum(u) <= amount 上界 + n*(1-y)
            val ubPoly = LinearPolynomial(
                sumPoly.monomials + listOf(LinearMonomial(-nAsValue, y)),
                sumPoly.constant
            )
            val ubRhs = LinearPolynomial(
                emptyList(),
                ubValue + nAsValue
            )
            constraints += LinearInequality(
                ubPoly, ubRhs, Comparison.LE, "${name}_amount_ub"
            )
        }

        return addConstraints(model, constraints) ?: ok
    }
    companion object {
        /** 从 Flt64 容差创建 [SatisfiedAmountInequalityFunction] 实例。 / Create a [SatisfiedAmountInequalityFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            amount: ValueRange<UInt64>? = null,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): SatisfiedAmountInequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            SatisfiedAmountInequalityFunction(
                inputs = inputs,
                amount = amount,
                epsilon = converter.intoValue(epsilon),
                converter = converter,
                name = name,
                displayName = displayName
            )
    }
}

/**
 * 任一满足函数：至少一个不等式必须满足。
 * AnyFunction: at least one inequality must be satisfied.
 *
 * 别名：`amount = [1, n]`
 * Alias: `amount = [1, n]`
 *
 * @param inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @param epsilon 边界检查的容差 / tolerance for boundary checks
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class AnyFunction<V>(
    inputs: List<LinearConstraintInput<V>>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "any",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(UInt64.one, UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /** 从 Flt64 容差创建 [AnyFunction] 实例。 / Create an [AnyFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AnyFunction<V> where V : RealNumber<V>, V : NumberField<V> = AnyFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 全部满足函数：所有不等式必须满足。
 * AllFunction: all inequalities must be satisfied.
 *
 * 别名：`amount = [n, n]`
 * Alias: `amount = [n, n]`
 *
 * @param inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @param epsilon 边界检查的容差 / tolerance for boundary checks
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class AllFunction<V>(
    inputs: List<LinearConstraintInput<V>>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(UInt64(inputs.size), UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /** 从 Flt64 容差创建 [AllFunction] 实例。 / Create an [AllFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AllFunction<V> where V : RealNumber<V>, V : NumberField<V> = AllFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 至少满足函数：至少 k 个不等式必须满足。
 * AtLeastInequalityFunction: at least k inequalities must be satisfied.
 *
 * 别名：`amount = [k, n]`
 * Alias: `amount = [k, n]`
 *
 * @param inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @property k 最小满足数量 / minimum satisfied count
 * @param epsilon 边界检查的容差 / tolerance for boundary checks
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class AtLeastInequalityFunction<V>(
    inputs: List<LinearConstraintInput<V>>,
    val k: UInt64,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "at_least",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = ValueRange(k, UInt64(inputs.size)).value!!,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    init {
        assert(k > UInt64.zero)
        assert(UInt64(inputs.size) >= k)
    }

    companion object {
        /** 从 Flt64 容差创建 [AtLeastInequalityFunction] 实例。 / Create an [AtLeastInequalityFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            k: UInt64,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): AtLeastInequalityFunction<V> where V : RealNumber<V>, V : NumberField<V> = AtLeastInequalityFunction(
            inputs = inputs,
            k = k,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 非全满足函数：不能同时满足所有不等式。
 * NotAllFunction: not all inequalities can be satisfied simultaneously.
 *
 * 别名：`amount = [1, n-1]`
 * Alias: `amount = [1, n-1]`
 *
 * @param inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @param epsilon 边界检查的容差 / tolerance for boundary checks
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class NotAllFunction<V>(
    inputs: List<LinearConstraintInput<V>>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "not_all",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = if (inputs.size > 1) ValueRange(UInt64.one, UInt64(inputs.size - 1)).value!! else null,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /** 从 Flt64 容差创建 [NotAllFunction] 实例。 / Create a [NotAllFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): NotAllFunction<V> where V : RealNumber<V>, V : NumberField<V> = NotAllFunction(
            inputs = inputs,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 可计数函数：满足的不等式数量必须在指定范围内。
 * NumerableFunction: the count of satisfied inequalities must be within a specified range.
 *
 * @param inputs 要检查的约束输入列表 / list of constraint inputs to check
 * @property amount 满足数量的目标范围 / target range of satisfied count
 * @param epsilon 边界检查的容差 / tolerance for boundary checks
 * @param converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class NumerableFunction<V>(
    inputs: List<LinearConstraintInput<V>>,
    override val amount: ValueRange<UInt64>,
    epsilon: V,
    converter: IntoValue<V>,
    override var name: String = "numerable",
    override var displayName: String? = null
) : SatisfiedAmountInequalityFunction<V>(
    inputs = inputs,
    amount = amount,
    epsilon = epsilon,
    converter = converter,
    name = name,
    displayName = displayName
) where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /** 从 Flt64 容差创建 [NumerableFunction] 实例。 / Create a [NumerableFunction] instance from Flt64 epsilon. */
        fun <V> from(
            inputs: List<LinearConstraintInput<V>>,
            amount: ValueRange<UInt64>,
            epsilon: Flt64 = Flt64(1e-6),
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): NumerableFunction<V> where V : RealNumber<V>, V : NumberField<V> = NumerableFunction(
            inputs = inputs,
            amount = amount,
            epsilon = converter.intoValue(epsilon),
            converter = converter,
            name = name,
            displayName = displayName
        )
    }
}
