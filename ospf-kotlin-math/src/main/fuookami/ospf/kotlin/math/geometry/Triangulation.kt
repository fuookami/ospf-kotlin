package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.minMaxOf
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

data class DelaunayTriangulation2(
    val triangles: List<Triangle2>,
    val points: List<Point2>
) {
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

fun isDelaunay(triangles: List<Triangle2>, points: List<Point2>): Boolean {
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

fun pointInCircumcircle(point: Point2, triangle: Triangle2): Boolean {
    return triangle.circumcircle() containsPointStrict point
}

data object Delaunay {
    fun triangulate(points: List<Point2>): DelaunayTriangulation2 {
        val triangles = invoke(points)
        return DelaunayTriangulation2(triangles, points)
    }

    fun triangulateRet(points: List<Point2>): Ret<DelaunayTriangulation2> {
        if (points.size < 3) {
            return Failed(ErrorCode.IllegalArgument, "At least 3 points are required for triangulation.")
        }
        return Ok(triangulate(points))
    }

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

    fun invokeRet(points: List<Point2>): Ret<List<Triangle2>> {
        if (points.size < 3) {
            return Failed(ErrorCode.IllegalArgument, "At least 3 points are required for triangulation.")
        }
        return Ok(invoke(points))
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

@JvmName("delaunayTriangulate2")
fun delaunayTriangulate(points: List<Point2>): DelaunayTriangulation2 {
    return Delaunay.triangulate(points)
}

@JvmName("delaunayTriangulate2Ret")
fun delaunayTriangulateRet(points: List<Point2>): Ret<DelaunayTriangulation2> {
    return Delaunay.triangulateRet(points)
}

@JvmName("triangulate2")
fun triangulate(points: List<Point2>): List<Triangle2> {
    return Delaunay(points)
}

@JvmName("triangulate2Ret")
fun triangulateRet(points: List<Point2>): Ret<List<Triangle2>> {
    return Delaunay.invokeRet(points)
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
