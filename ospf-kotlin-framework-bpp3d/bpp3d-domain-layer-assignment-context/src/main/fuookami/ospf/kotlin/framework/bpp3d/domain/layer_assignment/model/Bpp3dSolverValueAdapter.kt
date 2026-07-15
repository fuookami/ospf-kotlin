/**
 * BPP3D solver value adapter.
 * BPP3D 求解器值适配器。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue

/**
 * Convert UInt64 to solver FltX.
 * UInt64 转求解器 FltX。
 *
 * @return 求解器数值 / solver numeric value
*/
private fun UInt64.toSolverNumber(): FltX = FltX(this.toULong().toDouble())

/**
 * Extract solver FltX from Quantity.
 * Quantity 提取求解器 FltX。
 *
 * @return 求解器数值 / solver numeric value
*/
private fun Quantity<FltX>.toSolverNumber(): FltX = this.value

/**
 * Convert UInt64 value range to solver value range.
 * UInt64 值域转求解器值域。
 *
 * @return 求解器值域 / solver value range
*/
private fun ValueRange<UInt64>.toSolverRange(): ValueRange<FltX> {
    val lower = this.lowerBound.value.unwrap().toSolverNumber()
    val upper = this.upperBound.value.unwrap().toSolverNumber()
    return ValueRange(
        lower,
        upper,
        this.lowerBound.interval,
        this.upperBound.interval,
        FltX
    ).value!!
}

/**
 * BPP3D solver value adapter interface, converts business types to solver numeric types.
 * BPP3D 求解器值适配器接口，将业务类型转换为求解器数值类型。
*/
interface Bpp3dSolverValueAdapter {

    /**
     * Convert amount to solver value.
     * 数量转求解器值。
     *
     * @param value 数量值 / amount value
     * @return 求解器数值 / solver numeric value
    */
    fun amountToSolver(value: UInt64): FltX

    /**
     * Convert amount value range to solver value range.
     * 数量值域转求解器值域。
     *
     * @param value 数量值域 / amount value range
     * @return 求解器值域 / solver value range
    */
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<FltX> = value.toSolverRange()

    /**
     * Convert length to solver value.
     * 长度转求解器值。
     *
     * @param value 长度量 / length quantity
     * @return 求解器数值 / solver numeric value
    */
    fun lengthToSolver(value: Quantity<FltX>): FltX

    /**
     * Convert area to solver value.
     * 面积转求解器值。
     *
     * @param value 面积量 / area quantity
     * @return 求解器数值 / solver numeric value
    */
    fun areaToSolver(value: Quantity<FltX>): FltX

    /**
     * Convert volume to solver value.
     * 体积转求解器值。
     *
     * @param value 体积量 / volume quantity
     * @return 求解器数值 / solver numeric value
    */
    fun volumeToSolver(value: Quantity<FltX>): FltX

    /**
     * Convert depth to solver value (defaults to length conversion).
     * 深度转求解器值（默认委托给长度转换）。
     *
     * @param value 深度量 / depth quantity
     * @return 求解器数值 / solver numeric value
    */
    fun depthToSolver(value: Quantity<FltX>): FltX = lengthToSolver(value)

    /**
     * Convert weight to solver value.
     * 重量转求解器值。
     *
     * @param value 重量量 / weight quantity
     * @return 求解器数值 / solver numeric value
    */
    fun weightToSolver(value: Quantity<FltX>): FltX

    /**
     * Convert demand value to solver value.
     * 需求值转求解器值。
     *
     * @param value 需求值 / demand value
     * @return 求解器数值 / solver numeric value
    */
    fun toSolver(value: Bpp3dDemandValue): FltX {
        return when (value) {
            is Bpp3dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp3dDemandValue.Weight -> weightToSolver(value.value)
        }
    }
}

/**
 * Default implementation using direct FltX conversion.
 * 直接使用 FltX 转换的默认实现。
*/
private data object DirectBpp3dSolverValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): FltX = value.toSolverNumber()
    override fun lengthToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun areaToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun volumeToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun weightToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
}

/**
 * Default BPP3D solver value adapter instance.
 * 默认 BPP3D 求解器值适配器实例。
*/
val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DirectBpp3dSolverValueAdapter
