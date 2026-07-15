package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.framework.csp1d.domain.material.error.Csp1dTypeError
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Quantity<V> 算术策略 / Arithmetic strategy for Quantity<V>
 *
 * 提供领域数值下的 Quantity 加减运算，避免在领域模型中直接依赖 Flt64/FltX / Provides Quantity addition/subtraction under domain numerics, avoiding direct Flt64/FltX dependency in domain models
 *
 * @param V 数值类型 / Numeric value type
*/
interface QuantityArithmetic<V : RealNumber<V>> {

    /**
     * 物理量加法 / Quantity addition
     *
     * @param a 左操作数 / Left operand
     * @param b 右操作数 / Right operand
     * @return 相加结果 / Addition result
    */
    fun add(a: Quantity<V>, b: Quantity<V>): Ret<Quantity<V>>

    /**
     * 物理量减法 / Quantity subtraction
     *
     * @param a 左操作数 / Left operand
     * @param b 右操作数 / Right operand
     * @return 相减结果 / Subtraction result
    */
    fun subtract(a: Quantity<V>, b: Quantity<V>): Ret<Quantity<V>>

    /**
     * 创建指定单位的零值物理量 / Create zero quantity for the given unit
     *
     * @param unit 物理单位 / Physical unit
     * @return 零值物理量 / Zero quantity
    */
    fun zero(unit: PhysicalUnit): Quantity<V>

    /**
     * 物理量加法（失败返回 null）/ Quantity addition (returns null on failure)
     *
     * @param a 左操作数 / Left operand
     * @param b 右操作数 / Right operand
     * @return 相加结果，失败时返回 null / Addition result, or null on failure
    */
    fun addOrNull(a: Quantity<V>, b: Quantity<V>): Quantity<V>? {
        return add(a, b).value
    }

    /**
     * 物理量减法（失败返回 null）/ Quantity subtraction (returns null on failure)
     *
     * @param a 左操作数 / Left operand
     * @param b 右操作数 / Right operand
     * @return 相减结果，失败时返回 null / Subtraction result, or null on failure
    */
    fun subtractOrNull(a: Quantity<V>, b: Quantity<V>): Quantity<V>? {
        return subtract(a, b).value
    }

    /**
     * 判断物理量是否为正 / Check if quantity is positive
     *
     * @param q 物理量 / Quantity
     * @return 是否为正 / Whether positive
    */
    fun isPositive(q: Quantity<V>): Boolean {
        return when (q.value partialOrd q.value.constants.zero) {
            is Order.Greater -> true
            else -> false
        }
    }

    /**
     * 判断物理量是否非负 / Check if quantity is non-negative
     *
     * @param q 物理量 / Quantity
     * @return 是否非负 / Whether non-negative
    */
    fun isNonNegative(q: Quantity<V>): Boolean {
        return when (q.value partialOrd q.value.constants.zero) {
            is Order.Less -> false
            else -> true
        }
    }

    /**
     * 判断物理量是否为零 / Check if quantity is zero
     *
     * @param q 物理量 / Quantity
     * @return 是否为零 / Whether zero
    */
    fun isZero(q: Quantity<V>): Boolean {
        return q.value partialOrd q.value.constants.zero == Order.Equal
    }
}

private val defaultQuantityArithmetic64 = object : QuantityArithmetic<Flt64> {
    override fun add(a: Quantity<Flt64>, b: Quantity<Flt64>): Ret<Quantity<Flt64>> = a.plusSafe(b)
    override fun subtract(a: Quantity<Flt64>, b: Quantity<Flt64>): Ret<Quantity<Flt64>> = a.minusSafe(b)
    override fun zero(unit: PhysicalUnit): Quantity<Flt64> =
        Quantity(Flt64.zero, unit)
}

private val defaultQuantityArithmeticX = object : QuantityArithmetic<FltX> {
    override fun add(a: Quantity<FltX>, b: Quantity<FltX>): Ret<Quantity<FltX>> = a.plusSafe(b)
    override fun subtract(a: Quantity<FltX>, b: Quantity<FltX>): Ret<Quantity<FltX>> = a.minusSafe(b)
    override fun zero(unit: PhysicalUnit): Quantity<FltX> =
        Quantity(FltX.zero, unit)
}

/**
 * 默认物理量算术解析器 / Default quantity arithmetic resolver
 *
 * 通过领域数值样本解析对应的 QuantityArithmetic 实现，调用方只依赖泛型接口。
 * Resolves a QuantityArithmetic implementation from a domain value sample so callers depend only on the generic interface.
*/
object DefaultQuantityArithmetic {

    /**
     * 根据领域数值样本解析算术策略 / Resolve arithmetic strategy from a domain value sample
     *
     * @param V 数值类型 / Numeric value type
     * @param sample 领域数值样本 / Domain value sample
     * @return 泛型物理量算术策略 / Quantity arithmetic strategy
    */
    @Suppress("UNCHECKED_CAST")
    fun <V : RealNumber<V>> resolveFor(sample: V): Ret<QuantityArithmetic<V>> {
        return when (sample) {
            is Flt64 -> Ok(defaultQuantityArithmetic64 as QuantityArithmetic<V>)
            is FltX -> Ok(defaultQuantityArithmeticX as QuantityArithmetic<V>)
            else -> Failed(Csp1dTypeError("Unsupported RealNumber type: ${sample::class}"))
        }
    }
}
