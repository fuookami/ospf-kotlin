# ospf-kotlin-utils/math 对标 ospf-rust-math 进度纪要（对照版）

最后更新：2026-04-02（M5 完成）
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

---

## 待办事项

### P1 性能优化（中优先级）

#### S-PERF-NEXT: 进一步性能优化
- 热点路径深度优化（`combineTerms` 仍有提升空间）
- 内存分配优化（减少中间对象创建）
- 并行化支持（大规模多项式运算）
- 基准对比：建立优化前后对照基线

### P2 功能扩展（低优先级）

#### S-FUNC: 高级功能
- 多项式因式分解
- 符号积分
- 更多数值类型支持（复数、有理数）
- 稀疏多项式优化

---

## 里程碑总结

| 里程碑 | 状态 | 主要内容 | 测试数 |
|--------|------|----------|--------|
| M1 | ✓ | 几何核心：Edge/Triangle/Circle | 78 |
| M2 | ✓ | 结构化结果：Delaunay | 16 |
| M3 | ✓ | 符号稳定化：Serde + DSL | 34 |
| M4 | ✓ | 性能与文档：Benchmark + README | - |
| M5 | ✓ | 架构重构：Generic 合并到 Typed | - |

**所有里程碑（M1-M5）已完成。**

---

## 单元测试清单

### 几何模块 ✓
- `EdgeTest.kt`：29 tests
- `TriangleTest.kt`：25 tests
- `CircleTest.kt`：24 tests
- `TriangulationTest.kt`：16 tests

### Symbol 模块 ✓
- `SerializationTest.kt`：17 tests
- `DslTest.kt`：17 tests
- `EvaluateTest.kt`：多项式求值
- `CombineTermsTest.kt`：合并同类项
- `CompileTest.kt`：编译求值
- `DifferentiateTest.kt`：求导
- `LatexTest.kt`：LaTeX 输出
- `ConvertTest.kt`：类型转换
- `MatrixFormTest.kt`：矩阵形式

### 测试命令
```bash
# 几何模块
mvn -pl ospf-kotlin-utils -Dtest=EdgeTest,TriangleTest,CircleTest,TriangulationTest test

# Symbol 模块
mvn -pl ospf-kotlin-utils -Dtest=SerializationTest,DslTest,EvaluateTest,CombineTermsTest test
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