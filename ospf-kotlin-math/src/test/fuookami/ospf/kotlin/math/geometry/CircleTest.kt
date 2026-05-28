package fuookami.ospf.kotlin.math.geometry

import kotlin.math.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.*

/**
 * Circle 测试类
 * Circle test class
 *
 * 测试圆的基本操作：面积、周长、点包含、相交、交点等。
 * Tests basic circle operations: area, circumference, point containment, intersection, etc.
 */
class CircleTest {

    // ============================================================================
    // 圆创建测试 / Circle creation tests
    // ============================================================================

    @Test
    fun testCircleCreation() {
        val center = point2(Flt64(1.0), Flt64(2.0))
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(center, vector2(Flt64(5.0), Flt64.zero))

        assertTrue(circle.center partialEq center)
        assertTrue((circle.radius - Flt64(5.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 面积测试 / Area tests
    // ============================================================================

    @Test
    fun testCircleArea() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 面积 = π * r² = π * 9
        val expectedArea = PI * 9.0
        assertTrue((circle.area - Flt64(expectedArea)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testCircleAreaUnit() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64.one, Flt64.zero))

        // 单位圆面积 = π
        assertTrue((circle.area - Flt64(PI)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 周长测试 / Circumference tests
    // ============================================================================

    @Test
    fun testCircleCircumference() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 周长 = 2 * π * r = 2 * π * 3 = 6π
        val expectedCircumference = 2.0 * PI * 3.0
        assertTrue((circle.circumference - Flt64(expectedCircumference)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 直径测试 / Diameter tests
    // ============================================================================

    @Test
    fun testCircleDiameter() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        assertTrue((circle.diameter - Flt64(6.0)).abs() ls Flt64(1e-10))
    }

    // ============================================================================
    // 点包含测试 / Point containment tests
    // ============================================================================

    @Test
    fun testCircleContainsPointCenter() {
        val center = point2(Flt64.zero, Flt64.zero)
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(center, vector2(Flt64(5.0), Flt64.zero))

        // 圆心在圆内 / Center is inside
        assertTrue(circle containsPoint center)
    }

    @Test
    fun testCircleContainsPointOnBoundary() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 圆上的点 / Point on boundary
        assertTrue(circle containsPoint point2(Flt64(5.0), Flt64.zero))
        assertTrue(circle containsPoint point2(Flt64(3.0), Flt64(4.0)))
    }

    @Test
    fun testCircleContainsPointOutside() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 圆外的点 / Point outside
        assertFalse(circle containsPoint point2(Flt64(6.0), Flt64.zero))
        assertFalse(circle containsPoint point2(Flt64(4.0), Flt64(4.0)))
    }

    @Test
    fun testCircleContainsPointStrict() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 边界点不在严格内部 / Boundary not strictly inside
        assertFalse(circle containsPointStrict point2(Flt64(5.0), Flt64.zero))
        // 内部点在严格内部 / Interior is strictly inside
        assertTrue(circle containsPointStrict point2(Flt64(3.0), Flt64(3.0)))
    }

    // ============================================================================
    // 两圆相交测试 / Circle intersection tests
    // ============================================================================

    @Test
    fun testCircleIntersectsTrue() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(5.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 相交（圆心距 5，半径和 6）/ Intersect
        assertTrue(c1 intersects c2)
    }

    @Test
    fun testCircleIntersectsFalse() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c3 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(10.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 相离（圆心距 10，半径和 6）/ Separate
        assertFalse(c1 intersects c3)
    }

    @Test
    fun testCircleIntersectsTangent() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(6.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 外切（圆心距 6，半径和 6）/ External tangent
        assertTrue(c1 intersects c2)
    }

    // ============================================================================
    // 两圆包含测试 / Circle containment tests
    // ============================================================================

    @Test
    fun testCircleContainsCircleTrue() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(10.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(2.0), Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // c2 在 c1 内 / c2 inside c1
        assertTrue(c1 containsCircle c2)
    }

    @Test
    fun testCircleContainsCircleFalse() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(10.0), Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // c2 不在 c1 内 / c2 not inside c1
        assertFalse(c1 containsCircle c2)
    }

    @Test
    fun testCircleContainsCircleConcentric() {
        // 同心圆 / Concentric circles
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(10.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        assertTrue(c1 containsCircle c2)
        assertFalse(c2 containsCircle c1)
    }

    // ============================================================================
    // 两圆交点测试 / Intersection points tests
    // ============================================================================

    @Test
    fun testCircleIntersectionPointsTwo() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(4.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 两个交点 / Two intersections
        val points = c1 intersectionPoints c2
        assertEquals(2, points.size)

        // 验证交点在两圆上 / Verify points are on both circles
        for (p in points) {
            val d1 = (p.x * p.x + p.y * p.y).sqrt()
            val d2 = ((p.x - Flt64(4.0)).sqr() + p.y.sqr()).sqrt()
            assertTrue((d1 - Flt64(5.0)).abs() ls Flt64(1e-10))
            assertTrue((d2 - Flt64(3.0)).abs() ls Flt64(1e-10))
        }
    }

    @Test
    fun testCircleIntersectionPointsNone() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(10.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 无交点 / No intersection
        val points = c1 intersectionPoints c2
        assertTrue(points.isEmpty())
    }

    @Test
    fun testCircleIntersectionPointsTangent() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(6.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 外切，一个交点 / External tangent, one intersection
        val points = c1 intersectionPoints c2
        assertEquals(1, points.size)
        // 交点应在 (3, 0) / Intersection at (3, 0)
        assertTrue((points[0].x - Flt64(3.0)).abs() ls Flt64(1e-10))
        assertTrue((points[0].y - Flt64.zero).abs() ls Flt64(1e-10))
    }

    @Test
    fun testCircleIntersectionPointsSameCircle() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 同一个圆，无交点 / Same circle, no intersection points
        val points = c1 intersectionPoints c2
        assertTrue(points.isEmpty())
    }

    // ============================================================================
    // 球体测试 / Sphere tests
    // ============================================================================

    @Test
    fun testSphereVolume() {
        val sphere = Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>(point3(Flt64.zero, Flt64.zero, Flt64.zero), vector3(Flt64(3.0), Flt64.zero, Flt64.zero))

        // 体积 = (4/3) * π * r³ = (4/3) * π * 27
        val expectedVolume = (4.0 / 3.0) * PI * 27.0
        assertTrue((sphere.volume - Flt64(expectedVolume)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testSphereSurfaceArea() {
        val sphere = Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>(point3(Flt64.zero, Flt64.zero, Flt64.zero), vector3(Flt64(3.0), Flt64.zero, Flt64.zero))

        // 表面积 = 4 * π * r² = 4 * π * 9
        val expectedArea = 4.0 * PI * 9.0
        assertTrue((sphere.surfaceArea - Flt64(expectedArea)).abs() ls Flt64(1e-10))
    }

    @Test
    fun testSphereContainsPoint() {
        val sphere = Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>(point3(Flt64.zero, Flt64.zero, Flt64.zero), vector3(Flt64(5.0), Flt64.zero, Flt64.zero))

        // 球心在球内 / Center is inside
        assertTrue(sphere containsPoint point3(Flt64.zero, Flt64.zero, Flt64.zero))

        // 球面上的点 / Point on surface
        assertTrue(sphere containsPoint point3(Flt64(5.0), Flt64.zero, Flt64.zero))
        assertTrue(sphere containsPoint point3(Flt64(3.0), Flt64(4.0), Flt64.zero))

        // 球外的点 / Point outside
        assertFalse(sphere containsPoint point3(Flt64(6.0), Flt64.zero, Flt64.zero))
    }

    // ============================================================================
    // 边界情况测试 / Edge case tests
    // ============================================================================

    @Test
    fun testCircleZeroRadius() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(5.0), Flt64(5.0)), vector2(Flt64.zero, Flt64.zero))

        assertTrue(circle.area ls Flt64(1e-10))
        assertTrue(circle.circumference ls Flt64(1e-10))
        assertTrue(circle.diameter ls Flt64(1e-10))

        // 只有圆心在圆内 / Only center is inside
        assertTrue(circle containsPoint point2(Flt64(5.0), Flt64(5.0)))
        assertFalse(circle containsPoint point2(Flt64(5.0), Flt64(6.0)))
    }

    @Test
    fun testCircleLargeRadius() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(1e6), Flt64.zero))

        // 面积和周长应正确计算 / Area and circumference should compute correctly
        assertTrue(circle.area gr Flt64.zero)
        assertTrue(circle.circumference gr Flt64.zero)

        // 远处的点 / Distant point
        assertTrue(circle containsPoint point2(Flt64(1e6), Flt64.zero))
        assertFalse(circle containsPoint point2(Flt64(1e6 + 1.0), Flt64.zero))
    }

    // ============================================================================
    // 边界点测试 / Point on boundary tests
    // ============================================================================

    @Test
    fun testCirclePointOnBoundaryTrue() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 圆上的点 / Points on boundary
        assertTrue(circle.pointOnBoundary(point2(Flt64(5.0), Flt64.zero)))
        assertTrue(circle.pointOnBoundary(point2(Flt64.zero, Flt64(5.0))))
        assertTrue(circle.pointOnBoundary(point2(Flt64(3.0), Flt64(4.0))))
        assertTrue(circle.pointOnBoundary(point2(Flt64(-5.0), Flt64.zero)))
    }

    @Test
    fun testCirclePointOnBoundaryFalse() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 圆内的点 / Point inside
        assertFalse(circle.pointOnBoundary(point2(Flt64.zero, Flt64.zero)))
        assertFalse(circle.pointOnBoundary(point2(Flt64(3.0), Flt64(3.0))))

        // 圆外的点 / Point outside
        assertFalse(circle.pointOnBoundary(point2(Flt64(6.0), Flt64.zero)))
    }

    @Test
    fun testCirclePointOnBoundaryWithEpsilon() {
        val circle = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))

        // 接近边界的点 / Point near boundary
        val nearPoint = point2(Flt64(5.0001), Flt64.zero)
        assertTrue(circle.pointOnBoundary(nearPoint, Flt64(0.001)))
        assertFalse(circle.pointOnBoundary(nearPoint, Flt64(1e-10)))
    }

    // ============================================================================
    // 相切测试 / Tangent tests
    // ============================================================================

    @Test
    fun testCircleIsTangentExternal() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(6.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 外切：圆心距 = 半径和 / External tangent: distance = sum of radii
        assertTrue(c1.isTangent(c2))
        assertTrue(c2.isTangent(c1))
    }

    @Test
    fun testCircleIsTangentInternal() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(3.0), Flt64.zero), vector2(Flt64(2.0), Flt64.zero))

        // 内切：圆心距 = 半径差 / Internal tangent: distance = difference of radii
        assertTrue(c1.isTangent(c2))
        assertTrue(c2.isTangent(c1))
    }

    @Test
    fun testCircleIsTangentFalse() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 相交 / Intersecting
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(4.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        assertFalse(c1.isTangent(c2))

        // 相离 / Separate
        val c3 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(10.0), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        assertFalse(c1.isTangent(c3))

        // 包含 / Contained
        val c4 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(1.0), Flt64.zero), vector2(Flt64(1.0), Flt64.zero))
        assertFalse(c1.isTangent(c4))
    }

    @Test
    fun testCircleIsTangentWithEpsilon() {
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        // 接近外切 / Near external tangent
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64(6.001), Flt64.zero), vector2(Flt64(3.0), Flt64.zero))
        assertTrue(c1.isTangent(c2, Flt64(0.01)))
        assertFalse(c1.isTangent(c2, Flt64(1e-10)))
    }

    @Test
    fun testCircleIsTangentConcentric() {
        // 同心圆不相切 / Concentric circles are not tangent
        val c1 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(5.0), Flt64.zero))
        val c2 = Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>(point2(Flt64.zero, Flt64.zero), vector2(Flt64(3.0), Flt64.zero))

        assertFalse(c1.isTangent(c2))
    }
}
