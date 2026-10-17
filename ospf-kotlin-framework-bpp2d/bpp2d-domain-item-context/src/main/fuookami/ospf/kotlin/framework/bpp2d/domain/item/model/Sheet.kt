/**
 * 板材模型。 / Sheet model.
 *
 * 对应 BPP3D 的 `Bin` / `BinType`：板材类型描述可变产能与承重，板材实例承载已排样的层。
 * / Mirrors BPP3D `Bin` / `BinType`: a sheet type describes area capacity and load limit, while a sheet
 * instance carries the placed layers.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 板材类型描述。 / Sheet type descriptor.
 *
 * @param V 数值类型 / numeric type
 * @property width 板材宽度 / sheet width
 * @property height 板材高度 / sheet height
 * @property maxLoad 最大承重 / maximum load
 * @property typeCode 板材类型编码 / sheet type code
 * @property isMain 是否为主板材 / whether it is the main sheet type
*/
open class SheetType<V : FloatingNumber<V>>(
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    val maxLoad: Quantity<V>,
    open val typeCode: SheetTypeId,
    val isMain: Boolean = false
) : Sheet2Geometry<V> {

    /** 板材面积 / sheet area */
    override val area: Quantity<V> = width * height

    /**
     * 创建覆盖部分属性的副本。 / Create a copy overriding selected properties.
     *
     * @param width 覆盖宽度 / overridden width
     * @param height 覆盖高度 / overridden height
     * @param maxLoad 覆盖最大承重 / overridden maximum load
     * @param typeCode 覆盖类型编码 / overridden type code
     * @param isMain 覆盖是否为主板材 / overridden main flag
     * @return 新的板材类型 / new sheet type
    */
    fun new(
        width: Quantity<V>? = null,
        height: Quantity<V>? = null,
        maxLoad: Quantity<V>? = null,
        typeCode: SheetTypeId? = null,
        isMain: Boolean? = null
    ): SheetType<V> {
        return SheetType(
            width = width ?: this.width,
            height = height ?: this.height,
            maxLoad = maxLoad ?: this.maxLoad,
            typeCode = typeCode ?: this.typeCode,
            isMain = isMain ?: this.isMain
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SheetType<*>) return false
        if (typeCode != other.typeCode) return false
        if (isMain != other.isMain) return false
        return quantityEqual(width, other.width)
                && quantityEqual(height, other.height)
                && quantityEqual(maxLoad, other.maxLoad)
    }

    override fun hashCode(): Int {
        var result = typeCode.hashCode()
        result = 31 * result + isMain.hashCode()
        result = 31 * result + width.value.hashCode()
        result = 31 * result + height.value.hashCode()
        return result
    }

    override fun toString(): String = "$typeCode-${width.value}×${height.value}"
}

/**
 * 板材实例：某种板材类型加其上排好的层。 / Sheet instance: a sheet type plus the layers laid out on it.
 *
 * @param V 数值类型 / numeric type
 * @property type 板材类型 / sheet type
 * @property layers 层列表 / layer list
 * @property batchNo 批次号 / batch number
*/
data class Sheet<V : FloatingNumber<V>>(
    val type: SheetType<V>,
    val layers: List<SheetLayer<V>> = emptyList(),
    val batchNo: BatchNo? = null
) : Copyable<Sheet<V>> {

    /** 板材宽度 / sheet width */
    val width: Quantity<V> get() = type.width

    /** 板材高度 / sheet height */
    val height: Quantity<V> get() = type.height

    /** 已排样物品的件数映射 / amount map of placed items */
    val itemAmounts: Map<ItemId, UInt64>
        get() {
            val counter = LinkedHashMap<ItemId, UInt64>()
            for (layer in layers) {
                for ((itemId, amount) in layer.itemAmounts) {
                    counter[itemId] = (counter[itemId] ?: UInt64.zero) + amount
                }
            }
            return counter
        }

    override fun copy(): Sheet<V> {
        return Sheet(
            type = type,
            layers = layers.map { it.copy() },
            batchNo = batchNo
        )
    }
}
