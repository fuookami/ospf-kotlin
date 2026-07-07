package fuookami.ospf.kotlin.framework.csp1d.domain.yield.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * 需求聚合键，确保相同产品不同单位的产出不混算 / Demand aggregation key ensuring same-product different-unit outputs are not mixed
 *
 * @param V 数值类型 / Numeric value type
 * @property productId 产品标识 / Product identifier
 * @property unit 物理单位 / Physical unit
 */
data class DemandAggregationKey<V : RealNumber<V>>(
    val productId: ProductId,
    val unit: PhysicalUnit
)

/**
 * 欠产 / Under-production
 *
 * @param V 数值类型 / Numeric value type
 * @property demand 对应的产品需求 / Corresponding product demand
 * @property shortfall 欠缺量 / Shortfall quantity
 */
data class UnderProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val shortfall: Quantity<V>
)

/**
 * 超产 / Over-production
 *
 * @param V 数值类型 / Numeric value type
 * @property demand 对应的产品需求 / Corresponding product demand
 * @property surplus 盈余量 / Surplus quantity
 */
data class OverProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val surplus: Quantity<V>
)

/**
 * 产品产出汇总 / Product output summary
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property totalQuantity 总产出量 / Total output quantity
 * @property mode 需求模式 / Demand mode
 */
data class ProductOutput<V : RealNumber<V>>(
    val product: Product<V>,
    val totalQuantity: Quantity<V>,
    val mode: DemandMode? = null
)

/**
 * 产出偏差分析结果 / Yield deviation analysis result
 */
data class YieldAnalysis<V : RealNumber<V>>(
    val underProductions: List<UnderProduction<V>>,
    val overProductions: List<OverProduction<V>>,
    val outputs: List<ProductOutput<V>>
)