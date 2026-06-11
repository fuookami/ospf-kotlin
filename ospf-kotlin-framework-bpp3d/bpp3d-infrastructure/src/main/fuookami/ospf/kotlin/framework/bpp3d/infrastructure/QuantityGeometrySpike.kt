/**
 * 量纲几何 Spike 实现。
 * Quantity geometry spike implementation.
 */
package fuookami.ospf.kotlin.framework.bpp3d.infrastructure

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.utils.functional.Order

data class QuantityVector2(
    val x: Quantity<InfraNumber>,
    val y: Quantity<InfraNumber>
) {
    operator fun plus(rhs: QuantityVector2): QuantityVector2 {
        return (asQuantityGeometry() + rhs.asQuantityGeometry()).toCompat()
    }

    operator fun minus(rhs: QuantityVector2): QuantityVector2 {
        return (asQuantityGeometry() - rhs.asQuantityGeometry()).toCompat()
    }
}

data class QuantityVector3(
    val x: Quantity<InfraNumber>,
    val y: Quantity<InfraNumber>,
    val z: Quantity<InfraNumber>
) {
    operator fun plus(rhs: QuantityVector3): QuantityVector3 {
        return (asQuantityGeometry() + rhs.asQuantityGeometry()).toCompat()
    }

    operator fun minus(rhs: QuantityVector3): QuantityVector3 {
        return (asQuantityGeometry() - rhs.asQuantityGeometry()).toCompat()
    }
}

data class QuantityPoint2(
    val x: Quantity<InfraNumber>,
    val y: Quantity<InfraNumber>
) {
    operator fun plus(offset: QuantityVector2): QuantityPoint2 {
        return (asQuantityGeometry() + offset.asQuantityGeometry()).toCompat()
    }

    operator fun minus(offset: QuantityVector2): QuantityPoint2 {
        return (asQuantityGeometry() - offset.asQuantityGeometry()).toCompat()
    }

    infix fun ord(rhs: QuantityPoint2): Order {
        return asQuantityGeometry() ord rhs.asQuantityGeometry()
    }
}

data class QuantityPoint3(
    val x: Quantity<InfraNumber>,
    val y: Quantity<InfraNumber>,
    val z: Quantity<InfraNumber>
) {
    operator fun plus(offset: QuantityVector3): QuantityPoint3 {
        return (asQuantityGeometry() + offset.asQuantityGeometry()).toCompat()
    }

    operator fun minus(offset: QuantityVector3): QuantityPoint3 {
        return (asQuantityGeometry() - offset.asQuantityGeometry()).toCompat()
    }

    infix fun ord(rhs: QuantityPoint3): Order {
        return asQuantityGeometry() ord rhs.asQuantityGeometry()
    }
}

data class QuantityRectangle2(
    val minX: Quantity<InfraNumber>,
    val minY: Quantity<InfraNumber>,
    val maxX: Quantity<InfraNumber>,
    val maxY: Quantity<InfraNumber>
) {
    init {
        require((asQuantityGeometry().minX.partialOrd(asQuantityGeometry().maxX) ?: error("Incomparable x")) !is Order.Greater) { "minX should be <= maxX" }
        require((asQuantityGeometry().minY.partialOrd(asQuantityGeometry().maxY) ?: error("Incomparable y")) !is Order.Greater) { "minY should be <= maxY" }
    }

    val width: Quantity<InfraNumber> get() = asQuantityGeometry().width
    val height: Quantity<InfraNumber> get() = asQuantityGeometry().height
    val area: Quantity<InfraNumber> get() = asQuantityGeometry().area

    fun intersect(rhs: QuantityRectangle2): QuantityRectangle2? {
        return asQuantityGeometry().intersect(rhs.asQuantityGeometry())?.toCompat()
    }

    fun intersectArea(rhs: QuantityRectangle2): Quantity<InfraNumber>? {
        return intersect(rhs)?.area
    }
}

private fun QuantityVector2.asQuantityGeometry(): QuantityVector2G<InfraNumber> {
    return QuantityVector2G(x = x, y = y)
}

private fun QuantityVector3.asQuantityGeometry(): QuantityVector3G<InfraNumber> {
    return QuantityVector3G(x = x, y = y, z = z)
}

private fun QuantityPoint2.asQuantityGeometry(): QuantityPoint2G<InfraNumber> {
    return QuantityPoint2G(x = x, y = y)
}

private fun QuantityPoint3.asQuantityGeometry(): QuantityPoint3G<InfraNumber> {
    return QuantityPoint3G(x = x, y = y, z = z)
}

private fun QuantityRectangle2.asQuantityGeometry(): QuantityRectangle2G<InfraNumber> {
    return QuantityRectangle2G(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY
    )
}

private fun QuantityVector2G<InfraNumber>.toCompat(): QuantityVector2 {
    return QuantityVector2(x = x, y = y)
}

private fun QuantityVector3G<InfraNumber>.toCompat(): QuantityVector3 {
    return QuantityVector3(x = x, y = y, z = z)
}

private fun QuantityPoint2G<InfraNumber>.toCompat(): QuantityPoint2 {
    return QuantityPoint2(x = x, y = y)
}

private fun QuantityPoint3G<InfraNumber>.toCompat(): QuantityPoint3 {
    return QuantityPoint3(x = x, y = y, z = z)
}

private fun QuantityRectangle2G<InfraNumber>.toCompat(): QuantityRectangle2 {
    return QuantityRectangle2(
        minX = minX,
        minY = minY,
        maxX = maxX,
        maxY = maxY
    )
}
