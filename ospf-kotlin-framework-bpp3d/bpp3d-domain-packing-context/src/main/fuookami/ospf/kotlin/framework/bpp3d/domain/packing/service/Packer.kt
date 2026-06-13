@file:Suppress("DEPRECATION")
/**
 * 装箱器。
 * Packer.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.LoadingOrderCalculator
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.*

/**
 * 装箱器，将最终箱子转换为装箱结果。
 * Packer, converts final bins into packing results.
 *
 * @property loadingOrderCalculator 装载顺序计算器 / loading order calculator
 */
class Packer(
    private val loadingOrderCalculator: LoadingOrderCalculator = LoadingOrderCalculator(
        maxBlockDepth = null,
        sameTypeJudger = { lhs, rhs -> lhs.pattern == rhs.pattern }
    )
) {
    private fun requireSingleCylinderAxisPerLayer(
        bin: Bin<BinLayer, FltX>,
        source: String
    ) {
        for ((layerIndex, layerPlacement) in bin.units.withIndex()) {
            val axes = layerPlacement.unit.units.mapNotNull { placement ->
                (placement.resolvedPackingShape() as? CylinderPackingShape3)?.axis
            }.toSet()
            if (axes.size > 1) {
                throw IllegalArgumentException(
                    unsupportedMixedCylinderAxesInLayerMessage(
                        source = source,
                        layerIndex = layerIndex,
                        axes = axes
                    )
                )
            }
        }
    }

    /**
     * 终态装箱分析：将 final bins 转为 PackingResult/SchemaDTO 所需结构。
     * Final packing analyzer: converts final bins into PackingResult-ready structures.
     *
     * @param bins 最终箱子列表 / final bin list
     * @param context 装箱上下文 / packing context
     * @return 装箱结果 / packing result
     */
    suspend operator fun invoke(
        bins: List<Bin<BinLayer, FltX>>,
        context: PackingContext = PackingContext()
    ): PackingResult {
        val packedBins = bins.mapIndexed { index, bin ->
            requireSingleCylinderAxisPerLayer(
                bin = bin,
                source = "Packer.invoke"
            )
            val itemPlacements = bin.dump().units
            val loadingOrders = loadingOrderCalculator(itemPlacements).toMap()
            val packedBin = PackedBin(
                name = "bin-${index + 1}",
                type = bin.type,
                batchNo = bin.batchNo,
                items = itemPlacements.map { placement ->
                    PackedItem(
                        placement = placement,
                        loadingOrder = loadingOrders[placement] ?: UInt64.zero
                    )
                }
            )
            requirePackedBinShapeGeometry(
                bin = packedBin,
                source = "Packer.invoke"
            )
            packedBin
        }

        return PackingResult(
            aggregation = PackingAggregation(packedBins),
            materialSummary = summarizeMaterials(packedBins),
            info = context.info
        )
    }

    /**
     * 汇总物料使用情况。
     * Summarize material usage.
     *
     * @param bins 已装箱列表 / packed bin list
     * @return 物料汇总列表 / material summary list
     */
    private fun summarizeMaterials(bins: List<PackedBin>): List<MaterialSummary> {
        val summary = LinkedHashMap<MaterialKey, UInt64>()
        for (bin in bins) {
            for (item in bin.items) {
                for ((material, amount) in item.placement.unit.materialAmounts) {
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
