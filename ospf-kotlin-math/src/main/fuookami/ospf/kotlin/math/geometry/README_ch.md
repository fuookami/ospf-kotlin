# ospf-kotlin-math/geometry

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的维度泛型 2D/3D 几何基本类型与算法。

## 核心类型

### 维度

| 类型 | 描述 |
|------|------|
| `Dim1` | 一维空间（直线），size = 1 |
| `Dim2` | 二维空间（平面），size = 2 |
| `Dim3` | 三维空间（立体），size = 3 |

### 点 / 向量

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Point<D, V>` | N 维点 | `distance`, `midpoint`, `approxEq`, `plus(Vector)`, `minus(Vector)` |
| `Vector<D, V>` | N 维向量 | `norm`, `unit`, `dot`, `cross`（2D/3D 扩展）, `angle`, `projectionOn`, `scale` |

工厂函数：

```kotlin
val p = point2(Flt64(1.0), Flt64(2.0))    // Point<Dim2, Flt64>
val q = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))  // Point<Dim3, Flt64>
val v = vector2(Flt64(1.0), Flt64(0.0))   // Vector<Dim2, Flt64>
val w = vector3(Flt64(1.0), Flt64(0.0), Flt64(0.0))  // Vector<Dim3, Flt64>
```

`Point<Dim2, Flt64>` 扩展属性：`.x`, `.y`, `.pair`
`Point<Dim3, Flt64>` 扩展属性：`.x`, `.y`, `.z`, `.triple`

### 距离度量

| 类型 | 描述 |
|------|------|
| `Distance.Euclidean` | 欧几里得距离：`sqrt(sum((a-b)^2))` |
| `Distance.Manhattan` | 曼哈顿距离：`sum(\|a-b\|)` |
| `Distance.Minkowski(p)` | 闵可夫斯基距离；p=1 为曼哈顿，p=2 为欧几里得 |
| `Distance.Chebyshev` | 切比雪夫距离：`max(\|a-b\|)` |

```kotlin
val d = p1.distance(p2)                          // 欧几里得（默认）
val d2 = p1.distanceBetween(p2, Distance.Manhattan)
```

## 基本图元

### 边

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Edge<P, D, V>` | 连接两点的线段 | `length`, `lengthSquared`, `vector`, `direction`, `unitDirection`, `midpoint`, `pointAt(t)`, `containsPoint`, `approxEq` |

2D 扩展（`Edge<Point<Dim2, Flt64>, Dim2, Flt64>`）：
- `intersects(other)`, `intersectionPoint(other)`, `closestPoint(point)`, `distanceToPoint(point)`

### 三角形

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Triangle<P, D, V>` | 由三个顶点定义的三角形 | `e1`, `e2`, `e3`, `edges`, `vertices`, `perimeter`, `area`（海伦公式）, `centroid`, `isDegenerate`, `illegal` |

2D 扩展（`Triangle<Point<Dim2, Flt64>, Dim2, Flt64>`）：
- `area2D()`（叉积法）, `containsPoint`, `circumcircle()`, `circumcenter()`, `incenter()`

3D 扩展（`Triangle<Point<Dim3, Flt64>, Dim3, Flt64>`）：
- `area3D()`（叉积法）, `normal()`（单位法向量，退化时返回 null）

### 圆

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Circle<P, Vec, D, Va>` | 由圆心、方向和半径定义的圆/球 | 通过圆心 + 半径向量构造 |

2D 扩展（`Circle<Point<Dim2, Flt64>, Vector<Dim2, Flt64>, Dim2, Flt64>`）：
- `.x`, `.y`, `.area`, `.circumference`, `.diameter`
- `containsPoint`, `containsPointStrict`, `intersects`, `containsCircle`, `pointOnBoundary`, `isTangent`, `intersectionPoints`

3D 扩展（`Circle<Point<Dim3, Flt64>, Vector<Dim3, Flt64>, Dim3, Flt64>`）：
- `.volume`, `.surfaceArea`, `containsPoint`

伴生对象：`Circle.circumcircleOf(triangle)` — 计算二维三角形的外接圆

### 矩形

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Rectangle<P, D, V>` | 四顶点矩形 | `length`, `width`, `area`, `leftUpperPoint`, `rightBottomPoint` |

2D 扩展：`contains(point, withLowerBound, withUpperBound, withBorder)`

### 四边形

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Quadrilateral<P, D, V>` | 四顶点四边形 | `edges`, `diagonals`, `perimeter`, `centroid`, `areaByTriangles` |

2D 扩展（`Quadrilateral<Point<Dim2, Flt64>, Dim2, Flt64>`）：
- `area`（Shoelace 公式）, `isConvex()`, `illegal`

工厂函数：`quadrilateral2(p1, p2, p3, p4)`

## 三维形状

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Shape3<V>` | 三维形状接口 | `boundingCuboid: Cuboid3<V>` |
| `Cuboid3<V>` | 长方体（宽、高、深） | `volume`, `atOrigin()`, `at(x,y,z)`, `along(axis)` |
| `Cylinder3<V>` | 圆柱体（半径、高度、轴） | `diameter`, `baseArea(pi)`, `volume(pi)`, `projectionOn(plane)`, `permute(permutation)`, `boundingBoxAtOrigin()`, `toBoundingBox(x,y,z)` |
| `Cuboid3View<V>` | 带轴置换视图的长方体 | `cuboid`, `width`, `height`, `depth` |

类型别名：`AxisAlignedCylinder3<V> = Cylinder3<V>`

## 二维形状投影

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Projection2<V>`（密封接口） | 二维形状投影接口 | `Circle2`、`Rectangle2` 的基类型 |
| `Circle2<V>` | 按半径定义的圆 | `diameter`, `area(pi)`, `boundingBoxAtOrigin()` |
| `Rectangle2<V>` | 按宽高定义的矩形 | `area`, `along(axis)`, `permute(permutation)`, `atOrigin()` |

类型别名：`Shape2<V> = Projection2<V>`

## 包围盒

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Box2<V>` | 二维轴对齐包围盒 | `width`, `height`, `maxX`, `maxY`, `contains`, `overlapped`, `intersect` |
| `Box3<V>` | 三维轴对齐包围盒 | `width`, `height`, `depth`, `maxX`, `maxY`, `maxZ`, `contains`, `overlapped`, `intersect` |

类型别名：`AxisAlignedBox2<V> = Box2<V>`, `AxisAlignedBox3<V> = Box3<V>`

工厂函数：`Box2.atOrigin(shape)`, `Box3.atOrigin(cuboid)`

## 放置

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Placement2<V>` | 二维形状放置在 (x, y) 处 | `width`, `height`, `maxX`, `maxY`, `contains`, `overlapped`, `intersect` |
| `Placement3<V>` | 三维形状放置在 (x, y, z) 处 | `box`, `width`, `height`, `depth`, `maxX`, `maxY`, `maxZ`, `contains`, `overlapped`, `intersect` |

## 轴与置换

| 类型 | 描述 | 值 / 关键操作 |
|------|------|-------------|
| `Axis2` | 二维轴枚举 | `X`, `Y` |
| `Axis3` | 三维轴枚举 | `X`, `Y`, `Z` |
| `AxisPlane3` | 三维平面枚举 | `XY`, `XZ`, `YZ`; `contains(axis)` |
| `AxisPermutation2` | 二维轴置换 | `XY`, `YX`; `apply(rectangle)`, `apply(circle)` |
| `AxisPermutation3` | 三维轴置换 | `XYZ`, `ZYX`, `YXZ`, `ZXY`, `XZY`, `YZX`; `apply(cuboid)`, `apply(cylinder)`, `mapAxis(axis)` |

## 平面坐标系

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `PlaneFrame3` | 三维平面坐标系 | `XY`, `YX`, `XZ`, `ZX`, `YZ`, `ZY`; `normalAxis`, `distance(point)`, `point2(point)`, `point3(point2, distance)`, `vector(distance)`, `footprint(cuboid)` |
| `PlanePoint2<V>` | 平面坐标系中的二维点 | `x`, `y` |
| `PlanePoint3<V>` | 平面坐标系中的三维点 | `x`, `y`, `z`, `along(axis)` |
| `PlaneVector3<V>` | 平面坐标系中的三维向量 | `x`, `y`, `z` |

## 三角剖分

| 类型 | 描述 | 关键操作 |
|------|------|---------|
| `Delaunay` | Delaunay 三角剖分算法（data object） | `triangulate(points)`, `invoke(points)`, `triangulateRet(points)`, `invokeRet(points)` |
| `DelaunayTriangulation2` | 二维 Delaunay 剖分结果 | `triangles`, `points`, `edges` |

顶层函数：

```kotlin
// 2D Delaunay — 返回 DelaunayTriangulation2
fun delaunayTriangulate(points: List<Point<Dim2, Flt64>>): DelaunayTriangulation2
fun delaunayTriangulateRet(points: List<Point<Dim2, Flt64>>): Ret<DelaunayTriangulation2>

// 2D — 返回三角形列表
fun triangulate(points: List<Point<Dim2, Flt64>>): List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>
fun triangulateRet(points: List<Point<Dim2, Flt64>>): Ret<List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>>>

// 3D — 投影到 XY 平面
fun triangulate(points: List<Point<Dim3, Flt64>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>

// 3D — 从等值线
fun triangulate(isolines: List<Pair<Flt64, List<Point<Dim2, Flt64>>>>): List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>

// 校验
fun isDelaunay(triangles, points): Boolean
fun pointInCircumcircle(point, triangle): Boolean
```

## 使用示例

### 点和向量操作

```kotlin
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

val distance = p1.distance(p2)  // Flt64(5.0)
val v = p2 - p1                 // Vector<Dim2, Flt64>
val normalized = v.unit         // 单位向量

val cross2d = vector2(Flt64(1.0), Flt64(0.0)) cross vector2(Flt64(0.0), Flt64(1.0))  // Flt64(1.0)
val cross3d = vector3(Flt64(1.0), Flt64(0.0), Flt64(0.0)) cross vector3(Flt64(0.0), Flt64(1.0), Flt64(0.0))
// cross3d = vector3(0, 0, 1)
```

### 边操作

```kotlin
val edge = Edge(point2(Flt64(0.0), Flt64(0.0)), point2(Flt64(10.0), Flt64(0.0)))

val mid = edge.midpoint()               // Point<Dim2, Flt64>
val point = edge.pointAt(Flt64(0.3))     // Point<Dim2, Flt64>
val contains = edge containsPoint mid    // true
val len = edge.length                    // Flt64(10.0)

// 二维交集
val other = Edge(point2(Flt64(5.0), Flt64(-5.0)), point2(Flt64(5.0), Flt64(5.0)))
val intersects = edge intersects other           // true
val intersection = edge intersectionPoint other  // Point<Dim2, Flt64>
```

### 三角形操作

```kotlin
val triangle = Triangle(
    point2(Flt64(0.0), Flt64(0.0)),
    point2(Flt64(4.0), Flt64(0.0)),
    point2(Flt64(2.0), Flt64(3.0))
)

val area = triangle.area             // 海伦公式
val area2d = triangle.area2D()       // 叉积法
val perimeter = triangle.perimeter
val centroid = triangle.centroid
val cc = triangle.circumcircle()     // Circle<Point<Dim2, Flt64>, ...>
val center = triangle.circumcenter() // Point<Dim2, Flt64>
val ic = triangle.incenter()         // Point<Dim2, Flt64>
```

### Delaunay 三角剖分

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

### 距离度量

```kotlin
val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

val euclidean = p1.distance(p2)                              // Flt64(5.0)
val manhattan = p1.distanceBetween(p2, Distance.Manhattan)   // Flt64(7.0)
val chebyshev = p1.distanceBetween(p2, Distance.Chebyshev)   // Flt64(4.0)
val minkowski = p1.distanceBetween(p2, Distance.Minkowski(3))
```

## 边界语义

### 退化情况

| 情况 | 处理方式 |
|------|---------|
| 退化三角形（共线） | `illegal` 返回 `true`，`area` 返回 0 |
| 重合顶点 | `isDegenerate` 返回 `true` |
| 空三角剖分输入 | 返回空列表 / 空 `DelaunayTriangulation2` |
| 重复的 3D 投影坐标 | `triangulate(List<Point<Dim3>>)` 抛出 `IllegalArgumentException` |
| 零长度边 | `unitDirection` 返回 `null` |
| 退化法向量 | `Triangle.normal()` 返回 `null` |

### Epsilon 容差

近似比较使用可配置的 epsilon：

```kotlin
val epsilon = Flt64(1e-9)
p1.approxEq(p2, epsilon)
edge.approxEq(other, epsilon)
circle pointOnBoundary (point, epsilon)
```
