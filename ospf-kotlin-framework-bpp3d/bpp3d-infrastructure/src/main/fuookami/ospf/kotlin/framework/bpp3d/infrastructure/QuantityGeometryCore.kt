/**
 * 泛型量纲几何基础设施。
 * Quantity geometry infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
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

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> quantityBinary(
    lhs: Quantity<V>,
    rhs: Quantity<V>,
    op: (Quantity<FltX>, Quantity<FltX>) -> Quantity<FltX>,
    symbol: String
): Quantity<V> {
    return when (lhs.value) {
        is FltX -> op(lhs as Quantity<FltX>, rhs as Quantity<FltX>) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '$symbol': ${lhs.value::class.simpleName}"
        )
    }
}

internal fun <V : FloatingNumber<V>> quantityPlusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityPlus(r) }, "+")
}

internal fun <V : FloatingNumber<V>> quantityMinusByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityMinus(r) }, "-")
}

internal fun <V : FloatingNumber<V>> quantityTimesByValue(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(lhs, rhs, { l, r -> l.quantityTimes(r) }, "*")
}

internal fun <V : FloatingNumber<V>> quantityZeroByValue(sample: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(sample, sample)
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByValue(
    quantity: Quantity<V>,
    scale: UInt64
): Quantity<V> {
    return when (quantity.value) {
        is FltX -> ((quantity as Quantity<FltX>).quantityTimes(FltX(scale.toULong().toDouble()))) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityScaleByFltXValue(
    quantity: Quantity<V>,
    scale: FltX
): Quantity<V> {
    return when (quantity.value) {
        is FltX -> ((quantity as Quantity<FltX>).quantityTimes(scale)) as Quantity<V>
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '*': ${quantity.value::class.simpleName}"
        )
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <V : FloatingNumber<V>> quantityRatioByValue(lhs: Quantity<V>, rhs: Quantity<V>): V {
    return when (lhs.value) {
        is FltX -> ((lhs as Quantity<FltX>).quantityDiv(rhs as Quantity<FltX>).value) as V
        else -> throw IllegalArgumentException(
            "Unsupported quantity numeric type for '/': ${lhs.value::class.simpleName}"
        )
    }
}

internal fun <V : FloatingNumber<V>> repeatedQuantitySumByValue(
    sample: Quantity<V>,
    times: UInt64
): Quantity<V> {
    var acc = quantityZeroByValue(sample)
    var i = UInt64.zero
    while (i < times) {
        acc = quantityPlusByValue(acc, sample)
        i += UInt64.one
    }
    return acc
}

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

operator fun <V : FloatingNumber<V>> Quantity<V>.plus(rhs: Quantity<V>): Quantity<V> {
    return quantityPlusByValue(this, rhs)
}

operator fun <V : FloatingNumber<V>> Quantity<V>.minus(rhs: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(this, rhs)
}

operator fun <V : FloatingNumber<V>> Quantity<V>.times(rhs: Quantity<V>): Quantity<V> {
    return quantityTimesByValue(this, rhs)
}

operator fun <V : FloatingNumber<V>> Quantity<V>.div(rhs: Quantity<V>): Quantity<V> {
    return quantityBinary(this, rhs, { l, r -> l.quantityDiv(r) }, "/")
}

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

fun Quantity<FltX>.abs(): Quantity<FltX> {
    return if (this.value >= FltX.zero) {
        this
    } else {
        (-this.value) * this.unit
    }
}

fun <T, V : FloatingNumber<V>> Iterable<T>.sumOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var sum = selector(iterator.next())
    while (iterator.hasNext()) {
        sum = quantityPlusByValue(sum, selector(iterator.next()))
    }
    return sum
}

fun <T, V : FloatingNumber<V>> Iterable<T>.maxOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "max") is Order.Greater) {
            best = current
        }
    }
    return best
}

fun <T, V : FloatingNumber<V>> Iterable<T>.maxOfOrNullQuantity(selector: (T) -> Quantity<V>): Quantity<V>? {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "max") is Order.Greater) {
            best = current
        }
    }
    return best
}

fun <T, V : FloatingNumber<V>> Iterable<T>.minOfQuantity(selector: (T) -> Quantity<V>): Quantity<V> {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "min") is Order.Less) {
            best = current
        }
    }
    return best
}

fun <T, V : FloatingNumber<V>> Iterable<T>.minOfOrNullQuantity(selector: (T) -> Quantity<V>): Quantity<V>? {
    val iterator = iterator()
    if (!iterator.hasNext()) {
        return null
    }
    var best = selector(iterator.next())
    while (iterator.hasNext()) {
        val current = selector(iterator.next())
        if (quantityOrd(current, best, "min") is Order.Less) {
            best = current
        }
    }
    return best
}

fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        quantityOrd(selector(lhs), selector(rhs), "sort").value
    }
}

fun <T, V : FloatingNumber<V>> Iterable<T>.sortedByDescendingQuantity(selector: (T) -> Quantity<V>): List<T> {
    return sortedWith { lhs, rhs ->
        quantityOrd(selector(rhs), selector(lhs), "sort").value
    }
}

fun <T, V : FloatingNumber<V>> Iterable<T>.maxByQuantity(selector: (T) -> Quantity<V>): T {
    val iterator = iterator()
    require(iterator.hasNext()) { "Collection is empty." }
    var bestItem = iterator.next()
    var bestValue = selector(bestItem)
    while (iterator.hasNext()) {
        val item = iterator.next()
        val value = selector(item)
        if (quantityOrd(value, bestValue, "max") is Order.Greater) {
            bestValue = value
            bestItem = item
        }
    }
    return bestItem
}

data class QuantityVector2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y)
        )
    }

    operator fun minus(rhs: QuantityVector2<V>): QuantityVector2<V> {
        return QuantityVector2(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y)
        )
    }
}

data class QuantityVector3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityPlusByValue(x, rhs.x),
            y = quantityPlusByValue(y, rhs.y),
            z = quantityPlusByValue(z, rhs.z)
        )
    }

    operator fun minus(rhs: QuantityVector3<V>): QuantityVector3<V> {
        return QuantityVector3(
            x = quantityMinusByValue(x, rhs.x),
            y = quantityMinusByValue(y, rhs.y),
            z = quantityMinusByValue(z, rhs.z)
        )
    }
}

data class QuantityPoint2<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>
) {
    operator fun plus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y)
        )
    }

    operator fun minus(offset: QuantityVector2<V>): QuantityPoint2<V> {
        return QuantityPoint2(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y)
        )
    }

    infix fun ord(rhs: QuantityPoint2<V>): Order {
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class QuantityPoint3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>
) {
    operator fun plus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityPlusByValue(x, offset.x),
            y = quantityPlusByValue(y, offset.y),
            z = quantityPlusByValue(z, offset.z)
        )
    }

    operator fun minus(offset: QuantityVector3<V>): QuantityPoint3<V> {
        return QuantityPoint3(
            x = quantityMinusByValue(x, offset.x),
            y = quantityMinusByValue(y, offset.y),
            z = quantityMinusByValue(z, offset.z)
        )
    }

    infix fun ord(rhs: QuantityPoint3<V>): Order {
        when (val zOrder = quantityOrd(z, rhs.z, "z")) {
            Order.Equal -> {}
            else -> return zOrder
        }
        when (val yOrder = quantityOrd(y, rhs.y, "y")) {
            Order.Equal -> {}
            else -> return yOrder
        }
        return quantityOrd(x, rhs.x, "x")
    }
}

data class Rectangle2<V : FloatingNumber<V>>(
    val minX: Quantity<V>,
    val minY: Quantity<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    init {
        require(quantityOrd(minX, maxX, "x") !is Order.Greater) { "minX should be <= maxX" }
        require(quantityOrd(minY, maxY, "y") !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: Quantity<V> get() = quantityMinusByValue(maxX, minX)
    val height: Quantity<V> get() = quantityMinusByValue(maxY, minY)
    val area: Quantity<V> get() = quantityTimesByValue(width, height)

    fun intersect(rhs: Rectangle2<V>): Rectangle2<V>? {
        val left = quantityMax(minX, rhs.minX, "x")
        val right = quantityMin(maxX, rhs.maxX, "x")
        val bottom = quantityMax(minY, rhs.minY, "y")
        val top = quantityMin(maxY, rhs.maxY, "y")
        return if (quantityOrd(left, right, "x") is Order.Less
            && quantityOrd(bottom, top, "y") is Order.Less
        ) {
            Rectangle2(left, bottom, right, top)
        } else {
            null
        }
    }

    fun intersectArea(rhs: Rectangle2<V>): Quantity<V>? {
        return intersect(rhs)?.area
    }
}

fun <V : FloatingNumber<V>> point2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityPoint2<V> {
    return QuantityPoint2(x = x, y = y)
}

fun <V : FloatingNumber<V>> point3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityPoint3<V> {
    return QuantityPoint3(x = x, y = y, z = z)
}

fun <V : FloatingNumber<V>> vector2(
    x: Quantity<V>,
    y: Quantity<V>
): QuantityVector2<V> {
    return QuantityVector2(x = x, y = y)
}

fun <V : FloatingNumber<V>> vector3(
    x: Quantity<V>,
    y: Quantity<V>,
    z: Quantity<V>
): QuantityVector3<V> {
    return QuantityVector3(x = x, y = y, z = z)
}

fun point2FltX(): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = Quantity(FltX.zero, Meter),
        y = Quantity(FltX.zero, Meter)
    )
}

fun point2FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return QuantityPoint2(
        x = Quantity(x, unit),
        y = Quantity(y, unit)
    )
}

fun point2FltX(
    point: Point<Dim2, FltX>,
    unit: PhysicalUnit = Meter
): QuantityPoint2<FltX> {
    return point2FltX(point[0], point[1], unit)
}

fun point3FltX(): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = Quantity(FltX.zero, Meter),
        y = Quantity(FltX.zero, Meter),
        z = Quantity(FltX.zero, Meter)
    )
}

fun point3FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityPoint3<FltX> {
    return QuantityPoint3(
        x = Quantity(x, unit),
        y = Quantity(y, unit),
        z = Quantity(z, unit)
    )
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
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityVector2<FltX> {
    return QuantityVector2(
        x = Quantity(x, unit),
        y = Quantity(y, unit)
    )
}

fun vector2FltX(vector: Vector<Dim2, FltX>, unit: PhysicalUnit = Meter): QuantityVector2<FltX> {
    return vector2FltX(vector[0], vector[1], unit)
}

fun vector3FltX(
    x: FltX = FltX.zero,
    y: FltX = FltX.zero,
    z: FltX = FltX.zero,
    unit: PhysicalUnit = Meter
): QuantityVector3<FltX> {
    return QuantityVector3(
        x = Quantity(x, unit),
        y = Quantity(y, unit),
        z = Quantity(z, unit)
    )
}

fun vector3FltX(vector: Vector<Dim3, FltX>, unit: PhysicalUnit = Meter): QuantityVector3<FltX> {
    return vector3FltX(vector[0], vector[1], vector[2], unit)
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

internal fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.quantityPartialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

infix fun <V : FloatingNumber<V>> Quantity<V>.ord(rhs: Quantity<V>): Order {
    return quantityOrd(this, rhs, "quantity")
}

fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityMax(lhs, rhs, "max")
}

fun <V : FloatingNumber<V>> max(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
    var current = max(lhs, rhs)
    for (value in rest) {
        current = max(current, value)
    }
    return current
}

fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    return quantityMin(lhs, rhs, "min")
}

fun <V : FloatingNumber<V>> min(lhs: Quantity<V>, rhs: Quantity<V>, vararg rest: Quantity<V>): Quantity<V> {
    var current = min(lhs, rhs)
    for (value in rest) {
        current = min(current, value)
    }
    return current
}

internal fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

internal fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}
