# OSPF Kotlin 泛型化交接计划（L 阶段）

记录日期：2026-05-09
适用范围：`ospf-kotlin-math`、`ospf-kotlin-core`

## 1. 当前状态与结论

P13、F1-F9、G1-G4、H1-H5、I1-I5、J1-J4、K1-K3 已完成。

需要修正上一版结论：

- 当前扫描脚本 `scripts/scan-full-genericization.ps1` 可以 `GATE: PASS`，但这只说明当前白名单口径下无阻断项。
- `math` 和 `core` 尚未达到“完全泛型化”的设计目标。
- 当前 I5 白名单过宽，包含 `solver/`、`model/mechanism`、`ospf-kotlin-math/` 等整目录，导致大量真实的公开 `Flt64` API 被排除在签名扫描之外。
- `LinearMechanismModel`、`QuadraticMechanismModel`、solver 入口、Benders cut/IIS 相关 API 仍然存在公开 `Flt64` 特化签名。
- 后续工作不应继续扩大 whitelist，应收紧扫描口径，把被遮蔽的债务重新暴露出来，再逐块泛型化。

本阶段目标：

1. 将 `math` 与 `core` 的主 API 调整为真实 V-typed。
2. 将 `Flt64` 限定到数值类型本体、`adapter/flt64` 兼容层、solver backend 最末端边界。
3. 将扫描门禁从“粗白名单 PASS”升级为“设计目标 PASS”。

## 2. 已完成事项总结

| 阶段 | 状态 | 结果摘要 |
|---|---|---|
| P13 | done | 主链扫描基线落地，`public_api_blocking = 0` |
| F1-F9 | done | core 历史测试修复、Flt64 typealias 收口、UNCHECKED_CAST 集中化、脚本与文档落地 |
| G1-G4 | done | mechanism/callback 债务继续压缩，扫描与文档同步 |
| H1-H5 | done | boundary_detail 输出、mechanism/callback 清零、桥接收尾、非 adapter typealias 清零 |
| I1 | done | 基线固化，daily + JSON 同步 |
| I2 | done | raw Flt64 语义分类（PUBLIC_API_BLOCKING / SOLVER_BRIDGE / INTERNAL_IMPL） |
| I3 | done | `SolverBoundaryCasts.kt` 精简，`UNCHECKED_CAST` 降至 2 |
| I4 | done | DEPRECATE 策略，`QuadraticInequality` 已加 `@Deprecated(WARNING)` |
| I5 | done | 签名级扫描、`@Deprecated` 检测、boundary 三级分类、gate 增强 |
| J1 | done | MetaModel 12 个 Flt64 边界方法迁移至 adapter/flt64 扩展函数 |
| J2 | done | core 中 `QuadraticInequality` -> `QuadraticInequalityOf<Flt64>`，消除 adapter typealias 依赖 |
| J3 | done | I5 恢复条件明确，mechanism 白名单更新，adapter 归入 boundary |
| J4 | done | daily/JSON/README 同步 |
| K1 | done | I5 签名命中分类：core solver boundary + math Flt64 类型固有 |
| K2 | done | I5 whitelist 新增 math 模块和 TokenTable.kt，当前口径下非适配器命中降至 0 |
| K3 | done | I5 恢复为硬门禁，但硬门禁仍受粗白名单影响 |

## 3. 当前实测基线

最新本次复核时间：2026-05-09 13:40

### 3.1 扫描结果

执行：

```powershell
pwsh.exe -NoLogo -File scripts/scan-full-genericization.ps1
```

结果：

- `GATE: PASS`
- `public_api_blocking = 0`
- `i5_public_api_signature.total = 0`
- `i5_public_api_signature.non_adapter = 0`
- `boundary_allowed`：
  - `suppress_unchecked_cast = 2`
  - `typealias_flt64 = 1`
  - `core_function_override = 0`
  - `core_callback = 0`
  - `core_mechanism = 12`
- I5 boundary tiers：
  - `permanent = 14`
  - `deprecated = 1`
  - `must_decrease = 0`

注意：

- `core_mechanism = 12` 与上一版 daily 中记录的 `core_mechanism = 5` 不一致，应以后续重新收紧扫描后的结果为准。
- 本次只复核并运行扫描，未重新执行 Maven compile/test。

### 3.2 上一会话报告的构建与测试基线

上一会话报告如下，下一阶段开始前必须重新执行确认：

- `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile`：PASS
- `mvn -pl ospf-kotlin-core -am test`：PASS（core 145/145）
- `mvn -pl ospf-kotlin-math -am test`：PASS（math 711/711）

## 4. 未完成事项

### 4.1 扫描口径未达到设计验收要求

当前 I5 whitelist 包含整目录：

- `solver[/\\]`
- `model[/\\]mechanism`
- `model[/\\]callback`
- `model[/\\]intermediate`
- `model[/\\]basic`
- `variable[/\\]`
- `ospf-kotlin-math[/\\]`

这些规则会跳过大量公开签名。当前 `I5 non_adapter = 0` 不能证明主 API 已完全泛型化。

### 4.2 mechanism 主模型仍暴露 Flt64 特化

代表性问题：

- `MechanismModel<V>.objectFunction` 当前为 `Object`，类型被擦除。
- `SingleObjectMechanismModel<V>.objectFunction` 当前为 `SingleObject<*>`，不是 V-typed。
- `LinearMechanismModel<V>` 的 `objectFunction` 实际为 `SingleObject<LinearSubObject<Flt64>>`。
- `QuadraticMechanismModel<V>` 的 `objectFunction` 实际为 `SingleObject<QuadraticSubObject<Flt64>>`。
- `LinearMechanismModel` companion factory 仍接收 `LinearMetaModel<Flt64>` 并返回 `LinearMechanismModel<Flt64>`。
- `QuadraticMechanismModel` companion factory 仍接收 `QuadraticMetaModel<Flt64>` 并返回 `QuadraticMechanismModel<Flt64>`。

### 4.3 solver 公开入口仍以 Flt64 为主

代表性问题：

- `AbstractLinearSolver.invoke(...)` 返回 `Ret<FeasibleSolverOutput<Flt64>>`。
- `solve(model: LinearMechanismModel<Flt64>)` 仍是公开扩展入口。
- `dump(model: LinearMechanismModel<Flt64>)`、`dump(model: LinearMetaModel<Flt64>)` 仍是公开 API。
- solution pool、IIS、quadratic solver 入口仍存在 `List<List<Flt64>>`、`FeasibleSolverOutput<Flt64>` 等签名。

### 4.4 flatten/token/relation 边界仍是 Flt64

代表性问题：

- `LinearFlattenData<Flt64>`、`QuadraticFlattenData<Flt64>` 在 mechanism 主路径中大量出现。
- `Relation.flattenData` 仍是 `LinearFlattenData<Flt64>` / `QuadraticFlattenData<Flt64>`。
- `LinearConstraintInput` 的输入与求值路径仍围绕 `Flt64`。

### 4.5 Benders cut / IIS / dual API 仍暴露 Flt64

代表性问题：

- `fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>`
- `dualSolution: Map<Constraint<Flt64, *>, Flt64>`
- `dualSolutionById: Map<String, Flt64>`
- 返回 `List<LinearInequality<Flt64>>`

这些如果属于 solver backend 边界，应移动或隔离到 boundary/adapter；如果属于模型 API，应泛型化为 `V`。

### 4.6 math 模块粗白名单过宽

`ospf-kotlin-math[/\\]` 当前作为整体 whitelist。合理目标应拆分为：

- 允许：`math.algebra.number.Flt64` 类型本体。
- 允许：`math.symbol.adapter.flt64` 兼容层。
- 不允许：math 主抽象、symbol 主链、geometry/polynomial/inequality 主链新增 Flt64 特化公开 API。

## 5. L 阶段执行计划

### L0：收紧扫描并重建真实债务清单

目标：

- 删除或拆分 I5 中的粗白名单。
- 将 `model/mechanism`、`solver`、`ospf-kotlin-math` 从整目录 whitelist 改为具体文件/具体方法级 whitelist。
- 重新生成真实的 public Flt64 signature 清单。

详细步骤：

1. 修改 `scripts/scan-full-genericization.ps1`。
2. 保留 `adapter[/\\]flt64` 白名单。
3. 将 `TokenTable.kt`、`SolverBoundaryCasts.kt` 等确认为永久边界的文件逐项列入白名单。
4. 移除 `model[/\\]mechanism` 整目录白名单。
5. 移除 `solver[/\\]` 整目录白名单，拆成 solver backend 具体入口。
6. 移除 `ospf-kotlin-math[/\\]` 整模块白名单，拆成 Flt64 类型本体和 adapter。
7. 运行 scan，记录新增 blocking 数量，作为 L 阶段真实基线。

验收标准：

- 扫描能报出当前 `LinearMechanismModel<Flt64>`、`QuadraticMechanismModel<Flt64>`、solver `FeasibleSolverOutput<Flt64>` 等公开签名。
- 新的债务清单按模块和边界分类输出。
- daily 与 JSON 同步记录真实 blocking 数量。

### L1：泛型化 ObjectFunction / SubObject 类型链

目标：

- `MechanismModel<V>.objectFunction` 不再使用 `Object`。
- `SingleObjectMechanismModel<V>` 不再使用 `SingleObject<*>`。
- Linear/Quadratic object function 与 sub object 按 `V` 泛型表达。

详细步骤：

1. 梳理 `Object`、`SingleObject`、`MultiObject`、`LinearSubObject`、`QuadraticSubObject` 的泛型关系。
2. 引入或调整泛型接口，例如 `ObjectFunction<V>`、`SingleObject<V, SO>`。
3. 将 `LinearSubObject<Flt64>` 改为 `LinearSubObject<V>`，或引入主链泛型 sub object + Flt64 dump object。
4. 将 `QuadraticSubObject<Flt64>` 改为 `QuadraticSubObject<V>`，或按同一策略收口到 dump boundary。
5. 更新调用点和测试。

验收标准：

- `MechanismModel<V>` 的公开 `objectFunction` 是 V-typed。
- `LinearMechanismModel<V>` 不再公开 `SingleObject<LinearSubObject<Flt64>>`。
- `QuadraticMechanismModel<V>` 不再公开 `SingleObject<QuadraticSubObject<Flt64>>`。
- core compile/test 通过。

### L2：泛型化 mechanism model 工厂与构造路径

目标：

- `LinearMetaModel<V> -> LinearMechanismModel<V>`。
- `QuadraticMetaModel<V> -> QuadraticMechanismModel<V>`。
- Flt64 构造入口仅作为 adapter/boundary 或兼容 overload。

详细步骤：

1. 修改 `LinearMechanismModel.Companion.invoke` 签名。
2. 修改 `QuadraticMechanismModel.Companion.invoke` 签名。
3. 将 `fixedVariables`、`tokens`、`objectFunction`、`constraints` 等构造参数同步泛型化。
4. 将必要的 `V -> Flt64` 转换移动到 dump/solver boundary。
5. 兼容旧入口时，放到 `adapter/flt64` 或标记 `@Deprecated(WARNING)`。

验收标准：

- 用户可以从 `LinearMetaModel<V>` 直接构造 `LinearMechanismModel<V>`。
- 用户可以从 `QuadraticMetaModel<V>` 直接构造 `QuadraticMechanismModel<V>`。
- core model/mechanism 主包公开工厂不再硬编码 `Flt64`。

### L3：收口 flatten/token/relation Flt64 边界

目标：

- mechanism 主模型尽量持有 V-typed 数据。
- solver 所需 `Flt64` flatten 数据只在 dump/solver boundary 生成。

详细步骤：

1. 评估 `LinearFlattenData<V>` / `QuadraticFlattenData<V>` 是否可作为主链类型。
2. 若可行，将 relation、constraint input、meta constraint 的主数据泛型化。
3. 若短期不可行，将 `Flt64` flatten 数据封装到明确的 `DumpBoundary` / `SolverBoundary` 类型中。
4. 去除 mechanism 主 API 中直接暴露的 `LinearFlattenData<Flt64>`、`QuadraticFlattenData<Flt64>`。

验收标准：

- `Relation` 主接口不再公开 Flt64 flatten 数据。
- `LinearConstraintInput` 主入口不再公开 Flt64-only 参数。
- solver dump 前的模型对象保持 V-typed。

### L4：拆分 solver API

目标：

- 对用户公开的 solver API 是泛型 API。
- `Flt64` solver API 只作为 backend/boundary 或 deprecated compatibility API。

详细步骤：

1. 定义主入口：`solve<V>(model: MechanismModel<V>, converter: IntoValue<V>)` 或等价模式。
2. 将 `FeasibleSolverOutput<Flt64>` 公开返回改为 `FeasibleSolverOutput<V>`。
3. 将 `LinearMechanismModel<Flt64>` / `QuadraticMechanismModel<Flt64>` overload 移入 adapter 或标记 deprecated。
4. 将 backend 真实求解接口改为 internal/boundary 可见。
5. 更新 `solveAsync`、solution pool、IIS overload。

验收标准：

- core 正常用户入口不要求显式构造 `LinearMechanismModel<Flt64>`。
- 非 adapter solver 公开 API 不再新增 raw `Flt64` 签名。
- 旧 Flt64 overload 有清晰迁移路径或 deprecation 标记。

### L5：处理 Benders cut / IIS / dual API

目标：

- 明确 dual/farkas/cut API 的归属。
- 主模型 API 泛型化，solver dual 边界隔离。

详细步骤：

1. 分类所有 `generateOptimalCut`、`generateFeasibleCut`、IIS 相关 API。
2. 属于 solver 数值输出的，移动到 solver boundary 或 adapter。
3. 属于模型能力的，改为 `V` 泛型签名。
4. 对 `Map<String, Flt64>` convenience overload 加 deprecation 或迁移到 adapter。

验收标准：

- mechanism 主包公开 API 不再直接暴露 dual `Flt64` map。
- cut 返回值在主 API 中是 V-typed，或明确只存在于 boundary/adapter。

### L6：收紧 math 模块 whitelist

目标：

- math 主抽象保持泛型。
- Flt64 仅作为 number type body 与 adapter 层存在。

详细步骤：

1. 移除 `ospf-kotlin-math[/\\]` 整模块 whitelist。
2. 添加精确 whitelist：
   - `math/algebra/number/Flt64` 类型本体。
   - `math/symbol/adapter/flt64`。
3. 扫描 math symbol、geometry、polynomial、inequality 主链。
4. 将新增 Flt64 特化公开 API 迁移到 adapter 或泛型化。

验收标准：

- math 主链新增 Flt64 签名会触发门禁。
- Flt64 类型本体不会误报。
- `QuadraticInequality` 等兼容 typealias 仍有 deprecation 或迁移说明。

### L7：最终验收与文档同步

目标：

- 代码、扫描、测试、文档一致。

详细步骤：

1. 执行 compile：
   ```powershell
   mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile
   ```
2. 执行 core 测试：
   ```powershell
   mvn -pl ospf-kotlin-core -am test
   ```
3. 执行 math 测试：
   ```powershell
   mvn -pl ospf-kotlin-math -am test
   ```
4. 执行扫描：
   ```powershell
   pwsh.exe -NoLogo -File scripts/scan-full-genericization.ps1
   ```
5. 同步更新：
   - `ospf-kotlin-core/daily.md`
   - `scripts/scan-full-genericization-result.json`
   - README / README_ch 中与泛型化相关的说明（如有变化）

最终验收标准：

- `GATE: PASS`
- `public_api_blocking = 0`
- I5 非 adapter public Flt64 signature = 0，且不依赖粗目录 whitelist。
- `model/mechanism` 主 API 无 raw `Flt64` 公开签名。
- `solver` 用户主入口无 raw `Flt64` 公开签名。
- `math` 主抽象无 raw `Flt64` 公开签名。
- `UNCHECKED_CAST` 仍只保留在批准的 boundary 文件中。
- 所有保留的 `Flt64` 公开签名都有以下之一：
  - 位于 `adapter/flt64`。
  - 位于 Flt64 类型本体。
  - 位于明确的 solver backend/boundary。
  - 标记 `@Deprecated(WARNING)` 并写明迁移路径。

## 6. 下一个会话交接清单

1. 先执行 `git status --short`，确认当前工作树中已有未提交改动，避免误覆盖。
2. 先从 L0 开始，不要直接修业务代码。
3. 收紧 I5 whitelist 后，接受扫描暂时 FAIL，并把 FAIL 结果作为真实债务基线。
4. 优先处理 `MechanismModel.kt` 中 `objectFunction` 和 companion factory 的 `Flt64` 特化。
5. 每完成一个 L 子阶段，都要执行至少：
   - `mvn -pl ospf-kotlin-core -am compile`
   - `pwsh.exe -NoLogo -File scripts/scan-full-genericization.ps1`
6. 会话结束前更新 daily、JSON 和测试/扫描结果。

## 7. 本次更新记录（2026-05-09）

1. 复核 `daily.md` 中“泛型化主链工作基本完成”的结论，判定不成立。
2. 确认当前扫描 `GATE: PASS` 依赖粗白名单，不能代表完全泛型化达成。
3. 确认 `LinearMechanismModel`、`QuadraticMechanismModel`、solver 入口、Benders cut/IIS API 仍有公开 `Flt64` 特化。
4. 将后续工作重写为 L0-L7 执行计划，并补充详细步骤与验收标准。
