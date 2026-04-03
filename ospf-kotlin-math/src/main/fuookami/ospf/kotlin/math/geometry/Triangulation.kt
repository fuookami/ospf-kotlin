package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.minMaxOf

// ============================================================================
// Delaunay 三角剖分结果 / Delaunay triangulation result
// ============================================================================

/**
 * Delaunay 三角剖分结果
 * Delaunay triangulation result
 *
 * 包含生成的三角形、原始点集和边集合。
 * Contains the generated triangles, original point set, and edge collection.
 *
 * @property triangles 生成的三角形 / Generated triangles
 * @property points 原始点集 / Original point set
 */
data class DelaunayTriangulation2(
    val triangles: List<Triangle2>,
    val points: List<Point2>
) {
    /**
     * 获取所有唯一边
     * Get all unique edges
     *
     * @return 去重后的边列表 / Deduplicated edge list
     */
    val edges: List<Edge2> by lazy {
        val result = mutableListOf<Edge2>()
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
     * 查找点在原始点集中的索引
     * Find indices of points in the original point set
     */
    private fun findPointIndices(p1: Point2, p2: Point2): Pair<Int, Int> {
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

// ============================================================================
// Delaunay 校验函数 / Delaunay validation function
// ============================================================================

/**
 * 验证三角剖分是否满足 Delaunay 条件
 * Verify if triangulation satisfies Delaunay condition
 *
 * Delaunay 条件：任何三角形的外接圆内不包含其他点。
 * Delaunay condition: No other points are contained within the circumcircle of any triangle.
 *
 * @param triangles 三角形列表 / Triangle list
 * @param points 点集 / Point set
 * @return 是否满足 Delaunay 条件 / Whether Delaunay condition is satisfied
 */
fun isDelaunay(triangles: List<Triangle2>, points: List<Point2>): Boolean {
    for (triangle in triangles) {
        val circumcircle = triangle.circumcircle()

        // 检查每个点是否在任何三角形的外接圆内
        // Check if any point is inside the circumcircle of any triangle
        for (point in points) {
            // 跳过三角形的顶点 / Skip triangle vertices
            if (triangle.vertices.any { it approxEq point }) {
                continue
            }

            if (circumcircle containsPointStrict point) {
                return false
            }
        }
    }
    return true
}

// ============================================================================
// Delaunay 三角剖分算法 / Delaunay triangulation algorithm
// ============================================================================

data object Delaunay {
    // Bowyer-Watson algorithm
    // Kotlin implementation of http://paulbourke.net/papers/triangulate

    /**
     * 执行 Delaunay 三角剖分（返回结构化结果）
     * Perform Delaunay triangulation (returns structured result)
     *
     * @param points 输入点集 / Input point set
     * @return 三角剖分结果 / Triangulation result
     */
    fun triangulate(points: List<Point2>): DelaunayTriangulation2 {
        val triangles = invoke(points)
        return DelaunayTriangulation2(triangles, points)
    }

    /**
     * 执行 Delaunay 三角剖分（返回三角形列表，旧 API）
     * Perform Delaunay triangulation (returns triangle list, legacy API)
     *
     * @param points 输入点集 / Input point set
     * @return 三角形列表 / Triangle list
     */
    operator fun invoke(points: List<Point2>): List<Triangle2> {
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
            val edges = ArrayList<Edge2>()
            val thisTriangles = ArrayList<Triangle2>()

            for (triangle in undeterminedTriangles) {
                // check if the point is inside the triangle circum circle
                val circumcircle = Circle2.circumcircleOf(triangle)
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

    private fun deleteDuplicateEdges(edges: List<Edge2>): List<Edge2> {
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

    private fun updateTriangles(triangles: MutableList<Triangle2>, point: Point2, edges: List<Edge2>) {
        triangles.addAll(edges.map { Triangle2(it.from, it.to, point) })
    }

    private fun removeOriginSuperTriangle(
        triangles: MutableList<Triangle2>,
        superTriangle: Triangle2,
        undeterminedTriangles: List<Triangle2>
    ) {
        fun isSuperTriangle(triangle: Triangle2): Boolean {
            return triangle.illegal
                    || (triangle.p1 == superTriangle.p1 || triangle.p2 == superTriangle.p1 || triangle.p3 == superTriangle.p1)
                    || (triangle.p1 == superTriangle.p2 || triangle.p2 == superTriangle.p2 || triangle.p3 == superTriangle.p2)
                    || (triangle.p1 == superTriangle.p3 || triangle.p2 == superTriangle.p3 || triangle.p3 == superTriangle.p3)
        }
        triangles.removeAll { isSuperTriangle(it) }
        triangles.addAll(undeterminedTriangles.filter { !isSuperTriangle(it) })
    }

    private fun getSuperTriangle(points: List<Point2>): Triangle2 {
        val (minX, maxX) = points.minMaxOf { point: Point2 -> point.x }
        val dx = maxX - minX
        val midX = (maxX + minX) / Flt64.two

        val (minY, maxY) = points.minMaxOf { point: Point2 -> point.y }
        val dy = maxY - minY
        val midY = (maxY + minY) / Flt64.two

        val dMax = max(dx, dy)
        return Triangle(
            Point2(midX - Flt64.two * dMax, midY - dMax),
            Point2(midX, midY + Flt64.two * dMax),
            Point2(midX + Flt64.two * dMax, midY - dMax)
        )
    }
}

// ============================================================================
// 便捷函数 / Convenience functions
// ============================================================================

/**
 * 执行 Delaunay 三角剖分（返回结构化结果）
 * Perform Delaunay triangulation (returns structured result)
 *
 * @param points 输入点集 / Input point set
 * @return 三角剖分结果 / Triangulation result
 */
@JvmName("delaunayTriangulate2")
fun delaunayTriangulate(points: List<Point2>): DelaunayTriangulation2 {
    return Delaunay.triangulate(points)
}

/**
 * 执行 Delaunay 三角剖分（返回三角形列表，旧 API）
 * Perform Delaunay triangulation (returns triangle list, legacy API)
 *
 * @param points 输入点集 / Input point set
 * @return 三角形列表 / Triangle list
 */
@JvmName("triangulate2")
fun triangulate(points: List<Point2>): List<Triangle2> {
    return Delaunay(points)
}

@JvmName("triangulate3Points")
fun triangulate(points: List<Point3>): List<Triangle3> {
    for (i in points.indices) {
        for (j in (i + 1) until points.size) {
            if ((points[i].x eq points[j].x) && (points[i].y eq points[j].y)) {
                throw IllegalArgumentException("Point3 list contains duplicated projected coordinates at indices $i and $j.")
            }
        }
    }

    fun get(point2: Point2): Point3 {
        return points.find { (it.x eq point2.x) && (it.y eq point2.y) }!!
    }

    val triangles = triangulate(points.map { Point2(it.x, it.y) })
    return triangles.map {
        Triangle3(get(it.p1), get(it.p2), get(it.p3))
    }
}

@JvmName("triangulate3Isolines")
fun triangulate(isolines: List<Pair<Flt64, List<Point2>>>): List<Triangle3> {
    val triangles = ArrayList<Triangle3>()
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







