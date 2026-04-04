package fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.serialization.Serializable

@Serializable
data class RenderLoadingPlanItemDTO(
    val name: String,
    val packageType: String? = null,
    val width: FltX,
    val height: FltX,
    val depth: FltX,
    val x: FltX,
    val y: FltX,
    val z: FltX,
    val weight: FltX,
    val loadingOrder: UInt64,
    val info: Map<String, String> = emptyMap()
)

@Serializable
data class RenderLoadingPlanDTO(
    val group: List<String> = emptyList(),
    val name: String,
    val typeCode: String,
    val width: FltX,
    val height: FltX,
    val depth: FltX,
    val loadingRate: FltX,
    val weight: FltX,
    val volume: FltX,
    val items: List<RenderLoadingPlanItemDTO> = emptyList(),
    val info: Map<String, String> = emptyMap()
)

@Serializable
data class SchemaDTO(
    val kpi: Map<String, String> = emptyMap(),
    val loadingPlans: List<RenderLoadingPlanDTO> = emptyList(),
)



