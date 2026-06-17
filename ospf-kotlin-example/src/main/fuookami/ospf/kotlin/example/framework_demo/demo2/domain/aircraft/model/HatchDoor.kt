package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/** 货物位置与舱门之间的空间关系类型。Spatial relationship type between a cargo position and a hatch door. */
enum class DoorUbietyType {
    Front,
    AdjacentFront,
    Beside,
    Opposite,
    AdjacentBehind,
    Behind;
}

/**
 * 描述货物位置与附近舱门之间的空间关系。Describes the spatial relationship between a cargo position and a nearby hatch door.
 *
 * @property type 类型。
 * @property sameSide 是否同侧。
 * @property position 位置。
 * @property door 舱门。
 */
data class DoorUbiety(
    val type: DoorUbietyType,
    val sameSide: Boolean,
    val position: Position,
    val door: HatchDoor
)

/**
 * 飞机甲板上的舱门（具有位置和臂坐标）。A hatch door on the aircraft deck with its location and arm coordinates.
 *
 * @property location 位置。
 * @property besideBulk 是否靠近隔框。
 * @property noseDoor 是否机头门。
 * @property lateralArm 横向臂。
 * @property frontArm 前臂。
 * @property backArm 后臂。
 */
data class HatchDoor(
    val location: DeckLocation,
    val besideBulk: Boolean,
    val noseDoor: Boolean,
    val lateralArm: Quantity<Flt64>,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>
)
