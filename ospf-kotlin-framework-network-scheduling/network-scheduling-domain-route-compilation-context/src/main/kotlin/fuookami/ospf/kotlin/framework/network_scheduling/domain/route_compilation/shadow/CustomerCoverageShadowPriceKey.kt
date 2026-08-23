package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.shadow

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.CustomerId

/**
 * 客户覆盖影子价格键。 / Customer-coverage shadow price key.
 *
 * @property customerId 客户标识 / Customer identifier
 */
data class CustomerCoverageShadowPriceKey(
    val customerId: CustomerId
) : ShadowPriceKey(CustomerCoverageShadowPriceKey::class)
