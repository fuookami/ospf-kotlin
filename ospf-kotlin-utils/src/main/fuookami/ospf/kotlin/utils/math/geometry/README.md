# ospf-kotlin-utils/math/geometry

[中文文档 (README_ch.md)](./README_ch.md)

2D/3D geometry primitives and algorithms for OSPF Kotlin.

## Core Types

### Point / Vector

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Point2<T>` | 2D point | `distance`, `toVector` |
| `Point3<T>` | 3D point | `distance`, `toVector` |
| `Vector2<T>` | 2D vector | `length`, `normalize`, `dot`, `cross` |
| `Vector3<T>` | 3D vector | `length`, `normalize`, `dot`, `cross` |

### Edge / Segment

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Edge2<T>` | 2D line segment | `midpoint`, `pointAt(t)`, `length`, `intersects`, `intersectionPoint`, `closestPoint`, `distanceToPoint`, `containsPoint`, `approxEq` |

### Triangle

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Triangle2<T>` | 2D triangle | `area`, `perimeter`, `centroid`, `containsPoint`, `circumcircle`, `circumcenter`, `incenter`, `edges`, `vertices` |
| `Triangle3<T>` | 3D triangle | `area`, `perimeter`, `centroid`, `normal`, `edges`, `vertices` |

### Circle / Sphere

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Circle2<T>` | 2D circle | `area`, `circumference`, `diameter`, `containsPoint`, `containsPointStrict`, `intersects`, `containsCircle`, `intersectionPoints` |
| `Sphere3<T>` | 3D sphere | `volume`, `surfaceArea`, `containsPoint` |

### Triangulation

| Type | Description | Key Operations |
|------|-------------|----------------|
| `DelaunayTriangulation2<T>` | Delaunay result | `triangles`, `points`, `edges` |
| `triangulate(points)` | Delaunay triangulation | Returns `DelaunayTriangulation2` |
| `isDelaunay(triangles, points)` | Delaunay validation | Boolean check |

## Usage Examples

### Point and Vector Operations

```kotlin
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val p1 = Point2(Flt64(0.0), Flt64(0.0))
val p2 = Point2(Flt64(3.0), Flt64(4.0))

val distance = p1.distance(p2)  // Flt64(5.0)
val v = p2 - p1                 // Vector2(Flt64(3.0), Flt64(4.0))
val normalized = v.normalize()  // Vector2(Flt64(0.6), Flt64(0.8))
```

### Edge Operations

```kotlin
val edge = Edge2(Point2(Flt64(0.0), Flt64(0.0)), Point2(Flt64(10.0), Flt64(0.0)))

val mid = edge.midpoint()               // Point2(Flt64(5.0), Flt64(0.0))
val point = edge.pointAt(Flt64(0.3))     // Point2(Flt64(3.0), Flt64(0.0))
val contains = edge.containsPoint(mid)   // true
val len = edge.length                   // Flt64(10.0)

// Intersection
val other = Edge2(Point2(Flt64(5.0), Flt64(-5.0)), Point2(Flt64(5.0), Flt64(5.0)))
val intersects = edge.intersects(other)  // true
val intersection = edge.intersectionPoint(other)  // Point2(Flt64(5.0), Flt64(0.0))
```

### Triangle Operations

```kotlin
val triangle = Triangle2(
    Point2(Flt64(0.0), Flt64(0.0)),
    Point2(Flt64(4.0), Flt64(0.0)),
    Point2(Flt64(2.0), Flt64(3.0))
)

val area = triangle.area()          // Flt64(6.0)
val perimeter = triangle.perimeter   // Flt64(9.0)
val centroid = triangle.centroid     // Point2(Flt64(2.0), Flt64(1.0))
val circumcircle = triangle.circumcircle()  // Circle2
val circumcenter = triangle.circumcenter()  // Point2
```

### Delaunay Triangulation

```kotlin
val points = listOf(
    Point2(Flt64(0.0), Flt64(0.0)),
    Point2(Flt64(1.0), Flt64(0.0)),
    Point2(Flt64(0.5), Flt64(1.0)),
    Point2(Flt64(0.5), Flt64(0.5))
)

val result = triangulate(points)
// result.triangles: List<Triangle2>
// result.points: List<Point2>
// result.edges: List<Edge2>

val isValid = isDelaunay(result.triangles, result.points)  // true
```

## Boundary Semantics

### Degenerate Cases

| Case | Handling |
|------|----------|
| Degenerate triangle (collinear) | `isDegenerate` returns `true`, area returns 0 |
| Empty triangulation input | Returns empty `DelaunayTriangulation2` |
| Duplicate points | Rejected in triangulation (projection check) |
| Zero-length edge | `approxEq` handles with epsilon tolerance |

### Epsilon Tolerance

All approximate comparisons use configurable epsilon:

```kotlin
val epsilon = Flt64(1e-9)
edge.approxEq(other, epsilon)
circle.containsPoint(point, epsilon)
```

## Performance Notes

| Operation | Complexity | Hotspot Path |
|-----------|------------|--------------|
| `distance` | O(1) | Square root |
| `intersects` (edge) | O(1) | Cross product |
| `triangulate` | O(n log n) | Delaunay algorithm |
| `containsPoint` (triangle) | O(1) | Barycentric coordinates |

For large triangulations (n > 10,000), consider pre-sorting points by x-coordinate.

## Test Coverage

- `EdgeTest.kt`: 29 tests (midpoint, intersection, closest-point, distance)
- `TriangleTest.kt`: 25 tests (area, centroid, circumcircle, incenter)
- `CircleTest.kt`: 24 tests (area, contains, intersects, intersectionPoints)
- `TriangulationTest.kt`: 16 tests (Delaunay validation, result object)

Run tests:

```powershell
mvn -pl ospf-kotlin-utils -Dtest=EdgeTest,TriangleTest,CircleTest,TriangulationTest test
```

## Related

- [Main README](../README.md)
- [Symbol Module](../symbol/README.md)
- [Value Range Module](../algebra/value_range/README.md)