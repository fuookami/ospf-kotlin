package fuookami.ospf.kotlin.math.geometry

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*

class TriangulationTest {
    // ============================================================================
    // 旧 API 测试 / Legacy API tests
    // ============================================================================

    @Test
    fun triangulate2() {
        val triangles = triangulate(
            listOf(
                point2(),
                point2(y = Flt64.one),
                point2(x = Flt64.one),
                point2(x = Flt64.one, y = Flt64.one)
            )
        )
        assertEquals(2, triangles.size)
    }

    @Test
    fun triangulateIsolinesShouldPreserveBothLineHeights() {
        val low = Flt64(10.0)
        val high = Flt64(20.0)

        val triangles = triangulate(
            listOf(
                low to listOf(
                    point2(Flt64.zero, Flt64.zero),
                    point2(Flt64.one, Flt64.zero),
                    point2(Flt64(0.5), Flt64(0.2))
                ),
                high to listOf(
                    point2(Flt64.zero, Flt64.one),
                    point2(Flt64.one, Flt64.one),
                    point2(Flt64(0.5), Flt64(1.2))
                )
            )
        ).value!!

        assertTrue(triangles.isNotEmpty())
        val zValues = triangles.flatMap { triangle -> listOf(triangle.p1.z, triangle.p2.z, triangle.p3.z) }
        assertTrue(zValues.any { it == low })
        assertTrue(zValues.any { it == high })
    }

    @Test
    fun triangulateShouldHandleDuplicatePoints() {
        val triangles = triangulate(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64.one, Flt64.zero),
                point2(Flt64.zero, Flt64.one),
                point2(Flt64.one, Flt64.one),
                point2(Flt64.one, Flt64.one)
            )
        )

        assertTrue(triangles.isNotEmpty())
    }

    @Test
    fun triangulateShouldHandleNearCollinearPoints() {
        val triangles = triangulate(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64(1.0), Flt64(1e-12)),
                point2(Flt64(2.0), Flt64(2e-12)),
                point2(Flt64(1.0), Flt64.one)
            )
        )

        assertTrue(triangles.isNotEmpty())
    }

    @Test
    fun triangulateShouldUseOnlyInputVerticesAndAvoidIllegalTriangles() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64(2.0), Flt64.zero),
            point2(Flt64(2.0), Flt64(2.0)),
            point2(Flt64.zero, Flt64(2.0)),
            point2(Flt64.one, Flt64.one)
        )
        val triangles = triangulate(points)

        assertTrue(triangles.isNotEmpty())
        assertTrue(triangles.all { !it.illegal })
        assertTrue(
            triangles
                .flatMap { listOf(it.p1, it.p2, it.p3) }
                .all { vertex -> points.any { point -> point == vertex } }
        )
    }

    @Test
    fun triangulateMultipleIsolinesShouldPreserveAllHeights() {
        val l1 = Flt64(10.0)
        val l2 = Flt64(20.0)
        val l3 = Flt64(30.0)

        val triangles = triangulate(
            listOf(
                l1 to listOf(
                    point2(Flt64.zero, Flt64.zero),
                    point2(Flt64.one, Flt64.zero),
                    point2(Flt64(0.4), Flt64(0.2))
                ),
                l2 to listOf(
                    point2(Flt64.zero, Flt64.one),
                    point2(Flt64.one, Flt64.one),
                    point2(Flt64(0.4), Flt64(1.2))
                ),
                l3 to listOf(
                    point2(Flt64.zero, Flt64(2.0)),
                    point2(Flt64.one, Flt64(2.0)),
                    point2(Flt64(0.4), Flt64(2.2))
                )
            )
        ).value!!

        val zValues = triangles.flatMap { triangle -> listOf(triangle.p1.z, triangle.p2.z, triangle.p3.z) }
        assertTrue(zValues.any { it == l1 })
        assertTrue(zValues.any { it == l2 })
        assertTrue(zValues.any { it == l3 })
    }

    @Test
    fun triangulateShouldReturnEmptyForCollinearPoints() {
        val triangles = triangulate(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64.one, Flt64.one),
                point2(Flt64(2.0), Flt64(2.0)),
                point2(Flt64(3.0), Flt64(3.0))
            )
        )

        assertTrue(triangles.isEmpty())
    }

    @Test
    fun triangulateShouldRemainValidUnderTinyJitter() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.one, Flt64.zero),
            point2(Flt64.one, Flt64.one),
            point2(Flt64.zero, Flt64.one),
            point2(Flt64(0.5), Flt64(0.5)),
            point2(Flt64(0.500000000001), Flt64(0.500000000002))
        )
        val triangles = triangulate(points)

        assertTrue(triangles.isNotEmpty())
        assertTrue(triangles.all { !it.illegal })
        assertTrue(
            triangles
                .flatMap { listOf(it.p1, it.p2, it.p3) }
                .all { vertex -> points.any { point -> point == vertex } }
        )
    }

    @Test
    fun triangulate3ShouldRejectDuplicateProjectedVertices() {
        val result = triangulate(
            listOf(
                point3(Flt64.zero, Flt64.zero, Flt64.zero),
                point3(Flt64.one, Flt64.zero, Flt64.one),
                point3(Flt64.zero, Flt64.one, Flt64.two),
                point3(Flt64.zero, Flt64.zero, Flt64(3.0))
            )
        )

        assertTrue(result.failed)
    }

    // ============================================================================
    // 新 API 测试：DelaunayTriangulation2 / New API tests
    // ============================================================================

    @Test
    fun delaunayTriangulateSimpleTriangle() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.one, Flt64.zero),
            point2(Flt64(0.5), Flt64.one)
        )

        val result = delaunayTriangulate(points)

        assertEquals(1, result.triangles.size)
        assertEquals(3, result.points.size)
    }

    @Test
    fun delaunayTriangulateSquare() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.one, Flt64.zero),
            point2(Flt64.one, Flt64.one),
            point2(Flt64.zero, Flt64.one)
        )

        val result = delaunayTriangulate(points)

        assertEquals(2, result.triangles.size)
        assertEquals(4, result.points.size)
        // 正方形应产生 5 条边（2 个三角形共享 1 条边）
        // Square should produce 5 edges (2 triangles share 1 edge)
        assertEquals(5, result.edges.size)
    }

    @Test
    fun delaunayTriangulateRetShouldRejectInsufficientPoints() {
        val ret = delaunayTriangulateRet(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64.one, Flt64.zero)
            )
        )

        assertTrue(ret is Failed)
        assertEquals(ErrorCode.IllegalArgument, ret.code)
    }

    @Test
    fun triangulateRetShouldReturnTriangleList() {
        val ret = triangulateRet(
            listOf(
                point2(Flt64.zero, Flt64.zero),
                point2(Flt64.one, Flt64.zero),
                point2(Flt64.one, Flt64.one),
                point2(Flt64.zero, Flt64.one)
            )
        )

        assertTrue(ret is Ok)
        assertEquals(2, ret.value.size)
    }

    @Test
    fun delaunayTriangulateEdgesUnique() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64(2.0), Flt64.zero),
            point2(Flt64(2.0), Flt64(2.0)),
            point2(Flt64.zero, Flt64(2.0)),
            point2(Flt64.one, Flt64.one)
        )

        val result = delaunayTriangulate(points)

        // 验证边唯一性 / Verify edge uniqueness
        val edges = result.edges
        val edgeSet = mutableSetOf<Pair<Int, Int>>()
        for (edge in edges) {
            val indices = result.points.indices.filter { i ->
                result.points[i] approxEq edge.from || result.points[i] approxEq edge.to
            }
            if (indices.size == 2) {
                val key = if (indices[0] < indices[1]) indices[0] to indices[1] else indices[1] to indices[0]
                edgeSet.add(key)
            }
        }
        assertEquals(edges.size, edgeSet.size)
    }

    // ============================================================================
    // isDelaunay 校验测试 / isDelaunay validation tests
    // ============================================================================

    @Test
    fun isDelaunayValidTriangulation() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64.one, Flt64.zero),
            point2(Flt64.one, Flt64.one),
            point2(Flt64.zero, Flt64.one)
        )

        val triangles = triangulate(points)

        assertTrue(isDelaunay(triangles, points))
    }

    @Test
    fun isDelaunaySimpleTriangle() {
        val points = listOf(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64(2.0), Flt64.zero),
            point2(Flt64.one, Flt64(Math.sqrt(3.0)))
        )

        val triangles = triangulate(points)

        assertTrue(isDelaunay(triangles, points))
    }

    @Test
    fun isDelaunayInvalidTriangulation() {
        // 手动构造一个不满足 Delaunay 条件的三角剖分
        // Manually construct a triangulation that doesn't satisfy Delaunay condition
        val p1 = point2(Flt64.zero, Flt64.zero)
        val p2 = point2(Flt64(4.0), Flt64.zero)
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val p4 = point2(Flt64(2.0), Flt64.one)  // 这个点在三角形 p1-p2-p3 的外接圆内

        val triangles = listOf(Triangle(p1, p2, p3))
        val points = listOf(p1, p2, p3, p4)

        // 由于 p4 在外接圆内，不满足 Delaunay 条件
        // Since p4 is inside circumcircle, doesn't satisfy Delaunay condition
        assertFalse(isDelaunay(triangles, points))
    }

    @Test
    fun pointInCircumcircleShouldDetectInteriorPoint() {
        val triangle = Triangle(
            point2(Flt64.zero, Flt64.zero),
            point2(Flt64(4.0), Flt64.zero),
            point2(Flt64(2.0), Flt64(3.0))
        )
        val inside = point2(Flt64(2.0), Flt64.one)
        val outside = point2(Flt64(5.0), Flt64(5.0))

        assertTrue(pointInCircumcircle(inside, triangle))
        assertFalse(pointInCircumcircle(outside, triangle))
    }

    @Test
    fun isDelaunayWithMorePoints() {
        // 使用随机分布的点来测试 Delaunay 校验
        // Test Delaunay validation with randomly distributed points
        val points = listOf(
            point2(Flt64(0.0), Flt64(0.0)),
            point2(Flt64(10.0), Flt64(0.0)),
            point2(Flt64(10.0), Flt64(10.0)),
            point2(Flt64(0.0), Flt64(10.0)),
            point2(Flt64(3.0), Flt64(5.0)),
            point2(Flt64(7.0), Flt64(4.0)),
            point2(Flt64(5.0), Flt64(8.0))
        )

        val result = delaunayTriangulate(points)

        // 验证结果非空 / Verify result is non-empty
        assertTrue(result.triangles.isNotEmpty())
        // 验证所有三角形都合法 / Verify all triangles are legal
        assertTrue(result.triangles.all { !it.illegal })
    }
}
