# ospf-kotlin-math / OSPF Kotlin 数学模块

[English Documentation (README.md)](./README.md)

OSPF Kotlin 的数学代数与符号系统。提供基础数学类型、代数结构、符号表达式和数值运算。

## 模块结构

| 包 | 描述 |
|---|------|
| `algebra.concept` | 代数结构接口（群、环、域、幺半群） |
| `algebra.law` | 代数定律验证，用于结构测试 |
| `algebra.number` | 数值类型实现（Int64、Flt64、UInt64） |
| `algebra.value_range` | 类型化值范围，带边界验证 |
| `chaotic_operator` | 混沌系统吸引子和映射（30+ 实现） |
| `combinatorics` | 组合算法和运算 |
| `fractal_operator` | 分形生成（曼德博集） |
| `geometry` | 几何原语（点、圆、三角形等） |
| `multiarray` | 多维数组，支持爱因斯坦求和 |
| `operator` | 数学运算符和函子 |
| `ordinary` | 常用数学运算（GCD、LCM、质数、因式分解） |
| `parallel` | 并行计算工具（分块折叠） |
| `symbol` | 符号表达式系统（多项式、不等式） |

## 核心功能

### 代数结构

```kotlin
import fuookami.ospf.kotlin.math.algebra.concept.*

// 域支持加减乘除
interface Field<Self> : CommutativeRing<Self>, MultiplicativeGroup<Self>

// 数值类型实现代数结构
val a = Flt64(3.14)
val b = Flt64(2.0)
val c = a + b  // 通过群的加法
val d = a * b  // 通过环的乘法
val e = a / b  // 通过域的除法
```

### 符号表达式

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

val x = symbolOf("x")
val y = symbolOf("y")

// 线性多项式：1 + 2x + 3y
val linear = LinearPolynomial<Flt64>(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)

// 在 x=2, y=3 处求值
val result = linear.evaluate(mapOf(x to Flt64(2.0), y to Flt64(3.0)))
// 结果：1 + 2*2 + 3*3 = 14
```

### 爱因斯坦求和

```kotlin
import fuookami.ospf.kotlin.math.multiarray.einsum.*

// 使用爱因斯坦表示法进行矩阵乘法
val a = MultiArray.newWith(Shape2(2, 3), 1.0)
val b = MultiArray.newWith(Shape2(3, 4), 2.0)

// 方法 1：便捷函数
val c1 = matmul(a, b, 0.0)

// 方法 2：字符串表示法
val c2 = einsumDouble(a, b, "ij,jk->ik")
```

### GCD/LCM 运算

```kotlin
import fuookami.ospf.kotlin.math.ordinary.*

// GCD 支持多参数
val gcd1 = gcd(UInt64(12), UInt64(8))         // 4
val gcd2 = gcd(UInt64(12), UInt64(8), UInt64(4)) // 4
val gcd3 = gcd(listOf(UInt64(0), UInt64(0)))  // 0（边界情况）

// LCM 零值处理
val lcm1 = lcm(UInt64(4), UInt64(6))    // 12
val lcm2 = lcm(UInt64(4), UInt64(0))    // 0（零值短路）
```

### 质数因式分解

```kotlin
import fuookami.ospf.kotlin.math.ordinary.Prime
import fuookami.ospf.kotlin.math.ordinary.Factorization

// 获取指定范围内的质数（缓存以提升性能）
val primes = Prime.getPrimesUpTo(UInt64(100))

// 因式分解
val factors = factorize(UInt64(60))  // [(2, 2), (3, 1), (5, 1)]
```

### 并行运算

```kotlin
import fuookami.ospf.kotlin.math.parallel.fold

// 对大型集合进行分块并行折叠
val sum = parallelFold(
    collection = largeList,
    initial = Flt64.zero,
    chunkSize = 100  // 限制协程数量
) { acc, item -> acc + item }
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

## 测试

```powershell
# 运行所有测试
mvn -pl ospf-kotlin-math test

# 运行特定测试
mvn -pl ospf-kotlin-math -Dtest=GCDTest,LCMTest test
```

## 依赖

- `ospf-kotlin-utils`: 工具函数和类型
- `ospf-kotlin-multiarray`: 多维数组基础

## 相关模块

- [symbol/README_ch.md](src/main/fuookami/ospf/kotlin/math/symbol/README_ch.md) - 符号表达式文档
- [geometry/README_ch.md](src/main/fuookami/ospf/kotlin/math/geometry/README_ch.md) - 几何模块文档
- [algebra/value_range/README_ch.md](src/main/fuookami/ospf/kotlin/math/algebra/value_range/README_ch.md) - 值范围文档