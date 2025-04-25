package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*

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

    private val leftUpperRightBottom: List<Flt64> by lazy {
        val minMaxValues = p1.indices.map {
            minMax(p1[it], p2[it], p3[it], p4[it])
        }
        minMaxValues.map { it.first } + minMaxValues.map { it.second }
    }

    val leftUpperPoint: Point<D> get() = Point(
        leftUpperRightBottom.take(p1.dim.size),
        p1.dim
    )

    val rightBottomPoint: Point<D> get() = Point(
        leftUpperRightBottom.subList(p1.dim.size, p1.dim.size * 2),
        p1.dim
    )
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
        Interval.Closed
    } else {
        Interval.Open
    }
    val upperInterval = if (withBorder && withUpperBound) {
        Interval.Closed
    } else {
        Interval.Open
    }
    val xRange = when (val result = ValueRange(minX, maxX, lowerInterval, upperInterval)) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return false
        }
    }
    val yRange = when (val result = ValueRange(minY, maxY, lowerInterval, upperInterval)) {
        is Ok -> {
            result.value
        }

        is Failed -> {
            return false
        }
    }
    return xRange.contains(point.x) && yRange.contains(point.y)
}

typealias Rectangle3 = Rectangle<Point3, Dim3>
