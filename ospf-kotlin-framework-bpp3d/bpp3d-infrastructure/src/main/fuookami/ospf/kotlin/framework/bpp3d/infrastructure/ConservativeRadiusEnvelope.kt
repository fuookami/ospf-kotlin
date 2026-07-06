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

    val envelopeRadius: FltX get() = rMax
    val envelopeDiameter: FltX get() = rMax * FltX(2.0)

    fun footprintWidth(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> height
            Axis3.Y -> envelopeDiameter
            Axis3.Z -> envelopeDiameter
        }
    }

    fun footprintDepth(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> envelopeDiameter
            Axis3.Y -> envelopeDiameter
            Axis3.Z -> height
        }
    }

    fun boundingWidth(axis: Axis3, height: FltX): FltX {
        return footprintWidth(axis, height)
    }

    fun boundingHeight(axis: Axis3, height: FltX): FltX {
        return when (axis) {
            Axis3.X -> envelopeDiameter
            Axis3.Y -> height
            Axis3.Z -> envelopeDiameter
        }
    }

    fun boundingDepth(axis: Axis3, height: FltX): FltX {
        return footprintDepth(axis, height)
    }

    fun supportCoverageRadius(): FltX {
        return envelopeRadius
    }

    fun collisionMargin(): FltX {
        return envelopeDiameter
    }

    fun realFootprintWidth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> height
            Axis3.Y -> radius * FltX(2.0)
            Axis3.Z -> radius * FltX(2.0)
        }
    }

    fun realFootprintDepth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> radius * FltX(2.0)
            Axis3.Y -> radius * FltX(2.0)
            Axis3.Z -> height
        }
    }

    fun realBoundingWidth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return realFootprintWidth(axis, height, radius)
    }

    fun realBoundingHeight(axis: Axis3, height: FltX, radius: FltX): FltX {
        return when (axis) {
            Axis3.X -> radius * FltX(2.0)
            Axis3.Y -> height
            Axis3.Z -> radius * FltX(2.0)
        }
    }

    fun realBoundingDepth(axis: Axis3, height: FltX, radius: FltX): FltX {
        return realFootprintDepth(axis, height, radius)
    }

    fun isRadiusValid(radius: FltX): Boolean {
        return radius >= rMin && radius <= rMax
    }
}
