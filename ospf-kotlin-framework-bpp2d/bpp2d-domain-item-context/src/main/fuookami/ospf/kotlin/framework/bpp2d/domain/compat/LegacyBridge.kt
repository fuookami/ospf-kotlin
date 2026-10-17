/**
 * 既有 `*Need` API 到新模型的桥接。 / Bridge from the legacy `*Need` API to the new models.
 *
 * `RectangularPackingDemand.kt` 中的 `*Need` 类型是最早落地的二维矩形装箱需求模型，下游可能已经在用。
 * 本文件提供到新框架模型的双向桥接，使既有调用方可以渐进迁移，而不必一次性改写。
 * / The `*Need` types in `RectangularPackingDemand.kt` are the earliest 2D rectangular packing demand
 * models and may already be used downstream. This file bridges them to the new framework models so existing
 * callers can migrate incrementally.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.compat

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetType
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetTypeId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.originPoint2Of
import fuookami.ospf.kotlin.framework.bpp2d.domain.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 把既有矩形物品转换为新物品模型。 / Convert a legacy rectangle item into the new item model.
 *
 * @param V 数值类型 / numeric type
 * @return 实际物品或失败 / actual item, or failure
*/
fun <V : FloatingNumber<V>> RectangleItem2<V>.toActualItem(): Ret<ActualItem<V>> {
    val shape = when (val result = rectangleShape2(width = width, height = height)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return ok(
        ActualItem(
            id = ItemId(id),
            name = id,
            shape = shape,
            allowRotate = allowRotate,
            weight = quantityZeroOf(width)
        )
    )
}

/**
 * 把既有板材转换为新板材类型。 / Convert a legacy sheet into the new sheet type.
 *
 * @param V 数值类型 / numeric type
 * @param typeCode 板材类型编码 / sheet type code
 * @param maxLoad 最大承重 / maximum load
 * @return 板材类型或失败 / sheet type, or failure
*/
fun <V : FloatingNumber<V>> Sheet2<V>.toSheetType(
    typeCode: String = id,
    maxLoad: Quantity<V>? = null
): Ret<SheetType<V>> {
    val shape = when (val result = sheet2Shape(width = width, height = height)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return ok(
        SheetType(
            width = shape.width,
            height = shape.height,
            maxLoad = maxLoad ?: quantityZeroOf(shape.width),
            typeCode = SheetTypeId(typeCode),
            isMain = true
        )
    )
}

/**
 * 把既有放置转换为新放置模型。 / Convert a legacy placement into the new placement model.
 *
 * @param V 数值类型 / numeric type
 * @param allowRotate 是否允许旋转 / whether rotation is allowed
 * @return 放置或失败 / placement, or failure
*/
fun <V : FloatingNumber<V>> PlannedRectangle2<V>.toLayerPlacement(
    allowRotate: Boolean = false
): Ret<LayerPlacement2<V>> {
    val item = when (val result = item.toActualItem()) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val shape = if (rotated && allowRotate) item.shape.rotated() else item.shape
    return ok(
        LayerPlacement2(
            shape = shape,
            position = point2Of(x = x, y = y),
            rotated = rotated && allowRotate,
            itemId = item.id.value
        )
    )
}

/**
 * 把既有盒体需求转换为新包围盒。 / Convert a legacy box need into the new bounding box.
 *
 * @param V 数值类型 / numeric type
 * @return 包围盒或失败 / bounding box, or failure
*/
fun <V : FloatingNumber<V>> Box2Need<V>.toBox2Geometry(): Ret<Box2Geometry<V>> {
    val width = when (val result = quantityMinusSafe(maxX, minX)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val height = when (val result = quantityMinusSafe(maxY, minY)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return ok(
        Box2Geometry.of(
            x = minX,
            y = minY,
            width = width,
            height = height
        )
    )
}

/**
 * 把既有二维场景转换为新层。 / Convert a legacy 2D scene into the new layer.
 *
 * @param V 数值类型 / numeric type
 * @return 层或失败 / layer, or failure
*/
fun <V : FloatingNumber<V>> PackingScene2<V>.toLayer(): Ret<Layer<V>> {
    val shape = when (val result = sheet2Shape(width = sheet.width, height = sheet.height)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    val converted = ArrayList<LayerPlacement2<V>>(placements.size)
    for (placement in placements) {
        when (val result = placement.toLayerPlacement(allowRotate = placement.item.allowRotate)) {
            is Ok -> converted.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    val sheetShape = Sheet2Shape(width = shape.width, height = shape.height)
    return ok(
        Layer(
            iteration = 0L,
            from = PackingScene2::class,
            sheet = null,
            shape = sheetShape,
            units = converted
        )
    )
}

/**
 * 由新层构造既有场景。 / Build a legacy scene from the new layer.
 *
 * @param V 数值类型 / numeric type
 * @return 场景或失败 / scene, or failure
*/
fun <V : FloatingNumber<V>> Layer<V>.toLegacyScene(): Ret<PackingScene2<V>> {
    val completed = units.all { it.itemId != null }
    if (!completed) {
        return Failed(
            Bpp2dValidationError(
                "层内放置缺少物品编号，无法还原为既有场景 / Layer placements lack item ids, cannot rebuild the legacy scene"
            )
        )
    }
    val itemCache = LinkedHashMap<String, RectangleItem2<V>>()
    val planned = ArrayList<PlannedRectangle2<V>>(units.size)
    for (placement in units) {
        val itemId = placement.itemId ?: continue
        val item = itemCache.getOrPut(itemId) {
            RectangleItem2(
                id = itemId,
                width = placement.shape.boundingWidth,
                height = placement.shape.boundingHeight,
                allowRotate = placement.rotated
            )
        }
        planned.add(
            PlannedRectangle2(
                item = item,
                x = placement.x,
                y = placement.y,
                rotated = placement.rotated
            )
        )
    }
    val sheet = Sheet2(
        id = "layer-${index}",
        width = shape.width,
        height = shape.height
    )
    return ok(
        PackingScene2(
            sheet = sheet,
            placements = planned
        )
    )
}

/**
 * 便于从原点构造落点，转出给兼容层使用。 / Re-export origin construction for the compatibility layer.
 *
 * @param V 数值类型 / numeric type
 * @param sample 样本量值 / sample quantity
 * @return 原点落点 / origin position
*/
internal fun <V : FloatingNumber<V>> compatOrigin(sample: Quantity<V>) = originPoint2Of(sample)
