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
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.operation.ToLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.operation.ToQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.reciprocal
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.IdentifierGenerator
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractQuadraticMechanismModel

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

    val discrete: Boolean get() = false

    val range: ExpressionRange<V>
    val lowerBound: Bound<V>? get() = range.lowerBound
    val upperBound: Bound<V>? get() = range.upperBound
    val fixedValue: V? get() = range.fixedValue

    // --- V-typed primary path (abstract) ---
    fun prepare(values: Map<Symbol, V>?, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>): V?

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

    fun evaluate(tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
    fun evaluate(values: Map<Symbol, V>, tokenTable: AbstractTokenTable<V>?, converter: IntoValue<V>, zeroIfNone: Boolean = false): V?
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

    fun flush(force: Boolean = false)

    fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try = ok

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

    fun asMutable(): MutableQuadraticPolynomial<V>

    override fun toQuadraticPolynomial(): QuadraticPolynomial<V> = polynomial
}
