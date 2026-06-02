package fuookami.ospf.kotlin.framework.csp1d.domain.produce

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

data class ProduceInput<V : RealNumber<V>>(
    val candidatePlans: List<CuttingPlan<V>>,
    val demands: List<ProductDemand<V>>,
    val materials: List<Material<V>>,
    val machines: List<Machine<V>> = emptyList()
)
