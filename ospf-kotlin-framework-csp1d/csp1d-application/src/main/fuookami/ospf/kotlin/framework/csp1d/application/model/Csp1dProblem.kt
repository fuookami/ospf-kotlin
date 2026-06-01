package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Costar
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Machine
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand

/**
 * CSP1D 问题定义 / CSP1D problem definition
 *
 * @param V 数值类型 / Numeric value type
 * @property products 产品列表 / Product list
 * @property materials 物料列表 / Material list
 * @property machines 设备列表 / Machine list
 * @property costars 配规列表 / Costar list
 * @property demands 需求列表 / Demand list
 * @property configuration 求解配置 / Solving configuration
 */
data class Csp1dProblem<V : RealNumber<V>>(
    val products: List<Product<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>>,
    val costars: List<Costar<V>> = emptyList(),
    val demands: List<ProductDemand<V>>,
    val configuration: Csp1dConfiguration<V> = Csp1dConfiguration()
)

/**
 * CSP1D 求解配置 / CSP1D solving configuration
 *
 * @param V 数值类型 / Numeric value type
 * @property maxInitialPlans 初始方案上限 / Initial plan limit
 * @property maxPricingPlans 每轮定价新增方案上限 / Max pricing plans per iteration
 * @property iterationLimit 列生成迭代上限 / Column generation iteration limit
 */
data class Csp1dConfiguration<V : RealNumber<V>>(
    val maxInitialPlans: Int = 1024,
    val maxPricingPlans: Int = 64,
    val iterationLimit: Int = 8
)

