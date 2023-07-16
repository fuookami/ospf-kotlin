package fuookami.ospf.kotlin.utils.math.geometry

object Delaunay {
    // Bowyer-Watson algorithm
    // Kotlin implementation of http://paulbourke.net/papers/triangulate
    operator fun invoke(points: List<Point2>): List<Triangle2> {
        if (points.size < 3) {
            return emptyList()
        }
        val triangles = arrayListOf(superTriangle(points))
        // todo
        return triangles
    }

    private fun superTriangle(points: List<Point2>): Triangle2 {
        // todo
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
