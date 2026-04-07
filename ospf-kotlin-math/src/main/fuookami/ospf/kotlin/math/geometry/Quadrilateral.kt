/**
 * Quadrilateral（四边形）
 * Quadrilateral
 *
 * 提供四边形的几何表示及相关计算。
 * Provides geometric representation and calculations for quadrilaterals.
 *
 * 主要功能：
 * Main features:
 * - Quadrilateral: 泛型四边形，由四个顶点定义 / Generic quadrilateral, defined by four vertices
 * - Quadrilateral2/Quadrilateral3: 2D/3D 四边形的类型别名 / 2D/3D quadrilateral type aliases
 * - 边和角获取（edges, diagonals）/ Edge and diagonal access
 * - 周长计算（perimeter）/ Perimeter calculation
 * - 重心计算（centroid）/ Centroid calculation
 * - 面积计算（area, areaByTriangles）/ Area calculation
 * - 凸性判断（isConvex）/ Convexity check
 * - 非法四边形判断（illegal）/ Illegal quadrilateral check
 *
 * 应用场景：几何建模、区域表示、形状分析、网格剖分等。
 * Applications: geometric modeling, region representation, shape analysis, mesh partitioning, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Quadrilateral - 泛型四边形
 * Quadrilateral - Generic quadrilateral
 *
 * 表示由四个顶点组成的四边形。
 * Represents a quadrilateral composed of four vertices.
 *
 * @param p1 第一个顶点 / First vertex
 * @param p2 第二个顶点 / Second vertex
 * @param p3 第三个顶点 / Third vertex
 * @param p4 第四个顶点 / Fourth vertex
 */
class Quadrilateral<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P,
    val p4: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
        assert(p3.size == p4.size)
    }

    /** 获取第一条边（p1 -> p2）/ Get the first edge (p1 -> p2) */
    val e1: Edge<P, D> get() = Edge(p1, p2)
    /** 获取第二条边（p2 -> p3）/ Get the second edge (p2 -> p3) */
    val e2: Edge<P, D> get() = Edge(p2, p3)
    /** 获取第三条边（p3 -> p4）/ Get the third edge (p3 -> p4) */
    val e3: Edge<P, D> get() = Edge(p3, p4)
    /** 获取第四条边（p4 -> p1）/ Get the fourth edge (p4 -> p1) */
    val e4: Edge<P, D> get() = Edge(p4, p1)

    /** 获取所有边 / Get all edges */
    val edges: List<Edge<P, D>> get() = listOf(e1, e2, e3, e4)
    /** 获取两条对角线 / Get both diagonals */
    val diagonals: List<Edge<P, D>> get() = listOf(Edge(p1, p3), Edge(p2, p4))

    /** 计算周长 / Calculate the perimeter */
    val perimeter: Flt64 by lazy { e1.length + e2.length + e3.length + e4.length }

    /** 计算重心（四边形的几何中心）/ Calculate the centroid (geometric center of the quadrilateral) */
    val centroid: Point<D> by lazy {
        Point(
            p1.indices.map { (p1[it] + p2[it] + p3[it] + p4[it]) / Flt64(4.0) },
            p1.dim
        )
    }

    /** 计算面积（通过分割为两个三角形）/ Calculate area (by splitting into two triangles) */
    val areaByTriangles: Flt64 by lazy {
        Triangle(p1, p2, p3).area + Triangle(p1, p3, p4).area
    }
}

/** Quadrilateral2 类型别名，表示 2D 四边形 / Quadrilateral2 type alias, representing 2D quadrilateral */
typealias Quadrilateral2 = Quadrilateral<Point2, Dim2>
/** Quadrilateral3 类型别名，表示 3D 四边形 / Quadrilateral3 type alias, representing 3D quadrilateral */
typealias Quadrilateral3 = Quadrilateral<Point3, Dim3>

/**
 * 计算 2D 四边形的面积（使用鞋带公式）
 * Calculate the area of a 2D quadrilateral (using shoelace formula)
 */
val Quadrilateral2.area: Flt64
    get() {
        val sum1 = p1.x * p2.y + p2.x * p3.y + p3.x * p4.y + p4.x * p1.y
        val sum2 = p1.y * p2.x + p2.y * p3.x + p3.y * p4.x + p4.y * p1.x
        val doubleArea = sum1 - sum2
        val absolute = if (doubleArea < Flt64.zero) {
            -doubleArea
        } else {
            doubleArea
        }
        return absolute / Flt64.two
    }

/**
 * 判断 2D 四边形是否为凸四边形
 * Check if a 2D quadrilateral is convex
 *
 * 通过检查所有相邻边的叉积方向是否一致来判断凸性。
 * Checks convexity by verifying that all adjacent edge cross products have consistent sign.
 *
 * @return 是否为凸四边形 / Whether the quadrilateral is convex
 */
fun Quadrilateral2.isConvex(): Boolean {
    fun crossSign(a: Point2, b: Point2, c: Point2): Flt64 {
        val v1 = vector2(b.x - a.x, b.y - a.y)
        val v2 = vector2(c.x - b.x, c.y - b.y)
        return v1 cross v2
    }

    val cross1 = crossSign(p1, p2, p3)
    val cross2 = crossSign(p2, p3, p4)
    val cross3 = crossSign(p3, p4, p1)
    val cross4 = crossSign(p4, p1, p2)

    val allPositive = cross1 > Flt64.zero && cross2 > Flt64.zero && cross3 > Flt64.zero && cross4 > Flt64.zero
    val allNegative = cross1 < Flt64.zero && cross2 < Flt64.zero && cross3 < Flt64.zero && cross4 < Flt64.zero
    return allPositive || allNegative
}

/**
 * 判断 2D 四边形是否非法（面积为零）
 * Check if a 2D quadrilateral is illegal (area is zero)
 *
 * @return 是否非法 / Whether illegal
 */
val Quadrilateral2.illegal: Boolean
    get() = area eq Flt64.zero

/**
 * 创建 2D 四边形
 * Create a 2D quadrilateral
 *
 * @param p1 第一个顶点 / First vertex
 * @param p2 第二个顶点 / Second vertex
 * @param p3 第三个顶点 / Third vertex
 * @param p4 第四个顶点 / Fourth vertex
 * @return 2D 四边形 / 2D quadrilateral
 */
fun quadrilateral2(p1: Point2, p2: Point2, p3: Point2, p4: Point2): Quadrilateral2 {
    return Quadrilateral(p1, p2, p3, p4)
}

