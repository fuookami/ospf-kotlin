package fuookami.ospf.kotlin.framework.csp1d.domain.yield.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemandShadowPriceKey

/**
 * yield 建模配置，供 application 层消费 / Yield modeling configuration for application layer
 *
 * @param V 数值类型 / Numeric value type
 * @property underProductionPenalty 欠产惩罚系数，按 product + unit 口径 / Under-production penalty coefficient per product+unit
 * @property overProductionPenalty 超产惩罚系数，按 product + unit 口径 / Over-production penalty coefficient per product+unit
 * @property overProductionUpperBound 超产上限，按 product + unit 口径，null 表示无上限 / Over-production upper bound per product+unit, null for no limit
 */
data class YieldModelingConfig<V : RealNumber<V>>(
    val underProductionPenalty: Map<ProductDemandShadowPriceKey, V> = emptyMap(),
    val overProductionPenalty: Map<ProductDemandShadowPriceKey, V> = emptyMap(),
    val overProductionUpperBound: Map<ProductDemandShadowPriceKey, V> = emptyMap()
)

/**
 * yield 建模结果，从 solver solution 回填 / Yield modeling result back-filled from solver solution
 *
 * @param V 数值类型 / Numeric value type
 * @property underProductions 欠产变量值 / Under-production variable values
 * @property overProductions 超产变量值 / Over-production variable values
 */
data class YieldModelingResult<V : RealNumber<V>>(
    val underProductions: List<ModeledUnderProduction<V>> = emptyList(),
    val overProductions: List<ModeledOverProduction<V>> = emptyList()
)

/**
 * 欠产变量结果（建模层扁平类型，从 solver solution 回填）/ Under-production variable result (flat modeling type, back-filled from solver solution)
 *
 * 与 YieldModel.kt 中的 UnderProduction（分析层富类型）不同，此类型仅记录 solver 变量值，
 * 不持有 ProductDemand 或 Quantity 等领域对象。
 *
 * @param V 数值类型 / Numeric value type
 * @property productId 产品 ID / Product id
 * @property unitSymbol 需求单位符号 / Demand unit symbol
 * @property amount 欠产量 / Under-production amount
 */
data class ModeledUnderProduction<V : RealNumber<V>>(
    val productId: String,
    val unitSymbol: String,
    val amount: V
)

/**
 * 超产变量结果（建模层扁平类型，从 solver solution 回填）/ Over-production variable result (flat modeling type, back-filled from solver solution)
 *
 * 与 YieldModel.kt 中的 OverProduction（分析层富类型）不同，此类型仅记录 solver 变量值，
 * 不持有 ProductDemand 或 Quantity 等领域对象。
 *
 * @param V 数值类型 / Numeric value type
 * @property productId 产品 ID / Product id
 * @property unitSymbol 需求单位符号 / Demand unit symbol
 * @property amount 超产量 / Over-production amount
 */
data class ModeledOverProduction<V : RealNumber<V>>(
    val productId: String,
    val unitSymbol: String,
    val amount: V
)
