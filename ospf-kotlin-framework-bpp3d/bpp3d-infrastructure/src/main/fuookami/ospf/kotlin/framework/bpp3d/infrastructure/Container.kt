@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Predicate
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Dim2
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.Vector
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.div
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.gr
import fuookami.ospf.kotlin.quantities.quantity.leq
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter

private fun merge(
    counter: MutableMap<AbstractCuboid<InfraScalar>, UInt64>,
    unit: AbstractCuboid<InfraScalar>
) {
    when (unit) {
        is Container2<*, *> -> {
            for ((it, amount) in unit.amounts) {
                counter[it] = (counter[it] ?: UInt64.zero) + amount
            }
        }

        is Container3<*> -> {
            for ((it, amount) in unit.amounts) {
                counter[it] = (counter[it] ?: UInt64.zero) + amount
            }
        }

        else -> {
            counter[unit] = (counter[unit] ?: UInt64.zero) + UInt64.one
        }
    }
}

private fun <V : FloatingNumber<V>> maxQuantity(values: Iterable<Quantity<V>>): Quantity<V>? {
    var maximum: Quantity<V>? = null
    for (value in values) {
        maximum = if (maximum == null || (value gr maximum) == true) {
            value
        } else {
            maximum
        }
    }
    return maximum
}

private fun quantityWeightedSum(
    amounts: Map<AbstractCuboid<InfraScalar>, UInt64>,
    zero: Quantity<InfraScalar>,
    selector: (AbstractCuboid<InfraScalar>) -> Quantity<InfraScalar>
): Quantity<InfraScalar> {
    return amounts.asSequence().fold(zero) { acc, (unit, amount) ->
        acc + (selector(unit) * infraScalar(amount))
    }
}

interface AbstractContainer2Shape<P : ProjectivePlane> {
    val length: Quantity<InfraScalar>
    val width: Quantity<InfraScalar>
    val plane: P

    fun restSpace(offset: QuantityPoint2) = Container2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )

    fun restSpace(offset: QuantityVector2) = Container2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )

    fun restSpace(offset: Point<Dim2, InfraScalar>) = restSpace(point2(offset))
    fun restSpace(offset: Vector<Dim2, InfraScalar>) = restSpace(vector2(offset))
}

class Container2Shape<P : ProjectivePlane>(
    override val length: Quantity<InfraScalar> = infraInfinity() * Meter,
    override val width: Quantity<InfraScalar> = infraInfinity() * Meter,
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
        S : Container2<S, P>,
        P : ProjectivePlane
        > : Copyable<S> {
    val shape: AbstractContainer2Shape<P>
    val units: List<Placement2<*, P>>
    val amounts: Map<AbstractCuboid<InfraScalar>, UInt64> get() = count(units)

    val length: Quantity<InfraScalar> get() = shape.length
    val width: Quantity<InfraScalar> get() = shape.width

    companion object {
        fun <P : ProjectivePlane> count(units: List<Placement2<*, P>>): Map<AbstractCuboid<InfraScalar>, UInt64> {
            val counter = HashMap<AbstractCuboid<InfraScalar>, UInt64>()
            for (placement in units) {
                merge(counter, placement.unit)
            }
            return counter
        }
    }

    fun amount(unit: AbstractCuboid<InfraScalar>) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid<InfraScalar>>) = amounts.entries.filter { predicate(it.key) }.sumOf { it.value }
    fun contains(unit: AbstractCuboid<InfraScalar>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid<InfraScalar>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface AbstractContainer3Shape : Eq<AbstractContainer3Shape> {
    val width: Quantity<InfraScalar>
    val height: Quantity<InfraScalar>
    val depth: Quantity<InfraScalar>
    val volume: Quantity<InfraScalar> get() = width * height * depth

    fun enabled(
        unit: AbstractCuboid<InfraScalar>,
        orientation: Orientation = Orientation.Upright
    ): Boolean {
        return (width geq orientation.width(unit)) == true
                && (height geq orientation.height(unit)) == true
                && (depth geq orientation.depth(unit)) == true
    }

    fun enabled(unit: Placement3<*>): Boolean {
        if ((unit.maxX gr width) == true) {
            return false
        }
        if ((unit.maxY gr height) == true) {
            return false
        }
        if ((unit.maxZ gr depth) == true) {
            return false
        }
        return true
    }

    fun enabled(units: List<Placement3<*>>): Boolean {
        val maxX = maxQuantity(units.map { it.maxX })
        if (maxX != null && (maxX gr width) == true) {
            return false
        }
        val maxY = maxQuantity(units.map { it.maxY })
        if (maxY != null && (maxY gr height) == true) {
            return false
        }
        val maxZ = maxQuantity(units.map { it.maxZ })
        if (maxZ != null && (maxZ gr depth) == true) {
            return false
        }
        return true
    }

    fun maxAmount(
        unit: AbstractCuboid<InfraScalar>,
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

    fun restSpace(offset: QuantityPoint3) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    fun restSpace(offset: QuantityVector3) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    fun restSpace(offset: Point<Dim3, InfraScalar>) = restSpace(point3(offset))
    fun restSpace(offset: Vector<Dim3, InfraScalar>) = restSpace(vector3(offset))

    override fun partialEq(rhs: AbstractContainer3Shape): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

data class Container3Shape(
    override val width: Quantity<InfraScalar> = infraInfinity() * Meter,
    override val height: Quantity<InfraScalar> = infraInfinity() * Meter,
    override val depth: Quantity<InfraScalar> = infraInfinity() * Meter
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

        operator fun invoke(container: Container2<*, *>) = this(container.shape)
        operator fun invoke(space: AbstractContainer3Shape) = Container3Shape(space.width, space.height, space.depth)
        operator fun invoke(container: Container3<*>) = this(container.shape)
    }
}

interface Container3<S : Container3<S>> : AbstractCuboid<InfraScalar>, Copyable<S> {
    val shape: AbstractContainer3Shape get() = Container3Shape()
    val units: List<Placement3<*>>
    val amounts: Map<AbstractCuboid<InfraScalar>, UInt64> get() = count(units)

    override val width: Quantity<InfraScalar> get() = shape.width
    override val height: Quantity<InfraScalar> get() = shape.height
    override val depth: Quantity<InfraScalar> get() = shape.depth

    override val weight
        get() = quantityWeightedSum(
            amounts = amounts,
            zero = infraZero() * Kilogram
        ) { it.weight }
    override val volume get() = depth * height * width
    override val actualVolume: Quantity<InfraScalar>
        get() = quantityWeightedSum(
            amounts = amounts,
            zero = volume * infraZero()
        ) { it.actualVolume }
    val loadingRate: InfraScalar get() = (actualVolume / (volume + (infraEpsilon() * volume.unit))).value

    companion object {
        fun count(units: List<Placement3<*>>): Map<AbstractCuboid<InfraScalar>, UInt64> {
            val counter = HashMap<AbstractCuboid<InfraScalar>, UInt64>()
            for (placement in units) {
                merge(counter, placement.unit)
            }
            return counter
        }
    }

    fun enabled(unit: AbstractCuboid<InfraScalar>, orientation: Orientation = Orientation.Upright) = shape.enabled(unit, orientation)
    fun amount(unit: AbstractCuboid<InfraScalar>) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid<InfraScalar>>) = amounts.entries.filter { predicate(it.key) }.sumOf { it.value }
    fun contains(unit: AbstractCuboid<InfraScalar>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid<InfraScalar>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface Container3CuboidUnit<S> : Container3<S>, Cuboid<S> where S : Container3<S>, S : Cuboid<S> {
    override val self: S
        get() = copy()
}


