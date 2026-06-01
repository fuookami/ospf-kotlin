package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 切割方案对需求的统一贡献值 / Unified demand contribution produced by a cutting plan
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property quantity 贡献值 / Contribution quantity
 */
data class CuttingPlanDemandContribution<V : RealNumber<V>>(
    val product: Product<V>,
    val quantity: Quantity<V>
)

