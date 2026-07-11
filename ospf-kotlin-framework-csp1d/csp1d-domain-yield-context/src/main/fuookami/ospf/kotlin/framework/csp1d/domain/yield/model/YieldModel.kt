package fuookami.ospf.kotlin.framework.csp1d.domain.yield.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

/**
 * Demand aggregation key ensuring same-product different-unit outputs are not mixed.
 * 需求聚合键，确保相同产品不同单位的产出不混算
 *
 * @param V Numeric value type / 数值类型
 * @property productId Product identifier / 产品标识
 * @property unit Physical unit / 物理单位
*/
data class DemandAggregationKey<V : RealNumber<V>>(
    val productId: ProductId,
    val unit: PhysicalUnit
)

/**
 * Under-production shortfall for a product demand.
 * 欠产
 *
 * @param V Numeric value type / 数值类型
 * @property demand Corresponding product demand / 对应的产品需求
 * @property shortfall Shortfall quantity / 欠缺量
*/
data class UnderProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val shortfall: Quantity<V>
)

/**
 * Over-production surplus for a product demand.
 * 超产
 *
 * @param V Numeric value type / 数值类型
 * @property demand Corresponding product demand / 对应的产品需求
 * @property surplus Surplus quantity / 盈余量
*/
data class OverProduction<V : RealNumber<V>>(
    val demand: ProductDemand<V>,
    val surplus: Quantity<V>
)

/**
 * Product output summary.
 * 产品产出汇总
 *
 * @param V Numeric value type / 数值类型
 * @property product Product / 产品
 * @property totalQuantity Total output quantity / 总产出量
 * @property mode Demand mode / 需求模式
*/
data class ProductOutput<V : RealNumber<V>>(
    val product: Product<V>,
    val totalQuantity: Quantity<V>,
    val mode: DemandMode? = null
)

/**
 * Yield deviation analysis result.
 * 产出偏差分析结果
 *
 * @param V Numeric value type / 数值类型
 * @property underProductions Under-production list / 欠产列表
 * @property overProductions Over-production list / 超产列表
 * @property outputs Product output summary list / 产品产出汇总列表
*/
data class YieldAnalysis<V : RealNumber<V>>(
    val underProductions: List<UnderProduction<V>>,
    val overProductions: List<OverProduction<V>>,
    val outputs: List<ProductOutput<V>>
)
