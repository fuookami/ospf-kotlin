package fuookami.ospf.kotlin.framework.csp1d.domain.yield.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductId

/**
 * Yield modeling configuration for application layer.
 * yield 建模配置，供 application 层消费
 *
 * @param V Numeric value type / 数值类型
 * @property underProductionPenalty Under-production penalty coefficient per product+unit / 欠产惩罚系数，按 product + unit 口径
 * @property overProductionPenalty Over-production penalty coefficient per product+unit / 超产惩罚系数，按 product + unit 口径
 * @property overProductionUpperBound Over-production upper bound per product+unit, null for no limit / 超产上限，按 product + unit 口径，null 表示无上限
*/
data class YieldModelingConfig<V : RealNumber<V>>(
    val underProductionPenalty: Map<ProductDemandShadowPriceKey, V> = emptyMap(),
    val overProductionPenalty: Map<ProductDemandShadowPriceKey, V> = emptyMap(),
    val overProductionUpperBound: Map<ProductDemandShadowPriceKey, V> = emptyMap()
)

/**
 * Yield modeling result back-filled from solver solution.
 * yield 建模结果，从 solver solution 回填
 *
 * @param V Numeric value type / 数值类型
 * @property underProductions Under-production variable values / 欠产变量值
 * @property overProductions Over-production variable values / 超产变量值
*/
data class YieldModelingResult<V : RealNumber<V>>(
    val underProductions: List<ModeledUnderProduction<V>> = emptyList(),
    val overProductions: List<ModeledOverProduction<V>> = emptyList()
)

/**
 * Under-production variable result (flat modeling type, back-filled from solver solution).
 * 欠产变量结果（建模层扁平类型，从 solver solution 回填）
 *
 * Unlike [UnderProduction] in YieldModel.kt (analysis-layer rich type), this type only records solver variable values
 * and does not hold domain objects such as ProductDemand or Quantity.
 * 与 YieldModel.kt 中的 UnderProduction（分析层富类型）不同，此类型仅记录 solver 变量值，
 * 不持有 ProductDemand 或 Quantity 等领域对象。
 *
 * @param V Numeric value type / 数值类型
 * @property productId Product id / 产品 ID
 * @property unitSymbol Demand unit symbol / 需求单位符号
 * @property amount Under-production amount / 欠产量
*/
data class ModeledUnderProduction<V : RealNumber<V>>(
    val productId: ProductId,
    val unitSymbol: String,
    val amount: V
)

/**
 * Over-production variable result (flat modeling type, back-filled from solver solution).
 * 超产变量结果（建模层扁平类型，从 solver solution 回填）
 *
 * Unlike [OverProduction] in YieldModel.kt (analysis-layer rich type), this type only records solver variable values
 * and does not hold domain objects such as ProductDemand or Quantity.
 * 与 YieldModel.kt 中的 OverProduction（分析层富类型）不同，此类型仅记录 solver 变量值，
 * 不持有 ProductDemand 或 Quantity 等领域对象。
 *
 * @param V Numeric value type / 数值类型
 * @property productId Product id / 产品 ID
 * @property unitSymbol Demand unit symbol / 需求单位符号
 * @property amount Over-production amount / 超产量
*/
data class ModeledOverProduction<V : RealNumber<V>>(
    val productId: ProductId,
    val unitSymbol: String,
    val amount: V
)
