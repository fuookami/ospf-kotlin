/**
 * 影子价格键类型转换支持 / Shadow price key type cast support
 *
 * 本文件提供影子价格键的安全类型转换工具函数。
 * This file provides utility functions for safe type casting of shadow price keys.
*/
@file:Suppress("UNCHECKED_CAST")
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.produce.service.limits

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey

/**
 * 将约束参数安全收敛为目标影子价格 key 类型。
 * 约束分组由对应 pipeline 构造，运行期 key 类型不变量由构造路径保证。
 *
 * Safely narrows constraint args to the target shadow-price key type.
 * The pipeline construction path owns the runtime key-type invariant.
*/
internal inline fun <reified K : ShadowPriceKey> shadowPriceKeyOf(args: Any?): K? {
    return args as? K
}
