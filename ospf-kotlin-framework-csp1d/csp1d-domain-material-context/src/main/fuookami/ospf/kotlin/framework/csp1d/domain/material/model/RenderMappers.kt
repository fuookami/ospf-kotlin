package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.toFltX
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderCuttingPlanProductionDTO
import fuookami.ospf.kotlin.framework.csp1d.infrastructure.dto.RenderProductionType

/**
 * 将领域 Product 映射到渲染 DTO / Map domain Product to render DTO
 *
 * @param x 起始坐标 / Start coordinate
 * @param productionType 生产类型 / Production type
 * @param info 附加信息 / Additional info
 * @return 渲染 DTO / Render DTO
 */
fun <V : RealNumber<V>> Product<V>.toRenderDto(
    x: FltX,
    productionType: RenderProductionType = RenderProductionType.Product,
    info: Map<String, String> = emptyMap()
): RenderCuttingPlanProductionDTO {
    val renderWidth = maxWidth()?.toFltX()?.value ?: FltX.zero
    val renderUnitLength = length?.toFltX()?.value
    return RenderCuttingPlanProductionDTO(
        name = name,
        x = x,
        width = renderWidth,
        unitLength = renderUnitLength,
        productionType = productionType,
        info = info
    )
}
