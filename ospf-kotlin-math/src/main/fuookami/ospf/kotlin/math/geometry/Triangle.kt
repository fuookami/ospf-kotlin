/**
 * 三角形
 * Triangle
 *
 * 定义几何空间中的三角形数据结构，由三个顶点构成。
 * 支持边长、周长、面积、重心、退化检测等操作。
 * Defines triangle data structure in geometric space, composed of three vertices.
 * Supports side length, perimeter, area, centroid, degeneracy detection, etc.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.vector3
import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 三角形数据类
 * Triangle data class
 *
 * 由三个顶点定义的三角形，支持任意维度和浮点数类型。
 * 提供边、周长、面积、重心、退化/非法检测等计算功能。
 * A triangle defined by three vertices, supporting arbitrary dimensions and floating-point types.
 * Provides functionality for edges, perimeter, area, centroid, degeneracy/illegal detection, etc.
 *
 * @param P 点类型 / Point type
 * @param D 维度类型 / Dimension type
 * @param V 数值类型 / Number type
 * @property p1 第一个顶点 / First vertex
 * @property p2 第二个顶点 / Second vertex
 * @property p3 第三个顶点 / Third vertex
 */
class Triangle<P : Point<D, V>, D : Dimension, V : FloatingNumber<V>>(
    val p1: P,
    val p2: P,
    val p3: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
    }

    /** 第一条边（p1 -> p2） / First edge (p1 -> p2) */
    val e1: Edge<P, D, V> get() = Edge(p1, p2)
    /** 第二条边（p2 -> p3） / Second edge (p2 -> p3) */
    val e2: Edge<P, D, V> get() = Edge(p2, p3)
    /** 第三条边（p3 -> p1） / Third edge (p3 -> p1) */
    val e3: Edge<P, D, V> get() = Edge(p3, p1)

    /** 所有边的列表 / List of all edges */
    val edges: List<Edge<P, D, V>> get() = listOf(e1, e2, e3)

    /** 所有顶点的列表 / List of all vertices */
    val vertices: List<P> get() = listOf(p1, p2, p3)

    /** 周长 / Perimeter */
    val perimeter: V by lazy { e1.length + e2.length + e3.length }

    @Suppress("UNCHECKED_CAST")
    private fun castPoint(position: List<V>): P {
        // 安全不变量：P 受限于 Point<D, V>，构造结果保持相同维度与数值类型。
        // Safety invariant: P is constrained by Point<D, V>, and constructed point keeps the same D/V.
        return Point(position, p1.dim) as P
    }

    @Suppress("UNCHECKED_CAST")
    private fun castValue(value: Any): V {
        // 安全不变量：当前三角形数值泛型为 V，sqrt 结果与输入域一致。
        // Safety invariant: triangle numeric generic is V, and sqrt result stays in the same numeric domain.
        return value as V
    }

    /** 重心 / Centroid */
    val centroid: P by lazy {
        val v = p1[0]
        val three = v.constants.three
        castPoint(p1.indices.map { (p1[it] + p2[it] + p3[it]) / three })
    }

    /** 是否退化（存在重合顶点） / Whether degenerate (has coincident vertices) */
    val isDegenerate: Boolean by lazy {
        p1.approxEq(p2) || p2.approxEq(p3) || p3.approxEq(p1)
    }

    /** 是否非法（三点共线） / Whether illegal (three collinear points) */
    val illegal: Boolean by lazy {
        for (i in p1.indices) {
            if (p1[i] eq p2[i] && p2[i] eq p3[i]) {
                return@lazy true
            }
        }
        false
    }

    /** 面积（海伦公式） / Area (Heron's formula) */
    val area: V by lazy {
        val a = e1.length
        val b = e2.length
        val c = e3.length
        val v = p1[0]
        val two = v.constants.two
        val p = (a + b + c) / two
        val s = p * (p - a) * (p - b) * (p - c)
        castValue(s.sqrt())
    }

    override fun toString() = "Triangle($p1, $p2, $p3)"
}

/** 计算二维三角形面积（叉积法） / Compute 2D triangle area (cross product method) */
fun Triangle<Point<Dim2, Flt64>, Dim2, Flt64>.area2D(): Flt64 {
    val v1x = p2.x - p1.x
    val v1y = p2.y - p1.y
    val v2x = p3.x - p1.x
    val v2y = p3.y - p1.y
    val cross = v1x * v2y - v1y * v2x
    return cross.abs() / Flt64.two
}

/** 判断二维点是否在三角形内（含边界） / Check whether a 2D point is inside the triangle (inclusive) */
infix fun Triangle<Point<Dim2, Flt64>, Dim2, Flt64>.containsPoint(point: Point<Dim2, Flt64>): Boolean {
    val v0x = p3.x - p1.x
    val v0y = p3.y - p1.y
    val v1x = p2.x - p1.x
    val v1y = p2.y - p1.y
    val v2x = point.x - p1.x
    val v2y = point.y - p1.y

    val dot00 = v0x * v0x + v0y * v0y
    val dot01 = v0x * v1x + v0y * v1y
    val dot02 = v0x * v2x + v0y * v2y
    val dot11 = v1x * v1x + v1y * v1y
    val dot12 = v1x * v2x + v1y * v2y

    val denom = dot00 * dot11 - dot01 * dot01
    if (denom eq Flt64.zero) {
        return false
    }

    val u = (dot11 * dot02 - dot01 * dot12) / denom
    val v = (dot00 * dot12 - dot01 * dot02) / denom

    return u geq Flt64.zero && v geq Flt64.zero && u + v leq Flt64.one
}

/** 计算二维三角形的外接圆 / Compute the circumcircle of a 2D triangle */
fun Triangle<Point<Dim2, Flt64>, Dim2, Flt64>.circumcircle(): Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64> {
    return Circle.circumcircleOf(this)
}

/** 计算二维三角形的外心 / Compute the circumcenter of a 2D triangle */
fun Triangle<Point<Dim2, Flt64>, Dim2, Flt64>.circumcenter(): Point<Dim2, Flt64> {
    val cc = circumcircle()
    return cc.center
}

/** 计算二维三角形的内心 / Compute the incenter of a 2D triangle */
fun Triangle<Point<Dim2, Flt64>, Dim2, Flt64>.incenter(): Point<Dim2, Flt64> {
    val a = e2.length
    val b = e3.length
    val c = e1.length
    val perimeter = a + b + c

    if (perimeter eq Flt64.zero) {
        return p1
    }

    return point2(
        (a * p1.x + b * p2.x + c * p3.x) / perimeter,
        (a * p1.y + b * p2.y + c * p3.y) / perimeter
    )
}

/** 计算三维三角形面积（叉积法） / Compute 3D triangle area (cross product method) */
fun Triangle<Point<Dim3, Flt64>, Dim3, Flt64>.area3D(): Flt64 {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2
    return cross.norm / Flt64.two
}

/** 计算三维三角形的法向量（单位向量），退化时返回 null / Compute the normal vector (unit) of a 3D triangle, returns null if degenerate */
fun Triangle<Point<Dim3, Flt64>, Dim3, Flt64>.normal(): Vector<Dim3, Flt64>? {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2

    if (cross.norm eq Flt64.zero) {
        return null
    }

    return cross.unit
}
