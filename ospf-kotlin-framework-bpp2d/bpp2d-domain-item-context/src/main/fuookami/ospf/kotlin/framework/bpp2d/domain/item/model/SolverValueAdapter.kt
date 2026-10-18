/**
 * 二维装箱求解器值适配器。 / 2D packing solver value adapter.
 *
 * 对应 BPP3D 的 `Bpp3dSolverValueAdapter`：把业务量值转换为求解器数值。二维装箱的所有求解器数值转换
 * 都必须经过此处，领域层不直接接触 `Flt64`。
 * / Mirrors BPP3D `Bpp3dSolverValueAdapter`: business quantities are converted to solver numbers here, so
 * the domain layer never touches `Flt64` directly.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 二维装箱求解器值适配器。 / 2D packing solver value adapter.
 *
 * @param V 数值类型 / numeric type
*/
interface Bpp2dSolverValueAdapter<V : FloatingNumber<V>> {

    /**
     * 件数转换为求解器数值。 / Convert an amount to a solver number.
     *
     * @param value 件数 / amount
     * @return 求解器数值 / solver number
    */
    fun amountToSolver(value: UInt64): V

    /**
     * 长度转换为求解器数值。 / Convert a length to a solver number.
     *
     * @param value 长度量 / length quantity
     * @return 求解器数值 / solver number
    */
    fun lengthToSolver(value: Quantity<V>): V

    /**
     * 面积转换为求解器数值。 / Convert an area to a solver number.
     *
     * @param value 面积量 / area quantity
     * @return 求解器数值 / solver number
    */
    fun areaToSolver(value: Quantity<V>): V

    /**
     * 重量转换为求解器数值，默认委托长度转换。 / Convert a weight to a solver number, delegating to length by default.
     *
     * @param value 重量量 / weight quantity
     * @return 求解器数值 / solver number
    */
    fun weightToSolver(value: Quantity<V>): V = lengthToSolver(value)

    /**
     * 需求值转换为求解器数值。 / Convert a demand value to a solver number.
     *
     * @param value 需求值 / demand value
     * @return 求解器数值 / solver number
    */
    fun toSolver(value: Bpp2dDemandValue<V>): V {
        return when (value) {
            is Bpp2dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp2dDemandValue.Area -> areaToSolver(value.value)
        }
    }

    /**
     * 件数值域转换为求解器值域。 / Convert an amount value range to a solver value range.
     *
     * @param value 件数值域 / amount value range
     * @return 求解器值域，无法构造时返回 null / solver value range, or null when it cannot be built
    */
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<V>? {
        val lower = amountToSolver(value.lowerBound.value.unwrap())
        val upper = amountToSolver(value.upperBound.value.unwrap())
        return ValueRange(
            lower,
            upper,
            value.lowerBound.interval,
            value.upperBound.interval,
            lower.constants
        ).value
    }
}

/**
 * 按给定系数缩放的求解器值适配器。 / Solver value adapter scaling by the given factors.
 *
 * 二维排样的面积数值通常远大于件数，直接进模型会造成系数尺度失衡；此适配器允许把长度与面积按比例
 * 缩放，同时保持件数为整数。 / Areas in 2D packing are usually far larger than amounts; scaling lengths and
 * areas keeps model coefficients balanced while amounts stay integral.
 *
 * @param V 数值类型 / numeric type
 * @property constants 数值常量提供者 / numeric constants provider
 * @property lengthScale 长度缩放系数 / length scaling factor
 * @property areaScale 面积缩放系数 / area scaling factor
*/
class ScaledBpp2dSolverValueAdapter<V : FloatingNumber<V>>(
    private val constants: fuookami.ospf.kotlin.math.algebra.concept.FloatingNumberConstants<V>,
    private val lengthScale: V,
    private val areaScale: V
) : Bpp2dSolverValueAdapter<V> {

    override fun amountToSolver(value: UInt64): V {
        var acc = constants.zero
        var remaining = value
        while (remaining > UInt64.zero) {
            acc += constants.one
            remaining -= UInt64.one
        }
        return acc
    }

    override fun lengthToSolver(value: Quantity<V>): V = value.value * lengthScale

    override fun areaToSolver(value: Quantity<V>): V = value.value * areaScale
}

/**
 * FltX 求解器值适配器：把业务量值直接取出为 `FltX`。 / FltX solver value adapter extracting business
 * quantities as `FltX`.
*/
private data object FltXBpp2dSolverValueAdapter : Bpp2dSolverValueAdapter<FltX> {
    override fun amountToSolver(value: UInt64): FltX {
        return FltX(value.toULong().toDouble())
    }

    override fun lengthToSolver(value: Quantity<FltX>): FltX = value.value

    override fun areaToSolver(value: Quantity<FltX>): FltX = value.value
}

/**
 * 默认的 `FltX` 求解器值适配器。 / Default `FltX` solver value adapter.
*/
val DefaultBpp2dSolverValueAdapter: Bpp2dSolverValueAdapter<FltX> = FltXBpp2dSolverValueAdapter

/**
 * 把无符号整数转换为 `FltX`。 / Convert an unsigned integer to `FltX`.
 *
 * @param value 无符号整数 / unsigned integer
 * @return FltX 数值 / FltX value
*/
fun bpp2dAmountToFltX(value: UInt64): FltX = FltX(value.toULong().toDouble())