@file:Suppress("unused")

/** 二次掩码范围函数符号 / Quadratic masking range function symbol */
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
 * 二次掩码区间函数符号 / Quadratic masking range function symbol
 *
 * 提供 [QuadraticMaskingRangeFunction]，实现当 z=1 时 y=poly，当 z=0 时 y 自由的二次约束建模。
 *
 * Provides [QuadraticMaskingRangeFunction] for quadratic constraint modeling where y=poly when z=1, and y is free when z=0.
 */

/**
 * 二次掩码范围：当 z = 1 时，y 被强制等于多项式；当 z = 0 时，y 自由（在边界内）。
 * Quadratic masking range: when z = 1, y is forced to equal polynomial;
 * when z = 0, y is free (within bounds).
 * 使用 Big-M 公式：y <= polynomial + M*(1-z), y >= polynomial - M*(1-z)。
 * Uses Big-M formulation: y <= polynomial + M*(1-z), y >= polynomial - M*(1-z).
 *
 * @property _polynomial 要掩码的二次多项式 / the quadratic polynomial to mask
 * @property z 二值控制变量 / the binary control variable
 * @param bigM Big-M 常量（默认从二次多项式范围推导，失败时回退到 1e6）/ Big-M constant (inferred from quadratic polynomial range by default, falls back to 1e6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class QuadraticMaskingRangeFunction<V>(
    val _polynomial: QuadraticPolynomial<V>,
    val z: AbstractVariableItem<*, *>,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val bigM: V = bigM ?: _polynomial.defaultBigM(converter)

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_y")

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in _polynomial.monomials) {
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
            }
            SolverBoundaryCasts.symbolAsIntermediateStar<V>(z)?.let { deps.add(it) }
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
     * 求值掩码逻辑：z=0 时返回 0，z=1 时返回多项式值。
     * Evaluate masking logic: return 0 when z=0, polynomial value when z=1.
     *
     * @param resolve 符号解析函数 / symbol resolution function
     * @return 掩码结果值或 null / masking result value or null
     */
    private fun evaluateMasking(
        resolve: (Symbol) -> V?
    ): V? {
        val zValue = resolve(z) ?: return converter.zero
        if (zValue eq converter.zero) {
            return converter.zero
        }
        return evaluateQuadratic(_polynomial, resolve)
    }

    /** 使用 Flt64 值预计算求解器结果。 / Pre-compute solver result with Flt64 values. */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val targetValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (targetValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(targetValues, tokenTable, converter, false)
        }
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = QuadraticPolynomial(listOf(QuadraticMonomial.linear(converter.one, resultVar)), converter.zero)

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    /** 使用 Flt64 token 列表求值（始终返回 null）。 / Evaluate with Flt64 token list (always returns null). */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 结果列表求值（始终返回 null）。 / Evaluate with Flt64 results list (always returns null). */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null
    /** 使用 Flt64 值映射求值（始终返回 null）。 / Evaluate with Flt64 value map (always returns null). */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? = null

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateMasking { symbol ->
            evaluateSymbol(symbol, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateMasking { symbol ->
            evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateMasking { symbol ->
            evaluateSymbol(symbol, values, tokenTable, zeroIfNone)
        }
    }
    /** 使用 Flt64 结果列表进行求解器求值。 / Evaluate solver with Flt64 results list. */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetResults = results.map { converter.intoValue(it) }
        return evaluate(targetResults, tokenTable, converter, zeroIfNone)
    }
    /** 使用 Flt64 值映射进行求解器求值。 / Evaluate solver with Flt64 value map. */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(targetValues, tokenTable, converter, zeroIfNone)
    }

    override fun toRawString(unfold: UInt64): String = displayName ?: name

    /**
     * 将辅助变量 (resultVar) 注册到 token 集合中。
     * Register helper variable (resultVar) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(listOf(resultVar))) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 注册 Big-M 掩码约束。
     * Register Big-M masking constraints.
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val m = bigM
        val resultMon = QuadraticMonomial.linear(converter.one, resultVar)
        val bigMMon = QuadraticMonomial.linear(m, z)

        val negatedPolyMonos = _polynomial.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }

        val constraints = mutableListOf<QuadraticInequalityOf<V>>()

        // Constraint 1: y - polynomial + M*z <= M / 约束 1：y - 多项式 + M*z <= M
        val lhs1 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(bigMMon), -_polynomial.constant)
        val rhs1 = QuadraticPolynomial<V>(emptyList(), m)
        constraints += QuadraticInequalityOf(lhs1, rhs1, Comparison.LE, "${name}_upper")

        // Constraint 2: y - polynomial - M*z >= -M / 约束 2：y - 多项式 - M*z >= -M
        val negBigMMon = QuadraticMonomial.linear(-m, z)
        val lhs2 = QuadraticPolynomial(listOf(resultMon) + negatedPolyMonos + listOf(negBigMMon), -_polynomial.constant)
        val rhs2 = QuadraticPolynomial<V>(emptyList(), -m)
        constraints += QuadraticInequalityOf(lhs2, rhs2, Comparison.GE, "${name}_lower")

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        /** 创建 [QuadraticMaskingRangeFunction] 实例。 / Create a [QuadraticMaskingRangeFunction] instance. */
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            z: AbstractVariableItem<*, *>,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticMaskingRangeFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticMaskingRangeFunction(polynomial, z, bigM, converter, name, displayName)
    }
}
