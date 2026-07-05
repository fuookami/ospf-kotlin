@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/** 货物位置标签枚举 / Item location tag enum */
enum class ItemLocationTag {
    Main,
    Low,
    Bulk,
    Head,
    Tail
}

/**
 * 货物位置，描述货物可装载的舱位和位置标签。
 * Item location, describing which deck and position tags the item can be loaded to.
 *
 * @property tags 位置标签集合 / set of location tags
 */
data class ItemLocation(
    val tags: Set<ItemLocationTag>,
) {
    companion object {
        val head = ItemLocation(setOf(ItemLocationTag.Main, ItemLocationTag.Head))
        val tail = ItemLocation(setOf(ItemLocationTag.Main, ItemLocationTag.Tail))
        val normalMain = ItemLocation(setOf(ItemLocationTag.Main))
        val lowBulk = ItemLocation(setOf(ItemLocationTag.Low, ItemLocationTag.Bulk))
        val lowNotBulk = ItemLocation(setOf(ItemLocationTag.Low))
    }

    operator fun contains(tag: ItemLocationTag): Boolean {
        return tags.contains(tag)
    }

    val main = contains(ItemLocationTag.Main)
    val head = contains(ItemLocationTag.Head)
    val tail = contains(ItemLocationTag.Tail)
    val normalMain = main && !head && !tail
    val specialMain = head || tail
    val low = contains(ItemLocationTag.Low)
    val bulk = contains(ItemLocationTag.Bulk)
    val lowNotBulk = low && !bulk

    /**
     * 检查该位置是否允许在指定甲板位置装载。
     * Check whether this location is allowed at the given deck location.
     *
     * @param location 甲板位置 / deck location
     * @return 如果允许则返回 ok，否则返回失败原因 / ok if allowed, otherwise a failure reason
     */
    fun enabledIn(location: DeckLocation): Try {
        return when (location) {
            DeckLocation.Main -> {
                if (low) {
                    Failed(ErrorCode.ApplicationFailed, "主舱货物不能装载到下舱货舱")
                } else {
                    ok
                }
            }

            DeckLocation.LowForward, DeckLocation.LowAft -> {
                if (main) {
                    Failed(ErrorCode.ApplicationFailed, "下舱货物不能装载到主舱货舱")
                } else {
                    ok
                }
            }
        }
    }

    /**
     * 检查该位置是否允许在指定货位位置装载。
     * Check whether this location is allowed at the given position location.
     *
     * @param location 货位位置 / position location
     * @return 如果允许则返回 ok，否则返回失败原因 / ok if allowed, otherwise a failure reason
     */
    fun enabledIn(location: PositionLocation): Try {
        if (low && location.main) {
            return Failed(ErrorCode.ApplicationFailed, "低舱位货物不能装载到主舱位")
        }
        if (main && location.low) {
            return Failed(ErrorCode.ApplicationFailed, "主舱位货物不能装载到低舱位")
        }
        if (!bulk && location.bulk) {
            return Failed(ErrorCode.ApplicationFailed, "非散货不能装载到散货位")
        }
        if (bulk && !location.bulk) {
            return Failed(ErrorCode.ApplicationFailed, "散货不能装载到非散货位")
        }
        if (head && !location.head) {
            return Failed(ErrorCode.ApplicationFailed, "头部货物不能装载到非头部位")
        }
        if (tail && !location.tail) {
            return Failed(ErrorCode.ApplicationFailed, "尾部货物不能装载到非尾部位")
        }
        return ok
    }
}

/** 货物状态枚举 / Item status enum */
enum class ItemStatus {
    Loaded {
        override val loaded = true
        override val available = false
    },
    AdjustmentNeeded {
        override val loaded = true
        override val available = true
        override val adjustmentNeeded = true
    },
    Preassigned {
        override val stowageNeeded = true
    },
    Optional {
        override val stowageNeeded = true
    },
    Reserved {
        override val available = false
    };

    open val loaded = false
    open val available = true
    open val unavailable get() = !available
    open val stowageNeeded get() = false
    open val adjustmentNeeded get() = false
}

/**
 * 货物类型信息，包含货物类型集合和优先级。
 * Cargo type information, including a set of cargo types and priority.
 *
 * @property types 货物类型集合 / set of cargo types
 * @property priority 货物优先级 / cargo priority
 */
data class ItemCargo(
    val types: Set<CargoType>,
    val priority: CargoPriority
) {
    operator fun contains(cargo: CargoType): Boolean {
        return types.contains(cargo)
    }

    operator fun contains(type: String): Boolean {
        return types.any { it.type == type }
    }

    operator fun contains(code: CargoCode): Boolean {
        return types.any { it.code == code }
    }
}

/**
 * 货物订单信息，包含硬停时间、复称时间、车辆板信息和顺序号。
 * Item order information, including hardstand time, reweigh time, car-board info, and order number.
 *
 * @property hardstand 硬停时间 / hardstand instant
 * @property reweighed 复称时间 / reweigh instant
 * @property carBoard 车辆板信息 / car-board info
 * @property order 顺序号 / order number
 */
data class ItemOrder(
    val hardstand: kotlin.time.Instant?,
    val reweighed: kotlin.time.Instant?,
    val carBoard: CarBoard?,
    val order: UInt8?
) {
    /**
     * 车辆板信息，包含车辆和板号。
     * Car-board information, including car and board identifiers.
     *
     * @property car 车辆标识 / car identifier
     * @property board 板号 / board identifier
     */
    data class CarBoard(
        val car: String,
        val board: String
    ) : PartialOrd<CarBoard>, Ord<CarBoard> {
        override fun partialOrd(rhs: CarBoard): Order {
            when (val result = car ord rhs.car) {
                Order.Equal -> {}

                else -> {
                    return result
                }
            }

            return board ord rhs.board
        }
    }
}

/**
 * 货物实例，包含标识、目的地、来源、ULD、重量、位置、状态和订单信息。
 * Item instance, containing identifier, destination, source, ULD, weight, location, status, and order info.
 *
 * @property id 货物标识 / item identifier
 * @property destination 目的地 IATA 代码 / destination IATA code
 * @property source 来源航班号 / source flight number
 * @property uld ULD 信息 / ULD information
 * @property weight 重量 / weight
 * @property location 货物位置 / item location
 * @property cargo 货物类型 / item cargo type
 * @property status 货物状态 / item status
 * @property order 订单信息 / order information
 */
class Item(
    val id: String,
    val destination: IATA,
    val source: FlightNo?,
    val uld: ULD?,
    val weight: Quantity<Flt64>,
    val location: ItemLocation,
    val cargo: ItemCargo,
    val status: ItemStatus,
    val order: ItemOrder?
): ManualIndexed() {
    override fun toString(): String {
        return id
    }
}

/** 货物对类型别名 / Type alias for a pair of items */
typealias ItemPair = Pair<Item, Item>
