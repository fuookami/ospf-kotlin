package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.ordinary.*

object Delaunay {
    // Bowyer-Watson algorithm
    // Kotlin implementation of http://paulbourke.net/papers/triangulate
    operator fun invoke(points: List<Point2>): List<Triangle2> {
        if (points.size < 3) {
            return emptyList()
        }

        val sortedPoints = points.sortedWith { lhs, rhs ->
            when (val result = lhs.x.compareTo(rhs.x)) {
                0 -> { lhs.y.compareTo(rhs.y) }
                else -> { result }
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
                val circumCircle = Circle2.circumCircleOf(triangle)
                val dist = ((circumCircle.x - point.x).sqr() + (circumCircle.y - point.y).sqr()).sqrt()
                if (triangle.illegal || (dist leq circumCircle.radius)) {
                    edges.add(triangle.e1)
                    edges.add(triangle.e2)
                    edges.add(triangle.e3)
                } else if ((circumCircle.x + circumCircle.radius) leq point.x) {
                    triangles.add(triangle)
                } else {
                    thisTriangles.add(triangle)
                }
            }

            val uniqueEdges = deleteDuplicateEdges(edges)
            updateTriangles(thisTriangles, point, uniqueEdges)
            undeterminedTriangles = thisTriangles
        }

        removeOriginSuperTriangle(triangles, superTriangle, undeterminedTriangles)
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
        val (minX, maxX) = minMaxOf(points) { point: Point2 -> point.x }!!
        val dx = maxX - minX
        val midX = (maxX + minX) / Flt64.two

        val (minY, maxY) = minMaxOf(points) { point: Point2 -> point.y }!!
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

@JvmName("triangulate2")
fun triangulate(points: List<Point2>): List<Triangle2> {
    return Delaunay(points)
}

@JvmName("triangulate3")
fun triangulate(points: List<Point3>): List<Triangle3> {
    fun get(point2: Point2): Point3 {
        return points.find { (it.x eq point2.x) && (it.y eq point2.y) }!!
    }

    val triangles = triangulate(points.map { Point2(it.x, it.y) })
    return triangles.map {
        Triangle3(get(it.p1), get(it.p2), get(it.p3))
    }
}
