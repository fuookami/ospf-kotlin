/**
 * BPP3D 求解器值适配器。
 * BPP3D solver value adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * UInt64 转求解器 InfraNumber。
 * Convert UInt64 to solver InfraNumber.
 */
private fun UInt64.toSolverInfraNumber(): InfraNumber = InfraNumber(this.toULong().toDouble())

/**
 * Quantity 提取求解器 InfraNumber。
 * Extract solver InfraNumber from Quantity.
 */
private fun Quantity<InfraNumber>.toSolverInfraNumber(): InfraNumber = this.value

/**
 * UInt64 值域转求解器值域。
 * Convert UInt64 value range to solver value range.
 */
private fun ValueRange<UInt64>.toSolverRange(): ValueRange<InfraNumber> {
    val lower = this.lowerBound.value.unwrap().toSolverInfraNumber()
    val upper = this.upperBound.value.unwrap().toSolverInfraNumber()
    return ValueRange(
        lower,
        upper,
        this.lowerBound.interval,
        this.upperBound.interval,
        InfraNumber
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
    fun amountToSolver(value: UInt64): InfraNumber

    /**
     * 数量值域转求解器值域。
     * Convert amount value range to solver value range.
     *
     * @param value 数量值域 / amount value range
     * @return 求解器值域 / solver value range
     */
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<InfraNumber> = value.toSolverRange()

    /**
     * 长度转求解器值。
     * Convert length to solver value.
     *
     * @param value 长度量 / length quantity
     * @return 求解器数值 / solver numeric value
     */
    fun lengthToSolver(value: Quantity<InfraNumber>): InfraNumber

    /**
     * 面积转求解器值。
     * Convert area to solver value.
     *
     * @param value 面积量 / area quantity
     * @return 求解器数值 / solver numeric value
     */
    fun areaToSolver(value: Quantity<InfraNumber>): InfraNumber

    /**
     * 体积转求解器值。
     * Convert volume to solver value.
     *
     * @param value 体积量 / volume quantity
     * @return 求解器数值 / solver numeric value
     */
    fun volumeToSolver(value: Quantity<InfraNumber>): InfraNumber

    /**
     * 深度转求解器值（默认委托给长度转换）。
     * Convert depth to solver value (defaults to length conversion).
     *
     * @param value 深度量 / depth quantity
     * @return 求解器数值 / solver numeric value
     */
    fun depthToSolver(value: Quantity<InfraNumber>): InfraNumber = lengthToSolver(value)

    /**
     * 重量转求解器值。
     * Convert weight to solver value.
     *
     * @param value 重量量 / weight quantity
     * @return 求解器数值 / solver numeric value
     */
    fun weightToSolver(value: Quantity<InfraNumber>): InfraNumber

    /**
     * 需求值转求解器值。
     * Convert demand value to solver value.
     *
     * @param value 需求值 / demand value
     * @return 求解器数值 / solver numeric value
     */
    fun toSolver(value: Bpp3dDemandValue): InfraNumber {
        return when (value) {
            is Bpp3dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp3dDemandValue.Weight -> weightToSolver(value.value)
        }
    }
}

/**
 * BPP3D 需求值适配器别名。
 * BPP3D demand value adapter alias.
 */
typealias Bpp3dDemandValueAdapter = Bpp3dSolverValueAdapter

/**
 * 默认 BPP3D 需求值适配器，直接使用 InfraNumber 转换。
 * Default BPP3D demand value adapter, uses direct InfraNumber conversion.
 */
data object DefaultBpp3dDemandValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): InfraNumber = value.toSolverInfraNumber()
    override fun lengthToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun areaToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun volumeToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun weightToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
}

/**
 * 默认 BPP3D 求解器值适配器实例。
 * Default BPP3D solver value adapter instance.
 */
val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dDemandValueAdapter

