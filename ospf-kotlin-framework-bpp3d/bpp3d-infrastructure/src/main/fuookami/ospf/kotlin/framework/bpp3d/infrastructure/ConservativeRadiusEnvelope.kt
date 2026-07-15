/**
 * 保守半径包络。
 * Conservative radius envelope.
*/
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3

/**
 * 连续半径圆柱的保守包络尺寸。
 * Conservative envelope dimensions for continuous-radius cylinders.
 *
 * @property rMin 半径下界 / radius lower bound
 * @property rMax 半径上界 / radius upper bound
*/
data class ConservativeRadiusEnvelope(
    val rMin: FltX,
    val rMax: FltX
) {
    init {
        require(rMin > FltX.zero) {
            "半径下界必须为正数。 / Radius lower bound must be positive."
        }
        require(rMax >= rMin) {
            "半径上界必须大于等于下界。 / Radius upper bound must be greater than or equal to lower bound."
        }
    }

    /**
     * The envelope radius, equal to the upper bound radius.
     * 包络半径，等于半径上界。
    */
    val envelopeRadius: FltX get() = rMax

    /**
     * The envelope diameter, twice the upper bound radius.
     * 包络直径，等于半径上界的两倍。
    */
    val envelopeDiameter: FltX get() = rMax * FltX(2.0)

    /**
     * Computes the footprint width for the given axis and height.
     * 计算给定轴向和高度下的占位宽度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @return The footprint width.
     *         中文：占位宽度。
    */
    fun footprintWidth(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> height
            Axis3.Y -> envelopeDiameter
            Axis3.Z -> envelopeDiameter
        }
    }

    /**
     * Computes the footprint depth for the given axis and height.
     * 计算给定轴向和高度下的占位深度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @return The footprint depth.
     *         中文：占位深度。
    */
    fun footprintDepth(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> envelopeDiameter
            Axis3.Y -> envelopeDiameter
            Axis3.Z -> height
        }
    }

    /**
     * Computes the bounding width for the given axis and height.
     * 计算给定轴向和高度下的包围宽度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @return The bounding width, same as footprint width.
     *         中文：包围宽度，与占位宽度相同。
    */
    fun boundingWidth(axis: Axis3, height: FltX): FltX {
        return footprintWidth(axis, height)
    }

    /**
     * Computes the bounding height for the given axis and height.
     * 计算给定轴向和高度下的包围高度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @return The bounding height.
     *         中文：包围高度。
    */
    fun boundingHeight(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> envelopeDiameter
            Axis3.Y -> height
            Axis3.Z -> envelopeDiameter
        }
    }

    /**
     * Computes the bounding depth for the given axis and height.
     * 计算给定轴向和高度下的包围深度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @return The bounding depth, same as footprint depth.
     *         中文：包围深度，与占位深度相同。
    */
    fun boundingDepth(axis: Axis3, height: FltX): FltX {
        return footprintDepth(axis, height)
    }

    /**
     * Returns the support coverage radius.
     * 返回支撑覆盖半径。
     *
     * @return The support coverage radius, equal to the envelope radius.
     *         中文：支撑覆盖半径，等于包络半径。
    */
    fun supportCoverageRadius(): FltX {
        return envelopeRadius
    }

    /**
     * Returns the collision margin diameter.
     * 返回碰撞余量直径。
     *
     * @return The collision margin, equal to the envelope diameter.
     *         中文：碰撞余量，等于包络直径。
    */
    fun collisionMargin(): FltX {
        return envelopeDiameter
    }

    /**
     * Computes the real footprint width for the given axis, height, and actual radius.
     * 计算给定轴向、高度和实际半径下的真实占位宽度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @param radius The actual radius.
     *               中文：实际半径。
     * @return The real footprint width.
     *         中文：真实占位宽度。
    */
    fun realFootprintWidth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> height
            Axis3.Y -> radius * FltX(2.0)
            Axis3.Z -> radius * FltX(2.0)
        }
    }

    /**
     * Computes the real footprint depth for the given axis, height, and actual radius.
     * 计算给定轴向、高度和实际半径下的真实占位深度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @param radius The actual radius.
     *               中文：实际半径。
     * @return The real footprint depth.
     *         中文：真实占位深度。
    */
    fun realFootprintDepth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> radius * FltX(2.0)
            Axis3.Y -> radius * FltX(2.0)
            Axis3.Z -> height
        }
    }

    /**
     * Computes the real bounding width for the given axis, height, and actual radius.
     * 计算给定轴向、高度和实际半径下的真实包围宽度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @param radius The actual radius.
     *               中文：实际半径。
     * @return The real bounding width, same as real footprint width.
     *         中文：真实包围宽度，与真实占位宽度相同。
    */
    fun realBoundingWidth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return realFootprintWidth(axis, height, radius)
    }

    /**
     * Computes the real bounding height for the given axis, height, and actual radius.
     * 计算给定轴向、高度和实际半径下的真实包围高度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @param radius The actual radius.
     *               中文：实际半径。
     * @return The real bounding height.
     *         中文：真实包围高度。
    */
    fun realBoundingHeight(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> radius * FltX(2.0)
            Axis3.Y -> height
            Axis3.Z -> radius * FltX(2.0)
        }
    }

    /**
     * Computes the real bounding depth for the given axis, height, and actual radius.
     * 计算给定轴向、高度和实际半径下的真实包围深度。
     *
     * @param axis The orientation axis.
     *             中文：方向轴。
     * @param height The cylinder height.
     *               中文：圆柱高度。
     * @param radius The actual radius.
     *               中文：实际半径。
     * @return The real bounding depth, same as real footprint depth.
     *         中文：真实包围深度，与真实占位深度相同。
    */
    fun realBoundingDepth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return realFootprintDepth(axis, height, radius)
    }

    /**
     * Checks whether the given radius is within the valid range [rMin, rMax].
     * 检查给定半径是否在有效范围 [rMin, rMax] 内。
     *
     * @param radius The radius to validate.
     *               中文：待验证的半径。
     * @return True if the radius is valid, false otherwise.
     *         中文：半径有效则返回 true，否则返回 false。
    */
    fun isRadiusValid(radius: FltX): Boolean {
        return radius >= rMin && radius <= rMax
    }
}
