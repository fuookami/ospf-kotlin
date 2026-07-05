/**
 * BPP3D 求解器值适配器。
 * BPP3D solver value adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue

/**
 * UInt64 转求解器 FltX。
 * Convert UInt64 to solver FltX.
 *
 * @return 求解器数值 / solver numeric value
 */
private fun UInt64.toSolverNumber(): FltX = FltX(this.toULong().toDouble())

/**
 * Quantity 提取求解器 FltX。
 * Extract solver FltX from Quantity.
 *
 * @return 求解器数值 / solver numeric value
 */
private fun Quantity<FltX>.toSolverNumber(): FltX = this.value

/**
 * UInt64 值域转求解器值域。
 * Convert UInt64 value range to solver value range.
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
 * BPP3D 求解器值适配器接口，将业务类型转换为求解器数值类型。
 * BPP3D solver value adapter interface, converts business types to solver numeric types.
 */
interface Bpp3dSolverValueAdapter {
    /**
     * 数量转求解器值。
     * Convert amount to solver value.
     *
     * @param value 数量值 / amount value
     * @return 求解器数值 / solver numeric value
     */
    fun amountToSolver(value: UInt64): FltX

    /**
     * 数量值域转求解器值域。
     * Convert amount value range to solver value range.
     *
     * @param value 数量值域 / amount value range
     * @return 求解器值域 / solver value range
     */
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<FltX> = value.toSolverRange()

    /**
     * 长度转求解器值。
     * Convert length to solver value.
     *
     * @param value 长度量 / length quantity
     * @return 求解器数值 / solver numeric value
     */
    fun lengthToSolver(value: Quantity<FltX>): FltX

    /**
     * 面积转求解器值。
     * Convert area to solver value.
     *
     * @param value 面积量 / area quantity
     * @return 求解器数值 / solver numeric value
     */
    fun areaToSolver(value: Quantity<FltX>): FltX

    /**
     * 体积转求解器值。
     * Convert volume to solver value.
     *
     * @param value 体积量 / volume quantity
     * @return 求解器数值 / solver numeric value
     */
    fun volumeToSolver(value: Quantity<FltX>): FltX

    /**
     * 深度转求解器值（默认委托给长度转换）。
     * Convert depth to solver value (defaults to length conversion).
     *
     * @param value 深度量 / depth quantity
     * @return 求解器数值 / solver numeric value
     */
    fun depthToSolver(value: Quantity<FltX>): FltX = lengthToSolver(value)

    /**
     * 重量转求解器值。
     * Convert weight to solver value.
     *
     * @param value 重量量 / weight quantity
     * @return 求解器数值 / solver numeric value
     */
    fun weightToSolver(value: Quantity<FltX>): FltX

    /**
     * 需求值转求解器值。
     * Convert demand value to solver value.
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
 * 直接使用 FltX 转换的默认实现。
 * Default implementation using direct FltX conversion.
 */
private data object DirectBpp3dSolverValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): FltX = value.toSolverNumber()
    override fun lengthToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun areaToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun volumeToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
    override fun weightToSolver(value: Quantity<FltX>): FltX = value.toSolverNumber()
}

/**
 * 默认 BPP3D 求解器值适配器实例。
 * Default BPP3D solver value adapter instance.
 */
val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DirectBpp3dSolverValueAdapter
