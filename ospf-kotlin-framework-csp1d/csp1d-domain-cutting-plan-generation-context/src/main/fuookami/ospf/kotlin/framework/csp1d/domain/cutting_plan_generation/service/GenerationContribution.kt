package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*

internal fun <V : RealNumber<V>> generationContributionLength(
    product: Product<V>,
    material: Material<V>
): Quantity<V>? {
    return product.length ?: if (product.dynamicLength) {
        material.length
    } else {
        null
    }
}
