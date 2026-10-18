/**
 * 物品模型。 / Item model.
 *
 * 对应 BPP3D 的 `Item`：物品持有装载形状（矩形件或圆形件）、是否允许 90° 旋转、所属物料与件数。
 * 二维侧不再区分长方体朝向，旋转只有「不旋转 / 旋转 90°」两种。
 * / Mirrors BPP3D `Item`: an item owns a packing shape (rectangle or circle), rotation allowance,
 * materials and amount. 2D has only "upright" and "rotated by 90 degrees".
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.item.model

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 物品抽象接口。 / Abstract item interface.
 *
 * @param V 数值类型 / numeric type
*/
interface AbstractItem<V : FloatingNumber<V>> {

    /** 物品编号 / item identifier */
    val id: ItemId

    /** 物品名称 / item name */
    val name: String

    /** 装载形状 / packing shape */
    val shape: PackingShape2<V>

    /** 是否允许旋转 90 度 / whether rotation by 90 degrees is allowed */
    val allowRotate: Boolean

    /** 单件重量 / per-piece weight */
    val weight: Quantity<V>

    /** 物料与件数映射 / material-to-amount map */
    val materialAmounts: Map<MaterialKey, UInt64>

    /** 批次号 / batch number */
    val batchNo: BatchNo?

    /** 包围宽度 / bounding width */
    val width: Quantity<V> get() = shape.boundingWidth

    /** 包围高度 / bounding height */
    val height: Quantity<V> get() = shape.boundingHeight

    /** 实际占用面积 / actual occupied area */
    val area: Quantity<V> get() = shape.area

    /** 外接矩形面积 / enclosing rectangle area */
    val boundingArea: Quantity<V> get() = shape.boundingArea

    /** 形状类型 / shape type */
    val shapeType: PackingShape2Type get() = shape.shapeType
}

/**
 * 实际物品：板材上真正被排样的物品。 / Actual item: the item really placed on a sheet.
 *
 * @param V 数值类型 / numeric type
 * @property id 物品编号 / item identifier
 * @property name 物品名称 / item name
 * @property shape 装载形状 / packing shape
 * @property allowRotate 是否允许旋转 90 度 / whether rotation by 90 degrees is allowed
 * @property weight 单件重量 / per-piece weight
 * @property materialAmounts 物料与件数映射 / material-to-amount map
 * @property batchNo 批次号 / batch number
*/
data class ActualItem<V : FloatingNumber<V>>(
    override val id: ItemId,
    override val name: String,
    override val shape: PackingShape2<V>,
    override val allowRotate: Boolean,
    override val weight: Quantity<V>,
    override val materialAmounts: Map<MaterialKey, UInt64> = emptyMap(),
    override val batchNo: BatchNo? = null
) : AbstractItem<V>, Copyable<ActualItem<V>> {

    override fun copy(): ActualItem<V> {
        return copy(
            materialAmounts = materialAmounts.toMap()
        )
    }

    override fun toString(): String = "$id-$name"
}

/**
 * 图样物品：可重复多次的图样块，用于条带/图样候选生成。 / Pattern item: a repeated pattern block used by
 * strip and pattern candidate generation.
 *
 * @param V 数值类型 / numeric type
 * @property id 物品编号 / item identifier
 * @property name 物品名称 / item name
 * @property shape 装载形状 / packing shape
 * @property allowRotate 是否允许旋转 90 度 / whether rotation by 90 degrees is allowed
 * @property weight 单件重量 / per-piece weight
 * @property materialAmounts 物料与件数映射 / material-to-amount map
 * @property batchNo 批次号 / batch number
 * @property amount 图样内件数 / amount inside the pattern block
*/
data class PatternItem<V : FloatingNumber<V>>(
    override val id: ItemId,
    override val name: String,
    override val shape: PackingShape2<V>,
    override val allowRotate: Boolean,
    override val weight: Quantity<V>,
    override val materialAmounts: Map<MaterialKey, UInt64> = emptyMap(),
    override val batchNo: BatchNo? = null,
    val amount: UInt64 = UInt64.one
) : AbstractItem<V> {

    override fun toString(): String = "$id-$name×$amount"
}

/**
 * 物品键：按编号、形状、旋转许可与批次识别物品，用于需求归并与列去重。 / Item key identifying an item by id,
 * shape, rotation allowance and batch, used for demand aggregation and column deduplication.
 *
 * @param V 数值类型 / numeric type
 * @property id 物品编号 / item identifier
 * @property shape 装载形状 / packing shape
 * @property allowRotate 是否允许旋转 90 度 / whether rotation by 90 degrees is allowed
 * @property batchNo 批次号 / batch number
*/
data class ItemKey<V : FloatingNumber<V>>(
    val id: ItemId,
    val shape: PackingShape2<V>,
    val allowRotate: Boolean,
    val batchNo: BatchNo?
)

/**
 * 生成物品键。 / Build the item key of an item.
 *
 * @param V 数值类型 / numeric type
 * @return 物品键 / item key
*/
fun <V : FloatingNumber<V>> AbstractItem<V>.key(): ItemKey<V> {
    return ItemKey(
        id = id,
        shape = shape,
        allowRotate = allowRotate,
        batchNo = batchNo
    )
}

/**
 * 按旋转与否展开候选形状。圆形件与正方形始终只有一种候选。 / Candidate shapes by rotation allowance.
 * Circle items and squares always have a single candidate.
 *
 * @param V 数值类型 / numeric type
 * @return 候选形状列表 / candidate shape list
*/
fun <V : FloatingNumber<V>> AbstractItem<V>.candidateShapes(): List<Shape2Candidate<V>> {
    return shape.candidateShapes(allowRotate)
}

/**
 * 判断物品是否为圆形件。 / Check whether an item is circular.
 *
 * @param V 数值类型 / numeric type
 * @return 是否圆形件 / whether circular
*/
fun <V : FloatingNumber<V>> AbstractItem<V>.isCircleItem(): Boolean {
    return shape.isCircle()
}