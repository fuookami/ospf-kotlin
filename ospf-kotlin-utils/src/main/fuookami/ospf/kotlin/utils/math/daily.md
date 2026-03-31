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

## 未完成事项

### 1. 测试密度后续补齐
- `algebra`：补 `number/concept/value_range` 的属性测试与跨类型一致性测试。
- `symbol`：补随机表达式 round-trip 与高阶项回归集。

### 2. 代数常量抽象重构（仅立项，未实现）
背景：
- Kotlin 无法等价 Rust 的“静态成员 trait”约束。
- 计划采用“伴生对象实现常量接口”的方式提供 `Zero/One/...` 能力。

计划接口清单：
1. `HasZero<T>`：`val zero: T`
2. `HasOne<T>`：`val one: T`
3. `HasTwo<T>`：`val two: T`
4. `HasThree<T>`：`val three: T`
5. `HasFive<T>`：`val five: T`
6. `HasTen<T>`：`val ten: T`
7. `HasHalf<T>`：`val half: T`
8. `HasBounds<T>`：`val minimum: T`、`val maximum: T`
9. `HasFixedPrecision<T>`：`val decimalDigits: Int?`、`val decimalPrecision: T?`、`val epsilon: T?`
10. `HasInfinity<T>`：`val infinity: T?`、`val negativeInfinity: T?`
11. `HasNaN<T>`：`val nan: T?`
12. `HasTranscendentals<T>`：`val pi: T`、`val e: T`、`val lg2: T`

组合接口（便于泛型约束）：
- `ArithmeticConst<T>` = `HasZero<T>` + `HasOne<T>`
- `RealConst<T>` = `ArithmeticConst<T>` + `HasTwo<T>` + `HasThree<T>` + `HasFive<T>` + `HasTen<T>` + `HasBounds<T>` + `HasFixedPrecision<T>` + `HasInfinity<T>` + `HasNaN<T>`
- `FloatingConst<T>` = `RealConst<T>` + `HasHalf<T>` + `HasTranscendentals<T>`

使用策略（待落地）：
- 泛型算法优先显式传入 companion provider。
- 反射获取 companion 仅作为受控备选路径。

## 下一步建议
1. 先完成 `algebra` 补测，再做 `symbol` round-trip 回归集。
2. 进入“伴生对象常量接口”最小可用实现（先 `HasZero/HasOne/HasTwo/HasHalf` 与组合接口）。

## 执行任务清单（下一阶段）

### T1（P1）algebra 属性与一致性补测
目标：
- 为 `number/concept/value_range` 增补属性测试与跨类型一致性测试，提升代数层回归保障。

建议拆分：
1. `number`：加法交换律、乘法交换律、分配律、单位元与逆元（适用类型）抽样测试。
2. `concept`：对 `Arithmetic/NumberField/RealNumber` 关键行为做最小编译期与运行期约束回归。
3. `value_range`：边界开闭区间、空区间、交并集与包含关系一致性。

建议新增测试文件：
- `src/test/fuookami/ospf/kotlin/utils/math/algebra/number/NumberPropertiesTest.kt`
- `src/test/fuookami/ospf/kotlin/utils/math/algebra/concept/ConceptPropertyTest.kt`
- `src/test/fuookami/ospf/kotlin/utils/math/algebra/value_range/ValueRangePropertyTest.kt`

验收命令：
- `mvn -pl ospf-kotlin-utils "-Dtest=NumberPropertiesTest,ConceptPropertyTest,ValueRangePropertyTest,LawTest,QuantityValueRangeTest" test`
- `mvn -pl ospf-kotlin-utils test`

完成标准：
- 新增测试稳定通过。
- 不引入已有测试回归。

### T2（P1）symbol round-trip 与高阶项回归补测
目标：
- 覆盖随机表达式 parse -> normalize/compile -> serialize -> parse 的 round-trip 语义稳定性。
- 增加高阶项（高次幂、多变量）在化简与求值链路的回归样例。

建议拆分：
1. 随机表达式生成（受控种子、固定样本规模）。
2. round-trip 等价判定（语义等价优先，不强制字符串逐字一致）。
3. 高阶项回归集（人工样例 + 随机样例混合）。

建议新增测试文件：
- `src/test/fuookami/ospf/kotlin/utils/math/symbol/roundtrip/SymbolRoundTripTest.kt`
- `src/test/fuookami/ospf/kotlin/utils/math/symbol/operation/HighOrderRegressionTest.kt`

验收命令：
- `mvn -pl ospf-kotlin-utils "-Dtest=SymbolRoundTripTest,HighOrderRegressionTest,ParserPolynomialTest,CompileTest,EvaluateTest,SerializationTest" test`
- `mvn -pl ospf-kotlin-utils test`

完成标准：
- round-trip 测试可重复（固定随机种子）。
- 高阶项样例全部通过且无现有用例回退。

### T3（P2）伴生对象常量接口最小落地
目标：
- 完成伴生对象常量接口最小可用版本，替代部分 `value -> constants` 依赖路径。

第一阶段范围（最小实现）：
- 接口：`HasZero<T>`、`HasOne<T>`、`HasTwo<T>`、`HasHalf<T>`、`ArithmeticConst<T>`、`FloatingConst<T>`（或先行子集）。
- 类型接入：先接 `Flt64/Flt32/Int32`（最常用类型），再按回归情况扩展。
- 算法接入：挑选 1~2 个高频泛型算法改为显式 provider 参数。

建议新增/修改文件：
- `src/main/.../algebra/concept/ConstantProviders.kt`（新建）
- `src/main/.../algebra/number/*`（对应 companion 实现接口）
- `src/test/.../algebra/concept/ConstantProviderTest.kt`（新建）

验收命令：
- `mvn -pl ospf-kotlin-utils "-Dtest=ConstantProviderTest,ConceptCompileTest,LawTest" test`
- `mvn -pl ospf-kotlin-utils test`

完成标准：
- 最小接口可被 companion 实现并在泛型算法中使用。
- 无反射路径也可完整运行核心测试。

### T4（P2）反射获取 companion 的受控通道（可选）
目标：
- 提供可选反射 fallback，仅用于需要运行时类型驱动的场景。

约束：
- 默认关闭反射路径；优先显式 provider 传入。
- 反射失败时给出清晰错误信息，不静默降级。
- 为反射路径补性能与稳定性回归。

验收命令：
- `mvn -pl ospf-kotlin-utils "-Dtest=ConstantProviderTest" test`

完成标准：
- 显式 provider 与反射 fallback 行为一致。

## 里程碑与顺序
1. 先做 `T1`（代数补测）。
2. 再做 `T2`（symbol round-trip）。
3. 然后做 `T3`（常量接口最小落地）。
4. 最后视需要做 `T4`（反射 fallback）。

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

任务状态更新：
- `T1（algebra 属性与一致性补测）`：已完成。
- `T2（symbol round-trip 与高阶项回归补测）`：未开始。
- `T3（伴生对象常量接口最小落地）`：未开始。

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

任务状态更新：
- `T1（algebra 属性与一致性补测）`：已完成。
- `T2（symbol round-trip 与高阶项回归补测）`：已完成。
- `T3（伴生对象常量接口最小落地）`：未开始。

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

任务状态更新：
- `T1（algebra 属性与一致性补测）`：已完成。
- `T2（symbol round-trip 与高阶项回归补测）`：已完成。
- `T3（伴生对象常量接口最小落地）`：已完成。
- `T4（反射 fallback）`：未开始（可选）。
