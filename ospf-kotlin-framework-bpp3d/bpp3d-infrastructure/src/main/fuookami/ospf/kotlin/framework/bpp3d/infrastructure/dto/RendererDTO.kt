package fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.math.*

@Serializable
data class RenderLoadingPlanItemDTO(
    val name: String,
    val packageType: String?,
    val width: Flt64,
    val height: Flt64,
    val depth: Flt64,
    val x: Flt64,
    val y: Flt64,
    val z: Flt64,
    val weight: Flt64,
    val loadingOrder: UInt64,
    val info: Map<String, String> = emptyMap()
)

@Serializable
data class RenderLoadingPlanDTO(
    val group: List<String>,
    val typeCode: String,
    val width: Flt64,
    val height: Flt64,
    val depth: Flt64,
    val loadingRate: Flt64,
    val weight: Flt64,
    val volume: Flt64,
    val items: List<RenderLoadingPlanItemDTO>,
    val info: Map<String, String> = emptyMap()
)

@Serializable
data class SchemaDTO(
    val kpi: Map<String, String>,
    val loadingPlans: List<RenderLoadingPlanDTO>,
)
