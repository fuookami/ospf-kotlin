/**
 * 装箱几何契约消息。 / Packing geometry contract messages.
 *
 * 与 BPP3D 的 `PackingGeometryContract` 对齐：把几何违规整理为可检索的中英双语消息，统一采用
 * `Unsupported placement geometry in $source: type=..., bin=..., item[i] ...` 结构，便于按 `type` 与
 * `bin` 检索定位问题。
 * / Mirrors BPP3D `PackingGeometryContract`: geometry violations are rendered as searchable bilingual
 * messages sharing the `Unsupported placement geometry in $source: type=..., bin=..., item[i] ...`
 * structure, so problems can be located by `type` and `bin`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 生成单个放置的形状诊断信息。 / Build the shape diagnostic of one placement.
 *
 * @param shape 装载形状 / packing shape
 * @param rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @return 形状诊断信息 / shape diagnostic
*/
internal fun placementShapeDiagnostic(shape: PackingShape2<FltX>, rotated: Boolean): String {
    val typeText = when (shape.shapeType) {
        PackingShape2Type.Rectangle -> "rectangle"
        PackingShape2Type.Circle -> "circle"
    }
    return "形状 shape=$typeText, rotated=$rotated"
}

/**
 * 生成形状构成诊断信息，用于混排矩形件与圆形件时的排查。 / Build the shape-composition diagnostic used
 * when rectangles and circles are mixed on one sheet.
 *
 * @param counts 各形状类型的件数 / amount per shape type
 * @return 形状构成诊断信息 / shape composition diagnostic
*/
internal fun mixedShapeDiagnostic(counts: Map<PackingShape2Type, Int>): String {
    val text = counts.entries
        .sortedBy { it.key.name }
        .joinToString(separator = ", ") { (type, count) -> "${type.name.lowercase()}=$count" }
    return "形状构成 mixed shapes [$text]"
}

/**
 * 放置越过板材边界的错误信息。 / Outside-sheet placement violation message.
 *
 * @param source 调用来源 / call source
 * @param sheetName 板材实例名称，即消息中的 `bin` / sheet instance name, the `bin` field of the message
 * @param itemIndex 物品序号 / item index
 * @param diagnostic 形状诊断信息 / shape diagnostic
 * @param mixedDiagnostic 形状构成诊断信息 / shape composition diagnostic
 * @return 错误信息 / error message
*/
internal fun unsupportedOutsideSheetGeometryMessage(
    source: String,
    sheetName: String,
    itemIndex: Int,
    diagnostic: String,
    mixedDiagnostic: String
): String {
    return "放置越过板材边界 / Placement outside sheet: " +
            "Unsupported placement geometry in $source: type=outside_sheet, bin=$sheetName, " +
            "item[$itemIndex] $diagnostic is outside sheet; $mixedDiagnostic."
}

/**
 * 放置相互重叠的错误信息。 / Placement overlap violation message.
 *
 * @param source 调用来源 / call source
 * @param sheetName 板材实例名称，即消息中的 `bin` / sheet instance name, the `bin` field of the message
 * @param lhsIndex 左侧物品序号 / left item index
 * @param lhsDiagnostic 左侧形状诊断信息 / left shape diagnostic
 * @param rhsIndex 右侧物品序号 / right item index
 * @param rhsDiagnostic 右侧形状诊断信息 / right shape diagnostic
 * @param mixedDiagnostic 形状构成诊断信息 / shape composition diagnostic
 * @return 错误信息 / error message
*/
internal fun unsupportedPlacementOverlapMessage(
    source: String,
    sheetName: String,
    lhsIndex: Int,
    lhsDiagnostic: String,
    rhsIndex: Int,
    rhsDiagnostic: String,
    mixedDiagnostic: String
): String {
    return "放置相互重叠 / Placement overlap: " +
            "Unsupported placement geometry in $source: type=overlap, bin=$sheetName, " +
            "item[$lhsIndex] $lhsDiagnostic overlaps item[$rhsIndex] $rhsDiagnostic; $mixedDiagnostic."
}

/**
 * 放置非法旋转的错误信息。 / Illegal rotation violation message.
 *
 * @param source 调用来源 / call source
 * @param sheetName 板材实例名称，即消息中的 `bin` / sheet instance name, the `bin` field of the message
 * @param itemIndex 物品序号 / item index
 * @param diagnostic 形状诊断信息 / shape diagnostic
 * @param itemId 物品编号，未知时为 null / item identifier, null when unknown
 * @param mixedDiagnostic 形状构成诊断信息 / shape composition diagnostic
 * @return 错误信息 / error message
*/
internal fun unsupportedIllegalRotationMessage(
    source: String,
    sheetName: String,
    itemIndex: Int,
    diagnostic: String,
    itemId: String?,
    mixedDiagnostic: String
): String {
    val itemText = itemId ?: "unknown"
    return "放置非法旋转 / Illegal placement rotation: " +
            "Unsupported placement geometry in $source: type=illegal_rotation, bin=$sheetName, " +
            "item[$itemIndex] $diagnostic (item=$itemText) is rotated although the item forbids rotation; " +
            "$mixedDiagnostic."
}
