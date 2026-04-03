# ospf-kotlin-utils/math/geometry

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的 2D/3D 几何基本类型与算法。

## 核心类型

### 点 / 向量

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Point2<T>` | 2D 点 | `distance`, `toVector` |
| `Point3<T>` | 3D 点 | `distance`, `toVector` |
| `Vector2<T>` | 2D 向量 | `length`, `normalize`, `dot`, `cross` |
| `Vector3<T>` | 3D 向量 | `length`, `normalize`, `dot`, `cross` |

### 边 / 线段

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Edge2<T>` | 2D 线段 | `midpoint`, `pointAt(t)`, `length`, `intersects`, `intersectionPoint`, `closestPoint`, `distanceToPoint`, `containsPoint`, `approxEq` |

### 三角形

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Triangle2<T>` | 2D 三角形 | `area`, `perimeter`, `centroid`, `containsPoint`, `circumcircle`, `circumcenter`, `incenter`, `edges`, `vertices` |
| `Triangle3<T>` | 3D 三角形 | `area`, `perimeter`, `centroid`, `normal`, `edges`, `vertices` |

### 圆 / 球

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Circle2<T>` | 2D 圆 | `area`, `circumference`, `diameter`, `containsPoint`, `containsPointStrict`, `intersects`, `containsCircle`, `intersectionPoints` |
| `Sphere3<T>` | 3D 球 | `volume`, `surfaceArea`, `containsPoint` |

### 三角剖分

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `DelaunayTriangulation2<T>` | Delaunay 结果 | `triangles`, `points`, `edges` |
| `triangulate(points)` | Delaunay 三角剖分 | 返回 `DelaunayTriangulation2` |
| `isDelaunay(triangles, points)` | Delaunay 校验 | Boolean 检查 |

## 使用示例

### 点和向量操作

```kotlin
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.algebra.number.Flt64

val p1 = Point2(Flt64(0.0), Flt64(0.0))
val p2 = Point2(Flt64(3.0), Flt64(4.0))

val distance = p1.distance(p2)  // Flt64(5.0)
val v = p2 - p1                 // Vector2(Flt64(3.0), Flt64(4.0))
val normalized = v.normalize()  // Vector2(Flt64(0.6), Flt64(0.8))
```

### 边操作

```kotlin
val edge = Edge2(Point2(Flt64(0.0), Flt64(0.0)), Point2(Flt64(10.0), Flt64(0.0)))

val mid = edge.midpoint()               // Point2(Flt64(5.0), Flt64(0.0))
val point = edge.pointAt(Flt64(0.3))     // Point2(Flt64(3.0), Flt64(0.0))
val contains = edge.containsPoint(mid)   // true
val len = edge.length                   // Flt64(10.0)

// 交集
val other = Edge2(Point2(Flt64(5.0), Flt64(-5.0)), Point2(Flt64(5.0), Flt64(5.0)))
val intersects = edge.intersects(other)  // true
val intersection = edge.intersectionPoint(other)  // Point2(Flt64(5.0), Flt64(0.0))
```

### 三角形操作

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

### Delaunay 三角剖分

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

## 边界语义

### 退化情况

| 情况 | 处理方式 |
|------|---------|
| 退化三角形（共线） | `isDegenerate` 返回 `true`，面积返回 0 |
| 空三角剖分输入 | 返回空 `DelaunayTriangulation2` |
| 重复点 | 在三角剖分中拒绝（投影检查） |
| 零长度边 | `approxEq` 使用 epsilon 容差处理 |

### Epsilon 容差

所有近似比较使用可配置的 epsilon：

```kotlin
val epsilon = Flt64(1e-9)
edge.approxEq(other, epsilon)
circle.containsPoint(point, epsilon)
```

## 性能建议

| 操作 | 复杂度 | 热点路径 |
|------|--------|---------|
| `distance` | O(1) | 平方根计算 |
| `intersects`（边） | O(1) | 叉积计算 |
| `triangulate` | O(n log n) | Delaunay 算法 |
| `containsPoint`（三角形） | O(1) | 重心坐标 |

对于大型三角剖分（n > 10,000），建议预先按 x 坐标排序点集。

## 测试覆盖

- `EdgeTest.kt`: 29 个测试（中点、交集、最近点、距离）
- `TriangleTest.kt`: 25 个测试（面积、重心、外接圆、内心）
- `CircleTest.kt`: 24 个测试（面积、包含、交集、交点）
- `TriangulationTest.kt`: 16 个测试（Delaunay 校验、结果对象）

运行测试：

```powershell
mvn -pl ospf-kotlin-utils -Dtest=EdgeTest,TriangleTest,CircleTest,TriangulationTest test
```

## 相关链接

- [主 README](../README.md)
- [Symbol 模块](../symbol/README.md)
- [Value Range 模块](../algebra/value_range/README.md)