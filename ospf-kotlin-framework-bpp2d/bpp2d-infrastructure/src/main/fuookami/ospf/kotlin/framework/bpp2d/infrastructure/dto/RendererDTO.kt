/**
 * 二维渲染 DTO。 / 2D renderer DTOs.
 *
 * 与 BPP3D 的 `RendererDTO` 对齐，输出外部渲染器消费的二维排样数据：板材尺寸、物品形状元数据
 * （矩形或圆形）、坐标与实际占用面积。
 * / Mirrors BPP3D `RendererDTO` and emits the 2D packing data consumed by the external renderer: sheet
 * size, item shape metadata (rectangle or circle), coordinates and the actual occupied area.
*/
package fuookami.ospf.kotlin.framework.bpp2d.infrastructure.dto

/**
 * 渲染形状类型。 / Render shape type.
*/
enum class RenderShape2TypeDTO {
    /** 矩形件 / rectangle */
    Rectangle,

    /** 圆形件 / circle */
    Circle
}

/**
 * 二维矩形描述。 / 2D rectangle description.
 *
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property width 宽度 / width
 * @property height 高度 / height
*/
data class RenderRectangle2DTO(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double
)

/**
 * 二维板材渲染描述。 / 2D sheet render description.
 *
 * @property id 板材编号 / sheet identifier
 * @property typeCode 板材类型编码 / sheet type code
 * @property width 板材宽度 / sheet width
 * @property height 板材高度 / sheet height
 * @property area 板材面积 / sheet area
 * @property loadingRate 板材装载率 / sheet loading rate
*/
data class RenderSheet2DTO(
    val id: String,
    val typeCode: String,
    val width: Double,
    val height: Double,
    val area: Double,
    val loadingRate: Double
)

/**
 * 二维物品渲染描述。 / 2D item render description.
 *
 * @property name 物品名称 / item name
 * @property shapeType 渲染形状类型 / render shape type
 * @property x 左下角 x 坐标 / lower-left x coordinate
 * @property y 左下角 y 坐标 / lower-left y coordinate
 * @property boundingWidth 包围宽度 / bounding width
 * @property boundingHeight 包围高度 / bounding height
 * @property radius 圆形件半径 / circle radius
 * @property diameter 圆形件直径 / circle diameter
 * @property actualArea 实际占用面积 / actual occupied area
 * @property boundingArea 外接矩形面积 / enclosing rectangle area
 * @property rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @property loadingOrder 装载顺序 / loading order
 * @property info 附加信息 / additional info
*/
data class RenderItem2DTO(
    val name: String,
    val shapeType: RenderShape2TypeDTO,
    val x: Double,
    val y: Double,
    val boundingWidth: Double,
    val boundingHeight: Double,
    val radius: Double? = null,
    val diameter: Double? = null,
    val actualArea: Double,
    val boundingArea: Double,
    val rotated: Boolean = false,
    val loadingOrder: Long = 0L,
    val info: Map<String, String> = emptyMap()
)

/**
 * 二维装载方案。 / 2D loading plan.
 *
 * @property name 方案名称 / plan name
 * @property sheet 板材描述 / sheet description
 * @property items 物品描述列表 / item description list
 * @property kpi 关键指标 / key indicators
*/
data class RenderLoadingPlan2DTO(
    val name: String,
    val sheet: RenderSheet2DTO,
    val items: List<RenderItem2DTO>,
    val kpi: Map<String, String> = emptyMap()
)

/**
 * 二维渲染方案。 / 2D render schema.
 *
 * @property plans 装载方案列表 / loading plan list
 * @property summary 汇总信息 / summary information
*/
data class RenderSchema2DTO(
    val plans: List<RenderLoadingPlan2DTO>,
    val summary: Map<String, String> = emptyMap()
)