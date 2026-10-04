@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

/** 次序统计函数符号 / Order statistic function symbols */

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

private fun <V> validateOrderInputs(
    polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    operation: String,
    allowEmpty: Boolean = false
): Ret<List<CapturedLinearPolynomialBounds<V>>> where V : RealNumber<V>, V : NumberField<V> {
    if (!allowEmpty && polynomials.isEmpty()) {
        return Failed(
            fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument,
            "$operation 输入不能为空。 / $operation inputs cannot be empty."
        )
    }
    val snapshots = captureFinitePolynomialBounds(polynomials, converter, operation)
    if (snapshots !is Ok || snapshots.value.isEmpty()) return snapshots
    val lower = snapshots.value.map { it.lower }.reduce { current, value ->
        if (value.compareTo(current) < 0) value else current
    }
    val upper = snapshots.value.map { it.upper }.reduce { current, value ->
        if (value.compareTo(current) > 0) value else current
    }
    return if ((upper - lower).isFinite()) {
        snapshots
    } else {
        Failed(
            fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument,
            "$operation 可用范围宽度不可编码。 / $operation range width is not finite."
        )
    }
}

private fun <V> indexPolynomial(
    selectors: List<AbstractVariableItem<*, *>>,
    converter: IntoValue<V>
): LinearPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    val monomials = mutableListOf<LinearMonomial<V>>()
    for ((index, selector) in selectors.withIndex()) {
        if (index == 0) continue
        var coefficient = converter.zero
        repeat(index) { coefficient += converter.one }
        monomials += LinearMonomial(coefficient, selector)
    }
    return LinearPolynomial(monomials, converter.zero)
}

private data class DescendingOrder<V>(
    val polynomials: List<LinearPolynomial<V>>,
    val functions: List<MathFunctionSymbol<V>>
) where V : RealNumber<V>, V : NumberField<V>

private fun <V> sortDescending(
    polynomials: List<LinearPolynomial<V>>,
    converter: IntoValue<V>,
    name: String
): DescendingOrder<V> where V : RealNumber<V>, V : NumberField<V> {
    val ordered = polynomials.toMutableList()
    val functions = mutableListOf<MathFunctionSymbol<V>>()
    for (pass in 0 until ordered.size - 1) {
        for (index in 0 until ordered.size - pass - 1) {
            val pair = listOf(ordered[index], ordered[index + 1])
            val maximum = MaxFunction(
                polynomials = pair,
                converter = converter,
                name = "${name}_cmp_${pass}_${index}_max"
            )
            val minimum = MinFunction(
                polynomials = pair,
                converter = converter,
                name = "${name}_cmp_${pass}_${index}_min"
            )
            functions += maximum
            functions += minimum
            ordered[index] = maximum.resultPolynomial
            ordered[index + 1] = minimum.resultPolynomial
        }
    }
    return DescendingOrder(ordered, functions)
}

/**
 * 返回最小输入的 0-based 索引；并列最优时模型可选任意下标，evaluate 返回最小下标。
 * Returns the zero-based index of a minimum input. The model allows any tied optimum; evaluate returns the smallest tied index.
 *
 * @property polynomials 输入线性多项式 / input linear polynomials
 * @property selectorVars 最小值选择变量 / minimum selector variables
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ArgMinFunction<V> private constructor(
    val polynomials: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val inner = MinFunction(polynomials, converter = converter, name = name, displayName = displayName)

    val selectorVars: List<AbstractVariableItem<*, *>> get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>> get() = inner.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = indexPolynomial(selectorVars, converter)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var selectedIndex = -1
        var selectedValue: V? = null
        for ((index, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluateWith(values) ?: return null
            if (selectedValue == null || value.compareTo(selectedValue) < 0) {
                selectedIndex = index
                selectedValue = value
            }
        }
        var result = converter.zero
        repeat(selectedIndex) { result += converter.one }
        return result
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "ArgMinFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return inner.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "ArgMinFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return inner.registerConstraints(model)
    }

    companion object {
        /** 校验并创建最小值索引函数。 / Validate and create an argmin function.
         *
         * @param polynomials 输入线性多项式 / input linear polynomials
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或参数错误 / function or argument error
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "argmin",
            displayName: String? = null
        ): Ret<ArgMinFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            return checkedFunctionWithBounds(
                "ArgMinFunction",
                validateOrderInputs(polynomials, converter, "ArgMinFunction")
            ) { bounds ->
                ArgMinFunction(
                    polynomials = bounds.map { it.polynomial },
                    converter = converter,
                    name = name,
                    displayName = displayName,
                    inputBounds = bounds
                )
            }
        }
    }
}

/**
 * 返回最大输入的 0-based 索引；并列最优时模型可选任意下标，evaluate 返回最小下标。
 * Returns the zero-based index of a maximum input. The model allows any tied optimum; evaluate returns the smallest tied index.
 *
 * @property polynomials 输入线性多项式 / input linear polynomials
 * @property selectorVars 最大值选择变量 / maximum selector variables
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class ArgMaxFunction<V> private constructor(
    val polynomials: List<LinearPolynomial<V>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val inner = MaxFunction(polynomials, converter = converter, name = name, displayName = displayName)

    val selectorVars: List<AbstractVariableItem<*, *>> get() = inner.selectorVars

    override val helperVariables: List<AbstractVariableItem<*, *>> get() = inner.helperVariables

    override val resultPolynomial: LinearPolynomial<V> get() = indexPolynomial(selectorVars, converter)

    override fun evaluate(values: Map<Symbol, V>): V? {
        var selectedIndex = -1
        var selectedValue: V? = null
        for ((index, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluateWith(values) ?: return null
            if (selectedValue == null || value.compareTo(selectedValue) > 0) {
                selectedIndex = index
                selectedValue = value
            }
        }
        var result = converter.zero
        repeat(selectedIndex) { result += converter.one }
        return result
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "ArgMaxFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return inner.registerAuxiliaryTokens(tokens)
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "ArgMaxFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return inner.registerConstraints(model)
    }

    companion object {
        /** 校验并创建最大值索引函数。 / Validate and create an argmax function.
         *
         * @param polynomials 输入线性多项式 / input linear polynomials
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或参数错误 / function or argument error
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            converter: IntoValue<V>,
            name: String = "argmax",
            displayName: String? = null
        ): Ret<ArgMaxFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            return checkedFunctionWithBounds(
                "ArgMaxFunction",
                validateOrderInputs(polynomials, converter, "ArgMaxFunction")
            ) { bounds ->
                ArgMaxFunction(
                    polynomials = bounds.map { it.polynomial },
                    converter = converter,
                    name = name,
                    displayName = displayName,
                    inputBounds = bounds
                )
            }
        }
    }
}

/**
 * 计算 0-based 第 k 大输入值；比较交换网络使结果对任意目标方向都精确。
 * Computes the zero-based k-th largest input exactly with a compare-exchange network for either objective direction.
 *
 * @property polynomials 输入线性多项式 / input linear polynomials
 * @property k 从 0 开始的排名 / zero-based rank
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class KthLargestFunction<V> private constructor(
    val polynomials: List<LinearPolynomial<V>>,
    val k: Int,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val sorted = sortDescending(polynomials, converter, name)

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = sorted.functions.flatMap { it.helperVariables }

    override val resultPolynomial: LinearPolynomial<V> get() = sorted.polynomials[k]

    override fun evaluate(values: Map<Symbol, V>): V? {
        val evaluated = polynomials.map { it.evaluateWith(values) ?: return null }
            .sortedWith(kotlin.Comparator<V> { left, right -> right.compareTo(left) })
        return evaluated[k]
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "KthLargestFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "KthLargestFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return registerFunctionsAtomically(model, sorted.functions)
    }

    companion object {
        /** 校验并创建 0-based 第 k 大值函数。 / Validate and create a zero-based k-th largest function.
         *
         * @param polynomials 输入线性多项式 / input linear polynomials
         * @param k 从 0 开始的排名 / zero-based rank
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或参数错误 / function or argument error
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            k: Int,
            converter: IntoValue<V>,
            name: String = "kth_largest",
            displayName: String? = null
        ): Ret<KthLargestFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            if (polynomials.isEmpty() || k !in polynomials.indices) {
                return Failed(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument,
                    "k 必须是输入列表内的 0-based 下标。 / k must be a zero-based index in the input list."
                )
            }
            return checkedFunctionWithBounds(
                "KthLargestFunction",
                validateOrderInputs(polynomials, converter, "KthLargestFunction")
            ) { bounds ->
                KthLargestFunction(
                    polynomials = bounds.map { it.polynomial },
                    k = k,
                    converter = converter,
                    name = name,
                    displayName = displayName,
                    inputBounds = bounds
                )
            }
        }
    }
}

/**
 * 计算前 k 大输入值之和，k 可为 0 或输入个数；结果对任意目标方向精确。
 * Sums the top k inputs, including k=0 or the full input count, exactly for either objective direction.
 *
 * @property polynomials 输入线性多项式 / input linear polynomials
 * @property k 求和项数 / number of terms to sum
 * @property name 函数名称 / function name
 * @property displayName 可选显示名称 / optional display name
 */
class TopKSumFunction<V> private constructor(
    val polynomials: List<LinearPolynomial<V>>,
    val k: Int,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?,
    private val inputBounds: List<CapturedLinearPolynomialBounds<V>>
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    private val sorted = if (k == 0 || k == polynomials.size) {
        DescendingOrder(polynomials, emptyList())
    } else {
        sortDescending(polynomials, converter, name)
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = sorted.functions.flatMap { it.helperVariables }

    override val resultPolynomial: LinearPolynomial<V>
        get() = sumLinearPolynomials(sorted.polynomials.take(k), converter.zero)

    override fun evaluate(values: Map<Symbol, V>): V? {
        val evaluated = polynomials.map { it.evaluateWith(values) ?: return null }
        return evaluated.sortedWith(kotlin.Comparator<V> { left, right -> right.compareTo(left) })
            .take(k)
            .fold(converter.zero) { acc, value -> acc + value }
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "TopKSumFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        when (val validation = validateCapturedPolynomialBounds(inputBounds, converter, "TopKSumFunction")) {
            is Ok -> Unit
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        return registerFunctionsAtomically(model, sorted.functions)
    }

    companion object {
        /** 校验并创建前 k 大值之和函数。 / Validate and create a top-k sum function.
         *
         * @param polynomials 输入线性多项式 / input linear polynomials
         * @param k 求和项数 / number of terms to sum
         * @param converter 数值转换器 / numeric value converter
         * @param name 函数名 / function name
         * @param displayName 可选显示名 / optional display name
         * @return 函数或参数错误 / function or argument error
         */
        operator fun <V> invoke(
            polynomials: List<LinearPolynomial<V>>,
            k: Int,
            converter: IntoValue<V>,
            name: String = "top_k_sum",
            displayName: String? = null
        ): Ret<TopKSumFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            if (k !in 0..polynomials.size) {
                return Failed(
                    fuookami.ospf.kotlin.utils.error.ErrorCode.IllegalArgument,
                    "k 必须在 0 到输入数量之间。 / k must be between zero and the input count."
                )
            }
            val validation = validateOrderInputs(
                polynomials = polynomials,
                converter = converter,
                operation = "TopKSumFunction",
                allowEmpty = true
            )
            return checkedFunctionWithBounds("TopKSumFunction", validation) { bounds ->
                TopKSumFunction(
                    polynomials = bounds.map { it.polynomial },
                    k = k,
                    converter = converter,
                    name = name,
                    displayName = displayName,
                    inputBounds = bounds
                )
            }
        }
    }
}
