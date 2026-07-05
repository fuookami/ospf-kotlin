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

/**
 * 抽象 2D 容器形状接口，提供二维容器的几何约束和空间计算。
 * Abstract 2D container shape interface providing geometric constraints and spatial calculations.
 */
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

    /**
     * 计算给定点偏移后的剩余空间。
     * Calculate the remaining space after the given point offset.
     * @param offset 偏移点 / the offset point
     * @return 剩余容器形状 / the remaining container shape
     */
    fun restSpace(offset: Point<Dim2, FltX>) = restSpace(point2FltX(offset))
    /**
     * 计算给定向量偏移后的剩余空间。
     * Calculate the remaining space after the given vector offset.
     * @param offset 偏移向量 / the offset vector
     * @return 剩余容器形状 / the remaining container shape
     */
    fun restSpace(offset: Vector<Dim2, FltX>) = restSpace(vector2FltX(offset))
}

/**
 * 2D 容器形状的具体实现。
 * Concrete implementation of a 2D container shape.
 */
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

/**
 * 2D 容器接口，表示具有平面的二维装箱容器。
 * 2D container interface representing a two-dimensional packing container with a plane.
 */
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

    /**
     * 获取指定单元的数量。
     * Get the amount of the specified unit.
     * @param unit 要查询的单元 / the unit to query
     * @return 单元数量 / the amount of the unit
     */
    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero
    /**
     * 计算满足谓词的单元总数。
     * Calculate the total amount of units matching the predicate.
     * @param predicate 谓词条件 / the predicate condition
     * @return 满足条件的单元总数 / total amount of matching units
     */
    fun amount(predicate: Predicate<AbstractCuboid<V>>): UInt64 =
        amounts.entries
            .asSequence()
            .filter { predicate(it.key) }
            .fold(UInt64.zero) { acc, entry -> acc + entry.value }
    /**
     * 检查是否包含指定单元。
     * Check if the container contains the specified unit.
     * @param unit 要检查的单元 / the unit to check
     * @return 是否包含该单元 / whether the unit is contained
     */
    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    /**
     * 检查是否包含满足谓词的单元。
     * Check if there is any unit matching the predicate.
     * @param predicate 谓词条件 / the predicate condition
     * @return 是否包含匹配的单元 / whether any matching unit exists
     */
    fun contains(predicate: Predicate<AbstractCuboid<V>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

/**
 * 抽象 3D 容器形状接口，提供三维容器的几何约束和空间计算。
 * Abstract 3D container shape interface providing geometric constraints and spatial calculations.
 */
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

    /**
     * 判断给定的包装形状在指定位置是否可被容器容纳。
     * Determine whether the given packing shape can be accommodated at the specified position.
     * @param shape 包装形状 / the packing shape
     * @param position 放置位置 / the placement position
     * @return 是否可容纳 / whether it can be accommodated
     */
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

    /**
     * 计算容器在给定方向和限制条件下最多能容纳的单元数量。
     * Calculate the maximum amount of the given unit that can fit in the container under the given orientation and limits.
     * @param unit 要放置的单元 / the unit to place
     * @param orientation 放置方向 / the placement orientation
     * @param maxXAmount X 轴方向最大数量限制 / maximum amount limit on X axis
     * @param maxYAmount Y 轴方向最大数量限制 / maximum amount limit on Y axis
     * @param maxZAmount Z 轴方向最大数量限制 / maximum amount limit on Z axis
     * @return 最大容纳数量 / the maximum amount that can fit
     */
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

    /**
     * 计算给定带量向量偏移后的剩余空间。
     * Calculate the remaining space after the given quantity vector offset.
     * @param offset 带量偏移向量 / the quantity offset vector
     * @return 剩余容器形状 / the remaining container shape
     */
    fun restSpace(offset: QuantityVector3<FltX>) = Container3Shape(
        width = width - offset.x,
        height = height - offset.y,
        depth = depth - offset.z
    )

    /**
     * 计算给定点偏移后的剩余空间。
     * Calculate the remaining space after the given point offset.
     * @param offset 偏移点 / the offset point
     * @return 剩余容器形状 / the remaining container shape
     */
    fun restSpace(offset: Point<Dim3, FltX>) = restSpace(point3FltX(offset))
    /**
     * 计算给定向量偏移后的剩余空间。
     * Calculate the remaining space after the given vector offset.
     * @param offset 偏移向量 / the offset vector
     * @return 剩余容器形状 / the remaining container shape
     */
    fun restSpace(offset: Vector<Dim3, FltX>) = restSpace(vector3FltX(offset))

    override fun partialEq(rhs: Container3Geometry<FltX>): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

/**
 * 3D 容器形状的数据类，包含宽、高、深三个维度。
 * Data class for 3D container shape with width, height and depth dimensions.
 */
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

/**
 * 3D 容器接口，表示具有体积的三维装箱容器。
 * 3D container interface representing a three-dimensional packing container with volume.
 */
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

    /**
     * 判断容器是否能够容纳指定方向和摆放的单元。
     * Determine whether the container can accommodate the specified unit with the given orientation.
     * @param unit 要检查的单元 / the unit to check
     * @param orientation 放置方向 / the placement orientation
     * @return 是否可容纳 / whether it can be accommodated
     */
    fun enabled(unit: AbstractCuboid<V>, orientation: Orientation = Orientation.Upright) = shape.enabled(unit, orientation)
    /**
     * 获取指定单元的数量。
     * Get the amount of the specified unit.
     * @param unit 要查询的单元 / the unit to query
     * @return 单元数量 / the amount of the unit
     */
    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero
    fun amount(predicate: Predicate<AbstractCuboid<V>>): UInt64 =
        amounts.entries
            .asSequence()
            .filter { predicate(it.key) }
            .fold(UInt64.zero) { acc, entry -> acc + entry.value }
    /**
     * 检查是否包含指定单元。
     * Check if the container contains the specified unit.
     * @param unit 要检查的单元 / the unit to check
     * @return 是否包含该单元 / whether the unit is contained
     */
    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
    /**
     * 检查是否包含满足谓词的单元。
     * Check if there is any unit matching the predicate.
     * @param predicate 谓词条件 / the predicate condition
     * @return 是否包含匹配的单元 / whether any matching unit exists
     */
    fun contains(predicate: Predicate<AbstractCuboid<V>>) = amounts.entries.any { predicate(it.key) && it.value != UInt64.zero }
}

/**
 * 3D 容器长方体单元，同时具备容器和长方体的特性。
 * 3D container cuboid unit that combines container and cuboid characteristics.
 */
interface Container3CuboidUnit<S, V> : Container3<S, V>, Cuboid<S, V> where S : Container3<S, V>, S : Cuboid<S, V>, V : FloatingNumber<V> {
    override val self: S
        get() = copy()
}
