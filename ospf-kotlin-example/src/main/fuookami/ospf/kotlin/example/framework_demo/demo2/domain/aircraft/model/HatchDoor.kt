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
 * @property type The type of spatial relationship. / 空间关系类型
 * @property sameSide Whether the position and door are on the same side. / 位置和舱门是否同侧
 * @property position The cargo position. / 货物位置
 * @property door The hatch door. / 舱门
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
 * @property location The deck location where the hatch door is situated. / 舱门所在的甲板位置
 * @property besideBulk Whether the door is beside a bulkhead. / 舱门是否靠近隔框
 * @property noseDoor Whether this is a nose door. / 是否为机头门
 * @property lateralArm The lateral arm coordinate of the door. / 舱门的横向臂坐标
 * @property frontArm The front arm coordinate of the door. / 舱门的前臂坐标
 * @property backArm The back arm coordinate of the door. / 舱门的后臂坐标
*/
data class HatchDoor(
    val location: DeckLocation,
    val besideBulk: Boolean,
    val noseDoor: Boolean,
    val lateralArm: Quantity<Flt64>,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>
)
