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

    /**
     * 使用标记的已知结果求值。
     * Evaluate using the token's known result.
     *
     * @return 求值结果，若标记结果未知则返回 null / The evaluation result, or null if the token result is unknown
    */
    fun evaluate(): V?

    /**
     * 使用解向量按索引求值。
     * Evaluate using a solution vector by index lookup.
     *
     * @param solution 解向量，索引对应标记在标记表中的位置 / The solution vector whose indices correspond to token positions in the token table
     * @return 求值结果，若标记未在标记表中找到则返回 null / The evaluation result, or null if the token is not found in the token table
    */
    fun evaluate(solution: List<V>): V?

    /**
     * 使用解映射按键求值。
     * Evaluate using a solution map by key lookup.
     *
     * @param solution 解映射，键对应变量项的唯一标识 / The solution map whose keys correspond to variable item keys
     * @return 求值结果，若键未在映射中找到则返回 null / The evaluation result, or null if the key is not found in the map
    */
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
 *
 * @property tokenTable 符号表，用于按标记查找索引 / Token table for index lookup by token
 * @property _coefficientFlt64 Flt64 类型的原始系数 / Raw Flt64 coefficient
 * @property token 线性标记 / The linear token
 * @property converter 值转换器，将 Flt64 转换为目标类型 V / Value converter from Flt64 to target type V
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
 *
 * @property tokenTable 符号表，用于按标记查找索引 / Token table for index lookup by token
 * @property _coefficientFlt64 Flt64 类型的原始系数 / Raw Flt64 coefficient
 * @property token1 第一个二次标记 / First quadratic token
 * @property token2 第二个二次标记（null 表示线性项）/ Second quadratic token (null for linear term)
 * @property converter 值转换器，将 Flt64 转换为目标类型 V / Value converter from Flt64 to target type V
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
