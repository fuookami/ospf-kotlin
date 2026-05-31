@file:Suppress("DEPRECATION")

/**
 * 渲染器数据传输对象。
 * Renderer data transfer objects.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.serialization.Serializable

/** 渲染真实形状类型 / Render actual shape type */
@Serializable
enum class RenderShapeTypeDTO {
    /** 长方体 / Cuboid */
    Cuboid,
    /** 圆柱 / Cylinder */
    Cylinder
}

/** 渲染三维轴向 / Render 3D axis */
@Serializable
enum class RenderAxis3DTO {
    /** X轴 / X axis */
    X,
    /** Y轴 / Y axis */
    Y,
    /** Z轴 / Z axis */
    Z
}

/** 算法侧形状类型 / Algorithm-side shape type */
@Serializable
enum class RenderAlgorithmShapeTypeDTO {
    /** 真实长方体 / Actual cuboid */
    Cuboid,
    /** 竖直圆柱 / Vertical cylinder */
    VerticalCylinder,
    /** 兼容外接长方体 / Compatibility bounding cuboid */
    BoundingCuboid
}

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
    val shapeType: RenderShapeTypeDTO = RenderShapeTypeDTO.Cuboid,
    val renderShapeType: RenderShapeTypeDTO = shapeType,
    val algorithmShapeType: RenderAlgorithmShapeTypeDTO = RenderAlgorithmShapeTypeDTO.Cuboid,
    val radius: FltX? = null,
    val diameter: FltX? = null,
    val axis: RenderAxis3DTO? = null,
    val boundingWidth: FltX? = null,
    val boundingHeight: FltX? = null,
    val boundingDepth: FltX? = null,
    val actualVolume: FltX? = null,
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


