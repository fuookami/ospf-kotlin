# BPP3D 主流程重构交接

日期：2026-05-27

## 1. 已完成摘要

以下事项已完成主线闭环，本文不再保留逐日执行细节：

1. `ColumnGenerationAlgorithm` 已上移到 application 层，形成 application 编排入口与可插拔执行器结构。
2. `bpp3d-domain-layer-selection-context` 已从主线架构中移除，不再作为独立 layer-selection domain。
3. demand 建模已支持 item amount、material amount、material weight 及混合模式。
4. shadow price、reduced cost 与 layer statistics 已按 active demand entries 统一口径处理。
5. layer generation 已形成委托式接口与基础实现骨架，block/BLA/pattern/pile/circle packing 均已有主线接入点。
6. application CG 已覆盖初始列、LP、SP、加列、final IP/MIP、packing analyzer 的主流程。
7. Gurobi 标准执行器、求解指标透传、CSV 回归、随机回归与多 bin/mixed-demand 回归已建立。
8. `final bins -> PackingResult -> SchemaDTO` 的结果分析链路已存在。

## 2. 当前问题

`bpp3d-domain-packing-context` 的职责需要修正。

它不应只表示 final packing result 或 renderer adapter，也不应先实现临时贪心包装器。它应作为前置包装规划上下文：根据物料数量或重量需求，在可选包装方案中通过 MIP/set covering 选择包装方案，并生成包装后的 `Item`，供后续 layer generation 与 CG 主流程使用。

参考路径：

`E:\workspace\tcl\bpp3d\bpp3d-project-hztv\bpp3d-domain-hztv-packing-context\src\main\com\tcl\bpp3d\hztv\domain\packing_context`

## 3. 下一轮目标

下一轮只聚焦 packing-context 语义修正与 application 输入侧接入：

1. 在 `bpp3d-domain-packing-context` 中实现 MIP/set covering 包装规划。
2. 支持 material amount demand 与 material weight demand。
3. 从 `PackingProgram` 选择包装方案，生成包装后的 `ActualItem` 与数量。
4. 将包装后的 item demand 接入 application CG 输入侧。
5. 保留现有 final packing analyzer/renderer adapter 能力，但明确它不是 packing-context 的主职责。

不纳入本轮：

1. 圆柱真实几何与可变半径，继续归 `cylinder.md`。
2. three.js renderer 展示细节。
3. 真实业务大规模性能基线沉淀。

## 4. 建模要求

包装规划直接使用 MIP/set covering，不使用贪心作为主实现。

符号：

1. `M`：物料集合。
2. `P`：可选包装方案集合。
3. `d[m]`：物料 `m` 的需求数量。
4. `a[p,m]`：包装方案 `p` 对物料 `m` 的容量。
5. `x[p]`：选择包装方案 `p` 的包装件数量，非负整数。
6. `y[p,m]`：分配到包装方案 `p` 的物料 `m` 数量，非负整数。

重量需求先折算为数量：

```text
amount[m] = ceil(weightDemand[m] / unitWeight[m])
```

基础约束：

```text
forall m:
  sum_p y[p,m] = d[m]

forall p,m:
  0 <= y[p,m] <= a[p,m] * x[p]
```

建议目标：

```text
minimize
  package_count_weight * sum_p x[p]
+ volume_weight * sum_p volume[p] * x[p]
+ slack_weight * sum_p,m (a[p,m] * x[p] - y[p,m])
```

说明：

1. `x[p]` 决定选多少个包装件。
2. `y[p,m]` 决定实际装入多少物料。
3. slack 表示未使用包装容量，用于减少过度空包。
4. 这种模型比单纯 `sum_p a[p,m] * x[p] >= d[m]` 更适合本项目，因为后续需要生成实际包含物料的 `Package/ActualItem`。

## 5. 修改清单

### 5.1 packing-context 模型

新增或调整模型，建议命名如下：

1. `MaterialPackingDemand`
2. `MaterialPackingProgramCandidate`
3. `MaterialPackingAssignment`
4. `PackageSelection`
5. `PackagedItem`
6. `MaterialPackingPlan`
7. `MaterialPackingObjectiveConfig`

输出结构必须能表达：

1. 生成的 `Package` 列表。
2. 生成的 `ActualItem` 与数量。
3. 每个包装方案的选择数量。
4. 每个包装方案内物料分配量。
5. 未包装物料，正常可行解中应为空。
6. 求解信息，如 objective、gap、time、selected_package_count。

### 5.2 MIP 服务

新增服务，建议命名：

1. `MaterialPackingMipSolver`
2. 或 `MaterialPacker`，但其内部必须直接走 MIP/set covering。

职责：

1. 将 material amount / weight demand 标准化为数量 demand。
2. 从 `PackingProgram.materials` 构造容量矩阵 `a[p,m]`。
3. 构造 MIP 变量 `x[p]`、`y[p,m]`。
4. 建立 demand equality 与 capacity linking 约束。
5. 求解后生成 `Package`、`ActualItem`、`PackageSelection`。
6. 透传求解状态与指标。

### 5.3 solver 接入

优先复用项目现有 OSPF modeling/solver adapter，不在 domain 中硬编码 Gurobi API。

建议接口：

```kotlin
interface MaterialPackingSolverExecutor {
    suspend fun solve(request: MaterialPackingMipRequest): MaterialPackingMipResult
}
```

若现有模块依赖不允许 domain 直接依赖 solver，则采用两层结构：

1. domain 定义 request/result 与模型语义。
2. application 或 adapter 层提供具体 executor。

### 5.4 包装后 Item 生成

包装结果应生成下游可用的 `ActualItem`：

1. `ActualItem.pack` 指向生成的 `Package`。
2. `ActualItem.materialAmounts` 能反映包装件内实际物料数量。
3. `PackagedItem.amount` 表示相同包装 item 的数量。
4. 未满包必须标记 `pending=true`。
5. 后续 demand entries 应基于包装后 item 重新生成。

### 5.5 现有 Packer 边界

现有 `Packer` 处理的是：

```text
LayerBin -> PackingResult -> SchemaDTO
```

它应被视为 final packing analyzer 或 renderer adapter 辅助能力，不代表 packing-context 主职责。

下一轮可选择：

1. 保留类名但补 KDoc 明确职责。
2. 或重命名为 `FinalPackingAnalyzer`，并同步更新调用方与测试。

### 5.6 application 接入

在 application 输入侧增加包装规划步骤：

```text
material demand
-> MaterialPackingMipSolver
-> packaged ActualItem + amount
-> demand entries from packaged items
-> ColumnGenerationApplicationService
```

必须保持兼容：

1. 已经传入 item demands 的场景继续可用。
2. 只传 material amount/weight 的场景走包装规划。
3. 混合场景需要有清晰规则，避免同一需求被重复计算。

## 6. 执行步骤

1. 运行 `git status --short`，确认只修改 `ospf-kotlin-framework-bpp3d` 相关文件，不处理 `.rules/chore.md` 和 `ospf-kotlin-multiarray` 的既有脏改。
2. 检查 `bpp3d-domain-packing-context` 当前类与测试，确认 final analyzer 与前置 packing planner 的边界。
3. 新增 packing planner 领域模型。
4. 实现 MIP request/result 与 solver executor 抽象。
5. 实现 MIP/set covering 建模与结果还原。
6. 生成 `Package`、`ActualItem`、`PackagedItem`。
7. 补 domain 级单元测试。
8. 在 application 增加 material-only 输入到 packaged item 的接入测试。
9. 更新文档与命名边界。
10. 串行运行回归测试，避免 Maven 并发生成目录竞争。

## 7. 测试要求

packing-context 至少补以下测试：

1. 单物料数量需求：需求 7，方案容量 5/2，应选择 1 个 5 和 1 个 2。
2. 单物料重量需求：需求 5kg，单重 2kg，应折算为 3 件。
3. 多物料组合包装：组合方案比单物料方案更优时，应选择组合方案。
4. 未满包：需求 6，只有容量 5 的方案，应生成 2 个包装，其中一个 `pending=true`。
5. 不可行场景：无任何方案覆盖某个物料时，应返回明确不可行状态或 restMaterials。
6. objective tie-break：同覆盖数下优先更少包装件、更少空容量或更低体积成本。

application 至少补以下测试：

1. material amount only 输入可先包装成 item，再进入 CG 主流程。
2. material weight only 输入可先包装成 item，再进入 CG 主流程。
3. packing planner 输出的 material summary 与最终 packing analyzer summary 一致。

## 8. 验收标准

- [x] `daily.md` 不再作为交接文档使用。
- [x] `refactor.md` 是唯一主线交接入口。
- [x] `packing-context` 存在 MIP/set covering 包装规划主实现。
- [x] 包装规划支持 material amount demand。
- [x] 包装规划支持 material weight demand。
- [x] 包装规划输出包装后的 `ActualItem` 与数量。
- [x] 未满包可以通过 `pending=true` 表达。
- [x] 现有 `LayerBin -> PackingResult -> SchemaDTO` 链路不被破坏。
- [x] application 可从 material-only demand 进入包装规划，再进入 CG。
- [x] packing-context domain 测试通过。
- [x] application 默认测试通过。
- [x] 如本机具备 Gurobi，`gurobi-cg-test` 中相关回归通过。

## 9. 建议验证命令

默认回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am test '-Dgpg.skip=true'"
```

application 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

Gurobi 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

## 10. 注意事项

1. 不要恢复独立 `bpp3d-domain-layer-selection-context`。
2. 不要把圆柱真实几何混入本轮 packing-context 验收。
3. 不要把 greedy planner 作为主实现提交；如需要启发式，只能作为 MIP 不可用时的显式 fallback。
4. 不要在 domain 中直接绑定 Gurobi API，优先保留 solver executor 抽象。
5. 当前工作区可能有与本任务无关的 `.rules/chore.md` 和 `ospf-kotlin-multiarray` 改动，下一轮不要回滚或提交这些文件。

## 11. 本轮执行结果（2026-05-27）

已完成实现：

1. `bpp3d-domain-packing-context` 新增 `MaterialPackingDemand`、`MaterialPackingProgramCandidate`、`MaterialPackingAssignment`、`PackageSelection`、`PackagedItem`、`MaterialPackingPlan`、`MaterialPackingObjectiveConfig`。
2. 新增 `MaterialPackingSolverExecutor` 及 `MaterialPackingMipRequest/Result` 抽象，domain 未绑定 Gurobi API。
3. 新增 `ExhaustiveMaterialPackingSolverExecutor`（按 set-covering 语义求解 `x[p]`）与 `MaterialPacker`（需求标准化、容量建模、`Package/ActualItem` 还原、`pending` 标记）。
4. `Packer` 增加 KDoc，明确其定位为 final packing analyzer。
5. `bpp3d-application` 的 `ColumnGenerationApplicationService` 新增 material-only 输入接入：

```text
material demand -> MaterialPacker -> packaged item demands -> CG
```

6. 混合场景新增 `MaterialPackingMixedDemandPolicy`：`Reject` / `ReplaceItemDemands` / `Merge`，默认 `Reject`，避免重复计数。
7. `ColumnGenerationApplicationResponse` 增加 `materialPackingPlan` 透传，便于联调验收。

已完成测试：

1. `MaterialPackerTest`：覆盖数量需求、重量折算、组合优先、`pending`、不可行、tie-break。
2. `MaterialPackingApplicationIntegrationTest`：覆盖 material amount only、material weight only、planner summary 与 final packing analyzer summary 一致性。
3. 回归命令执行通过：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am test '-Dgpg.skip=true'"
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

4. Gurobi 回归执行通过（`GurobiColumnGenerationTest`：10 run，0 fail，0 error，1 skipped）：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

备注：

1. surefire 输出有 `Corrupted channel` 警告，但不影响本次测试结论（构建成功且用例无失败）。

## 12. 主线剩余工作

以下事项不阻塞 packing-context MIP 主实现验收，但仍属于 BPP3D 主线后续必须完成的业务验收工作。

### 12.1 真实业务大规模数据联调

现有 CSV、随机、Gurobi 中大规模回归已补齐，但仍需要接入真实业务外部数据集跑出稳定基线。

下一轮应使用以下入口接真实 CSV：

1. `bpp3d.gurobi.dataset.suite.paths`
2. `bpp3d.gurobi.dataset.suite.dir`

需要沉淀的结果：

1. 总耗时与单 case 耗时。
2. LP/MILP gap。
3. selected bin count。
4. selected layer count。
5. schema 与 packing aggregation 一致性。

### 12.2 Gurobi 性能基线沉淀

当前已有 `elapsed`、`lpInfos`、`finalInfo`，也已有阈值断言入口。后续需要用真实数据确认阈值是否合理。

需要校准的参数：

1. `max.elapsed.seconds`
2. `max.total.elapsed.seconds`
3. gap 阈值。
4. thread 数。
5. time limit。
6. not-improvement time。

### 12.3 final IP/MIP analyzer 到 packing 的真实场景验收

测试侧已覆盖多物料、多 bin、大样本、schema、CSV、随机场景，但仍需要真实业务数据确认：

1. `final bins -> packing -> schema` 输出符合业务口径。
2. 物料汇总与需求口径一致。
3. 装载计划数量符合选中 bin/layer。
4. 空 bin 与选中 bin 的处理符合业务预期。
5. renderer DTO KPI 能被前端或下游服务稳定消费。

### 12.4 CirclePackingLayerGenerator 的 cylinder 路线

当前 `CirclePackingLayerGenerator` 只有基础矩形/六角圆密排策略，不等同于 `cylinder.md` 中的真实圆柱几何。

仍归 `cylinder.md` 专题推进的事项：

1. 可变半径。
2. 真实圆形碰撞与密排。
3. 圆柱姿态与边界几何。
4. 重量价值函数。

这些事项不算当前 BPP3D 主线 packing-context 验收项。

### 12.5 文档整理

`refactor.md` 已压缩为“已完成摘要 + 下一轮目标 + 本轮执行结果 + 主线剩余工作”，不再保留追加式逐日流水。

后续维护规则：

1. 新增完成项只写摘要，不写长流水。
2. 新增未完成项写入本章节或对应专题文档。
3. 圆柱专题只在 `cylinder.md` 展开，`refactor.md` 只保留边界说明。
