@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/** 具名物料库存，不同物料不混加；期初量指优化起点事件发生前的库存。 / Named inventory; opening stock precedes horizon-start events.
 * @property id 稳定物料身份 / Stable material identity
 * @property initial 期初库存与统一计量单位 / Opening stock and canonical unit
 * @property minimum 全时域最低库存，null 表示零 / Minimum stock throughout the horizon; null means zero
 * @property maximum 可选库存上限 / Optional storage limit
 */
data class ActivityMaterial<V : RealNumber<V>>(
    val id: String,
    val initial: Quantity<V>,
    val minimum: Quantity<V>? = null,
    val maximum: Quantity<V>? = null
)

/** 模式内部物料事件；正值入库，负值消耗，同一时点原子结算。 / Intra-mode material event; positive supplies, negative consumes; simultaneous events net atomically.
 * @property material 物料身份 / Material identity
 * @property quantity 有符号库存变化量 / Signed inventory change
 * @property at 相对活动起点的事件时间，允许发生在结束点 / Offset from activity start, including completion
 */
data class ActivityMaterialFlow<V : RealNumber<V>>(
    val material: String,
    val quantity: Quantity<V>,
    val at: Duration = Duration.ZERO
)

/** 与活动选择无关的未来到货或需求，不自动从执行历史推导。 / Fixed future receipt or demand, never inferred from execution history.
 * @property material 物料身份 / Material identity
 * @property time 事件时间，包含优化区间两端 / Event time, including both horizon endpoints
 * @property quantity 有符号库存变化量 / Signed inventory change
 */
data class ActivityMaterialEvent<V : RealNumber<V>>(
    val material: String,
    val time: Instant,
    val quantity: Quantity<V>
)

/** 同时点所有事件结算后的库存。 / Stock after all simultaneous events settle.
 * @property time 事件时间 / Event time
 * @property quantity 该时点之后的库存 / Stock after the event
 */
data class ActivityInventoryLevel<V : RealNumber<V>>(val time: Instant, val quantity: Quantity<V>)

/** 库存复核轨迹，含原始期初量与事件后库存。 / Inventory audit trail retaining opening and post-event stock.
 * @property material 物料身份 / Material identity
 * @property initial 事件发生前的期初库存 / Opening stock before events
 * @param levels 事件后库存，包含优化区间两端 / Post-event levels, including horizon endpoints
 */
class ActivityMaterialInventory<V : RealNumber<V>>(
    val material: String,
    val initial: Quantity<V>,
    levels: List<ActivityInventoryLevel<V>>
) {
    val levels = levels.toList()
}
