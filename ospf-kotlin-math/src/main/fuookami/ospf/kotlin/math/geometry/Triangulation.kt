/**
 * Triangulation.
 * 中文三角剖分。
 *
 * Implements 2D Delaunay triangulation algorithm, supporting decomposition of point sets into triangle meshes.
 * Also supports triangulation of 3D points and isolines.
 * 中文实现二维 Delaunay 三角剖分算法，支持将点集分解为三角形网格。
 * 中文同时支持三维点和等值线的三角剖分。
 */
package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 2D Delaunay triangulation result.
 * 中文二维 Delaunay 三角剖分结果。
 *
 * @property triangles the list of triangles / 三角形列表
 * @property points the input point set / 输入点集
 */
data class DelaunayTriangulation2(
    val triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>,
    val points: List<Point<Dim2, Flt64>>
) {
    /** The deduplicated list of edges. / 中文去重后的边列表。 */
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

    /**
     * Find the indices of two points in the point set.
     * 中文查找两个点在点集中的索引。
     *
     * @param p1 the first point / 第一个点
     * @param p2 the second point / 第二个点
     * @return the pair of indices / 两个点的索引对
     */
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
 * Check whether a list of triangles satisfies the Delaunay condition.
 * 中文判断三角形列表是否满足 Delaunay 条件。
 *
 * @param triangles the list of triangles / 三角形列表
 * @param points the point set / 点集
 * @return whether the Delaunay condition is satisfied / 是否满足 Delaunay 条件
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
 * Check whether a point is strictly inside the circumcircle of a triangle.
 * 中文判断点是否在三角形外接圆内（严格）。
 *
 * @param point the point to check / 待检测的点
 * @param triangle the triangle / 三角形
 * @return whether inside the circumcircle / 是否在外接圆内
 */
fun pointInCircumcircle(point: Point<Dim2, Flt64>, triangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    return triangle.circumcircle() containsPointStrict point
}

/**
 * Delaunay triangulation algorithm object.
 * 中文Delaunay 三角剖分算法对象。
 *
 * Provides 2D Delaunay triangulation implementation based on the Bowyer-Watson algorithm.
 * 中文提供基于 Bowyer-Watson 算法的二维 Delaunay 三角剖分实现。
 */
data object Delaunay {
    /**
     * Perform Delaunay triangulation on a 2D point set, returning full result.
     * 中文对二维点集进行 Delaunay 三角剖分，返回完整结果。
     *
     * @param points the 2D point set / 二维点集
     * @return the triangulation result / 三角剖分结果
     */
    fun triangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
        val triangles = invoke(points)
        return DelaunayTriangulation2(triangles, points)
    }

    /**
     * Perform Delaunay triangulation on a 2D point set (with error handling).
     * 中文对二维点集进行 Delaunay 三角剖分（带错误处理）。
     *
     * @param points the 2D point set (at least 3 points) / 二维点集（至少 3 个点）
     * @return the triangulation result or error / 三角剖分结果或错误
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

    /**
     * 删除重复边
     * Delete duplicate edges
     *
     * @param edges 边列表 / The list of edges
     * @return 去重后的边列表 / The deduplicated list of edges
     */
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

    /**
     * 使用新点和边界更新三角形列表
     * Update triangle list with a new point and edges
     *
     * @param triangles 三角形列表（可变） / The mutable triangle list
     * @param point 新插入的点 / The newly inserted point
     * @param edges 边界列表 / The list of edges
     */
    private fun updateTriangles(triangles: MutableList<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>, point: Point<Dim2, Flt64>, edges: List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>) {
        triangles.addAll(edges.map { Triangle<Point<Dim2, Flt64>, Dim2, Flt64>(it.from, it.to, point) })
    }

    /**
     * 移除原始超级三角形及其关联三角形
     * Remove the original super triangle and its associated triangles
     *
     * @param triangles 三角形列表（可变） / The mutable triangle list
     * @param superTriangle 超级三角形 / The super triangle
     * @param undeterminedTriangles 未确定的三角形列表 / The list of undetermined triangles
     */
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

    /**
     * 计算包围所有点的超级三角形
     * Compute the super triangle that encloses all points
     *
     * @param points 二维点集 / The 2D point set
     * @return 超级三角形 / The super triangle
     */
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

/**
 * 对二维点集进行 Delaunay 三角剖分，返回完整结果
 * Perform Delaunay triangulation on a 2D point set, returning full result
 *
 * @param points 二维点集 / 2D point set
 * @return 三角剖分结果 / The triangulation result
 */
@JvmName("delaunayTriangulate2")
fun delaunayTriangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
    return Delaunay.triangulate(points)
}

/**
 * 对二维点集进行 Delaunay 三角剖分（带错误处理）
 * Perform Delaunay triangulation on a 2D point set (with error handling)
 *
 * @param points 二维点集（至少 3 个点） / 2D point set (at least 3 points)
 * @return 三角剖分结果或错误 / The triangulation result or error
 */
@JvmName("delaunayTriangulate2Ret")
fun delaunayTriangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2> {
    return Delaunay.triangulateRet(points)
}

/**
 * 对二维点集进行三角剖分，返回三角形列表
 * Triangulate a 2D point set, returning triangle list
 *
 * @param points 二维点集 / 2D point set
 * @return 三角形列表 / List of triangles
 */
@JvmName("triangulate2")
fun triangulate(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> {
    return Delaunay(points)
}

/**
 * 对二维点集进行三角剖分（带错误处理）
 * Triangulate a 2D point set (with error handling)
 *
 * @param points 二维点集（至少 3 个点） / 2D point set (at least 3 points)
 * @return 三角形列表或错误 / Triangle list or error
 */
@JvmName("triangulate2Ret")
fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>> {
    return Delaunay.invokeRet(points)
}

/**
 * 对三维点集进行三角剖分（投影到 XY 平面）
 * Triangulate a 3D point set (projected onto XY plane)
 *
 * @param points 三维点集（投影坐标不得重复） / 3D point set (projected coordinates must be unique)
 * @return 三维三角形列表或失败原因 / List of 3D triangles or failure reason
 */
@JvmName("triangulate3Points")
fun triangulate(points: List<Point<Dim3, Flt64>>): Ret<List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>> {
    for (i in points.indices) {
        for (j in (i + 1) until points.size) {
            if ((points[i].x eq points[j].x) && (points[i].y eq points[j].y)) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "Point<Dim3, Flt64> list contains duplicated projected coordinates at indices $i and $j."
                )
            }
        }
    }

    fun get(point: Point<Dim2, Flt64>): Point<Dim3, Flt64> {
        return points.find { (it.x eq point.x) && (it.y eq point.y) }!!
    }

    val triangles = triangulate(points.map { point2(it.x, it.y) })
    return Ok(triangles.map {
        Triangle<Point<Dim3, Flt64>, Dim3, Flt64>(get(it.p1), get(it.p2), get(it.p3))
    })
}

/**
 * 对等值线集合进行三角剖分，生成三维三角形网格
 * Triangulate a set of isolines to generate a 3D triangle mesh
 *
 * @param isolines 等值线列表，每条由 (Z 值, XY 点集) 组成 / List of isolines, each composed of (Z value, XY point set)
 * @return 三维三角形列表或失败原因 / List of 3D triangles or failure reason
 */
@JvmName("triangulate3Isolines")
fun triangulate(isolines: List<Pair<Flt64, List<Point<Dim2, Flt64>>>>): Ret<List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>> {
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
        when (val result = triangulate(points)) {
            is Ok -> triangles.addAll(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(triangles)
}
