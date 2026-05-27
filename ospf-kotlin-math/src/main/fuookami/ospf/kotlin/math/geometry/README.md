# ospf-kotlin-math/geometry

[中文文档 (README_ch.md)](./README_ch.md)

Dimension-generic 2D/3D geometry primitives and algorithms for OSPF Kotlin.

## Core Types

### Dimension

| Type | Description |
|------|-------------|
| `Dim1` | 1D space (line), size = 1 |
| `Dim2` | 2D space (plane), size = 2 |
| `Dim3` | 3D space (solid), size = 3 |

### Point / Vector

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Point<D, V>` | N-dimensional point | `distance`, `midpoint`, `approxEq`, `plus(Vector)`, `minus(Vector)` |
| `Vector<D, V>` | N-dimensional vector | `norm`, `unit`, `dot`, `cross` (2D/3D extension), `angle`, `projectionOn`, `scale` |

Factory functions:

```kotlin
val p = point2(Flt64(1.0), Flt64(2.0))    // Point<Dim2, Flt64>
val q = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))  // Point<Dim3, Flt64>
val v = vector2(Flt64(1.0), Flt64(0.0))   // Vector<Dim2, Flt64>
val w = vector3(Flt64(1.0), Flt64(0.0), Flt64(0.0))  // Vector<Dim3, Flt64>
```

Extension properties for `Point<Dim2, Flt64>`: `.x`, `.y`, `.pair`
Extension properties for `Point<Dim3, Flt64>`: `.x`, `.y`, `.z`, `.triple`

### Distance

| Type | Description |
|------|-------------|
| `Distance.Euclidean` | Straight-line distance: `sqrt(sum((a-b)^2))` |
| `Distance.Manhattan` | Sum of absolute differences: `sum(\|a-b\|)` |
| `Distance.Minkowski(p)` | Generalization; p=1 Manhattan, p=2 Euclidean |
| `Distance.Chebyshev` | Max absolute difference: `max(\|a-b\|)` |

```kotlin
val d = p1.distance(p2)                          // Euclidean (default)
val d2 = p1.distanceBetween(p2, Distance.Manhattan)
```

## Primitives

### Edge

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Edge<P, D, V>` | Line segment between two points | `length`, `lengthSquared`, `vector`, `direction`, `unitDirection`, `midpoint`, `pointAt(t)`, `containsPoint`, `approxEq` |

2D-specific extensions for `Edge<Point<Dim2, Flt64>, Dim2, Flt64>`:
- `intersects(other)`, `intersectionPoint(other)`, `closestPoint(point)`, `distanceToPoint(point)`

### Triangle

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Triangle<P, D, V>` | Triangle defined by 3 vertices | `e1`, `e2`, `e3`, `edges`, `vertices`, `perimeter`, `area` (Heron's), `centroid`, `isDegenerate`, `illegal` |

2D-specific extensions for `Triangle<Point<Dim2, Flt64>, Dim2, Flt64>`:
- `area2D()` (cross product), `containsPoint`, `circumcircle()`, `circumcenter()`, `incenter()`

3D-specific extensions for `Triangle<Point<Dim3, Flt64>, Dim3, Flt64>`:
- `area3D()` (cross product), `normal()` (unit normal vector, null if degenerate)

### Circle

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Circle<P, Vec, D, Va>` | Circle/sphere defined by center, direction, radius | Constructor from center + radius vector |

2D-specific extensions for `Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>`:
- `.x`, `.y`, `.area`, `.circumference`, `.diameter`
- `containsPoint`, `containsPointStrict`, `intersects`, `containsCircle`, `pointOnBoundary`, `isTangent`, `intersectionPoints`

3D-specific extensions for `Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>`:
- `.volume`, `.surfaceArea`, `containsPoint`

Companion: `Circle.circumcircleOf(triangle)` — compute circumcircle of a 2D triangle

### Rectangle

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Rectangle<P, D, V>` | Quadrilateral with 4 vertices | `length`, `width`, `area`, `leftUpperPoint`, `rightBottomPoint` |

2D extension: `contains(point, withLowerBound, withUpperBound, withBorder)`

### Quadrilateral

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Quadrilateral<P, D, V>` | Quadrilateral with 4 vertices | `edges`, `diagonals`, `perimeter`, `centroid`, `areaByTriangles` |

2D-specific extensions for `Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64>`:
- `area` (Shoelace formula), `isConvex()`, `illegal`

Factory: `quadrilateral2(p1, p2, p3, p4)`

## 3D Shapes

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Shape3<V>` | Interface for 3D shapes | `boundingCuboid: Cuboid3<V>` |
| `Cuboid3<V>` | Rectangular cuboid (width, height, depth) | `volume`, `atOrigin()`, `at(x,y,z)`, `along(axis)` |
| `Cylinder3<V>` | Cylinder (radius, height, axis) | `diameter`, `baseArea(pi)`, `volume(pi)`, `projectionOn(plane)`, `permute(permutation)`, `boundingBoxAtOrigin()`, `toBoundingBox(x,y,z)` |
| `Cuboid3View<V>` | Cuboid with axis permutation view | `cuboid`, `width`, `height`, `depth` |

Type aliases: `AxisAlignedCylinder3<V> = Cylinder3<V>`

## 2D Shape Projections

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Projection2<V>` (sealed) | 2D shape projection interface | Base type for `Circle2`, `Rectangle2` |
| `Circle2<V>` | Circle by radius | `diameter`, `area(pi)`, `boundingBoxAtOrigin()` |
| `Rectangle2<V>` | Rectangle by width/height | `area`, `along(axis)`, `permute(permutation)`, `atOrigin()` |

Type alias: `Shape2<V> = Projection2<V>`

## Bounding Boxes

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Box2<V>` | 2D axis-aligned bounding box | `width`, `height`, `maxX`, `maxY`, `contains`, `overlapped`, `intersect` |
| `Box3<V>` | 3D axis-aligned bounding box | `width`, `height`, `depth`, `maxX`, `maxY`, `maxZ`, `contains`, `overlapped`, `intersect` |

Type aliases: `AxisAlignedBox2<V> = Box2<V>`, `AxisAlignedBox3<V> = Box3<V>`

Factory: `Box2.atOrigin(shape)`, `Box3.atOrigin(cuboid)`

## Placements

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Placement2<V>` | 2D shape placed at (x, y) | `width`, `height`, `maxX`, `maxY`, `contains`, `overlapped`, `intersect` |
| `Placement3<V>` | 3D shape placed at (x, y, z) | `box`, `width`, `height`, `depth`, `maxX`, `maxY`, `maxZ`, `contains`, `overlapped`, `intersect` |

## Axes and Permutations

| Type | Description | Values / Key Operations |
|------|-------------|------------------------|
| `Axis2` | 2D axis enum | `X`, `Y` |
| `Axis3` | 3D axis enum | `X`, `Y`, `Z` |
| `AxisPlane3` | 3D plane enum | `XY`, `XZ`, `YZ`; `contains(axis)` |
| `AxisPermutation2` | 2D axis permutation | `XY`, `YX`; `apply(rectangle)`, `apply(circle)` |
| `AxisPermutation3` | 3D axis permutation | `XYZ`, `ZYX`, `YXZ`, `ZXY`, `XZY`, `YZX`; `apply(cuboid)`, `apply(cylinder)`, `mapAxis(axis)` |

## Plane Frame

| Type | Description | Key Operations |
|------|-------------|----------------|
| `PlaneFrame3` | 3D plane coordinate frame | `XY`, `YX`, `XZ`, `ZX`, `YZ`, `ZY`; `normalAxis`, `distance(point)`, `point2(point)`, `point3(point2, distance)`, `vector(distance)`, `footprint(cuboid)` |
| `PlanePoint2<V>` | 2D point in plane frame | `x`, `y` |
| `PlanePoint3<V>` | 3D point in plane frame | `x`, `y`, `z`, `along(axis)` |
| `PlaneVector3<V>` | 3D vector in plane frame | `x`, `y`, `z` |

## Triangulation

| Type | Description | Key Operations |
|------|-------------|----------------|
| `Delaunay` | Delaunay triangulation algorithm (data object) | `triangulate(points)`, `invoke(points)`, `triangulateRet(points)`, `invokeRet(points)` |
| `DelaunayTriangulation2` | 2D Delaunay result | `triangles`, `points`, `edges` |

Top-level functions:

```kotlin
// 2D Delaunay — returns DelaunayTriangulation2
fun delaunayTriangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2
fun delaunayTriangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2>

// 2D — returns triangle list
fun triangulate(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>
fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>>

// 3D — project onto XY plane
fun triangulate(points: List<Point<Dim3, Flt64>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>

// 3D — from isolines
fun triangulate(isolines: List<Pair<Flt64, List<Point<Dim2, Flt64>>>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>

// Validation
fun isDelaunay(triangles, points): Boolean
fun pointInCircumcircle(point, triangle): Boolean
```

## Usage Examples

### Point and Vector Operations

```kotlin
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

val distance = p1.distance(p2)  // Flt64(5.0)
val v = p2 - p1                 // Vector<Dim2, Flt64>
val normalized = v.unit         // unit vector

val cross2d = vector2(Flt64(1.0), Flt64(0.0)) cross vector2(Flt64(0.0), Flt64(1.0))  // Flt64(1.0)
val cross3d = vector3(Flt64(1.0), Flt64(0.0), Flt64(0.0)) cross vector3(Flt64(0.0), Flt64(1.0), Flt64(0.0))
// cross3d = vector3(0, 0, 1)
```

### Edge Operations

```kotlin
val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(10.0), Flt64(0.0)))

val mid = edge.midpoint()               // Point<Dim2, Flt64>
val point = edge.pointAt(Flt64(0.3))     // Point<Dim2, Flt64>
val contains = edge containsPoint mid    // true
val len = edge.length                    // Flt64(10.0)

// 2D intersection
val other = Edge(point2(Flt64(5.0), Flt64(-5.0)), point2(Flt64(5.0), Flt64(5.0)))
val intersects = edge intersects other           // true
val intersection = edge intersectionPoint other  // Point<Dim2, Flt64>
```

### Triangle Operations

```kotlin
val triangle = Triangle(
    point2(Flt64(0.0), Flt64(0.0)),
    point2(Flt64(4.0), Flt64(0.0)),
    point2(Flt64(2.0), Flt64(3.0))
)

val area = triangle.area             // Heron's formula
val area2d = triangle.area2D()       // cross product method
val perimeter = triangle.perimeter
val centroid = triangle.centroid
val cc = triangle.circumcircle()     // Circle<Point<Dim2, Flt64>, ...>
val center = triangle.circumcenter() // Point<Dim2, Flt64>
val ic = triangle.incenter()         // Point<Dim2, Flt64>
```

### Delaunay Triangulation

```kotlin
val points = listOf(
    point2(Flt64(0.0), Flt64(0.0)),
    point2(Flt64(1.0), Flt64(0.0)),
    point2(Flt64(0.5), Flt64(1.0)),
    point2(Flt64(0.5), Flt64(0.5))
)

val result = Delaunay.triangulate(points)
// result.triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>
// result.points: List<Point<Dim2, Flt64>>
// result.edges: List<Edge<Point<Dim2, Flt64>, Dim2, Flt64>>

val isValid = isDelaunay(result.triangles, result.points)  // true
```

### Distance Metrics

```kotlin
val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

val euclidean = p1.distance(p2)                              // Flt64(5.0)
val manhattan = p1.distanceBetween(p2, Distance.Manhattan)   // Flt64(7.0)
val chebyshev = p1.distanceBetween(p2, Distance.Chebyshev)   // Flt64(4.0)
val minkowski = p1.distanceBetween(p2, Distance.Minkowski(3))
```

## Boundary Semantics

### Degenerate Cases

| Case | Handling |
|------|----------|
| Degenerate triangle (collinear) | `illegal` returns `true`, `area` returns 0 |
| Coincident vertices | `isDegenerate` returns `true` |
| Empty triangulation input | Returns empty list / empty `DelaunayTriangulation2` |
| Duplicate 3D projected points | `triangulate(List<Point<Dim3>>)` throws `IllegalArgumentException` |
| Zero-length edge | `unitDirection` returns `null` |
| Degenerate normal | `Triangle.normal()` returns `null` |

### Epsilon Tolerance

Approximate comparisons use configurable epsilon:

```kotlin
val epsilon = Flt64(1e-9)
p1.approxEq(p2, epsilon)
edge.approxEq(other, epsilon)
circle pointOnBoundary (point, epsilon)
```
