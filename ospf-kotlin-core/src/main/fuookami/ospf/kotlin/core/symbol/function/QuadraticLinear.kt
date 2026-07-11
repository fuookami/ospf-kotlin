@file:Suppress("unused")

/** 二次线性函数符号 / Quadratic linear function symbol */
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
 * 二次线性函数符号 / Quadratic linear function symbol
 *
 * 提供 [QuadraticLinearFunction]，将二次多项式包装为二次中间符号。
 *
 * Provides [QuadraticLinearFunction] for wrapping a quadratic polynomial as a quadratic intermediate symbol.
*/

/**
 * 二次线性函数：将 QuadraticPolynomial 包装为二次中间符号。
 * Quadratic linear function: wraps a QuadraticPolynomial as a quadratic intermediate symbol.
 * 若多项式为纯线性，则不需要辅助变量或约束。
 * If the polynomial is purely linear, no helper variable or constraint is needed.
 * 若包含二次项，则创建辅助变量 y 并约束 y = polynomial。
 * If it contains quadratic terms, creates a helper variable y with constraint y = polynomial.
 *
 * @property _polynomial 封装的二次多项式 / the wrapped quadratic polynomial
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
*/
class QuadraticLinearFunction<V>(
    private val _polynomial: QuadraticPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {

    private val isLinear: Boolean by lazy {
        _polynomial.monomials.none { it.isQuadratic }
    }

    private val y: AbstractVariableItem<*, *>? by lazy {
        if (!isLinear) URealVar("${name}_y") else null
    }

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = if (isLinear) Linear else Quadratic
    override val parent: IntermediateSymbol<out V>? = null
    override val operationCategory: Category get() = category

    override val dependencies: Set<IntermediateSymbol<out V>>
        get() {
            val deps = mutableSetOf<IntermediateSymbol<out V>>()
            for (m in _polynomial.monomials) {
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
     * 使用 Flt64 值预计算求解器结果。
     * Pre-compute solver result with Flt64 values.
     *
     * @param values 符号到 Flt64 值的映射，可为 null / symbol-to-Flt64 value map, may be null
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
        get() = _polynomial

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    /**
     * 使用 Flt64 token 列表求值（始终返回 null）。
     * Evaluate with Flt64 token list (always returns null).
     *
     * @param tokenList Flt64 token 列表 / Flt64 token list
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 始终返回 null / always returns null
    */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /**
     * 使用 Flt64 结果列表求值（始终返回 null）。
     * Evaluate with Flt64 results list (always returns null).
     *
     * @param results Flt64 结果值列表 / list of Flt64 result values
     * @param tokenList Flt64 token 列表 / Flt64 token list
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 始终返回 null / always returns null
    */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /**
     * 使用 Flt64 值映射求值（始终返回 null）。
     * Evaluate with Flt64 value map (always returns null).
     *
     * @param values 符号到 Flt64 值的映射 / symbol-to-Flt64 value map
     * @param tokenList 可选的 Flt64 token 列表 / optional Flt64 token list
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
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, results, tokenTable, zeroIfNone)
        }
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateQuadratic(_polynomial) { symbol ->
            evaluateSymbol(symbol, values, tokenTable, zeroIfNone)
        }
    }

    /**
     * 使用 Flt64 结果列表进行求解器求值。
     * Evaluate solver with Flt64 results list.
     *
     * @param results Flt64 结果值列表 / list of Flt64 result values
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 求值结果或 null / evaluation result or null
    */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetResults = results.map { converter.intoValue(it) }
        return evaluate(targetResults, tokenTable, converter, zeroIfNone)
    }

    /**
     * 使用 Flt64 值映射进行求解器求值。
     * Evaluate solver with Flt64 value map.
     *
     * @param values 符号到 Flt64 值的映射 / symbol-to-Flt64 value map
     * @param tokenTable 可选的 token 表 / optional token table
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
     * 将辅助变量 y 注册到 token 集合中（仅当为二次时）。
     * Register helper variable y with the token collection (only if quadratic).
    */
    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        if (!isLinear && y != null) {
            return when (val result = tokens.add(listOf(y!!))) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 注册二次等式约束 y = polynomial（仅当为二次时）。
     * Register the quadratic equality constraint y = polynomial (only if quadratic).
    */
    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        if (!isLinear && y != null) {
            val yMon = QuadraticMonomial.linear(converter.one, y!!)
            val lhs = QuadraticPolynomial(
                listOf(yMon) + _polynomial.monomials.map { QuadraticMonomial(-it.coefficient, it.symbol1, it.symbol2) },
                -_polynomial.constant
            )
            val rhs = QuadraticPolynomial<V>(emptyList(), converter.zero)
            val constraint = QuadraticInequalityOf(lhs, rhs, Comparison.EQ, name)
            return when (val result = model.addConstraint(relation = constraint, name = name)) {
                is Ok -> ok
                is Failed -> Failed(result.error)
                is Fatal -> Fatal(result.errors)
            }
        }
        return ok
    }

    companion object {
        /** 创建 [QuadraticLinearFunction] 实例。 / Create a [QuadraticLinearFunction] instance. */
        operator fun <V> invoke(
            polynomial: QuadraticPolynomial<V>,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): QuadraticLinearFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            QuadraticLinearFunction(polynomial, converter, name, displayName)
    }
}
