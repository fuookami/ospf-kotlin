/**
 * 三角剖分
 * Triangulation
 *
 * 实现二维 Delaunay 三角剖分算法，支持将点集分解为三角形网格。
 * 同时支持三维点和等值线的三角剖分。
 * Implements 2D Delaunay triangulation algorithm, supporting decomposition of point sets into triangle meshes.
 * Also supports triangulation of 3D points and isolines.
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.math.geometry.point2
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.minMaxOf
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * 二维 Delaunay 三角剖分结果
 * 2D Delaunay triangulation result
 *
 * @property triangles 三角形列表 / List of triangles
 * @property points 输入点集 / Input point set
 */
data class DelaunayTriangulation2(
    val triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>,
    val points: List<Point<Dim2, Flt64>>
) {
    /** 去重后的边列表 / Deduplicated list of edges */
    val edges: List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>> by lazy {
        val result = mutableListOf<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>()
        val seen = mutableSetOf<Pair<Int, Int>>()

        for (triangle in triangles) {
            for (edge in triangle.edges) {
                val (i1, i2) = findPointIndices(edge.from, edge.to)
                val key = if (i1 < i2) i1 to i2 else i2 to i1

                if (!seen.contains(key)) {
                    seen.add(key)
                    result.add(edge)
                }
            }
        }
        result
    }

    private fun findPointIndices(p1: Point<Dim2, Flt64>, p2: Point<Dim2, Flt64>): Pair<Int, Int> {
        var i1 = 0
        var i2 = 0

        for ((i, p) in points.withIndex()) {
            if (p approxEq p1) {
                i1 = i
            }
            if (p approxEq p2) {
                i2 = i
            }
        }

        return i1 to i2
    }
}

/**
 * 判断三角形列表是否满足 Delaunay 条件
 * Check whether a list of triangles satisfies the Delaunay condition
 *
 * @param triangles 三角形列表 / List of triangles
 * @param points 点集 / Point set
 * @return 是否满足 Delaunay 条件 / Whether the Delaunay condition is satisfied
 */
fun isDelaunay(triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>, points: List<Point<Dim2, Flt64>>): Boolean {
    for (triangle in triangles) {
        val circumcircle = triangle.circumcircle()

        for (point in points) {
            if (triangle.vertices.any { it approxEq point }) {
                continue
            }

            if (pointInCircumcircle(point, triangle)) {
                return false
            }
        }
    }
    return true
}

/**
 * 判断点是否在三角形外接圆内（严格）
 * Check whether a point is strictly inside the circumcircle of a triangle
 *
 * @param point 待检测的点 / The point to check
 * @param triangle 三角形 / The triangle
 * @return 是否在外接圆内 / Whether inside the circumcircle
 */
fun pointInCircumcircle(point: Point<Dim2, Flt64>, triangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    return triangle.circumcircle() containsPointStrict point
}

/**
 * Delaunay 三角剖分算法对象
 * Delaunay triangulation algorithm object
 *
 * 提供基于 Bowyer-Watson 算法的二维 Delaunay 三角剖分实现。
 * Provides 2D Delaunay triangulation implementation based on the Bowyer-Watson algorithm.
 */
data object Delaunay {
    /**
     * 对二维点集进行 Delaunay 三角剖分，返回完整结果
     * Perform Delaunay triangulation on a 2D point set, returning full result
     *
     * @param points 二维点集 / 2D point set
     * @return 三角剖分结果 / Triangulation result
     */
    fun triangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
        val triangles = invoke(points)
        return DelaunayTriangulation2(triangles, points)
    }

    /**
     * 对二维点集进行 Delaunay 三角剖分（带错误处理）
     * Perform Delaunay triangulation on a 2D point set (with error handling)
     *
     * @param points 二维点集（至少 3 个点） / 2D point set (at least 3 points)
     * @return 三角剖分结果或错误 / Triangulation result or error
     */
    fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2> {
        if (points.size < 3) {
            return Failed(ErrorCode.IllegalArgument, "At least 3 points are required for triangulation.")
        }
        return Ok(triangulate(points))
    }

    /**
     * 对二维点集进行 Delaunay 三角剖分，返回三角形列表
     * Perform Delaunay triangulation on a 2D point set, returning triangle list
     *
     * @param points 二维点集 / 2D point set
     * @return 三角形列表 / List of triangles
     */
    operator fun invoke(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> {
        if (points.size < 3) {
            return emptyList()
        }

        val sortedPoints = points.sortedWith { lhs, rhs ->
            when (val result = lhs.x.compareTo(rhs.x)) {
                0 -> {
                    lhs.y.compareTo(rhs.y)
                }

                else -> {
                    result
                }
            }
        }
        val superTriangle = getSuperTriangle(sortedPoints)
        val triangles = arrayListOf(superTriangle)
        var undeterminedTriangles = arrayListOf(superTriangle)

        for (point in sortedPoints) {
            val edges = ArrayList<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>()
            val thisTriangles = ArrayList<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>()

            for (triangle in undeterminedTriangles) {
                val circumcircle = Circle.circumcircleOf(triangle)
                val dist = ((circumcircle.x - point.x).sqr() + (circumcircle.y - point.y).sqr()).sqrt()
                if (triangle.illegal || (dist leq circumcircle.radius)) {
                    edges.add(triangle.e1)
                    edges.add(triangle.e2)
                    edges.add(triangle.e3)
                } else if ((circumcircle.x + circumcircle.radius) leq point.x) {
                    triangles.add(triangle)
                } else {
                    thisTriangles.add(triangle)
                }
            }

            val uniqueEdges = deleteDuplicateEdges(edges)
            updateTriangles(
                triangles = thisTriangles,
                point = point,
                edges = uniqueEdges
            )
            undeterminedTriangles = thisTriangles
        }

        removeOriginSuperTriangle(
            triangles = triangles,
            superTriangle = superTriangle,
            undeterminedTriangles = undeterminedTriangles
        )
        return triangles
    }

    /**
     * 对二维点集进行 Delaunay 三角剖分（带错误处理）
     * Perform Delaunay triangulation on a 2D point set (with error handling)
     *
     * @param points 二维点集（至少 3 个点） / 2D point set (at least 3 points)
     * @return 三角形列表或错误 / Triangle list or error
     */
    fun invokeRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>> {
        if (points.size < 3) {
            return Failed(ErrorCode.IllegalArgument, "At least 3 points are required for triangulation.")
        }
        return Ok(invoke(points))
    }

    private fun deleteDuplicateEdges(edges: List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>): List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>> {
        val duplication = edges.map { false }.toMutableList()
        for (i in edges.indices) {
            if (duplication[i]) {
                continue
            }

            for (j in ((i + 1) until edges.size)) {
                if (edges[i] == edges[j]) {
                    duplication[i] = true
                    duplication[j] = true
                }
            }
        }
        return edges.indices.filter { !duplication[it] }.map { edges[it] }
    }

    private fun updateTriangles(triangles: MutableList<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>, point: Point<Dim2, Flt64>, edges: List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>) {
        triangles.addAll(edges.map { Triangle<Point<Dim2, Flt64>, Dim2, Flt64>(it.from, it.to, point) })
    }

    private fun removeOriginSuperTriangle(
        triangles: MutableList<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>,
        superTriangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>,
        undeterminedTriangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>
    ) {
        fun isSuperTriangle(triangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
            return triangle.illegal
                    || (triangle.p1 == superTriangle.p1 || triangle.p2 == superTriangle.p1 || triangle.p3 == superTriangle.p1)
                    || (triangle.p1 == superTriangle.p2 || triangle.p2 == superTriangle.p2 || triangle.p3 == superTriangle.p2)
                    || (triangle.p1 == superTriangle.p3 || triangle.p2 == superTriangle.p3 || triangle.p3 == superTriangle.p3)
        }
        triangles.removeAll { isSuperTriangle(it) }
        triangles.addAll(undeterminedTriangles.filter { !isSuperTriangle(it) })
    }

    private fun getSuperTriangle(points: List<Point<Dim2, Flt64>>): Triangle<Point<Dim2, Flt64>, Dim2, Flt64> {
        val (minX, maxX) = points.minMaxOf { point: Point<Dim2, Flt64> -> point.x }
        val dx = maxX - minX
        val midX = (maxX + minX) / Flt64.two

        val (minY, maxY) = points.minMaxOf { point: Point<Dim2, Flt64> -> point.y }
        val dy = maxY - minY
        val midY = (maxY + minY) / Flt64.two

        val dMax = max(dx, dy)
        return Triangle(
            point2(midX - Flt64.two * dMax, midY - dMax),
            point2(midX, midY + Flt64.two * dMax),
            point2(midX + Flt64.two * dMax, midY - dMax)
        )
    }
}

/** 对二维点集进行 Delaunay 三角剖分，返回完整结果 / Perform Delaunay triangulation on a 2D point set, returning full result */
@JvmName("delaunayTriangulate2")
fun delaunayTriangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
    return Delaunay.triangulate(points)
}

/** 对二维点集进行 Delaunay 三角剖分（带错误处理） / Perform Delaunay triangulation on a 2D point set (with error handling) */
@JvmName("delaunayTriangulate2Ret")
fun delaunayTriangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2> {
    return Delaunay.triangulateRet(points)
}

/** 对二维点集进行三角剖分，返回三角形列表 / Triangulate a 2D point set, returning triangle list */
@JvmName("triangulate2")
fun triangulate(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> {
    return Delaunay(points)
}

/** 对二维点集进行三角剖分（带错误处理） / Triangulate a 2D point set (with error handling) */
@JvmName("triangulate2Ret")
fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>> {
    return Delaunay.invokeRet(points)
}

/**
 * 对三维点集进行三角剖分（投影到 XY 平面）
 * Triangulate a 3D point set (projected onto XY plane)
 *
 * @param points 三维点集（投影坐标不得重复） / 3D point set (projected coordinates must be unique)
 * @return 三维三角形列表 / List of 3D triangles
 */
@JvmName("triangulate3Points")
fun triangulate(points: List<Point<Dim3, Flt64>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>> {
    for (i in points.indices) {
        for (j in (i + 1) until points.size) {
            if ((points[i].x eq points[j].x) && (points[i].y eq points[j].y)) {
                throw IllegalArgumentException("Point<Dim3, Flt64> list contains duplicated projected coordinates at indices $i and $j.")
            }
        }
    }

    fun get(point: Point<Dim2, Flt64>): Point<Dim3, Flt64> {
        return points.find { (it.x eq point.x) && (it.y eq point.y) }!!
    }

    val triangles = triangulate(points.map { point2(it.x, it.y) })
    return triangles.map {
        Triangle<Point<Dim3, Flt64>, Dim3, Flt64>(get(it.p1), get(it.p2), get(it.p3))
    }
}

/**
 * 对等值线集合进行三角剖分，生成三维三角形网格
 * Triangulate a set of isolines to generate a 3D triangle mesh
 *
 * @param isolines 等值线列表，每条由 (Z 值, XY 点集) 组成 / List of isolines, each composed of (Z value, XY point set)
 * @return 三维三角形列表 / List of 3D triangles
 */
@JvmName("triangulate3Isolines")
fun triangulate(isolines: List<Pair<Flt64, List<Point<Dim2, Flt64>>>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>> {
    val triangles = ArrayList<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>()
    for (i in 0 until isolines.lastIndex) {
        val thisLine = isolines[i]
        val nextLine = isolines[i + 1]
        val points = (thisLine.second.map { point3(it.x, it.y, thisLine.first) } + nextLine.second.map {
            point3(
                it.x,
                it.y,
                nextLine.first
            )
        })
        triangles.addAll(triangulate(points))
    }
    return triangles
}
