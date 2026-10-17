/**
 * 装箱器。 / Packer.
 *
 * 对应 BPP3D 的 `Packer`：把排样完成的板材展开为二维放置列表，计算装载顺序、校验几何并汇总物料，
 * 输出 `PackingResult`。
 * / Mirrors BPP3D `Packer`: expands the laid-out sheets into 2D placements, computes the loading order,
 * validates the geometry, summarises the materials and emits a `PackingResult`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.service.LoadingOrderCalculator
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.*

/**
 * 装箱器，把最终板材转换为装箱结果。 / Packer converting the final sheets into a packing result.
 *
 * @property loadingOrderCalculator 装载顺序计算器 / loading order calculator
 * @property itemCatalog 物品目录，用于物料汇总与旋转合法性判定；缺失的物品不参与汇总且视为允许旋转 /
 * item catalogue used for the material summary and rotation legality; a missing item contributes no
 * material and is treated as allowing rotation
*/
class Packer(
    private val loadingOrderCalculator: LoadingOrderCalculator<FltX> = LoadingOrderCalculator(),
    private val itemCatalog: Map<ItemId, AbstractItem<FltX>> = emptyMap()
) {

    /**
     * 展开板材并生成装箱结果。 / Expand the sheets and build the packing result.
     *
     * 每个板材的放置来自 `Sheet.layers → SheetLayer.layers → Layer.units`，装载顺序由
     * `LoadingOrderCalculator` 按「先下后上、先左后右」给出，几何与旋转由 `PackingGeometryGuard` 校验。
     * / Placements of a sheet come from `Sheet.layers → SheetLayer.layers → Layer.units`; the loading order
     * is produced by `LoadingOrderCalculator` following "bottom first, then left to right", and the geometry
     * and rotation are validated by `PackingGeometryGuard`.
     *
     * @param sheets 最终板材列表 / final sheet list
     * @param context 装箱上下文 / packing context
     * @return 装箱结果或失败 / packing result, or failure
    */
    operator fun invoke(
        sheets: List<Sheet<FltX>>,
        context: PackingContext = PackingContext()
    ): Ret<PackingResult> {
        val rotationRule = PackingRotationRule.byItemCatalog(itemCatalog)
        val packedSheets = ArrayList<PackedSheet>(sheets.size)
        for ((index, sheet) in sheets.withIndex()) {
            val name = "sheet-${index + 1}"
            val placements = sheet.layers.flatMap { sheetLayer ->
                sheetLayer.layers.flatMap { layer -> layer.units }
            }
            val loadingOrders = loadingOrderCalculator(placements)
            val validation = PackingGeometryGuard.validate(
                sheet = sheet.type,
                placements = placements,
                sheetName = name,
                source = "Packer.invoke",
                rotationRule = rotationRule
            )
            when (validation) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            packedSheets.add(
                PackedSheet(
                    name = name,
                    type = sheet.type,
                    batchNo = sheet.batchNo,
                    items = placements.map { placement ->
                        PackedItem(
                            placement = placement,
                            loadingOrder = loadingOrders[placement] ?: UInt64.zero,
                            itemId = placement.itemId
                        )
                    }
                )
            )
        }
        return ok(
            PackingResult(
                aggregation = PackingAggregation(bins = packedSheets),
                materialSummary = summarizeMaterials(packedSheets),
                info = context.info
            )
        )
    }

    /**
     * 汇总物料使用情况。 / Summarise the material usage.
     *
     * 物料件数来自物品目录中每个已装箱物品的 `materialAmounts`，目录中缺失的物品不计入。 / Material
     * amounts come from the `materialAmounts` of every packed item in the catalogue; items missing from the
     * catalogue are not counted.
     *
     * @param sheets 已装箱板材列表 / packed sheet list
     * @return 物料汇总列表 / material summary list
    */
    private fun summarizeMaterials(sheets: List<PackedSheet>): List<MaterialSummary> {
        val summary = LinkedHashMap<MaterialKey, UInt64>()
        for (sheet in sheets) {
            for (packed in sheet.items) {
                val itemId = packed.itemId ?: continue
                val item = itemCatalog[ItemId(itemId)] ?: continue
                for ((material, amount) in item.materialAmounts) {
                    summary[material] = (summary[material] ?: UInt64.zero) + amount
                }
            }
        }
        return summary.map { (material, amount) ->
            MaterialSummary(
                material = material,
                amount = amount
            )
        }
    }
}
