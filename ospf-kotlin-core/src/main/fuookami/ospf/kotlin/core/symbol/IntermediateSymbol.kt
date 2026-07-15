@file:Suppress("unused")

/**
 * 中间符号 / Intermediate symbols
 *
 * 定义数学优化模型中的中间符号核心接口与实现。
 * Defines core interfaces and implementations for intermediate symbols in mathematical optimization models.
*/
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try

/**
 * 中间符号核心接口定义 / Core intermediate symbol interface definitions
 *
 * 提供 [IntermediateSymbol]、[LinearIntermediateSymbol]、[QuadraticIntermediateSymbol] 等
 * 中间符号的基础接口，用于数学优化模型中的符号表达式求值与管理。
 *
 * Provides base interfaces for intermediate symbols such as [IntermediateSymbol],
 * [LinearIntermediateSymbol], and [QuadraticIntermediateSymbol], used for symbolic
 * expression evaluation and management in mathematical optimization models.
*/

/**
 * 中间符号接口 / Intermediate symbol interface
 *
 * 数学优化模型中所有中间符号的基础接口。中间符号封装了表达式求值逻辑，
 * 支持缓存、依赖追踪和边界管理。
 *
 * Base interface for all intermediate symbols in mathematical optimization models.
 * Intermediate symbols encapsulate expression evaluation logic and support caching,
 * dependency tracking, and bound management.
 *
 * @property name 符号名称 / Symbol name
 * @property displayName 可选显示名称 / Optional display name
 * @property discrete 是否为离散变量 / Whether the symbol is discrete
 * @property range 表达式值域 / Expression value range
 * @property category 符号类别 / Symbol category
 * @property cached 是否已缓存 / Whether the value is cached
 * @property parent 父符号 / Parent symbol
 * @property dependencies 依赖符号集合 / Set of dependency symbols
 * @property identifier 全局唯一标识符 / Globally unique identifier
 * @property index 在所属组合中的索引 / Index within owning combination
*/
interface IntermediateSymbol<V> : Symbol where V : RealNumber<V>, V : NumberField<V> {
    override var name: String
    override var displayName: String?

    /** Whether this symbol represents a discrete variable / 是否表示离散变量 */
    val discrete: Boolean get() = false

    /** Value range of this symbol / 此符号的值范围 */
    val range: ExpressionRange<V>

    /** Lower bound of the value range / 值范围的下界 */
    val lowerBound: Bound<V>? get() = range.lowerBound

    /** Upper bound of the value range / 值范围的上界 */
    val upperBound: Bound<V>? get() = range.upperBound

    /** Fixed value if the range is a singleton / 若范围为单点时的固定值 */
    val fixedValue: V? get() = range.fixedValue

    // --- V-generic primary path (abstract) ---

    /**
     * 准备符号值，根据固定值和令牌表进行求值。
     * Prepare symbol value by evaluating with fixed values and token table.
     *
     * @param values 固定值映射（可空） / Fixed values map (nullable)
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @return 求值结果，若无法求值则返回 null / Evaluation result, or null if not evaluable
    */
    fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V?

    /**
     * 准备符号值并缓存结果。
     * Prepare symbol value and cache the result.
     *
     * @param values 固定值映射（可空） / Fixed values map (nullable)
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
    */
    fun prepareAndCache(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>) {
        if (values.isNullOrEmpty()) {
            prepare(null, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, solution = null, value = it)
            }
        } else {
            prepare(values, tokenTable, converter)?.let {
                tokenTable.cache(cacheKey = this, fixedValues = values, value = it)
            }
        }
    }

    /**
     * 使用令牌表求值符号表达式。
     * Evaluate symbol expression using token table.
     *
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
    */
    fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?

    /**
     * 使用求解器结果列表和令牌表求值符号表达式。
     * Evaluate symbol expression using solver results list and token table.
     *
     * @param results 求解器结果列表 / Solver results list
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
    */
    fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?

    /**
     * 使用固定值映射和令牌表求值符号表达式。
     * Evaluate symbol expression using fixed values map and token table.
     *
     * @param values 固定值映射 / Fixed values map
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
    */
    fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?

    /**
     * 从令牌表直接求值符号表达式。
     * Evaluate symbol expression directly from token table.
     *
     * @param tokenTable 令牌表 / Token table
     * @param converter 值转换器 / Value converter
     * @param zeroIfNone 无值时是否返回零 / Whether to return zero when value is absent
     * @return 求值结果 / Evaluation result
    */
    fun evaluateFromTokens(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V? =
        evaluate(tokenTable, converter, zeroIfNone)

    val category: Category
    val operationCategory: Category get() = category

    val cached: Boolean
    val parent: IntermediateSymbol<*>? get() = null
    val args: Any? get() = parent?.args
    val dependencies: Set<IntermediateSymbol<*>>

    val identifier: UInt64
    val index: Int

    /**
     * 刷新符号缓存。
     * Flush symbol cache.
     *
     * @param force 是否强制刷新 / Whether to force flush
    */
    fun flush(force: Boolean = false)

    /**
     * 注册辅助令牌到令牌集合。
     * Register auxiliary tokens to the token collection.
     *
     * @param tokens 可添加的令牌集合 / Addable token collection
     * @return 操作结果 / Operation result
    */
    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

    /**
     * 获取符号的原始字符串表示。
     * Get raw string representation of the symbol.
     *
     * @param unfold 展开深度 / Unfold depth
     * @return 原始字符串 / Raw string
    */
    fun toRawString(unfold: UInt64 = UInt64.zero): String
}

/**
 * 线性中间符号接口 / Linear intermediate symbol interface
 *
 * 表示可转换为线性多项式的中间符号。支持可变与不可变多项式访问。
 *
 * Represents an intermediate symbol that can be converted to a linear polynomial.
 * Supports both mutable and immutable polynomial access.
 *
 * @property polynomial 对应的线性多项式 / The associated linear polynomial
*/
interface LinearIntermediateSymbol<V> : IntermediateSymbol<V>, ToLinearPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        /**
         * 创建空的线性中间符号。
         * Create an empty linear intermediate symbol.
         *
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 空的线性中间符号 / Empty linear intermediate symbol
        */
        fun <V> empty(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): LinearIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return LinearExpressionSymbol(
                _utilsPolynomial = MutableLinearPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: LinearPolynomial<V>

    /**
     * 获取可变线性多项式表示。
     * Get mutable linear polynomial representation.
     *
     * @return 可变线性多项式 / Mutable linear polynomial
    */
    fun asMutable(): MutableLinearPolynomial<V>

    override fun toLinearPolynomial(): LinearPolynomial<V> = polynomial
}

/**
 * 二次中间符号接口 / Quadratic intermediate symbol interface
 *
 * 表示可转换为二次多项式的中间符号。支持可变与不可变多项式访问。
 *
 * Represents an intermediate symbol that can be converted to a quadratic polynomial.
 * Supports both mutable and immutable polynomial access.
 *
 * @property polynomial 对应的二次多项式 / The associated quadratic polynomial
*/
interface QuadraticIntermediateSymbol<V> : IntermediateSymbol<V>, ToQuadraticPolynomial<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
    companion object {
        /**
         * 创建空的二次中间符号。
         * Create an empty quadratic intermediate symbol.
         *
         * @param constants 实数常量定义 / Real number constants definition
         * @param parent 父级符号（可空） / Parent symbol (nullable)
         * @param name 符号名称 / Symbol name
         * @param displayName 显示名称（可空） / Display name (nullable)
         * @return 空的二次中间符号 / Empty quadratic intermediate symbol
        */
        fun <V> empty(
            constants: RealNumberConstants<V>,
            parent: IntermediateSymbol<*>? = null,
            name: String = "",
            displayName: String? = null
        ): QuadraticIntermediateSymbol<V> where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
            return QuadraticExpressionSymbol(
                _utilsPolynomial = MutableQuadraticPolynomial(
                    monomials = emptyList(),
                    constant = constants.zero
                ),
                category = Quadratic,
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    val polynomial: QuadraticPolynomial<V>

    /**
     * 获取可变二次多项式表示。
     * Get mutable quadratic polynomial representation.
     *
     * @return 可变二次多项式 / Mutable quadratic polynomial
    */
    fun asMutable(): MutableQuadraticPolynomial<V>

    override fun toQuadraticPolynomial(): QuadraticPolynomial<V> = polynomial
}
