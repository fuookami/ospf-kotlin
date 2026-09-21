@file:Suppress("unused")

/** 二次阶梯范围函数符号 / Quadratic step range function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * 二次步进区间函数符号 / Quadratic in-step-range function symbol
 *
 * 提供 [QuadraticInStepRangeFunction]，实现二次约束下的区间步进函数建模。
 *
 * Provides [QuadraticInStepRangeFunction] for interval step function modeling under quadratic constraints.
 */

/**
 * 二次步进区间函数：若 x 在 [lower, upper] 范围内则 y = x，否则 y = 0。 / Quadratic in-step-range function: y = x if x in [lower, upper], else y = 0.
 * 使用 inside、below、above 三个互斥二值变量完整表达区间内、低于下界和高于上界。 / Uses three mutually exclusive binary variables, inside, below, and above, to represent the complete interval partition.
 *
 * 连续域的严格区间补集不能由有限个非严格不等式精确表达，因此由 outsideTolerance 定义边界外容差带。
 * A strict complement of a closed interval cannot be represented exactly by finitely many non-strict inequalities;
 * outsideTolerance defines the exterior tolerance band.
 * 容差带内的直接求值返回 null，solver 模型也不允许该区域。 / Direct evaluation returns null in that band, and the solver model excludes it.
 *
 * @property x 二次多项式输入 / quadratic polynomial input
 * @property lower 范围的下界 / lower bound of the range
 * @property upper 范围的上界 / upper bound of the range
 * @param bigM Big-M 常量（默认从二次输入范围推导，失败时回退到 1e6）/ Big-M constant (inferred from quadratic input range by default, falls back to 1e6)
 * @param outsideTolerance 区间外分类容差（默认 1e-6）/ exterior classification tolerance (default 1e-6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 * @property bigM 约束使用的 Big-M / Big-M used by the constraints
 * @property outsideTolerance 区间外分类容差 / exterior classification tolerance
 * @property inside 区间内状态变量 / inside-state variable
 * @property below 下界以下状态变量 / below-range state variable
 * @property above 上界以上状态变量 / above-range state variable
 * @property y 结果辅助变量 / result helper variable
 */
class QuadraticInStepRangeFunction<V>(
    val x: QuadraticPolynomial<V>,
    val lower: V,
    val upper: V,
    bigM: V? = null,
    outsideTolerance: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    /** Big-M used by the four branch/value constraints. / 四组分支/取值约束使用的 Big-M。 */
    val bigM: V = bigM ?: x.defaultBigM(converter)
    /** Exterior classification tolerance. / 区间外分类容差。 */
    val outsideTolerance: V = outsideTolerance ?: converter.intoValue(Flt64(1e-6))

    init {
        require(lower.isFinite() && upper.isFinite()) {
            "QuadraticInStepRange bounds must be finite"
        }
        require(lower ls upper || lower eq upper) {
            "QuadraticInStepRange lower bound must be <= upper bound"
        }
        require(this.outsideTolerance gr converter.zero) {
            "QuadraticInStepRange outside tolerance must be positive"
        }
    }

    val inside: AbstractVariableItem<*, *> = BinVar("${name}_inside")
    val below: AbstractVariableItem<*, *> = BinVar("${name}_below")
    val above: AbstractVariableItem<*, *> = BinVar("${name}_above")
    val y: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    // The result is a signed linear helper; the wrapped operation is quadratic.
    // 结果是有符号线性辅助变量，底层操作类别仍为二次。
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = Quadratic

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in x.monomials) {
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) dep.flush(force)
    }

    /**
     * 从 token 表求值单个符号。 / Evaluate a single symbol from the token table.
     *
     * @param symbol 要求值的符号 / the symbol to evaluate
     * @param tokenTable token 表 / the token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 符号值或 null / symbol value or null
     */
    private fun evaluateSymbol(
        symbol: Symbol,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable.find(symbol)?.result ?: if (zeroIfNone) converter.zero else null
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    /**
     * 从结果列表求值单个符号。 / Evaluate a single symbol from a results list.
     *
     * @param symbol 要求值的符号 / the symbol to evaluate
     * @param results 结果值列表 / list of result values
     * @param tokenTable token 表 / the token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 符号值或 null / symbol value or null
     */
    private fun evaluateSymbol(
        symbol: Symbol,
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        return when (symbol) {
            is AbstractVariableItem<*, *> -> {
                val index = tokenTable.indexOf(symbol)
                if (index != null && index >= 0 && index < results.size) results[index]
                else if (zeroIfNone) converter.zero else null
            }
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(results, tokenTable, converter, zeroIfNone)
            else -> if (zeroIfNone) converter.zero else null
        }
    }

    /**
     * 从值映射求值单个符号。 / Evaluate a single symbol from a value map.
     *
     * @param symbol 要求值的符号 / the symbol to evaluate
     * @param values 符号到值的映射 / symbol-to-value map
     * @param tokenTable 可选的 token 表 / optional token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 符号值或 null / symbol value or null
     */
    private fun evaluateSymbol(
        symbol: Symbol,
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean
    ): V? {
        return values[symbol] ?: when (symbol) {
            is AbstractVariableItem<*, *> -> tokenTable?.find(symbol)?.result
            is IntermediateSymbol<*> -> SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol).evaluate(values, tokenTable, converter, zeroIfNone)
            else -> null
        } ?: if (zeroIfNone) converter.zero else null
    }

    /**
     * 求值二次多项式。 / Evaluate a quadratic polynomial.
     *
     * @param poly 要求值的二次多项式 / the quadratic polynomial to evaluate
     * @param resolve 符号解析函数 / symbol resolution function
     * @return 多项式值或 null / polynomial value or null
     */
    private fun evaluateQuadratic(
        poly: QuadraticPolynomial<V>,
        resolve: (Symbol) -> V?
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbol1Value = resolve(monomial.symbol1) ?: return null
            var termValue = monomial.coefficient * symbol1Value
            if (monomial.symbol2 != null) {
                val symbol2Value = resolve(monomial.symbol2!!) ?: return null
                termValue *= symbol2Value
            }
            value += termValue
        }
        return value
    }

    /**
     * 求值步进区间逻辑：区间内返回 x，明确位于容差带外时返回 0，容差带内返回 null。 / Evaluate step-range logic: return x inside the interval,
     * zero when clearly outside the tolerance band, and null inside the exterior tolerance band.
     *
     * @param resolve 符号解析函数 / symbol resolution function
     * @return 步进区间结果值、容差带内的 null，或输入缺失时的 null / step-range result, null in the tolerance band, or null for missing input
     */
    private fun evaluateStepRange(
        resolve: (Symbol) -> V?
    ): V? {
        val xValue = evaluateQuadratic(x, resolve) ?: return null
        val inLower = xValue gr lower || xValue eq lower
        val inUpper = xValue ls upper || xValue eq upper
        if (inLower && inUpper) {
            return xValue
        }

        val clearlyBelow = xValue ls (lower - outsideTolerance) || xValue eq (lower - outsideTolerance)
        val clearlyAbove = xValue gr (upper + outsideTolerance) || xValue eq (upper + outsideTolerance)
        return if (clearlyBelow || clearlyAbove) converter.zero else null
    }

    /** 使用 Flt64 值预计算求解器结果。 / Pre-compute solver result with Flt64 values.
     *
     * @param values Flt64 值映射 / Flt64 value map
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @return 预计算结果或 null / pre-computed result or null
     */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val targetValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (targetValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(targetValues, tokenTable, converter, false)
        }
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, y)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    /** 使用 Flt64 token 列表求值（始终返回 null）。 / Evaluate with Flt64 token list (always returns null).
     *
     * @param tokenList Flt64 token 列表 / Flt64 token list
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 始终返回 null / always returns null
     */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /** 使用 Flt64 结果列表求值（始终返回 null）。 / Evaluate with Flt64 results list (always returns null).
     *
     * @param results Flt64 结果值列表 / Flt64 results list
     * @param tokenList Flt64 token 列表 / Flt64 token list
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 始终返回 null / always returns null
     */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /** 使用 Flt64 值映射求值（始终返回 null）。 / Evaluate with Flt64 value map (always returns null).
     *
     * @param values Flt64 值映射 / Flt64 value map
     * @param tokenList Flt64 token 列表 / Flt64 token list
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 始终返回 null / always returns null
     */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? = null

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateStepRange { symbol ->
            evaluateSymbol(symbol, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateStepRange { symbol ->
            evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateStepRange { symbol ->
            evaluateSymbol(symbol, values, tokenTable, zeroIfNone)
        }
    }

    /** 使用 Flt64 结果列表进行求解器求值。 / Evaluate solver with Flt64 results list.
     *
     * @param results Flt64 结果值列表 / Flt64 results list
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 求值结果或 null / evaluation result or null
     */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetResults = results.map { converter.intoValue(it) }
        return evaluate(targetResults, tokenTable, converter, zeroIfNone)
    }

    /** 使用 Flt64 值映射进行求解器求值。 / Evaluate solver with Flt64 value map.
     *
     * @param values Flt64 值映射 / Flt64 value map
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 求值结果或 null / evaluation result or null
     */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(targetValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * 将辅助变量 (inside, below, above, y) 注册到 token 集合中。 / Register helper variables (inside, below, above, y) with the token collection.
     *
     * @param tokens 目标 token 集合 / target token collection
     * @return 注册结果 / registration result
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(listOf(inside, below, above, y))) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 为完整的三状态区间划分注册 Big-M 约束。 / Register Big-M constraints for the complete three-state interval partition.
     *
     * @param model 二次机制模型 / quadratic mechanism model
     * @return 注册结果 / registration result
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        if (!isUsableExplicitBigM(bigM, converter)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "QuadraticInStepRange Big-M must be finite and positive. / QuadraticInStepRange Big-M must be finite and positive."
            )
        }
        if (!(outsideTolerance.isFinite() && outsideTolerance gr converter.zero)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "QuadraticInStepRange outside tolerance must be finite and positive. / QuadraticInStepRange outside tolerance must be finite and positive."
            )
        }
        val m = bigM
        val zero = converter.zero
        val one = converter.one
        val tolerance = outsideTolerance
        val yMon = QuadraticMonomial.linear(converter.one, y)
        val insideMon = QuadraticMonomial.linear(one, inside)
        val belowMon = QuadraticMonomial.linear(one, below)
        val aboveMon = QuadraticMonomial.linear(one, above)
        val insideMMon = QuadraticMonomial.linear(m, inside)
        val negInsideMMon = QuadraticMonomial.linear(-m, inside)
        val belowMMon = QuadraticMonomial.linear(m, below)
        val negAboveMMon = QuadraticMonomial.linear(-m, above)

        val negXMonos = x.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
        val posXMonos = x.monomials

        val constraints = mutableListOf<QuadraticInequalityOf<V>>()

        // inside = 1 => x >= lower / inside = 1 表示 x 不低于下界
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(posXMonos + negInsideMMon, x.constant),
            QuadraticPolynomial(emptyList(), lower - m),
            Comparison.GE,
            "${name}_inside_lb"
        )

        // inside = 1 => x <= upper / inside = 1 表示 x 不高于上界
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(posXMonos + insideMMon, x.constant),
            QuadraticPolynomial(emptyList(), upper + m),
            Comparison.LE,
            "${name}_inside_ub"
        )

        // below = 1 => x <= lower - tolerance / below = 1 表示明确低于下界
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(posXMonos + belowMMon, x.constant),
            QuadraticPolynomial(emptyList(), lower - tolerance + m),
            Comparison.LE,
            "${name}_below"
        )

        // above = 1 => x >= upper + tolerance / above = 1 表示明确高于上界
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(posXMonos + negAboveMMon, x.constant),
            QuadraticPolynomial(emptyList(), upper + tolerance - m),
            Comparison.GE,
            "${name}_above"
        )

        // inside + below + above = 1 / 三个状态互斥且完备
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(listOf(insideMon, belowMon, aboveMon), zero),
            QuadraticPolynomial(emptyList(), one),
            Comparison.EQ,
            "${name}_partition"
        )

        // inside = 1 => y = x / 进入区间分支时 y = x
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(listOf(yMon) + negXMonos + negInsideMMon, -x.constant),
            QuadraticPolynomial(emptyList(), -m),
            Comparison.GE,
            "${name}_value_lb"
        )
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(listOf(yMon) + negXMonos + insideMMon, -x.constant),
            QuadraticPolynomial(emptyList(), m),
            Comparison.LE,
            "${name}_value_ub"
        )

        // inside = 0 => y = 0 / 未进入区间分支时 y = 0
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(listOf(yMon, negInsideMMon), zero),
            QuadraticPolynomial(emptyList(), zero),
            Comparison.LE,
            "${name}_zero_ub"
        )
        constraints += QuadraticInequalityOf(
            QuadraticPolynomial(listOf(yMon, insideMMon), zero),
            QuadraticPolynomial(emptyList(), zero),
            Comparison.GE,
            "${name}_zero_lb"
        )

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        /**
         * 创建二次步进区间函数实例。 / Create a quadratic in-step-range function instance.
         *
         * @param x 二次多项式输入 / quadratic polynomial input
         * @param lower 范围下界 / lower bound of the range
         * @param upper 范围上界 / upper bound of the range
         * @param bigM 可选显式 Big-M / optional explicit Big-M
         * @param outsideTolerance 区间外分类容差 / exterior classification tolerance
         * @param converter 值类型转换器 / value type converter
         * @param name 函数名称 / function name
         * @param displayName 可选显示名称 / optional display name
         * @return 二次步进区间函数实例 / [QuadraticInStepRangeFunction] instance
         */
        operator fun <V> invoke(
            x: QuadraticPolynomial<V>,
            lower: V,
            upper: V,
            bigM: V? = null,
            outsideTolerance: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticInStepRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticInStepRangeFunction(x, lower, upper, bigM, outsideTolerance, converter, name, displayName)
    }
}
