# ospf-kotlin-math / OSPF Kotlin 数学模块

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的综合数学代数与符号系统。提供基础数学类型、代数结构、符号表达式和数值运算，专注于类型安全、精度和性能。

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
| `geometry` | 几何原语 | `Point`、`Vector`、`Circle`、`Triangle`、`Rectangle`、`Quadrilateral` |
| `multiarray` | 多维数组 | `MultiArray`、`Einsum`、`TensorExpr` |
| `operator` | 数学运算符 | `Plus`、`Minus`、`Times`、`Div`、`Pow`、`Trigonometry` |
| `ordinary` | 常用数学运算 | `gcd`、`lcm`、`Prime`、`Factorization` |
| `parallel` | 并行计算 | `parallelFold`、`chunked` |
| `symbol` | 符号表达式系统 | `Symbol`、`LinearPolynomial`、`CanonicalPolynomial`、`Inequality` |

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

// 二次多项式：x² + 2xy + y²
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

### 表达式 DSL

```kotlin
import fuookami.ospf.kotlin.math.symbol.dsl.*

val expr = symbolDsl {
    val x = symbol("x")
    val y = symbol("y")
    
    // 使用 DSL 构建多项式
    val poly = (x + y) * (x - y)  // x² - y²
    
    // 求值
    poly.evaluate(x to Flt64(3.0), y to Flt64(2.0))
}
```

### 几何原语

```kotlin
import fuookami.ospf.kotlin.math.geometry.*

// 点和向量
val p1 = Point2(Flt64(0.0), Flt64(0.0))
val p2 = Point2(Flt64(3.0), Flt64(4.0))
val v = Vector2(Flt64(3.0), Flt64(4.0))

// 距离计算
val distance = p1.distanceTo(p2)  // 5.0

// 几何形状
val circle = Circle(center = p1, radius = Flt64(5.0))
val triangle = Triangle(p1, p2, p3)
val rectangle = Rectangle(p1, Flt64(10.0), Flt64(5.0))

// 面积和周长
val area = triangle.area()
val perimeter = rectangle.perimeter()

// 包含检查
val contains = circle.contains(p2)

// 三角剖分
val polygon = listOf(p1, p2, p3, p4)
val triangles = triangulate(polygon)
```

### 距离度量

```kotlin
import fuookami.ospf.kotlin.math.geometry.Distance

// 欧几里得距离（L2）
val euclidean = Distance.Euclidean<Flt64>()

// 曼哈顿距离（L1）
val manhattan = Distance.Manhattan<Flt64>()

// 闵可夫斯基距离（Lp）
val minkowski = Distance.Minkowski<Flt64>(p = Flt64(3.0))

// 切比雪夫距离（L∞）
val chebyshev = Distance.Chebyshev<Flt64>()

// 计算距离
val dist = euclidean.calculate(p1, p2)
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
import fuookami.ospf.kotlin.math.chaotic_operator.*

// 洛伦兹吸引子
val lorenz = Lorenz(
    sigma = Flt64(10.0),
    rho = Flt64(28.0),
    beta = Flt64(8.0 / 3.0)
)

// 生成轨迹
val initial = Vector3(Flt64(1.0), Flt64(1.0), Flt64(1.0))
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
import fuookami.ospf.kotlin.math.fractal_operator.*

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
val k = Scale.kilo      // 10³
val M = Scale.mega      // 10⁶
val m = Scale.milli     // 10⁻³
val μ = Scale.micro     // 10⁻⁶

// 自定义缩放
val custom = Scale(2, 10)  // 2¹⁰ = 1024

// 缩放算术
val km = Scale.kilo * Scale.milli  // 单位
val result = Scale.mega / Scale.kilo  // 10³
```

## 性能优化

| 功能 | 优化方式 | 说明 |
|------|---------|------|
| GCD 多参数 | 迭代器折叠模式 | 避免 % 0 漏洞 |
| LCM | 零值短路 | 防止进入危险的 GCD 路径 |
| 质数缓存 | 埃拉托斯特尼筛法 | O(n log log n) 初始化 |
| 因式分解 | O(√n) 上界 | 使用 sqrt(n) 代替 n |
| 并行折叠 | 分块限制 | 控制协程数量 |
| Contract 运算 | 步幅预计算 | 输出驱动迭代 |
| 多项式求值 | 编译优化 | 减少重复求值开销 |
| IntX/FltX | 延迟求值 | 仅在需要时执行 BigDecimal 运算 |

## 测试

```powershell
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
- 几何原语运算
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