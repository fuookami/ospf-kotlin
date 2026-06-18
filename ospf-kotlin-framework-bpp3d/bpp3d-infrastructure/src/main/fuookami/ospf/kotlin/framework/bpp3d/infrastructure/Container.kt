/**
 * 容器基础设施。
 * Container infrastructure.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

private fun <V : FloatingNumber<V>> quantityWeightedSumByValue(
    amounts: Map<AbstractCuboid<V>, UInt64>,
    zero: Quantity<V>,
    selector: (AbstractCuboid<V>) -> Quantity<V>
): Quantity<V> {
    return amounts.asSequence().fold(zero) { acc, (unit, amount) ->
        quantityPlusByValue(acc, quantityScaleByValue(selector(unit), amount).value!!)
    }
}

@Suppress("UNCHECKED_CAST")
private fun <V : FloatingNumber<V>> merge(
    counter: MutableMap<AbstractCuboid<V>, UInt64>,
    unit: AbstractCuboid<V>
) {
    when (unit) {
        is Container2<*, *, *> -> {
            for ((it, amount) in unit.amounts) {
                val key = it as AbstractCuboid<V>
                counter[key] = (counter[key] ?: UInt64.zero) + amount
            }
        }

        is Container3<*, *> -> {
            for ((it, amount) in unit.amounts) {
                val key = it as AbstractCuboid<V>
                counter[key] = (counter[key] ?: UInt64.zero) + amount
            }
        }

        else -> {
            counter[unit] = (counter[unit] ?: UInt64.zero) + UInt64.one
        }
    }
}

private fun <V : FloatingNumber<V>, P : ProjectivePlane> count2(units: List<QuantityPlacement2<*, V, P>>): Map<AbstractCuboid<V>, UInt64> {
    val counter = HashMap<AbstractCuboid<V>, UInt64>()
    for (placement in units) {
        merge(counter, placement.unit)
    }
    return counter
}

private fun <V : FloatingNumber<V>> count3(units: List<QuantityPlacement3<*, V>>): Map<AbstractCuboid<V>, UInt64> {
    val counter = HashMap<AbstractCuboid<V>, UInt64>()
    for (placement in units) {
        merge(counter, placement.unit)
    }
    return counter
}

interface AbstractContainer2Shape<P : ProjectivePlane> : Container2Geometry<P, FltX> {
    override val length: Quantity<FltX>
    override val width: Quantity<FltX>
    override val plane: P

    override fun restSpace(offset: QuantityPoint2<FltX>): QuantityContainer2Shape<P, FltX> = QuantityContainer2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )

    override fun restSpace(offset: QuantityVector2<FltX>): QuantityContainer2Shape<P, FltX> = QuantityContainer2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )

    fun restSpace(offset: Point<Dim2, FltX>) = restSpace(point2FltX(offset))
    fun restSpace(offset: Vector<Dim2, FltX>) = restSpace(vector2FltX(offset))
}

class Container2Shape<P : ProjectivePlane>(
    override val length: Quantity<FltX> = FltX.maximum * Meter,
    override val width: Quantity<FltX> = FltX.maximum * Meter,
    override val plane: P
) : AbstractContainer2Shape<P> {
    companion object {
        operator fun <P : ProjectivePlane> invoke(
            space: AbstractContainer3Shape,
            plane: P
        ) = Container2Shape(
            length = plane.length(space),
            width = plane.width(space),
            plane = plane
        )
    }
}

interface Container2<
        S : Container2<S, V, P>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        > : Copyable<S> {
    val shape: Container2Geometry<P, V>
    val units: List<QuantityPlacement2<*, V, P>>
    val amounts: Map<AbstractCuboid<V>, UInt64> get() = count(units)

    val length: Quantity<V> get() = shape.length
    val width: Quantity<V> get() = shape.width

    companion object {
        fun <V : FloatingNumber<V>, P : ProjectivePlane> count(units: List<QuantityPlacement2<*, V, P>>): Map<AbstractCuboid<V>, UInt64> {
            return count2(units)
        }
    }

    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid<V>>): UInt64 =
        amounts.entries
            .asSequence()
            .filter { predicate(it.key) }
            .fold(UInt64.zero) { acc, entry -> acc + entry.value }
    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid<V>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface AbstractContainer3Shape : Container3Geometry<FltX> {
    override val width: Quantity<FltX>
    override val height: Quantity<FltX>
    override val depth: Quantity<FltX>
    override val volume: Quantity<FltX> get() = width * height * depth

    override fun enabled(
        unit: AbstractCuboid<FltX>,
        orientation: Orientation
    ): Boolean {
        return (width geq orientation.width(unit)) == true
                && (height geq orientation.height(unit)) == true
                && (depth geq orientation.depth(unit)) == true
    }

    override fun enabled(unit: QuantityPlacement3<*, FltX>): Boolean {
        return (width geq unit.maxX) == true
                && (height geq unit.maxY) == true
                && (depth geq unit.maxZ) == true
    }

    fun enabled(
        shape: PackingShape3<FltX>,
        position: QuantityPoint3<FltX>
    ): Boolean {
        val x = position.x
        val y = position.y
        val z = position.z

        val withinLowerBound = (x geq (FltX.zero * x.unit)) == true
                && (y geq (FltX.zero * y.unit)) == true
                && (z geq (FltX.zero * z.unit)) == true
        if (!withinLowerBound) {
            return false
        }

        return when (shape) {
            is CylinderPackingShape3 -> {
                if (shape.axis == Axis3.Y) {
                    val centerX = x + shape.radius
                    val centerZ = z + shape.radius
                    val left = centerX - shape.radius
                    val right = centerX + shape.radius
                    val front = centerZ - shape.radius
                    val back = centerZ + shape.radius
                    (left geq (FltX.zero * left.unit)) == true
                            && (front geq (FltX.zero * front.unit)) == true
                            && (width geq right) == true
                            && (depth geq back) == true
                            && (height geq (y + shape.boundingHeight)) == true
                } else {
                    (width geq (x + shape.boundingWidth)) == true
                            && (height geq (y + shape.boundingHeight)) == true
                            && (depth geq (z + shape.boundingDepth)) == true
                }
            }

            else -> {
                (width geq (x + shape.boundingWidth)) == true
                        && (height geq (y + shape.boundingHeight)) == true
                        && (depth geq (z + shape.boundingDepth)) == true
            }
        }
    }

    override fun enabled(units: List<QuantityPlacement3<*, FltX>>): Boolean {
        return (units.maxOfOrNullQuantity { it.maxX }?.let { width geq it } ?: true) == true
                && (units.maxOfOrNullQuantity { it.maxY }?.let { height geq it } ?: true) == true
                && (units.maxOfOrNullQuantity { it.maxZ }?.let { depth geq it } ?: true) == true
    }

    fun maxAmount(
        unit: AbstractCuboid<FltX>,
        orientation: Orientation = Orientation.Upright,
        maxXAmount: UInt64 = UInt64.maximum,
        maxYAmount: UInt64 = UInt64.maximum,
        maxZAmount: UInt64 = UInt64.maximum
    ): UInt64 {
        val xAmount = min(
            (width / orientation.width(unit)).value.floor().toUInt64(),
            maxXAmount
        )
        val yAmount = min(
            (height / orientation.height(unit)).value.floor().toUInt64(),
            maxYAmount
        )
        val zAmount = min(
            (depth / orientation.depth(unit)).value.floor().toUInt64(),
            maxZAmount
        )
        return xAmount * yAmount * zAmount
    }

    override fun restSpace(offset: QuantityPoint3<FltX>): QuantityContainer3Shape<FltX> = QuantityContainer3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    fun restSpace(offset: QuantityVector3<FltX>) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    fun restSpace(offset: Point<Dim3, FltX>) = restSpace(point3FltX(offset))
    fun restSpace(offset: Vector<Dim3, FltX>) = restSpace(vector3FltX(offset))

    override fun partialEq(rhs: Container3Geometry<FltX>): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

data class Container3Shape(
    override val width: Quantity<FltX> = FltX.maximum * Meter,
    override val height: Quantity<FltX> = FltX.maximum * Meter,
    override val depth: Quantity<FltX> = FltX.maximum * Meter
) : AbstractContainer3Shape {
    companion object {
        operator fun invoke(space: AbstractContainer2Shape<*>): Container3Shape {
            return when (space.plane) {
                Bottom -> {
                    Container3Shape(width = space.width, depth = space.length)
                }

                Side -> {
                    Container3Shape(width = space.length, height = space.width)
                }

                Front -> {
                    Container3Shape(width = space.length, height = space.width)
                }
            }
        }

        operator fun invoke(container: Container2<*, FltX, *>) = this(container.shape as AbstractContainer2Shape<*>)
        operator fun invoke(space: AbstractContainer3Shape) = Container3Shape(space.width, space.height, space.depth)
        operator fun invoke(container: Container3<*, FltX>) = this(container.shape as AbstractContainer3Shape)
    }
}

interface Container3<
        S : Container3<S, V>,
        V : FloatingNumber<V>
        > : AbstractCuboid<V>, Copyable<S> {
    val shape: Container3Geometry<V>
    val units: List<QuantityPlacement3<*, V>>
    val amounts: Map<AbstractCuboid<V>, UInt64> get() = count(units)

    override val width: Quantity<V> get() = shape.width
    override val height: Quantity<V> get() = shape.height
    override val depth: Quantity<V> get() = shape.depth

    override val weight
        get() = quantityWeightedSumByValue(
            amounts = amounts,
            zero = Quantity(quantityZeroByValue(width).value, Kilogram)
        ) { it.weight }
    override val volume get() = quantityTimesByValue(quantityTimesByValue(depth, height), width)
    override val actualVolume: Quantity<V>
        get() = quantityWeightedSumByValue(
            amounts = amounts,
            zero = quantityZeroByValue(volume)
        ) { it.actualVolume }
    val loadingRate: V get() = quantityRatioByValue(
        actualVolume,
        quantityPlusByValue(
            volume,
            quantityScaleByFltXValue(volume, FltX.epsilon).value!!
        )
    ).value!!

    companion object {
        fun <V : FloatingNumber<V>> count(units: List<QuantityPlacement3<*, V>>): Map<AbstractCuboid<V>, UInt64> {
            return count3(units)
        }
    }

    fun enabled(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright) = shape.enabled(unit, orientation)
    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid<V>>): UInt64 =
        amounts.entries
            .asSequence()
            .filter { predicate(it.key) }
            .fold(UInt64.zero) { acc, entry -> acc + entry.value }
    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid<V>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface Container3CuboidUnit<S, V> : Container3<S, V>, Cuboid<S, V> where S : Container3<S, V>, S : Cuboid<S, V>, V : FloatingNumber<V> {
    override val self: S
        get() = copy()
}
