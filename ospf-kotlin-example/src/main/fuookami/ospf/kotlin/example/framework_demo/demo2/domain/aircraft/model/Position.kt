package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.utils.functional.*

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

import fuookami.ospf.kotlin.quantities.quantity.*

/** Tags describing the location of a cargo position within the aircraft. */
enum class PositionLocationTag {
    Main,
    Low,
    LowForward,
    LowAft,
    Bulk,
    Head,
    Tail
}

/** A set of location tags that classify a cargo position's placement. */
data class PositionLocation(
    val tags: Set<PositionLocationTag>
) {
    companion object {
        val head = PositionLocation(setOf(PositionLocationTag.Main, PositionLocationTag.Head))
        val tail = PositionLocation(setOf(PositionLocationTag.Main, PositionLocationTag.Tail))
        val normalMain = PositionLocation(setOf(PositionLocationTag.Main))
        val lowForwardBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowForward, PositionLocationTag.Bulk))
        val lowForwardNotBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowForward))
        val lowAftBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowAft, PositionLocationTag.Bulk))
        val lowAftNotBulk = PositionLocation(setOf(PositionLocationTag.Low, PositionLocationTag.LowAft))
    }

    operator fun contains(tag: PositionLocationTag) = tags.contains(tag)

    val main = contains(PositionLocationTag.Main)
    val head = contains(PositionLocationTag.Head)
    val tail = contains(PositionLocationTag.Tail)
    val normalMain = contains(PositionLocationTag.Main)
    val specialMain = head || tail
    val low = contains(PositionLocationTag.Low)
    val lowForward = contains(PositionLocationTag.LowForward)
    val lowAft = contains(PositionLocationTag.LowAft)
    val bulk = contains(PositionLocationTag.Bulk)
    val lowNotBulk = low && !bulk
    val location = when {
        contains(PositionLocationTag.Main) -> DeckLocation.Main
        contains(PositionLocationTag.LowForward) -> DeckLocation.LowForward
        contains(PositionLocationTag.LowAft) -> DeckLocation.LowAft
        else -> DeckLocation.Main
    }
}

operator fun DeckLocation.contains(location: PositionLocation): Boolean {
    return location.location == this
}

/** Coordinate system for a cargo position with longitudinal and lateral arm measurements. */
class PositionCoordinate(
    private val aircraftModel: AircraftModel,
    val frontArm: Quantity<Flt64>,
    val backArm: Quantity<Flt64>,
    val leftArm: Quantity<Flt64>,
    val rightArm: Quantity<Flt64>,
    val offsets: HashMap<ULDCode, Quantity<Flt64>>
) {
    val longitudinalArm = (frontArm + backArm) / Flt64.two
    val lateralArm = (leftArm + rightArm) / Flt64.two

    val averageOffset = if (offsets.isNotEmpty()) {
        offsets.values.fold(Flt64.zero * aircraftModel.lengthUnit) { acc, offset -> acc + offset } / Flt64(offsets.size)
    } else {
        lateralArm
    }
    val minOffset = if (offsets.isNotEmpty()) {
        offsets.values.minWithThreeWayComparatorOrNull { lhs, rhs -> (lhs partialOrd rhs)!! } ?: (Flt64.zero * aircraftModel.lengthUnit)
    } else {
        lateralArm
    }

    fun offset(uld: ULD): Quantity<Flt64> {
        return offsets[uld.code] ?: averageOffset
    }

    val transverse = (leftArm leq (Flt64.zero * aircraftModel.lengthUnit))!! && ((Flt64.zero * aircraftModel.lengthUnit) leq rightArm)!!
    val onLeft = !transverse && (lateralArm leq (Flt64.zero * aircraftModel.lengthUnit))!!
    val onRight = !transverse && (lateralArm gr (Flt64.zero * aircraftModel.lengthUnit))!!

    fun inFrontOf(arm: Quantity<Flt64>): Boolean {
        return (backArm ls arm)!!
    }

    fun behind(arm: Quantity<Flt64>): Boolean {
        return (frontArm ls arm)!!
    }

    fun on(arm: Quantity<Flt64>): Boolean {
        return (frontArm leq arm)!! && (arm leq backArm)!!
    }

    fun between(frontArm: Quantity<Flt64>, backArm: Quantity<Flt64>): Boolean {
        return if ((backArm ls frontArm)!!) {
            between(backArm, frontArm)
        } else {
            (frontArm leq this.frontArm)!! && (this.backArm leq backArm)!!
        }
    }

    fun withIntersectionWith(frontArm: Quantity<Flt64>, backArm: Quantity<Flt64>): Boolean {
        return if ((backArm ls frontArm)!!) {
            withIntersectionWith(backArm, frontArm)
        } else {
            (this.frontArm leq backArm)!! && (this.backArm geq frontArm)!!
        }
    }

    fun withIntersectionWith(other: PositionCoordinate): Boolean {
        return withIntersectionWith(other.frontArm, other.backArm)
                && (this.leftArm leq other.rightArm)!! && (other.leftArm leq this.rightArm)!!
    }
}

/** Physical dimensions and area of a cargo position. */
data class PositionShape(
    private val aircraftModel: AircraftModel,
    val length: Quantity<Flt64>,
    val width: Quantity<Flt64>,
    val volume: Quantity<Flt64>
) {
    val area = (length * width).to(aircraftModel.areaUnit)!!
}

/** A cargo position on the aircraft with its coordinates, shape, location, and loading order. */
data class Position(
    val id: UInt64,
    val spaceName: String,
    val sizeCode: String,
    val linearLoadingOrder: UInt8,
    val coordinate: PositionCoordinate,
    val shape: PositionShape,
    val location: PositionLocation
) {
    companion object {
        operator fun invoke(
            aircraftModel: AircraftModel,
            id: UInt64,
            spaceName: String,
            sizeCode: String,
            frontArm: Quantity<Flt64>,
            backArm: Quantity<Flt64>,
            leftArm: Quantity<Flt64>,
            rightArm: Quantity<Flt64>,
            volume: Quantity<Flt64>,
            offsets: HashMap<ULDCode, Quantity<Flt64>>,
            location: PositionLocation,
            linearLoadingOrder: UInt8,
        ): Position {
            val coordinate = PositionCoordinate(aircraftModel, frontArm, backArm, leftArm, rightArm, offsets)
            val shape = PositionShape(aircraftModel, backArm - frontArm, rightArm - leftArm, volume)
            return Position(
                id = id,
                spaceName = spaceName,
                sizeCode = sizeCode,
                linearLoadingOrder = linearLoadingOrder,
                coordinate = coordinate,
                shape = shape,
                location = location
            )
        }
    }

    internal lateinit var _loadingOrder: LoadingOrder
    val loadingOrder by ::_loadingOrder

    val alphaSpaceName: String = spaceName.filter { it.isLetterOrDigit() }
    val enabledULDs by coordinate.offsets::keys
}
