/**
 * 尾板优化器。 / Tail sheet optimiser.
 *
 * 列生成保证「覆盖需求」，但最后一板（尾板）通常是整块板材排布，会超出需求量。此前的裁剪是「先放先得」，
 * 尾板用哪一列完全取决于 MILP 的选列结果。本优化器在裁剪阶段重新选择尾板用列：对剩余需求枚举全部候选列，
 * 优先选能覆盖更多剩余需求的列，其次选超出量更小的列，从而减少尾板的废弃件数。
 * / Column generation guarantees demand coverage, but the last sheet (tail sheet) is a whole-sheet layout and
 * normally exceeds the demand. The previous trimming was first-come-first-served, so the tail sheet kept whatever
 * column the MILP selected. This optimiser re-selects the tail column during trimming: it enumerates every
 * candidate column against the residual demand, preferring the one covering more residual demand and then the one
 * with the smaller overshoot, which reduces discarded pieces on the tail sheet.
*/
package fuookami.ospf.kotlin.framework.bpp2d.application.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.LayerPlacement2
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Layer
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.Sheet
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.SheetLayer

/**
 * 尾板优化器。 / Tail sheet optimiser.
 *
 * @property candidates 可用于尾板的候选列 / candidate columns usable for the tail sheet
*/
class TailSheetOptimizer(
    private val candidates: List<Layer<FltX>> = emptyList()
) {

    /**
     * 一个候选列对剩余需求的评估。 / Evaluation of one candidate column against the residual demand.
     *
     * @property layer 候选列 / candidate column
     * @property covered 可覆盖的剩余需求件数 / residual demand pieces covered
     * @property wasted 超出需求的件数 / pieces exceeding the demand
    */
    private data class Evaluation(
        val layer: Layer<FltX>,
        val covered: Int,
        val wasted: Int
    )

    /**
     * 按需求裁剪板材，并优化尾板选列。 / Trim the sheets to the demand and optimise the tail column.
     *
     * @param sheets 列生成产出的板材 / sheets produced by column generation
     * @param demands 物品编号到需求件数 / item identifier to demanded amount
     * @return 裁剪后的板材 / trimmed sheets
    */
    fun optimize(
        sheets: List<Sheet<FltX>>,
        demands: Map<String, ULong>
    ): List<Sheet<FltX>> {
        if (sheets.isEmpty()) {
            return emptyList()
        }
        val remaining = LinkedHashMap<String, ULong>()
        for ((itemId, amount) in demands) {
            if (amount > 0uL) {
                remaining[itemId] = amount
            }
        }
        if (remaining.isEmpty()) {
            return emptyList()
        }

        val result = ArrayList<Sheet<FltX>>(sheets.size)
        for (sheet in sheets) {
            if (remaining.values.all { it == 0uL }) {
                break
            }
            val original = sheet.layers.firstOrNull()?.layers?.firstOrNull()
            val best = chooseColumn(original = original, remaining = remaining) ?: continue
            if (best.covered == 0) {
                continue
            }

            val trimmedLayer = trimLayer(best.layer, remaining)
            if (trimmedLayer.units.isEmpty()) {
                continue
            }
            val template = sheet.layers.first()
            result.add(
                Sheet(
                    type = sheet.type,
                    layers = listOf(
                        SheetLayer(
                            type = template.type,
                            shape = trimmedLayer.shape,
                            layers = listOf(trimmedLayer),
                            batchNo = template.batchNo
                        )
                    ),
                    batchNo = sheet.batchNo
                )
            )
            for ((itemId, amount) in trimmedLayer.itemAmounts) {
                val used = amount.toULong()
                val left = remaining[itemId.value] ?: 0uL
                remaining[itemId.value] = if (left <= used) 0uL else left - used
            }
        }
        return result
    }

    /**
     * 在候选中选出对剩余需求最优的列。 / Pick the column that fits the residual demand best.
     *
     * 排序规则：先最大化覆盖件数，再最小化超出件数，最后优先使用原列以避免无谓换列。
     * / Ordering: maximise covered pieces, then minimise overshoot, and finally prefer the original column to avoid
     * pointless swapping.
     *
     * @param original 原列，可能为 null / original column, may be null
     * @param remaining 剩余需求 / residual demand
     * @return 最优列评估，无候选时返回 null / evaluation of the best column, or null when there is no candidate
    */
    private fun chooseColumn(
        original: Layer<FltX>?,
        remaining: Map<String, ULong>
    ): Evaluation? {
        val pool = LinkedHashSet<Layer<FltX>>()
        if (original != null) {
            pool.add(original)
        }
        pool.addAll(candidates)
        if (pool.isEmpty()) {
            return null
        }

        val evaluations = pool.map { evaluate(it, remaining) }
        return evaluations.sortedWith(
            compareByDescending<Evaluation> { it.covered }
                .thenBy { it.wasted }
                .thenBy { if (it.layer == original) 0 else 1 }
        ).firstOrNull()
    }

    /**
     * 评估候选列对剩余需求的覆盖与超出。 / Evaluate how a candidate column covers and exceeds the residual demand.
     *
     * @param layer 候选列 / candidate column
     * @param remaining 剩余需求 / residual demand
     * @return 评估结果 / evaluation
    */
    private fun evaluate(layer: Layer<FltX>, remaining: Map<String, ULong>): Evaluation {
        var covered = 0
        var wasted = 0
        for ((itemId, amount) in layer.itemAmounts) {
            val produced = amount.toULong().toInt()
            val demand = (remaining[itemId.value] ?: 0uL).toInt()
            covered += minOf(produced, demand)
            wasted += if (produced > demand) produced - demand else 0
        }
        return Evaluation(layer = layer, covered = covered, wasted = wasted)
    }

    /**
     * 按剩余需求裁剪一层的放置。 / Trim the placements of a layer to the residual demand.
     *
     * @param layer 候选列 / candidate column
     * @param remaining 剩余需求 / residual demand
     * @return 裁剪后的层 / trimmed layer
    */
    private fun trimLayer(layer: Layer<FltX>, remaining: Map<String, ULong>): Layer<FltX> {
        val budget = LinkedHashMap<String, Int>()
        for ((itemId, amount) in remaining) {
            budget[itemId] = amount.toInt()
        }
        val units = ArrayList<LayerPlacement2<FltX>>(layer.units.size)
        for (unit in layer.units) {
            val itemId = unit.itemId ?: continue
            val left = budget[itemId] ?: 0
            if (left <= 0) {
                continue
            }
            budget[itemId] = left - 1
            units.add(unit)
        }
        return Layer(
            iteration = layer.iteration,
            from = layer.from,
            sheet = layer.sheet,
            shape = layer.shape,
            units = units
        )
    }
}
