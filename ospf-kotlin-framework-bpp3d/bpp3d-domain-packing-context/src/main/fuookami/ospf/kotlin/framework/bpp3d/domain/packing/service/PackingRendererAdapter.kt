@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.RenderLoadingPlanItemDTO
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.toFltX

class PackingRendererAdapter {
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
                    loadingOrder = packed.loadingOrder
                )
            }

            val usedVolume = itemDtos.fold(FltX.zero) { acc, item ->
                acc + (item.width * item.height * item.depth)
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
