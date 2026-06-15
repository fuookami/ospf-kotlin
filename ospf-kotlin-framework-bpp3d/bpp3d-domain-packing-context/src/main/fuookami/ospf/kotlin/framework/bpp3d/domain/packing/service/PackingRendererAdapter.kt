/**
 * 装箱渲染适配器。
 * Packing renderer adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.PI
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.PackingResult

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
            PackingAlgorithmShapeType.HorizontalCylinderX -> RenderAlgorithmShapeTypeDTO.HorizontalCylinderX
            PackingAlgorithmShapeType.HorizontalCylinderZ -> RenderAlgorithmShapeTypeDTO.HorizontalCylinderZ
        }
    }

    private fun Axis3.toRenderAxis3(): RenderAxis3DTO {
        return when (this) {
            Axis3.X -> RenderAxis3DTO.X
            Axis3.Y -> RenderAxis3DTO.Y
            Axis3.Z -> RenderAxis3DTO.Z
        }
    }

    private fun solverRadiusSelectionResult(
        item: Item,
        solverRadiusByVariableName: Map<String, CylinderRadiusSelectionResult>,
        solverRadiusByUniqueKey: Map<String, CylinderRadiusSelectionResult>
    ): CylinderRadiusSelectionResult? {
        val spec = item.packageShape.shapeSpec as? PackageShapeSpec.VerticalCylinder ?: return null
        val key = spec.radiusWeightFunctionKey ?: return null
        val prototype = spec.continuousRadiusSolverPrototype(
            source = continuousCylinderRadiusSolverSource(item)
        )
        prototype?.variableName?.let { variableName ->
            solverRadiusByVariableName[variableName]?.let {
                return it
            }
        }
        return solverRadiusByUniqueKey[key]
    }

    /**
     * 将装箱结果转换为渲染方案 DTO。
     * Convert packing result to rendering schema DTO.
     *
     * @param result 装箱结果 / packing result
     * @return 渲染方案 DTO / rendering schema DTO
     */
    fun toSchema(result: PackingResult): SchemaDTO {
        return toSchema(result, emptyList())
    }

    /**
     * 将装箱结果转换为渲染方案 DTO，支持 solver 选出半径回写。
     * Convert packing result to rendering schema DTO with solver-selected radius writeback.
     *
     * @param result 装箱结果 / packing result
     * @param continuousRadiusSelectionResults 连续半径已选择结果列表 / continuous-radius selection results
     * @return 渲染方案 DTO / rendering schema DTO
     */
    fun toSchema(
        result: PackingResult,
        continuousRadiusSelectionResults: List<CylinderRadiusSelectionResult>
    ): SchemaDTO {
        val solverRadiusByVariableName = continuousRadiusSelectionResults.mapNotNull { result ->
            result.variableName?.let { variableName -> variableName to result }
        }.toMap()
        val solverRadiusByUniqueKey = continuousRadiusSelectionResults
            .groupBy { it.key }
            .filterValues { it.size == 1 }
            .mapValues { (_, results) -> results.single() }
        val loadingPlans = result.aggregation.bins.map { bin ->
            requirePackedBinShapeGeometry(
                bin = bin,
                source = "PackingRendererAdapter.toSchema"
            )
            val itemDtos = bin.items.map { packed ->
                val placement = packed.placement
                val shape = placement.resolvedPackingShape()
                val renderShapeType = shape.shapeType.toRenderShapeType()
                val cylinderShape = shape as? CylinderPackingShape3
                val itemUnit = placement.unit
                val solverResult = (itemUnit as? Item)?.let { item ->
                    solverRadiusSelectionResult(
                        item = item,
                        solverRadiusByVariableName = solverRadiusByVariableName,
                        solverRadiusByUniqueKey = solverRadiusByUniqueKey
                    )
                }

                // 使用 solver 选出半径计算真实体积；PWL 路径使用 actualRadiusSquared 消除近似误差。
                // Use solver-selected radius for actual volume; PWL path uses actualRadiusSquared to eliminate approximation error.
                val actualVolume: FltX = if (cylinderShape != null && solverResult != null) {
                    val pwlMetadata = solverResult.pwlMetadata
                    if (pwlMetadata != null) {
                        // PWL 路径：使用 actualRadiusSquared (r²) 计算真实体积，而非 solverRadiusSquared (q ≈ r²)。
                        // PWL path: use actualRadiusSquared (r²) for true volume, not solverRadiusSquared (q ≈ r²).
                        pwlMetadata.actualVolume(
                            height = cylinderShape.cylinder.height.value,
                            pi = FltX(PI)
                        ).toFltX()
                    } else {
                        // 非 PWL 路径：直接使用 solver 选出的半径。
                        // Non-PWL path: use solver-selected radius directly.
                        val solverRadius = solverResult.selectedRadius
                        (FltX(PI) * solverRadius * solverRadius * cylinderShape.cylinder.height).value.toFltX()
                    }
                } else {
                    shape.actualVolume.value.toFltX()
                }

                val radius: FltX? = if (cylinderShape != null && solverResult != null) {
                    solverResult.selectedRadius.value.toFltX()
                } else {
                    cylinderShape?.radius?.value?.toFltX()
                }
                val diameter: FltX? = if (cylinderShape != null && solverResult != null) {
                    (solverResult.selectedRadius * FltX(2.0)).value.toFltX()
                } else {
                    cylinderShape?.diameter?.value?.toFltX()
                }

                // 构建 PWL 诊断信息（仅 PWL 路径时附加）。
                // Build PWL diagnostic info (attached only for PWL path).
                val pwlInfo: Map<String, String> = if (cylinderShape != null && solverResult?.pwlMetadata != null) {
                    val pwl = solverResult.pwlMetadata!!
                    mapOf(
                        "pwl_volume" to pwl.pwlVolume(
                            height = cylinderShape.cylinder.height.value,
                            pi = FltX(PI)
                        ).toDouble().toString(),
                        "pwl_absolute_error" to pwl.pwlAbsoluteError.toDouble().toString(),
                        "pwl_relative_error" to pwl.pwlRelativeError.toDouble().toString(),
                        "pwl_max_relative_error" to pwl.maxPWLRelativeError.toDouble().toString(),
                        "pwl_num_segments" to pwl.numSegments.toString(),
                        "pwl_within_envelope" to pwl.isWithinEnvelope.toString(),
                        "pwl_selection_source" to pwl.selectionSource
                    )
                } else {
                    emptyMap()
                }

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
                    radius = radius,
                    diameter = diameter,
                    axis = shape.axis?.toRenderAxis3(),
                    boundingWidth = shape.boundingWidth.value.toFltX(),
                    boundingHeight = shape.boundingHeight.value.toFltX(),
                    boundingDepth = shape.boundingDepth.value.toFltX(),
                    actualVolume = actualVolume,
                    info = pwlInfo
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
