package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.*

/**
 * 生成以米为单位的量值。 / Build a metre quantity.
 *
 * @param value 数值 / numeric value
 * @return 量值 / quantity
*/
internal fun meters(value: Double): Quantity<FltX> = FltX(value) * Meter

/**
 * 生成板材形状。 / Build a sheet shape.
 *
 * @param width 宽度 / width
 * @param height 高度 / height
 * @return 板材形状 / sheet shape
*/
internal fun sheetShapeOf(width: Double, height: Double): Sheet2Shape<FltX> {
    return sheet2Shape(width = meters(width), height = meters(height)).valueOrFail()
}

/**
 * 生成矩形放置。 / Build a rectangle placement.
 *
 * @param x 左下角 x 坐标 / lower-left x coordinate
 * @param y 左下角 y 坐标 / lower-left y coordinate
 * @param width 宽度 / width
 * @param height 高度 / height
 * @param rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @param itemId 物品编号 / item identifier
 * @return 矩形放置 / rectangle placement
*/
internal fun rectanglePlacementOf(
    x: Double,
    y: Double,
    width: Double,
    height: Double,
    rotated: Boolean = false,
    itemId: String? = null
): LayerPlacement2<FltX> {
    return LayerPlacement2(
        shape = rectangleShape2(width = meters(width), height = meters(height)).valueOrFail(),
        position = QuantityPoint2(x = meters(x), y = meters(y)),
        rotated = rotated,
        itemId = itemId
    )
}

/**
 * 生成圆形放置。 / Build a circle placement.
 *
 * @param x 左下角 x 坐标 / lower-left x coordinate
 * @param y 左下角 y 坐标 / lower-left y coordinate
 * @param radius 半径 / radius
 * @param rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @param itemId 物品编号 / item identifier
 * @return 圆形放置 / circle placement
*/
internal fun circlePlacementOf(
    x: Double,
    y: Double,
    radius: Double,
    rotated: Boolean = false,
    itemId: String? = null
): LayerPlacement2<FltX> {
    return LayerPlacement2(
        shape = circleShape2(radius = meters(radius)).valueOrFail(),
        position = QuantityPoint2(x = meters(x), y = meters(y)),
        rotated = rotated,
        itemId = itemId
    )
}

/**
 * 生成测试用矩形物品。 / Build a rectangle item for tests.
 *
 * @param id 物品编号 / item identifier
 * @param allowRotate 是否允许旋转 90 度 / whether rotation by 90 degrees is allowed
 * @param width 宽度 / width
 * @param height 高度 / height
 * @param materialAmounts 物料与件数映射 / material-to-amount map
 * @return 实际物品 / actual item
*/
internal fun rectangleItemOf(
    id: String,
    allowRotate: Boolean,
    width: Double = 1.0,
    height: Double = 3.0,
    materialAmounts: Map<MaterialKey, UInt64> = emptyMap()
): ActualItem<FltX> {
    return ActualItem(
        id = ItemId(id),
        name = id,
        shape = rectangleShape2(width = meters(width), height = meters(height)).valueOrFail(),
        allowRotate = allowRotate,
        weight = FltX(1.0) * Kilogram,
        materialAmounts = materialAmounts
    )
}

/**
 * 生成板材类型。 / Build a sheet type.
 *
 * @param width 宽度 / width
 * @param height 高度 / height
 * @param code 类型编码 / type code
 * @param maxLoad 最大承重 / maximum load
 * @return 板材类型 / sheet type
*/
internal fun sheetTypeOf(
    width: Double,
    height: Double,
    code: String = "S1",
    maxLoad: Double = 1000.0
): SheetType<FltX> {
    return SheetType(
        width = meters(width),
        height = meters(height),
        maxLoad = FltX(maxLoad) * Kilogram,
        typeCode = SheetTypeId(code),
        isMain = true
    )
}

/**
 * 生成层。 / Build a layer.
 *
 * @param type 所属板材类型 / owning sheet type
 * @param units 放置列表 / placement list
 * @param width 层宽，缺省时取板材宽度 / layer width, the sheet width when omitted
 * @param height 层高，缺省时取板材高度 / layer height, the sheet height when omitted
 * @return 层 / layer
*/
internal fun layerOf(
    type: SheetType<FltX>,
    units: List<LayerPlacement2<FltX>>,
    width: Double? = null,
    height: Double? = null
): Layer<FltX> {
    val layerWidth = width ?: type.width.value.toDouble()
    val layerHeight = height ?: type.height.value.toDouble()
    return Layer(
        iteration = 0L,
        from = Layer::class,
        sheet = type,
        shape = sheetShapeOf(width = layerWidth, height = layerHeight),
        units = units
    )
}

/**
 * 生成板材实例。 / Build a sheet instance.
 *
 * @param type 板材类型 / sheet type
 * @param units 放置列表 / placement list
 * @param batch 批次号 / batch number
 * @return 板材实例 / sheet instance
*/
internal fun sheetOf(
    type: SheetType<FltX>,
    units: List<LayerPlacement2<FltX>>,
    batch: String? = null
): Sheet<FltX> {
    val batchNo = batch?.let { BatchNo(it) }
    return Sheet(
        type = type,
        layers = listOf(
            SheetLayer(
                type = type,
                shape = sheetShapeOf(width = type.width.value.toDouble(), height = type.height.value.toDouble()),
                layers = listOf(layerOf(type = type, units = units)),
                batchNo = batchNo
            )
        ),
        batchNo = batchNo
    )
}
