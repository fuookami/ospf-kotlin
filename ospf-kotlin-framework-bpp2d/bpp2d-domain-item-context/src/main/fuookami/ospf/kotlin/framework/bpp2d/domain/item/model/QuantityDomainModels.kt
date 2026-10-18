/**
 * 量纲域模型。 / Quantity-typed domain models.
 *
 * 对应 BPP3D 的 `QuantityDomainModels`：以完全泛型的数值类型描述输入，并提供 `toModel()` 收敛到求解器
 * 使用的 `FltX` 模型。转换必须经过 `Bpp2dSolverValueAdapter`。
 * / Mirrors BPP3D `QuantityDomainModels`: inputs are described with a fully generic numeric type and
 * `toModel()` converges them onto the `FltX` solver model. Conversion goes through
 * `Bpp2dSolverValueAdapter`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 把泛型量值转换为 `FltX` 量值。 / Convert a generic quantity to an `FltX` quantity.
 *
 * @param V 数值类型 / numeric type
 * @param value 泛型量值 / generic quantity
 * @return FltX 量值 / FltX quantity
*/
fun <V : FloatingNumber<V>> Quantity<V>.toFltXQuantity(): Quantity<FltX> {
    return Quantity(FltX(value.toString().toDouble()), unit)
}

/**
 * 量纲物品形状规格。 / Quantity item shape specification.
 *
 * @param V 数值类型 / numeric type
*/
sealed interface QuantityItemShape<V : FloatingNumber<V>> {

    /** 矩形件规格 / rectangle specification
     *
     * @property width 宽度 / width
     * @property height 高度 / height
    */
    data class Rectangle<V : FloatingNumber<V>>(
        val width: Quantity<V>,
        val height: Quantity<V>
    ) : QuantityItemShape<V>

    /** 圆形件规格 / circle specification
     *
     * @property radius 半径 / radius
    */
    data class Circle<V : FloatingNumber<V>>(
        val radius: Quantity<V>
    ) : QuantityItemShape<V>
}

/**
 * 把量纲形状规格转换为装载形状。 / Convert a quantity shape specification into a packing shape.
 *
 * @param V 数值类型 / numeric type
 * @return 装载形状或失败 / packing shape, or failure
*/
fun <V : FloatingNumber<V>> QuantityItemShape<V>.toShape(): Ret<PackingShape2<V>> {
    return when (this) {
        is QuantityItemShape.Rectangle -> rectangleShape2(width = width, height = height)
        is QuantityItemShape.Circle -> circleShape2(radius = radius)
    }
}

/**
 * 量纲物品。 / Quantity item.
 *
 * @param V 数值类型 / numeric type
 * @property id 物品编号 / item identifier
 * @property name 物品名称 / item name
 * @property shape 量纲形状规格 / quantity shape specification
 * @property allowRotate 是否允许旋转 90 度 / whether rotation is allowed
 * @property weight 单件重量 / per-piece weight
 * @property materialAmounts 物料与件数映射 / material-to-amount map
 * @property batchNo 批次号 / batch number
 * @property amount 件数 / amount
*/
data class QuantityItem<V : FloatingNumber<V>>(
    val id: ItemId,
    val name: String,
    val shape: QuantityItemShape<V>,
    val allowRotate: Boolean,
    val weight: Quantity<V>,
    val materialAmounts: Map<MaterialNo, UInt64> = emptyMap(),
    val batchNo: BatchNo? = null,
    val amount: UInt64 = UInt64.one
) {

    /**
     * 转换为 `FltX` 实际物品。 / Convert to the `FltX` actual item.
     *
     * @param materialCatalog 物料编号到物料键的映射 / material number to material key mapping
     * @return 实际物品或失败 / actual item, or failure
    */
    fun toModel(
        materialCatalog: Map<MaterialNo, MaterialKey> = emptyMap()
    ): Ret<ActualItem<FltX>> {
        val shape2 = when (val converted = shape.toShape()) {
            is Ok -> converted.value
            is fuookami.ospf.kotlin.utils.functional.Failed ->
                return Failed(converted.error)

            is fuookami.ospf.kotlin.utils.functional.Fatal ->
                return Fatal(converted.errors)
        }
        val fltXShape = when (shape2) {
            is RectanglePackingShape2 -> RectanglePackingShape2(
                width = shape2.width.toFltXQuantity(),
                height = shape2.height.toFltXQuantity()
            )

            is CirclePackingShape2 -> CirclePackingShape2(
                radius = shape2.radius.toFltXQuantity()
            )
        }
        val amounts = materialAmounts
            .mapNotNull { (no, amt) -> materialCatalog[no]?.let { key -> key to amt } }
            .toMap()
        return ok(
            ActualItem(
                id = id,
                name = name,
                shape = fltXShape,
                allowRotate = allowRotate,
                weight = weight.toFltXQuantity(),
                materialAmounts = amounts,
                batchNo = batchNo
            )
        )
    }
}

/**
 * 量纲板材类型。 / Quantity sheet type.
 *
 * @param V 数值类型 / numeric type
 * @property width 板材宽度 / sheet width
 * @property height 板材高度 / sheet height
 * @property maxLoad 最大承重 / maximum load
 * @property typeCode 板材类型编码 / sheet type code
 * @property isMain 是否为主板材 / whether it is the main sheet type
*/
data class QuantitySheetType<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>,
    val maxLoad: Quantity<V>,
    val typeCode: SheetTypeId,
    val isMain: Boolean = false
) {

    /**
     * 转换为 `FltX` 板材类型。 / Convert to the `FltX` sheet type.
     *
     * @return 板材类型 / sheet type
    */
    fun toModel(): SheetType<FltX> {
        return SheetType(
            width = width.toFltXQuantity(),
            height = height.toFltXQuantity(),
            maxLoad = maxLoad.toFltXQuantity(),
            typeCode = typeCode,
            isMain = isMain
        )
    }
}

/**
 * 量纲层。 / Quantity layer.
 *
 * @param V 数值类型 / numeric type
 * @property iteration 生成迭代 / generation iteration
 * @property from 生成来源 / generation source
 * @property width 层宽 / layer width
 * @property height 层高 / layer height
 * @property units 放置列表 / placement list
*/
data class QuantityLayer<V : FloatingNumber<V>>(
    val iteration: Long,
    val from: KClass<*>,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val units: List<QuantityLayerPlacement<V>>
) {

    /**
     * 转换为 `FltX` 层。 / Convert to the `FltX` layer.
     *
     * @param sheet 所属板材类型 / owning sheet type
     * @return 层或失败 / layer, or failure
    */
    fun toModel(sheet: SheetType<FltX>? = null): Ret<Layer<FltX>> {
        val shape = when (val converted = sheet2Shape(width.toFltXQuantity(), height.toFltXQuantity())) {
            is Ok -> converted.value
            is fuookami.ospf.kotlin.utils.functional.Failed ->
                return Failed(converted.error)

            is fuookami.ospf.kotlin.utils.functional.Fatal ->
                return Fatal(converted.errors)
        }
        val placements = ArrayList<Placement2<FltX>>(units.size)
        for (unit in units) {
            when (val converted = unit.toModel()) {
                is Ok -> placements.add(converted.value)
                is fuookami.ospf.kotlin.utils.functional.Failed ->
                    return Failed(converted.error)

                is fuookami.ospf.kotlin.utils.functional.Fatal ->
                    return Fatal(converted.errors)
            }
        }
        return ok(
            Layer(
                iteration = iteration,
                from = from,
                sheet = sheet,
                shape = shape,
                units = placements
            )
        )
    }
}

/**
 * 量纲层放置。 / Quantity layer placement.
 *
 * @param V 数值类型 / numeric type
 * @property shape 量纲形状规格 / quantity shape specification
 * @property x x 坐标 / x coordinate
 * @property y y 坐标 / y coordinate
 * @property rotated 是否旋转 90 度 / whether rotated by 90 degrees
 * @property itemId 物品编号 / item identifier
*/
data class QuantityLayerPlacement<V : FloatingNumber<V>>(
    val shape: QuantityItemShape<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val rotated: Boolean = false,
    val itemId: ItemId? = null
) {

    /**
     * 转换为 `FltX` 放置。 / Convert to the `FltX` placement.
     *
     * @return 放置或失败 / placement, or failure
    */
    fun toModel(): Ret<Placement2<FltX>> {
        val shape2 = when (val converted = shape.toShape()) {
            is Ok -> converted.value
            is fuookami.ospf.kotlin.utils.functional.Failed ->
                return Failed(converted.error)

            is fuookami.ospf.kotlin.utils.functional.Fatal ->
                return Fatal(converted.errors)
        }
        val fltXShape = when (shape2) {
            is RectanglePackingShape2 -> RectanglePackingShape2(
                width = shape2.width.toFltXQuantity(),
                height = shape2.height.toFltXQuantity()
            )

            is CirclePackingShape2 -> CirclePackingShape2(
                radius = shape2.radius.toFltXQuantity()
            )
        }
        val fltX = if (rotated) fltXShape.rotated() else fltXShape
        return ok(
            Placement2(
                shape = fltX,
                position = QuantityPoint2(
                    x = x.toFltXQuantity(),
                    y = y.toFltXQuantity()
                ),
                rotated = rotated,
                itemId = itemId?.value
            )
        )
    }
}