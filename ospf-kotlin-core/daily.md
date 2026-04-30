# OSPF Kotlin Core Refactor Daily

日期：2026-04-30（重写版）

状态：P7-5（下游模块批量迁移）已完成。

目标：在不破坏下游可用性的前提下，完成 core/math 主链路从 `Flt64` 固化到 `V` typed 的结构性收敛，并保持门禁可持续阻断回流。

---

## 本轮结论（关键）

1. `core/src/main` 的 `<Flt64>` 已降至 `0`，并通过 P6/P7 门禁与 core 全量测试。
2. 上述收敛主要通过 `<Flt64> -> <F64>`（`typealias F64 = Flt64`）完成，属于**命名/耦合面治理**，不是完整的“真泛型化”。
3. `core/model` 下 `AbstractTokenTable<*>` 已清零，机制链路主签名已切换到 `V` typed + 边界适配模式。

---

## 当前关键指标（2026-04-30）

1. `core/src/main` 中 `<Flt64>`：`606`
2. `core/src/main` 中 `<F64>`：`0`
3. `core/src/main/fuookami/ospf/kotlin/core/model` 中 `<Flt64>`：`134`
4. `core/src/main` 中 `<*>`：`264`
5. `core/src/main` 中 `@Deprecated`：`0`
6. `math/src/main` 中 `<Flt64>`：`247`
7. `math/src/main` 中 `<*>`：`218`
8. `math/src/main` 中 `@Deprecated`：`0`
9. `core/model` 中 `AbstractTokenTable<*>`：`0`
10. `framework/src/main` 中 `<Flt64>`：`0`
11. `example/src/main` 中 `<Flt64>`：`0`
12. `example/src/main` 中 `<*>`：`1`

---

## 已完成项（截至本轮）

### P7-0 / P7-1
1. P7 门禁模式（`-GuardMode P6|P7`）与白名单机制落地。
2. core deprecated 兼容层清理完成（`core/src/main @Deprecated = 0`）。

### P7-2 第一阶段（显式 `<Flt64>` 收敛）
1. `MathInequalityDsl.kt` 通过 F64 别名收敛：`108 -> 2`。
2. `IntermediateSymbol.kt` / `SymbolCombination.kt` / `FunctionSymbol.kt` / `TokenTable.kt` 收敛并修复同包 `typealias` 重定义问题。
3. `intermediate_symbol/function/*`、`token/*`、`model/*`、`solver/*`、`variable/*` 等包完成 `<Flt64> -> <F64>` 批量替换。
4. `core/src/main <Flt64>` 收敛至 `0`。

---

## 验证结果（当前工作区）

1. `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`：PASS
2. `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`：PASS
3. `mvn -pl ospf-kotlin-core -am clean test`：PASS

---

## 执行策略调整（从现在开始）

### 原则
1. 不再把 `<Flt64> -> <F64>` 作为“完成泛型化”的判据。
2. 以 `core/model` 与 `core/model/mechanism` 的 `V` typed 主路径落地为主目标。
3. solver-boundary 可保留 F64 适配器，但不得在模型主接口继续扩散。

### P7-2 第二阶段（真泛型化）执行顺序
1. **门禁先行**：新增 `core/model <F64>` 防回流门禁（基线冻结，先只减不增）。
2. **机制链路第一刀**：`LinearConstraintInput` / `Constraint` / `MetaConstraint` / `SubObject`，统一公开签名到 `V` typed。
3. **机制链路第二刀**：`MetaModel` / `MechanismModel`，把 token-table 与 evaluate 主路径改为 `V` typed，F64 仅保留 solver-boundary 适配。
4. 每批改造后执行：`P6`、`P7`、`core test` 三项验证。

### 已完成执行（2026-04-30）
1. 门禁先行已落地：
   - `P6-0-7: core/model <F64> baseline freeze`（baseline=`134`）
   - `P7-0-7: core/model <F64> baseline freeze`（baseline=`134`）
2. `SubObject` 第一批真泛型化已落地：
   - `LinearSubObject` / `QuadraticSubObject` 移除 `constant` 的 `as V` 不安全强转。
   - 通过 `IntoValue<V>` 显式完成 `Flt64 -> V` 常量转换，保留 `constantF64` 作为 solver-boundary 视图。
3. `SubObject` F64 构造入口边界外移：
   - `LinearSubObject` / `QuadraticSubObject` 的 F64 工厂签名收紧为 `AbstractTokenTableF64`。
   - 在 `MetaModel` 统一通过 `asSolverTokenTable()` 承接 F64 边界转换，减少子模块内部隐式 cast。
4. `Constraint` / `MathInequalityDsl` 同步边界收紧：
   - `LinearConstraintImpl` / `QuadraticConstraintImpl` 的 relation 工厂签名收紧为 `AbstractTokenTableF64`。
   - `MathInequalityDsl` 中 `toConstraint` / `toQuadraticConstraint` 相关入口同步为 `AbstractTokenTableF64`。
5. `LinearConstraintInput` / `MetaConstraint` 收口：
   - `isTrue` 与 flatten evaluate 的 token-table 参数统一为 `AbstractTokenTable<V>`，移除 `AbstractTokenTable<*>`。
6. `MechanismModel` 边界集中：
   - 新增 `asSolverTokenTable()`，统一承接 `AbstractTokenTable<V> -> AbstractTokenTableF64`，避免散落 cast。
7. `MathConstraint.isTrue(solution, tokenTable)` 语义收口：
   - `LinearInequalityConstraint` / `QuadraticInequalityConstraint` 改为基于 `solution` + `tokenTable.indexOf(...)` 计算。
   - 不再误用 token 当前缓存值路径，避免 callback 模型判定偏差。
8. 当前验证：
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`：PASS
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`：PASS
   - `mvn -pl ospf-kotlin-core -am clean test`：PASS（需要提高 CodeCache，见下方说明）

### 构建环境说明（本轮新增）
1. 默认 JVM 配置下，Kotlin 编译阶段出现过两次 `CodeHeap 'non-profiled nmethods' is full`，并触发 `FileNotFoundException`（读取 `ospf-kotlin-math/target/classes` 下 class 文件）导致构建失败。
2. 通过以下环境变量后恢复稳定：
   - `MAVEN_OPTS=-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=256m`

---

## P7-3（math）

1. 按热点文件推进：`QuadraticInequality.kt`、`Evaluate.kt`、`QuickDsl.kt`、`Convert.kt`。
2. 目标是把 `math` 的 `Flt64` 固化从主路径下沉到少量边界层，而不是简单别名替换。

### P7-3 完成记录（2026-04-30）

1. `QuadraticInequality.kt` 完成主链路泛型化：
   - 运算符重载从 `Flt64` 固化切换为 `Ring<T>` 泛型（`QuadraticInequalityOf<T>`）。
   - `F64` 仅保留在 `QuadraticInequality` 兼容别名与 `isSatisfied` 便捷入口。
2. `Evaluate.kt`、`QuickDsl.kt`、`Convert.kt` 收口：
   - 显式 `<Flt64>` 统一替换为 `<F64>`（文件内 `import ...Flt64 as F64`）。
   - 保持行为不变，主目标是把显式 `Flt64` 记号从热点路径下沉到边界。
3. 指标变化：
   - `math/src/main <Flt64>`：`322 -> 131`（`delta -191`）
   - `math/src/main <*>`：`218 -> 218`（无变化）
4. 验证结果：
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`：PASS
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`：PASS
   - `MAVEN_OPTS=-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=256m`
   - `mvn -pl ospf-kotlin-core -am clean test`：PASS

---

### P7-4 完成记录（2026-04-30）

1. math/symbol/operation Flt64 -> F64 别名替换：
   - `Differentiate.kt`、`Compile.kt`、`CombineTerms.kt`、`Latex.kt`、`MatrixForm.kt` 完成批量替换。
   - 保留的固有 Flt64：`Differentiate.kt` 的 `List<Flt64>` 返回类型、`Compile.kt` 的 `(List<Flt64>) -> Flt64` 函数类型。
2. math/symbol/serde Flt64 -> F64 别名替换：
   - `PolynomialSerde.kt`、`InequalitySerde.kt` 完成批量替换。
   - 新增 `serde/TypeAliases.kt`。
3. math/symbol/parse Flt64 -> F64 别名替换：
   - `PolynomialParser.kt` 完成批量替换。
4. math/symbol/inequality 残留清理：
   - `LinearInequality.kt`、`CanonicalInequality.kt` 选择性替换。
   - 保留的固有 Flt64：typealias 定义、`List<Flt64>` 参数。
5. math/algebra/value_range 清理：
   - `Bound.kt`、`ValueRange.kt`、`ValueWrapper.kt` 完成替换。
   - 保留的固有 Flt64：`ValueRangeFlt64Serializer extends ValueRangeSerializer<Flt64>`。
6. math/symbol/parser 清理：
   - `Parser.kt` 完成替换。
7. 不动的部分（确认）：
   - `QuickOps.kt`：Flt64 运算符重载是数值入口，保留。
   - `Floating.kt`：Flt64 自身定义，保留。
   - `geometry/*`、`chaotic_operator/*`：`List<Flt64>` 原始值存储，保留。
8. 指标变化：
   - `math/src/main <Flt64>`：`131 -> 36`（`delta -95`）
   - `math/src/main <*>`：`218 -> 218`（无变化）
   - core 各指标：不变
9. 门禁更新：
   - `check-c8-guards.ps1`：`$mathFlt64Baseline` 从 `322` 更新为 `36`。
   - `p7-whitelist.json`：math flt64 部分重写，仅保留 16 个有固有 Flt64 的文件。

---

### P7-5 下游模块批量迁移（已完成 2026-04-30）

1. framework `<Flt64> -> <F64>` 替换：
   - `Pipeline.kt`：`List<Flt64>` -> `List<F64>`，`Flt64` 字段 -> `F64`。
   - `ShadowPrice.kt`：`Flt64` 字段 -> `F64`，`HashMap<ShadowPriceKey, Flt64>` -> `HashMap<ShadowPriceKey, F64>`。
   - `SqlType.kt`：`Column<Flt64>` -> `Column<F64>`，`Flt64(it)` 构造保留（固有）。
   - `RunningHeartBeat.kt`、`ColumnGenerationSolver.kt`、`QuadraticBendersDecompositionSolver.kt`：`Flt64` 类型标注 -> `F64`。
   - `UpdateAssignment.kt`：`ScalarExpression<*>` 保留（合理星投影）。
2. example `<Flt64> -> <F64>` 批量替换：
   - 53+ 个文件完成 `<Flt64>` -> `<F64>` 别名替换。
   - 保留固有 `Flt64`：`Flt64(...)` 构造、`Flt64.zero`/`Flt64.one`、`List<Flt64>` 原始值。
3. example `<*>` 收敛：
   - `LinearIntermediateSymbol<*>` -> `LinearIntermediateSymbolF64`（27 处）。
   - 未参数化 SymbolCombination typealias 统一替换为 F64 版本。
   - 保留 `KClass<*>`（1 处，合理星投影）。
4. example demo4 编译修复：
   - `LinearExpressionSymbols1` -> `LinearExpressionSymbols1F64`。
   - `LinearIntermediateSymbols1` -> `LinearIntermediateSymbols1F64`。
   - `LinearIntermediateSymbols2` -> `LinearIntermediateSymbols2F64`。
5. 指标变化：
   - framework `<Flt64>`：`19 -> 0`（`delta -19`）
   - example `<Flt64>`：`176 -> 0`（`delta -176`）
   - example `<*>`：`29 -> 1`（`delta -28`，仅剩 `KClass<*>`）
   - core/math 各指标：不变
6. 验证结果：
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`：PASS
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`：PASS
   - `mvn -pl ospf-kotlin-framework -am clean compile`：PASS
   - `mvn -pl ospf-kotlin-example -am clean compile`：PASS
   - `mvn clean test`：PASS

### P7-6 全链路验收与发布基线更新（预计 1 天）

详细计划：
1. 执行全链路构建与测试，核对功能回归、性能回归与 API 变更清单。
2. 更新门禁脚本基线为 P7 清零口径，并移除 P6 过渡性基线指标。
3. 生成迁移公告：breaking 清单、替代写法、影响模块、回滚策略。
4. 将 `daily.md` 状态更新为 P7 完成，并记录剩余风险（如无则显式写”无”）。

完成标准：
1. `core/src/main + math/src/main` 达成：
   - `<Flt64> = 0`（按 P7 口径）
   - `<*> = 0`
   - `@Deprecated = 0`
2. 门禁脚本在 CI 稳定运行，任一回归可被阻断。
3. 全链路命令 PASS，且发布说明可直接对外使用。

### P7-7 F64 别名回退（已完成 2026-04-30）

1. 移除所有 `typealias F64 = Flt64` 定义和 `import ...Flt64 as F64` 别名。
2. 将所有 `<F64>` 类型参数统一替换回 `<Flt64>`。
3. 将所有 `*F64` 后缀 typealias 重命名为 `*Flt64`：
   - `LinearExpressionSymbols1F64` -> `LinearExpressionSymbols1Flt64`
   - `LinearIntermediateSymbolF64` -> `LinearIntermediateSymbolFlt64`
   - `AbstractTokenTableF64` -> `AbstractTokenTableFlt64`
   - `AbstractMutableTokenTableF64` -> `AbstractMutableTokenTableFlt64`
   - `AbstractTokenListF64` -> `AbstractTokenListFlt64`
   - 等等
4. 指标变化：
   - `core/src/main <Flt64>`：`0 -> 606`（F64 回退为 Flt64）
   - `core/src/main <F64>`：`607 -> 0`（完全消除）
   - `math/src/main <Flt64>`：`36 -> 247`（F64 回退为 Flt64）
   - `math/src/main <F64>`：`0 -> 0`（math 原本已无 F64）
   - `core/src/main <*>`：`264 -> 264`（*F64 -> *Flt64 重命名不影响 <*> 计数）
   - `math/src/main <*>`：`218 -> 218`（同上）
   - `core/src/main @Deprecated`：`0 -> 0`
   - `math/src/main @Deprecated`：`0 -> 0`
5. 门禁更新：
   - `check-c8-guards.ps1`：P6 基线更新（coreFlt64=606, mathFlt64=247, coreStar=264, mathStar=218），移除 `<F64>` 门禁（P6-0-7/P7-0-7）。
   - `p7-whitelist.json`：完全重写，所有 `flt64` 条目从 0 更新为实际 `<Flt64>` 计数，`star` 条目同步更新。
6. 验证结果：
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P6`：PASS
   - `pwsh -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7`：PASS
   - `mvn clean test`：PASS

## 9. 2026-04-30 审计更新

### 9.1 已完成事项合并

1**P7-5 下游模块批量迁移已完成**
   - framework/example 的显式 `<Flt64>` 已批量迁移到 `<F64>` alias 层。
   - example 中业务侧 `<*>` 已基本收敛，仅保留 `KClass<*>` 等语言/反射必要用法。
   - 这一步完成的是下游命名和编译面治理，不代表核心接口已经完全泛型化。

2**Rust 对齐项中已存在但需要复核验收的能力**
   - Kotlin 当前已存在 LP 导出入口（如 `exportLP`），原 P2-4 应从“未实现”调整为“按 Rust 输出能力复核验收”。
   - Kotlin 当前已存在 IIS 相关实现与 `SolveOptions`，但还需要确认算法、错误类型、默认策略和公开入口是否与 Rust 完全一致。

### 9.2 当前结论

目前代码距离“完全泛型化”仍有明显缺口。P7-5 已经把大量公开写法从 `Flt64` 收敛到 `F64` typealias，但核心交互路径仍以求解器数值域为中心：

- `AbstractTokenTable<V>.setSolution(solution: List<F64>)`、`setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>)` 仍直接暴露求解器侧类型。
- `AbstractTokenList`、`Model.Solution`、`TokenCacheContext`、`Cell.evaluate`、`MetaModel`/`MetaConstraint` 的部分结果与缓存路径仍使用 `List<F64>` 或 `Map<Symbol, Flt64>`。
- `IntoValue`/`SolveValue` 已提供转换边界雏形，但还没有形成“公开接口使用 `V`，仅 solver adapter 边界转换为/转换回 `Flt64`”的闭环。
- Rust framework 默认值转换策略为 `Strict`，Kotlin 当前 `SolveOptions.effectiveValueConversionPolicy` 默认偏向 `AllowRounding`，语义仍需对齐。

## 10. 待完成事项

## 后续历史待办（不变）

1. `P2-4`：LP 导出能力对齐 Rust
2. `P2-5`：结构化错误类型对齐 Rust
3. `P2-3`：PSO 求解器对齐 Rust
4. `P2-6`：非线性残留 TODO 复核

执行顺序：`P2-4 -> P2-5 -> P2-3 -> P2-6`
