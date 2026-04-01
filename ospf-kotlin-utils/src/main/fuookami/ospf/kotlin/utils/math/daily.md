# ospf-kotlin-utils/math 对标 ospf-rust-math 改进纪要（重整版）

日期：2026-04-01  
范围：`ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math`

## 已完成事项

### P0 稳定基线
- 修复主代码编译阻塞（Kotlin 2.2 相关泛型/签名兼容）。
- 修复 `Compile.kt` canonical compileEval 对 `one` 的透传。
- 修复 `Quantity.kt` 缺失导入导致的符号解析问题。
- 为 canonical/generic canonical 补齐兼容构造与重载（含 `factors` 兼容）。
- 修复 `Vector.minus` 行为并补回归测试。

验证：
- `mvn -pl ospf-kotlin-utils -DskipTests compile` 通过。
- 关键几何测试与全量测试通过。

### P1 API 补齐（数论 + 组合）
- 数论新增：`defactorize`、`divisors`、`divisorCount`、`eulerTotient`、`gcdMod`、`extendedGcd`、`lcmByFactorization`。
- 组合新增：`combineCount/combine/combineSequence`、`permuteCount/permute/permuteSequence`、`crossCount/crossSequence/cross2/cross3`。
- 已补对应测试并通过。

### P2 抽象升级（代数 + 几何）
- 新增抽象：`TotallyOrdered`、`VectorSpace`、`NormedSpace`、`InnerProductSpace`。
- `Vector` 对齐：`scale`、`dot`、`angle`、`projectionOn`、2D/3D `cross`、`Flt64 * Vector`。
- 已补 `VectorTest` 并通过。

### P3 Symbol 体系增强
- 新增：`SymbolId`、`OwnedSymbolLike`、`OwnedSymbol`、`Symbol.stableId()`、`Symbol.owned(...)`。
- 兼容保留：`IdentifiedSymbol`，并统一 `identity()/defaultSymbolComparator` 到稳定 id 语义。
- 已补 `SymbolIdentityTest` 并通过。

### P4 工程化与补测
- 文档：新增中英 README（互链）。
- 基准：新增 JMH 入口与 `bench` profile。
- 补测已完成：
  - `chaotic_operator` 三批补测（离散映射、连续系统、生成器语义）。
  - `fractal_operator` 首批补测（Mandelbrot 基本行为与生成器语义）。
  - `geometry` 首批补测（Point/Distance/Edge/Triangle/Circle 边界与退化场景）。
- 本轮补测中同步修复：
  - `Distance.Minkowski` 改为对坐标差取绝对值后再幂运算，修复奇数 `p` 在负差值下错误结果。

最新验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=GeometryPrimitiveTest,VectorTest,RectangleTest,TriangulationTest" test` 通过（12 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（410 tests, 0 failures）。

## 当前状态
- `T1（algebra 属性与一致性补测）`：已完成。
- `T2（symbol round-trip 与高阶项回归补测）`：已完成。
- `T3（伴生对象常量接口最小落地）`：已完成。
- `T4（反射 fallback）`：已完成。

## T3 扩展阶段（进行中）
- `T3.1（聚合工具显式 constants 入口扩展）`：第三批已完成。
- `T3.2（核心调用点从 reified fallback 迁移到显式 constants）`：第一批已完成。
- `T3.3（全量迁移与专项回归矩阵）`：第二批已完成。

## 历史进展记录

## 本轮进展（T1 已完成）

新增测试文件：
- `algebra/number/NumberPropertiesTest.kt`
- `algebra/concept/ConceptPropertyTest.kt`
- `algebra/value_range/ValueRangePropertyTest.kt`

新增覆盖点：
- `number`：交换律、分配律、单位元行为、跨类型（Int/Rational/Float）小整数转换一致性。
- `concept`：`RealNumber + NumberField` 泛型下零元/幺元一致性，常量关系一致性（`one/two/three/five/ten/half`）。
- `value_range`：开闭区间边界包含差异、等端点开区间非法性、交并对称性、非重叠区间行为、负系数缩放边界翻转。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=NumberPropertiesTest,ConceptPropertyTest,ValueRangePropertyTest,LawTest,QuantityValueRangeTest" test` 通过（32 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（422 tests, 0 failures）。

## 本轮进展（T2 已完成）

新增测试文件：
- `symbol/roundtrip/SymbolRoundTripTest.kt`
- `symbol/operation/HighOrderRegressionTest.kt`

新增覆盖点：
- 随机多项式表达式（固定 seed）parse -> canonical -> expr/json -> expr -> canonical 的语义 round-trip。
- 高阶项（含 4~7 次组合项）在 compile/evaluate/gradient 链路上的一致性回归。
- 高阶多项式 partial evaluate 与全量 evaluate 的结果一致性。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=SymbolRoundTripTest,HighOrderRegressionTest,ParserPolynomialTest,CompileTest,EvaluateTest,SerializationTest" test` 通过（30 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（427 tests, 0 failures）。

## 本轮进展（T3 最小落地已完成）

新增实现：
- 新增伴生对象常量接口文件：`algebra/concept/ConstantProviders.kt`
  - `HasZero/HasOne/HasTwo/HasThree/HasFive/HasTen/HasHalf`
  - `HasBounds/HasFixedPrecision/HasInfinity/HasNaN/HasTranscendentals`
  - 组合接口：`ArithmeticConst/RealConst/FloatingConst`
- 对齐现有常量体系：
  - `ArithmeticConstants<Self>` 继承 `ArithmeticConst<Self>`
  - `RealNumberConstants<Self>` 继承 `RealConst<Self>`
  - `FloatingNumberConstants<Self>` 继承 `FloatingConst<Self>`

新增测试：
- `algebra/concept/ConstantProviderTest.kt`
  - 验证 companion 显式传入 `ArithmeticConst/RealConst` 可用于泛型算法。
  - 验证浮点常量（`half/lg2/pi/e`）基本约束。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=ConstantProviderTest,ConceptCompileTest,LawTest" test` 通过（17 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（430 tests, 0 failures）。

## 本轮进展（T4 已完成）

实现收尾：
- 受控 companion reflection fallback 已完成并在目标数学路径接入。
- 对依赖 legacy reified 调用的测试补充类级属性开关，避免与“默认关闭 fallback”策略冲突：
  - `algebra/value_range/QuantityValueRangeTest.kt`
  - `algebra/value_range/ValueRangePropertyTest.kt`
  - `ordinary/FactorizationTest.kt`
  - `ordinary/GCDTest.kt`
  - `ordinary/LCMTest.kt`
  - `ordinary/FltXPowerStrategyTest.kt`

验证结果：
- `mvn -pl ospf-kotlin-utils "-Dtest=ValueRangePropertyTest,QuantityValueRangeTest,FactorizationTest,GCDTest,LCMTest,FltXPowerStrategyTest,ConstantProviderReflectionFallbackTest,ConstantProviderTest" test` 通过（35 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（432 tests, 0 failures）。

任务状态更新：
- `T1（algebra 属性与一致性补测）`：已完成。
- `T2（symbol round-trip 与高阶项回归补测）`：已完成。
- `T3（伴生对象常量接口最小落地）`：已完成。
- `T4（反射 fallback）`：已完成。

## 本轮进展（T3 扩展第一批已完成）

实现：
- `functional/Collection.kt`
  - 去除直接 `companionObjectInstance` 获取常量，统一改为受控 resolver。
  - 新增 `Iterable.sumOf/sumOfOrNull` 的显式 `ArithmeticConstants` 参数重载，并保留 reified 包装入口。
- `parallel/Fold.kt`
  - 新增 `sumOfParallelly/trySumOfParallelly/exTrySumOfParallelly` 的显式 `ArithmeticConstants` 参数重载。
  - reified 包装入口改为走受控 resolver。
- `operator/Precision.kt`
  - 新增 `withPrecision(constants, precision = constants.decimalPrecision)` 显式入口。
  - reified 默认入口改为走受控 resolver。
- 关键调用点迁移：
  - `math/geometry/Distance.kt`、`math/geometry/Vector.kt` 改为显式传入 `Flt64` constants（不依赖 fallback）。
  - `parallel/ExParallelTest.kt` 改为显式传入 `Int64` constants。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=GeometryPrimitiveTest,RectangleTest,TriangulationTest,VectorTest,ExParallelTest" test` 通过（58 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（432 tests, 0 failures）。

## 本轮进展（T3 扩展第二批已完成）

实现：
- `functional/Collection.kt`
  - 新增 `sum/sumOrNull` 显式 `ArithmeticConstants` 参数入口（`Iterable/Map/Sequence`）。
  - 新增 `sumOf/sumOfOrNull` 显式 `ArithmeticConstants` 参数入口（`Map/Sequence`），并保留 reified 包装入口。
- 新增回归测试：
  - `functional/CollectionConstantPathTest.kt`
  - 覆盖“fallback 默认关闭时，显式 constants 路径可用；reified 默认路径抛错”的行为。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=CollectionConstantPathTest,GeometryPrimitiveTest,RectangleTest,TriangulationTest,VectorTest,ExParallelTest" test` 通过（61 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（435 tests, 0 failures）。

## 本轮进展（T3 扩展第三批已完成）

实现：
- `functional/Collection.kt`
  - 新增 `average/averageOrNull` 显式 `ArithmeticConstants` 参数入口（`Iterable/Map/Sequence`，含 `Flt64` 返回与同类型返回两个分支）。
  - 对应 reified 入口统一改为显式入口 + 受控 resolver 包装，减少函数体内重复 resolver 调用。
  - 去除 `sum/sumOrNull` 显式入口中无 lambda 的冗余 `inline` 声明，消除编译期性能提示噪声。
- `functional/CollectionConstantPathTest.kt`
  - 扩展覆盖到 `average/averageOrNull`：
    - fallback 关闭时显式 constants 路径可用；
    - fallback 关闭时 reified 默认路径抛错。

验证：
- `mvn -pl ospf-kotlin-utils "-Dtest=CollectionConstantPathTest" test` 通过（3 tests）。
- `mvn -pl ospf-kotlin-utils test` 通过（435 tests, 0 failures）。
