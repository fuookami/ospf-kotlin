package fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto

import kotlinx.serialization.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

@Serializable
data class RendererItemDTO(
    val name: String,
    val packageType: String?,
    val warehouse: String?,
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
data class RendererBinDTO(
    val batchNo: BatchNo?,
    val order: UInt64,
    val typeCode: String?,
    val width: Flt64,
    val height: Flt64,
    val depth: Flt64,
    val loadingRate: Flt64,
    val weight: Flt64,
    val volume: Flt64,
    val items: List<RendererItemDTO>
)

@Serializable
data class RendererDTO(
    val restAmount: UInt64?,
    val bins: Map<String, RendererBinDTO>
)
