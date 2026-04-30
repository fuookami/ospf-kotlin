# OSPF Kotlin Core Refactor Daily

日期：2026-04-30（重写版）

状态：P7-2（第一阶段 + 第二阶段）已完成；P7-3（math 主链路收口）已完成。

目标：在不破坏下游可用性的前提下，完成 core/math 主链路从 `Flt64` 固化到 `V` typed 的结构性收敛，并保持门禁可持续阻断回流。

---

## 本轮结论（关键）

1. `core/src/main` 的 `<Flt64>` 已降至 `0`，并通过 P6/P7 门禁与 core 全量测试。
2. 上述收敛主要通过 `<Flt64> -> <F64>`（`typealias F64 = Flt64`）完成，属于**命名/耦合面治理**，不是完整的“真泛型化”。
3. `core/model` 下 `AbstractTokenTable<*>` 已清零，机制链路主签名已切换到 `V` typed + 边界适配模式。

---

## 当前关键指标（2026-04-30）

1. `core/src/main` 中 `<Flt64>`：`0`
2. `core/src/main` 中 `<F64>`：`607`
3. `core/src/main/fuookami/ospf/kotlin/core/model` 中 `<F64>`：`134`
4. `core/src/main` 中 `<*>`：`264`
5. `core/src/main` 中 `@Deprecated`：`0`
6. `math/src/main` 中 `<Flt64>`：`131`
7. `math/src/main` 中 `<*>`：`218`
8. `math/src/main` 中 `@Deprecated`：`0`
9. `core/model` 中 `AbstractTokenTable<*>`：`0`

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


### P7-4 下游模块批量迁移（预计 2~3 天）

详细计划：
1. 按模块分批改造：`framework` -> `framework-*` -> `example` -> `starters`。
2. 替换所有被删除兼容 API 的调用点，统一到 P7 主路径签名。
3. 对编译失败按“签名不匹配 / 泛型不兼容 / 推断退化”分类修复，避免临时 cast 回退。
4. 每批完成后执行对应模块编译与关键场景测试。

完成标准：
1. 下游模块无对已删除 deprecated API 的引用。
2. 全仓 `src/main` 不新增 `<*>` 与 `<Flt64>` 回流。
3. 以下命令全部 PASS：
   - `mvn -pl ospf-kotlin-framework -am clean compile`
   - `mvn -pl ospf-kotlin-example -am clean compile`

### P7-5 全链路验收与发布基线更新（预计 1 天）

详细计划：
1. 执行全链路构建与测试，核对功能回归、性能回归与 API 变更清单。
2. 更新门禁脚本基线为 P7 清零口径，并移除 P6 过渡性基线指标。
3. 生成迁移公告：breaking 清单、替代写法、影响模块、回滚策略。
4. 将 `daily.md` 状态更新为 P7 完成，并记录剩余风险（如无则显式写“无”）。

完成标准：
1. `core/src/main + math/src/main` 达成：
   - `<Flt64> = 0`（按 P7 口径）
   - `<*> = 0`
   - `@Deprecated = 0`
2. 门禁脚本在 CI 稳定运行，任一回归可被阻断。
3. 全链路命令 PASS，且发布说明可直接对外使用。

## 后续历史待办（不变）

1. `P2-4`：LP 导出能力对齐 Rust
2. `P2-5`：结构化错误类型对齐 Rust
3. `P2-3`：PSO 求解器对齐 Rust
4. `P2-6`：非线性残留 TODO 复核

执行顺序：`P2-4 -> P2-5 -> P2-3 -> P2-6`
