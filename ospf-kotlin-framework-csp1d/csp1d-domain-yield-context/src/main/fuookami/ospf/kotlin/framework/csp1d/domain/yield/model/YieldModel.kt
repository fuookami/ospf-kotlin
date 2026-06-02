package fuookami.ospf.kotlin.framework.csp1d.domain.yield.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlanDemandContribution
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.DemandMode
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 产品产出贡献，记录某产品/配规在切割方案集合中的累计产出 / Product output contribution recording cumulative output for a product or costar
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property totalQuantity 累计产出量 / Cumulative output quantity
 * @property mode 需求口径标签 / Demand mode label
 */
data class ProductOutput<V : RealNumber<V>>(
    val product: Product<V>,
    val totalQuantity: Quantity<V>,
    val mode: DemandMode? = null
)

/**
 * 欠产量，产出不足需求的部分 / Under-production quantity: output shortfall relative to demand
 *
 * @param V 数值类型 / Numeric value type
 * @property demand 原始需求 / Original demand
 * @property shortfall 欠缺量 / Shortfall quantity
 */
data class UnderProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val shortfall: Quantity<V>
)

/**
 * 超产量，产出超出需求的部分 / Over-production quantity: output surplus relative to demand
 *
 * @param V 数值类型 / Numeric value type
 * @property demand 原始需求 / Original demand
 * @property surplus 超产量 / Surplus quantity
 */
data class OverProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val surplus: Quantity<V>
)

/**
 * 产出偏差分析，按统一 ProductDemand 口径统计欠产与超产 / Yield deviation analysis: under/over production per unified ProductDemand
 *
 * @param V 数值类型 / Numeric value type
 * @property underProductions 欠产列表 / Under-production list
 * @property overProductions 超产列表 / Over-production list
 * @property outputs 按产品聚合的产出贡献 / Product-aggregated output contributions
 */
data class YieldAnalysis<V : RealNumber<V>>(
    val underProductions: List<UnderProduction<V>>,
    val overProductions: List<OverProduction<V>>,
    val outputs: List<ProductOutput<V>>
)
