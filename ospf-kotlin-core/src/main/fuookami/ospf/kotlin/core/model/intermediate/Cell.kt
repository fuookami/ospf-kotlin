package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.Token
import fuookami.ospf.kotlin.core.token.AbstractTokenTable
import fuookami.ospf.kotlin.core.variable.VariableItemKey
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField

/**
 * Generic Cell<V> with typed public evaluation and Flt64 solver evaluation.
 * 泛型 Cell<V>：公开求值使用 V，求解器边界求值使用 Flt64。
 */
interface Cell<V : RealNumber<V>> {
    /** V-typed evaluation for public callers. / 面向调用方的 V 类型求值。 */
    fun evaluate(): V?
    fun evaluate(solution: List<V>): V?
    fun evaluate(solution: Map<VariableItemKey, V>): V?

    /** Flt64 evaluation for solver-boundary callers. / 面向求解器边界的 Flt64 求值。 */
    fun evaluateFlt64(): Flt64?
    fun evaluateFlt64(solution: List<Flt64>): Flt64?
    fun evaluateFlt64(solution: Map<VariableItemKey, Flt64>): Flt64?

    /** Explicit conversion helper kept for compatibility. / 保留给兼容路径使用的显式转换工具。 */
    fun evaluateAsV(converter: IntoValue<V>): V? = evaluateFlt64()?.let { converter.intoValue(it) }
}

typealias CellFlt64 = Cell<Flt64>

interface LinearCell<V : RealNumber<V>> : Cell<V> {
    /** V-typed coefficient for public callers. / 面向调用方的 V 类型系数。 */
    val coefficient: V
    val token: Token<V>

    /** Flt64 coefficient for solver-boundary callers. / 面向求解器边界的 Flt64 系数。 */
    val coefficientFlt64: Flt64

    /** Explicit conversion helper kept for compatibility. / 保留给兼容路径使用的显式转换工具。 */
    fun coefficientAsV(converter: IntoValue<V>): V = converter.intoValue(coefficientFlt64)
}

typealias LinearCellFlt64 = LinearCell<Flt64>
typealias LinearCellI = LinearCell<Flt64>

interface QuadraticCell<V : RealNumber<V>> : Cell<V> {
    /** V-typed coefficient for public callers. / 面向调用方的 V 类型系数。 */
    val coefficient: V
    val token1: Token<V>
    val token2: Token<V>?

    /** Flt64 coefficient for solver-boundary callers. / 面向求解器边界的 Flt64 系数。 */
    val coefficientFlt64: Flt64

    /** Explicit conversion helper kept for compatibility. / 保留给兼容路径使用的显式转换工具。 */
    fun coefficientAsV(converter: IntoValue<V>): V = converter.intoValue(coefficientFlt64)
}

typealias QuadraticCellFlt64 = QuadraticCell<Flt64>
typealias QuadraticCellI = QuadraticCell<Flt64>

class LinearCellImpl<V>(
    private val tokenTable: AbstractTokenTable<V>,
    private val _coefficientFlt64: Flt64,
    override val token: Token<V>,
    private val converter: IntoValue<V>? = null
) : LinearCell<V> where V : RealNumber<V>, V : NumberField<V> {
    override val coefficientFlt64: Flt64 get() = _coefficientFlt64
    @Suppress("UNCHECKED_CAST")
    override val coefficient: V get() = if (converter != null) converter.intoValue(_coefficientFlt64) else _coefficientFlt64 as V

    override fun evaluate(): V? {
        return token.result?.let { coefficient * it }
    }

    override fun evaluate(solution: List<V>): V? {
        return tokenTable.indexOf(token)?.let {
            coefficient * solution[it]
        }
    }

    override fun evaluate(solution: Map<VariableItemKey, V>): V? {
        return solution[token.key]?.let { coefficient * it }
    }

    override fun evaluateFlt64(): Flt64? {
        return token.resultFlt64?.let { _coefficientFlt64 * it }
    }

    override fun evaluateFlt64(solution: List<Flt64>): Flt64? {
        return tokenTable.indexOf(token)?.let {
            _coefficientFlt64 * solution[it]
        }
    }

    override fun evaluateFlt64(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return solution[token.key]?.let { _coefficientFlt64 * it }
    }

    override fun toString(): String {
        return if (_coefficientFlt64 eq Flt64.one) {
            token.name
        } else {
            "$_coefficientFlt64 * ${token.name}"
        }
    }
}

typealias LinearCellImplFlt64 = LinearCellImpl<Flt64>

class QuadraticCellImpl<V>(
    private val tokenTable: AbstractTokenTable<V>,
    private val _coefficientFlt64: Flt64,
    override val token1: Token<V>,
    override val token2: Token<V>? = null,
    private val converter: IntoValue<V>? = null
) : QuadraticCell<V> where V : RealNumber<V>, V : NumberField<V> {
    override val coefficientFlt64: Flt64 get() = _coefficientFlt64
    @Suppress("UNCHECKED_CAST")
    override val coefficient: V get() = if (converter != null) converter.intoValue(_coefficientFlt64) else _coefficientFlt64 as V

    override fun evaluate(): V? {
        return if (token2 == null) {
            token1.result?.let { coefficient * it }
        } else {
            token1.result?.let { result1 -> token2.result?.let { result2 -> coefficient * result1 * result2 } }
        }
    }

    override fun evaluate(solution: List<V>): V? {
        return if (token2 == null) {
            tokenTable.indexOf(token1)?.let {
                coefficient * solution[it]
            }
        } else {
            tokenTable.indexOf(token1)?.let { index1 ->
                tokenTable.indexOf(token2)?.let { index2 ->
                    coefficient * solution[index1] * solution[index2]
                }
            }
        }
    }

    override fun evaluate(solution: Map<VariableItemKey, V>): V? {
        return if (token2 == null) {
            solution[token1.key]?.let { coefficient * it }
        } else {
            solution[token1.key]?.let { result1 ->
                solution[token2.key]?.let { result2 ->
                    coefficient * result1 * result2
                }
            }
        }
    }

    override fun evaluateFlt64(): Flt64? {
        return if (token2 == null) {
            token1.resultFlt64?.let { _coefficientFlt64 * it }
        } else {
            token1.resultFlt64?.let { result1 -> token2.resultFlt64?.let { result2 -> _coefficientFlt64 * result1 * result2 } }
        }
    }

    override fun evaluateFlt64(solution: List<Flt64>): Flt64? {
        return if (token2 == null) {
            tokenTable.indexOf(token1)?.let {
                _coefficientFlt64 * solution[it]
            }
        } else {
            tokenTable.indexOf(token1)?.let { index1 ->
                tokenTable.indexOf(token2)?.let { index2 ->
                    _coefficientFlt64 * solution[index1] * solution[index2]
                }
            }
        }
    }

    override fun evaluateFlt64(solution: Map<VariableItemKey, Flt64>): Flt64? {
        return if (token2 == null) {
            solution[token1.key]?.let { _coefficientFlt64 * it }
        } else {
            solution[token1.key]?.let { result1 -> solution[token2.key]?.let { result2 -> _coefficientFlt64 * result1 * result2 } }
        }
    }

    override fun toString(): String {
        return if (token2 == null) {
            if (_coefficientFlt64 eq Flt64.one) {
                token1.name
            } else {
                "$_coefficientFlt64 * ${token1.name}"
            }
        } else {
            if (_coefficientFlt64 eq Flt64.one) {
                "${token1.name} * ${token2.name}"
            } else {
                "$_coefficientFlt64 * ${token1.name} * ${token2.name}"
            }
        }
    }
}

typealias QuadraticCellImplFlt64 = QuadraticCellImpl<Flt64>
