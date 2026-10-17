/**
 * 二维装箱聚合。 / 2D packing aggregation.
 *
 * 对应 BPP3D 的 `Aggregation`：三维侧的 `PackedBin` 在二维侧成为 `PackedSheet`（一块板材及其上的放置），
 * 聚合结果再与物料汇总、附加信息一起构成 `PackingResult`。
 * / Mirrors BPP3D `Aggregation`: the 3D `PackedBin` becomes `PackedSheet` (one sheet plus its placements),
 * and the aggregation together with the material summary and extra info forms `PackingResult`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*

/**
 * 已装箱物品：放置信息与装载顺序。 / Packed item: the placement plus its loading order.
 *
 * @property placement 物品在板材上的二维放置 / 2D placement of the item on the sheet
 * @property loadingOrder 物品在板材内的装载顺序，首个装载的序号为 1 / loading order inside the sheet, the
 * first loaded item being 1
 * @property itemId 物品编号文本 / plain item identifier text
*/
data class PackedItem(
    val placement: LayerPlacement2<FltX>,
    val loadingOrder: UInt64,
    val itemId: String? = null
)

/**
 * 已装箱板材：一块板材及其上的物品。 / Packed sheet: one sheet and the items on it.
 *
 * `actualArea` 与 `loadingRate` 都以**实际占用面积**为口径，圆形件按 πr² 计入，而非外接矩形面积，
 * 与 `Layer.area`、`SheetLayer.loadingRate` 保持一致。
 * / Both `actualArea` and `loadingRate` use the **actual occupied area**, counting circles by πr² rather
 * than by their enclosing rectangle, matching `Layer.area` and `SheetLayer.loadingRate`.
 *
 * @property name 板材实例名称 / sheet instance name
 * @property type 板材类型 / sheet type
 * @property batchNo 批次号 / batch number
 * @property items 已装箱物品列表 / packed item list
*/
data class PackedSheet(
    val name: String,
    val type: SheetType<FltX>,
    val batchNo: BatchNo? = null,
    val items: List<PackedItem> = emptyList()
) {

    /** 板材实际占用面积，无物品时为零 / actually occupied area of the sheet, zero when it holds no item */
    val actualArea: Quantity<FltX> = quantitySum(items.map { it.placement.area })
        ?: quantityZeroOf(type.width)

    /**
     * 板材装载率：实际占用面积与板材面积之比，板材面积非正时为零。 / Sheet loading rate: the actually
     * occupied area over the sheet area, zero when the sheet area is not positive.
     *
     * @return 装载率 / loading rate
    */
    val loadingRate: FltX
        get() {
            if (!quantityIsPositive(type.area)) {
                return type.width.value.constants.zero
            }
            return actualArea.value / type.area.value
        }
}

/**
 * 一次装箱结果中所有板材的聚合。 / Aggregation of all sheets in one packing result.
 *
 * @property bins 已装箱板材列表 / packed sheet list
*/
data class PackingAggregation(
    val bins: List<PackedSheet>
)

/**
 * 物料使用汇总。 / Material usage summary.
 *
 * @property material 物料键 / material key
 * @property amount 该物料的总件数 / total amount of the material
*/
data class MaterialSummary(
    val material: MaterialKey,
    val amount: UInt64
)

/**
 * 最终装箱结果。 / Final packing result.
 *
 * @property aggregation 所有板材的装箱聚合 / packing aggregation over all sheets
 * @property materialSummary 物料使用汇总 / material usage summary
 * @property info 附加键值信息 / additional key-value information
*/
data class PackingResult(
    val aggregation: PackingAggregation,
    val materialSummary: List<MaterialSummary> = emptyList(),
    val info: Map<String, String> = emptyMap()
)
