/**
 * Packer.
 * 装箱器。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.LoadingOrderCalculator
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Packer, converts final bins into packing results.
 * 装箱器，将最终箱子转换为装箱结果。
 *
 * @property loadingOrderCalculator The loading order calculator.
 * 装载顺序计算器。
*/
class Packer(
    private val loadingOrderCalculator: LoadingOrderCalculator = LoadingOrderCalculator(
        maxBlockDepth = null,
        sameTypeJudger = { lhs, rhs -> lhs.pattern == rhs.pattern }
    )
) {

    /**
     * Validate that cylinder axes are consistent within each layer. Mixed cylinder axes in the same layer are not allowed.
     * 校验每层中圆柱体的轴向是否一致。同一层内不允许混合不同轴向的圆柱体。
     *
     * @param bin The bin to validate.
     * 待校验的箱子。
     * @param source The caller source identifier.
     * 调用来源标识。
     * @return The validation result, fails when mixed axes are detected.
     * 校验结果，轴向不一致时失败。
    */
    private fun requireSingleCylinderAxisPerLayer(
        bin: Bin<BinLayer, FltX>,
        source: String
    ): Try {
        for ((layerIndex, layerPlacement) in bin.units.withIndex()) {
            val axes = layerPlacement.unit.units.mapNotNull { placement ->
                (placement.resolvedPackingShape() as? CylinderPackingShape3)?.axis
            }.toSet()
            if (axes.size > 1) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    unsupportedMixedCylinderAxesInLayerMessage(
                        source = source,
                        layerIndex = layerIndex,
                        axes = axes
                    )
                )
            }
        }
        return ok
    }

    /**
     * Final packing analyzer: converts final bins into PackingResult-ready structures.
     * 终态装箱分析：将 final bins 转为 PackingResult/SchemaDTO 所需结构。
     *
     * @param bins The final bin list.
     * 最终箱子列表。
     * @param context The packing context.
     * 装箱上下文。
     * @return The packing result.
     * 装箱结果。
    */
    suspend operator fun invoke(
        bins: List<Bin<BinLayer, FltX>>,
        context: PackingContext = PackingContext()
    ): Ret<PackingResult> {
        val packedBins = bins.mapIndexed { index, bin ->
            when (val result = requireSingleCylinderAxisPerLayer(
                bin = bin,
                source = "Packer.invoke"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
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
            when (val result = requirePackedBinShapeGeometry(
                bin = packedBin,
                source = "Packer.invoke"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            packedBin
        }

        return Ok(PackingResult(
            aggregation = PackingAggregation(packedBins),
            materialSummary = summarizeMaterials(packedBins),
            info = context.info
        ))
    }

    /**
     * Summarize material usage.
     * 汇总物料使用情况。
     *
     * @param bins The packed bin list.
     * 已装箱列表。
     * @return The material summary list.
     * 物料汇总列表。
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
