package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

/**
 * Stowage solution containing the final item-to-position assignments and
 * predicate/recommended load weights for each position.
 * 配载方案，包含最终的货物-舱位分配以及每个位置的谓词/推荐装载重量。
 *
 * @property stowage 从舱位到其分配货物的映射 / the mapping from positions to their assigned items
 * @property predicateLoadWeight 每个位置的谓词装载重量 / the predicate load weight per position
 * @property recommendedLoadWeight 每个位置的推荐装载重量 / the recommended load weight per position
*/
class Solution(
    val stowage: Map<Position, List<Item>>,
    val predicateLoadWeight: Map<Position, Quantity<Flt64>>,
    val recommendedLoadWeight: Map<Position, Quantity<Flt64>>
)

/**
 * Renders the stowage solution into a response DTO.
 * 将配载方案渲染为响应 DTO。
 *
 * @return 渲染后的响应 DTO / the rendered response DTO
*/
fun Solution.render(): RenderDTO {
    val positions = (stowage.keys + predicateLoadWeight.keys + recommendedLoadWeight.keys)
        .distinct()
        .sortedWith(
            compareBy<Position>(
                { it.loadingOrder.location.ordinal },
                { it.loadingOrder.order.toString().toInt() },
                { it.id.toString() }
            )
        )

    return RenderDTO(
        positions = positions.map { position ->
            RenderPositionDTO(
                positionId = position.id.toString(),
                positionName = position.spaceName,
                itemIds = stowage[position].orEmpty().map { it.id }.sorted(),
                predicateLoadWeight = predicateLoadWeight[position]?.toRenderWeight(),
                recommendedLoadWeight = recommendedLoadWeight[position]?.toRenderWeight()
            )
        }
    )
}

private fun Quantity<Flt64>.toRenderWeight(): RenderWeightDTO {
    return RenderWeightDTO(
        value = value.toDouble(),
        unit = unit.symbol ?: unit.name
    )
}
