package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*

data class Rectangle<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
    companion object {
        operator fun invoke(leftUpperPoint: Point2, rightBottomPoint: Point2): Rectangle2 {
            return Rectangle(
                leftUpperPoint,
                Point2(rightBottomPoint.x, leftUpperPoint.y),
                rightBottomPoint,
                Point2(leftUpperPoint.x, rightBottomPoint.y)
            )
        }
    }

    val length: Flt64
    val width: Flt64

    init {
        val e1 = Edge(p1, p2)
        val e2 = Edge(p2, p3)
        val l1 = e1.length
        val l2 = e2.length
        width = min(l1, l2)
        length = max(l1, l2)

        // assert is at right angles
        assert(((e1.vector * e2.vector) / (l1 * l2)) eq Flt64.zero)
    }

    val area: Flt64 by lazy { length * width }
}

typealias Rectangle2 = Rectangle<Point2, Dim2>

fun Rectangle2.contains(
    point: Point2,
    withLowerBound: Boolean = true,
    withUpperBound: Boolean = true,
    withBorder: Boolean = true
): Boolean {
    val (minX, maxX) = minMax(p1.x, p2.x, p3.x, p4.x)
    val (minY, maxY) = minMax(p1.y, p2.y, p3.y, p4.y)
    val lowerInterval = if (withBorder && withLowerBound) {
        IntervalType.Closed
    } else {
        IntervalType.Open
    }
    val upperInterval = if (withBorder && withUpperBound) {
        IntervalType.Closed
    } else {
        IntervalType.Open
    }
    val xRange = ValueRange(minX, maxX, lowerInterval, upperInterval)
    val yRange = ValueRange(minY, maxY, lowerInterval, upperInterval)
    return xRange.contains(point.x) && yRange.contains(point.y)
}

typealias Rectangle3 = Rectangle<Point3, Dim3>
