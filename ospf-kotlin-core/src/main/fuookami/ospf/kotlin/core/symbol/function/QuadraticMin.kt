@file:Suppress("unused")

/** 二次最小值函数符号 / Quadratic minimum function symbol */
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
 * 二次最小值函数符号 / Quadratic minimum function symbol
 *
 * 提供 [QuadraticMinFunction]，实现二次约束下的最小值建模。
 *
 * Provides [QuadraticMinFunction] for minimum value modeling under quadratic constraints.
 */

/**
 * 二次最小值函数：y = min(p1, p2, ..., pn)。
 * Quadratic min function: y = min(p1, p2, ..., pn).
 * 使用 Big-M 公式与二值选择变量实现精确最小值，
 * Uses Big-M formulation with binary selection variables for exact min,
 * 或使用简单下界约束实现松弛最小值。
 * or simple lower-bound constraints for relaxed min.
 *
 * @property polynomials 要取最小值的二次多项式列表 / list of quadratic polynomials to take the min of
 * @property exact 若为 true，使用二值变量实现精确最小值；若为 false，仅强制 y <= pi / if true, uses binary variables for exact min; if false, only enforces y <= pi
 * @param bigM 精确公式的 Big-M 常量（默认从候选范围推导，失败时回退到 1e6）/ Big-M constant for exact formulation (inferred from candidate ranges by default, falls back to 1e6)
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 */
class QuadraticMinFunction<V>(
    val polynomials: List<QuadraticPolynomial<V>>,
    val exact: Boolean = true,
    bigM: V? = null,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    private val explicitBigM: V? = bigM

    val resultVar: AbstractVariableItem<*, *> = RealVar("${name}_min")
    val binVars: List<AbstractVariableItem<*, *>> by lazy {
        if (exact) polynomials.indices.map { i -> BinVar("${name}_u_$i") } else emptyList()
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = Linear
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = Linear

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (poly in polynomials) {
                for (m in poly.monomials) {
                    SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol1)?.let { deps.add(it) }
                    SolverBoundaryCasts.symbolAsIntermediateStar<V>(m.symbol2)?.let { deps.add(it) }
                }
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
     * 从所有多项式中选择最小值。
     * Choose the minimum value from all polynomials.
     *
     * @param eval 多项式求值函数 / polynomial evaluation function
     * @return 最小值或 null / minimum value or null
     */
    private fun chooseMin(
        eval: (QuadraticPolynomial<V>) -> V?
    ): V? {
        var minValue: V? = null
        for (poly in polynomials) {
            val value = eval(poly) ?: return null
            if (minValue == null || value ls minValue) {
                minValue = value
            }
        }
        return minValue
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
        return chooseMin { poly ->
            evaluateQuadratic(poly) { symbol ->
                evaluateSymbol(symbol, tokenTable, zeroIfNone)
            }
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return chooseMin { poly ->
            evaluateQuadratic(poly) { symbol ->
                evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
            }
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return chooseMin { poly ->
            evaluateQuadratic(poly) { symbol ->
                evaluateSymbol(symbol, values, tokenTable, zeroIfNone)
            }
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
     * 将辅助变量 (resultVar, binVars) 注册到 token 集合中。
     * Register helper variables (resultVar, binVars) with the token collection.
     */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        val allVars = mutableListOf<AbstractVariableItem<*, *>>(resultVar)
        allVars.addAll(binVars)
        return when (val result = tokens.add(allVars)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    /**
     * 注册最小值约束（y <= pi，精确模式下：y >= pi - M*(1-ui), sum(ui)=1）。
     * Register min constraints (y <= pi, and if exact: y >= pi - M*(1-ui), sum(ui)=1).
     */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val one = converter.one
        val zero = converter.zero
        val resultMon = QuadraticMonomial.linear(one, resultVar)
        val constraints = mutableListOf<QuadraticInequalityOf<V>>()
        val bounds = if (explicitBigM == null) {
            polynomials.map { it.finiteBounds(converter) }.takeIf { it.all { bound -> bound != null } }
        } else {
            null
        }
        val minLower = bounds?.map { it!!.lower }?.reduce { acc, value ->
            if (value ls acc) value else acc
        }

        // y <= pi for each polynomial / 对每个多项式 y <= pi
        for ((i, poly) in polynomials.withIndex()) {
            val negatedMonos = poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
            val lhs = QuadraticPolynomial(negatedMonos + listOf(resultMon), -poly.constant)
            val rhs = QuadraticPolynomial<V>(emptyList(), zero)
            constraints += QuadraticInequalityOf(lhs, rhs, Comparison.LE, "${name}_lb_$i")
        }

        if (exact) {
            // y >= pi - M*(1 - ui) for each polynomial / 对每个多项式 y >= pi - M*(1 - ui)
            for ((i, poly) in polynomials.withIndex()) {
                val uVar = binVars[i]
                val negatedPolyMonos = poly.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) }
                val currentBigM = explicitBigM ?: if (bounds != null && minLower != null) {
                    ensurePositiveBigM(bounds[i]!!.upper - minLower, converter)
                } else {
                    poly.defaultBigM(converter)
                }
                val bigMTerm = QuadraticMonomial.linear(-currentBigM, uVar)
                val lhs = QuadraticPolynomial(negatedPolyMonos + listOf(resultMon, bigMTerm), -poly.constant)
                val rhs = QuadraticPolynomial<V>(emptyList(), -currentBigM)
                constraints += QuadraticInequalityOf(lhs, rhs, Comparison.GE, "${name}_ub_$i")
            }

            // sum(ui) = 1 / 选择变量之和等于 1
            val sumMonos = binVars.map { QuadraticMonomial.linear(one, it) }
            val sumLhs = QuadraticPolynomial(sumMonos, zero)
            val sumRhs = QuadraticPolynomial<V>(emptyList(), one)
            constraints += QuadraticInequalityOf(sumLhs, sumRhs, Comparison.EQ, "${name}_u")
        }

        return addQuadraticConstraints(model, constraints) ?: ok
    }

    companion object {
        /** 创建 [QuadraticMinFunction] 实例。 / Create a [QuadraticMinFunction] instance. */
        operator fun <V> invoke(
            polynomials: List<QuadraticPolynomial<V>>,
            exact: Boolean = true,
            bigM: V? = null,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticMinFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticMinFunction(polynomials, exact, bigM, converter, name, displayName)
    }
}
