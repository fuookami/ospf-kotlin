# ospf-kotlin-utils/math 对标 ospf-rust-math 进度纪要（对照版）

最后更新：2026-04-03（M11-M14 全部完成）
对照基线：`E:\workspace\ospf-rust\ospf-rust-math`（含仓库根 `daily.md`）

---

## 已完成事项（与 Rust 已对齐或等效）

### 1) 模块骨架对齐
- Rust 的 `algebra/combinatorics/geometry/ordinary/symbol` 在 Kotlin 侧均有对应实现。
- Rust `operator` 能力在 Kotlin 侧以 `utils/operator` 独立模块提供（`Abs/Exp/Reciprocal/Tolerance` 等）。

### 2) algebra + value_range 对齐
- `TotallyOrdered/VectorSpace/NormedSpace/InnerProductSpace`、`Bounded/Infinite/Fixed/Epsilon/Scalar` 等概念已具备。
- `ValueRange + TypedValueRange` 支持 compile-time（open/closed）与 runtime interval 双轨模型。
- `plus/minus/times/div` typed 路径与跨 kind typed 路径可用；Infinity、半开半闭、空区间等边界已有回归。

### 3) ordinary + combinatorics 对齐
- 数论链路 `factorize/defactorize/divisors/divisorCount/eulerTotient/gcd/lcm/isPrime/getPrimes` 已具备。
- 组合链路 `combinations/permutations/cross` 已具备。

### 4) symbol 主链路对齐
- `SymbolId/OwnedSymbol`、`monomial/polynomial/inequality`、`convert/evaluate/differentiate/compile/latex/matrix form` 已具备。
- parser 已具备，并新增 `NumberParser<T>`（至少 `Int64/Flt64` 路径）。
- `SymbolExpr` JSON 读写链路可用（基于 `Expr`）。

### 5) 几何核心链路可用
- `Point/Vector/Edge/Triangle/Quadrilateral/Circle/Triangulation` 与 Delaunay 主流程可运行。
- 2D/3D 三角剖分、isolines、重复投影点拒绝等关键边界已补强。

### 6) 验证与基准
- 关键回归用例已存在（`ValueRange/MatrixForm/Triangulation/NumberParser`）。
- JMH 基准已扩展到 `symbol/geometry/typed value range`（覆盖面高于 Rust 当前基准入口）。

### 7) 几何 API 深度补齐（M1-M2 完成）

#### G-EDGE: Edge2 能力补齐 ✓
- `midpoint()` / `pointAt(t)` / `lengthSquared` / `direction` / `unitDirection`
- `containsPoint(point, epsilon)` 点包含判断
- 2D 特有：`intersects` / `intersectionPoint` / `closestPoint` / `distanceToPoint`
- 容差比较：`approxEq` / `approxEqUndirected`
- 测试：`EdgeTest.kt` (29 tests)

#### G-TRI: Triangle 能力补齐 ✓
- `perimeter` / `centroid` / `isDegenerate` / `edges` / `vertices`
- 2D 特有：`area2D()` / `containsPoint` / `circumcircle()` / `circumcenter()` / `incenter()`
- 3D 特有：`area3D()` / `normal()`
- 测试：`TriangleTest.kt` (25 tests)

#### G-CIRCLE: Circle2/Sphere3 能力补齐 ✓
- Circle2: `area` / `circumference` / `diameter`
- Circle2: `containsPoint` / `containsPointStrict` / `intersects` / `containsCircle` / `intersectionPoints`
- Sphere3: `volume` / `surfaceArea` / `containsPoint`
- 测试：`CircleTest.kt` (24 tests)

#### G-DELAUNAY: Delaunay 结果对象化 ✓
- `DelaunayTriangulation2` 数据类：`triangles` / `points` / `edges`
- `delaunayTriangulate(points)` 新 API 返回结构化结果
- `isDelaunay(triangles, points)` 校验函数
- 旧 API `triangulate(points)` 保持兼容
- 测试：`TriangulationTest.kt` (16 tests)

### 8) Symbol 序列化扩展（M3 完成）

#### S-SERDE: Polynomial/Inequality 序列化 ✓
- `LinearPolynomial.toJsonString()` / `linearPolynomialFromJson()`
- `QuadraticPolynomial.toJsonString()` / `quadraticPolynomialFromJson()`
- `CanonicalPolynomial.toJsonString()` / `canonicalPolynomialFromJson()`
- `LinearInequality.toJsonString()` / `linearInequalityFromJson()`
- `QuadraticInequality.toJsonString()` / `quadraticInequalityFromJson()`
- `CanonicalInequality.toJsonString()` / `canonicalInequalityFromJson()`
- 边界场景：constant-only、non-polynomial 返回 null、function call 报错
- 测试：`SerializationTest.kt` (17 tests)

### 9) Symbol DSL 快捷入口（M3 完成）

#### S-DSL: DSL 到 typed 模型快捷链路 ✓
- `linearPolynomial(symbolOf) { ... }` 快捷入口
- `quadraticPolynomial(symbolOf, symbolComparator?) { ... }` 快捷入口
- `canonicalPolynomial(symbolOf) { ... }` 快捷入口
- `linearInequality(symbolOf) { ... }` 快捷入口
- `quadraticInequality(symbolOf, symbolComparator?) { ... }` 快捷入口
- `canonicalInequality(symbolOf) { ... }` 快捷入口
- 所有比较操作符：`lt` / `le` / `eq` / `ne` / `ge` / `gt`
- 错误场景：符号缺失、非法指数、函数调用转多项式
- 测试：`DslTest.kt` (17 tests)

### 10) 性能工程化（M4 完成）

#### S-PERF: 性能工程化 ✓
- 热点路径锁定：`combineTerms` 为主要热点（30 ops/ms vs 25K ops/ms for plus）
- 基准补充：`polynomialPlus/Minus/TimesScalar/DivScalar`、`combineTermsStress`
- 基线报告：`BENCHMARK_REPORT_2026-04-02.md`
- 阈值告警：Warning 5%, Critical 10%

### 11) 文档补齐（M4 完成）

#### DOC-MATH: 子模块 README ✓
- `geometry/README.md` + `README_ch.md`
- `algebra/value_range/README.md` + `README_ch.md`
- `symbol/README.md` + `README_ch.md`

### 12) Generic 层合并（M5 完成）

#### 架构重构：消除 Typed ↔ Generic 双层结构 ✓
- 约束修改：`NumberField<T>` → `Ring<T>`，保持 `Int32`
- 新增 Ring-based 操作文件（6个）：
  - `LinearQuadraticOps.kt`：Linear/Quadratic 操作
  - `CanonicalOps.kt`：Canonical 操作
  - `CompileOps.kt`：编译求值
  - `DifferentiateOps.kt`：求导
  - `LatexOps.kt`：LaTeX 输出
  - `ConvertOps.kt`：类型转换
- 删除 Generic 层：18 个文件（11 源文件 + 7 测试文件）
- 更新调用点：8 个 operation 文件 + 1 个 serde 文件
- 收益：消除转换开销、减少 2000+ 行代码、简化维护

### 13) 性能深度优化（M6 完成）

#### S-PERF-NEXT: combineTerms 热点优化 ✓
- 问题：`combineCanonicalMonomials` 使用 `Map<Symbol, Int32>` 作为 HashMap key
  - 每次需要排序 powers entries（O(k log k)）
  - Map hashCode/equals 开销大
  - 中间对象创建压力
- 方案：`PowerVectorKey` 混合密集/稀疏模式
  - 密集模式：单 IntArray（小符号集或高稀疏度）
  - 稀疏模式：双 IntArray（indices + powers）
  - 自动模式选择（阈值 0.5）
- 新增文件：
  - `PowerVectorKey.kt`：高性能 HashMap key 实现
- 性能提升：
  - `combineTermsStress`: 61 → 94 ops/ms (**+55%**, 同环境 JDK 21)

### 14) 功能对标验证（M7 完成）

#### 功能对比分析 ✓
对比 Rust `ospf-rust-math` 与 Kotlin `ospf-kotlin-utils/math` 功能覆盖：

**Rust 有但 Kotlin 已有等效实现：**
| Rust 功能 | Kotlin 对应 | 状态 |
|-----------|-------------|------|
| `big_decimal_pow.rs` | `FltXPowerStrategy.kt` | ✓ 已对齐 |
| `operator/contains.rs` | `ValueRange.contains()` | ✓ 已对齐 |
| `symbol/macros` | `symbol/dsl/SymbolDsl.kt` | ✓ 已对齐 |

**Rust 有但 Kotlin 可选：**
| Rust 功能 | 说明 | 建议 |
|-----------|------|------|
| `AddRef/SubRef/NegRef` | 引用运算 Traits | Kotlin 不需要（不可变模型） |

**Kotlin 特有功能（Rust 无）：**
| Kotlin 功能 | 说明 |
|-------------|------|
| `chaotic_operator/` | 40+ 混沌系统（Lorenz/Chen/Chua 等） |
| `fractal_operator/` | 分形算子（MandelbrotSet） |
| `operator/Trigonometry.kt` | 三角函数扩展 |
| `symbol/parser/NumberParser.kt` | 类型化数值解析器 |

**Geometry 模块差异：**
- Rust 有 `point_on_boundary`/`is_tangent` - Kotlin 未实现
- Kotlin 有 `circumcircleOf(triangle)` - Rust 未实现
- 核心功能对齐，边界方法可按需补充

**结论**：**核心功能已完全对齐**，Kotlin 在混沌/分形领域有额外扩展。

### 15) Geometry 边界方法补齐（M8 完成）

#### G-FUNC: Circle2 边界方法 ✓
- `pointOnBoundary(point, epsilon)` - 判断点是否在圆上
- `isTangent(other, epsilon)` - 判断两圆是否相切（外切/内切）
- 测试：`CircleTest.kt` 新增 12 tests（总计 36 tests）

### 16) 多项式因式分解（M9 完成）

#### S-FUNC: 一元二次多项式因式分解 ✓
- `extractUnivariateCoefficients()` - 从多项式提取系数 (a, b, c)
- `solve()` - 求根（使用判别式 Δ = b² - 4ac）
- `factorize()` - 因式分解为 a(x - r₁)(x - r₂) 形式
- `QuadraticFactorization.expand()` - 因式展开回多项式
- 支持边界情况：线性退化、重根、无实根
- 新增文件：`symbol/operation/Factorization.kt`
- 测试：`FactorizationTest.kt` (17 tests)

### 17) 符号积分（M10 完成）

#### S-INTEGRATE: 一元多项式积分 ✓
- `integrateLinear()` - 线性多项式积分（Linear → Quadratic）
- `integrateQuadratic()` - 二次多项式积分（Quadratic → Canonical）
- `integrateCanonical()` - 规范多项式积分（Canonical → Canonical，支持任意次数）
- 支持多元多项式（指定积分变量，其他视为常数）
- 积分-微分回环测试验证正确性
- 新增文件：`symbol/operation/IntegrateOps.kt`
- 测试：`IntegrationTest.kt` (18 tests)

### 18) Mutable 多项式 combineTerms（M11 完成）

#### M11: Mutable 多项式 FastSum 支持 ✓
- 新增文件：`symbol/operation/MutableCombineOps.kt`
- `MutableLinearPolynomial.combineTerms(zero, isZero)` - 原地合并同类项
- `MutableQuadraticPolynomial.combineTerms(zero, isZero, comparator)` - 原地合并同类项
- `MutableCanonicalPolynomial.combineTerms(zero, isZero, comparator)` - 原地合并同类项
- 可选便捷方法：`addAssignAndCombine()` / `minusAssignAndCombine()`
- 修复 `zero()` / `one()` API：使用 `resolveArithmeticConstants<T>()` 泛型获取
- 测试：`MutableCombineTest.kt` (9 tests)

### 19) MultiArray 快速求和（M14 完成）

#### M14: MultiArray FastSum 支持 ✓
- 新增文件：`multi_array/FastSum.kt`
- `sumAll(zero)` - 全局求和
- `sumAxis(axis, zero)` - 沿单轴求和
- `sumAxes(axes, zero)` - 沿多轴求和
- `cumsumAxis(axis, zero)` - 累积求和（前缀和）
- 支持 `Ring<T>` 约束（可用于多项式求和）
- 异常：`AxisOutOfBoundsException`
- 测试：`FastSumTest.kt` (14 tests)

---

## 待办事项

### P3 已完成扩展

#### M12: Mutable 多项式 addAssignAndCombine 方法 ✓
**状态**：已在 M11 中实现，无需单独追踪。

#### M13: Symbol + MultiArray 文档示例 ✓
**状态**：已在 `symbol/README.md` 和 `README_ch.md` 中添加 MultiArray 集成章节。

### P4 可选扩展（未来）

#### S-FUNC-NEXT: 高级功能（可选）
- ~~多项式因式分解~~ ✓ M9 已完成
- ~~符号积分~~ ✓ M10 已完成
- ~~更多数值类型支持~~ 有理数已有完整实现（Rtn8/16/32/64/X），复数使用外部库 kotlinmath
- ~~稀疏多项式优化~~ ✓ M6 已完成（PowerVectorKey 密集/稀疏双模式）

#### REF-OPS: 引用运算 Traits（不需要）
- Kotlin 不存在所有权/借用概念，无需实现 `AddRef/SubRef` 等引用运算
- 数据类默认不可变，直接值运算即可

#### SYMBOL-DYN-ID: 高维符号标识符（不需要）
- Rust 需要 `SymbolDynId` 支持高维符号分量（向量/矩阵）
- Kotlin 无此语法限制，可直接使用 List/Map 表示符号集合

---

## 里程碑总结

| 里程碑 | 状态 | 主要内容 | 测试数 |
|--------|------|----------|--------|
| M1 | ✓ | 几何核心：Edge/Triangle/Circle | 78 |
| M2 | ✓ | 结构化结果：Delaunay | 16 |
| M3 | ✓ | 符号稳定化：Serde + DSL | 34 |
| M4 | ✓ | 性能与文档：Benchmark + README | - |
| M5 | ✓ | 架构重构：Generic 合并到 Typed | - |
| M6 | ✓ | 性能优化：PowerVectorKey (+55%) | - |
| M7 | ✓ | 功能对标：Rust vs Kotlin 对比验证 | - |
| M8 | ✓ | Geometry 补齐：pointOnBoundary/isTangent | 12 |
| M9 | ✓ | Symbol 功能：多项式因式分解 | 17 |
| M10 | ✓ | Symbol 功能：多项式积分 | 18 |
| M11 | ✓ | Mutable combineTerms 原地合并 | 9 |
| M12 | ✓ | addAssignAndCombine 便捷方法 | - |
| M13 | ✓ | Symbol + MultiArray 文档示例 | - |
| M14 | ✓ | MultiArray FastSum（sumAxis/sumAll） | 14 |

**M1-M14 全部完成。**

---

## 单元测试清单

### 几何模块 ✓
- `EdgeTest.kt`：29 tests
- `TriangleTest.kt`：25 tests
- `CircleTest.kt`：36 tests (+12 新增)
- `TriangulationTest.kt`：16 tests

### Symbol 模块 ✓
- `SerializationTest.kt`：17 tests
- `DslTest.kt`：17 tests
- `FactorizationTest.kt`：17 tests (+新增)
- `IntegrationTest.kt`：18 tests (+新增)
- `MutableCombineTest.kt`：9 tests (+新增)
- `EvaluateTest.kt`：多项式求值
- `CombineTermsTest.kt`：合并同类项
- `CompileTest.kt`：编译求值
- `DifferentiateTest.kt`：求导
- `LatexTest.kt`：LaTeX 输出
- `ConvertTest.kt`：类型转换
- `MatrixFormTest.kt`：矩阵形式

### MultiArray 模块 ✓
- `FastSumTest.kt`：14 tests (+新增)

### 测试命令
```bash
# 几何模块
mvn -pl ospf-kotlin-utils -Dtest=EdgeTest,TriangleTest,CircleTest,TriangulationTest test

# Symbol 模块
mvn -pl ospf-kotlin-utils -Dtest=SerializationTest,DslTest,FactorizationTest,IntegrationTest,EvaluateTest,CombineTermsTest,MutableCombineTest test

# MultiArray 模块
mvn -pl ospf-kotlin-utils -Dtest=FastSumTest test
```

---

## 架构变更记录

### M5 重构：Generic → Typed 统一

**变更前**：
```
Typed 类型 (LinearMonomial<T>, ...)
    ↓ toGenericXxx()
Generic 类型 (GenericLinearMonomial<T>, ...)
    ↓ 操作函数
Generic 结果
    ↓ toXxxPolynomial()
Typed 结果
```

**变更后**：
```
Typed 类型 (LinearMonomial<T>, ...)
    ↓ 直接操作函数
Typed 结果
```

**新增文件**：
| 文件 | 功能 |
|------|------|
| `LinearQuadraticOps.kt` | Ring-based Linear/Quadratic 操作 |
| `CanonicalOps.kt` | Ring-based Canonical 操作 |
| `CompileOps.kt` | 编译求值（compileEval, compileGradient） |
| `DifferentiateOps.kt` | 求导（derivative, gradient） |
| `LatexOps.kt` | LaTeX 输出（toLatexString） |
| `ConvertOps.kt` | 类型转换（toQuadraticMonomial 等） |
| `Sum.kt` | 求和扩展函数（sum, sumInt32） |

**删除文件**：
- `symbol/generic/*.kt`（11 个源文件）
- `test/.../generic/*.kt`（7 个测试文件）

### M6 重构：PowerVectorKey 性能优化

**变更前**：
```kotlin
// combineCanonicalMonomials 使用 Map 作为 key
val coefficientOfPowers = LinkedHashMap<Map<Symbol, Int32>, T>()
for (monomial in this) {
    // 每次需要排序规范化
    val normalizedPowers = monomial.powers.entries
        .sortedWith { lhs, rhs -> comparator.compare(lhs.key, rhs.key) }
        .associate { it.key to it.value }
    coefficientOfPowers[normalizedPowers] = ...
}
```

**变更后**：
```kotlin
// 使用 PowerVectorKey 作为 key
val coefficientOfKey = LinkedHashMap<PowerVectorKey, T>()
for (monomial in this) {
    val key = PowerVectorKey.create(
        powers = monomial.powers,
        symbolIndex = symbolIndex,
        totalSymbols = symbolList.size
    )
    coefficientOfKey[key] = ...
}
```

**新增文件**：
| 文件 | 功能 |
|------|------|
| `PowerVectorKey.kt` | 高性能 HashMap key（密集/稀疏双模式） |

**性能提升**：
| Benchmark | 基线 | 优化后 | 提升 |
|-----------|------|--------|------|
| combineTermsStress | 61 ops/ms | 94 ops/ms | **+55%** |

注：同环境 JDK 21 对比，之前报告误用了 JDK 17 基线。

### M11 实现：Mutable 多项式 FastSum 支持 ✓

**变更后**：
```
MutableLinearPolynomial<T>
    ├── plusAssign(LinearMonomial<T>)    ✓ 已有
    ├── plusAssign(LinearPolynomial<T>)  ✓ 已有
    ├── minusAssign(...)                 ✓ 已有
    ├── timesAssign(T)                   ✓ 已有
    ├── divAssign(T)                     ✓ 已有
    └── combineTerms(zero, isZero)       ✓ 新增
```

**新增文件**：
| 文件 | 功能 |
|------|------|
| `symbol/operation/MutableCombineOps.kt` | Mutable 多项式原地合并 |

**实现内容**：
```kotlin
// 原地合并同类项
fun <T : NumberField<T>> MutableLinearPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
)

fun <T : NumberField<T>> MutableQuadraticPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
)

fun <T : NumberField<T>> MutableCanonicalPolynomial<T>.combineTerms(
    zero: T,
    isZero: (T) -> Boolean = { it == zero },
    symbolComparator: Comparator<Symbol>? = null
)

// 可选便捷方法：累加后自动合并
fun <T : NumberField<T>> MutableLinearPolynomial<T>.addAssignAndCombine(
    rhs: LinearPolynomial<T>,
    zero: T,
    isZero: (T) -> Boolean = { it == zero }
)
```

**使用示例**：
```kotlin
// FastSum 模式：先累加，后合并
val result = MutableLinearPolynomial.fromConstant(Flt64.zero)
for (poly in polynomials) {
    result += poly  // 快速累加（不合并）
}
result.combineTerms(Flt64.zero)  // 最后合并
val final = result.toImmutable()
```

### M14 实现：MultiArray FastSum 支持 ✓

**变更后**：
```
AbstractMultiArray<T, S>
    ├── get(i) / get(vararg v)    ✓ 已有
    ├── view(dummyVector)         ✓ 已有
    ├── reshape(newShape)         ✓ 已有
    ├── toStorageOrder(order)     ✓ 已有
    └── sumAll(zero)              ✓ 新增
    └── sumAxis(axis, zero)       ✓ 新增
    └── sumAxes(axes, zero)       ✓ 新增
    └── cumsumAxis(axis, zero)    ✓ 新增
```

**新增文件**：
| 文件 | 功能 |
|------|------|
| `multi_array/FastSum.kt` | MultiArray 求和扩展函数 |

**实现内容**：
```kotlin
// 全局求和
fun <T> AbstractMultiArray<T, *>.sumAll(zero: T): T where T : Ring<T>

// 沿单轴求和
fun <T> AbstractMultiArray<T, *>.sumAxis(axis: Int, zero: T): MultiArray<T, DynShape> where T : Ring<T>

// 沿多轴求和
fun <T> AbstractMultiArray<T, *>.sumAxes(axes: IntArray, zero: T): MultiArray<T, DynShape> where T : Ring<T>

// 累积求和（前缀和）
fun <T> AbstractMultiArray<T, *>.cumsumAxis(axis: Int, zero: T): MultiArray<T, DynShape> where T : Ring<T>
```

**使用示例**：
```kotlin
// 符号数组求和
val equations: MultiArray<LinearPolynomial<Flt64>, Shape2> = MultiArray.newBy(Shape2(3, 4)) { i, _ ->
    // 创建多项式...
}

// 沿轴 0 求和 -> 1D 数组
val sum0 = equations.sumAxis(0, LinearPolynomial.fromConstant(Flt64.zero))

// 全局求和 -> 单个多项式
val total = equations.sumAll(LinearPolynomial.fromConstant(Flt64.zero))
```

---

## 新增 API 使用示例

```kotlin
// Ring-based 操作（直接使用，无需转换）
val poly = LinearPolynomial<Flt64>(
    monomials = listOf(
        LinearMonomial(Flt64(2.0), x),
        LinearMonomial(Flt64(3.0), y)
    ),
    constant = Flt64(1.0)
)

// 合并同类项
val combined = poly.combineLinearTerms(Flt64.zero)

// 求值
val value = poly.evaluateLinear(mapOf(x to Flt64(1.0), y to Flt64(2.0)))

// 编译求值（高性能）
val compiled = poly.compileEvalLinear(listOf(x, y), zero = Flt64.zero)
val result = compiled(listOf(Flt64(1.0), Flt64(2.0)))

// 求导
val deriv = poly.derivativeLinear(x, zero = Flt64.zero)
val grad = poly.gradientLinear(listOf(x, y), zero = Flt64.zero)

// LaTeX 输出
val latex = poly.toLatexString(
    LatexNumberOps(
        isZero = { it == Flt64.zero },
        isOne = { it == Flt64.one },
        isNegative = { it.toDouble() < 0.0 },
        abs = { it.abs() },
        format = { it.toString() }
    )
)

// 类型转换
val quadratic = poly.toQuadraticPolynomial()
val canonical = poly.toCanonicalPolynomial()
```