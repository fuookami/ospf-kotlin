@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.ceil as quantityCeil
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.div as quantityDiv
import fuookami.ospf.kotlin.quantities.quantity.eq as quantityEq
import fuookami.ospf.kotlin.quantities.quantity.floor as quantityFloor
import fuookami.ospf.kotlin.quantities.quantity.geq as quantityGeq
import fuookami.ospf.kotlin.quantities.quantity.gr as quantityGr
import fuookami.ospf.kotlin.quantities.quantity.leq as quantityLeq
import fuookami.ospf.kotlin.quantities.quantity.ls as quantityLs
import fuookami.ospf.kotlin.quantities.quantity.minus as quantityMinus
import fuookami.ospf.kotlin.quantities.quantity.partialOrd as quantityPartialOrd
import fuookami.ospf.kotlin.quantities.quantity.plus as quantityPlus
import fuookami.ospf.kotlin.quantities.quantity.round as quantityRound
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.Order

private fun QuantityFlt64.toScalar(unit: PhysicalUnit): Flt64 {
    return if (this.unit == unit) {
        this.value
    } else {
        this.convertTo(unit)?.value
            ?: throw IllegalArgumentException("Incompatible unit: ${this.unit} vs $unit")
    }
}

operator fun Flt64.times(unit: PhysicalUnit): QuantityFlt64 = Quantity(this, unit)

infix fun QuantityFlt64.eq(rhs: QuantityFlt64): Boolean = this.quantityEq(rhs)
infix fun QuantityFlt64.neq(rhs: QuantityFlt64): Boolean = !this.quantityEq(rhs)
infix fun QuantityFlt64.leq(rhs: QuantityFlt64): Boolean = this.quantityLeq(rhs) ?: false
infix fun QuantityFlt64.geq(rhs: QuantityFlt64): Boolean = this.quantityGeq(rhs) ?: false
infix fun QuantityFlt64.ls(rhs: QuantityFlt64): Boolean = this.quantityLs(rhs) ?: false
infix fun QuantityFlt64.gr(rhs: QuantityFlt64): Boolean = this.quantityGr(rhs) ?: false

infix fun QuantityFlt64.eq(rhs: Flt64): Boolean = this.value == rhs
infix fun QuantityFlt64.neq(rhs: Flt64): Boolean = this.value != rhs
infix fun QuantityFlt64.leq(rhs: Flt64): Boolean = this.value <= rhs
infix fun QuantityFlt64.geq(rhs: Flt64): Boolean = this.value >= rhs
infix fun QuantityFlt64.ls(rhs: Flt64): Boolean = this.value < rhs
infix fun QuantityFlt64.gr(rhs: Flt64): Boolean = this.value > rhs

infix fun Flt64.eq(rhs: QuantityFlt64): Boolean = this == rhs.value
infix fun Flt64.neq(rhs: QuantityFlt64): Boolean = this != rhs.value
infix fun Flt64.leq(rhs: QuantityFlt64): Boolean = this <= rhs.value
infix fun Flt64.geq(rhs: QuantityFlt64): Boolean = this >= rhs.value
infix fun Flt64.ls(rhs: QuantityFlt64): Boolean = this < rhs.value
infix fun Flt64.gr(rhs: QuantityFlt64): Boolean = this > rhs.value

operator fun QuantityFlt64.plus(rhs: QuantityFlt64): QuantityFlt64 = this.quantityPlus(rhs)
operator fun QuantityFlt64.minus(rhs: QuantityFlt64): QuantityFlt64 = this.quantityMinus(rhs)
operator fun QuantityFlt64.times(rhs: QuantityFlt64): QuantityFlt64 = this.quantityTimes(rhs)
operator fun QuantityFlt64.div(rhs: QuantityFlt64): QuantityFlt64 = this.quantityDiv(rhs)

operator fun QuantityFlt64.plus(rhs: Flt64): QuantityFlt64 = this + (rhs * this.unit)
operator fun QuantityFlt64.minus(rhs: Flt64): QuantityFlt64 = this - (rhs * this.unit)
operator fun QuantityFlt64.times(rhs: Flt64): QuantityFlt64 = this.quantityTimes(rhs)
operator fun QuantityFlt64.div(rhs: Flt64): QuantityFlt64 = this.quantityDiv(rhs)

operator fun Flt64.plus(rhs: QuantityFlt64): QuantityFlt64 = (this * rhs.unit) + rhs
operator fun Flt64.minus(rhs: QuantityFlt64): QuantityFlt64 = (this * rhs.unit) - rhs
operator fun Flt64.times(rhs: QuantityFlt64): QuantityFlt64 = this.quantityTimes(rhs)

operator fun QuantityFlt64.rem(rhs: QuantityFlt64): QuantityFlt64 {
    val right = rhs.toScalar(this.unit)
    return (this.value % right) * this.unit
}

operator fun QuantityFlt64.rem(rhs: Flt64): QuantityFlt64 {
    return (this.value % rhs) * this.unit
}

operator fun Flt64.rem(rhs: QuantityFlt64): QuantityFlt64 {
    return (this % rhs.value) * rhs.unit
}

fun QuantityFlt64.floor(): Flt64 = this.quantityFloor().value
fun QuantityFlt64.ceil(): Flt64 = this.quantityCeil().value
fun QuantityFlt64.round(): Flt64 = this.quantityRound().value
fun QuantityFlt64.toDouble(): Double = this.value.toDouble()
fun QuantityFlt64.toFlt64(): Flt64 = this.value
fun QuantityFlt64.abs(): QuantityFlt64 = if (this.value >= Flt64.zero) this else (-this.value) * this.unit

infix fun QuantityFlt64.ord(rhs: QuantityFlt64): Order {
    return this.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity: ${this.unit} vs ${rhs.unit}")
}

fun max(lhs: QuantityFlt64, rhs: QuantityFlt64): QuantityFlt64 {
    return if (lhs gr rhs) lhs else rhs
}

fun max(lhs: QuantityFlt64, rhs: QuantityFlt64, vararg rest: QuantityFlt64): QuantityFlt64 {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

fun min(lhs: QuantityFlt64, rhs: QuantityFlt64): QuantityFlt64 {
    return if (lhs leq rhs) lhs else rhs
}

fun min(lhs: QuantityFlt64, rhs: QuantityFlt64, vararg rest: QuantityFlt64): QuantityFlt64 {
    var current = min(lhs, rhs)
    for (value in rest) {
        current = min(current, value)
    }
    return current
}

fun <T> Iterable<T>.sumOfQuantity(selector: (T) -> QuantityFlt64): QuantityFlt64 {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return Flt64.zero * Meter
    }
    var sum = selector(iterator.next())
    while (iterator.hasNext()) {
        sum += selector(iterator.next())
    }
    return sum
}

fun <T> Iterable<T>.sumOf(selector: (T) -> QuantityFlt64): QuantityFlt64 {
    return sumOfQuantity(selector)
}

fun <T> Iterable<T>.maxOf(selector: (T) -> QuantityFlt64): QuantityFlt64 {
    val iterator = this.iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (current gr best) {
            best = current
        }
    }
    return best
}

fun <T> Iterable<T>.maxOfOrNull(selector: (T) -> QuantityFlt64): QuantityFlt64? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (current gr best) {
            best = current
        }
    }
    return best
}

fun <T> Iterable<T>.minOf(selector: (T) -> QuantityFlt64): QuantityFlt64 {
    val iterator = this.iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (current ls best) {
            best = current
        }
    }
    return best
}

fun <T> Iterable<T>.minOfOrNull(selector: (T) -> QuantityFlt64): QuantityFlt64? {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (current ls best) {
            best = current
        }
    }
    return best
}

fun <T> Iterable<T>.sortedBy(selector: (T) -> QuantityFlt64): List<T> {
    return this.toList().sortedWith(compareBy { selector(it).toDouble() })
}

fun <T> Iterable<T>.sortedByDescending(selector: (T) -> QuantityFlt64): List<T> {
    return this.toList().sortedWith(compareByDescending { selector(it).toDouble() })
}

fun <T> Iterable<T>.maxBy(selector: (T) -> QuantityFlt64): T {
    val iterator = this.iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var bestItem = iterator.next()
    var bestValue = selector(bestItem)
    while (iterator.hasNext()) {
        val item = iterator.next()
        val value = selector(item)
        if (value gr bestValue) {
            bestValue = value
            bestItem = item
        }
    }
    return bestItem
}

operator fun QuantityPoint2.plus(offset: Point<Dim2, Flt64>): QuantityPoint2 {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

operator fun QuantityPoint2.minus(rhs: QuantityPoint2): QuantityVector2 {
    return QuantityVector2(
        x = x - rhs.x,
        y = y - rhs.y
    )
}

infix fun QuantityPoint2.eq(rhs: QuantityPoint2): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y
}

infix fun QuantityPoint2.neq(rhs: QuantityPoint2): Boolean = !(this eq rhs)

operator fun QuantityPoint2.plus(offset: Vector<Dim2, Flt64>): QuantityPoint2 {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

operator fun QuantityPoint3.plus(offset: Point<Dim3, Flt64>): QuantityPoint3 {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

operator fun QuantityPoint3.minus(rhs: QuantityPoint3): QuantityVector3 {
    return QuantityVector3(
        x = x - rhs.x,
        y = y - rhs.y,
        z = z - rhs.z
    )
}

infix fun QuantityPoint3.eq(rhs: QuantityPoint3): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y && this.z eq rhs.z
}

infix fun QuantityPoint3.neq(rhs: QuantityPoint3): Boolean = !(this eq rhs)

operator fun QuantityPoint3.plus(offset: Vector<Dim3, Flt64>): QuantityPoint3 {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

fun point2(
    x: QuantityFlt64 = Flt64.zero * Meter,
    y: QuantityFlt64 = Flt64.zero * Meter
): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

fun point2(): QuantityPoint2 = QuantityPoint2(x = Flt64.zero * Meter, y = Flt64.zero * Meter)

fun point2(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint2 {
    return QuantityPoint2(x = x * unit, y = y * unit)
}

fun point2(
    point: Point<Dim2, Flt64>,
    unit: PhysicalUnit = Meter
): QuantityPoint2 {
    return point2(point[0], point[1], unit)
}

fun point3(
    x: QuantityFlt64 = Flt64.zero * Meter,
    y: QuantityFlt64 = Flt64.zero * Meter,
    z: QuantityFlt64 = Flt64.zero * Meter
): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

fun point3(): QuantityPoint3 = QuantityPoint3(
    x = Flt64.zero * Meter,
    y = Flt64.zero * Meter,
    z = Flt64.zero * Meter
)

fun point3(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    z: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint3 {
    return QuantityPoint3(x = x * unit, y = y * unit, z = z * unit)
}

fun point3(
    point: Point<Dim3, Flt64>,
    unit: PhysicalUnit = Meter
): QuantityPoint3 {
    return point3(point[0], point[1], point[2], unit)
}

fun point3(vector: Vector<Dim3, Flt64>, unit: PhysicalUnit = Meter): QuantityPoint3 {
    return point3(vector[0], vector[1], vector[2], unit)
}

fun vector2(
    x: QuantityFlt64 = Flt64.zero * Meter,
    y: QuantityFlt64 = Flt64.zero * Meter
): QuantityVector2 {
    return QuantityVector2(x = x, y = y)
}

fun vector2(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityVector2 {
    return QuantityVector2(x = x * unit, y = y * unit)
}

fun vector2(vector: Vector<Dim2, Flt64>, unit: PhysicalUnit = Meter): QuantityVector2 {
    return vector2(vector[0], vector[1], unit)
}

fun vector3(
    x: QuantityFlt64 = Flt64.zero * Meter,
    y: QuantityFlt64 = Flt64.zero * Meter,
    z: QuantityFlt64 = Flt64.zero * Meter
): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}

fun vector3(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    z: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityVector3 {
    return QuantityVector3(x = x * unit, y = y * unit, z = z * unit)
}

fun vector3(vector: Vector<Dim3, Flt64>, unit: PhysicalUnit = Meter): QuantityVector3 {
    return vector3(vector[0], vector[1], vector[2], unit)
}
