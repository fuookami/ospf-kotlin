/**
 * Rectangle（矩形）
 * Rectangle
 *
 * 提供矩形的几何表示及相关计算。
 * Provides geometric representation and calculations for rectangles.
 *
 * 主要功能：
 * Main features:
 * - Rectangle: 泛型矩形，由四个顶点定义 / Generic rectangle, defined by four vertices
 * - Rectangle2/Rectangle3: 2D/3D 矩形的类型别名 / 2D/3D rectangle type aliases
 * - 长度、宽度计算（length, width）/ Length, width calculation
 * - 面积计算（area）/ Area calculation
 * - 边界点获取（leftUpperPoint, rightBottomPoint）/ Boundary point access
 * - 点包含判断（contains）/ Point containment check
 *
 * 支持通过左上角和右下角两点快速构造矩形。
 * Supports quick rectangle construction via left-upper and right-bottom corner points.
 *
 * 应用场景：边界框表示、区域裁剪、空间索引、碰撞检测等。
 * Applications: bounding box representation, region clipping, spatial indexing, collision detection, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.min
import fuookami.ospf.kotlin.math.ordinary.minMax
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange

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

    val leftUpperPoint: Point<D>
        get() = Point(
            leftUpperRightBottom.take(p1.dim.size),
            p1.dim
        )

    val rightBottomPoint: Point<D>
        get() = Point(
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

typealias Rectangle3 = Rectangle<Point3, Dim3>








