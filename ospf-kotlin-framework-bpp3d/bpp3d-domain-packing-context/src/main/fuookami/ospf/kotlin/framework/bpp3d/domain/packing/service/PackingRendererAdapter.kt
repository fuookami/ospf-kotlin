@file:Suppress("DEPRECATION")

/**
 * 装箱渲染适配器。
 * Packing renderer adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderAlgorithmShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanItemDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.toFltX

/**
 * 装箱渲染适配器，将装箱结果转换为渲染 DTO。
 * Packing renderer adapter, converts packing results to rendering DTOs.
 */
class PackingRendererAdapter {
    /**
     * 将装箱结果转换为渲染方案 DTO。
     * Convert packing result to rendering schema DTO.
     *
     * @param result 装箱结果 / packing result
     * @return 渲染方案 DTO / rendering schema DTO
     */
    fun toSchema(result: PackingResult): SchemaDTO {
        val loadingPlans = result.aggregation.bins.map { bin ->
            val itemDtos = bin.items.map { packed ->
                val placement = packed.placement
                RenderLoadingPlanItemDTO(
                    name = placement.unit.toString(),
                    packageType = placement.unit.packageType.name,
                    width = placement.width.value.toFltX(),
                    height = placement.height.value.toFltX(),
                    depth = placement.depth.value.toFltX(),
                    x = placement.absoluteX.value.toFltX(),
                    y = placement.absoluteY.value.toFltX(),
                    z = placement.absoluteZ.value.toFltX(),
                    weight = placement.weight.value.toFltX(),
                    loadingOrder = packed.loadingOrder,
                    shapeType = RenderShapeTypeDTO.Cuboid,
                    renderShapeType = RenderShapeTypeDTO.Cuboid,
                    algorithmShapeType = RenderAlgorithmShapeTypeDTO.Cuboid,
                    boundingWidth = placement.width.value.toFltX(),
                    boundingHeight = placement.height.value.toFltX(),
                    boundingDepth = placement.depth.value.toFltX(),
                    actualVolume = placement.view.actualVolume.value.toFltX()
                )
            }

            val usedVolume = itemDtos.fold(FltX.zero) { acc, item ->
                acc + (item.actualVolume ?: (item.width * item.height * item.depth))
            }
            val totalVolume = bin.type.width.value.toFltX() * bin.type.height.value.toFltX() * bin.type.depth.value.toFltX()
            val loadingRate = if (totalVolume == FltX.zero) {
                FltX.zero
            } else {
                usedVolume / totalVolume
            }

            RenderLoadingPlanDTO(
                group = bin.group,
                name = bin.name,
                typeCode = bin.type.typeCode,
                width = bin.type.width.value.toFltX(),
                height = bin.type.height.value.toFltX(),
                depth = bin.type.depth.value.toFltX(),
                loadingRate = loadingRate,
                weight = itemDtos.fold(FltX.zero) { acc, item -> acc + item.weight },
                volume = usedVolume,
                items = itemDtos
            )
        }

        val kpi = linkedMapOf<String, String>().apply {
            this["bin_count"] = loadingPlans.size.toString()
            this["material_count"] = result.materialSummary.size.toString()
            if (result.info.isNotEmpty()) {
                putAll(result.info)
            }
        }

        return SchemaDTO(
            kpi = kpi,
            loadingPlans = loadingPlans
        )
    }
}
