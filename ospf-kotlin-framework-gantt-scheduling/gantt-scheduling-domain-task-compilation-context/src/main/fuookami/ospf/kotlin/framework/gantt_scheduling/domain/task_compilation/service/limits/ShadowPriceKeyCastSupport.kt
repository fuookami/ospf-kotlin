@file:Suppress("UNCHECKED_CAST")

/** 影子价格键类型转换支持 / Shadow price key cast support */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.framework.model.ShadowPriceKey

/**
 * 将约束参数安全收敛为目标影子价格 key 类型。
 * 约束分组由对应 pipeline 构造，运行期 key 类型不变量由构造路径保证。
 *
 * Safely narrows constraint args to the target shadow-price key type.
 * The pipeline construction path owns the runtime key-type invariant.
 *
 * @param K 目标影子价格键类型 / Target shadow price key type
 * @param args 约束参数 / Constraint arguments
 * @return 安全转换后的影子价格键，失败返回 null / Safely cast shadow price key, or null on failure
*/
internal inline fun <reified K : ShadowPriceKey> shadowPriceKeyOf(args: Any?): K? {
    return args as? K
}
