/**
 * 余料分析。 / Remnant analysis.
 *
 * 与 BPP3D 在装箱终态输出几何信息对应：二维排样在终态需要知道每块板材上还剩哪些可用矩形区域，供余料入库
 * 或后续工单复用。本分析器按「水平切片」把已用区域从板材中扣掉，输出互不重叠的自由矩形。
 * / Mirrors the geometry output of BPP3D at the final packing stage: 2D packing needs to know which rectangular
 * regions remain usable on each sheet so remnants can be stocked or reused by later jobs. This analyser subtracts
 * the occupied regions from the sheet by horizontal slices and reports pairwise disjoint free rectangles.
 *
 * 说明：这是**余料报告**，不是余料库存。跨工单的库存、优先级与库位管理属于仓储语义，不在本框架内。
 * / Note: this is **remnant reporting**, not remnant inventory. Cross-job stock, priority and warehouse location
 * management are inventory semantics and stay outside this framework.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

/**
 * 板材上的一块剩余矩形。 / A remaining rectangle on a sheet.
 *
 * @property sheetName 板材名称 / sheet name
 * @property rectangle 自由矩形 / free rectangle
 * @property area 自由矩形面积 / free rectangle area
*/
data class Remnant2(
    val sheetName: String,
    val rectangle: Box2Geometry<FltX>,
    val area: Quantity<FltX>
)

/**
 * 余料分析器。 / Remnant analyser.
*/
class FreeSpaceAnalyzer {

    /**
     * 分析一组板材的余料。 / Analyse the remnants of a set of sheets.
     *
     * @param sheets 已装箱板材 / packed sheets
     * @return 每块板材的余料矩形列表 / remnant rectangles per sheet
    */
    fun analyze(sheets: List<Sheet<FltX>>): List<Remnant2> {
        val result = ArrayList<Remnant2>()
        for ((index, sheet) in sheets.withIndex()) {
            val name = "sheet-${index + 1}"
            result.addAll(analyzeSheet(name = name, sheet = sheet))
        }
        return result
    }

    /**
     * 分析单块板材的余料。 / Analyse the remnants of a single sheet.
     *
     * @param name 板材名称 / sheet name
     * @param sheet 已装箱板材 / packed sheet
     * @return 余料矩形列表 / remnant rectangle list
    */
    fun analyzeSheet(name: String, sheet: Sheet<FltX>): List<Remnant2> {
        val width = sheet.type.width
        val height = sheet.type.height
        val zero = quantityZeroOf(width)

        // 收集全部放置的包围盒（含圆形件的外接正方形，保守但安全）。
        // Collect every placement's bounding box (circle items use their enclosing square, conservative but safe).
        val occupied = ArrayList<Box2Geometry<FltX>>()
        for (sheetLayer in sheet.layers) {
            for (layer in sheetLayer.layers) {
                for (unit in layer.units) {
                    occupied.add(unit.box())
                }
            }
        }
        if (occupied.isEmpty()) {
            val whole = Box2Geometry.of(x = zero, y = zero, width = width, height = height)
            return listOf(
                Remnant2(
                    sheetName = name,
                    rectangle = whole,
                    area = width * height
                )
            )
        }

        // 以所有 y 边界切分水平带，在每条带上求出未被占用的 x 区间。
        // Slice horizontally at every y boundary, then compute the unoccupied x intervals in each slice.
        val boundaries = LinkedHashSet<FltX>()
        boundaries.add(zero.value)
        boundaries.add(height.value)
        for (box in occupied) {
            boundaries.add(box.y.value)
            val maxY = (box.maxY() as? fuookami.ospf.kotlin.utils.functional.Ok)?.value
            if (maxY != null) {
                boundaries.add(maxY.value)
            }
        }
        val sortedBoundaries = boundaries.filter { it >= zero.value && it <= height.value }.sortedBy { it }

        val remnants = ArrayList<Remnant2>()
        for (index in 0 until sortedBoundaries.size - 1) {
            val lower = sortedBoundaries[index]
            val upper = sortedBoundaries[index + 1]
            if (upper <= lower) {
                continue
            }
            val sliceHeight = upper - lower
            if (sliceHeight <= zero.value) {
                continue
            }

            // 该带内被占用的 x 区间。 / Occupied x intervals within this slice.
            val blocked = ArrayList<Pair<FltX, FltX>>()
            for (box in occupied) {
                val boxMaxY = (box.maxY() as? fuookami.ospf.kotlin.utils.functional.Ok)?.value?.value ?: continue
                // 与带相交即计入。 / Count it when it intersects the slice.
                if (box.y.value >= upper || boxMaxY <= lower) {
                    continue
                }
                val boxMaxX = (box.maxX() as? fuookami.ospf.kotlin.utils.functional.Ok)?.value?.value ?: continue
                blocked.add(box.x.value to boxMaxX)
            }
            blocked.sortBy { it.first }

            var cursor = zero.value
            for ((blockStart, blockEnd) in blocked) {
                if (blockStart > cursor) {
                    addRemnant(
                        target = remnants,
                        name = name,
                        width = width,
                        startX = cursor,
                        endX = blockStart,
                        startY = lower,
                        endY = upper
                    )
                }
                if (blockEnd > cursor) {
                    cursor = blockEnd
                }
            }
            if (cursor < width.value) {
                addRemnant(
                    target = remnants,
                    name = name,
                    width = width,
                    startX = cursor,
                    endX = width.value,
                    startY = lower,
                    endY = upper
                )
            }
        }

        // 丢弃退化矩形，并按面积降序输出，便于下游优先使用大块余料。
        // Drop degenerate rectangles and sort by area descending so downstream code reuses the largest first.
        return remnants
            .filter { quantityIsPositive(it.area) }
            .sortedByDescendingQuantity { it.area }
    }

    /**
     * 追加一块余料矩形。 / Append one remnant rectangle.
     *
     * @param target 目标列表 / target list
     * @param name 板材名称 / sheet name
     * @param width 板材宽度单位样本 / sheet width, used as the unit sample
     * @param startX 左边界 / left edge
     * @param endX 右边界 / right edge
     * @param startY 下边界 / bottom edge
     * @param endY 上边界 / top edge
    */
    private fun addRemnant(
        target: MutableList<Remnant2>,
        name: String,
        width: Quantity<FltX>,
        startX: FltX,
        endX: FltX,
        startY: FltX,
        endY: FltX
    ) {
        if (endX <= startX || endY <= startY) {
            return
        }
        val rectangle = Box2Geometry.of(
            x = Quantity(startX, width.unit),
            y = Quantity(startY, width.unit),
            width = Quantity(endX - startX, width.unit),
            height = Quantity(endY - startY, width.unit)
        )
        target.add(
            Remnant2(
                sheetName = name,
                rectangle = rectangle,
                area = rectangle.width * rectangle.height
            )
        )
    }
}
