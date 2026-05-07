package fuookami.ospf.kotlin.math.geometry

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.ordinary.max
import fuookami.ospf.kotlin.math.ordinary.minMaxOf
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret

data class DelaunayTriangulation2(
    val triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>,
    val points: List<Point<Dim2, Flt64>>
) {
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

fun pointInCircumcircle(point: Point<Dim2, Flt64>, triangle: Triangle<Point<Dim2, Flt64>, Dim2, Flt64>): Boolean {
    return triangle.circumcircle() containsPointStrict point
}

data object Delaunay {
    fun triangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
        val triangles = invoke(points)
        return DelaunayTriangulation2(triangles, points)
    }

    fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2> {
        if (points.size < 3) {
            return Failed(ErrorCode.IllegalArgument, "At least 3 points are required for triangulation.")
        }
        return Ok(triangulate(points))
    }

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
            Point<Dim2, Flt64>(midX - Flt64.two * dMax, midY - dMax),
            Point<Dim2, Flt64>(midX, midY + Flt64.two * dMax),
            Point<Dim2, Flt64>(midX + Flt64.two * dMax, midY - dMax)
        )
    }
}

@JvmName("delaunayTriangulate2")
fun delaunayTriangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2 {
    return Delaunay.triangulate(points)
}

@JvmName("delaunayTriangulate2Ret")
fun delaunayTriangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2> {
    return Delaunay.triangulateRet(points)
}

@JvmName("triangulate2")
fun triangulate(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> {
    return Delaunay(points)
}

@JvmName("triangulate2Ret")
fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>> {
    return Delaunay.invokeRet(points)
}

@JvmName("triangulate3Points")
fun triangulate(points: List<Point<Dim3, Flt64>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>> {
    for (i in points.indices) {
        for (j in (i + 1) until points.size) {
            if ((points[i].x eq points[j].x) && (points[i].y eq points[j].y)) {
                throw IllegalArgumentException("Point<Dim3, Flt64> list contains duplicated projected coordinates at indices $i and $j.")
            }
        }
    }

    fun get(point2: Point<Dim2, Flt64>): Point<Dim3, Flt64> {
        return points.find { (it.x eq point2.x) && (it.y eq point2.y) }!!
    }

    val triangles = triangulate(points.map { Point<Dim2, Flt64>(it.x, it.y) })
    return triangles.map {
        Triangle<Point<Dim3, Flt64>, Dim3, Flt64>(get(it.p1), get(it.p2), get(it.p3))
    }
}

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
