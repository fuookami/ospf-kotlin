@file:Suppress("DEPRECATION")

/**
 * 泛型容器核心实现。
 * Quantity container core implementation.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3
import fuookami.ospf.kotlin.math.geometry.QuantityCuboid3View
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.eq
import fuookami.ospf.kotlin.quantities.quantity.geq
import fuookami.ospf.kotlin.quantities.quantity.minus
import fuookami.ospf.kotlin.quantities.quantity.plus
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram

private fun <V : FloatingNumber<V>> quantityTimesByValue(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) * (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) * (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> quantityMinusByValue(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) - (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) - (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> quantityPlusByValue(
    lhs: Quantity<V>,
    rhs: Quantity<V>
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (lhs.value) {
        is InfraNumber -> ((lhs as Quantity<InfraNumber>) + (rhs as Quantity<InfraNumber>)) as Quantity<V>
        is FltX -> ((lhs as Quantity<FltX>) + (rhs as Quantity<FltX>)) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${lhs.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> quantityZeroByValue(sample: Quantity<V>): Quantity<V> {
    return quantityMinusByValue(sample, sample)
}

private fun <V : FloatingNumber<V>> quantityScaleByValue(
    quantity: Quantity<V>,
    scale: UInt64
): Quantity<V> {
    @Suppress("UNCHECKED_CAST")
    return when (quantity.value) {
        is InfraNumber -> ((quantity as Quantity<InfraNumber>) * infraScalar(scale)) as Quantity<V>
        is FltX -> ((quantity as Quantity<FltX>) * FltX(scale.toULong().toString())) as Quantity<V>
        else -> throw IllegalArgumentException("Unsupported quantity scalar type: ${quantity.value::class.simpleName}")
    }
}

private fun <V : FloatingNumber<V>> quantityWeightedSumByValue(
    amounts: Map<AbstractCuboid<V>, UInt64>,
    zero: Quantity<V>,
    selector: (AbstractCuboid<V>) -> Quantity<V>
): Quantity<V> {
    return amounts.asSequence().fold(zero) { acc, (unit, amount) ->
        quantityPlusByValue(acc, quantityScaleByValue(selector(unit), amount))
    }
}

private fun <V : FloatingNumber<V>> maxQuantityByValue(values: Iterable<Quantity<V>>): Quantity<V>? {
    var maximum: Quantity<V>? = null
    for (value in values) {
        maximum = if (maximum == null || (value geq maximum) == true) {
            value
        } else {
            maximum
        }
    }
    return maximum
}

/**
 * 泛型 3D 容器形状接口。
 * Quantity 3D container shape interface.
 *
 * @param V 数值类型 / Numeric scalar type
 */
interface Container3Geometry<V : FloatingNumber<V>> : Eq<Container3Geometry<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>
    val volume: Quantity<V>
        get() = quantityTimesByValue(quantityTimesByValue(width, height), depth)

    fun enabled(
        unit: AbstractCuboid<V>,
        orientation: Orientation = Orientation.Upright
    ): Boolean {
        return (width geq orientation.width(unit)) == true
                && (height geq orientation.height(unit)) == true
                && (depth geq orientation.depth(unit)) == true
    }

    fun enabled(unit: QuantityCuboidPlacement3<*, V>): Boolean {
        return (width geq unit.maxX) == true
                && (height geq unit.maxY) == true
                && (depth geq unit.maxZ) == true
    }

    fun enabled(units: List<QuantityCuboidPlacement3<*, V>>): Boolean {
        val maxX = maxQuantityByValue(units.map { it.maxX })
        if (maxX != null && (width geq maxX) != true) {
            return false
        }
        val maxY = maxQuantityByValue(units.map { it.maxY })
        if (maxY != null && (height geq maxY) != true) {
            return false
        }
        val maxZ = maxQuantityByValue(units.map { it.maxZ })
        if (maxZ != null && (depth geq maxZ) != true) {
            return false
        }
        return true
    }

    fun restSpace(offset: QuantityPoint3G<V>): QuantityContainer3Shape<V> {
        return QuantityContainer3Shape(
            width = quantityMinusByValue(width, offset.x),
            height = quantityMinusByValue(height, offset.y),
            depth = quantityMinusByValue(depth, offset.z)
        )
    }

    override fun partialEq(rhs: Container3Geometry<V>): Boolean? {
        return width eq rhs.width && height eq rhs.height && depth eq rhs.depth
    }
}

/**
 * 泛型 3D 容器形状实现。
 * Quantity 3D container shape implementation.
 *
 * @param V 数值类型 / Numeric scalar type
 * @property width 宽度 / Width
 * @property height 高度 / Height
 * @property depth 深度 / Depth
 */
data class QuantityContainer3Shape<V : FloatingNumber<V>>(
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    override val depth: Quantity<V>
) : Container3Geometry<V>

interface Container2Geometry<P : ProjectivePlane, V : FloatingNumber<V>> {
    val length: Quantity<V>
    val width: Quantity<V>
    val plane: P

    fun restSpace(offset: QuantityPoint2G<V>): QuantityContainer2Shape<P, V> {
        return QuantityContainer2Shape(
            length = quantityMinusByValue(length, offset.x),
            width = quantityMinusByValue(width, offset.y),
            plane = plane
        )
    }

    fun restSpace(offset: QuantityVector2G<V>): QuantityContainer2Shape<P, V> {
        return QuantityContainer2Shape(
            length = quantityMinusByValue(length, offset.x),
            width = quantityMinusByValue(width, offset.y),
            plane = plane
        )
    }
}

data class QuantityContainer2Shape<P : ProjectivePlane, V : FloatingNumber<V>>(
    override val length: Quantity<V>,
    override val width: Quantity<V>,
    override val plane: P
) : Container2Geometry<P, V>

interface QuantityContainer2<
        S : QuantityContainer2<S, V, P>,
        V : FloatingNumber<V>,
        P : ProjectivePlane
        > : Copyable<S> {
    val shape: Container2Geometry<P, V>
    val units: List<QuantityProjectionPlacement2<*, V, P>>
    val amounts: Map<AbstractCuboid<V>, UInt64> get() = count(units)

    val length: Quantity<V> get() = shape.length
    val width: Quantity<V> get() = shape.width

    companion object {
        fun <V : FloatingNumber<V>, P : ProjectivePlane> count(units: List<QuantityProjectionPlacement2<*, V, P>>): Map<AbstractCuboid<V>, UInt64> {
            val counter = HashMap<AbstractCuboid<V>, UInt64>()
            for (placement in units) {
                val unit = placement.unit
                counter[unit] = (counter[unit] ?: UInt64.zero) + UInt64.one
            }
            return counter
        }
    }

    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero

    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
}

interface QuantityContainer3<
        S : QuantityContainer3<S, V>,
        V : FloatingNumber<V>
        > : AbstractCuboid<V>, Copyable<S> {
    val shape: Container3Geometry<V>
    val units: List<QuantityCuboidPlacement3<*, V>>
    val amounts: Map<AbstractCuboid<V>, UInt64> get() = count(units)

    override val width: Quantity<V> get() = shape.width
    override val height: Quantity<V> get() = shape.height
    override val depth: Quantity<V> get() = shape.depth

    override val weight: Quantity<V>
        get() = quantityWeightedSumByValue(
            amounts = amounts,
            zero = Quantity(quantityZeroByValue(width).value, Kilogram)
        ) { it.weight }
    override val volume: Quantity<V> get() = quantityTimesByValue(quantityTimesByValue(depth, height), width)
    override val actualVolume: Quantity<V>
        get() = quantityWeightedSumByValue(
            amounts = amounts,
            zero = quantityZeroByValue(volume)
        ) { it.actualVolume }

    companion object {
        fun <V : FloatingNumber<V>> count(units: List<QuantityCuboidPlacement3<*, V>>): Map<AbstractCuboid<V>, UInt64> {
            val counter = HashMap<AbstractCuboid<V>, UInt64>()
            for (placement in units) {
                val unit = placement.unit
                counter[unit] = (counter[unit] ?: UInt64.zero) + UInt64.one
            }
            return counter
        }
    }

    fun amount(unit: AbstractCuboid<V>) = amounts[unit] ?: UInt64.zero

    fun contains(unit: AbstractCuboid<V>) = amounts[unit]?.let { it != UInt64.zero } ?: false
}

/**
 * 泛型 cuboid 接口。
 * Quantity cuboid interface.
 *
 * @param T 自身类型 / Self type
 * @param V 数值类型 / Numeric scalar type
 */
interface QuantityCuboid<T : QuantityCuboid<T, V>, V : FloatingNumber<V>> : AbstractCuboid<V> {
    val self: T
    val enabledOrientations: List<Orientation>

    fun geometryView(orientation: Orientation = Orientation.Upright): QuantityCuboid3View<V> {
        return QuantityCuboid3View(
            origin = QuantityCuboid3(
                width = width,
                height = height,
                depth = depth
            ),
            permutation = orientation.toAxisPermutation3()
        )
    }

    fun geometry(orientation: Orientation = Orientation.Upright): QuantityCuboid3<V> {
        return geometryView(orientation).cuboid
    }

    fun enabledOrientationsAt(
        space: Container3Geometry<V>,
        withRotation: Boolean = true
    ): List<Orientation> {
        return enabledOrientations.filter {
            (space.width geq it.width(this)) == true
                    && (space.height geq it.height(this)) == true
                    && (space.depth geq it.depth(this)) == true
                    && (withRotation || !it.rotated)
        }
    }

    fun view(orientation: Orientation = Orientation.Upright): QuantityCuboidView<T, V> {
        return QuantityCuboidView(self, orientation)
    }
}

/**
 * 泛型 cuboid 视图。
 * Quantity cuboid view.
 *
 * @param T 原始 cuboid 类型 / Source cuboid type
 * @param V 数值类型 / Numeric scalar type
 */
open class QuantityCuboidView<T : QuantityCuboid<T, V>, V : FloatingNumber<V>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid<V>, Copyable<QuantityCuboidView<T, V>> {
    private val geometryView: QuantityCuboid3View<V> by lazy { unit.geometryView(orientation) }

    override val width get() = geometryView.width
    override val height get() = geometryView.height
    override val depth get() = geometryView.depth
    override val weight by unit::weight

    val rotatedOrientation by orientation::rotation

    open val rotation: QuantityCuboidView<T, V>?
        get() {
            return if (unit.enabledOrientations.contains(rotatedOrientation)) {
                unit.view(rotatedOrientation)
            } else {
                null
            }
        }

    open fun rotationAt(space: Container3Geometry<V>): QuantityCuboidView<T, V>? {
        return if (unit.enabledOrientationsAt(space).contains(rotatedOrientation)) {
            unit.view(rotatedOrientation)
        } else {
            null
        }
    }

    override fun copy(): QuantityCuboidView<T, V> {
        return QuantityCuboidView(
            unit = unit,
            orientation = orientation
        )
    }
}

/**
 * 旧版 `Cuboid<T>` 的泛型适配器（`InfraNumber`）。
 * Quantity adapter for model `Cuboid<T>` (`InfraNumber`).
 */
data class ModelCuboidAdapter<T : Cuboid<T>>(
    val cuboid: T
) : QuantityCuboid<ModelCuboidAdapter<T>, InfraNumber> {
    override val self: ModelCuboidAdapter<T>
        get() = this
    override val width by cuboid::width
    override val height by cuboid::height
    override val depth by cuboid::depth
    override val weight by cuboid::weight
    override val enabledOrientations by cuboid::enabledOrientations
}

fun <T : Cuboid<T>> T.asQuantityCuboid(): ModelCuboidAdapter<T> {
    return ModelCuboidAdapter(this)
}

fun <P : ProjectivePlane> AbstractContainer2Shape<P>.asQuantityContainer2Shape(): Container2Geometry<P, InfraNumber> {
    return QuantityContainer2Shape(
        length = length,
        width = width,
        plane = plane
    )
}

fun AbstractContainer3Shape.asQuantityContainer3Shape(): Container3Geometry<InfraNumber> {
    return QuantityContainer3Shape(
        width = width,
        height = height,
        depth = depth
    )
}

