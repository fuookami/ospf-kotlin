@file:Suppress("unused")

/** 逻辑与函数符号 / Logical AND function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicOperation
import fuookami.ospf.kotlin.core.model.intermediate.BinaryLogicStructure
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

private fun <V> LinearPolynomial<V>.binaryVariableOrNull(converter: IntoValue<V>): BinVar?
        where V : RealNumber<V>, V : NumberField<V> {
    if (constant.compareTo(converter.zero) != 0 || monomials.size != 1) {
        return null
    }
    val monomial = monomials.single()
    return if (monomial.coefficient.compareTo(converter.one) == 0) {
        monomial.symbol as? BinVar
    } else {
        null
    }
}

/**
 * 逻辑运算函数符号 / Logical operation function symbols
 *
 * 提供 [AndFunction]、[OrFunction]、[NotFunction]、[XorFunction]，
 * 用于将逻辑运算线性化建模。 / Provides [AndFunction], [OrFunction], [NotFunction], and [XorFunction]
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
 * @param bigM Big-M 界限（默认从每个输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from each input range by default, falls back to 1e6)
 * @param tolerance 零容差（默认 1e-10）/ zero tolerance (default 1e-10)
 * @param strictBoundary 严格边界值（默认约 1.6e-9）/ strict boundary value (default about 1.6e-9)
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
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "AndFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_and")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_and_side${it}") }
    private val binaryInputs: List<BinVar>? by lazy {
        polynomials.map { it.binaryVariableOrNull(converter) }.let { inputs ->
            inputs.takeIf { values -> values.all { it != null } }?.map { it!! }
        }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = if (binaryInputs != null) listOf(resultVar) else listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure() = binaryInputs?.let {
        BinaryLogicStructure(BinaryLogicOperation.And, it, resultVar, converter, name)
    }

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

        binaryInputs?.let { inputs ->
            for ((i, input) in inputs.withIndex()) {
                allConstraints += LinearInequality(
                    LinearPolynomial(
                        listOf(
                            LinearMonomial(one, resultVar),
                            LinearMonomial(-one, input)
                        ),
                        zero
                    ),
                    LinearPolynomial(emptyList(), zero),
                    Comparison.LE,
                    "${name}_binary_le_$i"
                )
            }
            allConstraints += LinearInequality(
                LinearPolynomial(
                    inputs.map { LinearMonomial(one, it) } + LinearMonomial(-nValue, resultVar),
                    zero
                ),
                LinearPolynomial(emptyList(), one - nValue),
                Comparison.GE,
                "${name}_binary_sum"
            )
            addConstraints(model, allConstraints)?.let { return it }
            return ok
        }

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            val currentBigM = explicitBigM ?: polynomials[i].defaultBigM(converter)
            when (val result = safeNonzeroIndicatorConstraints(
                poly = polynomials[i],
                indVar = indicatorVars[i],
                sideVar = sideVars[i],
                bigM = currentBigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_and_nz_${i}"
            )) {
                is Ok -> allConstraints += result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
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
         * @param tolerance 零容差 / zero tolerance
         * @param strictBoundary 非零严格边界 / strict nonzero boundary
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
 * 或逻辑函数：当且仅当至少一个输入非零时 y = 1。 / OR function: y = 1 iff at least one input is nonzero.
 *
 * 使用非零指示变量，满足 sum(indicators) >= result 且 result >= each indicator。
 * Uses nonzero indicators with sum(indicators) >= result and result >= each indicator.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @property indicatorVars 非零指示变量列表 / Nonzero indicator variable list
 * @property sideVars 辅助边变量列表 / Auxiliary side variable list
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从每个输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from each input range by default, falls back to 1e6)
 * @param tolerance 零容差（默认 1e-10）/ zero tolerance (default 1e-10)
 * @param strictBoundary 严格边界值（默认约 1.6e-9）/ strict boundary value (default about 1.6e-9)
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
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "OrFunction requires at least one input polynomial" }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_or")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_or_side${it}") }
    private val binaryInputs: List<BinVar>? by lazy {
        polynomials.map { it.binaryVariableOrNull(converter) }.let { inputs ->
            inputs.takeIf { values -> values.all { it != null } }?.map { it!! }
        }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = if (binaryInputs != null) listOf(resultVar) else listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure() = binaryInputs?.let {
        BinaryLogicStructure(BinaryLogicOperation.Or, it, resultVar, converter, name)
    }

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

        binaryInputs?.let { inputs ->
            for ((i, input) in inputs.withIndex()) {
                allConstraints += LinearInequality(
                    LinearPolynomial(
                        listOf(
                            LinearMonomial(one, resultVar),
                            LinearMonomial(-one, input)
                        ),
                        zero
                    ),
                    LinearPolynomial(emptyList(), zero),
                    Comparison.GE,
                    "${name}_binary_ge_$i"
                )
            }
            allConstraints += LinearInequality(
                LinearPolynomial(
                    inputs.map { LinearMonomial(one, it) } + LinearMonomial(-one, resultVar),
                    zero
                ),
                LinearPolynomial(emptyList(), zero),
                Comparison.LE,
                "${name}_binary_sum"
            )
            addConstraints(model, allConstraints)?.let { return it }
            return ok
        }

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            val currentBigM = explicitBigM ?: polynomials[i].defaultBigM(converter)
            when (val result = safeNonzeroIndicatorConstraints(
                poly = polynomials[i],
                indVar = indicatorVars[i],
                sideVar = sideVars[i],
                bigM = currentBigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_or_nz_${i}"
            )) {
                is Ok -> allConstraints += result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
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
 * 非逻辑函数：当且仅当输入为零时 y = 1。 / NOT function: y = 1 iff input is zero.
 *
 * 使用非零指示变量，满足 y = 1 - indicator。 / Uses nonzero indicator with y = 1 - indicator.
 *
 * @property polynomial 输入线性多项式 / Input linear polynomial
 * @property indicatorVar 非零指示变量 / Nonzero indicator variable
 * @property sideVar 辅助边变量 / Auxiliary side variable
 * @property resultVar 结果变量 / Result variable
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from input range by default, falls back to 1e6)
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
    private val bigM: V = bigM ?: polynomial.defaultBigM(converter)
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))

    val indicatorVar: AbstractVariableItem<*, *> = BinVar("${name}_not_nz")
    val sideVar: AbstractVariableItem<*, *> = BinVar("${name}_not_side")
    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_not")
    private val binaryInput: BinVar? by lazy { polynomial.binaryVariableOrNull(converter) }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = if (binaryInput != null) listOf(resultVar) else listOf(indicatorVar, sideVar, resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure() = binaryInput?.let {
        BinaryLogicStructure(BinaryLogicOperation.Not, listOf(it), resultVar, converter, name)
    }

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

        binaryInput?.let { input ->
            allConstraints += LinearInequality(
                LinearPolynomial(
                    listOf(
                        LinearMonomial(one, resultVar),
                        LinearMonomial(one, input)
                    ),
                    zero
                ),
                LinearPolynomial(emptyList(), one),
                Comparison.EQ,
                "${name}_binary_result"
            )
            addConstraints(model, allConstraints)?.let { return it }
            return ok
        }

        // Nonzero indicator / 非零指示约束
        when (val result = safeNonzeroIndicatorConstraints(
            poly = polynomial,
            indVar = indicatorVar,
            sideVar = sideVar,
            bigM = bigM,
            tolerance = tolerance,
            strictBoundary = strictBoundary,
            namePrefix = "${name}_not_nz"
        )) {
            is Ok -> allConstraints += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

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
 * 异或逻辑函数：当且仅当恰好一个输入非零时 y = 1。 / XOR function: y = 1 iff exactly one input is nonzero.
 *
 * 使用非零指示变量，并精确强制结果等价于“恰好一个指示变量为 1”。
 * Uses nonzero indicators and exactly enforces that the result is one iff exactly one indicator is one.
 *
 * @property polynomials 输入线性多项式列表 / List of input linear polynomials
 * @property resultVar 结果变量 / Result variable
 * @property indicatorVars 非零指示变量列表 / Nonzero indicator variable list
 * @property sideVars 辅助边变量列表 / Auxiliary side variable list
 * @param converter 值类型转换器 / value type converter
 * @param bigM Big-M 界限（默认从每个输入范围推导，失败时回退到 1e6）/ Big-M bound (inferred from each input range by default, falls back to 1e6)
 * @param tolerance 零容差（默认 1e-10）/ zero tolerance (default 1e-10)
 * @param strictBoundary 严格边界值（默认约 1.6e-9）/ strict boundary value (default about 1.6e-9)
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
    private val explicitBigM: V? = bigM
    private val tolerance: V = tolerance ?: converter.intoValue(Flt64(NONZERO_TOLERANCE))
    private val strictBoundary: V = strictBoundary ?: converter.intoValue(Flt64(STRICT_BOUNDARY))
    private val n = polynomials.size

    init {
        require(n >= 1) { "XorFunction requires at least one input polynomial" }
        require(this.tolerance geq converter.zero) { "XorFunction tolerance must be non-negative" }
        require(this.strictBoundary gr this.tolerance) {
            "XorFunction strict boundary must exceed tolerance"
        }
    }

    val resultVar: AbstractVariableItem<*, *> = BinVar("${name}_xor")
    val indicatorVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_nz${it}") }
    val sideVars: List<AbstractVariableItem<*, *>> = (0 until n).map { BinVar("${name}_xor_side${it}") }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + indicatorVars + sideVars

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure() = polynomials.map { it.binaryVariableOrNull(converter) }.let { inputs ->
        inputs.takeIf { it.all { variable -> variable != null } }?.map { it!! }?.let {
            BinaryLogicStructure(BinaryLogicOperation.Xor, it, resultVar, converter, name)
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        var count = 0
        for (poly in polynomials) {
            val v = poly.evaluateWith(values) ?: return null
            val magnitude = v.abs()
            if (magnitude ls strictBoundary && magnitude gr tolerance) {
                return null
            }
            if (magnitude geq strictBoundary) count++
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
        val two = one + one
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // Nonzero indicators for each polynomial / 为每个多项式构建非零指示约束
        for (i in polynomials.indices) {
            val currentBigM = explicitBigM ?: polynomials[i].defaultBigM(converter)
            when (val result = safeNonzeroIndicatorConstraints(
                poly = polynomials[i],
                indVar = indicatorVars[i],
                sideVar = sideVars[i],
                bigM = currentBigM,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                namePrefix = "${name}_xor_nz_${i}"
            )) {
                is Ok -> allConstraints += result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        // y <= sum(a_i). / y 不得超过非零指示变量之和。
        allConstraints += LinearInequality(
            LinearPolynomial(
                listOf(LinearMonomial(one, resultVar)) +
                    indicatorVars.map { LinearMonomial(-one, it) },
                zero
            ),
            LinearPolynomial(emptyList(), zero),
            Comparison.LE,
            "${name}_xor_sum_ub"
        )

        // y >= a_i - sum_{j != i}(a_j).  If exactly a_i is one, this forces y = 1.
        // 若只有 a_i 为 1，则该行强制 y = 1。
        for (i in indicatorVars.indices) {
            val monomials = buildList {
                add(LinearMonomial(one, resultVar))
                indicatorVars.forEachIndexed { j, variable ->
                    add(LinearMonomial(if (i == j) -one else one, variable))
                }
            }
            allConstraints += LinearInequality(
                LinearPolynomial(monomials, zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.GE,
                "${name}_xor_single_${i}"
            )
        }

        // Any selected pair forces y = 0: y + a_i + a_j <= 2.
        // 任意两个指示变量同时为 1 时，强制 y = 0。
        for (i in indicatorVars.indices) {
            for (j in (i + 1) until indicatorVars.size) {
                allConstraints += LinearInequality(
                    LinearPolynomial(
                        listOf(
                            LinearMonomial(one, resultVar),
                            LinearMonomial(one, indicatorVars[i]),
                            LinearMonomial(one, indicatorVars[j])
                        ),
                        zero
                    ),
                    LinearPolynomial(emptyList(), two),
                    Comparison.LE,
                    "${name}_xor_pair_${i}_${j}"
                )
            }
        }

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
            tolerance: V? = null,
            strictBoundary: V? = null,
            name: String,
            displayName: String? = null
        ): XorFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            XorFunction(
                polynomials,
                converter,
                bigM,
                tolerance,
                strictBoundary,
                name,
                displayName
            )
    }
}
