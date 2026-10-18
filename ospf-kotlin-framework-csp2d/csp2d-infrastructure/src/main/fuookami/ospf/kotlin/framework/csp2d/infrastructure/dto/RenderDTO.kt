/**
 * 二维套裁渲染 DTO。 / 2D cutting stock renderer DTOs.
 *
 * 与 BPP2D 的渲染 DTO 对齐，但额外输出**刀线序列**：断头台工艺下车间需要的是下刀顺序，只给放置坐标无法施工。
 * / Aligned with the BPP2D renderer DTOs, with an additional **cut sequence**: under a guillotine process the workshop
 * needs the cut order, and placement coordinates alone are not manufacturable.
*/
package fuookami.ospf.kotlin.framework.csp2d.infrastructure.dto

/**
 * 渲染刀线描述。 / Render cut line description.
 *
 * @property orientation 刀线方向，`vertical` 或 `horizontal` / cut line orientation, `vertical` or `horizontal`
 * @property position 刀缝起始坐标 / the coordinate where the kerf strip starts
 * @property stage 工序级数 / process stage
 * @property stageLabel 工序标签 / process stage label
*/
data class RenderCutLine2DTO(
    val orientation: String,
    val position: Double,
    val stage: Int,
    val stageLabel: String
)

/**
 * 渲染裁切件描述。 / Render cut piece description.
 *
 * @property pieceId 成品件标识，余料为 null / finished part identifier, null for a remnant
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property width 宽度 / width
 * @property height 高度 / height
 * @property area 面积 / area
 * @property rotated 是否旋转 90° / whether rotated by 90 degrees
*/
data class RenderCutPiece2DTO(
    val pieceId: String?,
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val area: Double,
    val rotated: Boolean
)

/**
 * 渲染裁切图案描述：一张板材（或一段卷材）上的排样与刀序。 / Render cutting pattern description: the layout and cut
 * sequence on one sheet (or one coil segment).
 *
 * @property id 图案编号 / pattern identifier
 * @property sheetTypeCode 板材类型编码 / sheet type code
 * @property width 宽度 / width
 * @property height 高度 / height
 * @property area 面积 / area
 * @property usedArea 成品占用面积 / area occupied by finished parts
 * @property wasteArea 余料面积 / remnant area
 * @property utilization 利用率 / utilization
 * @property process 裁切工艺 / cutting process
 * @property pieces 裁切件列表 / cut piece list
 * @property cutLines 刀线序列 / cut sequence
*/
data class RenderCuttingPattern2DTO(
    val id: String,
    val sheetTypeCode: String,
    val width: Double,
    val height: Double,
    val area: Double,
    val usedArea: Double,
    val wasteArea: Double,
    val utilization: Double,
    val process: String,
    val pieces: List<RenderCutPiece2DTO>,
    val cutLines: List<RenderCutLine2DTO> = emptyList()
)

/**
 * 渲染裁切方案描述：一个方案是若干图案按次数的组合。 / Render cutting plan description: a plan is a combination of
 * patterns with usage counts.
 *
 * @property id 方案编号 / plan identifier
 * @property materialCode 物料编码 / material code
 * @property amount 方案使用次数 / plan usage count
 * @property patterns 图案列表 / pattern list
 * @property wasteArea 方案总余料面积 / total remnant area of the plan
 * @property utilization 方案利用率 / plan utilization
*/
data class RenderCuttingPlan2DTO(
    val id: String,
    val materialCode: String,
    val amount: Long,
    val patterns: List<RenderCuttingPattern2DTO>,
    val wasteArea: Double,
    val utilization: Double
)

/**
 * 渲染顶层结构。 / Top-level render schema.
 *
 * @property plans 裁切方案列表 / cutting plan list
 * @property kpi 关键指标 / key indicators
*/
data class RenderCuttingSchema2DTO(
    val plans: List<RenderCuttingPlan2DTO>,
    val kpi: Map<String, String> = emptyMap()
)
