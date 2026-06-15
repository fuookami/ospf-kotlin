/**
 * 终态装箱几何契约。
 * Final packing geometry contract.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.geometry.Axis3

/**
 * 横向圆柱支撑不足错误信息。
 * Horizontal cylinder support violation message.
 *
 * @param source 调用来源 / call source
 * @param binName 箱号 / bin name
 * @param itemIndex 物品序号 / item index
 * @param diagnostic 形状诊断信息 / shape diagnostic
 * @return 错误信息 / error message
 */
internal fun unsupportedHorizontalCylinderSupportMessage(
    source: String,
    binName: String,
    itemIndex: Int,
    diagnostic: String
): String {
    return "Unsupported placement geometry in $source: type=horizontal_support, bin=$binName, item[$itemIndex] $diagnostic must be placed on bin floor or cuboid support coverage."
}

/**
 * 越界放置错误信息。
 * Outside-bin placement violation message.
 *
 * @param source 调用来源 / call source
 * @param binName 箱号 / bin name
 * @param itemIndex 物品序号 / item index
 * @param diagnostic 形状诊断信息 / shape diagnostic
 * @return 错误信息 / error message
 */
internal fun unsupportedOutsideBinGeometryMessage(
    source: String,
    binName: String,
    itemIndex: Int,
    diagnostic: String
): String {
    return "Unsupported placement geometry in $source: type=outside_bin, bin=$binName, item[$itemIndex] $diagnostic is outside bin."
}

/**
 * 终态放置重叠错误信息。
 * Final placement overlap violation message.
 *
 * @param source 调用来源 / call source
 * @param binName 箱号 / bin name
 * @param lhsIndex 左侧物品序号 / left item index
 * @param lhsDiagnostic 左侧形状诊断信息 / left shape diagnostic
 * @param rhsIndex 右侧物品序号 / right item index
 * @param rhsDiagnostic 右侧形状诊断信息 / right shape diagnostic
 * @return 错误信息 / error message
 */
internal fun unsupportedPlacementOverlapMessage(
    source: String,
    binName: String,
    lhsIndex: Int,
    lhsDiagnostic: String,
    rhsIndex: Int,
    rhsDiagnostic: String
): String {
    return "Unsupported placement geometry in $source: type=overlap, bin=$binName, item[$lhsIndex] $lhsDiagnostic overlaps item[$rhsIndex] $rhsDiagnostic."
}

/**
 * 单层圆柱轴向混用错误信息。
 * Single-layer cylinder axis mixing violation message.
 *
 * @param source 调用来源 / call source
 * @param layerIndex 层序号 / layer index
 * @param axes 轴向集合 / axis set
 * @return 错误信息 / error message
 */
internal fun unsupportedMixedCylinderAxesInLayerMessage(
    source: String,
    layerIndex: Int,
    axes: Set<Axis3>
): String {
    val axisText = axes.sortedBy { it.name }.joinToString(
        separator = ", ",
        prefix = "[",
        postfix = "]"
    )
    return "Unsupported placement geometry in $source: layer[$layerIndex] mixes cylinder axes $axisText."
}
