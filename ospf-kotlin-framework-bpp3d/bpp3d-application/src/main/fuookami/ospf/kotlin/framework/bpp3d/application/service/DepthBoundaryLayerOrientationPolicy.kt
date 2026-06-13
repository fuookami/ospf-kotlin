package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.resolvedPackingShape

/**
 * 深度边界层轴向/朝向硬约束。
 * Hard axis/orientation constraints for depth boundary layers.
 *
 * @property firstLayerAllowedCylinderAxes depth 方向第一个 layer 允许的圆柱轴向 / allowed cylinder axes on the first depth layer
 * @property lastLayerAllowedCylinderAxes depth 方向最后一个 layer 允许的圆柱轴向 / allowed cylinder axes on the last depth layer
 * @property firstLayerAllowedCuboidOrientations depth 方向第一个 layer 允许的长方体朝向 / allowed cuboid orientations on the first depth layer
 * @property lastLayerAllowedCuboidOrientations depth 方向最后一个 layer 允许的长方体朝向 / allowed cuboid orientations on the last depth layer
 */
data class DepthBoundaryLayerOrientationPolicy(
    val firstLayerAllowedCylinderAxes: Set<Axis3>? = null,
    val lastLayerAllowedCylinderAxes: Set<Axis3>? = null,
    val firstLayerAllowedCuboidOrientations: Set<Orientation>? = null,
    val lastLayerAllowedCuboidOrientations: Set<Orientation>? = null
) {
    val enabled: Boolean
        get() = firstLayerAllowedCylinderAxes != null
                || lastLayerAllowedCylinderAxes != null
                || firstLayerAllowedCuboidOrientations != null
                || lastLayerAllowedCuboidOrientations != null

    init {
        requireNonEmpty(
            values = firstLayerAllowedCylinderAxes,
            fieldName = "firstLayerAllowedCylinderAxes"
        )
        requireNonEmpty(
            values = lastLayerAllowedCylinderAxes,
            fieldName = "lastLayerAllowedCylinderAxes"
        )
        requireNonEmpty(
            values = firstLayerAllowedCuboidOrientations,
            fieldName = "firstLayerAllowedCuboidOrientations"
        )
        requireNonEmpty(
            values = lastLayerAllowedCuboidOrientations,
            fieldName = "lastLayerAllowedCuboidOrientations"
        )
    }

    internal fun ensureSatisfied(bins: List<Bin<BinLayer, FltX>>) {
        if (!enabled) {
            return
        }
        for ((binIndex, bin) in bins.withIndex()) {
            val orderedLayers = bin.units.sortedBy { it.z.value.toDouble() }
            if (orderedLayers.isEmpty()) {
                continue
            }
            ensureBoundaryLayerSatisfied(
                binIndex = binIndex,
                side = DepthBoundaryLayerSide.First,
                placement = orderedLayers.first()
            )
            ensureBoundaryLayerSatisfied(
                binIndex = binIndex,
                side = DepthBoundaryLayerSide.Last,
                placement = orderedLayers.last()
            )
        }
    }

    private fun ensureBoundaryLayerSatisfied(
        binIndex: Int,
        side: DepthBoundaryLayerSide,
        placement: QuantityPlacement3<BinLayer, FltX>
    ) {
        val allowedCylinderAxes = when (side) {
            DepthBoundaryLayerSide.First -> firstLayerAllowedCylinderAxes
            DepthBoundaryLayerSide.Last -> lastLayerAllowedCylinderAxes
        }
        val allowedCuboidOrientations = when (side) {
            DepthBoundaryLayerSide.First -> firstLayerAllowedCuboidOrientations
            DepthBoundaryLayerSide.Last -> lastLayerAllowedCuboidOrientations
        }
        if (allowedCylinderAxes == null && allowedCuboidOrientations == null) {
            return
        }
        for (unitPlacement in placement.unit.units) {
            ensureBoundaryUnitSatisfied(
                binIndex = binIndex,
                side = side,
                layerPlacement = placement,
                unitPlacement = unitPlacement,
                allowedCylinderAxes = allowedCylinderAxes,
                allowedCuboidOrientations = allowedCuboidOrientations
            )
        }
    }

    private fun ensureBoundaryUnitSatisfied(
        binIndex: Int,
        side: DepthBoundaryLayerSide,
        layerPlacement: QuantityPlacement3<BinLayer, FltX>,
        unitPlacement: QuantityPlacement3<*, FltX>,
        allowedCylinderAxes: Set<Axis3>?,
        allowedCuboidOrientations: Set<Orientation>?
    ) {
        val shape = unitPlacement.resolvedPackingShape()
        val axis = shape.axis
        if (axis != null) {
            if (allowedCylinderAxes != null && !allowedCylinderAxes.contains(axis)) {
                throw IllegalArgumentException(
                    "Depth boundary layer orientation policy violation: " +
                            "bin=$binIndex, boundary=${side.label}, layer_z=${layerPlacement.z}, " +
                            "item=${unitPlacement.unit.describeBoundaryUnit()}, cylinder_axis=$axis, " +
                            "allowed_cylinder_axes=${allowedCylinderAxes.joinToString()}."
                )
            }
        } else if (allowedCuboidOrientations != null && !allowedCuboidOrientations.contains(unitPlacement.orientation)) {
            throw IllegalArgumentException(
                "Depth boundary layer orientation policy violation: " +
                        "bin=$binIndex, boundary=${side.label}, layer_z=${layerPlacement.z}, " +
                        "item=${unitPlacement.unit.describeBoundaryUnit()}, cuboid_orientation=${unitPlacement.orientation}, " +
                        "allowed_cuboid_orientations=${allowedCuboidOrientations.joinToString()}."
            )
        }
    }

    private fun Any.describeBoundaryUnit(): String {
        return (this as? ActualItem)?.id ?: toString()
    }

    private companion object {
        private fun requireNonEmpty(values: Set<*>?, fieldName: String) {
            require(values == null || values.isNotEmpty()) {
                "Depth boundary layer orientation policy $fieldName must be null or non-empty."
            }
        }
    }
}

private enum class DepthBoundaryLayerSide(val label: String) {
    First("first"),
    Last("last")
}
