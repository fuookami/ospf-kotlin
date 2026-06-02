package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 物理量算术策略接口，抽象 Quantity<V> 的加减运算 / Quantity arithmetic strategy interface abstracting add/subtract for Quantity<V>
 *
 * 由于 ospf-kotlin-quantities 框架的 Quantity<V> 加减法仅对具体数值类型（Int64/UInt64/IntX/Flt64/FltX）
 * 提供扩展函数，而领域层使用 V : RealNumber<V> 泛型约束，因此通过此接口将具体类型的算术能力注入领域模型，
 * 避免运行时类型分支。
 *
 * @param V 数值类型 / Numeric value type
 */
interface QuantityArithmetic<V : RealNumber<V>> {
    /**
     * 同单位物理量加法 / Same-unit quantity addition
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 和 / Sum
     */
    fun add(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>

    /**
     * 同单位物理量减法 / Same-unit quantity subtraction
     *
     * @param lhs 左操作数 / Left operand
     * @param rhs 右操作数 / Right operand
     * @return 差 / Difference
     */
    fun subtract(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V>
}

/**
 * Flt64 物理量算术实现 / Flt64 quantity arithmetic implementation
 */
class Flt64QuantityArithmetic : QuantityArithmetic<Flt64> {
    override fun add(lhs: Quantity<Flt64>, rhs: Quantity<Flt64>): Quantity<Flt64> {
        check(lhs.unit == rhs.unit) { "Unit mismatch: ${lhs.unit} vs ${rhs.unit}" }
        return Quantity(lhs.value + rhs.value, lhs.unit)
    }

    override fun subtract(lhs: Quantity<Flt64>, rhs: Quantity<Flt64>): Quantity<Flt64> {
        check(lhs.unit == rhs.unit) { "Unit mismatch: ${lhs.unit} vs ${rhs.unit}" }
        return Quantity(lhs.value - rhs.value, lhs.unit)
    }
}

/**
 * FltX 物理量算术实现 / FltX quantity arithmetic implementation
 */
class FltXQuantityArithmetic : QuantityArithmetic<FltX> {
    override fun add(lhs: Quantity<FltX>, rhs: Quantity<FltX>): Quantity<FltX> {
        check(lhs.unit == rhs.unit) { "Unit mismatch: ${lhs.unit} vs ${rhs.unit}" }
        return Quantity(lhs.value + rhs.value, lhs.unit)
    }

    override fun subtract(lhs: Quantity<FltX>, rhs: Quantity<FltX>): Quantity<FltX> {
        check(lhs.unit == rhs.unit) { "Unit mismatch: ${lhs.unit} vs ${rhs.unit}" }
        return Quantity(lhs.value - rhs.value, lhs.unit)
    }
}

/**
 * 默认算术策略解析，按运行时类型分发到 Flt64/FltX 实现 / Default arithmetic resolver dispatching by runtime type
 */
object DefaultQuantityArithmetic {
    /**
     * 按数值类型解析默认算术策略 / Resolve default arithmetic strategy by value type
     *
     * @param V 数值类型 / Numeric value type
     * @return 匹配的算术策略 / Matched arithmetic strategy
     * @throws IllegalArgumentException 当类型不受支持时 / When type is not supported
     */
    @Suppress("UNCHECKED_CAST")
    fun <V : RealNumber<V>> resolve(): QuantityArithmetic<V> {
        return when {
            else -> {
                // Attempt Flt64 first, then FltX, based on type identity
                try {
                    Flt64QuantityArithmetic() as QuantityArithmetic<V>
                } catch (@Suppress("TooGenericExceptionCaught") _: Throwable) {
                    FltXQuantityArithmetic() as QuantityArithmetic<V>
                }
            }
        }
    }

    /**
     * 按样例值解析算术策略 / Resolve arithmetic strategy by sample value
     *
     * @param sample 样例值 / Sample value
     * @return 匹配的算术策略 / Matched arithmetic strategy
     */
    @Suppress("UNCHECKED_CAST")
    fun <V : RealNumber<V>> resolveFor(sample: V): QuantityArithmetic<V> {
        return when (sample) {
            is Flt64 -> Flt64QuantityArithmetic() as QuantityArithmetic<V>
            is FltX -> FltXQuantityArithmetic() as QuantityArithmetic<V>
            else -> throw IllegalArgumentException(
                "Unsupported numeric type: ${sample::class.simpleName}. Provide a custom QuantityArithmetic<V>."
            )
        }
    }
}
