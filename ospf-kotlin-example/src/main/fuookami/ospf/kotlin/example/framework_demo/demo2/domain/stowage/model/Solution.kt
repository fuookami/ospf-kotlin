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
 * @property stowage the mapping from positions to their assigned items / 从舱位到其分配货物的映射
 * @property predicateLoadWeight the predicate load weight per position / 每个位置的谓词装载重量
 * @property recommendedLoadWeight the recommended load weight per position / 每个位置的推荐装载重量
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
 * @return the rendered response DTO / 渲染后的响应 DTO
*/
fun Solution.render(): RenderDTO {
    TODO("not implemented yet")
}
