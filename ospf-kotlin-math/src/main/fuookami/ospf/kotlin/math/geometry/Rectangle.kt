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

/**
 * Rectangle - 泛型矩形
 * Rectangle - Generic rectangle
 *
 * 表示由四个顶点组成的矩形，顶点之间必须形成直角。
 * Represents a rectangle composed of four vertices, which must form right angles.
 *
 * @param p1 第一个顶点 / First vertex
 * @param p2 第二个顶点 / Second vertex
 * @param p3 第三个顶点 / Third vertex
 * @param p4 第四个顶点 / Fourth vertex
 */
data class Rectangle<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
    companion object {
        /**
         * 通过左上角和右下角两点创建 2D 矩形
         * Create a 2D rectangle from left-upper and right-bottom corner points
         *
         * @param leftUpperPoint 左上角点 / Left-upper corner point
         * @param rightBottomPoint 右下角点 / Right-bottom corner point
         * @return 2D 矩形 / 2D rectangle
         */
        operator fun invoke(leftUpperPoint: Point2, rightBottomPoint: Point2): Rectangle2 {
            return Rectangle(
                leftUpperPoint,
                Point2(rightBottomPoint.x, leftUpperPoint.y),
                rightBottomPoint,
                Point2(leftUpperPoint.x, rightBottomPoint.y)
            )
        }
    }

    /** 矩形的长度（较长边） / The length of the rectangle (longer side) */
    val length: Flt64
    /** 矩形的宽度（较短边） / The width of the rectangle (shorter side) */
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

    /** 计算矩形的面积 / Calculate the area of the rectangle */
    val area: Flt64 by lazy { length * width }

    private val leftUpperRightBottom: List<Flt64> by lazy {
        val minMaxValues = p1.indices.map {
            minMax(p1[it], p2[it], p3[it], p4[it])
        }
        minMaxValues.map { it.first } + minMaxValues.map { it.second }
    }

    /** 获取左上角点 / Get the left-upper corner point */
    val leftUpperPoint: Point<D>
        get() = Point(
            leftUpperRightBottom.take(p1.dim.size),
            p1.dim
        )

    /** 获取右下角点 / Get the right-bottom corner point */
    val rightBottomPoint: Point<D>
        get() = Point(
            leftUpperRightBottom.subList(p1.dim.size, p1.dim.size * 2),
            p1.dim
        )
}

/** Rectangle2 类型别名，表示 2D 矩形 / Rectangle2 type alias, representing 2D rectangle */
typealias Rectangle2 = Rectangle<Point2, Dim2>

/**
 * 判断点是否在 2D 矩形内
 * Check if a point is inside the 2D rectangle
 *
 * 支持配置是否包含边界和上下界。
 * Supports configuration of boundary and bound inclusion.
 *
 * @param point 待检查的点 / The point to check
 * @param withLowerBound 是否包含下界，默认为 true / Whether to include lower bound, defaults to true
 * @param withUpperBound 是否包含上界，默认为 true / Whether to include upper bound, defaults to true
 * @param withBorder 是否包含边界，默认为 true / Whether to include border, defaults to true
 * @return 点是否在矩形内 / Whether the point is inside
 */
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

/** Rectangle3 类型别名，表示 3D 矩形 / Rectangle3 type alias, representing 3D rectangle */
typealias Rectangle3 = Rectangle<Point3, Dim3>








