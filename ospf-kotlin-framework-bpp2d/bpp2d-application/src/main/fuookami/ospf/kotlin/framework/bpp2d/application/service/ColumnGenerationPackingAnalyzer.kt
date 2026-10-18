/**
 * 装箱 KPI 分析器。 / Packing KPI analyzer.
 *
 * 对应 BPP3D 的 `ColumnGenerationPackingAnalyzer`：汇总板材数量、实际占用面积、面积利用率、剩余面积与形状
 * 构成。面积口径统一为 `PackingShape2.area`（圆形件为 `πr²`），而不是外接矩形面积。
 * / Mirrors BPP3D `ColumnGenerationPackingAnalyzer`: summarises sheet count, actually occupied area, area
 * utilisation, remaining area and shape composition. Areas use `PackingShape2.area` (πr² for circles) rather
 * than the enclosing rectangle area.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp2d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.PackingShape2Type

/**
 * 装箱快照。 / Packing snapshot.
 *
 * @property sheetCount 板材数量 / sheet count
 * @property itemCount 物品件数 / item count
 * @property actualArea 实际占用面积 / actually occupied area
 * @property boundingArea 外接矩形面积 / enclosing rectangle area
 * @property sheetArea 板材总面积 / total sheet area
 * @property remainingArea 剩余面积 / remaining area
 * @property areaUtilization 面积利用率 / area utilisation
 * @property loadingRate 平均装载率 / average loading rate
 * @property rectangleCount 矩形件数 / rectangle count
 * @property circleCount 圆形件数 / circle count
*/
data class ColumnGenerationPackingSnapshot(
    val sheetCount: Int,
    val itemCount: Int,
    val actualArea: FltX,
    val boundingArea: FltX,
    val sheetArea: FltX,
    val remainingArea: FltX,
    val areaUtilization: FltX,
    val loadingRate: FltX,
    val rectangleCount: Int,
    val circleCount: Int,
    val remnantCount: Int = 0,
    val remnantArea: FltX = FltX.zero
) {

    /**
     * 转换为字符串映射，便于写入日志与结果信息。 / Convert into a string map for logging and result info.
     *
     * @return 指标映射 / indicator map
    */
    fun toInfo(): Map<String, String> {
        return mapOf(
            "sheet_count" to sheetCount.toString(),
            "item_count" to itemCount.toString(),
            "actual_area" to actualArea.toString(),
            "bounding_area" to boundingArea.toString(),
            "sheet_area" to sheetArea.toString(),
            "remaining_area" to remainingArea.toString(),
            "area_utilization" to areaUtilization.toString(),
            "loading_rate" to loadingRate.toString(),
            "rectangle_count" to rectangleCount.toString(),
            "circle_count" to circleCount.toString(),
            "remnant_count" to remnantCount.toString(),
            "remnant_area" to remnantArea.toString()
        )
    }
}

/**
 * 装箱 KPI 分析器。 / Packing KPI analyzer.
*/
class ColumnGenerationPackingAnalyzer {

    /**
     * 分析列生成结果与装箱结果。 / Analyze the column generation result and the packing result.
     *
     * @param result 列生成结果 / column generation result
     * @param packing 装箱结果 / packing result
     * @return 装箱快照 / packing snapshot
    */
    fun analyze(
        result: ColumnGenerationResult,
        packing: PackingResult
    ): ColumnGenerationPackingSnapshot {
        val bins = packing.aggregation.bins
        var actualArea = FltX.zero
        var boundingArea = FltX.zero
        var sheetArea = FltX.zero
        var loadingRate = FltX.zero
        var itemCount = 0
        var rectangleCount = 0
        var circleCount = 0

        for (bin in bins) {
            sheetArea += bin.type.area.value
            loadingRate += bin.loadingRate
            for (item in bin.items) {
                itemCount += 1
                actualArea += item.placement.shape.area.value
                boundingArea += item.placement.shape.boundingArea.value
                when (item.placement.shape.shapeType) {
                    PackingShape2Type.Rectangle -> rectangleCount += 1
                    PackingShape2Type.Circle -> circleCount += 1
                }
            }
        }

        var remnantArea = FltX.zero
        for (remnant in packing.remnants) {
            remnantArea += remnant.area.value
        }

        val areaUtilization = if (sheetArea > FltX.zero) actualArea / sheetArea else FltX.zero
        val averageLoadingRate = if (bins.isEmpty()) FltX.zero else loadingRate / intToFltX(bins.size)
        val remainingArea = if (sheetArea > actualArea) sheetArea - actualArea else FltX.zero

        return ColumnGenerationPackingSnapshot(
            sheetCount = bins.size,
            itemCount = itemCount,
            actualArea = actualArea,
            boundingArea = boundingArea,
            sheetArea = sheetArea,
            remainingArea = remainingArea,
            areaUtilization = areaUtilization,
            loadingRate = averageLoadingRate,
            rectangleCount = rectangleCount,
            circleCount = circleCount,
            remnantCount = packing.remnants.size,
            remnantArea = remnantArea
        )
    }

    /**
     * 把整数换算为 `FltX`。 / Convert an integer into `FltX`.
     *
     * @param value 整数 / integer
     * @return FltX 数值 / FltX value
    */
    private fun intToFltX(value: Int): FltX {
        return FltX(value.toDouble())
    }
}
