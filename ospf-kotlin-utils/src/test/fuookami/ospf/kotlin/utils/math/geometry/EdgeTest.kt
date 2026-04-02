package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Edge 测试类
 * Edge test class
 *
 * 测试边的基本操作：创建、长度、中点、参数化、相交、最近点等。
 * Tests basic edge operations: creation, length, midpoint, parametric, intersection, closest point, etc.
 */
class EdgeTest {

    // ============================================================================
    // 边创建测试 / Edge creation tests
    // ============================================================================

    @Test
    fun testEdgeCreation() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(3.0), Flt64(4.0))
        val edge = Edge(p1, p2)

        assertTrue(edge.from partialEq p1)
        assertTrue(edge.to partialEq p2)
    }

    // ============================================================================
    // 长度测试 / Length tests
    // ============================================================================

    @Test
    fun testEdgeLength() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(3.0), Flt64(4.0))
        val edge = Edge(p1, p2)

        // 长度应为 5（3-4-5 三角形） / Length should be 5 (3-4-5 triangle)
        assertTrue((edge.length - Flt64(5.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeLengthSquared() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(3.0), Flt64(4.0))
        val edge = Edge(p1, p2)

        // 长度平方应为 25 / Length squared should be 25
        assertTrue((edge.lengthSquared - Flt64(25.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeLength3D() {
        val p1 = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))
        val p2 = point3(Flt64(4.0), Flt64(6.0), Flt64(8.0))
        val edge = Edge(p1, p2)

        // 长度：sqrt(3^2 + 4^2 + 5^2) = sqrt(50)
        val expectedLength = Flt64(Math.sqrt(50.0))
        assertTrue((edge.length - expectedLength).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 中点测试 / Midpoint tests
    // ============================================================================

    @Test
    fun testEdgeMidpoint() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(6.0))
        val edge = Edge(p1, p2)

        val mid = edge.midpoint()
        assertTrue((mid.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((mid.y - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeMidpoint3D() {
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(4.0), Flt64(6.0), Flt64(8.0))
        val edge = Edge(p1, p2)

        val mid = edge.midpoint()
        assertTrue((mid.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((mid.y - Flt64(3.0)).abs() ls Flt64(1e-10))
        assertTrue((mid.z - Flt64(4.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 方向向量测试 / Direction vector tests
    // ============================================================================

    @Test
    fun testEdgeDirection() {
        val p1 = point2(Flt64(1.0), Flt64(2.0))
        val p2 = point2(Flt64(4.0), Flt64(6.0))
        val edge = Edge(p1, p2)

        val dir = edge.direction
        assertTrue((dir.x - Flt64(3.0)).abs() ls Flt64(1e-10))
        assertTrue((dir.y - Flt64(4.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeUnitDirection() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(3.0), Flt64(4.0))
        val edge = Edge(p1, p2)

        val unitDir = edge.unitDirection
        assertNotNull(unitDir)
        // 单位向量应为 (3/5, 4/5) / Unit vector should be (3/5, 4/5)
        assertTrue((unitDir!!.x - Flt64(0.6)).abs() ls Flt64(1e-10))
        assertTrue((unitDir.y - Flt64(0.8)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeUnitDirectionZeroLength() {
        val p1 = point2(Flt64(1.0), Flt64(1.0))
        val p2 = point2(Flt64(1.0), Flt64(1.0))
        val edge = Edge(p1, p2)

        val unitDir = edge.unitDirection
        assertNull(unitDir)
    }

    // ============================================================================
    // 参数化点测试 / Parametric point tests
    // ============================================================================

    @Test
    fun testEdgePointAt() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(6.0))
        val edge = Edge(p1, p2)

        // t = 0 返回起点 / t = 0 returns start
        val q0 = edge.pointAt(Flt64(0.0))
        assertTrue((q0.x - Flt64(0.0)).abs() ls Flt64(1e-10))
        assertTrue((q0.y - Flt64(0.0)).abs() ls Flt64(1e-10))

        // t = 1 返回终点 / t = 1 returns end
        val q1 = edge.pointAt(Flt64(1.0))
        assertTrue((q1.x - Flt64(4.0)).abs() ls Flt64(1e-10))
        assertTrue((q1.y - Flt64(6.0)).abs() ls Flt64(1e-10))

        // t = 0.5 返回中点 / t = 0.5 returns midpoint
        val qHalf = edge.pointAt(Flt64(0.5))
        assertTrue((qHalf.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((qHalf.y - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgePointAtNegative() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val edge = Edge(p1, p2)

        // t = -0.5 返回延长线上的点（起点之前）/ t = -0.5 returns point on extended line (before start)
        val q = edge.pointAt(Flt64(-0.5))
        assertTrue((q.x - Flt64(-2.0)).abs() ls Flt64(1e-10))
        assertTrue((q.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgePointAtBeyondOne() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val edge = Edge(p1, p2)

        // t = 1.5 返回延长线上的点（终点之后）/ t = 1.5 returns point on extended line (after end)
        val q = edge.pointAt(Flt64(1.5))
        assertTrue((q.x - Flt64(6.0)).abs() ls Flt64(1e-10))
        assertTrue((q.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 点包含测试 / Point containment tests
    // ============================================================================

    @Test
    fun testEdgeContainsPointOnEdge() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 点在边上 / Point on edge
        val p1 = point2(Flt64(2.0), Flt64(0.0))
        assertTrue(edge.containsPoint(p1, Flt64(1e-10)))

        // 起点 / Start point
        assertTrue(edge.containsPoint(edge.from, Flt64(1e-10)))

        // 终点 / End point
        assertTrue(edge.containsPoint(edge.to, Flt64(1e-10)))
    }

    @Test
    fun testEdgeContainsPointNotOnEdge() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 点不在边上 / Point not on edge
        val p2 = point2(Flt64(2.0), Flt64(1.0))
        assertFalse(edge.containsPoint(p2, Flt64(1e-10)))
    }

    // ============================================================================
    // 相交测试 / Intersection tests
    // ============================================================================

    @Test
    fun testEdgeIntersectsCrossing() {
        // 相交的边 / Intersecting edges
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(2.0), Flt64(2.0)))
        val e2 = Edge(point2(Flt64(0.0), Flt64(2.0)), point2(Flt64(2.0), Flt64(0.0)))

        assertTrue(e1 intersects e2)

        val intersection = e1 intersectionPoint e2
        assertNotNull(intersection)
        assertTrue((intersection!!.x - Flt64(1.0)).abs() ls Flt64(1e-10))
        assertTrue((intersection.y - Flt64(1.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeIntersectsNotCrossing() {
        // 不相交的边（平行但不重叠）/ Non-intersecting edges (parallel but not overlapping)
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(1.0), Flt64(0.0)))
        val e2 = Edge(point2(Flt64(0.0), Flt64(1.0)), point2(Flt64(1.0), Flt64(1.0)))

        assertFalse(e1 intersects e2)
        assertNull(e1 intersectionPoint e2)
    }

    @Test
    fun testEdgeIntersectsParallel() {
        // 平行边 / Parallel edges
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(2.0), Flt64(0.0)))
        val e2 = Edge(point2(Flt64(0.0), Flt64(1.0)), point2(Flt64(2.0), Flt64(1.0)))

        assertFalse(e1 intersects e2)
        assertNull(e1 intersectionPoint e2)
    }

    @Test
    fun testEdgeIntersectsEndpoint() {
        // 端点相交（不共线）/ Endpoint intersection (not collinear)
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(2.0), Flt64(0.0)))
        val e2 = Edge(point2(Flt64(2.0), Flt64(0.0)), point2(Flt64(2.0), Flt64(2.0)))

        assertTrue(e1 intersects e2)
        val intersection = e1 intersectionPoint e2
        assertNotNull(intersection)
        assertTrue((intersection!!.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((intersection.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeIntersectsCollinearEndpoint() {
        // 共线边端点相接，返回 null（保守处理）/ Collinear edges with endpoint touch, returns null (conservative)
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(2.0), Flt64(0.0)))
        val e2 = Edge(point2(Flt64(2.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 共线边被认为不相交 / Collinear edges are considered not intersecting
        assertFalse(e1 intersects e2)
        assertNull(e1 intersectionPoint e2)
    }

    // ============================================================================
    // 最近点测试 / Closest point tests
    // ============================================================================

    @Test
    fun testEdgeClosestPointAbove() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 点在边正上方 / Point directly above edge
        val p = point2(Flt64(2.0), Flt64(3.0))
        val closest = edge closestPoint p

        assertTrue((closest.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((closest.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeClosestPointBeyondEnd() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 点在边延长线上（终点之后）/ Point on extended line (after end)
        val p = point2(Flt64(5.0), Flt64(0.0))
        val closest = edge closestPoint p

        // 最近点应限制在终点 / Closest point should be clamped to end
        assertTrue((closest.x - Flt64(4.0)).abs() ls Flt64(1e-10))
        assertTrue((closest.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeClosestPointBeforeStart() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        // 点在起点外侧 / Point outside start
        val p = point2(Flt64(-1.0), Flt64(2.0))
        val closest = edge closestPoint p

        // 最近点应限制在起点 / Closest point should be clamped to start
        assertTrue((closest.x - Flt64(0.0)).abs() ls Flt64(1e-10))
        assertTrue((closest.y - Flt64(0.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeClosestPointZeroLength() {
        val edge = Edge(point2(Flt64(1.0), Flt64(1.0)), point2(Flt64(1.0), Flt64(1.0)))

        val p = point2(Flt64(2.0), Flt64(3.0))
        val closest = edge closestPoint p

        // 零长度边的最近点就是起点 / Closest point on zero-length edge is the start
        assertTrue((closest.x - Flt64(1.0)).abs() ls Flt64(1e-10))
        assertTrue((closest.y - Flt64(1.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 点到边距离测试 / Distance to point tests
    // ============================================================================

    @Test
    fun testEdgeDistanceToPoint() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        val p = point2(Flt64(2.0), Flt64(3.0))
        val dist = edge distanceToPoint p

        assertTrue((dist - Flt64(3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testEdgeDistanceToPointOnEdge() {
        val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(4.0), Flt64(0.0)))

        val p = point2(Flt64(2.0), Flt64(0.0))
        val dist = edge distanceToPoint p

        assertTrue(dist ls Flt64(1e-10))
    }

    // ============================================================================
    // 近似相等测试 / Approximate equality tests
    // ============================================================================

    @Test
    fun testEdgeApproxEq() {
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))
        // 使用比 decimalPrecision 更小的 diff / Use smaller diff than decimalPrecision
        val diff = Flt64(1e-17)
        val e2 = Edge(
            point2(Flt64(0.0) + diff, Flt64(0.0) + diff),
            point2(Flt64(3.0) + diff, Flt64(4.0) + diff)
        )

        assertTrue(e1 approxEq e2)
    }

    @Test
    fun testEdgeApproxEqNotEqual() {
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))
        val e2 = Edge(point2(Flt64(0.1), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))

        assertFalse(e1 approxEq e2)
    }

    @Test
    fun testEdgeApproxEqUndirected() {
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))
        val e2 = Edge(point2(Flt64(3.0), Flt64(4.0)), point2(Flt64(0.0), Flt64(0.0)))

        // 方向相反但仍近似相等 / Equal ignoring direction
        assertTrue(e1 approxEqUndirected e2)
    }

    @Test
    fun testEdgeApproxEqUndirectedNotEqual() {
        val e1 = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))
        val e2 = Edge(point2(Flt64(0.1), Flt64(0.0)), point2(Flt64(3.0), Flt64(4.0)))

        assertFalse(e1 approxEqUndirected e2)
    }
}