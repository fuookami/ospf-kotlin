@file:Suppress("unused")

/** 函数符号基类 / Function symbol base class */
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*

/**
 * 函数符号核心接口与适配器 / Function symbol core interfaces and adapters
 *
 * 定义数学函数符号的基础接口（[MathFunctionSymbol]、[MathFunctionSymbolBase]）及
 * 线性函数符号适配器 [LinearFunctionSymbolAdapter]，用于将函数符号集成到
 * 中间符号体系中。
 *
 * Defines base interfaces for math function symbols ([MathFunctionSymbol],
 * [MathFunctionSymbolBase]) and the [LinearFunctionSymbolAdapter] for integrating
 * function symbols into the intermediate symbol system.
*/

/**
 * 函数符号注册生命周期的 V 泛型基类。
 * V-generic base for function symbol registration lifecycle.
 *
 * [registerAuxiliaryTokens] 与 [registerConstraints] 都在 V 类型边界上工作。
 * 实现会通过 [IntoValue]<V> 转换器，在内部构造约束时完成 V 与 Flt64 之间的转换。
 *
 * Both [registerAuxiliaryTokens] and [registerConstraints] operate on V-generic
 * boundaries. Implementations use their [IntoValue]<V> converter to convert
 * between V-generic data and Flt64 when constructing constraints internally.
 *
 * 运行时 token 集合与机制模型仍位于 Flt64 求解器边界，因此调用点会传入
 * `AddableTokenCollection<Flt64>` 与
 * `AbstractLinearMechanismModel<Flt64>`，
 * 它们也是 V 类型接口的子类型。
 *
 * At runtime, the token collection and mechanism model are always Flt64-based
 * (solver boundary), so call sites pass `AddableTokenCollection<Flt64>` and
 * `AbstractLinearMechanismModel<Flt64>` which are subtypes of the V-generic interfaces.
*/
interface MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 注册辅助变量到 token 集合 / Register auxiliary variables to the token collection
     * @param tokens 可添加 token 的集合 / the addable token collection
     * @return 操作结果 / operation result
    */
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try

    /**
     * 将线性约束注册到机制模型 / Register linear constraints to the mechanism model
     * @param model 线性机制模型 / the linear mechanism model
     * @return 操作结果 / operation result
    */
    fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try
}

/**
 * 可选接口，用于暴露结果多项式的 [MathFunctionSymbol] 实现。
 * Optional interface for [MathFunctionSymbol] implementations that expose
 * a result polynomial. [LinearFunctionSymbolAdapter] uses this to provide
 * a non-zero [polynomial] so that model.maximize(fn) and LinearPolynomial(fn)
 * produce the correct objective term.
 * [LinearFunctionSymbolAdapter] 使用此接口提供非零 [polynomial]，
 * 使 model.maximize(fn) 和 LinearPolynomial(fn) 产生正确的目标项。
 *
 * @property resultPolynomial 结果线性多项式 / The result linear polynomial
*/
interface HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

    /** Result linear polynomial of the function / 函数的结果线性多项式 */
    val resultPolynomial: LinearPolynomial<V>
}

/**
 * 基于数学符号的函数符号的基础接口。
 * Base interface for math-symbol-based function symbols.
 * 每个函数符号创建辅助变量并生成线性约束。
 * Each function symbol creates helper variables and generates linear constraints.
 *
 * @param V 数值类型（必须实现 RealNumber 和 NumberField）/ the numeric type (must implement RealNumber and NumberField).
 * @property name 函数符号名称 / Function symbol name
 * @property displayName 可选显示名称 / Optional display name
*/
interface MathFunctionSymbol<V> : MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    var name: String
    var displayName: String?

    /**
     * 此函数创建的辅助变量（如正/负松弛变量）。
     * Helper variables created by this function (e.g. pos/neg slack variables).
     * 暴露出来以便框架在目标函数中引用它们。
     * Exposed so the framework can reference them in objectives.
    */
    val helperVariables: List<AbstractVariableItem<*, *>>

    /**
     * 在给定已解析的符号值下计算此函数符号。
     * Evaluate this function symbol given resolved symbol values.
     * @param values 符号到值的映射 / symbol-to-value mapping
     * @return 计算结果，若输入未解析则为 null / evaluation result, or null if input unresolved
    */
    fun evaluate(values: Map<Symbol, V>): V?
}

/**
 * 二次函数符号注册的内部非泛型基类。
 * Internal non-generic base for quadratic function symbol registration.
 *
 * 镜像 [MathFunctionSymbolBase]，但用于二次机制模型。
 * Mirrors [MathFunctionSymbolBase] but for quadratic mechanism models.
 * 这是一个内部求解器边界接口。
 * This is an internal solver-boundary interface.
*/
internal interface QuadraticMathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 注册辅助变量到 token 集合 / Register auxiliary variables to the token collection
     * @param tokens 可添加 token 的集合 / the addable token collection
     * @return 操作结果 / operation result
    */
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try

    /**
     * 将约束注册到二次机制模型 / Register constraints to the quadratic mechanism model
     * @param model 二次机制模型 / the quadratic mechanism model
     * @return 操作结果 / operation result
    */
    fun registerConstraints(model: AbstractQuadraticMechanismModel<V>): Try
}

/**
 * 线性函数符号适配器 / Linear function symbol adapter
 *
 * 将 [MathFunctionSymbol] 包装为 [LinearIntermediateSymbol]，使函数符号可以
 * 存储在框架使用的 [LinearIntermediateSymbols1] 等容器中。
 *
 * Wraps a [MathFunctionSymbol] as a [LinearIntermediateSymbol], allowing function
 * symbols to be stored in framework containers such as [LinearIntermediateSymbols1].
 *
 * @property delegate 被包装的函数符号 / The wrapped function symbol
 * @property converter V 类型值转换器 / V-type value converter
 * @property pos 正松弛变量多项式 / Positive slack variable polynomial
 * @property neg 负松弛变量多项式 / Negative slack variable polynomial
 * @property polyX 带松弛调整的输入多项式 / Input polynomial with slack adjustments
*/
class LinearFunctionSymbolAdapter<V>(
    val delegate: MathFunctionSymbol<V>,
    private val converter: IntoValue<V>
) : LinearIntermediateSymbol<V>, MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {
    override var name: String
        get() = delegate.name
        set(value) { delegate.name = value }

    override var displayName: String?
        get() = delegate.displayName
        set(value) { delegate.displayName = value }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = delegate.helperVariables

    /**
     * 将正松弛变量暴露为 LinearPolynomial<V>。
     * Expose positive slack variable as a LinearPolynomial<V>.
     * 仅当委托是 withPositive=true 的 SlackFunction 时才有意义。
     * Only meaningful when the delegate is a SlackFunction with withPositive=true.
    */
    val pos: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val posVar = slack?.posVar ?: slackRange?.posVar
        posVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, v)),
                constant = converter.zero
            )
        }
    }

    /**
     * 负松弛变量多项式 / Negative slack variable polynomial
     * 仅当委托是 SlackFunction(withNegative=true) 或 SlackRangeFunction 时有意义。
     * Only meaningful when the delegate is a SlackFunction with withNegative=true or a SlackRangeFunction.
    */
    val neg: LinearPolynomial<V>? by lazy {
        val slack = delegate as? SlackFunction<V>
        val slackRange = delegate as? SlackRangeFunction<V>
        val negVar = slack?.negVar ?: slackRange?.negVar
        negVar?.let { v ->
            LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, v)),
                constant = converter.zero
            )
        }
    }

    /**
     * 带松弛调整的输入多项式 / Input polynomial with slack adjustments
     * 仅当委托是 SlackFunction 或 SlackRangeFunction 时有意义。
     * Only meaningful when the delegate is a SlackFunction or SlackRangeFunction.
    */
    val polyX: LinearPolynomial<V>? by lazy {
        when (val d = delegate) {
            is SlackFunction<V> -> {
                val unit = converter.one
                var result = LinearPolynomial(d.x.monomials.toMutableList(), d.x.constant)
                if (d.withNegative && d.negVar != null) {
                    result = LinearPolynomial(result.monomials + LinearMonomial(unit, d.negVar!!), result.constant)
                }
                if (d.withPositive && d.posVar != null) {
                    result = LinearPolynomial(result.monomials + LinearMonomial(-unit, d.posVar!!), result.constant)
                }
                result
            }
            is SlackRangeFunction<V> -> {
                val unit = converter.one
                var result = LinearPolynomial(d.x.monomials.toMutableList(), d.x.constant)
                result = LinearPolynomial(result.monomials + LinearMonomial(unit, d.negVar), result.constant)
                result = LinearPolynomial(result.monomials + LinearMonomial(-unit, d.posVar), result.constant)
                result
            }
            else -> null
        }
    }

    override fun evaluate(values: Map<Symbol, V>): V? = delegate.evaluate(values)

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = delegate.registerAuxiliaryTokens(tokens)

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try = delegate.registerConstraints(model)

    override val identifier: UInt64 get() = IdentifierGenerator.gen()
    override val index: Int get() = 0
    override val category: Category get() = fuookami.ospf.kotlin.math.symbol.Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    override val range: ExpressionRange<V> get() = SolverBoundaryCasts.fullExpressionRange()

    override fun flush(force: Boolean) {}

    /**
     * 求解器准备阶段的计算入口 / Solver preparation phase evaluation entry point
     * @param values Flt64 符号值映射，可为 null / Flt64 symbol-value mapping, may be null
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @return 计算结果 / evaluation result
    */
    internal fun prepareSolver(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        val targetValues = values?.let { SolverBoundaryCasts.mapValues(it, converter) }
        return if (targetValues.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(targetValues, tokenTable, converter, false)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    /**
     * 获取委托的结果多项式，若不存在则返回零多项式。
     * Get the delegate's result polynomial, or a zero polynomial if unavailable.
     *
     * @return 结果线性多项式 / result linear polynomial
    */
    /**
     * 获取委托的结果多项式，若不存在则返回零多项式。
     * Get the delegate's result polynomial, or a zero polynomial if unavailable.
     *
     * @return 结果线性多项式 / result linear polynomial
    */
    @Suppress("UNCHECKED_CAST")
    private fun resultPolynomialOrZero(): LinearPolynomial<V> {
        // 安全不变量：本模块内 HasResultPolynomial 的实现与 delegate 使用相同的 V 类型参数。
        // Safety invariant: HasResultPolynomial implementations in this module use the same V type parameter as delegate.
        return (delegate as? HasResultPolynomial<*>)?.resultPolynomial as? LinearPolynomial<V>
            ?: LinearPolynomial(emptyList(), converter.zero)
    }

    /** 展平后的单项式数据（来自结果多项式）/ Flattened monomial data from the result polynomial */
    internal val flattenedMonomials: LinearFlattenData<V> get() {
        val poly = resultPolynomialOrZero()
        return LinearFlattenData(poly.monomials, poly.constant)
    }

    override val polynomial: LinearPolynomial<V>
        get() = resultPolynomialOrZero()

    override fun asMutable(): MutableLinearPolynomial<V> {
        val poly = resultPolynomialOrZero()
        return MutableLinearPolynomial(poly.monomials, poly.constant)
    }

    /**
     * 基于 token 列表的 Flt64 求值（默认返回 null）/ Flt64 evaluation based on token list (default returns null)
     * @param tokenList token 列表 / the token list
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /**
     * 基于结果列表和 token 列表的 Flt64 求值（默认返回 null）/ Flt64 evaluation based on results and token list (default returns null)
     * @param results 结果值列表 / the result value list
     * @param tokenList token 列表 / the token list
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? = null

    /**
     * 基于符号值映射的 Flt64 求值 / Flt64 evaluation based on symbol-value mapping
     * @param values Flt64 符号值映射 / Flt64 symbol-value mapping
     * @param tokenList token 列表，可为 null / the token list, may be null
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        val v = delegate.evaluate(SolverBoundaryCasts.mapValues(values, converter)) ?: return null
        return converter.fromValue(v)
    }

    // V-generic evaluate overrides (P4-5) - delegate to Flt64-boundary evaluate + converter
    // V 类型求值重写 (P4-5) - 委托给 Flt64 边界求值 + 转换器
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            evaluate(tokenTable, converter, false)
        } else {
            evaluate(values, tokenTable, converter, false)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for (token in tokenTable.tokensInSolver) {
            val tokenValue = token.result ?: if (zeroIfNone) converter.zero else return null
            values[token.variable] = tokenValue
        }
        return delegate.evaluate(values)
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for ((index, token) in tokenTable.tokensInSolver.withIndex()) {
            val value = if (index < results.size) {
                results[index]
            } else {
                if (!zeroIfNone) {
                    return null
                }
                converter.zero
            }
            values[token.variable] = value
        }
        return delegate.evaluate(values)
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(values)
    }

    /**
     * 求解器结果转换为 V 类型后求值 / Evaluate after converting solver results to V type
     * @param results Flt64 结果列表 / Flt64 result list
     * @param tokenTable token 表 / the token table
     * @param converter 值类型转换器 / value type converter
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluateSolver(results: List<Flt64>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        val targetResults = results.map { converter.intoValue(it) }
        return evaluate(targetResults, tokenTable, converter, zeroIfNone)
    }

    /**
     * 求解器符号值转换为 V 类型后求值 / Evaluate after converting solver symbol values to V type
     * @param values Flt64 符号值映射 / Flt64 symbol-value mapping
     * @param tokenTable token 表，可为 null / the token table, may be null
     * @param converter 值类型转换器 / value type converter
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluateSolver(values: Map<Symbol, Flt64>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return delegate.evaluate(SolverBoundaryCasts.mapValues(values, converter))
    }
}

// ---- Converter-based helpers (safe, no unchecked casts) ----

/**
 * 使用提供的转换器将 LinearPolynomial<V> 转换为 LinearPolynomial<Flt64>。
 * Convert LinearPolynomial<V> to LinearPolynomial<Flt64> using the provided converter.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 转换后的 Flt64 类型线性多项式 / the converted Flt64-type linear polynomial
*/
internal fun <V> LinearPolynomial<V>.asFlt64Poly(converter: IntoValue<V>): LinearPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return LinearPolynomial(
        monomials.map { LinearMonomial(converter.fromValue(it.coefficient), it.symbol) },
        converter.fromValue(constant)
    )
}

/**
 * 使用提供的转换器将 QuadraticPolynomial<V> 转换为 QuadraticPolynomial<Flt64>。
 * Convert QuadraticPolynomial<V> to QuadraticPolynomial<Flt64> using the provided converter.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 转换后的 Flt64 类型二次多项式 / the converted Flt64-type quadratic polynomial
*/
internal fun <V> QuadraticPolynomial<V>.asFlt64QuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<Flt64> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.fromValue(constant)
    )
}

/**
 * 使用提供的转换器将 QuadraticPolynomial<Flt64> 转换为 QuadraticPolynomial<V>。
 * Convert QuadraticPolynomial<Flt64> to QuadraticPolynomial<V> using the provided converter.
 *
 * @param converter 值类型转换器 / value type converter
 * @return 转换后的 V 类型二次多项式 / the converted V-type quadratic polynomial
*/
internal fun <V> QuadraticPolynomial<Flt64>.asVQuadraticPoly(converter: IntoValue<V>): QuadraticPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {
    return QuadraticPolynomial(
        monomials.map { QuadraticMonomial(converter.intoValue(it.coefficient), it.symbol1, it.symbol2) },
        converter.intoValue(constant)
    )
}
