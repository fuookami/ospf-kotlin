@file:Suppress("DEPRECATION")
/**
 * 物理量运算工具函数。
 * Quantity operator utility functions.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.quantity.ceil as quantityCeil
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
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes

/**
 * 量纲运算基础设施。
 * Quantity operation infrastructure.
 */
private fun Quantity<FltX>.toScalar(unit: PhysicalUnit): FltX {
    return if (this.unit == unit) {
        this.value
    } else {
        this.convertTo(unit)?.value
            ?: throw IllegalArgumentException("Incompatible unit: ${this.unit} vs $unit")
    }
}

operator fun FltX.times(unit: PhysicalUnit): Quantity<FltX> = Quantity(this, unit)

infix fun Quantity<FltX>.eq(rhs: Quantity<FltX>): Boolean = this.quantityEq(rhs)
infix fun Quantity<FltX>.neq(rhs: Quantity<FltX>): Boolean = !this.quantityEq(rhs)
infix fun Quantity<FltX>.leq(rhs: Quantity<FltX>): Boolean = this.quantityLeq(rhs) ?: false
infix fun Quantity<FltX>.geq(rhs: Quantity<FltX>): Boolean = this.quantityGeq(rhs) ?: false
infix fun Quantity<FltX>.ls(rhs: Quantity<FltX>): Boolean = this.quantityLs(rhs) ?: false
infix fun Quantity<FltX>.gr(rhs: Quantity<FltX>): Boolean = this.quantityGr(rhs) ?: false

infix fun Quantity<FltX>.eq(rhs: FltX): Boolean = this.value == rhs
infix fun Quantity<FltX>.neq(rhs: FltX): Boolean = this.value != rhs
infix fun Quantity<FltX>.leq(rhs: FltX): Boolean = this.value <= rhs
infix fun Quantity<FltX>.geq(rhs: FltX): Boolean = this.value >= rhs
infix fun Quantity<FltX>.ls(rhs: FltX): Boolean = this.value < rhs
infix fun Quantity<FltX>.gr(rhs: FltX): Boolean = this.value > rhs

infix fun FltX.eq(rhs: Quantity<FltX>): Boolean = this == rhs.value
infix fun FltX.neq(rhs: Quantity<FltX>): Boolean = this != rhs.value
infix fun FltX.leq(rhs: Quantity<FltX>): Boolean = this <= rhs.value
infix fun FltX.geq(rhs: Quantity<FltX>): Boolean = this >= rhs.value
infix fun FltX.ls(rhs: Quantity<FltX>): Boolean = this < rhs.value
infix fun FltX.gr(rhs: Quantity<FltX>): Boolean = this > rhs.value

operator fun Quantity<FltX>.plus(rhs: Quantity<FltX>): Quantity<FltX> = this.quantityPlus(rhs)
operator fun Quantity<FltX>.minus(rhs: Quantity<FltX>): Quantity<FltX> = this.quantityMinus(rhs)
operator fun Quantity<FltX>.times(rhs: Quantity<FltX>): Quantity<FltX> = this.quantityTimes(rhs)
operator fun Quantity<FltX>.div(rhs: Quantity<FltX>): Quantity<FltX> = this.quantityDiv(rhs)

operator fun Quantity<FltX>.plus(rhs: FltX): Quantity<FltX> = this + (rhs * this.unit)
operator fun Quantity<FltX>.minus(rhs: FltX): Quantity<FltX> = this - (rhs * this.unit)
operator fun Quantity<FltX>.times(rhs: FltX): Quantity<FltX> = this.quantityTimes(rhs)
operator fun Quantity<FltX>.div(rhs: FltX): Quantity<FltX> = this.quantityDiv(rhs)

operator fun FltX.plus(rhs: Quantity<FltX>): Quantity<FltX> = (this * rhs.unit) + rhs
operator fun FltX.minus(rhs: Quantity<FltX>): Quantity<FltX> = (this * rhs.unit) - rhs
operator fun FltX.times(rhs: Quantity<FltX>): Quantity<FltX> = this.quantityTimes(rhs)

operator fun Quantity<FltX>.rem(rhs: Quantity<FltX>): Quantity<FltX> {
    val right = rhs.toScalar(this.unit)
    return (this.value % right) * this.unit
}

operator fun Quantity<FltX>.rem(rhs: FltX): Quantity<FltX> {
    return (this.value % rhs) * this.unit
}

operator fun FltX.rem(rhs: Quantity<FltX>): Quantity<FltX> {
    return (this % rhs.value) * rhs.unit
}

fun Quantity<FltX>.floor(): FltX = this.quantityFloor().value
fun Quantity<FltX>.ceil(): FltX = this.quantityCeil().value
fun Quantity<FltX>.round(): FltX = this.quantityRound().value
fun Quantity<FltX>.toDouble(): Double = this.value.toDouble()
fun Quantity<FltX>.toScalarValue(): FltX = this.value
fun Quantity<FltX>.abs(): Quantity<FltX> = if (this.value >= fltXZero()) this else (-this.value) * this.unit

infix fun Quantity<FltX>.ord(rhs: Quantity<FltX>): Order {
    return this.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity: ${this.unit} vs ${rhs.unit}")
}

fun max(lhs: Quantity<FltX>, rhs: Quantity<FltX>): Quantity<FltX> {
    return if (lhs gr rhs) lhs else rhs
}

fun max(lhs: Quantity<FltX>, rhs: Quantity<FltX>, vararg rest: Quantity<FltX>): Quantity<FltX> {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

fun min(lhs: Quantity<FltX>, rhs: Quantity<FltX>): Quantity<FltX> {
    return if (lhs leq rhs) lhs else rhs
}

fun min(lhs: Quantity<FltX>, rhs: Quantity<FltX>, vararg rest: Quantity<FltX>): Quantity<FltX> {
    var current = min(lhs, rhs)
    for (value in rest) {
        current = min(current, value)
    }
    return current
}

fun <T> Iterable<T>.sumOfQuantity(selector: (T) -> Quantity<FltX>): Quantity<FltX> {
    val iterator = this.iterator()
    if (!iterator.hasNext()) {
        return fltXZero() * Meter
    }
    var sum = selector(iterator.next())
    while (iterator.hasNext()) {
        sum += selector(iterator.next())
    }
    return sum
}

fun <T> Iterable<T>.sumOf(selector: (T) -> Quantity<FltX>): Quantity<FltX> {
    return sumOfQuantity(selector)
}

fun <T> Iterable<T>.maxOf(selector: (T) -> Quantity<FltX>): Quantity<FltX> {
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

fun <T> Iterable<T>.maxOfOrNull(selector: (T) -> Quantity<FltX>): Quantity<FltX>? {
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

fun <T> Iterable<T>.minOf(selector: (T) -> Quantity<FltX>): Quantity<FltX> {
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

fun <T> Iterable<T>.minOfOrNull(selector: (T) -> Quantity<FltX>): Quantity<FltX>? {
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

fun <T> Iterable<T>.sortedBy(selector: (T) -> Quantity<FltX>): List<T> {
    return this.toList().sortedWith(compareBy { selector(it).toDouble() })
}

fun <T> Iterable<T>.sortedByDescending(selector: (T) -> Quantity<FltX>): List<T> {
    return this.toList().sortedWith(compareByDescending { selector(it).toDouble() })
}

fun <T> Iterable<T>.maxBy(selector: (T) -> Quantity<FltX>): T {
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

operator fun QuantityPoint2<FltX>.plus(offset: Point<Dim2, FltX>): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

operator fun QuantityPoint2<FltX>.minus(rhs: QuantityPoint2<FltX>): QuantityVector2<FltX> {
    return QuantityVector2(
        x = x - rhs.x,
        y = y - rhs.y
    )
}

infix fun QuantityPoint2<FltX>.eq(rhs: QuantityPoint2<FltX>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y
}

infix fun QuantityPoint2<FltX>.neq(rhs: QuantityPoint2<FltX>): Boolean = !(this eq rhs)

operator fun QuantityPoint2<FltX>.plus(offset: Vector<Dim2, FltX>): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit)
    )
}

operator fun QuantityPoint3<FltX>.plus(offset: Point<Dim3, FltX>): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

operator fun QuantityPoint3<FltX>.minus(rhs: QuantityPoint3<FltX>): QuantityVector3<FltX> {
    return QuantityVector3(
        x = x - rhs.x,
        y = y - rhs.y,
        z = z - rhs.z
    )
}

infix fun QuantityPoint3<FltX>.eq(rhs: QuantityPoint3<FltX>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y && this.z eq rhs.z
}

infix fun QuantityPoint3<FltX>.neq(rhs: QuantityPoint3<FltX>): Boolean = !(this eq rhs)

operator fun QuantityPoint3<FltX>.plus(offset: Vector<Dim3, FltX>): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = x + (offset[0] * x.unit),
        y = y + (offset[1] * y.unit),
        z = z + (offset[2] * z.unit)
    )
}

fun point2FltX(): QuantityPoint2<FltX> = QuantityPoint2(x = fltXZero() * Meter, y = fltXZero() * Meter)

fun point2FltX(
    x: FltX = fltXZero(),
    y: FltX = fltXZero(),
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return QuantityPoint2(x = x * unit, y = y * unit)
}

fun point2FltX(
    point: Point<Dim2, FltX>,
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return point2FltX(point[0], point[1], unit)
}

fun point3FltX(): QuantityPoint3<FltX> = QuantityPoint3(
    x = fltXZero() * Meter,
    y = fltXZero() * Meter,
    z = fltXZero() * Meter
)

fun point3FltX(
    x: FltX = fltXZero(),
    y: FltX = fltXZero(),
    z: FltX = fltXZero(),
    unit: PhysicalUnit = Meter
): QuantityPoint3<FltX> {
    return QuantityPoint3(x = x * unit, y = y * unit, z = z * unit)
}

fun point3FltX(
    point: Point<Dim3, FltX>,
    unit: PhysicalUnit = Meter
): QuantityPoint3<FltX> {
    return point3FltX(point[0], point[1], point[2], unit)
}

fun point3FltX(vector: Vector<Dim3, FltX>, unit: PhysicalUnit = Meter): QuantityPoint3<FltX> {
    return point3FltX(vector[0], vector[1], vector[2], unit)
}

fun vector2FltX(
    x: FltX = fltXZero(),
    y: FltX = fltXZero(),
    unit: PhysicalUnit = Meter
): QuantityVector2<FltX> {
    return QuantityVector2(x = x * unit, y = y * unit)
}

fun vector2FltX(vector: Vector<Dim2, FltX>, unit: PhysicalUnit = Meter): QuantityVector2<FltX> {
    return vector2FltX(vector[0], vector[1], unit)
}

fun vector3FltX(
    x: FltX = fltXZero(),
    y: FltX = fltXZero(),
    z: FltX = fltXZero(),
    unit: PhysicalUnit = Meter
): QuantityVector3<FltX> {
    return QuantityVector3(x = x * unit, y = y * unit, z = z * unit)
}

fun vector3FltX(vector: Vector<Dim3, FltX>, unit: PhysicalUnit = Meter): QuantityVector3<FltX> {
    return vector3FltX(vector[0], vector[1], vector[2], unit)
}
