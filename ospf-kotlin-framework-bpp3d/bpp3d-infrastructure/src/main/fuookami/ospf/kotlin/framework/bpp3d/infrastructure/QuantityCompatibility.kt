@file:Suppress("DEPRECATION")

/**
 * 量纲兼容性基础设施。
 * Quantity compatibility infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

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

private fun InfraQuantity.toScalar(unit: PhysicalUnit): InfraNumber {
    return if (this.unit == unit) {
        this.value
    } else {
        this.convertTo(unit)?.value
            ?: throw IllegalArgumentException("Incompatible unit: ${this.unit} vs $unit")
    }
}

operator fun InfraNumber.times(unit: PhysicalUnit): InfraQuantity = Quantity(this, unit)

infix fun InfraQuantity.eq(rhs: InfraQuantity): Boolean = this.quantityEq(rhs)
infix fun InfraQuantity.neq(rhs: InfraQuantity): Boolean = !this.quantityEq(rhs)
infix fun InfraQuantity.leq(rhs: InfraQuantity): Boolean = this.quantityLeq(rhs) ?: false
infix fun InfraQuantity.geq(rhs: InfraQuantity): Boolean = this.quantityGeq(rhs) ?: false
infix fun InfraQuantity.ls(rhs: InfraQuantity): Boolean = this.quantityLs(rhs) ?: false
infix fun InfraQuantity.gr(rhs: InfraQuantity): Boolean = this.quantityGr(rhs) ?: false

infix fun InfraQuantity.eq(rhs: InfraNumber): Boolean = this.value == rhs
infix fun InfraQuantity.neq(rhs: InfraNumber): Boolean = this.value != rhs
infix fun InfraQuantity.leq(rhs: InfraNumber): Boolean = this.value <= rhs
infix fun InfraQuantity.geq(rhs: InfraNumber): Boolean = this.value >= rhs
infix fun InfraQuantity.ls(rhs: InfraNumber): Boolean = this.value < rhs
infix fun InfraQuantity.gr(rhs: InfraNumber): Boolean = this.value > rhs

infix fun InfraNumber.eq(rhs: InfraQuantity): Boolean = this == rhs.value
infix fun InfraNumber.neq(rhs: InfraQuantity): Boolean = this != rhs.value
infix fun InfraNumber.leq(rhs: InfraQuantity): Boolean = this <= rhs.value
infix fun InfraNumber.geq(rhs: InfraQuantity): Boolean = this >= rhs.value
infix fun InfraNumber.ls(rhs: InfraQuantity): Boolean = this < rhs.value
infix fun InfraNumber.gr(rhs: InfraQuantity): Boolean = this > rhs.value

operator fun InfraQuantity.plus(rhs: InfraQuantity): InfraQuantity = this.quantityPlus(rhs)
operator fun InfraQuantity.minus(rhs: InfraQuantity): InfraQuantity = this.quantityMinus(rhs)
operator fun InfraQuantity.times(rhs: InfraQuantity): InfraQuantity = this.quantityTimes(rhs)
operator fun InfraQuantity.div(rhs: InfraQuantity): InfraQuantity = this.quantityDiv(rhs)

operator fun InfraQuantity.plus(rhs: InfraNumber): InfraQuantity = this + (rhs * this.unit)
operator fun InfraQuantity.minus(rhs: InfraNumber): InfraQuantity = this - (rhs * this.unit)
operator fun InfraQuantity.times(rhs: InfraNumber): InfraQuantity = this.quantityTimes(rhs)
operator fun InfraQuantity.div(rhs: InfraNumber): InfraQuantity = this.quantityDiv(rhs)

operator fun InfraNumber.plus(rhs: InfraQuantity): InfraQuantity = (this * rhs.unit) + rhs
operator fun InfraNumber.minus(rhs: InfraQuantity): InfraQuantity = (this * rhs.unit) - rhs
operator fun InfraNumber.times(rhs: InfraQuantity): InfraQuantity = this.quantityTimes(rhs)

operator fun InfraQuantity.rem(rhs: InfraQuantity): InfraQuantity {
    val right = rhs.toScalar(this.unit)
    return (this.value % right) * this.unit
}

operator fun InfraQuantity.rem(rhs: InfraNumber): InfraQuantity {
    return (this.value % rhs) * this.unit
}

operator fun InfraNumber.rem(rhs: InfraQuantity): InfraQuantity {
    return (this % rhs.value) * rhs.unit
}

fun InfraQuantity.floor(): InfraNumber = this.quantityFloor().value
fun InfraQuantity.ceil(): InfraNumber = this.quantityCeil().value
fun InfraQuantity.round(): InfraNumber = this.quantityRound().value
fun InfraQuantity.toDouble(): Double = this.value.toDouble()
fun InfraQuantity.toScalarValue(): InfraNumber = this.value
fun InfraQuantity.abs(): InfraQuantity = if (this.value >= infraZero()) this else (-this.value) * this.unit

infix fun InfraQuantity.ord(rhs: InfraQuantity): Order {
    return this.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity: ${this.unit} vs ${rhs.unit}")
}

fun max(lhs: InfraQuantity, rhs: InfraQuantity): InfraQuantity {
    return if (lhs gr rhs) lhs else rhs
}

fun max(lhs: InfraQuantity, rhs: InfraQuantity, vararg rest: InfraQuantity): InfraQuantity {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

fun min(lhs: InfraQuantity, rhs: InfraQuantity): InfraQuantity {
    return if (lhs leq rhs) lhs else rhs
}

fun min(lhs: InfraQuantity, rhs: InfraQuantity, vararg rest: InfraQuantity): InfraQuantity {
    var current = min(lhs, rhs)
    for (value in rest) {
        current = min(current, value)
    }
    return current
}

fun <T> Iterable<T>.sumOfQuantity(selector: (T) -> InfraQuantity): InfraQuantity {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return infraZero() * Meter
    }
    var sum = selector(iterator.next())
    while (iterator.hasNext()) {
        sum += selector(iterator.next())
    }
    return sum
}

fun <T> Iterable<T>.sumOf(selector: (T) -> InfraQuantity): InfraQuantity {
    return sumOfQuantity(selector)
}

fun <T> Iterable<T>.maxOf(selector: (T) -> InfraQuantity): InfraQuantity {
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

fun <T> Iterable<T>.maxOfOrNull(selector: (T) -> InfraQuantity): InfraQuantity? {
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

fun <T> Iterable<T>.minOf(selector: (T) -> InfraQuantity): InfraQuantity {
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

fun <T> Iterable<T>.minOfOrNull(selector: (T) -> InfraQuantity): InfraQuantity? {
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

fun <T> Iterable<T>.sortedBy(selector: (T) -> InfraQuantity): List<T> {
    return this.toList().sortedWith(compareBy { selector(it).toDouble() })
}

fun <T> Iterable<T>.sortedByDescending(selector: (T) -> InfraQuantity): List<T> {
    return this.toList().sortedWith(compareByDescending { selector(it).toDouble() })
}

fun <T> Iterable<T>.maxBy(selector: (T) -> InfraQuantity): T {
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

operator fun QuantityPoint2.plus(offset: Point<Dim2, InfraNumber>): QuantityPoint2 {
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

operator fun QuantityPoint2.plus(offset: Vector<Dim2, InfraNumber>): QuantityPoint2 {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

operator fun QuantityPoint3.plus(offset: Point<Dim3, InfraNumber>): QuantityPoint3 {
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

operator fun QuantityPoint3.plus(offset: Vector<Dim3, InfraNumber>): QuantityPoint3 {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

fun point2(
    x: InfraQuantity = infraZero() * Meter,
    y: InfraQuantity = infraZero() * Meter
): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

fun point2(): QuantityPoint2 = QuantityPoint2(x = infraZero() * Meter, y = infraZero() * Meter)

fun point2(
    x: InfraNumber = infraZero(),
    y: InfraNumber = infraZero(),
    unit: PhysicalUnit = Meter
): QuantityPoint2 {
    return QuantityPoint2(x = x * unit, y = y * unit)
}

fun point2(
    point: Point<Dim2, InfraNumber>,
    unit: PhysicalUnit = Meter
): QuantityPoint2 {
    return point2(point[0], point[1], unit)
}

fun point3(
    x: InfraQuantity = infraZero() * Meter,
    y: InfraQuantity = infraZero() * Meter,
    z: InfraQuantity = infraZero() * Meter
): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

fun point3(): QuantityPoint3 = QuantityPoint3(
    x = infraZero() * Meter,
    y = infraZero() * Meter,
    z = infraZero() * Meter
)

fun point3(
    x: InfraNumber = infraZero(),
    y: InfraNumber = infraZero(),
    z: InfraNumber = infraZero(),
    unit: PhysicalUnit = Meter
): QuantityPoint3 {
    return QuantityPoint3(x = x * unit, y = y * unit, z = z * unit)
}

fun point3(
    point: Point<Dim3, InfraNumber>,
    unit: PhysicalUnit = Meter
): QuantityPoint3 {
    return point3(point[0], point[1], point[2], unit)
}

fun point3(vector: Vector<Dim3, InfraNumber>, unit: PhysicalUnit = Meter): QuantityPoint3 {
    return point3(vector[0], vector[1], vector[2], unit)
}

fun vector2(
    x: InfraQuantity = infraZero() * Meter,
    y: InfraQuantity = infraZero() * Meter
): QuantityVector2 {
    return QuantityVector2(x = x, y = y)
}

fun vector2(
    x: InfraNumber = infraZero(),
    y: InfraNumber = infraZero(),
    unit: PhysicalUnit = Meter
): QuantityVector2 {
    return QuantityVector2(x = x * unit, y = y * unit)
}

fun vector2(vector: Vector<Dim2, InfraNumber>, unit: PhysicalUnit = Meter): QuantityVector2 {
    return vector2(vector[0], vector[1], unit)
}

fun vector3(
    x: InfraQuantity = infraZero() * Meter,
    y: InfraQuantity = infraZero() * Meter,
    z: InfraQuantity = infraZero() * Meter
): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}

fun vector3(
    x: InfraNumber = infraZero(),
    y: InfraNumber = infraZero(),
    z: InfraNumber = infraZero(),
    unit: PhysicalUnit = Meter
): QuantityVector3 {
    return QuantityVector3(x = x * unit, y = y * unit, z = z * unit)
}

fun vector3(vector: Vector<Dim3, InfraNumber>, unit: PhysicalUnit = Meter): QuantityVector3 {
    return vector3(vector[0], vector[1], vector[2], unit)
}

