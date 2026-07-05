/**
 * 乘积函数符号 / Product function symbol
 *
 * 提供 [ProductFunction]，实现两个线性多项式乘积 y = left * right 的二次建模。
 *
 * Provides [ProductFunction] for quadratic modeling of the product of two linear polynomials y = left * right.
 */
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
import fuookami.ospf.kotlin.multiarray.Shape
import fuookami.ospf.kotlin.utils.functional.*

private typealias ProductIntermediate<V> = IntermediateSymbol<out V>

/**
 * 两个线性多项式的乘积：y = left * right。
 * Product of two linear polynomials: y = left * right.
 *
 * @property left 左侧线性多项式 / left linear polynomial
 * @property right 右侧线性多项式 / right linear polynomial
 * @property converter 值类型转换器 / value type converter
 * @property name 此函数的唯一名称 / unique name for this function
 * @property displayName 可选的人类可读显示名称 / optional human-readable display name
 * @param V 多项式系数的值类型 / value type for the polynomial coefficients.
 */
class ProductFunction<V>(
    val left: LinearPolynomial<V>,
    val right: LinearPolynomial<V>,
    private val converter: IntoValue<V>,
    override var name: String = "product",
    override var displayName: String? = null
) : QuadraticIntermediateSymbol<V>, QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    internal var _group: AbstractSymbolCombination<out Shape>? = null
    internal var _index: Int? = null
    override val identifier: UInt64 by lazy {
        _group?.identifier ?: IdentifierGenerator.gen()
    }
    override val index: Int by lazy {
        _index ?: 0
    }

    override val category: Category = Quadratic
    override val parent: ProductIntermediate<V>? = null
    override val operationCategory: Category = Quadratic

    override val dependencies: Set<ProductIntermediate<V>>
        get() {
            val deps = mutableSetOf<ProductIntermediate<V>>()
            for (m in left.monomials) {
                val s = m.symbol
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(s)?.let { deps.add(it) }
            }
            for (m in right.monomials) {
                val s = m.symbol
                SolverBoundaryCasts.symbolAsIntermediateStar<V>(s)?.let { deps.add(it) }
            }
            return deps
        }

    override val cached: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {
        for (dep in dependencies) {
            dep.flush(force)
        }
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
     * 求值线性多项式。
     * Evaluate a linear polynomial.
     *
     * @param poly 要求值的线性多项式 / the linear polynomial to evaluate
     * @param tokenTable token 表 / the token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 多项式值或 null / polynomial value or null
     */
    private fun evaluateLinear(
        poly: LinearPolynomial<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    /**
     * 从结果列表求值线性多项式。
     * Evaluate a linear polynomial from a results list.
     *
     * @param poly 要求值的线性多项式 / the linear polynomial to evaluate
     * @param results 结果值列表 / list of result values
     * @param tokenTable token 表 / the token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 多项式值或 null / polynomial value or null
     */
    private fun evaluateLinearFromResults(
        poly: LinearPolynomial<V>,
        results: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, results, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    /**
     * 从值映射求值线性多项式。
     * Evaluate a linear polynomial from a value map.
     *
     * @param poly 要求值的线性多项式 / the linear polynomial to evaluate
     * @param values 符号到值的映射 / symbol-to-value map
     * @param tokenTable 可选的 token 表 / optional token table
     * @param zeroIfNone 若为 true，缺失时返回零；否则返回 null / if true, return zero when missing; otherwise null
     * @return 多项式值或 null / polynomial value or null
     */
    private fun evaluateLinearFromValues(
        poly: LinearPolynomial<V>,
        values: Map<Symbol, V>,
        tokenTable: AbstractTokenTable<V>?,
        zeroIfNone: Boolean
    ): V? {
        var value = poly.constant
        for (monomial in poly.monomials) {
            val symbolValue = evaluateSymbol(monomial.symbol, values, tokenTable, zeroIfNone) ?: return null
            value += monomial.coefficient * symbolValue
        }
        return value
    }

    /**
     * 使用 Flt64 值预计算求解器结果。
     * Pre-compute solver result with Flt64 values.
     *
     * @param values 符号到 Flt64 值的映射，可为 null / symbol-to-Flt64 value map, or null
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @return 预计算结果或 null / pre-computed result or null
     */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val targetValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        val leftValue = if (targetValues.isNullOrEmpty()) {
            evaluateLinear(left, tokenTable, false)
        } else {
            evaluateLinearFromValues(left, targetValues, tokenTable, false)
        } ?: return null

        val rightValue = if (targetValues.isNullOrEmpty()) {
            evaluateLinear(right, tokenTable, false)
        } else {
            evaluateLinearFromValues(right, targetValues, tokenTable, false)
        } ?: return null

        return leftValue * rightValue
    }

    /**
     * 将 left * right 展开为 V 类型二次多项式。
     * Expand left * right into a V-generic quadratic polynomial.
     *
     * @return 展开后的二次多项式 / the expanded quadratic polynomial
     */
    private fun expandedQuadraticPoly(): QuadraticPolynomial<V> {
        val leftC = left
        val rightC = right
        val leftConst = leftC.constant
        val rightConst = rightC.constant

        val monomials = mutableListOf<QuadraticMonomial<V>>()

        for (lm in leftC.monomials) {
            for (rm in rightC.monomials) {
                monomials.add(QuadraticMonomial(
                    lm.coefficient * rm.coefficient,
                    lm.symbol,
                    rm.symbol
                ))
            }
        }

        for (lm in leftC.monomials) {
            monomials.add(QuadraticMonomial.linear(
                lm.coefficient * rightConst,
                lm.symbol
            ))
        }
        for (rm in rightC.monomials) {
            monomials.add(QuadraticMonomial.linear(
                rm.coefficient * leftConst,
                rm.symbol
            ))
        }

        return QuadraticPolynomial(monomials, leftConst * rightConst)
    }

    override val polynomial: QuadraticPolynomial<V>
        get() = expandedQuadraticPoly()

    override fun asMutable(): MutableQuadraticPolynomial<V> = MutableQuadraticPolynomial(emptyList(), converter.zero)

    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val leftValue = if (values.isNullOrEmpty()) {
            evaluateLinear(left, tokenTable, false)
        } else {
            evaluateLinearFromValues(left, values, tokenTable, false)
        } ?: return null

        val rightValue = if (values.isNullOrEmpty()) {
            evaluateLinear(right, tokenTable, false)
        } else {
            evaluateLinearFromValues(right, values, tokenTable, false)
        } ?: return null

        return leftValue * rightValue
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinear(left, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinear(right, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinearFromResults(left, results, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinearFromResults(right, results, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val leftValue = evaluateLinearFromValues(left, values, tokenTable, zeroIfNone) ?: return null
        val rightValue = evaluateLinearFromValues(right, values, tokenTable, zeroIfNone) ?: return null
        return leftValue * rightValue
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold neq UInt64.zero) {
            "product($left, $right)"
        } else {
            displayName ?: name
        }
    }

    override fun hashCode(): Int = identifier.toInt() * 31 + index
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        @Suppress("UNCHECKED_CAST")
        other as ProductFunction<V>
        return identifier == other.identifier && index == other.index && name == other.name
    }
    override fun toString(): String = displayName ?: name

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    override fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try {
        val poly = polynomial
        val rhs = QuadraticPolynomial<V>(constant = converter.zero)
        val inequality = QuadraticInequalityOf(poly, rhs, Comparison.EQ, "${name}_eq")
        return addQuadraticConstraints(model, listOf(inequality)) ?: ok
    }

    companion object {
        /** 创建 [ProductFunction] 实例。 / Create a [ProductFunction] instance. */
        operator fun <V> invoke(
            left: LinearPolynomial<V>,
            right: LinearPolynomial<V>,
            converter: IntoValue<V>,
            name: String = "product",
            displayName: String? = null
        ): ProductFunction<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> =
            ProductFunction(left, right, converter, name, displayName)
    }
}
