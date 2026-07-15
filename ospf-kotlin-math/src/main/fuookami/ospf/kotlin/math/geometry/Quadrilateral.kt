/**
 * 四边形
 * Quadrilateral
 *
 * 定义几何空间中的四边形数据结构，由四个顶点构成。
 * 支持边、对角线、周长、质心、面积、凸性检测等操作。
 * Defines quadrilateral data structure in geometric space, composed of four vertices.
 * Supports edges, diagonals, perimeter, centroid, area, convexity detection, etc.
*/
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber

/**
 * 四边形数据类
 * Quadrilateral data class
 *
 * 由四个顶点定义的四边形，支持任意维度和浮点数类型。
 * 提供边、对角线、周长、质心、面积（三角形分解）等计算功能。
 * A quadrilateral defined by four vertices, supporting arbitrary dimensions and floating-point types.
 * Provides functionality for edges, diagonals, perimeter, centroid, area (triangle decomposition), etc.
 *
 * @param P 点类型 / Point type
 * @param D 维度类型 / Dimension type
 * @param V 数值类型 / Number type
 * @property p1 第一个顶点 / First vertex
 * @property p2 第二个顶点 / Second vertex
 * @property p3 第三个顶点 / Third vertex
 * @property p4 第四个顶点 / Fourth vertex
*/
class Quadrilateral<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
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

    /** 第一条边（p1 -> p2） / First edge (p1 -> p2) */
    val e1: Edge<P, D, V> get() = Edge(p1, p2)

    /** 第二条边（p2 -> p3） / Second edge (p2 -> p3) */
    val e2: Edge<P, D, V> get() = Edge(p2, p3)

    /** 第三条边（p3 -> p4） / Third edge (p3 -> p4) */
    val e3: Edge<P, D, V> get() = Edge(p3, p4)

    /** 第四条边（p4 -> p1） / Fourth edge (p4 -> p1) */
    val e4: Edge<P, D, V> get() = Edge(p4, p1)

    /** 所有边的列表 / List of all edges */
    val edges: List<Edge<P, D, V>> get() = listOf(e1, e2, e3, e4)

    /** 对角线列表 / List of diagonals */
    val diagonals: List<Edge<P, D, V>> get() = listOf(Edge(p1, p3), Edge(p2, p4))

    /** 周长 / Perimeter */
    val perimeter: V by lazy { e1.length + e2.length + e3.length + e4.length }

    /** 质心 / Centroid */
    val centroid: Point<D, V> by lazy {
        val v = p1[0]
        val four = v.constants.two + v.constants.two
        Point(
            p1.indices.map { (p1[it] + p2[it] + p3[it] + p4[it]) / four },
            p1.dim
        )
    }

    /** 面积（三角形分解法） / Area (triangle decomposition method) */
    val areaByTriangles: V by lazy {
        Triangle(p1, p2, p3).area + Triangle(p1, p3, p4).area
    }
}

/**
 * 二维四边形面积（鞋带公式）
 * 2D quadrilateral area (Shoelace formula)
*/
val Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64>.area: Flt64
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
 * 判断二维四边形是否为凸四边形
 * Check whether a 2D quadrilateral is convex
 *
 * @return 是否为凸四边形 / Whether the quadrilateral is convex
*/
fun Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64>.isConvex(): Boolean {

    /**
     * Compute the cross product sign for three 2D points.
     * 计算三个二维点的叉积符号。
     *
     * @param a the first point / 第一个点
     * @param b the second point / 第二个点
     * @param c the third point / 第三个点
     * @return the cross product value / 叉积值
    */
    fun crossSign(a: Point<Dim2, Flt64>, b: Point<Dim2, Flt64>, c: Point<Dim2, Flt64>): Flt64 {
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

/** 是否非法（面积为零） / Whether illegal (zero area) */
val Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64>.illegal: Boolean
    get() = area eq Flt64.zero

/**
 * 创建二维四边形
 * Create a 2D quadrilateral
 *
 * @param p1 第一个顶点 / First vertex
 * @param p2 第二个顶点 / Second vertex
 * @param p3 第三个顶点 / Third vertex
 * @param p4 第四个顶点 / Fourth vertex
 * @return 二维四边形 / The 2D quadrilateral
*/
fun quadrilateral2(p1: Point<Dim2, Flt64>, p2: Point<Dim2, Flt64>, p3: Point<Dim2, Flt64>, p4: Point<Dim2, Flt64>): Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64> {
    return Quadrilateral(p1, p2, p3, p4)
}
