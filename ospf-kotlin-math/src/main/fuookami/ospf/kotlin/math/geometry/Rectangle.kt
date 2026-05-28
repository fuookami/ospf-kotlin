/**
 * 矩形
 * Rectangle
 *
 * 定义几何空间中的矩形数据结构，由四个顶点定义，支持面积、凸性检测、交集等操作。
 * Defines rectangle data structure in geometric space, defined by four vertices, supporting area, convexity detection, intersection, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.math.ordinary.*

/**
 * 通用矩形，由四个顶点定义（不要求轴对齐）。
 * General rectangle defined by four vertices (not necessarily axis-aligned).
 *
 * @param P 点类型 / The point type
 * @param D 维度类型 / The dimension type
 * @param V 数值类型 / The numeric type
 * @property p1 第一个顶点 / First vertex
 * @property p2 第二个顶点 / Second vertex
 * @property p3 第三个顶点 / Third vertex
 * @property p4 第四个顶点 / Fourth vertex
 */
data class Rectangle<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
    companion object {
        /** 通过左上角和右下角创建二维轴对齐矩形 / Create a 2D axis-aligned rectangle from left-upper and right-bottom corners */
        operator fun invoke(leftUpperPoint: Point<Dim2, Flt64>, rightBottomPoint: Point<Dim2, Flt64>): Rectangle<Point<Dim2, Flt64>, Dim2, Flt64> {
            return Rectangle(
                leftUpperPoint,
                point2(rightBottomPoint.x, leftUpperPoint.y),
                rightBottomPoint,
                point2(leftUpperPoint.x, rightBottomPoint.y)
            )
        }
    }

    /** 长边长度 / Length of the longer side */
    val length: V
    /** 短边长度 / Length of the shorter side */
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

    /** 面积 / Area */
    val area: V by lazy { length * width }

    private val leftUpperRightBottom: List<V> by lazy {
        val minMaxValues = p1.indices.map {
            minMax(p1[it], p2[it], p3[it], p4[it])
        }
        minMaxValues.map { it.first } + minMaxValues.map { it.second }
    }

    /** 左上角点 / Left-upper corner point */
    val leftUpperPoint: Point<D, V>
        get() = Point(
            leftUpperRightBottom.take(p1.dim.size),
            p1.dim
        )

    /** 右下角点 / Right-bottom corner point */
    val rightBottomPoint: Point<D, V>
        get() = Point(
            leftUpperRightBottom.subList(p1.dim.size, p1.dim.size * 2),
            p1.dim
        )
}

/** 判断指定点是否在矩形内 / Check whether a point is inside the rectangle */
fun Rectangle<Point<Dim2, Flt64>, Dim2, Flt64>.contains(
    point: Point<Dim2, Flt64>,
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
