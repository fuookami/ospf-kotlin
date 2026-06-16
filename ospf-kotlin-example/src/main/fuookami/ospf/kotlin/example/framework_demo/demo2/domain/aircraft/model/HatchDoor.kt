package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.*

/** Spatial relationship type between a cargo position and a hatch door. */
enum class DoorUbietyType {
    Front,
    AdjacentFront,
    Beside,
    Opposite,
    AdjacentBehind,
    Behind;
}

/** Describes the spatial relationship between a cargo position and a nearby hatch door. */
data class DoorUbiety(
    val type: DoorUbietyType,
    val sameSide: Boolean,
    val position: Position,
    val door: HatchDoor
)

/** A hatch door on the aircraft deck with its location and arm coordinates. */
data class HatchDoor(
    val location: DeckLocation,
    val besideBulk: Boolean,
    val noseDoor: Boolean,
    val lateralArm: Quantity<Flt64>,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>
)
