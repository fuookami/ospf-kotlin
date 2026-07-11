/**
 * 深度边界层方向策略。
 * Depth boundary layer orientation policy.
*/
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

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

    /** 是否启用深度边界层方向约束 / Whether depth boundary layer orientation constraints are enabled */
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

    /**
     * 确保层方向约束满足。
     * Ensure layer orientation constraints are satisfied.
     *
     * @param bins 待校验的容器列表 / list of bins to validate
     * @return 校验结果 / validation result
    */
    internal fun ensureSatisfied(bins: List<Bin<BinLayer, FltX>>): Try {
        if (!enabled) {
            return ok
        }
        for ((binIndex, bin) in bins.withIndex()) {
            val orderedLayers = bin.units.sortedBy { it.z.value.toDouble() }
            if (orderedLayers.isEmpty()) {
                continue
            }
            when (val first = ensureBoundaryLayerSatisfied(
                binIndex = binIndex,
                side = DepthBoundaryLayerSide.First,
                placement = orderedLayers.first()
            )) {
                is Ok -> {}
                is Failed -> return Failed(first.error)
                is Fatal -> return Fatal(first.errors)
            }
            when (val last = ensureBoundaryLayerSatisfied(
                binIndex = binIndex,
                side = DepthBoundaryLayerSide.Last,
                placement = orderedLayers.last()
            )) {
                is Ok -> {}
                is Failed -> return Failed(last.error)
                is Fatal -> return Fatal(last.errors)
            }
        }
        return ok
    }

    /**
     * 校验指定边界层是否满足方向约束。
     * Validate whether the specified boundary layer satisfies orientation constraints.
     *
     * @param binIndex 容器索引 / bin index
     * @param side 边界层侧别 / boundary layer side
     * @param placement 层放置信息 / layer placement info
     * @return 校验结果 / validation result
    */
    private fun ensureBoundaryLayerSatisfied(
        binIndex: Int,
        side: DepthBoundaryLayerSide,
        placement: QuantityPlacement3<BinLayer, FltX>
    ): Try {
        val allowedCylinderAxes = when (side) {
            DepthBoundaryLayerSide.First -> firstLayerAllowedCylinderAxes
            DepthBoundaryLayerSide.Last -> lastLayerAllowedCylinderAxes
        }
        val allowedCuboidOrientations = when (side) {
            DepthBoundaryLayerSide.First -> firstLayerAllowedCuboidOrientations
            DepthBoundaryLayerSide.Last -> lastLayerAllowedCuboidOrientations
        }
        if (allowedCylinderAxes == null && allowedCuboidOrientations == null) {
            return ok
        }
        for (unitPlacement in placement.unit.units) {
            when (val unit = ensureBoundaryUnitSatisfied(
                binIndex = binIndex,
                side = side,
                layerPlacement = placement,
                unitPlacement = unitPlacement,
                allowedCylinderAxes = allowedCylinderAxes,
                allowedCuboidOrientations = allowedCuboidOrientations
            )) {
                is Ok -> {}
                is Failed -> return Failed(unit.error)
                is Fatal -> return Fatal(unit.errors)
            }
        }
        return ok
    }

    /**
     * 校验单个边界层单元是否满足方向约束。
     * Validate whether a single boundary layer unit satisfies orientation constraints.
     *
     * @param binIndex 容器索引 / bin index
     * @param side 边界层侧别 / boundary layer side
     * @param layerPlacement 层放置信息 / layer placement info
     * @param unitPlacement 单元放置信息 / unit placement info
     * @param allowedCylinderAxes 允许的圆柱轴向集合 / allowed cylinder axes set
     * @param allowedCuboidOrientations 允许的长方体朝向集合 / allowed cuboid orientations set
     * @return 校验结果 / validation result
    */
    private fun ensureBoundaryUnitSatisfied(
        binIndex: Int,
        side: DepthBoundaryLayerSide,
        layerPlacement: QuantityPlacement3<BinLayer, FltX>,
        unitPlacement: QuantityPlacement3<*, FltX>,
        allowedCylinderAxes: Set<Axis3>?,
        allowedCuboidOrientations: Set<Orientation>?
    ): Try {
        val shape = unitPlacement.resolvedPackingShape()
        val axis = shape.axis
        if (axis != null) {
            if (allowedCylinderAxes != null && !allowedCylinderAxes.contains(axis)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Depth boundary layer orientation policy violation: " +
                            "bin=$binIndex, boundary=${side.label}, layer_z=${layerPlacement.z}, " +
                            "item=${unitPlacement.unit.describeBoundaryUnit()}, cylinder_axis=$axis, " +
                            "allowed_cylinder_axes=${allowedCylinderAxes.joinToString()}."
                )
            }
        } else if (allowedCuboidOrientations != null && !allowedCuboidOrientations.contains(unitPlacement.orientation)) {
            return Failed(
                ErrorCode.IllegalArgument,
                "Depth boundary layer orientation policy violation: " +
                        "bin=$binIndex, boundary=${side.label}, layer_z=${layerPlacement.z}, " +
                        "item=${unitPlacement.unit.describeBoundaryUnit()}, cuboid_orientation=${unitPlacement.orientation}, " +
                        "allowed_cuboid_orientations=${allowedCuboidOrientations.joinToString()}."
            )
        }
        return ok
    }

    /**
     * 获取边界单元的描述字符串，优先使用实际物品 ID。
     * Get a description string for the boundary unit, preferring the actual item ID.
     *
     * @return 边界单元的描述字符串 / description string of the boundary unit
    */
    private fun Any.describeBoundaryUnit(): String {
        return (this as? ActualItem)?.id?.toString() ?: toString()
    }

    private companion object {
        /**
         * 校验集合非空约束，null 或非空均合法。
         * Validate that the set is either null or non-empty.
         *
         * @param values 待校验的集合 / the set to validate
         * @param fieldName 字段名称，用于错误信息 / field name used in error message
        */
        private fun requireNonEmpty(values: Set<*>?, fieldName: String) {
            require(values == null || values.isNotEmpty()) {
                "Depth boundary layer orientation policy $fieldName must be null or non-empty."
            }
        }
    }
}

/** 深度边界层侧别 / Depth boundary layer side
 * @property label 侧别的显示标签 / display label of the side
*/
private enum class DepthBoundaryLayerSide(val label: String) {

    /** 第一层 / First layer */
    First("first"),

    /** 最后一层 / Last layer */
    Last("last")
}
