@file:Suppress("DEPRECATION")

/**
 * 装箱渲染适配器。
 * Packing renderer adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingAlgorithmShapeType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackingShapeType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderAlgorithmShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderAxis3DTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanItemDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderShapeTypeDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.toFltX
import fuookami.ospf.kotlin.math.geometry.Axis3

/**
 * 装箱渲染适配器，将装箱结果转换为渲染 DTO。
 * Packing renderer adapter, converts packing results to rendering DTOs.
 */
class PackingRendererAdapter {
    private fun PackingShapeType.toRenderShapeType(): RenderShapeTypeDTO {
        return when (this) {
            PackingShapeType.Cuboid -> RenderShapeTypeDTO.Cuboid
            PackingShapeType.Cylinder -> RenderShapeTypeDTO.Cylinder
        }
    }

    private fun PackingAlgorithmShapeType.toRenderAlgorithmShapeType(): RenderAlgorithmShapeTypeDTO {
        return when (this) {
            PackingAlgorithmShapeType.Cuboid -> RenderAlgorithmShapeTypeDTO.Cuboid
            PackingAlgorithmShapeType.VerticalCylinder -> RenderAlgorithmShapeTypeDTO.VerticalCylinder
            PackingAlgorithmShapeType.BoundingCuboid -> RenderAlgorithmShapeTypeDTO.BoundingCuboid
        }
    }

    private fun Axis3.toRenderAxis3(): RenderAxis3DTO {
        return when (this) {
            Axis3.X -> RenderAxis3DTO.X
            Axis3.Y -> RenderAxis3DTO.Y
            Axis3.Z -> RenderAxis3DTO.Z
        }
    }

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
                val shape = placement.unit.packingShape
                requireVerticalCylinderAxis(
                    shape = shape,
                    source = "PackingRendererAdapter.toSchema"
                )
                val renderShapeType = shape.shapeType.toRenderShapeType()
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
                    shapeType = renderShapeType,
                    renderShapeType = renderShapeType,
                    algorithmShapeType = shape.algorithmShapeType.toRenderAlgorithmShapeType(),
                    radius = (shape as? CylinderPackingShape3)?.radius?.value?.toFltX(),
                    diameter = (shape as? CylinderPackingShape3)?.diameter?.value?.toFltX(),
                    axis = shape.axis?.toRenderAxis3(),
                    boundingWidth = shape.boundingWidth.value.toFltX(),
                    boundingHeight = shape.boundingHeight.value.toFltX(),
                    boundingDepth = shape.boundingDepth.value.toFltX(),
                    actualVolume = shape.actualVolume.value.toFltX()
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
