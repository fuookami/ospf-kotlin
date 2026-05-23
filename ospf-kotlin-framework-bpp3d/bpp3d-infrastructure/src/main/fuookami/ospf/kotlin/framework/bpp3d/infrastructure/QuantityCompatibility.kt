@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
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
import fuookami.ospf.kotlin.quantities.quantity.times as quantityTimes
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.utils.functional.Order

private fun <V : FloatingNumber<V>> Quantity<V>.toScalar(unit: PhysicalUnit): V {
    return if (this.unit == unit) {
        this.value
    } else {
        this.convertTo(unit)?.value
            ?: throw IllegalArgumentException("Incompatible unit: ${this.unit} vs $unit")
    }
}

operator fun Flt64.times(unit: PhysicalUnit): QuantityFlt64 = Quantity(this, unit)

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityBinary(
    lhs: Quantity<V>,
    rhs: Quantity<V>,
    flt64Op: (Quantity<Flt64>, Quantity<Flt64>) -> Quantity<Flt64>,
    fltXOp: (Quantity<FltX>, Quantity<FltX>) -> Quantity<FltX>,
    symbol: String
): Quantity<V> {
    return when (lhs.value) {
        is Flt64 -> flt64Op(lhs as Quantity<Flt64>, rhs as Quantity<Flt64>) as Quantity<V>
        is FltX -> fltXOp(lhs as Quantity<FltX>, rhs as Quantity<FltX>) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '$symbol': ${lhs.value::class.simpleName}"
        )
    }
}

infix fun <V : FloatingNumber<V>> Quantity<V>.eq(rhs: Quantity<V>): Boolean = this.quantityEq(rhs)
infix fun <V : FloatingNumber<V>> Quantity<V>.neq(rhs: Quantity<V>): Boolean = !this.quantityEq(rhs)
infix fun <V : FloatingNumber<V>> Quantity<V>.leq(rhs: Quantity<V>): Boolean = this.quantityLeq(rhs) ?: false
infix fun <V : FloatingNumber<V>> Quantity<V>.geq(rhs: Quantity<V>): Boolean = this.quantityGeq(rhs) ?: false
infix fun <V : FloatingNumber<V>> Quantity<V>.ls(rhs: Quantity<V>): Boolean = this.quantityLs(rhs) ?: false
infix fun <V : FloatingNumber<V>> Quantity<V>.gr(rhs: Quantity<V>): Boolean = this.quantityGr(rhs) ?: false

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

operator fun <V : FloatingNumber<V>> Quantity<V>.plus(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityPlus(r) }, { l, r -> l.quantityPlus(r) }, "+")
}

operator fun <V : FloatingNumber<V>> Quantity<V>.minus(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityMinus(r) }, { l, r -> l.quantityMinus(r) }, "-")
}

operator fun <V : FloatingNumber<V>> Quantity<V>.times(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityTimes(r) }, { l, r -> l.quantityTimes(r) }, "*")
}

operator fun <V : FloatingNumber<V>> Quantity<V>.div(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityDiv(r) }, { l, r -> l.quantityDiv(r) }, "/")
}

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

infix fun <V : FloatingNumber<V>> Quantity<V>.ord(rhs: Quantity<V>): Order {
    return this.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity: ${this.unit} vs ${rhs.unit}")
}

fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return if (lhs gr rhs) lhs else rhs
}

fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return if (lhs leq rhs) lhs else rhs
}

fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
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

private fun <V : FloatingNumber<V>> quantityOrder(lhs: Quantity<V>, rhs: Quantity<V>): Order {
    return lhs.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity: ${lhs.unit} vs ${rhs.unit}")
}

private fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByQuantity(
    selector: (T) -> Quantity<V>,
    descending: Boolean
): List<T> {
    return this.toList().sortedWith { lhs, rhs ->
        val order = quantityOrder(selector(lhs), selector(rhs))
        when (order) {
            is Order.Less -> if (descending) 1 else -1
            is Order.Greater -> if (descending) -1 else 1
            Order.Equal -> 0
        }
    }
}

fun <T, V : FloatingNumber<V>> Iterable<T>.maxOf(selector: (T) -> Quantity<V>): Quantity<V> {
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

fun <T, V : FloatingNumber<V>> Iterable<T>.maxOfOrNull(selector: (T) -> Quantity<V>): Quantity<V>? {
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

fun <T, V : FloatingNumber<V>> Iterable<T>.minOf(selector: (T) -> Quantity<V>): Quantity<V> {
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

fun <T, V : FloatingNumber<V>> Iterable<T>.minOfOrNull(selector: (T) -> Quantity<V>): Quantity<V>? {
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

fun <T, V : FloatingNumber<V>> Iterable<T>.sortedBy(selector: (T) -> Quantity<V>): List<T> {
    return sortedByQuantity(selector, descending = false)
}

fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByDescending(selector: (T) -> Quantity<V>): List<T> {
    return sortedByQuantity(selector, descending = true)
}

fun <T, V : FloatingNumber<V>> Iterable<T>.maxBy(selector: (T) -> Quantity<V>): T {
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

operator fun <V : FloatingNumber<V>> QuantityPoint2G<V>.plus(offset: Point<Dim2, V>): QuantityPoint2G<V> {
    return QuantityPoint2G(
        x = x + Quantity(offset[0], x.unit),
        y = y + Quantity(offset[1], y.unit)
    )
}

operator fun <V : FloatingNumber<V>> QuantityPoint2G<V>.minus(rhs: QuantityPoint2G<V>): QuantityVector2G<V> {
    return QuantityVector2G(
        x = x - rhs.x,
        y = y - rhs.y
    )
}

infix fun <V : FloatingNumber<V>> QuantityPoint2G<V>.eq(rhs: QuantityPoint2G<V>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y
}

infix fun <V : FloatingNumber<V>> QuantityPoint2G<V>.neq(rhs: QuantityPoint2G<V>): Boolean = !(this eq rhs)

operator fun <V : FloatingNumber<V>> QuantityPoint2G<V>.plus(offset: Vector<Dim2, V>): QuantityPoint2G<V> {
    return QuantityPoint2G(
        x = x + Quantity(offset[0], x.unit),
        y = y + Quantity(offset[1], y.unit)
    )
}

operator fun <V : FloatingNumber<V>> QuantityPoint3G<V>.plus(offset: Point<Dim3, V>): QuantityPoint3G<V> {
    return QuantityPoint3G(
        x = x + Quantity(offset[0], x.unit),
        y = y + Quantity(offset[1], y.unit),
        z = z + Quantity(offset[2], z.unit)
    )
}

operator fun <V : FloatingNumber<V>> QuantityPoint3G<V>.minus(rhs: QuantityPoint3G<V>): QuantityVector3G<V> {
    return QuantityVector3G(
        x = x - rhs.x,
        y = y - rhs.y,
        z = z - rhs.z
    )
}

infix fun <V : FloatingNumber<V>> QuantityPoint3G<V>.eq(rhs: QuantityPoint3G<V>): Boolean {
    return this.x eq rhs.x && this.y eq rhs.y && this.z eq rhs.z
}

infix fun <V : FloatingNumber<V>> QuantityPoint3G<V>.neq(rhs: QuantityPoint3G<V>): Boolean = !(this eq rhs)

operator fun <V : FloatingNumber<V>> QuantityPoint3G<V>.plus(offset: Vector<Dim3, V>): QuantityPoint3G<V> {
    return QuantityPoint3G(
        x = x + Quantity(offset[0], x.unit),
        y = y + Quantity(offset[1], y.unit),
        z = z + Quantity(offset[2], z.unit)
    )
}

fun <V : FloatingNumber<V>> point2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityPoint2G<V> {
    return QuantityPoint2G(x = x, y = y)
}

fun point2(): QuantityPoint2 = QuantityPoint2G(x = Flt64.zero * Meter, y = Flt64.zero * Meter)

fun point2(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint2 {
    return QuantityPoint2G(x = x * unit, y = y * unit)
}

fun <V : FloatingNumber<V>> point2(
    point: Point<Dim2, V>,
    unit: PhysicalUnit = Meter
): QuantityPoint2G<V> {
    return point2(Quantity(point[0], unit), Quantity(point[1], unit))
}

fun <V : FloatingNumber<V>> point3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityPoint3G<V> {
    return QuantityPoint3G(x = x, y = y, z = z)
}

fun point3(): QuantityPoint3 = QuantityPoint3G(
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
    return QuantityPoint3G(x = x * unit, y = y * unit, z = z * unit)
}

fun <V : FloatingNumber<V>> point3(
    point: Point<Dim3, V>,
    unit: PhysicalUnit = Meter
): QuantityPoint3G<V> {
    return point3(Quantity(point[0], unit), Quantity(point[1], unit), Quantity(point[2], unit))
}

fun <V : FloatingNumber<V>> point3(
    vector: Vector<Dim3, V>,
    unit: PhysicalUnit = Meter
): QuantityPoint3G<V> {
    return point3(Quantity(vector[0], unit), Quantity(vector[1], unit), Quantity(vector[2], unit))
}

fun <V : FloatingNumber<V>> vector2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityVector2G<V> {
    return QuantityVector2G(x = x, y = y)
}

fun vector2(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityVector2 {
    return QuantityVector2G(x = x * unit, y = y * unit)
}

fun <V : FloatingNumber<V>> vector2(
    vector: Vector<Dim2, V>,
    unit: PhysicalUnit = Meter
): QuantityVector2G<V> {
    return vector2(Quantity(vector[0], unit), Quantity(vector[1], unit))
}

fun <V : FloatingNumber<V>> vector3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityVector3G<V> {
    return QuantityVector3G(x = x, y = y, z = z)
}

fun vector3(
    x: Flt64 = Flt64.zero,
    y: Flt64 = Flt64.zero,
    z: Flt64 = Flt64.zero,
    unit: PhysicalUnit = Meter
): QuantityVector3 {
    return QuantityVector3G(x = x * unit, y = y * unit, z = z * unit)
}

fun <V : FloatingNumber<V>> vector3(
    vector: Vector<Dim3, V>,
    unit: PhysicalUnit = Meter
): QuantityVector3G<V> {
    return vector3(Quantity(vector[0], unit), Quantity(vector[1], unit), Quantity(vector[2], unit))
}
