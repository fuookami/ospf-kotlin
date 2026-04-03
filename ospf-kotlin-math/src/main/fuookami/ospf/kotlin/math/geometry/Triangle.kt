package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.functional.sumOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Triangle - 泛型三角形
 * Triangle - Generic triangle
 *
 * 表示由三个点组成的三角形。
 * Represents a triangle composed of three points.
 *
 * @param p1 第一个顶点 / First vertex
 * @param p2 第二个顶点 / Second vertex
 * @param p3 第三个顶点 / Third vertex
 */
class Triangle<P : Point<D>, D : Dimension>(
    val p1: P,
    val p2: P,
    val p3: P
) {
    init {
        assert(p1.size == p2.size)
        assert(p2.size == p3.size)
    }

    // ============================================================================
    // 边 / Edges
    // ============================================================================

    /** 获取第一条边（p1 -> p2）/ Get the first edge (p1 -> p2) */
    val e1: Edge<P, D> get() = Edge(p1, p2)
    /** 获取第二条边（p2 -> p3）/ Get the second edge (p2 -> p3) */
    val e2: Edge<P, D> get() = Edge(p2, p3)
    /** 获取第三条边（p3 -> p1）/ Get the third edge (p3 -> p1) */
    val e3: Edge<P, D> get() = Edge(p3, p1)

    /** 获取所有边 / Get all edges */
    val edges: List<Edge<P, D>> get() = listOf(e1, e2, e3)

    /** 获取所有顶点 / Get all vertices */
    val vertices: List<P> get() = listOf(p1, p2, p3)

    // ============================================================================
    // 几何属性 / Geometric properties
    // ============================================================================

    /** 计算周长 / Calculate the perimeter */
    val perimeter: Flt64 by lazy { e1.length + e2.length + e3.length }

    /** 计算重心（三条中线的交点）/ Calculate the centroid (intersection of medians) */
    val centroid: P by lazy {
        @Suppress("UNCHECKED_CAST")
        p1.indices.map { (p1[it] + p2[it] + p3[it]) / Flt64(3.0) }.let {
            Point(it, p1.dim) as P
        }
    }

    /**
     * 判断三角形是否退化（有顶点重合）
     * Check if the triangle is degenerate (vertices coincide)
     *
     * @return 是否退化 / Whether degenerate
     */
    val isDegenerate: Boolean by lazy {
        p1.approxEq(p2) || p2.approxEq(p3) || p3.approxEq(p1)
    }

    /**
     * 判断三角形是否非法（所有维度坐标相同）
     * Check if the triangle is illegal (all coordinates same in some dimension)
     *
     * @return 是否非法 / Whether illegal
     */
    val illegal: Boolean by lazy {
        for (i in p1.indices) {
            if (p1[i] eq p2[i] && p2[i] eq p3[i]) {
                return@lazy true
            }
        }
        false
    }

    // ============================================================================
    // 面积计算 / Area calculation
    // ============================================================================

    /**
     * 计算面积（使用海伦公式）
     * Calculate area using Heron's formula
     *
     * 适用于任意三角形，仅使用边长。
     * Works for any triangle, uses only edge lengths.
     */
    val area: Flt64 by lazy {
        val a = e1.length
        val b = e2.length
        val c = e3.length
        val p = (a + b + c) / Flt64.two
        val s = p * (p - a) * (p - b) * (p - c)
        s.sqrt().toFlt64()
    }

    override fun toString() = "Triangle($p1, $p2, $p3)"
}

typealias Triangle2 = Triangle<Point2, Dim2>
typealias Triangle3 = Triangle<Point3, Dim3>

// ============================================================================
// 2D 三角形特有方法 / 2D triangle specific methods
// ============================================================================

/**
 * 计算 2D 三角形的面积（使用叉积）
 * Calculate the area of a 2D triangle using cross product
 *
 * 更精确的计算方法。
 * More precise calculation method.
 */
fun Triangle2.area2D(): Flt64 {
    val v1x = p2.x - p1.x
    val v1y = p2.y - p1.y
    val v2x = p3.x - p1.x
    val v2y = p3.y - p1.y
    val cross = v1x * v2y - v1y * v2x
    return cross.abs() / Flt64.two
}

/**
 * 判断点是否在三角形内部（包含边界）
 * Check if a point is inside the triangle (including boundary)
 *
 * 使用重心坐标法。
 * Uses barycentric coordinate method.
 *
 * @param point 待检查的点 / The point to check
 * @return 点是否在三角形内 / Whether the point is inside
 */
infix fun Triangle2.containsPoint(point: Point2): Boolean {
    // 使用重心坐标法 / Use barycentric coordinate method
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

    // 计算分母 / Calculate denominator
    val denom = dot00 * dot11 - dot01 * dot01
    if (denom eq Flt64.zero) {
        return false
    }

    // 计算重心坐标 / Calculate barycentric coordinates
    val u = (dot11 * dot02 - dot01 * dot12) / denom
    val v = (dot00 * dot12 - dot01 * dot02) / denom

    // 检查点是否在三角形内 / Check if point is inside triangle
    return u geq Flt64.zero && v geq Flt64.zero && u + v leq Flt64.one
}

/**
 * 计算外接圆
 * Calculate the circumcircle
 *
 * 外接圆是通过三个顶点的唯一圆。
 * The circumcircle is the unique circle passing through all three vertices.
 *
 * @return 外接圆 / The circumcircle
 */
fun Triangle2.circumcircle(): Circle2 {
    return Circle.circumcircleOf(this)
}

/**
 * 计算外心（外接圆圆心）
 * Calculate the circumcenter (center of circumscribed circle)
 *
 * @return 外心 / The circumcenter
 */
fun Triangle2.circumcenter(): Point2 {
    val cc = circumcircle()
    return cc.center
}

/**
 * 计算内心（内切圆圆心）
 * Calculate the incenter (center of inscribed circle)
 *
 * @return 内心 / The incenter
 */
fun Triangle2.incenter(): Point2 {
    val a = e2.length  // 边 p2-p3 的长度
    val b = e3.length  // 边 p3-p1 的长度
    val c = e1.length  // 边 p1-p2 的长度
    val perimeter = a + b + c

    if (perimeter eq Flt64.zero) {
        return p1
    }

    return point2(
        (a * p1.x + b * p2.x + c * p3.x) / perimeter,
        (a * p1.y + b * p2.y + c * p3.y) / perimeter
    )
}

// ============================================================================
// 3D 三角形特有方法 / 3D triangle specific methods
// ============================================================================

/**
 * 计算 3D 三角形的面积（使用叉积的模长）
 * Calculate the area of a 3D triangle using cross product magnitude
 */
fun Triangle3.area3D(): Flt64 {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2
    return cross.norm / Flt64.two
}

/**
 * 计算法向量
 * Calculate the normal vector
 *
 * 返回垂直于三角形平面的单位向量。
 * Returns the unit vector perpendicular to the triangle plane.
 * 零面积返回 null。
 * Returns null for zero area.
 *
 * @return 法向量，或 null（零面积）/ Normal vector, or null (zero area)
 */
fun Triangle3.normal(): Vector3? {
    val v1 = vector3(p2.x - p1.x, p2.y - p1.y, p2.z - p1.z)
    val v2 = vector3(p3.x - p1.x, p3.y - p1.y, p3.z - p1.z)
    val cross = v1 cross v2

    if (cross.norm eq Flt64.zero) {
        return null
    }

    return cross.unit
}







