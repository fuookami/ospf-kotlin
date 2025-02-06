package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*

private fun merge(
    counter: MutableMap<AbstractCuboid, UInt64>,
    unit: AbstractCuboid
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

interface AbstractContainer2Shape<P : ProjectivePlane> {
    val length: Flt64
    val width: Flt64
    val plane: P

    fun restSpace(offset: Point2) = Container2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )

    fun restSpace(offset: Vector2) = Container2Shape(
        length = length - offset.x,
        width = width - offset.y,
        plane = plane
    )
}

class Container2Shape<P : ProjectivePlane>(
    override val length: Flt64 = Flt64.infinity,
    override val width: Flt64 = Flt64.infinity,
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
    val amounts: Map<AbstractCuboid, UInt64> get() = count(units)

    val length: Flt64 get() = shape.length
    val width: Flt64 get() = shape.width

    companion object {
        fun <P : ProjectivePlane> count(units: List<Placement2<*, P>>): Map<AbstractCuboid, UInt64> {
            val counter = HashMap<AbstractCuboid, UInt64>()
            for (placement in units) {
                merge(counter, placement.unit)
            }
            return counter
        }
    }

    fun amount(unit: AbstractCuboid) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid>) = amounts.entries.filter { predicate(it.key) }.sumOf { it.value }
    fun contains(unit: AbstractCuboid) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface AbstractContainer3Shape: Eq<AbstractContainer3Shape> {
    val width: Flt64
    val height: Flt64
    val depth: Flt64
    val volume: Flt64 get() = width * height * depth

    fun enabled(
        unit: AbstractCuboid,
        orientation: Orientation = Orientation.Upright
    ): Boolean {
        return width geq orientation.width(unit)
                && height geq orientation.height(unit)
                && depth geq orientation.depth(unit)
    }

    fun enabled(unit: Placement3<*>): Boolean {
        if (unit.maxX gr width) {
            return false
        }
        if (unit.maxY gr height) {
            return false
        }
        if (unit.maxZ gr depth) {
            return false
        }
        return true
    }

    fun enabled(units: List<Placement3<*>>): Boolean {
        if ((units.maxOfOrNull { it.maxX } ?: Flt64.zero) gr width) {
            return false
        }
        if ((units.maxOfOrNull { it.maxY } ?: Flt64.zero) gr height) {
            return false
        }
        if ((units.maxOfOrNull { it.maxZ } ?: Flt64.zero) gr depth) {
            return false
        }
        return true
    }

    fun maxAmount(
        unit: AbstractCuboid,
        orientation: Orientation = Orientation.Upright,
        maxXAmount: UInt64 = UInt64.maximum,
        maxYAmount: UInt64 = UInt64.maximum,
        maxZAmount: UInt64 = UInt64.maximum
    ): UInt64 {
        val xAmount = min(
            (width / orientation.width(unit)).floor().toUInt64(),
            maxXAmount
        )
        val yAmount = min(
            (height / orientation.height(unit)).floor().toUInt64(),
            maxYAmount
        )
        val zAmount = min(
            (depth / orientation.depth(unit)).floor().toUInt64(),
            maxZAmount
        )
        return xAmount * yAmount * zAmount
    }

    fun restSpace(offset: Point3) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    fun restSpace(offset: Vector3) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    override fun partialEq(rhs: AbstractContainer3Shape): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

data class Container3Shape(
    override val width: Flt64 = Flt64.infinity,
    override val height: Flt64 = Flt64.infinity,
    override val depth: Flt64 = Flt64.infinity
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

interface Container3<S : Container3<S>> : AbstractCuboid, Copyable<S> {
    val shape: AbstractContainer3Shape get() = Container3Shape()
    val units: List<Placement3<*>>
    val amounts: Map<AbstractCuboid, UInt64> get() = count(units)

    override val width: Flt64 get() = shape.width
    override val height: Flt64 get() = shape.height
    override val depth: Flt64 get() = shape.depth

    override val weight get() = amounts.asSequence().sumOf { it.key.weight * it.value.toFlt64() }
    override val volume get() = depth * height * width
    override val actualVolume: Flt64 get() = amounts.asSequence().sumOf { it.key.actualVolume * it.value.toFlt64() }
    val loadingRate: Flt64 get() = actualVolume / (volume + Flt64.epsilon)

    companion object {
        fun count(units: List<Placement3<*>>): Map<AbstractCuboid, UInt64> {
            val counter = HashMap<AbstractCuboid, UInt64>()
            for (placement in units) {
                merge(counter, placement.unit)
            }
            return counter
        }
    }

    fun enabled(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright) = shape.enabled(unit, orientation)
    fun amount(unit: AbstractCuboid) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid>) = amounts.entries.filter { predicate(it.key) }.sumOf { it.value }
    fun contains(unit: AbstractCuboid) = amounts[unit]?.let { it != UInt64.zero } ?: false
    fun contains(predicate: Predicate<AbstractCuboid>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

interface Container3CuboidUnit<S> : Container3<S>, Cuboid<S> where S : Container3<S>, S : Cuboid<S>
