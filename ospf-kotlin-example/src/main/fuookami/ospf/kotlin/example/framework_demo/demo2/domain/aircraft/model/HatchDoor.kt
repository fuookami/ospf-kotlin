package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * Spatial relationship type between a cargo position and a hatch door.
 * 货物位置与舱门之间的空间关系类型。
*/
enum class DoorUbietyType {
    /** In front of the door / 在舱门前方 */
    Front,
    /** Adjacent to the front of the door / 紧邻舱门前方 */
    AdjacentFront,
    /** Beside the door / 在舱门旁边 */
    Beside,
    /** Opposite the door / 在舱门对面 */
    Opposite,
    /** Adjacent to the behind of the door / 紧邻舱门后方 */
    AdjacentBehind,
    /** Behind the door / 在舱门后方 */
    Behind;
}

/**
 * Describes the spatial relationship between a cargo position and a nearby hatch door.
 * 描述货物位置与附近舱门之间的空间关系。
 *
 * @property type 空间关系类型 / The type of spatial relationship.
 * @property sameSide 位置和舱门是否同侧 / Whether the position and door are on the same side.
 * @property position 货物位置 / The cargo position.
 * @property door 舱门 / The hatch door.
*/
data class DoorUbiety(
    val type: DoorUbietyType,
    val sameSide: Boolean,
    val position: Position,
    val door: HatchDoor
)

/**
 * A hatch door on the aircraft deck with its location and arm coordinates.
 * 飞机甲板上的舱门（具有位置和臂坐标）。
 *
 * @property location 舱门所在的甲板位置 / The deck location where the hatch door is situated.
 * @property besideBulk 舱门是否靠近隔框 / Whether the door is beside a bulkhead.
 * @property noseDoor 是否为机头门 / Whether this is a nose door.
 * @property lateralArm 舱门的横向臂坐标 / The lateral arm coordinate of the door.
 * @property frontArm 舱门的前臂坐标 / The front arm coordinate of the door.
 * @property backArm 舱门的后臂坐标 / The back arm coordinate of the door.
*/
data class HatchDoor(
    val location: DeckLocation,
    val besideBulk: Boolean,
    val noseDoor: Boolean,
    val lateralArm: Quantity<Flt64>,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>
)
