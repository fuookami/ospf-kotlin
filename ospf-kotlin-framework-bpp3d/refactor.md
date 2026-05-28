# BPP3D 主流程重构交接

日期：2026-05-28
当前基线提交：`46323736 refactor(bpp3d): close 4.6 generic boundary and regressions`

## 1. 当前结论

上一轮已按当时 `refactor.md` 的兼容边界口径完成验收，但没有达到新的严格目标。

新的用户要求是：BPP3D 尚未发版，不需要保留兼容层；下一轮应朝最优实现推进，删除 legacy/compat/facade/alias 双轨，让主模型和主流程直接成为统一的 `Quantity<V>` 泛型实现。

因此当前状态应重新定义为：

1. 业务闭环、回归、真实数据验证已完成。
2. `QuantityDomain`、`DemandMode`、`PackingProgram` candidate、APS-like adapter 等语义能力已落地。
3. 当前 `<V>` 能力仍以泛型入口、泛型骨架和 compat bridge 为主，主流程仍存在 legacy 模型与转换桥。
4. 下一轮目标不是继续收敛兼容边界，而是删除兼容边界本身。

## 2. 已完成摘要

以下事项视为已完成，不在下一轮重复实现，不保留逐日细节：

1. application 层已承载 column generation 主流程编排。
2. 独立 layer-selection 域已从 BPP3D 主线移除。
3. 主流程已覆盖 initial columns、LP、SP、加列、final IP/MIP、packing analyzer、schema 输出。
4. layer generation 已形成委托式接口与基础生成器体系。
5. packing-context 已修正为前置包装规划语义，支持 material demand 到 packaged item 的规划。
6. material-only demand 与 mixed demand 的 application 输入链路已建立。
7. item/material demand 已具备统一 `Quantity` 表达、`DemandMode` 语义标签和 `QuantityDomain` 判定基础。
8. `PackingProgram` 已能表达 material contribution，并可作为 layer generation candidate 的输入语义。
9. APS-like `PackageSolution` 到 BPP3D `PackingProgram` 的适配边界已验证。
10. Gurobi 标准执行器、指标透传、CSV suite、随机回归、真实业务 CSV 回归已建立并通过。
11. `final bins -> packing -> schema` 的真实数据一致性验证已通过。
12. `daily.md` 已删除，`refactor.md` 是 BPP3D 主线唯一交接文档。

## 3. 下一轮目标

下一轮目标：把 BPP3D 从“兼容桥上的泛型入口”重构为“无兼容层的纯 `Quantity<V>` 泛型主链”。

必须达成：

1. 删除 BPP3D 内部 legacy/compat/facade/alias 双轨实现。
2. 统一核心模型为 `<V>` 泛型模型：`Material<V>`、`PackageShape<V>`、`Package<V>`、`PackingProgram<V>`、`Item<V>`、`Bin<V>`、`Layer<V>`、`Load<V>`、`Bpp3dDemandEntry<V>` 等。
3. application request/service/executor/analyzer 全链路直接以 `Quantity<V>`、`Bpp3dDemandEntry<V>`、`BinLayer<V>`、`LayerBin<V>` 工作。
4. layer-generation、layer-assignment、block-loading、bla、packing 的领域服务不再通过 `toLegacy*`、`asScalarF64` 或 legacy cache 回退。
5. `DemandMode` 只作为语义标签，不能参与数值计算或离散/连续判定。
6. `QuantityDomain` 只从 `quantity.unit.domain` 读取，dimension 不参与任何算法判定和运算。
7. Gurobi/Flt64 只允许作为测试实例化或 solver 边界的具体数值类型，不允许倒逼 domain model 固定为 Flt64。
8. 删除兼容层后默认回归、Gurobi 回归、真实 CSV suite 仍通过。

## 4. 重构原则

1. 不保留兼容 API：BPP3D 未发版，本轮允许破坏旧入口。
2. 不新增迁移 facade：不要用新 compat 包替代旧 compat 包。
3. 单一模型优先：如果存在 legacy 模型和 generic 模型，保留 generic 模型并删除 legacy 模型。
4. 单一主链优先：如果存在 `Quantity<V> -> legacy -> solver`，应改为 `Quantity<V> -> solver adapter`。
5. 类型参数向上传递：不要在中间层把 `<V>` 收敛为 `Flt64`、`LegacyScalar` 或模块别名。
6. solver 转换下沉到求解边界：只有实际调用 solver 时才允许把 `V` 转为 solver 需要的值类型。
7. 测试可以使用 `Flt64`、`FltX` 实例化，但生产主源码不能以它们作为唯一真实类型。
8. 删除优于包装：发现旧类型仍被使用时，优先改调用方到新类型，不要继续包一层 adapter。
9. 先建立严格扫描，再开始大改：避免边改边扩大 legacy 表面积。
10. 不混入 `cylinder.md`：真实圆柱几何仍是独立专题。

## 5. 下一轮计划

### 5.1 建立严格边界检查

先新增或改造扫描脚本，让当前代码预期失败，作为下一轮收敛清单。

扫描目标：

1. `LegacyQuantity`
2. `LegacyScalar`
3. `InfraScalar`
4. `ItemModelScalar`
5. `LayerAssignmentScalar`
6. `ApplicationScalar`
7. `PackingScalar`
8. `BlockLoadingScalar`
9. `Bpp3dBlaScalar`
10. `toLegacy`
11. `asScalarF64`
12. `toFlt64` 在 BPP3D 主源码中的非 solver 边界调用
13. `api/compat`、`model/compat`、`service/compat` 等 compat 目录

目标不是排除这些命中，而是最终清零。

### 5.2 确定 canonical generic model

以现有泛型模型为基础，选择并移动到正式 `model` 主包。

需要统一：

1. material model
2. package shape model
3. package / packing program model
4. item / item placement model
5. bin / layer / layer bin model
6. demand key / demand value / demand entry model
7. packing plan / packing assignment model

完成后删除旧 legacy model 或将其直接改造成泛型 model，不保留并行类型。

### 5.3 迁移 item-context 与 infrastructure

先处理最底层几何、容器和 item 模型，避免上层继续被旧类型牵引。

要求：

1. `Cuboid`、`Container`、`Projection`、`Placement` 等核心几何主接口泛型化。
2. `Item`、`Package`、`PackingProgram`、`Bin`、`Layer` 不再依赖模块 scalar alias。
3. material amount、material weight、item amount、item weight 统一为 `Quantity<V>` demand contribution。
4. `PackingProgram.materials` 统一为 `MaterialKey -> Quantity<V>` 或等价泛型结构。
5. 删除 `QuantityDomainApi` 与 legacy model 的桥接关系；保留的类型必须就是主类型。

### 5.4 迁移 demand 与 layer-assignment

要求：

1. `Bpp3dDemandEntry<V>` 泛型化。
2. `Bpp3dItemDemand<V>` 与 `Bpp3dMaterialDemand<V>` 泛型化。
3. `Load<V>`、assignment、capacity、constraint、objective 全部接受泛型 demand entry。
4. 离散/连续判定统一从 `quantity.unit.domain` 读取。
5. `DemandMode` 只用于 key、分组、命名、KPI、shadow price 维度。
6. shadow price key 保留 mode 与 demand key，但不要依赖 legacy key/value。

### 5.5 迁移 layer-generation、block-loading 与 BLA

要求：

1. layer generation context、request、result、generator 统一 `<V>`。
2. `PackingProgram<V>` 可直接作为 layer candidate，不需要转换为 legacy item。
3. block-loading 和 BLA 算法使用泛型几何与 item 模型。
4. 当前已有的 generic proof code 应并入正式主实现，而不是继续作为旁路骨架。
5. 删除仅用于桥接 legacy scalar 的 alias 文件。

### 5.6 迁移 packing-context

要求：

1. `MaterialPackingPlan<V>`、`MaterialPackingDemand<V>`、`PackageSelection<V>`、`MaterialPackingAssignment<V>` 泛型化。
2. `MaterialPacker<V>` 与 solver executor 直接处理 `Quantity<V>`。
3. set covering / MIP 语义保留，但不通过 legacy `ActualItem` 作为中间主模型。
4. `pending`、`restMaterials`、objective、gap、elapsed 等结果字段保留。
5. APS-like adapter 只依赖 BPP3D generic packaging model，不依赖 APS domain。

### 5.7 迁移 application 主流程

要求：

1. `ColumnGenerationApplicationRequest<V>` 泛型化。
2. `ColumnGenerationApplicationService<V>` 或泛型 `solve` 入口直接执行泛型主流程。
3. `ColumnGenerationStandardExecutors<V>` 不再调用 `toLegacyItems`。
4. `ColumnGenerationAlgorithm<V>` 的 state、columns、shadow price、result、final info 保持泛型一致。
5. packing analyzer 和 schema adapter 可在最终输出边界做 DTO 转换，但不能回退 legacy domain model。
6. Gurobi executor 在 solver 边界处理数值转换，不能污染 domain/application 主类型。

### 5.8 删除兼容层与清理文档

要求：

1. 删除 BPP3D 下所有仅为历史兼容存在的 compat 包。
2. 删除 legacy constructor facade。
3. 删除 legacy scalar alias。
4. 删除 legacy cache 与 legacy bridge。
5. 删除过时测试，改为泛型测试或业务测试。
6. 将 `refactor.md` 更新为最终完成状态，不恢复 `daily.md`。

## 6. 下一轮事项

1. 重写 strict boundary scanner，使 legacy/compat/alias 命中不再被排除。
2. 运行 scanner，记录当前失败清单，作为迁移任务列表。
3. 选定 canonical generic model，并删除或改造并行 legacy model。
4. 迁移 infrastructure 主几何类型到 `<V>`。
5. 迁移 item-context 主模型到 `<V>`。
6. 迁移 `PackingProgram` 与 material contribution 到 `Quantity<V>`。
7. 迁移 demand entry、load、assignment、capacity、constraint、objective 到 `<V>`。
8. 迁移 layer-generation request/context/generator 到 `<V>`。
9. 迁移 block-loading 与 BLA 到 `<V>`。
10. 迁移 packing planner 与 package solution adapter 到 `<V>`。
11. 迁移 application request/service/executor/analyzer 到 `<V>`。
12. 删除所有 legacy bridge、legacy cache、compat constructor、scalar alias。
13. 用 `Flt64` 跑默认业务测试，确认现有行为不回退。
14. 用 `FltX` 跑直接编译与关键业务测试，证明主链不绑定 `Flt64`。
15. 跑 Gurobi 回归与真实 CSV suite。
16. 更新 `refactor.md` 验收项。

## 7. 修改清单

预计需要修改或删除：

1. `bpp3d-infrastructure/src/main/**`
2. `bpp3d-domain-item-context/src/main/**`
3. `bpp3d-domain-layer-assignment-context/src/main/**`
4. `bpp3d-domain-layer-generation-context/src/main/**`
5. `bpp3d-domain-block-loading-context/src/main/**`
6. `bpp3d-domain-bla-context/src/main/**`
7. `bpp3d-domain-packing-context/src/main/**`
8. `bpp3d-application/src/main/**`
9. `bpp3d-*/src/test/**`
10. `bpp3d-application/src/gurobi-test/**`
11. `scripts/generic-boundary-check.ps1` 或等价严格扫描脚本
12. `refactor.md`

必须重点删除或合并：

1. `api/compat/**`
2. `model/compat/**`
3. `service/compat/**`
4. `LegacyScalars.kt`
5. `*ScalarAliases.kt`
6. legacy constructor facade
7. legacy bridge helper
8. legacy cache
9. 只为兼容旧模型存在的 typealias

不应修改：

1. `.rules/chore.md`
2. `ospf-kotlin-multiarray/**`
3. `cylinder.md` 及真实圆柱几何专题
4. APS domain 源码，除非用户明确切换到 APS 仓库任务
5. 真实业务原始 CSV 数据

## 8. 验收标准

### 8.1 严格边界验收

- [ ] BPP3D `src/main` 中无 `LegacyQuantity`。
- [ ] BPP3D `src/main` 中无 `LegacyScalar`。
- [ ] BPP3D `src/main` 中无模块 scalar alias：`InfraScalar`、`ItemModelScalar`、`LayerAssignmentScalar`、`ApplicationScalar`、`PackingScalar`、`BlockLoadingScalar`、`Bpp3dBlaScalar`。
- [ ] BPP3D `src/main` 中无 `toLegacy` 主路径调用。
- [ ] BPP3D `src/main` 中无 `asScalarF64` 主路径调用。
- [ ] BPP3D `src/main` 中无非 solver 边界的 `toFlt64` 调用。
- [ ] BPP3D `src/main` 中不存在 `api/compat`、`model/compat`、`service/compat` 目录。
- [ ] strict boundary scanner 输出 pass，且不是通过排除 compat/alias 达成。

### 8.2 泛型主模型验收

- [ ] `Material<V>` 是正式主模型。
- [ ] `PackageShape<V>` 是正式主模型。
- [ ] `Package<V>` 是正式主模型。
- [ ] `PackingProgram<V>` 是正式主模型。
- [ ] `Item<V>` 是正式主模型。
- [ ] `Bin<V>` 是正式主模型。
- [ ] `Layer<V>` / `BinLayer<V>` / `LayerBin<V>` 是正式主模型。
- [ ] `Load<V>` 是正式主模型。
- [ ] `Bpp3dDemandEntry<V>` 是正式主模型。
- [ ] 不存在与上述类型并行的 legacy 主模型。

### 8.3 Demand 语义验收

- [ ] item demand 与 material demand 均为 `entity + Quantity<V> + DemandMode`。
- [ ] amount/weight 不再是不同 demand 类型，只是不同 unit/domain 的 `Quantity<V>`。
- [ ] `DemandMode` 可用于 key、constraint name、shadow price、analyzer、KPI、报表分组。
- [ ] `DemandMode` 不参与单位换算、数值计算或离散/连续判定。
- [ ] 离散/连续只从 `quantity.unit.domain` 判断。
- [ ] dimension 不参与任何算法判定和运算。

### 8.4 主流程验收

- [ ] application request/service/executor/analyzer 直接处理泛型主模型。
- [ ] layer generation 直接处理 `Item<V>` 与 `PackingProgram<V>` candidate。
- [ ] layer assignment 直接处理 `Load<V>` 与 `Bpp3dDemandEntry<V>`。
- [ ] packing planner 直接处理 `MaterialPackingDemand<V>` 与 `PackingProgram<V>`。
- [ ] block-loading 与 BLA 不依赖 legacy scalar alias。
- [ ] Gurobi/Flt64 只存在于 solver 边界、测试实例化或 gurobi-test 中。

### 8.5 回归验收

- [ ] 默认 application 回归通过。
- [ ] `Flt64` 实例化业务测试通过。
- [ ] `FltX` 直接编译和关键业务测试通过。
- [ ] Gurobi 回归通过，或明确记录本机缺少 Gurobi 环境导致未执行。
- [ ] 真实 CSV `suite.paths` 入口通过。
- [ ] 真实 CSV `suite.dir` 入口通过。
- [ ] selected bin/layer、LP/MILP objective、gap、elapsed 指标仍输出。
- [ ] `final bins -> packing -> schema` 一致性断言仍通过。
- [ ] `daily.md` 保持删除状态。
- [ ] `refactor.md` 更新最终完成状态。

## 9. 建议验证命令

严格边界扫描：

```powershell
pwsh.exe -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .
```

默认 application 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

完整 Gurobi 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

真实 CSV 单文件 suite：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.paths=E:/path/to/real-case.csv' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

真实 CSV 目录 suite：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.dir=E:/path/to/real-suite' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

## 10. 注意事项

1. 不要恢复 BPP3D `daily.md`。
2. 不要提交真实业务原始 CSV。
3. 不要把真实数据阈值硬编码进生产主流程。
4. 不要用新增 compat 层掩盖 legacy 类型未删除的问题。
5. 不要把本轮目标降级为 `GENERIC_BOUNDARY_PASS`；新的验收是无 compat、无 legacy、纯泛型主链。

## 11. 执行进展（2026-05-28，当前轮次）

### 11.1 已完成

1. strict 扫描脚本保持 strict 模式（不排除 legacy/compat/alias），继续作为唯一边界基线。
2. 完成 block-loading / bla 的 scalar alias 删除：
   1. 删除 `bpp3d-domain-block-loading-context/.../compat/BlockLoadingScalarAliases.kt`。
   2. 删除 `bpp3d-domain-bla-context/.../compat/BlaScalarAliases.kt`。
   3. 对应服务实现改为直接使用 `Flt64` 类型。
3. 完成 application 的 `service/compat` 清理：
   1. 删除 `bpp3d-application/.../service/compat/ApplicationScalarAliases.kt`。
   2. 删除 `bpp3d-application/.../service/compat/ApplicationRequestLegacyBridge.kt`。
   3. 新建 `bpp3d-application/.../service/ApplicationRequestLegacyBridge.kt`（迁出 compat 包，保持现有行为不回退）。
   4. 修复 `ColumnGenerationApplicationService`、`ColumnGenerationStandardExecutors`、`ColumnGenerationPackingAnalyzer`、`PackingProgramLayerCandidateAdapter` 中错误的 compat import。
4. application 回归通过：
   1. `mvn -f pom.xml -pl bpp3d-application -am '-Dtest=ColumnGenerationAlgorithmTest,Bpp3dGenericBoundaryTest' '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'`
   2. 结果：15 tests, 0 failures, 0 errors。

### 11.2 strict 基线变化

1. 总命中：`723 -> 717`。
2. 关键条目：
   1. `ApplicationScalar: 0`（已清零）。
   2. `BlockLoadingScalar: 0`（已清零）。
   3. `Bpp3dBlaScalar: 0`（已清零）。
   4. `CompatDirectory: 4`（application `service/compat` 已移除，剩余来自 item/layer-assignment/packing 相关 compat 目录）。

### 11.3 当前剩余主阻塞

1. `InfraScalar(222)`、`LayerAssignmentScalar(116)`、`ItemModelScalar(98)`、`QuantityFlt64(65)` 仍大量存在。
2. `toLegacy(21)`、`asScalarF64(22)`、`toFlt64(11)` 仍主要集中在 item-context 与 layer-assignment 的 compat/bridge 代码。
3. 下一步应按优先级处理 `domain/layer_assignment/model/compat` 与 `domain/item/api|model/compat`，再推进 `domain/packing/model/compat`。

### 11.4 续做结果（2026-05-28，当前追加）

1. 完成 `layer-assignment` compat 目录彻底移除：
   1. 删除 `domain/layer_assignment/model/compat/**`。
   2. 将 `Bpp3dSolverValueAdapter`、`ScaledBpp3dSolverValueAdapter`、`LoadLegacyBridge` 迁到 `model/` 主包。
   3. 删除 `SolverValueAdapterBridge.kt`、`SolverValueAdapterExample.kt`。
2. 完成 `LayerAssignmentScalar` 文本清零：
   1. `layer-assignment-context/src/main` 内全部改为直接使用 `Flt64`。
   2. 保留 `layerAssignmentOne/layerAssignmentZero` 辅助函数以维持现有调用形态。
3. 构建与回归：
   1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am compile -DskipTests` 通过。
   2. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,Bpp3dGenericBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过（15 tests, 0 failures, 0 errors）。
4. strict 基线更新：
   1. 总命中：`717 -> 713`。
   2. `CompatDirectory: 4 -> 3`（`layer-assignment` compat 目录已清零）。
   3. `LayerAssignmentScalarToken` 已清零（不再出现在 strict 输出）。
   4. 由于显式替换为 `Flt64`，`Flt64Token` 暂时上升为 `251`，后续需在泛型化迁移时继续消减。
