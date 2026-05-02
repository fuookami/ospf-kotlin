package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.math.ordinary.minMax
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

data class Rectangle<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
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

    val length: V
    val width: V

    init {
        val e1 = Edge(p1, p2)
        val e2 = Edge(p2, p3)
        val l1 = e1.length
        val l2 = e2.length
        val v = p1[0]
        width = min(l1, l2)
        length = max(l1, l2)

        assert(((e1.vector * e2.vector) / (l1 * l2)) eq v.constants.zero)
    }

    val area: V by lazy { length * width }

    private val leftUpperRightBottom: List<V> by lazy {
        val minMaxValues = p1.indices.map {
            minMax(p1[it], p2[it], p3[it], p4[it])
        }
        minMaxValues.map { it.first } + minMaxValues.map { it.second }
    }

    val leftUpperPoint: Point<D, V>
        get() = Point(
            leftUpperRightBottom.take(p1.dim.size),
            p1.dim
        )

    val rightBottomPoint: Point<D, V>
        get() = Point(
            leftUpperRightBottom.subList(p1.dim.size, p1.dim.size * 2),
            p1.dim
        )
}

typealias Rectangle2 = Rectangle<Point2, Dim2, Flt64>

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

        is Fatal -> {
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

        is Fatal -> {
            return false
        }
    }
    return xRange.contains(point.x) && yRange.contains(point.y)
}

typealias Rectangle3 = Rectangle<Point3, Dim3, Flt64>
