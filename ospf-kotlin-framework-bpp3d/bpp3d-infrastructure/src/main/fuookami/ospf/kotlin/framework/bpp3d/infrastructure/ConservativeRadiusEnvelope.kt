/**
 * 保守半径 envelope：使用 r_max 保守建模所有几何计算。
 * Conservative radius envelope: use r_max for all geometry calculations.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.geometry.Axis3

/**
 * 保守半径 envelope。
 * Conservative radius envelope.
 *
 * 使用 r_max 保守建模 placement footprint、bounding dimensions、支撑覆盖和碰撞安全边界。
 * Use r_max conservatively for placement footprint, bounding dimensions, support coverage and collision safety margins.
 *
 * @property rMin 半径下界 / radius lower bound
 * @property rMax 半径上界（envelope 半径）/ radius upper bound (envelope radius)
 */
data class ConservativeRadiusEnvelope(
    val rMin: InfraNumber,
    val rMax: InfraNumber
) {
    init {
        require(rMin.toDouble() > 0.0) { "rMin must be positive" }
        require(rMax.toDouble() >= rMin.toDouble()) { "rMax must be >= rMin" }
    }

    /** envelope 半径（保守使用 rMax）/ envelope radius (conservatively rMax) */
    val envelopeRadius: InfraNumber get() = rMax

    /** envelope 直径（保守使用 2*rMax）/ envelope diameter (conservatively 2*rMax) */
    val envelopeDiameter: InfraNumber get() = rMax * InfraNumber(2.0)

    /**
     * 保守 footprint 宽度。
     * Conservative footprint width.
     */
    fun footprintWidth(axis: Axis3, cylinderHeight: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.X -> cylinderHeight
            Axis3.Y, Axis3.Z -> envelopeDiameter
        }
    }

    /**
     * 保守 footprint 深度。
     * Conservative footprint depth.
     */
    fun footprintDepth(axis: Axis3, cylinderHeight: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.Z -> cylinderHeight
            Axis3.X, Axis3.Y -> envelopeDiameter
        }
    }

    /**
     * 保守 bounding 宽度。
     * Conservative bounding width.
     */
    fun boundingWidth(axis: Axis3, cylinderHeight: InfraNumber): InfraNumber {
        return footprintWidth(axis, cylinderHeight)
    }

    /**
     * 保守 bounding 高度。
     * Conservative bounding height.
     */
    fun boundingHeight(axis: Axis3, cylinderHeight: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.X, Axis3.Z -> envelopeDiameter
            Axis3.Y -> cylinderHeight
        }
    }

    /**
     * 保守 bounding 深度。
     * Conservative bounding depth.
     */
    fun boundingDepth(axis: Axis3, cylinderHeight: InfraNumber): InfraNumber {
        return footprintDepth(axis, cylinderHeight)
    }

    /**
     * 保守支撑覆盖半径。
     * Conservative support coverage radius.
     */
    fun supportCoverageRadius(): InfraNumber = rMax

    /**
     * 保守碰撞边界。
     * Conservative collision margin.
     */
    fun collisionMargin(): InfraNumber = envelopeDiameter

    /**
     * 使用真实半径计算真实 footprint 宽度。
     * Compute real footprint width using actual radius.
     */
    fun realFootprintWidth(axis: Axis3, cylinderHeight: InfraNumber, actualRadius: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.X -> cylinderHeight
            Axis3.Y, Axis3.Z -> actualRadius * InfraNumber(2.0)
        }
    }

    /**
     * 使用真实半径计算真实 footprint 深度。
     * Compute real footprint depth using actual radius.
     */
    fun realFootprintDepth(axis: Axis3, cylinderHeight: InfraNumber, actualRadius: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.Z -> cylinderHeight
            Axis3.X, Axis3.Y -> actualRadius * InfraNumber(2.0)
        }
    }

    /**
     * 使用真实半径计算真实 bounding 宽度。
     * Compute real bounding width using actual radius.
     */
    fun realBoundingWidth(axis: Axis3, cylinderHeight: InfraNumber, actualRadius: InfraNumber): InfraNumber {
        return realFootprintWidth(axis, cylinderHeight, actualRadius)
    }

    /**
     * 使用真实半径计算真实 bounding 高度。
     * Compute real bounding height using actual radius.
     */
    fun realBoundingHeight(axis: Axis3, cylinderHeight: InfraNumber, actualRadius: InfraNumber): InfraNumber {
        return when (axis) {
            Axis3.X, Axis3.Z -> actualRadius * InfraNumber(2.0)
            Axis3.Y -> cylinderHeight
        }
    }

    /**
     * 使用真实半径计算真实 bounding 深度。
     * Compute real bounding depth using actual radius.
     */
    fun realBoundingDepth(axis: Axis3, cylinderHeight: InfraNumber, actualRadius: InfraNumber): InfraNumber {
        return realFootprintDepth(axis, cylinderHeight, actualRadius)
    }

    /**
     * 验证 solver-selected radius 是否在 [rMin, rMax] 范围内。
     * Validate that solver-selected radius is within [rMin, rMax].
     */
    fun isRadiusValid(solverRadius: InfraNumber): Boolean {
        return solverRadius.toDouble() >= rMin.toDouble() && solverRadius.toDouble() <= rMax.toDouble()
    }
}
