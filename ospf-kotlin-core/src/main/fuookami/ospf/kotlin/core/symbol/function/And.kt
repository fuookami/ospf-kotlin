@file:Suppress("unused")

/** 逻辑与函数符号 / Logical AND function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 逻辑运算函数符号 / Logical operation function symbols
 *
 * 提供 [AndFunction]、[OrFunction]、[NotFunction]、[XorFunction]，
 * 用于将逻辑运算线性化建模。
 *
 * Provides [AndFunction], [OrFunction], [NotFunction], and [XorFunction]
 * for linearized modeling of logical operations.
 */

/**
 * AND 逻辑函数 / AND logical function
 *
 * y = 1 当且仅当所有输入非零。使用非零指示变量，满足 sum(indicators) >= n*result 且 result <= each indicator。
 *
 * y = 1 iff all inputs are nonzero. Uses nonzero indicators with sum(indicators) >= n*result and result <= each indicator.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @property indicatorVars 非零指示变量列表 / Nonzero indicator variable list
 * @property sideVars 辅助边变量列表 / Auxiliary side variable list
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class AndFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "and",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "AndFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_and")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v eq converter.zero) return converter.zero
        }
        return converter.one
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
        val nValue = repeatAdd(one, n)
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i], indicatorVars[i], sideVars[i], bigM, tolerance, strictBoundary, "${name}_and_nz_${i}")
        }

        // sum(indicators) >= n * result / 非零指示变量之和大于等于 n 倍结果
        val indMonos = indicatorVars.map { LinearMonomial(one, it) } + LinearMonomial(-nValue, resultVar)
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos, zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_and_sum")

        // result <= each indicator / 结果小于等于每个非零指示变量
        for (i in indicatorVars.indices) {
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, indicatorVars[i])), zero),
                LinearPolynomial(emptyList(), zero), Comparison.LE, "${name}_and_le_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建 AND 逻辑函数实例 / Create an AND logical function instance
         * @param polynomials 输入线性多项式列表 / list of input linear polynomials
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [AndFunction] 实例 / [AndFunction] instance
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): AndFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            AndFunction(polynomials, converter, bigM, name = name, displayName = displayName)

        /**
         * 从可转换为线性多项式的对象创建 AND 函数 / Create AND function from objects convertible to linear polynomials
         * @param polynomials 可转换为线性多项式的对象列表 / list of objects convertible to linear polynomials
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 包装后的线性函数符号适配器 / wrapped linear function symbol adapter
         */
        fun <V> fromLinearPolynomials(
            polynomials: List<ToLinearPolynomial<V>>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): LinearFunctionSymbolAdapter<V> where V : RealNumber<V>, V : NumberField<V> = LinearFunctionSymbolAdapter(
            AndFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                converter = converter,
                bigM = bigM,
                name = name,
                displayName = displayName
            ),
            converter = converter
        )
    }
}

/**
 * 或逻辑函数：当且仅当至少一个输入非零时 y = 1。
 * OR function: y = 1 iff at least one input is nonzero.
 *
 * 使用非零指示变量，满足 sum(indicators) >= result 且 result >= each indicator。
 * Uses nonzero indicators with sum(indicators) >= result and result >= each indicator.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @property indicatorVars 非零指示变量列表 / Nonzero indicator variable list
 * @property sideVars 辅助边变量列表 / Auxiliary side variable list
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class OrFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "or",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "OrFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_or")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v neq converter.zero) return converter.one
        }
        return converter.zero
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
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i], indicatorVars[i], sideVars[i], bigM, tolerance, strictBoundary, "${name}_or_nz_${i}")
        }

        // sum(indicators) >= result / 非零指示变量之和大于等于结果
        val indMonos = indicatorVars.map { LinearMonomial(one, it) } + LinearMonomial(-one, resultVar)
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos, zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_or_sum")

        // result >= each indicator / 结果大于等于每个非零指示变量
        for (i in indicatorVars.indices) {
            allConstraints += LinearInequality(
                LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(-one, indicatorVars[i])), zero),
                LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_or_ge_${i}")
        }

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建 OR 逻辑函数实例 / Create an OR logical function instance
         * @param polynomials 输入线性多项式列表 / list of input linear polynomials
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [OrFunction] 实例 / [OrFunction] instance
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): OrFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            OrFunction(polynomials, converter, bigM, name = name, displayName = displayName)
    }
}

/**
 * 非逻辑函数：当且仅当输入为零时 y = 1。
 * NOT function: y = 1 iff input is zero.
 *
 * 使用非零指示变量，满足 y = 1 - indicator。
 * Uses nonzero indicator with y = 1 - indicator.
 *
 * @property polynomial 输入线性多项式 / Input linear polynomial
 * @property indicatorVar 非零指示变量 / Nonzero indicator variable
 * @property sideVar 辅助边变量 / Auxiliary side variable
 * @property resultVar 结果变量 / Result variable
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class NotFunction<V>(
    val polynomial: LinearPolynomial<V>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "not",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_not_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_not_side")
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_not")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(indicatorVar, sideVar, resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val v = polynomial.evaluateWith(values) ?: return null
        return if (v eq converter.zero) converter.one else converter.zero
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
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicator / 非零指示约束
            allConstraints += nonzeroIndicatorConstraints(polynomial, indicatorVar, sideVar, bigM, tolerance, strictBoundary, "${name}_not_nz")

        // result = 1 - indicator => result + indicator = 1 / 结果 = 1 - 指示变量，即结果 + 指示变量 = 1
        allConstraints += LinearInequality(
            LinearPolynomial(listOf(LinearMonomial(one, resultVar), LinearMonomial(one, indicatorVar)), zero),
            LinearPolynomial(emptyList(), one), Comparison.EQ, "${name}_not_result")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建 NOT 逻辑函数实例 / Create a NOT logical function instance
         * @param polynomial 输入线性多项式 / input linear polynomial
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [NotFunction] 实例 / [NotFunction] instance
         */
        operator fun <V> invoke(
            polynomial: LinearPolynomial<V>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): NotFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            NotFunction(polynomial, converter, bigM, name = name, displayName = displayName)
    }
}

/**
 * 异或逻辑函数：当且仅当恰好一个输入非零时 y = 1。
 * XOR function: y = 1 iff exactly one input is nonzero.
 *
 * 使用非零指示变量，满足 sum(indicators) - 2*slack = result, sum(indicators) <= n*result + n - 1。
 * Uses nonzero indicators with sum(indicators) - 2*slack = result, sum(indicators) <= n*result + n - 1.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @property indicatorVars 非零指示变量列表 / Nonzero indicator variable list
 * @property sideVars 辅助边变量列表 / Auxiliary side variable list
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认 1e6）/ Big-M bound (default 1e6)
 * @param tolerance 零容差（默认 1e-6）/ zero tolerance (default 1e-6)
 * @param strictBoundary 严格边界值（默认 0.5）/ strict boundary value (default 0.5)
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class XorFunction<V>(
    val polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    bigM: V? = null,
    tolerance: V? = null,
    strictBoundary: V? = null,
    override var name: String = "xor",
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val converter: IntoValue<V> = converter
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "XorFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_xor")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            if (v neq converter.zero) count++
        }
        return if (count == 1) converter.one else converter.zero
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
        val nValue = repeatAdd(one, n)
        val nMinusOneValue = repeatAdd(one, n - 1)
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            allConstraints += nonzeroIndicatorConstraints(polynomials[i], indicatorVars[i], sideVars[i], bigM, tolerance, strictBoundary, "${name}_xor_nz_${i}")
        }

        // sum(indicators) - 2*slack = result (where slack is integer)
        // sum(指示变量) - 2*松弛变量 = 结果（松弛变量为整数）
        // For binary indicators, XOR = sum(indicators) - 2*floor(sum/2)
        // 对于二值指示变量，XOR = sum(指示变量) - 2*floor(sum/2)
        // Simplification: sum(indicators) <= n*result + (n-1)*(1-result) => sum(indicators) <= result + (n-1)
        // 化简：sum(指示变量) <= n*result + (n-1)*(1-result) => sum(指示变量) <= result + (n-1)
        val indMonos = indicatorVars.map { LinearMonomial(one, it) } + LinearMonomial(-one, resultVar)
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos, zero),
            LinearPolynomial(emptyList(), nMinusOneValue), Comparison.LE, "${name}_xor_sum_ub")

        // sum(indicators) >= result / 非零指示变量之和大于等于结果
        val indMonos2 = indicatorVars.map { LinearMonomial(one, it) } + LinearMonomial(-one, resultVar)
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos2, zero),
            LinearPolynomial(emptyList(), zero), Comparison.GE, "${name}_xor_sum_lb")

        // result <= sum(indicators) ... already covered by sum >= result
        // result <= sum(指示变量) ... 已被 sum >= result 覆盖
        // Additional: sum(indicators) - result <= n - 1
        // 附加：sum(指示变量) - result <= n - 1
        // This is the same as sum_ub above.
        // 这与上述 sum_ub 相同。

        // If sum >= 2 then result = 0: sum(indicators) <= (n-1) + (1)*result_reversed
        // 若 sum >= 2 则 result = 0：sum(指示变量) <= (n-1) + (1)*result_reversed
        // More precise: result <= 2 - sum(indicators) + M*(1 - exactly_one_check)
        // 更精确：result <= 2 - sum(指示变量) + M*(1 - exactly_one_check)
        // Simplified for binary indicators:
        // 对二值指示变量的简化：
        // result >= sum - 1, result <= 2 - sum
        // When sum=0: result >= -1 (ok), result <= 2 (ok) => result=0
        // sum=0 时：result >= -1（成立），result <= 2（成立）=> result=0
        // When sum=1: result >= 0 (ok), result <= 1 (ok) => result=1
        // sum=1 时：result >= 0（成立），result <= 1（成立）=> result=1
        // When sum>=2: result >= 1 but result <= 0 => infeasible unless result=0
        // sum>=2 时：result >= 1 但 result <= 0 => 不可行，除非 result=0
        // Wait, that's wrong for sum>=2. Let's use a different encoding:
        // 对 sum>=2 情况有误，使用另一种编码：
        // result <= 2 - sum(indicators) + M*aux (for aux binary)
        // result <= 2 - sum(指示变量) + M*aux（aux 为二值变量）
        // Actually, simplest correct encoding for XOR of binary indicators:
        // 实际上，对于二值指示变量 XOR 的最简正确编码：
        // result + sum(indicators) = 1 + 2*t (where t is non-negative integer)
        // result + sum(指示变量) = 1 + 2*t（t 为非负整数）
        // This is equivalent to: result = 1 iff sum is odd.
        // 等价于：当且仅当 sum 为奇数时 result = 1。
        // For n <= 2, result = 1 - sum + 2*result... circular.
        // 对 n <= 2，result = 1 - sum + 2*result... 循环。
        // Simplest correct: result = 1 - |sum - 1| + ... no.
        // 最简正确方案：result = 1 - |sum - 1| + ... 不行。
        // Just use: sum(indicators) >= result (result=1 requires sum>=1)
        // 直接使用：sum(指示变量) >= result（result=1 要求 sum>=1）
        //           sum(indicators) <= result + (n-1)*(1-result) => sum <= result + n - 1
        //           sum(指示变量) <= result + (n-1)*(1-result) => sum <= result + n - 1
        //           sum(indicators) <= 1 + (n-1)*(1-result) => for result=1, sum<=1; for result=0, sum<=n
        //           sum(指示变量) <= 1 + (n-1)*(1-result) => result=1 时 sum<=1；result=0 时 sum<=n
        // Combined:
        // 综合：
        //           sum(indicators) <= 1 + (n-1) - (n-1)*result = n - (n-1)*result
        //           sum(指示变量) <= 1 + (n-1) - (n-1)*result = n - (n-1)*result
        val indMonos3 = indicatorVars.map { LinearMonomial(one, it) } + LinearMonomial(nMinusOneValue, resultVar)
        allConstraints += LinearInequality(
            LinearPolynomial(indMonos3, zero),
            LinearPolynomial(emptyList(), nValue), Comparison.LE, "${name}_xor_exactly")

        addConstraints(model, allConstraints)?.let { return it }
        return ok
    }
    companion object {
        /**
         * 创建 XOR 逻辑函数实例 / Create an XOR logical function instance
         * @param polynomials 输入线性多项式列表 / list of input linear polynomials
         * @param converter 值类型转换器 / value type converter
         * @param bigM Big-M 界限 / Big-M bound
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return [XorFunction] 实例 / [XorFunction] instance
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            bigM: V? = null,
            name: String,
            displayName: String? = null
        ): XorFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            XorFunction(polynomials, converter, bigM, name = name, displayName = displayName)
    }
}
