package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.service

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey

/**
 * 影子价格键安全转换工具。 / Shadow price key safe cast utility.
 */
inline fun <reified K : ShadowPriceKey> shadowPriceKeyOf(args: Any?): K? {
    return args as? K
}
