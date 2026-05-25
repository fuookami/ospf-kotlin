package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

typealias QuantityFlt64 = Quantity<Flt64>

data class QuantityVector2(
    val x: QuantityFlt64,
    val y: QuantityFlt64
) {
    operator fun plus(rhs: QuantityVector2): QuantityVector2 {
        return (asGeneric() + rhs.asGeneric()).toCompat()
    }

    operator fun minus(rhs: QuantityVector2): QuantityVector2 {
        return (asGeneric() - rhs.asGeneric()).toCompat()
    }
}

data class QuantityVector3(
    val x: QuantityFlt64,
    val y: QuantityFlt64,
    val z: QuantityFlt64
) {
    operator fun plus(rhs: QuantityVector3): QuantityVector3 {
        return (asGeneric() + rhs.asGeneric()).toCompat()
    }

    operator fun minus(rhs: QuantityVector3): QuantityVector3 {
        return (asGeneric() - rhs.asGeneric()).toCompat()
    }
}

data class QuantityPoint2(
    val x: QuantityFlt64,
    val y: QuantityFlt64
) {
    operator fun plus(offset: QuantityVector2): QuantityPoint2 {
        return (asGeneric() + offset.asGeneric()).toCompat()
    }

    operator fun minus(offset: QuantityVector2): QuantityPoint2 {
        return (asGeneric() - offset.asGeneric()).toCompat()
    }

    infix fun ord(rhs: QuantityPoint2): Order {
        return asGeneric() ord rhs.asGeneric()
    }
}

data class QuantityPoint3(
    val x: QuantityFlt64,
    val y: QuantityFlt64,
    val z: QuantityFlt64
) {
    operator fun plus(offset: QuantityVector3): QuantityPoint3 {
        return (asGeneric() + offset.asGeneric()).toCompat()
    }

    operator fun minus(offset: QuantityVector3): QuantityPoint3 {
        return (asGeneric() - offset.asGeneric()).toCompat()
    }

    infix fun ord(rhs: QuantityPoint3): Order {
        return asGeneric() ord rhs.asGeneric()
    }
}

data class QuantityRectangle2(
    val minX: QuantityFlt64,
    val minY: QuantityFlt64,
    val maxX: QuantityFlt64,
    val maxY: QuantityFlt64
) {
    init {
        require((asGeneric().minX.partialOrd(asGeneric().maxX) ?: error("Incomparable x")) !is Order.Greater) { "minX should be <= maxX" }
        require((asGeneric().minY.partialOrd(asGeneric().maxY) ?: error("Incomparable y")) !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: QuantityFlt64 get() = asGeneric().width
    val height: QuantityFlt64 get() = asGeneric().height
    val area: QuantityFlt64 get() = asGeneric().area

    fun intersect(rhs: QuantityRectangle2): QuantityRectangle2? {
        return asGeneric().intersect(rhs.asGeneric())?.toCompat()
    }

    fun intersectArea(rhs: QuantityRectangle2): QuantityFlt64? {
        return intersect(rhs)?.area
    }
}

private fun QuantityVector2.asGeneric(): QuantityVector2G<Flt64> {
    return QuantityVector2G(x = x, y = y)
}

private fun QuantityVector3.asGeneric(): QuantityVector3G<Flt64> {
    return QuantityVector3G(x = x, y = y, z = z)
}

private fun QuantityPoint2.asGeneric(): QuantityPoint2G<Flt64> {
    return QuantityPoint2G(x = x, y = y)
}

private fun QuantityPoint3.asGeneric(): QuantityPoint3G<Flt64> {
    return QuantityPoint3G(x = x, y = y, z = z)
}

private fun QuantityRectangle2.asGeneric(): QuantityRectangle2G<Flt64> {
    return QuantityRectangle2G(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY
    )
}

private fun QuantityVector2G<Flt64>.toCompat(): QuantityVector2 {
    return QuantityVector2(x = x, y = y)
}

private fun QuantityVector3G<Flt64>.toCompat(): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}

private fun QuantityPoint2G<Flt64>.toCompat(): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

private fun QuantityPoint3G<Flt64>.toCompat(): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

private fun QuantityRectangle2G<Flt64>.toCompat(): QuantityRectangle2 {
    return QuantityRectangle2(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY
    )
}
