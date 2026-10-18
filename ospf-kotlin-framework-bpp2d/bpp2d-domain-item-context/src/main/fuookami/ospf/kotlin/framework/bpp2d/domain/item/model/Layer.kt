/**
 * 层模型。 / Layer model.
 *
 * 对应 BPP3D 的 `BinLayer`：层是一块板材上的排样单元，由若干二维放置组成，能给出件数、实际占用面积与
 * 利用率。`SheetLayer` 组合多个层，对应 BPP3D 的 `Bin`。
 * / Mirrors BPP3D `BinLayer`: a layer is a packing unit on one sheet made of 2D placements, exposing
 * amounts, actually occupied area and utilisation. `SheetLayer` combines layers, mirroring `Bin`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 二维层：一块板材上的一组放置。 / 2D layer: a group of placements on a sheet.
 *
 * @param V 数值类型 / numeric type
 * @property iteration 生成迭代 / generation iteration
 * @property from 生成来源 / generation source
 * @property sheet 所属板材类型 / owning sheet type
 * @property shape 层的包围形状 / bounding shape of the layer
 * @property units 放置列表，每个放置携带物品编号 / placement list, each carrying its item id
*/
class Layer<V : FloatingNumber<V>>(
    val iteration: Long,
    val from: KClass<*>,
    val sheet: SheetType<V>?,
    val shape: Sheet2Shape<V>,
    val units: List<LayerPlacement2<V>>
) : Copyable<Layer<V>>, Comparable<Layer<V>>, ManualIndexed() {

    /** 层内件数 / amount of items in the layer */
    val amount: UInt64 = UInt64(units.size.toULong())

    /** 层内实际占用面积 / actually occupied area of the layer */
    val area: Quantity<V> = quantitySum(units.map { it.shape.area })
        ?: quantityZeroOf(shape.width)

    /** 层的外接矩形面积 / enclosing rectangle area of the layer */
    val boundingArea: Quantity<V> get() = shape.area

    /** 层的件数映射 / item amount map of the layer */
    val itemAmounts: Map<ItemId, UInt64> = units
        .mapNotNull { placement -> placement.itemId?.let { ItemId(it) } }
        .groupingBy { it }
        .eachCount()
        .mapValues { (_, count) -> UInt64(count.toULong()) }

    /** 层内矩形状与圆形状各自的件数。 / Rectangle and circle counts inside the layer. */
    val shapeCounts: Map<PackingShape2Type, UInt64>
        get() = units
            .groupingBy { it.shape.shapeType }
            .eachCount()
            .mapValues { (_, count) -> UInt64(count.toULong()) }

    /**
     * 层的利用率：实际占用面积与外接矩形面积之比，分母为零时为零。 / Layer utilisation: actually occupied area
     * divided by enclosing rectangle area, zero when the denominator is zero.
     *
     * @return 利用率 / utilisation ratio
    */
    fun utilization(): V {
        if (!quantityIsPositive(shape.area)) {
            return shape.width.value.constants.zero
        }
        return area.value / boundingArea.value
    }

    /**
     * 按占用面积再按件数比较两个层。 / Compare two layers by occupied area then by amount.
     *
     * @param other 另一层 / other layer
     * @return 比较结果 / comparison result
    */
    override fun compareTo(other: Layer<V>): Int {
        when (quantityOrdOrNull(area, other.area)) {
            is Order.Less -> return -1
            is Order.Greater -> return 1
            is Order.Equal, null -> {}
        }
        return amount.compareTo(other.amount)
    }

    override fun copy(): Layer<V> {
        return Layer(
            iteration = iteration,
            from = from,
            sheet = sheet,
            shape = shape.copy(),
            units = units.map { it.copy() }
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Layer<*>) return false
        // 板材类型必须参与比较：同一排布放在不同板材上是不同的列，否则多板材类型会被错误去重。
        // The sheet type must participate: the same layout on different sheets is a different column, otherwise
        // multiple sheet types would be deduplicated incorrectly.
        if (sheet?.typeCode != other.sheet?.typeCode) return false
        if (units.size != other.units.size) return false
        if (!quantityEqual(shape.width, other.shape.width)) return false
        if (!quantityEqual(shape.height, other.shape.height)) return false
        return units == other.units
    }

    override fun hashCode(): Int {
        var result = units.hashCode()
        result = 31 * result + (sheet?.typeCode?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "Layer(${units.size}, ${shape.width.value}×${shape.height.value})"
}

/**
 * 板材层组合：一块板材上叠加的层，对应 BPP3D 的 `Bin`。 / Sheet layer combination: layers stacked on one
 * sheet, mirroring BPP3D `Bin`.
 *
 * @param V 数值类型 / numeric type
 * @property type 板材类型 / sheet type
 * @property shape 组合后的包围形状 / combined bounding shape
 * @property layers 层列表 / layer list
 * @property batchNo 批次号 / batch number
*/
class SheetLayer<V : FloatingNumber<V>>(
    val type: SheetType<V>,
    val shape: Sheet2Shape<V>,
    val layers: List<Layer<V>>,
    val batchNo: BatchNo? = null
) : Copyable<SheetLayer<V>>, Comparable<SheetLayer<V>>, ManualIndexed() {

    /** 组合内件数 / amount inside the combination */
    val amount: UInt64 = UInt64(layers.sumOf { it.units.size }.toULong())

    /** 组合内实际占用面积 / actually occupied area of the combination */
    val area: Quantity<V> = quantitySum(layers.map { it.area })
        ?: quantityZeroOf(shape.width)

    /**
     * 板材装载率：实际占用面积与板材面积之比，板材面积为零时为零。 / Sheet loading rate: actually occupied area
     * over sheet area, zero when the sheet area is zero.
     *
     * @return 装载率 / loading rate
    */
    val loadingRate: V
        get() {
            if (!quantityIsPositive(type.area)) {
                return shape.width.value.constants.zero
            }
            return area.value / type.area.value
        }

    /** 组合内件数映射 / item amount map of the combination */
    val itemAmounts: Map<ItemId, UInt64>
        get() {
            val counter = LinkedHashMap<ItemId, UInt64>()
            for (layer in layers) {
                for ((itemId, itemAmount) in layer.itemAmounts) {
                    counter[itemId] = (counter[itemId] ?: UInt64.zero) + itemAmount
                }
            }
            return counter
        }

    override fun compareTo(other: SheetLayer<V>): Int {
        when (quantityOrdOrNull(area, other.area)) {
            is Order.Less -> return -1
            is Order.Greater -> return 1
            is Order.Equal, null -> {}
        }
        return amount.compareTo(other.amount)
    }

    override fun copy(): SheetLayer<V> {
        return SheetLayer(
            type = type,
            shape = shape.copy(),
            layers = layers.map { it.copy() },
            batchNo = batchNo
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SheetLayer<*>) return false
        return layers == other.layers
    }

    override fun hashCode(): Int = layers.hashCode()

    override fun toString(): String = "SheetLayer(${layers.size} layers, rate=$loadingRate)"
}
