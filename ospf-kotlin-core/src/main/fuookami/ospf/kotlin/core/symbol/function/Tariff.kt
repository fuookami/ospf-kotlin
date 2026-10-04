@file:Suppress("unused")

/** 阶梯计费与固定启动费函数 / Tiered tariff and fixed-charge functions. */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.intermediate.DeferredFunctionStructure
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.variable.*

/**
 * 增量（marginal）阶梯费率工厂。每段只对该段内新增的数量计价，因此构造的总费用在断点处连续。
 *
 * Incremental marginal-rate tariff factory. Each rate prices only the quantity added within that tier, so the resulting total cost is continuous at breakpoints.
 */
object IncrementalTariff {
    /**
     * 由输入断点及对应边际费率构造连续总费用 PWL。
     * 首个断点的累计费用为 [baseCost]，随后按各段边际费率累计。
     *
     * Build a continuous total-cost PWL from input breakpoints and the corresponding marginal rates.
     * The cumulative cost at the first breakpoint is [baseCost], then accumulates using each tier's marginal rate.
     *
     * @param x 计费数量的线性表达式 / Linear expression for the billed quantity
     * @param breakpoints 严格递增的段边界 / Strictly increasing tier boundaries
     * @param marginalRates 每个区间的边际单价 / Marginal rate for each interval
     * @param converter 值类型转换器 / Value converter
     * @param baseCost 首个断点处的累计费用 / Cumulative cost at the first breakpoint
     * @param name 函数名称 / Function name
     * @param displayName 可选显示名称 / Optional display name
     * @return 连续分段线性总费用函数或结构化错误 / Continuous piecewise-linear total-cost function or a structured error
     */
    @JvmStatic
    fun <V> create(
        x: LinearPolynomial<V>,
        breakpoints: List<V>,
        marginalRates: List<V>,
        converter: IntoValue<V>,
        baseCost: V? = null,
        name: String = "incremental_tariff",
        displayName: String? = null
    ): Ret<UnivariateLinearPiecewiseFunction<V>> where V : FloatingNumber<V>, V : RealNumber<V>, V : NumberField<V> {
        val capturedX = x.snapshotCopy()
        val capturedBreakpoints = breakpoints.toList()
        val capturedRates = marginalRates.toList()
        if (name.isBlank() || capturedBreakpoints.size < 2 || capturedRates.size != capturedBreakpoints.size - 1) {
            return Failed(
                ErrorCode.IllegalArgument,
                "IncrementalTariff 需要非空名称、至少两个断点且费率数等于区间数 / IncrementalTariff requires a name, at least two breakpoints, and one rate per interval"
            )
        }
        if (capturedBreakpoints.any { !it.isFinite() } || capturedBreakpoints.zipWithNext().any { (left, right) -> left >= right }) {
            return Failed(
                ErrorCode.IllegalArgument,
                "IncrementalTariff 断点必须有限且严格递增 / IncrementalTariff breakpoints must be finite and strictly increasing"
            )
        }
        if (capturedRates.any { !it.isFinite() || it < converter.zero } || baseCost?.isFinite() == false) {
            return Failed(
                ErrorCode.IllegalArgument,
                "IncrementalTariff 费率必须有限且非负，基准费用必须有限 / IncrementalTariff rates must be finite and non-negative, and the base cost must be finite"
            )
        }
        return try {
            val points = ArrayList<Point<Dim2, V>>(capturedBreakpoints.size)
            var cost = baseCost ?: converter.zero
            points += Point<Dim2, V>(capturedBreakpoints.first(), cost)
            for (index in capturedRates.indices) {
                cost += capturedRates[index] * (capturedBreakpoints[index + 1] - capturedBreakpoints[index])
                if (!cost.isFinite()) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "IncrementalTariff 累计费用产生非有限值 / IncrementalTariff cumulative cost became non-finite"
                    )
                }
                points += Point<Dim2, V>(capturedBreakpoints[index + 1], cost)
            }
            UnivariateLinearPiecewiseFunction.fromPointsResult(
                x = capturedX,
                points = points,
                converter = converter,
                name = name,
                displayName = displayName
            )
        } catch (error: RuntimeException) {
            Failed(
                ErrorCode.IllegalArgument,
                "创建 IncrementalTariff 失败：${error.message ?: error::class.simpleName} / Failed to create IncrementalTariff: ${error.message ?: error::class.simpleName}"
            )
        }
    }
}

/**
 * 全量单价折扣函数。所选费率作用于全部数量；每个断点恰好归入右侧新档，断点之前宽度为 [boundaryGap] 的间隔不可取。
 *
 * All-units discount function. The selected rate applies to the full quantity; each breakpoint belongs to the new tier on its right, and the interval of width [boundaryGap] immediately before it is excluded.
 *
 * @property x 计费数量的线性表达式 / Linear expression for the billed quantity
 * @property breakpoints 有限且严格递增的档位边界 / Finite and strictly increasing tier boundaries
 * @property rates 每檔作用于全部数量的单价 / Per-unit rate applied to all quantity in each tier
 * @property boundaryGap 每个内部断点左侧排除的正间隔 / Positive excluded gap before each internal breakpoint
 * @property converter 值类型转换器 / Value converter
 * @property name 函数名称 / Function name
 * @property displayName 可选显示名称 / Optional display name
 * @property selectorVars 每档对应的二值选择变量 / Binary selector for each tariff tier
 * @property resultVar 总费用结果变量 / Total-cost result variable
 */
class AllUnitsDiscountFunction<V> private constructor(
    val x: LinearPolynomial<V>,
    val breakpoints: List<V>,
    val rates: List<V>,
    val boundaryGap: V,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?
) : MathFunctionSymbol<V>, HasResultPolynomial<V>
        where V : RealNumber<V>, V : NumberField<V> {

    val selectorVars: List<AbstractVariableItem<*, *>> = rates.indices.map { BinVar("${name}_tier_$it") }
    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_cost")
    private val maskingFunctions: List<MaskingFunction<V>> = rates.indices.map { index ->
        val upper = if (index == rates.lastIndex) breakpoints.last() else breakpoints[index + 1] - boundaryGap
        MaskingFunction(
            input = x.scaled(rates[index]),
            mask = selectorVars[index],
            bigM = maxAbs(breakpoints.first() * rates[index], breakpoints.last() * rates[index]),
            converter = converter,
            name = "${name}_rate_$index"
        )
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar) + selectorVars + maskingFunctions.flatMap { it.helperVariables }

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure(): DeferredFunctionStructure? = null

    override fun evaluate(values: Map<Symbol, V>): V? {
        val quantity = x.evaluateWith(values) ?: return null
        if (quantity < breakpoints.first() || quantity > breakpoints.last()) return null
        val tier = rates.indices.firstOrNull { index ->
            if (index == rates.lastIndex) {
                quantity >= breakpoints[index]
            } else {
                quantity >= breakpoints[index] && quantity <= breakpoints[index + 1] - boundaryGap
            }
        } ?: return null
        return quantity * rates[tier]
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val constraints = ArrayList<LinearInequality<V>>()
        val oneHot = selectorVars.map { LinearMonomial(converter.one, it) }
        constraints += LinearInequality(
            lhs = LinearPolynomial(oneHot, converter.zero),
            rhs = LinearPolynomial(emptyList(), converter.one),
            comparison = Comparison.EQ,
            name = "${name}_one_tier"
        )
        val relax = breakpoints.last() - breakpoints.first()
        for (index in rates.indices) {
            val selector = selectorVars[index]
            val lower = breakpoints[index]
            val upper = if (index == rates.lastIndex) breakpoints.last() else breakpoints[index + 1] - boundaryGap
            val lowerTerms = x.monomials.toMutableList()
            lowerTerms += LinearMonomial(-relax, selector)
            constraints += LinearInequality(
                lhs = LinearPolynomial(lowerTerms, x.constant),
                rhs = LinearPolynomial(emptyList(), lower - relax),
                comparison = Comparison.GE,
                name = "${name}_tier_${index}_lower"
            )
            val upperTerms = x.monomials.toMutableList()
            upperTerms += LinearMonomial(relax, selector)
            constraints += LinearInequality(
                lhs = LinearPolynomial(upperTerms, x.constant),
                rhs = LinearPolynomial(emptyList(), upper + relax),
                comparison = Comparison.LE,
                name = "${name}_tier_${index}_upper"
            )
        }
        val resultTerms = mutableListOf(LinearMonomial(converter.one, resultVar))
        for (mask in maskingFunctions) {
            resultTerms += LinearMonomial(-converter.one, mask.resultVar)
        }
        constraints += LinearInequality(
            lhs = LinearPolynomial(resultTerms, converter.zero),
            rhs = LinearPolynomial(emptyList(), converter.zero),
            comparison = Comparison.EQ,
            name = "${name}_cost_link"
        )

        return registerFunctionsAtomically(
            model,
            maskingFunctions + ConstraintRowsFunction(
                constraints = constraints,
                converter = converter,
                name = "${name}_rows"
            )
        )
    }

    companion object {
        /**
         * 创建全量折扣函数。费率必须非负且不递增，数量定义域必须非负；[boundaryGap] 必须为正且小于每档宽度。
         *
         * Create an all-units discount function. Rates must be non-negative and non-increasing, the quantity domain non-negative, and [boundaryGap] positive and smaller than every tier width.
         *
         * @param x 计费数量的线性表达式 / Linear expression for the billed quantity
         * @param breakpoints 有限且严格递增的档位边界 / Finite and strictly increasing tier boundaries
         * @param rates 每檔作用于全部数量的单价 / Per-unit rate applied to all quantity in each tier
         * @param boundaryGap 每个内部断点左侧排除的正间隔 / Positive excluded gap before each internal breakpoint
         * @param converter 值类型转换器 / Value converter
         * @param name 函数名称 / Function name
         * @param displayName 可选显示名称 / Optional display name
         * @return 全量折扣函数或结构化错误 / All-units discount function or a structured error
         */
        fun <V> create(
            x: LinearPolynomial<V>,
            breakpoints: List<V>,
            rates: List<V>,
            boundaryGap: V,
            converter: IntoValue<V>,
            name: String = "all_units_discount",
            displayName: String? = null
        ): Ret<AllUnitsDiscountFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val capturedX = x.snapshotCopy()
            val capturedBreakpoints = breakpoints.toList()
            val capturedRates = rates.toList()
            if (name.isBlank() || capturedBreakpoints.size < 2 || capturedRates.size != capturedBreakpoints.size - 1) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "全量折扣需要非空名称、至少两个边界且费率数等于区间数 / All-units discount requires a name, at least two boundaries, and one rate per interval"
                )
            }
            if (capturedBreakpoints.any { !it.isFinite() } || capturedBreakpoints.zipWithNext().any { (left, right) -> left >= right }) {
                return Failed(ErrorCode.IllegalArgument, "全量折扣边界必须有限且严格递增 / All-units discount boundaries must be finite and strictly increasing")
            }
            if (capturedRates.any { !it.isFinite() || it < converter.zero } || capturedRates.zipWithNext().any { (left, right) -> right > left }) {
                return Failed(ErrorCode.IllegalArgument, "全量折扣费率必须有限、非负且不递增 / All-units discount rates must be finite, non-negative, and non-increasing")
            }
            if (capturedBreakpoints.first() < converter.zero || !boundaryGap.isFinite() || boundaryGap <= converter.zero) {
                return Failed(ErrorCode.IllegalArgument, "全量折扣数量下界必须非负且 boundaryGap 必须为正有限值 / All-units discount requires a non-negative quantity domain and a positive finite boundaryGap")
            }
            if (capturedBreakpoints.zipWithNext().any { (left, right) -> right - left <= boundaryGap }) {
                return Failed(ErrorCode.IllegalArgument, "boundaryGap 必须小于每档区间宽度 / boundaryGap must be smaller than every tier width")
            }
            return try {
                val function = AllUnitsDiscountFunction(
                    x = capturedX,
                    breakpoints = capturedBreakpoints,
                    rates = capturedRates,
                    boundaryGap = boundaryGap,
                    converter = converter,
                    name = name,
                    displayName = displayName
                )
                for (index in capturedRates.indices) {
                    val tierUpper = if (index == capturedRates.lastIndex) capturedBreakpoints.last() else capturedBreakpoints[index + 1] - boundaryGap
                    val maskUpper = tierUpper * capturedRates[index]
                    when (val range = setFiniteSolverRange(
                        variable = function.maskingFunctions[index].resultVar,
                        lower = converter.zero,
                        upper = maskUpper,
                        converter = converter
                    )) {
                        is Ok -> {}
                        is Failed -> return Failed(range.error)
                        is Fatal -> return Fatal(range.errors)
                    }
                }
                val costs = capturedRates.indices.flatMap { index ->
                    val tierUpper = if (index == capturedRates.lastIndex) capturedBreakpoints.last() else capturedBreakpoints[index + 1] - boundaryGap
                    listOf(capturedBreakpoints[index] * capturedRates[index], tierUpper * capturedRates[index])
                }
                when (val range = setFiniteSolverRange(
                    variable = function.resultVar,
                    lower = costs.minOrNull() ?: converter.zero,
                    upper = costs.maxOrNull() ?: converter.zero,
                    converter = converter
                )) {
                    is Ok -> ok(function)
                    is Failed -> Failed(range.error)
                    is Fatal -> Fatal(range.errors)
                }
            } catch (error: RuntimeException) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "创建全量折扣函数失败：${error.message ?: error::class.simpleName} / Failed to create all-units discount function: ${error.message ?: error::class.simpleName}"
                )
            }
        }
    }
}

/**
 * 固定启动费及可选线性活动费用。仅传 [activation] 时，由调用方决定是否启动，即使活动量为零也可收取固定费；
 * 提供活动量和边界时则约束活动量为 0 或位于 `[minimumActive, maximumActivity]`，两者之间的正量区间不可取。
 *
 * Fixed activation charge plus an optional linear activity cost. When only [activation] is provided, the caller controls startup and may pay the fixed charge with zero activity.
 * When activity and bounds are supplied, activity is constrained to 0 or `[minimumActive, maximumActivity]`, excluding the positive interval below the minimum.
 *
 * @property activation 外部提供的二值启动变量 / Externally supplied binary activation variable
 * @property fixedCost 启动时收取的固定费用 / Fixed fee charged when active
 * @property activity 可选活动量表达式 / Optional activity expression
 * @property unitCost 活动量单位费用 / Cost per activity unit
 * @property minimumActive 启动时的最小活动量 / Minimum activity when started
 * @property maximumActivity 启动时的最大活动量 / Maximum activity when started
 * @property converter 值类型转换器 / Value converter
 * @property name 函数名称 / Function name
 * @property displayName 可选显示名称 / Optional display name
 * @property resultVar 固定费用结果变量 / Fixed-charge result variable
 */
class FixedChargeFunction<V> private constructor(
    val activation: AbstractVariableItem<*, *>,
    val fixedCost: V,
    val activity: LinearPolynomial<V>?,
    val unitCost: V,
    val minimumActive: V?,
    val maximumActivity: V?,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String?
) : MathFunctionSymbol<V>, HasResultPolynomial<V>
        where V : RealNumber<V>, V : NumberField<V> {

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_cost")

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = listOf(resultVar)

    override val resultPolynomial: LinearPolynomial<V>
        get() = LinearPolynomial(listOf(LinearMonomial(converter.one, resultVar)), converter.zero)

    override fun deferredStructure(): DeferredFunctionStructure? = null

    override fun evaluate(values: Map<Symbol, V>): V? {
        val active = values[activation] ?: return null
        if (active != converter.zero && active != converter.one) return null
        val amount = activity?.evaluateWith(values)
        if (activity != null) {
            val resolvedAmount = amount ?: return null
            if (active == converter.zero && resolvedAmount != converter.zero) return null
            if (active == converter.one &&
                (resolvedAmount < minimumActive!! || resolvedAmount > maximumActivity!!)
            ) return null
        }
        val variableCost = amount?.times(unitCost) ?: converter.zero
        return variableCost + fixedCost * active
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val constraints = ArrayList<LinearInequality<V>>()
        val output = mutableListOf(LinearMonomial(converter.one, resultVar), LinearMonomial(-fixedCost, activation))
        if (activity != null) {
            output += activity.monomials.map { LinearMonomial(-unitCost * it.coefficient, it.symbol) }
            constraints += LinearInequality(
                lhs = activity + LinearPolynomial(listOf(LinearMonomial(-maximumActivity!!, activation)), converter.zero),
                rhs = LinearPolynomial(emptyList(), converter.zero),
                comparison = Comparison.LE,
                name = "${name}_activity_upper"
            )
            constraints += LinearInequality(
                lhs = activity + LinearPolynomial(listOf(LinearMonomial(-minimumActive!!, activation)), converter.zero),
                rhs = LinearPolynomial(emptyList(), converter.zero),
                comparison = Comparison.GE,
                name = "${name}_activity_lower"
            )
        }
        constraints += LinearInequality(
            lhs = LinearPolynomial(output, -unitCost * (activity?.constant ?: converter.zero)),
            rhs = LinearPolynomial(emptyList(), converter.zero),
            comparison = Comparison.EQ,
            name = "${name}_cost_link"
        )
        return registerFunctionsAtomically(
            model,
            listOf(
                ConstraintRowsFunction(
                    constraints = constraints,
                    converter = converter,
                    name = "${name}_rows"
                )
            )
        )
    }

    companion object {
        /**
         * 创建固定启动费用函数，并验证可选的活动量联动边界。
         *
         * Create a fixed-charge function and validate optional activity-linking bounds.
         *
         * @param activation 外部二值启动变量 / External binary activation variable
         * @param fixedCost 启动时收取的固定费用 / Fixed fee charged when active
         * @param converter 值类型转换器 / Value converter
         * @param activity 可选活动量表达式 / Optional activity expression
         * @param unitCost 活动量单位费用 / Cost per activity unit
         * @param minimumActive 启动时的最小活动量；若提供 activity 则默认为 0 / Minimum activity when started; defaults to 0 when activity is supplied
         * @param maximumActivity 启动时的有限最大活动量；提供 activity 时必填 / Finite maximum activity when started; required when activity is supplied
         * @param name 函数名称 / Function name
         * @param displayName 可选显示名称 / Optional display name
         * @return 固定费用函数或结构化错误 / Fixed-charge function or a structured error
         */
        fun <V> create(
            activation: AbstractVariableItem<*, *>,
            fixedCost: V,
            converter: IntoValue<V>,
            activity: LinearPolynomial<V>? = null,
            unitCost: V = converter.zero,
            minimumActive: V? = null,
            maximumActivity: V? = null,
            name: String = "fixed_charge",
            displayName: String? = null
        ): Ret<FixedChargeFunction<V>> where V : RealNumber<V>, V : NumberField<V> {
            val capturedActivity = activity?.snapshotCopy()
            if (!activation.type.isBinaryType) {
                return Failed(ErrorCode.IllegalArgument, "固定费 activation 必须为二值变量 / Fixed-charge activation must be binary")
            }
            if (name.isBlank() || !fixedCost.isFinite() || fixedCost < converter.zero || !unitCost.isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "固定费名称、fixedCost 或 unitCost 无效 / Fixed-charge name, fixedCost, or unitCost is invalid")
            }
            if (capturedActivity == null && (minimumActive != null || maximumActivity != null)) {
                return Failed(ErrorCode.IllegalArgument, "未提供 activity 时不能提供活动量边界 / Activity bounds require an activity expression")
            }
            if (capturedActivity != null) {
                val min = minimumActive ?: converter.zero
                val max = maximumActivity ?: return Failed(
                    ErrorCode.IllegalArgument,
                    "固定费联动 activity 必须提供有限 maximumActivity / Linked fixed-charge activity requires a finite maximumActivity"
                )
                if (!min.isFinite() || !max.isFinite() || min < converter.zero || max <= converter.zero || min > max) {
                    return Failed(ErrorCode.IllegalArgument, "活动量边界必须满足 0 <= minimumActive <= maximumActivity 且最大值大于零 / Activity bounds must satisfy 0 <= minimumActive <= maximumActivity with a positive maximum")
                }
                if (!unitCost.isFinite()) {
                    return Failed(ErrorCode.IllegalArgument, "unitCost 必须有限 / unitCost must be finite")
                }
            } else if (unitCost != converter.zero) {
                return Failed(ErrorCode.IllegalArgument, "未提供 activity 时 unitCost 必须为零 / unitCost must be zero without an activity expression")
            }
            return try {
                val min = minimumActive ?: converter.zero
                val function = FixedChargeFunction(
                    activation = activation,
                    fixedCost = fixedCost,
                    activity = capturedActivity,
                    unitCost = unitCost,
                    minimumActive = capturedActivity?.let { minimumActive ?: converter.zero },
                    maximumActivity = maximumActivity,
                    converter = converter,
                    name = name,
                    displayName = displayName
                )
                val lower = if (capturedActivity == null) converter.zero else {
                    minOf(
                        a = converter.zero,
                        b = fixedCost + unitCost * min,
                        c = fixedCost + unitCost * maximumActivity!!
                    )
                }
                val upper = if (capturedActivity == null) fixedCost else {
                    maxOf(
                        a = converter.zero,
                        b = fixedCost + unitCost * min,
                        c = fixedCost + unitCost * maximumActivity!!
                    )
                }
                when (val range = setFiniteSolverRange(
                    variable = function.resultVar,
                    lower = lower,
                    upper = upper,
                    converter = converter
                )) {
                    is Ok -> ok(function)
                    is Failed -> Failed(range.error)
                    is Fatal -> Fatal(range.errors)
                }
            } catch (error: RuntimeException) {
                Failed(
                    ErrorCode.IllegalArgument,
                    "创建固定启动费函数失败：${error.message ?: error::class.simpleName} / Failed to create fixed-charge function: ${error.message ?: error::class.simpleName}"
                )
            }
        }
    }
}

private fun <V : Ring<V>> LinearPolynomial<V>.scaled(coefficient: V): LinearPolynomial<V> {
    return LinearPolynomial(
        monomials = monomials.map { LinearMonomial(it.coefficient * coefficient, it.symbol) },
        constant = constant * coefficient
    )
}

private fun <V : Ring<V>> LinearPolynomial<V>.snapshotCopy(): LinearPolynomial<V> {
    return LinearPolynomial(monomials.toList(), constant)
}

private fun <V> maxAbs(left: V, right: V): V where V : RealNumber<V>, V : NumberField<V> {
    val zero = left - left
    val leftAbs = if (left < zero) -left else left
    val rightAbs = if (right < zero) -right else right
    return if (leftAbs >= rightAbs) leftAbs else rightAbs
}

private class ConstraintRowsFunction<V>(
    private val constraints: List<LinearInequality<V>>,
    private val converter: IntoValue<V>,
    override var name: String
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    override var displayName: String? = null
    override val helperVariables: List<AbstractVariableItem<*, *>> = emptyList()
    override val resultPolynomial: LinearPolynomial<V> = LinearPolynomial(emptyList(), converter.zero)

    override fun deferredStructure(): DeferredFunctionStructure? = null

    override fun evaluate(values: Map<Symbol, V>): V? = resultPolynomial.evaluateWith(values)

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        return addConstraints(model, constraints) ?: ok
    }
}

private fun <V> setFiniteSolverRange(
    variable: AbstractVariableItem<*, *>,
    lower: V,
    upper: V,
    converter: IntoValue<V>
): Try where V : RealNumber<V>, V : NumberField<V> {
    if (!lower.isFinite() || !upper.isFinite() || lower > upper) {
        return Failed(ErrorCode.IllegalArgument, "结果范围必须有限且有序 / Result bounds must be finite and ordered")
    }
    val realVariable = variable as? RealVar
        ?: return Failed(ErrorCode.IllegalArgument, "结果变量必须为连续变量 / Result variable must be continuous")
    return try {
        val lowerFlt64 = converter.fromValue(lower)
        val upperFlt64 = converter.fromValue(upper)
        if (!lowerFlt64.isFinite() || !upperFlt64.isFinite() ||
            lowerFlt64 <= Flt64.minimum || upperFlt64 >= Flt64.maximum || lowerFlt64 > upperFlt64
        ) {
            return Failed(ErrorCode.IllegalArgument, "结果范围无法精确转换为有限 Flt64 / Result bounds cannot be converted to finite Flt64 values")
        }
        when (val range = ValueRange(
            lb = lowerFlt64,
            ub = upperFlt64,
            lbInterval = Interval.Closed,
            ubInterval = Interval.Closed,
            constants = Flt64
        )) {
            is Ok -> {
                realVariable.range.set(range.value)
                ok
            }
            is Failed -> Failed(range.error)
            is Fatal -> Fatal(range.errors)
        }
    } catch (error: RuntimeException) {
        Failed(
            ErrorCode.IllegalArgument,
            "设置结果范围失败：${error.message ?: error::class.simpleName} / Failed to set result bounds: ${error.message ?: error::class.simpleName}"
        )
    }
}
