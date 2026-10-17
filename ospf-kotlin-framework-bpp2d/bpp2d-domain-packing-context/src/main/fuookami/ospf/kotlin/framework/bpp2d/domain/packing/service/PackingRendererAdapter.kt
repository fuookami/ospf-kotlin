/**
 * 装箱渲染适配器。 / Packing renderer adapter.
 *
 * 对应 BPP3D 的 `PackingRendererAdapter`：把 `PackingResult` 映射为外部渲染器消费的 DTO。**双精度转换只允许
 * 出现在本适配器边界**，领域层始终保留 `Quantity<FltX>`。
 * / Mirrors BPP3D `PackingRendererAdapter`: maps a `PackingResult` onto the DTOs consumed by the external
 * renderer. **Double conversions are allowed only at this adapter boundary**; the domain layer always keeps
 * `Quantity<FltX>`.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.dto.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.*

/**
 * 装箱渲染适配器。 / Packing renderer adapter.
*/
class PackingRendererAdapter {

    /**
     * 把装载形状类型转换为渲染形状类型。 / Convert a packing shape type into the render shape type.
     *
     * @return 渲染形状类型 / render shape type
    */
    private fun PackingShape2Type.toRenderShape2Type(): RenderShape2TypeDTO {
        return when (this) {
            PackingShape2Type.Rectangle -> RenderShape2TypeDTO.Rectangle
            PackingShape2Type.Circle -> RenderShape2TypeDTO.Circle
        }
    }

    /**
     * 把装箱结果转换为渲染方案 DTO。 / Convert a packing result into the render schema DTO.
     *
     * 转换前会复用 `PackingGeometryGuard` 校验每个板材的几何，保证交给外部渲染器的数据本身自洽。圆形件
     * 额外输出 `radius` 与 `diameter`，`actualArea` 始终是**实际占用面积**（圆形件为 πr²）。
     * / Each sheet is re-validated by `PackingGeometryGuard` before conversion so the data handed to the
     * external renderer is self-consistent. Circle items additionally carry `radius` and `diameter`, and
     * `actualArea` is always the **actual occupied area** (πr² for circles).
     *
     * @param result 装箱结果 / packing result
     * @return 渲染方案 DTO 或失败 / render schema DTO, or failure
    */
    fun toSchema(result: PackingResult): Ret<RenderSchema2DTO> {
        val plans = ArrayList<RenderLoadingPlan2DTO>(result.aggregation.bins.size)
        for (packedSheet in result.aggregation.bins) {
            val validation = PackingGeometryGuard.requireSheetGeometry(
                sheet = packedSheet.type,
                placements = packedSheet.items.map { it.placement },
                sheetName = packedSheet.name,
                source = "PackingRendererAdapter.toSchema"
            )
            when (validation) {
                is Ok -> {}
                is Failed -> return Failed(validation.error)
                is Fatal -> return Fatal(validation.errors)
            }
            val items = packedSheet.items.mapIndexed { index, packed ->
                toItemDto(
                    packed = packed,
                    sheetName = packedSheet.name,
                    index = index
                )
            }
            plans.add(
                RenderLoadingPlan2DTO(
                    name = packedSheet.name,
                    sheet = RenderSheet2DTO(
                        id = packedSheet.name,
                        typeCode = packedSheet.type.typeCode.value,
                        width = packedSheet.type.width.value.toDouble(),
                        height = packedSheet.type.height.value.toDouble(),
                        area = packedSheet.type.area.value.toDouble(),
                        loadingRate = packedSheet.loadingRate.toDouble()
                    ),
                    items = items,
                    kpi = mapOf(
                        "item_count" to items.size.toString(),
                        "loading_rate" to packedSheet.loadingRate.toDouble().toString()
                    )
                )
            )
        }
        return ok(
            RenderSchema2DTO(
                plans = plans,
                summary = summaryOf(result = result, sheetCount = plans.size)
            )
        )
    }

    /**
     * 把已装箱物品转换为渲染物品 DTO。 / Convert a packed item into the render item DTO.
     *
     * @param packed 已装箱物品 / packed item
     * @param sheetName 所属板材名称 / owning sheet name
     * @param index 物品在板材内的序号 / item index inside the sheet
     * @return 渲染物品 DTO / render item DTO
    */
    private fun toItemDto(packed: PackedItem, sheetName: String, index: Int): RenderItem2DTO {
        val placement = packed.placement
        val shape = placement.shape
        val circle = shape as? CirclePackingShape2
        return RenderItem2DTO(
            name = packed.itemId ?: "$sheetName-item-${index + 1}",
            shapeType = shape.shapeType.toRenderShape2Type(),
            x = placement.x.value.toDouble(),
            y = placement.y.value.toDouble(),
            boundingWidth = shape.boundingWidth.value.toDouble(),
            boundingHeight = shape.boundingHeight.value.toDouble(),
            radius = circle?.radius?.value?.toDouble(),
            diameter = circle?.diameter?.value?.toDouble(),
            actualArea = shape.area.value.toDouble(),
            boundingArea = shape.boundingArea.value.toDouble(),
            rotated = placement.rotated,
            loadingOrder = packed.loadingOrder.toLong(),
            info = mapOf("sheet" to sheetName)
        )
    }

    /**
     * 汇总渲染信息：板材数、物品数、物料数与各物料件数，最后合并调用方附加信息。 / Summarise the render
     * information: sheet count, item count, material count and per-material amounts, with the caller-provided
     * info merged last.
     *
     * @param result 装箱结果 / packing result
     * @param sheetCount 板材数量 / sheet count
     * @return 汇总信息 / summary information
    */
    private fun summaryOf(result: PackingResult, sheetCount: Int): Map<String, String> {
        val summary = LinkedHashMap<String, String>()
        summary["sheet_count"] = sheetCount.toString()
        summary["item_count"] = result.aggregation.bins.sumOf { it.items.size }.toString()
        summary["material_count"] = result.materialSummary.size.toString()
        for (materialSummary in result.materialSummary) {
            summary["material:${materialSummary.material}"] = materialSummary.amount.toString()
        }
        summary.putAll(result.info)
        return summary
    }
}
