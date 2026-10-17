/**
 * 二维装箱求解器值适配器（层分配侧）。 / 2D packing solver value adapter for layer assignment.
 *
 * 层分配是唯一接触 `FltX` 求解模型的领域上下文，因此求解器数值转换与 `FltX` 收口都集中在此文件。
 * / Layer assignment is the only domain context touching the `FltX` solver model, so solver value conversion
 * and `FltX` extraction are centralised in this file.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.div
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.times

/**
 * 把无符号整数转换为 `FltX`。 / Convert an unsigned integer to `FltX`.
 *
 * @param value 无符号整数 / unsigned integer
 * @return FltX 数值 / FltX value
*/
fun bpp2dSolverAmount(value: UInt64): FltX = FltX(value.toULong().toDouble())

/**
 * 把 `FltX` 量值取出为求解器数值。 / Extract an `FltX` quantity as a solver number.
 *
 * @param value FltX 量值 / FltX quantity
 * @return 求解器数值 / solver number
*/
fun bpp2dSolverQuantity(value: Quantity<FltX>): FltX = value.value

/**
 * 计算两个 `FltX` 量值的比值，分母为零时返回零。 / Compute the ratio of two `FltX` quantities, zero when the
 * denominator is zero.
 *
 * @param numerator 分子量值 / numerator quantity
 * @param denominator 分母量值 / denominator quantity
 * @return 比值 / ratio
*/
fun bpp2dSolverRatio(numerator: Quantity<FltX>, denominator: Quantity<FltX>): FltX {
    if (denominator.value <= FltX.zero) {
        return FltX.zero
    }
    return numerator.value / denominator.value
}

/**
 * 把 `FltX` 量值按标量缩放。 / Scale an `FltX` quantity by a scalar.
 *
 * @param value FltX 量值 / FltX quantity
 * @param scaler 缩放标量 / scaling scalar
 * @return 缩放后的量值 / scaled quantity
*/
fun bpp2dSolverScale(value: Quantity<FltX>, scaler: FltX): Quantity<FltX> {
    return value.times(scaler)
}
