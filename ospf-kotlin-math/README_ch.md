# ospf-kotlin-math / OSPF Kotlin 数学模块

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的综合数学代数、符号与几何系统。提供基础数学类型、代数结构、符号表达式、几何原语和数值运算，专注于类型安全、精度和性能。

## 概述

`ospf-kotlin-math` 旨在为科学计算、优化问题和符号计算提供稳健的数学基础。核心设计原则包括：

- **类型安全**：通过代数结构约束实现强类型
- **精度控制**：支持任意精度算术（IntX、FltX）
- **不可变性**：所有值类型设计为不可变
- **可扩展性**：接口支持自定义实现

## 模块结构

| 包 | 描述 | 关键类型 |
|---|------|---------|
| `algebra.concept` | 代数结构接口 | `Group`、`Ring`、`Field`、`Monoid`、`Semigroup` |
| `algebra.law` | 代数定律验证 | `Associativity`、`Commutativity`、`Distributivity` |
| `algebra.number` | 数值类型实现 | `Int8`-`IntX`、`UInt8`-`UIntX`、`Flt32`、`Flt64`、`FltX`、`Rtn8`-`RtnX` |
| `algebra.value_range` | 类型化值范围 | `ValueRange`、`TypedValueRange`、`Bound`、`Interval` |
| `chaotic_operator` | 混沌系统吸引子 | `Lorenz`、`Chen`、`Rossler`、`LogisticMap`，30+ 实现 |
| `combinatorics` | 组合算法 | `permutations`、`combinations`、`cross` |
| `fractal_operator` | 分形生成 | `MandelbrotSet` |
| `functional` | 集合扩展 | `usize`、`uIndices` |
| `geometry` | 维度无关几何原语 | `Point`、`Vector`、`Edge`、`Triangle`、`Circle`、`Rectangle`、`Quadrilateral`、`Box2`/`Box3`、`Cuboid3`、`Cylinder3`、`Placement2`/`Placement3` |
| `multiarray` | 多维数组 | `MultiArray`、`Einsum`、`TensorExpr` |
| `operator` | 数学运算符 | `Plus`、`Minus`、`Times`、`Div`、`Pow`、`Trigonometry` |
| `ordinary` | 常用数学运算 | `gcd`、`lcm`、`Prime`、`Factorization` |
| `parallel` | 并行计算 | `parallelFold`、`chunked` |
| `symbol` | 符号表达式系统 | `Symbol`、`LinearPolynomial`、`CanonicalPolynomial`、`Inequality` |
| `symbol.expression` | 运行时表达式 AST | `ScalarExpression`、`BooleanExpression`、`PropertyPath` |
| `symbol.parse` | 直接多项式/不等式解析器 | `parseLinear`、`parseQuadratic`、`parseCanonical`、`ParseResult` |
| `symbol.serde` | 多项式/不等式 JSON 序列化与符号身份支持 | `linearPolynomialFromJson`、`SymbolIdentityExpr` |

## 表达式入口

- `symbol.expression.*` 是推荐入口，用于运行时 boolean/scalar expression 和 PropertyPath 求值。
- `symbol.parse` 提供直接的多项式和不等式解析（线性、二次、规范型）。
- `symbol.serde` 提供多项式和不等式的 JSON 序列化/反序列化。

## 架构设计

### 代数层次结构

```
Semigroup（半群，结合律 +）
    └── Monoid（幺半群，单位元）
        └── Group（群，逆元）
            └── AbelianGroup（阿贝尔群，交换律）

MultiplicativeSemigroup（乘法半群，结合律 *）
    └── MultiplicativeMonoid（乘法幺半群，单位元）
        └── MultiplicativeGroup（乘法群，逆元）

Ring（环）= AbelianGroup（+）+ MultiplicativeSemigroup（*）
    └── CommutativeRing（交换环，乘法交换律）
        └── Field（域，乘法逆元）
```

### 数值类型层次结构

```
Integer（有符号整数）
├── Int8、Int16、Int32、Int64（固定精度）
├── IntX（任意精度）
└── NumericInteger（Int8..Int64，带数值运算）

UInteger（无符号整数）
├── UInt8、UInt16、UInt32、UInt64（固定精度）
├── UIntX（任意精度）
└── NumericUInteger（UInt8..UInt64，带数值运算）

Floating（浮点数）
├── Flt32、Flt64（IEEE 754）
└── FltX（基于 BigDecimal 的任意精度）

Rational（有理数）
├── Rtn8..RtnX（有符号有理数）
└── URtn8..URtnX（无符号有理数）
```

### 几何类型层次结构

所有几何类型均为维度无关，通过 `<D : Dimension, V : FloatingNumber<V>>` 参数化：

```
Dimension（维度）
├── Dim1（一维）
├── Dim2（二维）
└── Dim3（三维）

Point<D, V>       -- D 维空间中的位置
Vector<D, V>      -- D 维空间中的方向和大小
Edge<P, D, V>     -- 连接两点的线段
Triangle<P, D, V> -- 三个顶点
Quadrilateral<P, D, V> -- 四个顶点
Rectangle<P, D, V> -- 四个直角顶点（可创建轴对齐矩形）
Circle<P, Vec, D, V>  -- 圆心 + 方向 + 半径（通用圆/球体）

Projection2<V>（密封接口，即 Shape2<V>）
├── Rectangle2<V> -- 宽度 + 高度
└── Circle2<V>    -- 半径

Shape3<V>（接口）
├── Cuboid3<V>    -- 宽度 + 高度 + 深度
└── Cylinder3<V>  -- 半径 + 高度 + 轴

Box2<V>           -- 二维包围盒（位置 + Shape2）
Box3<V>           -- 三维包围盒（位置 + Cuboid3）
Placement2<V>     -- 二维放置（位置 + Projection2）
Placement3<V>     -- 三维放置（位置 + Shape3）

Axis2、Axis3      -- 轴枚举（X, Y / X, Y, Z）
AxisPermutation2/3 -- 轴置换
AxisPlane3        -- 主平面枚举（XY, XZ, YZ）
PlaneFrame3       -- 平面投影坐标框架
```

## 核心功能

### 代数结构

所有数值类型都实现了相应的代数结构：

```kotlin
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*

// Flt64 实现 Field 接口（支持 +、-、*、/）
val a = Flt64(3.14)
val b = Flt64(2.0)

val sum = a + b      // 通过群的加法
val product = a * b  // 通过环的乘法
val quotient = a / b // 通过域的除法

// 类型安全运算
val c: Flt64 = Flt64.one + Flt64.two  // 编译时类型检查
```

### 数值类型

```kotlin
import fuookami.ospf.kotlin.math.algebra.number.*

// 固定精度整数
val i8 = Int8(127)
val i64 = Int64(9223372036854775807L)

// 任意精度整数
val bigInt = IntX("123456789012345678901234567890")

// 浮点数
val f32 = Flt32(3.14159f)
val f64 = Flt64(3.141592653589793)

// 任意精度浮点数
val bigFloat = FltX("3.141592653589793238462643383279")

// 有理数
val rational = RtnX(IntX(1), IntX(3))  // 1/3

// 类型转换
val toFlt64: Flt64 = i64.toFlt64()
val toIntX: IntX = f64.toIntX()
```

### 值范围

带编译时和运行时验证的类型化值范围：

```kotlin
import fuookami.ospf.kotlin.math.algebra.value_range.*

// 创建值范围
val range = ValueRange(
    lower = Bound(Flt64(0.0), Interval.Closed),
    upper = Bound(Flt64(100.0), Interval.Closed)
)

// 类型化值范围，编译时安全
val percentage: ClosedTypedValueRange<Flt64> = TypedValueRange.closed(
    Flt64(0.0),
    Flt64(100.0)
)

// 值截断
val clamped = range.clamp(Flt64(150.0))  // 返回 100.0

// 算术运算保持范围
val doubled = percentage * Flt64(2.0)  // 范围: [0, 200]
```

### 符号表达式

完整的符号表达式系统，支持多项式、不等式和各种运算：

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

val x = symbolOf("x")
val y = symbolOf("y")

// 线性多项式：1 + 2x + 3y
val linear = LinearPolynomial(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)

// 二次多项式：x^2 + 2xy + y^2
val quadratic = QuadraticPolynomial(
    monomials = listOf(
        QuadraticMonomial(Flt64(1.0), x, x),
        QuadraticMonomial(Flt64(2.0), x, y),
        QuadraticMonomial(Flt64(1.0), y, y)
    ),
    constant = Flt64.zero
)

// 规范多项式（通用形式）
val canonical = CanonicalPolynomial(
    monomials = listOf(
        CanonicalMonomial(Flt64(1.0), mapOf(x to 2, y to 1))
    ),
    constant = Flt64.zero
)

// 在指定值处求值
val result = linear.evaluate(mapOf(x to Flt64(2.0), y to Flt64(3.0)))
// 结果：1 + 2*2 + 3*3 = 14

// 微分
val derivative = linear.differentiate(x)  // d/dx(1 + 2x + 3y) = 2

// 编译为优化形式
val compiled = linear.compile()

// 转换为 LaTeX
val latex = linear.toLatex()  // "1 + 2x + 3y"

// 因式分解
val factored = factorize(linear)  // 尝试代数因式分解

// 区间极值计算
val extremum = linear.evaluateIntervalExtremum(
    mapOf(x to closedRange(0.0, 10.0), y to closedRange(-5.0, 5.0))
)
// 返回多项式在给定区间上的最小/最大值边界
```

### 不等式系统

```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.*

// 线性不等式：ax + by + c <= 0
val inequality = LinearInequality(
    polynomial = linearPolynomial,
    constraint = Constraint.LessEqual
)

// 检查可行性
val feasible = inequality.isFeasible()

// 组合不等式
val system = listOf(inequality1, inequality2, inequality3)
val result = solveInequalitySystem(system)
```

### 可满足性检查

检查给定赋值是否满足不等式：

```kotlin
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

val x = symbolOf("x")
val y = symbolOf("y")

// 创建线性不等式：2x + y <= 5
val inequality = Flt64LinearInequality(
    lhs = LinearPolynomial(
        monomials = listOf(
            LinearMonomial(Flt64(2.0), x),
            LinearMonomial(Flt64.one, y)
        ),
        constant = Flt64.zero
    ),
    rhs = LinearPolynomial(constant = Flt64(5.0)),
    comparison = Comparison.LE
)

// 使用映射赋值检查可满足性
val satisfied = inequality.isSatisfied(mapOf(x to Flt64.one, y to Flt64(2.0)))
// true: 2*1 + 2 = 4 <= 5

// 使用有序列表赋值（避免 Map 开销）
val order = listOf(x, y)
val satisfiedOrdered = inequality.isSatisfiedOrdered(order, listOf(Flt64.one, Flt64(2.0)))
// true

// 同样适用于 QuadraticInequality 和 CanonicalInequality
```

### 非抛出式解析函数

使用 `Ret<T>` 进行结构化错误处理的解析函数：

```kotlin
import fuookami.ospf.kotlin.math.symbol.parser.*
import fuookami.ospf.kotlin.utils.functional.*

// 使用结构化错误处理进行解析
val result = parseLinear("2*x + 3*y")
when (result) {
    is Ok -> println("解析成功: ${result.value}")
    is Failed -> println("错误: ${result.error}")
    is Fatal -> println("致命错误: ${result.errors}")
}

// 带错误分类的解析
val linearResult = parseLinearInequality("2*x + 3*y - 5 <= 0")
if (linearResult is Failed) {
    val issue = linearResult.error.context as? ParseIssue
    println("问题类型: ${issue?.type}") // Lexical, Syntax, Conversion, Semantic, Unknown
    println("位置: ${issue?.position}")
}

// 解析特定类型的多项式
val canonicalResult = parseCanonical("x^2 + 2*x*y + y^2")
val quadraticResult = parseQuadratic("x^2 + 2*x*y + y^2")
val inequalityResult = parseLinearInequality("2*x + y <= 5")
```

### 表达式 DSL

```kotlin
import fuookami.ospf.kotlin.math.symbol.expression.dsl.*

// 使用 DSL 构建布尔表达式
val expr = path("a").gt(5) and path("b").isNotNull()
```

### 几何原语

所有几何类型均为维度无关类型。使用工厂函数 `point2`/`point3` 和 `vector2`/`vector3` 创建 Flt64 类型的点和向量：

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 点和向量（维度无关，提供 Flt64 便捷工厂函数）
val p1 = point2(Flt64(0.0), Flt64(0.0))    // Point<Dim2, Flt64>
val p2 = point2(Flt64(3.0), Flt64(4.0))
val p3 = point2(Flt64(6.0), Flt64(0.0))
val v = vector2(Flt64(3.0), Flt64(4.0))     // Vector<Dim2, Flt64>

// 通过扩展属性访问坐标
val x: Flt64 = p1.x
val y: Flt64 = p1.y

// 三维点和向量
val q1 = point3(Flt64(1.0), Flt64(2.0), Flt64(3.0))  // Point<Dim3, Flt64>
val w = vector3(Flt64(0.0), Flt64(0.0), Flt64(1.0))   // Vector<Dim3, Flt64>
val z: Flt64 = q1.z

// 距离计算
val dist: Flt64 = p1 distance p2  // 5.0（默认欧几里得距离）

// 边
val edge = Edge(p1, p2)
val length: Flt64 = edge.length
val mid: Point<Dim2, Flt64> = edge.midpoint()

// 三角形
val triangle = Triangle(p1, p2, p3)
val area: Flt64 = triangle.area        // 海伦公式
val perimeter: Flt64 = triangle.perimeter
val centroid: Point<Dim2, Flt64> = triangle.centroid

// 二维三角形特有操作
val area2d: Flt64 = triangle.area2D()           // 叉积法
val cc: Circle<...> = triangle.circumcircle()
val incenterPt: Point<Dim2, Flt64> = triangle.incenter()
val containsPt: Boolean = triangle containsPoint p1

// 圆形（维度无关：圆心 + 方向 + 半径）
val circle = Circle(center = p1, radiusVec = vector2(Flt64(5.0), Flt64.zero))
val circleArea: Flt64 = circle.area
val circleCirc: Flt64 = circle.circumference
val inside: Boolean = circle containsPoint p2

// 矩形（四个顶点，不要求轴对齐）
val rect = Rectangle(p1, p2, p3, p4)  // 由 4 个顶点定义的通用矩形
val rectArea: Flt64 = rect.area
val rectContains: Boolean = rect.contains(point2(Flt64(1.0), Flt64(1.0)))

// 轴对齐矩形（由左上角和右下角创建）
val aaRect = Rectangle(point2(Flt64(0.0), Flt64(5.0)), point2(Flt64(10.0), Flt64(0.0)))

// 四边形
val quad = Quadrilateral(p1, p2, p3, p4)
val quadArea: Flt64 = quad.area          // 鞋带公式（二维）
val isConvex: Boolean = quad.isConvex()
val quadPerimeter: Flt64 = quad.perimeter

// 三角剖分（Delaunay）
val points = listOf(p1, p2, p3, p4, p5)
val triangles: List<Triangle<Point<Dim2, Flt64>, Dim2, Flt64>> = triangulate(points)
val result: DelaunayTriangulation2 = delaunayTriangulate(points)  // 完整结果（含边）
```

### 三维形状

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 长方体（宽 x 高 x 深）
val cuboid = Cuboid3(width = Flt64(10.0), height = Flt64(5.0), depth = Flt64(3.0))
val volume: Flt64 = cuboid.volume
val box: Box3<Flt64> = cuboid.atOrigin()

// 圆柱体（半径、高度、轴方向）
val cylinder = Cylinder3(radius = Flt64(2.5), height = Flt64(10.0), axis = Axis3.Z)
val cylVolume: Flt64 = cylinder.volume(Flt64.pi)
val cylBase: Flt64 = cylinder.baseArea(Flt64.pi)
val boundingCuboid: Cuboid3<Flt64> = cylinder.boundingCuboid

// 包围盒
val box2 = Box2(x = Flt64.zero, y = Flt64.zero, shape = Rectangle2(Flt64(10.0), Flt64(5.0)))
val box3 = Box3(x = Flt64.zero, y = Flt64.zero, z = Flt64.zero, cuboid = cuboid)
val overlaps: Boolean = box3.overlapped(otherBox)
val intersection: Box3<Flt64>? = box3.intersect(otherBox)
val inside: Boolean = box3.contains(Flt64(1.0), Flt64(1.0), Flt64(1.0))

// 放置（形状 + 位置）
val placement2 = Placement2(x = Flt64(1.0), y = Flt64(2.0), shape = Rectangle2(Flt64(5.0), Flt64(3.0)))
val placement3 = Placement3(x = Flt64(1.0), y = Flt64(2.0), z = Flt64(0.0), shape = cuboid)
val pOverlaps: Boolean = placement3.overlapped(otherPlacement)

// 轴置换（旋转/翻转形状）
val perm = AxisPermutation3.YXZ
val permutedCuboid: Cuboid3<Flt64> = perm.apply(cuboid)
val permutedCylinder: Cylinder3<Flt64> = perm.apply(cylinder)

// 平面框架和投影
val frame = PlaneFrame3.XY
val footprint: Rectangle2<Flt64> = frame.footprint(cuboid)
val dist: Flt64 = frame.distance(PlanePoint3(Flt64(1.0), Flt64(2.0), Flt64(5.0)))

// 圆柱体在平面上的投影
val proj: Projection2<Flt64> = cylinder.projectionOn(AxisPlane3.XY)
```

### 投影形状（Shape2）

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 二维投影形状（密封接口 Projection2<V>，即 Shape2<V>）
val rect2 = Rectangle2(width = Flt64(10.0), height = Flt64(5.0))
val circ2 = Circle2(radius = Flt64(2.5))

val rectArea: Flt64 = rect2.area
val rectAlongX: Flt64 = rect2.along(Axis2.X)
val permuted: Rectangle2<Flt64> = rect2.permute(AxisPermutation2.YX)

val circArea: Flt64 = circ2.area(Flt64.pi)
val diameter: Flt64 = circ2.diameter
```

### 距离度量

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 距离度量策略（密封接口）
val p1 = point2(Flt64(0.0), Flt64(0.0))
val p2 = point2(Flt64(3.0), Flt64(4.0))

// 默认：欧几里得距离（L2）
val dist: Flt64 = p1 distance p2  // 5.0

// 显式选择度量
val euclidean: Flt64 = Distance.Euclidean(p1, p2)    // 5.0
val manhattan: Flt64 = Distance.Manhattan(p1, p2)     // 7.0
val minkowski: Flt64 = Distance.Minkowski(p = 3)(p1, p2)
val chebyshev: Flt64 = Distance.Chebyshev(p1, p2)    // 4.0

// 自定义度量
val d: Flt64 = p1.distanceBetween(p2, Distance.Manhattan)
```

### 爱因斯坦求和

使用爱因斯坦表示法进行强大的张量运算：

```kotlin
import fuookami.ospf.kotlin.math.multiarray.einsum.*

// 创建数组
val a = MultiArray.newWith(Shape2(2, 3), 1.0)
val b = MultiArray.newWith(Shape2(3, 4), 2.0)

// 矩阵乘法
val c1 = matmul(a, b, 0.0)

// 使用字符串表示法
val c2 = einsumDouble(a, b, "ij,jk->ik")

// 张量缩并
val tensor = MultiArray.newWith(Shape3(2, 3, 4), 1.0)
val contracted = einsumDouble(tensor, "ijk->ij")  // 对 k 求和

// 外积
val v1 = MultiArray.newWith(Shape1(3), 1.0)
val v2 = MultiArray.newWith(Shape1(4), 1.0)
val outer = einsumDouble(v1, v2, "i,j->ij")

// 批量矩阵乘法
val batchA = MultiArray.newWith(Shape3(10, 2, 3), 1.0)
val batchB = MultiArray.newWith(Shape3(10, 3, 4), 1.0)
val batchC = einsumDouble(batchA, batchB, "bij,bjk->bik")
```

### 混沌与分形

支持混沌系统和分形生成：

```kotlin
import fuookami.ospf.kotlin.math.chaotic.*

// 洛伦兹吸引子
val lorenz = Lorenz(
    sigma = Flt64(10.0),
    rho = Flt64(28.0),
    beta = Flt64(8.0 / 3.0)
)

// 生成轨迹
val initial = vector3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
val trajectory = lorenz.iterate(initial, steps = 10000, dt = Flt64(0.01))

// 陈氏吸引子
val chen = Chen(a = Flt64(35.0), b = Flt64(3.0), c = Flt64(28.0))

// 逻辑斯谛映射
val logistic = LogisticMap(r = Flt64(3.9))
val series = logistic.iterate(Flt64(0.5), steps = 1000)

// 可用的混沌系统：
// - Lorenz、Chen、Rossler、Lu、Liu
// - Arneodo、Halvorsen、Thomas
// - Aizawa、Anishchenko、Rabinovich
// - 更多（30+ 实现）
```

### 分形生成

```kotlin
import fuookami.ospf.kotlin.math.fractal.*

// 曼德博集
val mandelbrot = MandelbrotSet(
    maxIterations = 1000,
    escapeRadius = Flt64(2.0)
)

// 检查点是否在集合内
val inSet = mandelbrot.contains(Flt64(-0.5), Flt64(0.0))

// 获取迭代次数
val iterations = mandelbrot.iterations(Flt64(-0.75), Flt64(0.1))
```

### 常用数学运算

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*

// GCD - 支持多参数
val gcd1 = gcd(UInt64(12), UInt64(8))              // 4
val gcd2 = gcd(UInt64(12), UInt64(8), UInt64(4))   // 4
val gcd3 = gcd(listOf(UInt64(0), UInt64(0)))       // 0（边界情况）

// LCM - 正确处理零值
val lcm1 = lcm(UInt64(4), UInt64(6))    // 12
val lcm2 = lcm(UInt64(4), UInt64(0))    // 0（零值短路）

// 质数工具
val primes = Prime.getPrimesUpTo(UInt64(100))  // [2, 3, 5, 7, 11, ...]
val isPrime = Prime.isPrime(UInt64(97))        // true

// 质因数分解
val factors = factorize(UInt64(60))  // [(2, 2), (3, 1), (5, 1)]

// 约数
val divisors = getDivisors(UInt64(12))  // [1, 2, 3, 4, 6, 12]
```

### 组合数学

```kotlin
import fuookami.ospf.kotlin.math.combinatorics.*

// 排列
val perms = permutations(listOf(1, 2, 3))
// [[1, 2, 3], [1, 3, 2], [2, 1, 3], [2, 3, 1], [3, 1, 2], [3, 2, 1]]

// 组合
val combs = combinations(listOf(1, 2, 3, 4), k = 2)
// [[1, 2], [1, 3], [1, 4], [2, 3], [2, 4], [3, 4]]

// 笛卡尔积
val product = cross(listOf(1, 2), listOf('a', 'b'))
// [[1, 'a'], [1, 'b'], [2, 'a'], [2, 'b']]
```

### 并行运算

```kotlin
import fuookami.ospf.kotlin.math.parallel.fold

// 带分块的并行折叠，限制协程数量
val sum = parallelFold(
    collection = largeList,
    initial = Flt64.zero,
    chunkSize = 100  // 限制并发协程数
) { acc, item -> acc + item }

// 并行归约
val product = parallelReduce(
    collection = numbers,
    initial = Flt64.one,
    chunkSize = 50
) { acc, item -> acc * item }
```

### 比较运算符

```kotlin
import fuookami.ospf.kotlin.math.ComparisonOperator

// 创建带精度的运算符
val op = ComparisonOperator<Flt64, Flt64>(Flt64(1e-10))

// 使用容差比较
with(op) {
    val a = Flt64(1.0)
    val b = Flt64(1.0 + 1e-15)

    a eq b    // true（在容差范围内）
    a neq b   // false
    a ls b    // false
    a leq b   // true
    a gr b    // false
    a geq b   // true
}
```

### 三值逻辑

```kotlin
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.BalancedTrivalent

// 三值逻辑（真、假、未知）
val t = Trivalent.True
val f = Trivalent.False
val u = Trivalent.Unknown

// 从可空布尔值转换
val fromNullable: Trivalent = Trivalent(null)  // Unknown

// 数值表示
val value: URtn8 = t.value  // 1
val isTrue: Boolean? = u.isTrue  // null

// 平衡三值逻辑（+1、-1、0）
val balanced = BalancedTrivalent.True  // value = +1
```

### 缩放和 SI 前缀

```kotlin
import fuookami.ospf.kotlin.math.Scale

// SI 前缀
val k = Scale.kilo      // 10^3
val M = Scale.mega      // 10^6
val m = Scale.milli     // 10^-3
val u = Scale.micro     // 10^-6

// 自定义缩放
val custom = Scale(2, 10)  // 2^10 = 1024

// 缩放算术
val km = Scale.kilo * Scale.milli  // 单位
val result = Scale.mega / Scale.kilo  // 10^3
```

## 性能优化

| 功能 | 优化方式 | 说明 |
|------|---------|------|
| GCD 多参数 | 迭代器折叠模式 | 避免 % 0 漏洞 |
| LCM | 零值短路 | 防止进入危险的 GCD 路径 |
| 质数缓存 | 埃拉托斯特尼筛法 | O(n log log n) 初始化 |
| 因式分解 | O(sqrt(n)) 上界 | 使用 sqrt(n) 代替 n |
| 并行折叠 | 分块限制 | 控制协程数量 |
| Contract 运算 | 步幅预计算 | 输出驱动迭代 |
| 多项式求值 | 编译优化 | 减少重复求值开销 |
| 区间极值 | 系数符号分析 | 线性扫描单项式 |
| IntX/FltX | 延迟求值 | 仅在需要时执行 BigDecimal 运算 |

## 测试

```bash
# 运行所有测试
mvn -pl ospf-kotlin-math test

# 运行特定测试类
mvn -pl ospf-kotlin-math -Dtest=GCDTest,LCMTest test

# 详细输出运行测试
mvn -pl ospf-kotlin-math test -Dsurefire.useFile=false

# 运行集成测试
mvn -pl ospf-kotlin-math verify
```

测试覆盖包括：
- 代数结构定律（结合律、交换律、分配律）
- 数值类型转换和算术
- 符号表达式解析、求值和微分
- 几何原语运算（点、向量、边、三角形、圆形、矩形、四边形）
- Delaunay 三角剖分
- 三维形状（长方体、圆柱体、包围盒、放置）
- 混沌和分形迭代
- 边界情况和边界条件

## 依赖

| 模块 | 用途 |
|------|------|
| `ospf-kotlin-utils` | 工具函数、函数式类型、Either |
| `ospf-kotlin-multiarray` | 多维数组基础 |

## 相关模块

- [symbol/README_ch.md](src/main/fuookami/ospf/kotlin/math/symbol/README_ch.md) - 符号表达式文档
- [geometry/README_ch.md](src/main/fuookami/ospf/kotlin/math/geometry/README_ch.md) - 几何模块文档
- [algebra/value_range/README_ch.md](src/main/fuookami/ospf/kotlin/math/algebra/value_range/README_ch.md) - 值范围文档

## 许可证

本模块是 OSPF Kotlin 项目的一部分，采用 Apache License 2.0 许可证。
