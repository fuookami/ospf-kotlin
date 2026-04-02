package fuookami.ospf.kotlin.utils.math.benchmark

import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.math.geometry.Distance
import fuookami.ospf.kotlin.utils.math.geometry.Point2
import fuookami.ospf.kotlin.utils.math.geometry.point2
import fuookami.ospf.kotlin.utils.math.geometry.triangulate

object GeometryBenchmarkOps {
    data class GeometryBenchmarkState(
        val points: List<Point2>,
        val isolines: List<Pair<Flt64, List<Point2>>>
    )

    @JvmStatic
    fun createState(pointCount: Int): GeometryBenchmarkState {
        val points = (0 until pointCount).map { i ->
            val x = Flt64((i % 16).toDouble() / 4.0)
            val y = Flt64((i / 16).toDouble() / 4.0 + (i % 5) * 1e-6)
            point2(x, y)
        }
        fun buildIsoline(z: Double, yBase: Double): Pair<Flt64, List<Point2>> {
            val line = (0 until 16).map { i ->
                val x = Flt64(i.toDouble() / 15.0)
                val y = Flt64(yBase + (i % 3) * 0.01)
                point2(x, y)
            }
            return Flt64(z) to line
        }
        val isolines = listOf(
            buildIsoline(10.0, 0.0),
            buildIsoline(20.0, 1.0),
            buildIsoline(30.0, 2.0)
        )
        return GeometryBenchmarkState(points, isolines)
    }

    @JvmStatic
    fun distanceEuclidean2DAsDouble(): Double {
        return Distance.Euclidean(point2(Flt64.zero, Flt64.zero), point2(Flt64(3.0), Flt64(4.0))).toDouble()
    }

    @JvmStatic
    fun distanceManhattan2DAsDouble(): Double {
        return Distance.Manhattan(point2(Flt64.zero, Flt64.zero), point2(Flt64(3.0), Flt64(4.0))).toDouble()
    }

    @JvmStatic
    fun distanceMinkowski3_2DAsDouble(): Double {
        return Distance.Minkowski(3)(point2(Flt64.zero, Flt64.zero), point2(Flt64(3.0), Flt64(4.0))).toDouble()
    }

    @JvmStatic
    fun triangulatePointCloudSize(state: GeometryBenchmarkState): Int {
        return triangulate(state.points).size
    }

    @JvmStatic
    fun triangulateIsolinesSize(state: GeometryBenchmarkState): Int {
        return triangulate(state.isolines).size
    }
}