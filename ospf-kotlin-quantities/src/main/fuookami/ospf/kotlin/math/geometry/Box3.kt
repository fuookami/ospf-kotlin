package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.Order

data class QuantityBox3<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val z: Quantity<V>,
    val cuboid: QuantityCuboid3<V>
) {
    companion object {
        fun <V : FloatingNumber<V>> atOrigin(cuboid: QuantityCuboid3<V>): QuantityBox3<V> {
            return QuantityBox3(
                x = quantityZeroOf(cuboid.width),
                y = quantityZeroOf(cuboid.height),
                z = quantityZeroOf(cuboid.depth),
                cuboid = cuboid
            )
        }
    }

    val width get() = cuboid.width
    val height get() = cuboid.height
    val depth get() = cuboid.depth

    val maxX: Quantity<V> get() = quantityPlus(x, width)
    val maxY: Quantity<V> get() = quantityPlus(y, height)
    val maxZ: Quantity<V> get() = quantityPlus(z, depth)

    fun contains(
        x: Quantity<V>,
        y: Quantity<V>,
        z: Quantity<V>,
        withLowerBound: Boolean = true,
        withUpperBound: Boolean = true,
        withBorder: Boolean = true
    ): Boolean {
        val includeLower = withBorder && withLowerBound
        val includeUpper = withBorder && withUpperBound
        return quantityContainsInRange(x, this.x, maxX, includeLower, includeUpper, "x")
                && quantityContainsInRange(y, this.y, maxY, includeLower, includeUpper, "y")
                && quantityContainsInRange(z, this.z, maxZ, includeLower, includeUpper, "z")
    }

    fun overlapped(rhs: QuantityBox3<V>): Boolean {
        if (quantityOrd(maxX, rhs.x, "x") !is Order.Greater) {
            return false
        }
        if (quantityOrd(x, rhs.maxX, "x") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxY, rhs.y, "y") !is Order.Greater) {
            return false
        }
        if (quantityOrd(y, rhs.maxY, "y") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxZ, rhs.z, "z") !is Order.Greater) {
            return false
        }
        if (quantityOrd(z, rhs.maxZ, "z") !is Order.Less) {
            return false
        }
        return true
    }

    fun intersect(rhs: QuantityBox3<V>): QuantityBox3<V>? {
        val minX = quantityMax(x, rhs.x, "x")
        val maxX = quantityMin(this.maxX, rhs.maxX, "x")
        val minY = quantityMax(y, rhs.y, "y")
        val maxY = quantityMin(this.maxY, rhs.maxY, "y")
        val minZ = quantityMax(z, rhs.z, "z")
        val maxZ = quantityMin(this.maxZ, rhs.maxZ, "z")
        if (quantityOrd(minX, maxX, "x") !is Order.Less) {
            return null
        }
        if (quantityOrd(minY, maxY, "y") !is Order.Less) {
            return null
        }
        if (quantityOrd(minZ, maxZ, "z") !is Order.Less) {
            return null
        }
        return QuantityBox3(
            x = minX,
            y = minY,
            z = minZ,
            cuboid = QuantityCuboid3(
                width = quantityMinus(maxX, minX),
                height = quantityMinus(maxY, minY),
                depth = quantityMinus(maxZ, minZ)
            )
        )
    }
}

typealias QuantityAxisAlignedBox3<V> = QuantityBox3<V>

