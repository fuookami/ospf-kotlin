@file:Suppress("unused")

/** 二次阶梯范围函数符号 / Quadratic step range function symbol */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 二次步进区间函数符号 / Quadratic in-step-range function symbol
 *
 * 提供 [QuadraticInStepRangeFunction]，实现二次约束下的区间步进函数建模。
 *
 * Provides [QuadraticInStepRangeFunction] for interval step function modeling under quadratic constraints.
 */

/**
 * 二次步进区间函数：若 x 在 [lower, upper] 范围内则 y = x，否则 y = 0。
 * Quadratic in-step-range function: y = x if x in [lower, upper], else y = 0.
 * 使用二值变量 z，其中 z=1 表示 x 在范围内。
 * Uses binary variable z where z=1 means x is in range.
 *
 * 约束：
 * Constraints:
 * - x - lower >= -M*(1-z)   =>   x - lower + M - M*z >= 0
 * - x - upper <= M*(1-z)    =>   x - upper - M + M*z <= 0
 * - y - x >= -M*(1-z)       =>   y - x + M - M*z >= 0
 * - y - x <= M*(1-z)        =>   y - x - M + M*z <= 0
 * - y <= M*z
 * - y >= -M*z
 *
 * @property x 二次多项式输入 / quadratic polynomial input
 * @property lower 范围的下界 / lower bound of the range
 * @property upper 范围的上界 / upper bound of the range
 * @param bigM Big-M 常量（默认 1e6）/ Big-M constant (default 1e6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class QuadraticInStepRangeFunction<V>(
    val x: QuadraticPolynomial<V>,
    val lower: V,
    val upper: V,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: converter.intoValue(Flt64(BIG_M_DEFAULT))

    init {
        require(lower ls upper || lower eq upper) {
            "QuadraticInStepRange lower bound must be <= upper bound"
        }
    }

    val z: AbstractVariableItem<*, *> = BinVar("${name}_z")
    val y: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Quadratic
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
     * 从 token 表求值单个符号。
     * Evaluate a single symbol from the token table.
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
     * 从结果列表求值单个符号。
     * Evaluate a single symbol from a results list.
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
     * 从值映射求值单个符号。
     * Evaluate a single symbol from a value map.
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
     * 求值二次多项式。
     * Evaluate a quadratic polynomial.
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
     * 求值步进区间逻辑：x 在范围内返回 x，否则返回 0。
     * Evaluate step-range logic: return x if in range, else 0.
     *
     * @param resolve 符号解析函数 / symbol resolution function
     * @return 步进区间结果值或 null / step-range result value or null
     */
    private fun evaluateStepRange(
        resolve: (Symbol) -> V?
    ): V? {
        val xValue = evaluateQuadratic(x, resolve) ?: return null
        val inLower = xValue gr lower || xValue eq lower
        val inUpper = xValue ls upper || xValue eq upper
        return if (inLower && inUpper) {
            xValue
        } else {
            converter.zero
        }
    }

    /** 使用 Flt64 值预计算求解器结果。 / Pre-compute solver result with Flt64 values. */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val typedValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (typedValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(typedValues, tokenTable, converter, false)
        }
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, y)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    /** 使用 Flt64 token 列表求值（始终返回 null）。 / Evaluate with Flt64 token list (always returns null). */
    internal fun evaluate(tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 结果列表求值（始终返回 null）。 / Evaluate with Flt64 results list (always returns null). */
    internal fun evaluate(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 值映射求值（始终返回 null）。 / Evaluate with Flt64 value map (always returns null). */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<fuookami.ospf.kotlin.math.algebra.number.Flt64>?, zeroIfNone: Boolean): Flt64? = null

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
    /** 使用 Flt64 结果列表进行求解器求值。 / Evaluate solver with Flt64 results list. */
    internal fun evaluateSolver(results: List<fuookami.ospf.kotlin.math.algebra.number.Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedResults = results.map { converter.intoValue(it) }
        return evaluate(typedResults, tokenTable, converter, zeroIfNone)
    }
    /** 使用 Flt64 值映射进行求解器求值。 / Evaluate solver with Flt64 value map. */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val typedValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(typedValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * 将辅助变量 (z, y) 注册到 token 集合中。
     * Register helper variables (z, y) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(listOf(z, y))) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 为步进区间函数注册 Big-M 约束。
     * Register Big-M constraints for the in-step-range function.
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val m = bigM
        val zero = converter.zero
        val yMon = QuadraticMonomial.linear(converter.one, y)
        val zMon = QuadraticMonomial.linear(m, z)
        val negZMon = QuadraticMonomial.linear(-m, z)

        val negXMonos = x.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
        val posXMonos = x.monomials

        val constraints = mutableListOf<QuadraticInequalityOf<V>>()

        // C1: x + M*(1-z) >= lower  =>  x + M - M*z >= lower / C1：x + M*(1-z) >= 下界
        val c1Lhs = QuadraticPolynomial(posXMonos + listOf(negZMon), x.constant + m)
        val c1Rhs = QuadraticPolynomial<V>(emptyList(), lower)
        constraints += QuadraticInequalityOf(c1Lhs, c1Rhs, Comparison.GE, "${name}_range_lb")

        // C2: x <= upper + M*(1-z)  =>  x - M + M*z <= upper / C2：x <= 上界 + M*(1-z)
        val c2Lhs = QuadraticPolynomial(posXMonos + listOf(zMon), x.constant - m)
        val c2Rhs = QuadraticPolynomial<V>(emptyList(), upper)
        constraints += QuadraticInequalityOf(c2Lhs, c2Rhs, Comparison.LE, "${name}_range_ub")

        // C3: y - x + M*(1-z) >= 0  =>  y - x + M - M*z >= 0 / C3：y - x + M*(1-z) >= 0
        val c3Lhs = QuadraticPolynomial(listOf(yMon) + negXMonos + listOf(negZMon), -x.constant + m)
        val c3Rhs = QuadraticPolynomial<V>(emptyList(), zero)
        constraints += QuadraticInequalityOf(c3Lhs, c3Rhs, Comparison.GE, "${name}_eq_lb")

        // C4: y - x - M*(1-z) <= 0  =>  y - x - M + M*z <= 0 / C4：y - x - M*(1-z) <= 0
        val c4Lhs = QuadraticPolynomial(listOf(yMon) + negXMonos + listOf(zMon), -x.constant - m)
        val c4Rhs = QuadraticPolynomial<V>(emptyList(), zero)
        constraints += QuadraticInequalityOf(c4Lhs, c4Rhs, Comparison.LE, "${name}_eq_ub")

        // C5: y <= M*z / C5：y <= M*z
        val c5Lhs = QuadraticPolynomial(listOf(yMon), zero)
        val c5Rhs = QuadraticPolynomial(listOf(zMon), zero)
        constraints += QuadraticInequalityOf(c5Lhs, c5Rhs, Comparison.LE, "${name}_zero_ub")

        // C6: y >= -M*z / C6：y >= -M*z
        val c6Lhs = QuadraticPolynomial(listOf(yMon), zero)
        val c6Rhs = QuadraticPolynomial(listOf(negZMon), zero)
        constraints += QuadraticInequalityOf(c6Lhs, c6Rhs, Comparison.GE, "${name}_zero_lb")

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        /** 创建 [QuadraticInStepRangeFunction] 实例。 / Create a [QuadraticInStepRangeFunction] instance. */
        operator fun <V> invoke(
            x: QuadraticPolynomial<V>,
            lower: V,
            upper: V,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticInStepRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticInStepRangeFunction(x, lower, upper, bigM, converter, name, displayName)
    }
}
