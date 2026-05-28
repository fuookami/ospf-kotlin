/**
 * 中间模型单元格
 * Intermediate model cells
 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.VariableItemKey

/**
 * 通用单元格接口，支持多种求值方式。
 * Generic cell interface supporting multiple evaluation modes.
 */
interface Cell<V : RealNumber<V>> {
    fun evaluate(): V?
    fun evaluate(solution: List<V>): V?
    fun evaluate(solution: Map<VariableItemKey, V>): V?
}

/**
 * 线性单元格接口，持有系数和标记。
 * Linear cell interface holding coefficient and token.
 */
interface LinearCell<V : RealNumber<V>> : Cell<V> {
    val coefficient: V
    val token: Token<V>
}

/**
 * 二次单元格接口，持有系数和两个标记。
 * Quadratic cell interface holding coefficient and two tokens.
 */
interface QuadraticCell<V : RealNumber<V>> : Cell<V> {
    val coefficient: V
    val token1: Token<V>
    val token2: Token<V>?
}

/**
 * 线性单元格实现，使用 Flt64 系数和 IntoValue 转换器。
 * Linear cell implementation using Flt64 coefficient and IntoValue converter.
 */
class LinearCellImpl<V>(
    private val tokenTable: AbstractTokenTable<V>,
    private val _coefficientFlt64: Flt64,
    override val token: Token<V>,
    private val converter: IntoValue<V>
) : LinearCell<V> where V : RealNumber<V>, V : NumberField<V> {
    override val coefficient: V get() = converter.intoValue(_coefficientFlt64)

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

    override fun toString(): String {
        return if (_coefficientFlt64 eq Flt64.one) {
            token.name
        } else {
            "$_coefficientFlt64 * ${token.name}"
        }
    }
}

/**
 * 二次单元格实现，使用 Flt64 系数和 IntoValue 转换器。
 * Quadratic cell implementation using Flt64 coefficient and IntoValue converter.
 */
class QuadraticCellImpl<V>(
    private val tokenTable: AbstractTokenTable<V>,
    private val _coefficientFlt64: Flt64,
    override val token1: Token<V>,
    override val token2: Token<V>? = null,
    private val converter: IntoValue<V>
) : QuadraticCell<V> where V : RealNumber<V>, V : NumberField<V> {
    override val coefficient: V get() = converter.intoValue(_coefficientFlt64)

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
