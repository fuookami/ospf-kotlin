/**
 * 装箱器。
 * Packer.
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
    /**
     * 校验每层中圆柱体的轴向是否一致。同一层内不允许混合不同轴向的圆柱体。
     * Validate that cylinder axes are consistent within each layer. Mixed cylinder axes in the same layer are not allowed.
     *
     * @param bin 待校验的箱子 / bin to validate
     * @param source 调用来源标识 / caller source identifier
     * @return 校验结果，轴向不一致时失败 / validation result, fails when mixed axes are detected
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
