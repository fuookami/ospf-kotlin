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
 * 中间符号体系中。 / Defines base interfaces for math function symbols ([MathFunctionSymbol],
 * [MathFunctionSymbolBase]) and the [LinearFunctionSymbolAdapter] for integrating
 * function symbols into the intermediate symbol system.
*/

/**
 * 函数符号注册生命周期的 V 泛型基类。 / V-generic base for function symbol registration lifecycle.
 *
 * [registerAuxiliaryTokens] 与 [registerConstraints] 都在 V 类型边界上工作。
 * 实现会通过 [IntoValue]<V> 转换器，在内部构造约束时完成 V 与 Flt64 之间的转换。 / Both [registerAuxiliaryTokens] and [registerConstraints] operate on V-generic
 * boundaries. Implementations use their [IntoValue]<V> converter to convert
 * between V-generic data and Flt64 when constructing constraints internally.
 *
 * 运行时 token 集合与机制模型仍位于 Flt64 求解器边界，因此调用点会传入
 * `AddableTokenCollection<Flt64>` 与
 * `AbstractLinearMechanismModel<Flt64>`，
 * 它们也是 V 类型接口的子类型。 / At runtime, the token collection and mechanism model are always Flt64-based
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
 * 可选接口，用于暴露结果多项式的 [MathFunctionSymbol] 实现。 / Optional interface for [MathFunctionSymbol] implementations that expose
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
 * 可选接口，用于暴露结果变量的 [MathFunctionSymbol] 实现。 / Optional interface for [MathFunctionSymbol] implementations that expose
 * a result variable.
 *
 * 适配器会将该变量转换为单位系数的结果多项式。 / The adapter converts this variable
 * to a unit-coefficient result polynomial.
 *
 * @property resultVar 结果变量 / Result variable
*/
interface HasResultVariable {

    /** 函数结果变量 / Function result variable */
    val resultVar: AbstractVariableItem<*, *>
}

/**
 * 基于数学符号的函数符号的基础接口。 / Base interface for math-symbol-based function symbols.
 * 每个函数符号创建辅助变量并生成线性约束。 / Each function symbol creates helper variables and generates linear constraints.
 *
 * @param V 数值类型（必须实现 RealNumber 和 NumberField）/ the numeric type (must implement RealNumber and NumberField).
 * @property name 函数符号名称 / Function symbol name
 * @property displayName 可选显示名称 / Optional display name
*/
interface MathFunctionSymbol<V> : MathFunctionSymbolBase<V> where V : RealNumber<V>, V : NumberField<V> {
    var name: String
    var displayName: String?

    /**
     * 此函数创建的辅助变量（如正/负松弛变量）。 / Helper variables created by this function (e.g. pos/neg slack variables).
     * 暴露出来以便框架在目标函数中引用它们。 / Exposed so the framework can reference them in objectives.
    */
    val helperVariables: List<AbstractVariableItem<*, *>>

    /**
     * 在给定已解析的符号值下计算此函数符号。 / Evaluate this function symbol given resolved symbol values.
     * @param values 符号到值的映射 / symbol-to-value mapping
     * @return 计算结果，若输入未解析则为 null / evaluation result, or null if input unresolved
    */
    fun evaluate(values: Map<Symbol, V>): V?
}

/**
 * 二次函数符号注册的内部非泛型基类。 / Internal non-generic base for quadratic function symbol registration.
 *
 * 镜像 [MathFunctionSymbolBase]，但用于二次机制模型。
 * Mirrors [MathFunctionSymbolBase] but for quadratic mechanism models.
 * 这是一个内部求解器边界接口。 / This is an internal solver-boundary interface.
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
    override val category: Category get() = Linear
    override val cached: Boolean get() = false
    override val dependencies: Set<IntermediateSymbol<*>> get() = emptySet()
    override val discrete: Boolean get() = false
    /**
     * 函数结果的表达式值域；分段线性函数可提供有限的 solver 边界值域。
     * Expression range of the function result; piecewise-linear delegates can expose
     * a finite range at the solver boundary.
     */
    override val range: ExpressionRange<V>
        get() = try {
            when (val function = delegate) {
                is UnivariateLinearPiecewiseFunction<*> -> when (val result = function.resolveOutputRange()) {
                    is Ok -> SolverBoundaryCasts.expressionRangeFromFlt64<V>(result.value)
                    is Failed, is Fatal -> SolverBoundaryCasts.fullExpressionRange()
                }
                else -> SolverBoundaryCasts.fullExpressionRange()
            }
        } catch (_: RuntimeException) {
            // A range is a non-Result compatibility property; malformed delegates must not
            // escape through it. Registration still reports the detailed failure via Ret/Try.
            SolverBoundaryCasts.fullExpressionRange()
        }

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
            semanticValuesFromTokens(tokenTable, converter)?.let { delegate.evaluate(it) }
        } else {
            delegate.evaluate(targetValues)
        }
    }
    override fun toRawString(unfold: UInt64): String = name

    /**
     * 获取委托的结果多项式；旧函数通过兼容结果属性解析。 / Get the delegate's result polynomial;
     * legacy functions are resolved through compatible result properties.
     *
     * `HasResultPolynomial` 是首选契约；未实现该接口的旧函数可以通过公开的 `resultPolynomial`、
     * `result` 或 `resultVar` 属性提供结果。 / `HasResultPolynomial` is the preferred contract;
     * legacy functions that do not implement it can expose their result through a public
     * `resultPolynomial`, `result`, or `resultVar` property.
     *
     * @return 结果线性多项式；无法解析时保留零多项式兼容行为 / result linear polynomial;
     * zero polynomial for delegates without a result contract, preserving compatibility
    */
    @Suppress("UNCHECKED_CAST")
    private fun resultPolynomialOrZero(): LinearPolynomial<V> {
        val resultPolynomial = try {
            (delegate as? HasResultPolynomial<*>)?.resultPolynomial
        } catch (_: RuntimeException) {
            null
        }
        if (resultPolynomial != null) {
            return resultPolynomial as LinearPolynomial<V>
        }

        resolveLegacyResultPolynomial()?.let { return it }

        val resultVariable = try {
            (delegate as? HasResultVariable)?.resultVar
        } catch (_: RuntimeException) {
            null
        }
        if (resultVariable != null) {
            return LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, resultVariable)),
                constant = converter.zero
            )
        }

        resolveLegacyResultVariable()?.let { resultVariable ->
            return LinearPolynomial(
                monomials = listOf(LinearMonomial(converter.one, resultVariable)),
                constant = converter.zero
            )
        }

        return LinearPolynomial(emptyList(), converter.zero)
    }

    /**
     * 解析未迁移旧函数公开的结果多项式属性。 / Resolve a result polynomial property exposed by
     * a legacy function that has not migrated to the marker interface.
     */
    @Suppress("UNCHECKED_CAST")
    private fun resolveLegacyResultPolynomial(): LinearPolynomial<V>? {
        val resultPolynomial = invokeLegacyResultAccessor("getResultPolynomial")
        if (resultPolynomial is LinearPolynomial<*>) {
            return resultPolynomial as LinearPolynomial<V>
        }

        val result = invokeLegacyResultAccessor("getResult")
        return if (result is LinearPolynomial<*>) {
            result as LinearPolynomial<V>
        } else {
            null
        }
    }

    /**
     * 解析未迁移旧函数公开的结果变量属性。 / Resolve a result variable property exposed by a
     * legacy function that has not migrated to the marker interface.
     */
    private fun resolveLegacyResultVariable(): AbstractVariableItem<*, *>? {
        return invokeLegacyResultAccessor("getResultVar") as? AbstractVariableItem<*, *>
    }

    /**
     * 安全读取结果契约 getter；反射失败转换为缺失结果。 / Safely read a result-contract getter;
     * reflection failures are converted to an absent result.
     *
     * 这里使用属性契约而非函数名称或名称字符串，兼容尚未实现标记接口的旧函数。 /
     * This uses property contracts rather than function classes or name strings, keeping
     * compatibility with legacy functions that have not implemented the marker interface.
     */
    private fun invokeLegacyResultAccessor(accessorName: String): Any? {
        val accessor = try {
            delegate.javaClass.methods.firstOrNull { method ->
                method.name == accessorName && method.parameterCount == 0
            }
        } catch (_: SecurityException) {
            null
        } ?: return null

        return try {
            accessor.trySetAccessible()
            accessor.invoke(delegate)
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
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
    internal fun evaluate(tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return evaluateResultPolynomialFlt64(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> tokenList.find(symbol)?.resultFlt64
                    is LinearFunctionSymbolAdapter<*> -> if (symbol === this) {
                        null
                    } else {
                        tokenList.let { symbol.evaluate(it, zeroIfNone) }
                    }
                    else -> null
                }
            },
            zeroIfNone = zeroIfNone
        )
    }

    /**
     * 基于结果列表和 token 列表的 Flt64 求值（默认返回 null）/ Flt64 evaluation based on results and token list (default returns null)
     * @param results 结果值列表 / the result value list
     * @param tokenList token 列表 / the token list
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList<Flt64>, zeroIfNone: Boolean): Flt64? {
        return evaluateResultPolynomialFlt64(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> tokenList.indexOf(symbol)?.let { index ->
                        results.getOrNull(index)
                    }
                    is LinearFunctionSymbolAdapter<*> -> if (symbol === this) {
                        null
                    } else {
                        symbol.evaluate(results, tokenList, zeroIfNone)
                    }
                    else -> null
                }
            },
            zeroIfNone = zeroIfNone
        )
    }

    /**
     * 基于符号值映射的 Flt64 求值 / Flt64 evaluation based on symbol-value mapping
     * @param values Flt64 符号值映射 / Flt64 symbol-value mapping
     * @param tokenList token 列表，可为 null / the token list, may be null
     * @param zeroIfNone 缺失值时是否使用零 / whether to use zero for missing values
     * @return 计算结果 / evaluation result
    */
    internal fun evaluate(values: Map<Symbol, Flt64>, tokenList: AbstractTokenList<Flt64>?, zeroIfNone: Boolean): Flt64? {
        return evaluateResultPolynomialFlt64(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> values[symbol]
                        ?: tokenList?.find(symbol)?.resultFlt64
                    is LinearFunctionSymbolAdapter<*> -> if (symbol === this) {
                        null
                    } else {
                        tokenList?.let { symbol.evaluate(it, zeroIfNone) }
                    }
                    else -> values[symbol]
                } ?: if (zeroIfNone) Flt64.zero else null
            },
            zeroIfNone = zeroIfNone
        )
    }

    /**
     * 从 token 中提取语义准备阶段所需的输入值。/ Extract input values for semantic preparation.
     *
     * 准备阶段仍然需要调用 delegate 的公开语义；只有 solver 后验路径读取结果多项式。
     * Preparation still uses the delegate's public semantics; only solver-result paths read
     * the result polynomial.
     */
    private fun semanticValuesFromTokens(
        tokenTable: AbstractTokenTable<V>,
        converter: IntoValue<V>
    ): Map<Symbol, V>? {
        val values = LinkedHashMap<Symbol, V>(tokenTable.tokensInSolver.size)
        for (token in tokenTable.tokensInSolver) {
            values[token.variable] = token.result(converter) ?: return null
        }
        return values
    }

    /**
     * 按结果多项式读取实际 solver token；绝不重新执行 delegate classifier。
     * Read the actual solver tokens through the result polynomial; never re-run the
     * delegate classifier.
     */
    private fun evaluateResultPolynomial(
        valueOf: (Symbol) -> V?,
        zeroIfNone: Boolean,
        zero: V
    ): V? {
        val poly = resultPolynomialOrZero()
        var result = poly.constant
        for (monomial in poly.monomials) {
            val value = valueOf(monomial.symbol) ?: if (zeroIfNone) zero else return null
            result += monomial.coefficient * value
        }
        return result
    }

    /**
     * Flt64 solver 边界上的结果多项式求值。/ Evaluate the result polynomial at the Flt64 solver boundary.
     */
    private fun evaluateResultPolynomialFlt64(
        valueOf: (Symbol) -> Flt64?,
        zeroIfNone: Boolean
    ): Flt64? {
        val poly = resultPolynomialOrZero()
        var result = converter.fromValue(poly.constant)
        for (monomial in poly.monomials) {
            val value = valueOf(monomial.symbol) ?: if (zeroIfNone) Flt64.zero else return null
            result += converter.fromValue(monomial.coefficient) * value
        }
        return result
    }

    // V-generic evaluate overrides (P4-5) - semantic preparation and structural solver reads are separate.
    // V 类型求值重写 (P4-5) - 准备阶段语义求值与 solver 结构读取明确分离。
    override fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V? {
        return if (values.isNullOrEmpty()) {
            semanticValuesFromTokens(tokenTable, converter)?.let { delegate.evaluate(it) }
        } else {
            delegate.evaluate(values)
        }
    }
    override fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateResultPolynomial(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> tokenTable.find(symbol)?.result(converter)
                    is IntermediateSymbol<*> -> if (symbol === this) {
                        null
                    } else {
                        SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol)
                            .evaluate(tokenTable, converter, zeroIfNone)
                    }
                    else -> null
                }
            },
            zeroIfNone = zeroIfNone,
            zero = converter.zero
        )
    }
    override fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateResultPolynomial(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> tokenTable.indexOf(symbol)?.let { index ->
                        results.getOrNull(index)
                    }
                    is IntermediateSymbol<*> -> if (symbol === this) {
                        null
                    } else {
                        SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol)
                            .evaluate(results, tokenTable, converter, zeroIfNone)
                    }
                    else -> null
                }
            },
            zeroIfNone = zeroIfNone,
            zero = converter.zero
        )
    }
    override fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean): V? {
        return evaluateResultPolynomial(
            valueOf = { symbol ->
                when (symbol) {
                    is AbstractVariableItem<*, *> -> values[symbol]
                        ?: tokenTable?.find(symbol)?.result(converter)
                    is IntermediateSymbol<*> -> if (symbol === this) {
                        values[symbol]
                    } else {
                        values[symbol] ?: SolverBoundaryCasts.dependencyAsIntermediate<V>(symbol)
                            .evaluate(values, tokenTable, converter, zeroIfNone)
                    }
                    else -> values[symbol]
                }
            },
            zeroIfNone = zeroIfNone,
            zero = converter.zero
        )
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
        val targetValues = SolverBoundaryCasts.mapValues(values, converter)
        return evaluate(targetValues, tokenTable, converter, zeroIfNone)
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
