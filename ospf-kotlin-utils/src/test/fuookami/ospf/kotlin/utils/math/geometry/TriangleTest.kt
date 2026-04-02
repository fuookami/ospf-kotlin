package fuookami.ospf.kotlin.utils.math.geometry

import fuookami.ospf.kotlin.utils.math.algebra.number.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*

/**
 * Triangle 测试类
 * Triangle test class
 *
 * 测试三角形的基本操作：创建、面积、周长、重心、点包含、外接圆等。
 * Tests basic triangle operations: creation, area, perimeter, centroid, point containment, circumcircle, etc.
 */
class TriangleTest {

    // ============================================================================
    // 三角形创建测试 / Triangle creation tests
    // ============================================================================

    @Test
    fun testTriangleCreation() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue(triangle.p1 partialEq p1)
        assertTrue(triangle.p2 partialEq p2)
        assertTrue(triangle.p3 partialEq p3)
    }

    // ============================================================================
    // 边和顶点测试 / Edges and vertices tests
    // ============================================================================

    @Test
    fun testTriangleEdges() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue(triangle.e1.from partialEq p1)
        assertTrue(triangle.e1.to partialEq p2)
        assertTrue(triangle.e2.from partialEq p2)
        assertTrue(triangle.e2.to partialEq p3)
        assertTrue(triangle.e3.from partialEq p3)
        assertTrue(triangle.e3.to partialEq p1)
    }

    @Test
    fun testTriangleVertices() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertEquals(3, triangle.vertices.size)
        assertTrue(triangle.vertices[0] partialEq p1)
        assertTrue(triangle.vertices[1] partialEq p2)
        assertTrue(triangle.vertices[2] partialEq p3)
    }

    // ============================================================================
    // 面积测试 / Area tests
    // ============================================================================

    @Test
    fun testTriangleArea() {
        // 3-4-5 三角形，面积为 6 / 3-4-5 triangle, area is 6
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue((triangle.area - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleArea2D() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        val area2D = triangle.area2D()
        assertTrue((area2D - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleArea3D() {
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(4.0), Flt64(0.0), Flt64(0.0))
        val p3 = point3(Flt64(0.0), Flt64(3.0), Flt64(0.0))
        val triangle = Triangle(p1, p2, p3)

        val area3D = triangle.area3D()
        assertTrue((area3D - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 周长测试 / Perimeter tests
    // ============================================================================

    @Test
    fun testTrianglePerimeter() {
        // 3-4-5 三角形，周长为 12 / 3-4-5 triangle, perimeter is 12
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue((triangle.perimeter - Flt64(12.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTrianglePerimeterEquilateral() {
        // 等边三角形边长为 2，周长为 6 / Equilateral triangle side 2, perimeter 6
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(2.0), Flt64(0.0))
        val p3 = point2(Flt64(1.0), Flt64(Math.sqrt(3.0)))
        val triangle = Triangle(p1, p2, p3)

        assertTrue((triangle.perimeter - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 重心测试 / Centroid tests
    // ============================================================================

    @Test
    fun testTriangleCentroid() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(6.0))
        val triangle = Triangle(p1, p2, p3)

        val centroid = triangle.centroid
        // 重心应为 (2, 2) / Centroid should be (2, 2)
        assertTrue((centroid.x - Flt64(2.0)).abs() ls Flt64(1e-10))
        assertTrue((centroid.y - Flt64(2.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleCentroid3D() {
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(6.0), Flt64(0.0), Flt64(0.0))
        val p3 = point3(Flt64(3.0), Flt64(9.0), Flt64(12.0))
        val triangle = Triangle(p1, p2, p3)

        val centroid = triangle.centroid
        assertTrue((centroid.x - Flt64(3.0)).abs() ls Flt64(1e-10))
        assertTrue((centroid.y - Flt64(3.0)).abs() ls Flt64(1e-10))
        assertTrue((centroid.z - Flt64(4.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 退化测试 / Degenerate tests
    // ============================================================================

    @Test
    fun testTriangleIsDegenerateCoincidentVertices() {
        // 两点重合 / Two vertices coincide
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(0.0), Flt64(0.0))  // 与 p1 重合
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue(triangle.isDegenerate)
    }

    @Test
    fun testTriangleIsNotDegenerate() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertFalse(triangle.isDegenerate)
    }

    // ============================================================================
    // 点包含测试 / Point containment tests
    // ============================================================================

    @Test
    fun testTriangleContainsPointInside() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(4.0))
        val triangle = Triangle(p1, p2, p3)

        val inside = point2(Flt64(1.0), Flt64(1.0))
        assertTrue(triangle containsPoint inside)
    }

    @Test
    fun testTriangleContainsPointOutside() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(4.0))
        val triangle = Triangle(p1, p2, p3)

        val outside = point2(Flt64(3.0), Flt64(3.0))
        assertFalse(triangle containsPoint outside)
    }

    @Test
    fun testTriangleContainsPointOnEdge() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(4.0))
        val triangle = Triangle(p1, p2, p3)

        // 点在边上 / Point on edge
        val onEdge = point2(Flt64(2.0), Flt64(0.0))
        assertTrue(triangle containsPoint onEdge)
    }

    @Test
    fun testTriangleContainsPointOnVertex() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(4.0))
        val triangle = Triangle(p1, p2, p3)

        // 点在顶点上 / Point on vertex
        assertTrue(triangle containsPoint p1)
        assertTrue(triangle containsPoint p2)
        assertTrue(triangle containsPoint p3)
    }

    // ============================================================================
    // 外接圆测试 / Circumcircle tests
    // ============================================================================

    @Test
    fun testTriangleCircumcircle() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(2.0), Flt64(0.0))
        val p3 = point2(Flt64(1.0), Flt64(1.0))
        val triangle = Triangle(p1, p2, p3)

        val circumcircle = triangle.circumcircle()

        // 外心应为 (1, 0)，半径为 1
        // Circumcenter should be (1, 0), radius is 1
        // 验证：圆心到三点距离相等
        // Verify: distances from center to all three points are equal
        assertTrue((circumcircle.center.x - Flt64(1.0)).abs() ls Flt64(1e-10))
        assertTrue((circumcircle.center.y - Flt64(0.0)).abs() ls Flt64(1e-10))
        assertTrue((circumcircle.radius - Flt64(1.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleCircumcenter() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(2.0), Flt64(0.0))
        val p3 = point2(Flt64(1.0), Flt64(Math.sqrt(3.0)))
        val triangle = Triangle(p1, p2, p3)

        val circumcenter = triangle.circumcenter()

        // 等边三角形的外心在重心位置 / Equilateral triangle circumcenter at centroid
        assertTrue((circumcenter.x - Flt64(1.0)).abs() ls Flt64(1e-10))
        assertTrue((circumcenter.y - Flt64(Math.sqrt(3.0) / 3.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 内心测试 / Incenter tests
    // ============================================================================

    @Test
    fun testTriangleIncenter() {
        // 等边三角形的内心在重心位置 / Equilateral triangle incenter at centroid
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(2.0), Flt64(0.0))
        val p3 = point2(Flt64(1.0), Flt64(Math.sqrt(3.0)))
        val triangle = Triangle(p1, p2, p3)

        val incenter = triangle.incenter()

        assertTrue((incenter.x - Flt64(1.0)).abs() ls Flt64(1e-10))
        assertTrue((incenter.y - Flt64(Math.sqrt(3.0) / 3.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleIncenterRightTriangle() {
        // 直角三角形的内心 / Right triangle incenter
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(3.0), Flt64(0.0))
        val p3 = point2(Flt64(0.0), Flt64(4.0))
        val triangle = Triangle(p1, p2, p3)

        val incenter = triangle.incenter()

        // 直角三角形内心距离各边距离相等 = r = (a+b-c)/2 / Right triangle incenter
        val expectedX = Flt64(1.0)
        val expectedY = Flt64(1.0)
        assertTrue((incenter.x - expectedX).abs() ls Flt64(1e-10))
        assertTrue((incenter.y - expectedY).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 法向量测试（3D） / Normal vector tests (3D)
    // ============================================================================

    @Test
    fun testTriangleNormal3D() {
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(1.0), Flt64(0.0), Flt64(0.0))
        val p3 = point3(Flt64(0.0), Flt64(1.0), Flt64(0.0))
        val triangle = Triangle(p1, p2, p3)

        val normal = triangle.normal()

        assertNotNull(normal)
        // 法向量应为 z 方向 / Normal should be z direction
        assertTrue((normal!!.x - Flt64(0.0)).abs() ls Flt64(1e-10))
        assertTrue((normal.y - Flt64(0.0)).abs() ls Flt64(1e-10))
        assertTrue((normal.z.abs() - Flt64(1.0)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testTriangleNormal3DDegenerate() {
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))  // 与 p1 重合
        val p3 = point3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
        val triangle = Triangle(p1, p2, p3)

        val normal = triangle.normal()

        assertNull(normal)
    }

    @Test
    fun testTriangleNormal3DCollinear() {
        // 三点共线 / Three points collinear
        val p1 = point3(Flt64(0.0), Flt64(0.0), Flt64(0.0))
        val p2 = point3(Flt64(1.0), Flt64(1.0), Flt64(0.0))
        val p3 = point3(Flt64(2.0), Flt64(2.0), Flt64(0.0))
        val triangle = Triangle(p1, p2, p3)

        val normal = triangle.normal()

        assertNull(normal)
    }

    // ============================================================================
    // illegal 测试 / Illegal tests
    // ============================================================================

    @Test
    fun testTriangleIllegal() {
        // 所有 x 坐标相同 / All x coordinates same
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(0.0), Flt64(1.0))
        val p3 = point2(Flt64(0.0), Flt64(2.0))
        val triangle = Triangle(p1, p2, p3)

        assertTrue(triangle.illegal)
    }

    @Test
    fun testTriangleNotIllegal() {
        val p1 = point2(Flt64(0.0), Flt64(0.0))
        val p2 = point2(Flt64(4.0), Flt64(0.0))
        val p3 = point2(Flt64(2.0), Flt64(3.0))
        val triangle = Triangle(p1, p2, p3)

        assertFalse(triangle.illegal)
    }
}