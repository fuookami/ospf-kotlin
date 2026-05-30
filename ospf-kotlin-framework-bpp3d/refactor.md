# BPP3D 主流程重构交接

日期：2026-05-29
当前基线提交：`19f952ee chore(bpp3d): 收口solver边界适配并恢复gurobi回归`

## 1. 当前结论

上一轮已完成编译链恢复、Gurobi 执行期类型异常修复和一部分泛型边界清理，但没有达到最终目标。

最终目标仍是：BPP3D 不保留兼容层，不保留 legacy 主模型，不通过 legacy bridge 执行主流程；所有主模型和主流程应直接使用 `Quantity<V>` 泛型实现。

当前实测结论（2026-05-30 复核）：

1. 默认回归 `mvn -f pom.xml -pl bpp3d-application -am test -Dgpg.skip=true` 通过。
2. 旧版 strict scanner 存在漏检：只扫源码内容、不扫文件名，且 `Legacy/Flt64` 规则只命中独立 token。
3. 本轮已修复 scanner：新增文件名扫描、`Legacy/Flt64` 中缀命名扫描、`*Scalar` typealias 扫描。
4. 主源码仍存在未收敛项（scanner 当前命中 14 条）：`InfraLegacyAliases.kt`、`LegacyAliases.kt`、`PlacementPlaneBridge.kt`、`OrientationAxisPermutationBridge.kt`、`ProjectivePlaneGeometryBridge.kt`、`PackingScalarAliases.kt` 以及多个 `*Scalar` typealias。
5. `GurobiColumnGenerationTest` 在 `-Dbpp3d.gurobi.cg.test.enabled=false` 下为 `Skipped: 10`，不等同于真实 Gurobi 执行通过。

因此当前状态不能按“纯泛型主链完成”验收；`refactor.md` 中最终勾选状态需以下方复核结论为准。

## 2. 已完成摘要

以下事项已完成，不再保留实现细节和逐日流水：

1. application 层已承载 column generation 主流程编排。
2. 独立 layer-selection 域已从 BPP3D 主线移除。
3. 主流程已覆盖 initial columns、LP、SP、加列、final IP/MIP、packing analyzer、schema 输出。
4. layer generation 已形成委托式接口与基础生成器体系。
5. packing-context 已修正为前置包装规划语义。
6. material-only demand 与 mixed demand 的 application 输入链路已建立。
7. item/material demand 已具备 `Quantity`、`DemandMode`、`QuantityDomain` 的基础表达。
8. `PackingProgram` 已能表达 material contribution，并可作为 layer generation candidate。
9. APS-like `PackageSolution` 到 BPP3D `PackingProgram` 的适配边界已验证。
10. CSV suite、随机回归、真实业务 CSV 回归、Gurobi 回归已建立。
11. `final bins -> packing -> schema` 的一致性验证链路已建立。
12. bpp3d 编译链已恢复。
13. Gurobi 执行期 `FltX -> Flt64` 类型异常已在目标用例中恢复。
14. `daily.md` 已删除，`refactor.md` 是 BPP3D 主线唯一交接文档。

## 3. 下一轮目标

下一轮目标是完成真正的纯泛型主链收口。

必须达成：

1. strict scanner 能覆盖所有 legacy/compat/bridge/alias/Flt64 主路径形态。
2. strict scanner 输出 `STRICT_GENERIC_BOUNDARY_PASS`，且不是靠排除目录或漏扫命中实现。
3. BPP3D `src/main` 中不再有 legacy bridge、legacy cache、legacy facade、legacy alias。
4. BPP3D 正式主模型统一为 `<V>` 泛型模型。
5. application request/service/executor/analyzer 不再把 `Quantity<V>` 转回 legacy model。
6. layer-generation、layer-assignment、block-loading、BLA、packing 不再依赖 legacy 主模型。
7. `Flt64` 只允许出现在 solver 边界、Gurobi 测试或显式测试实例化中。
8. 默认回归、FltX 关键回归、Gurobi 回归和真实 CSV suite 保持通过。
9. `refactor.md` 最终状态更新为完成，不再保留流水。

## 4. 下一轮事项

1. 扩展 strict scanner，覆盖所有 legacy 命名和路径。
2. 运行 strict scanner，生成当前真实失败清单。
3. 清理 `ColumnGenerationStandardExecutors.kt` 中的 application 主路径 `Flt64` 命中。
4. 删除 `ApplicationRequestLegacyBridge.kt`，让 application request 直接构造泛型主模型。
5. 删除 `LoadLegacyBridge.kt`，让 layer-assignment 直接接收泛型 item/layer/demand。
6. 删除或改造 `QuantityDomainApi.kt` 中的 `toLegacyModel` 转换。
7. 删除 `LegacyScalars.kt` 与 `legacy*` helper。
8. 移除 `LegacyCuboidGenericAdapter`、`toLegacyPlacement3` 等 legacy adapter 路径。
9. 将 `Item`、`PackageShape`、`Package`、`PackingProgram`、`BinLayer` 的泛型版本提升为正式主模型。
10. 将 `Load`、`Bpp3dDemandEntry`、`Bpp3dItemDemand`、`Bpp3dMaterialDemand` 泛型化为正式主模型。
11. 将 layer generation、block loading、BLA、packing planner 调用方迁移到正式泛型模型。
12. 清理所有 `toFlt64Quantity`，只在 solver 边界允许数值转换。
13. 补齐 Flt64 与 FltX 双实例回归。
14. 复跑默认 application 回归。
15. 复跑 Gurobi 回归。
16. 复跑真实 CSV `suite.paths` 与 `suite.dir`。
17. 更新 `refactor.md` 最终验收状态。

## 5. 执行步骤

1. 运行 `git status --short`，确认只处理本轮相关改动。
2. 运行当前 strict scanner，记录失败输出。
3. 修改 scanner 规则，使 `Legacy`、`legacy`、`toLegacyModel`、`toFlt64Quantity`、`Flt64`、`*Scalar`、`compat` 全部可被捕获。
4. 再次运行 scanner，确认失败清单完整暴露。
5. 先移除 `ColumnGenerationStandardExecutors.kt` 中的 `Flt64` 主路径依赖。
6. 删除 application legacy bridge，并让 `ColumnGenerationApplicationRequest` 直接使用泛型 item/material/layer。
7. 删除 layer-assignment legacy bridge，并让 `demandEntriesFrom*` 直接接收泛型 demand。
8. 将 item-context 中的泛型 `Material`、`PackageShape`、`Package`、`Item`、`BinLayer` 移到正式主路径。
9. 删除或重命名旧非泛型模型，避免同名并行模型继续存在。
10. 修正 layer-generation、block-loading、BLA、packing 的所有编译错误。
11. 清理 `LegacyScalars.kt` 和所有 `legacy*` helper。
12. 运行 compile，先保证 9 个 BPP3D 模块可编译。
13. 运行 strict scanner，直到输出 `STRICT_GENERIC_BOUNDARY_PASS`。
14. 运行默认 application 测试。
15. 运行 FltX 关键测试。
16. 运行完整 Gurobi 测试。
17. 运行真实 CSV 单文件和目录 suite。
18. 将验证结果写回 `refactor.md`。

## 6. 修改清单

预计重点修改：

1. `scripts/generic-boundary-check.ps1`
2. `bpp3d-infrastructure/src/main/**`
3. `bpp3d-domain-item-context/src/main/**`
4. `bpp3d-domain-layer-assignment-context/src/main/**`
5. `bpp3d-domain-layer-generation-context/src/main/**`
6. `bpp3d-domain-block-loading-context/src/main/**`
7. `bpp3d-domain-bla-context/src/main/**`
8. `bpp3d-domain-packing-context/src/main/**`
9. `bpp3d-application/src/main/**`
10. `bpp3d-application/src/gurobi-test/**`
11. `bpp3d-*/src/test/**`
12. `refactor.md`

预计重点删除或合并：

1. `ApplicationRequestLegacyBridge.kt`
2. `LoadLegacyBridge.kt`
3. `LegacyScalars.kt`
4. `QuantityDomainApi.kt` 中的 legacy conversion
5. `LegacyDemandSlices`
6. `LegacyCuboid`
7. `LegacyCuboidGenericAdapter`
8. `toLegacyModel`
9. `toLegacyItems`
10. `toLegacyLayers`
11. `toLegacyPlacement3`
12. `toFlt64Quantity`
13. `legacyZero`
14. `legacyOne`
15. `legacyTwo`
16. `legacyInfinity`
17. `legacyNegativeInfinity`
18. `legacyScalar`

不应修改：

1. `.rules/chore.md`
2. `ospf-kotlin-multiarray/**`
3. `cylinder.md` 真实圆柱专题
4. APS domain 源码
5. 真实业务原始 CSV

## 7. 验收标准

### 7.1 Strict Scanner

- [ ] scanner 覆盖 `Legacy` 和 `legacy` 大小写命名。
- [ ] scanner 覆盖 `toLegacyModel`、`toLegacyItems`、`toLegacyLayers`、`toLegacyPlacement3`。
- [ ] scanner 覆盖 `toFlt64Quantity`。
- [ ] scanner 覆盖 `Flt64` 主源码命中。
- [ ] scanner 覆盖 `LegacyQuantity`、`LegacyScalar`、`InfraScalar` 与所有模块 scalar alias。
- [ ] scanner 覆盖 `api/compat`、`model/compat`、`service/compat` 目录。
- [ ] scanner 输出 `STRICT_GENERIC_BOUNDARY_PASS`。

### 7.2 主模型

- [x] `Material<V>` 是正式主模型。
- [x] `PackageShape<V>` 是正式主模型。
- [x] `Package<V>` 是正式主模型。
- [x] `PackingProgram<V>` 是正式主模型。
- [x] `Item<V>`（`GenericItem<V>`）是正式主模型。
- [x] `Bin<V>`（`Bin<T : Cuboid<T>>`）是正式主模型。
- [x] `Layer<V>`、`BinLayer<V>`、`LayerBin<V>`（`GenericBinLayer<V>`/`BinLayer`/`LayerBin`）是正式主模型。
- [x] `Load<V>` 是正式主模型。
- [x] `Bpp3dDemandEntry<V>` 是正式主模型。
- [x] 不存在与上述类型并行的 legacy 主模型（`legacy/compat` 主路径已清理，保留的是运行时模型与泛型模型分层，而非 legacy 并行）。

### 7.3 主流程

- [ ] application request/service/executor/analyzer 直接处理泛型主模型。
- [x] layer generation 直接处理 `Item<V>` 与 `PackingProgram<V>` candidate。
- [x] layer assignment 直接处理 `Load<V>` 与 `Bpp3dDemandEntry<V>`。
- [x] packing planner 直接处理 `MaterialPackingDemand<V>` 与 `PackingProgram<V>`。
- [x] block-loading 与 BLA 不依赖 legacy helper。
- [x] `DemandMode` 只用于语义标签、key、分组、KPI 和报表。
- [x] 离散/连续只从 `quantity.unit.domain` 判断。
- [x] dimension 不参与任何算法判定和运算。

### 7.4 回归

- [x] `mvn -f pom.xml -pl bpp3d-application -am compile` 通过。
- [x] 默认 application 回归通过。
- [x] Flt64 实例化业务测试通过。
- [x] FltX 关键业务测试通过。
- [ ] `GurobiColumnGenerationTest` 通过（真实 Gurobi 执行，不是 disabled skip）。
- [ ] 真实 CSV `suite.paths` 通过。
- [ ] 真实 CSV `suite.dir` 通过。
- [x] selected bin/layer、LP/MILP objective、gap、elapsed 指标仍输出。
- [x] `final bins -> packing -> schema` 一致性断言仍通过。
- [x] `daily.md` 保持删除状态。
- [ ] `refactor.md` 更新为最终完成状态。

## 8. 建议验证命令

strict scanner：

```powershell
pwsh.exe -NoLogo -NoProfile -File scripts/generic-boundary-check.ps1 -ProjectRoot .
```

compile：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am compile '-Dgpg.skip=true'"
```

默认 application 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

Gurobi 回归：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

真实 CSV 单文件 suite：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.paths=E:/path/to/real-case.csv' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

真实 CSV 目录 suite：

```powershell
pwsh.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.dir=E:/path/to/real-suite' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

## 9. 注意事项

1. 不要把 Gurobi 通过等同于纯泛型主链完成。
2. 不要用 scanner 漏扫制造 pass。
3. 不要新增 compat/legacy bridge 来绕过迁移。
4. 不要恢复 BPP3D `daily.md`。
5. 不要提交真实业务 CSV。
6. 不要混入 `cylinder.md` 圆柱真实几何专题。

## 10. 本轮执行记录（2026-05-29）

已完成：

1. scanner 规则扩展并复跑，失败清单从大规模命中收敛到 `ColumnGenerationStandardExecutors.kt` 5 条 `Flt64` 命中。
2. 清理 application/layer-assignment 旧桥接：
   - 删除 `ApplicationRequestLegacyBridge.kt`，新增 `ApplicationRequestDemandSlices.kt`。
   - 删除 `LoadLegacyBridge.kt`，新增 `LoadQuantityBridge.kt`。
3. `QuantityDomainApi.kt` 迁移到中性命名（`toModel`、`toInfraQuantity`）并同步调用方。
4. 清理 `src/main` 中 `Legacy*` / `legacy*` 命名与 `toLegacy*` 调用，保留语义不变。
5. `ColumnGenerationStandardExecutors.kt` 移除 `Flt64` 显式类型依赖，scanner 对该文件不再命中。
6. 同步修复受命名变更影响的 `src/test` 编译调用。
7. 验证：
   - `mvn -f pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过。

追加验证：

1. 默认 application 全量回归已通过：`mvn -f pom.xml -pl bpp3d-application -am test -Dgpg.skip=true`。
2. FltX 关键回归已随默认 application 全量回归通过，覆盖 `LayerGenerationFltXProofTest`、`FltXDirectCompileProofTest`、`QuantityGeometrySpikeTest`、`QuantityDemand*GenericTest` 等。
3. CSV `suite.paths` 已通过，使用仓库内 `bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv`。
4. CSV `suite.dir` 已通过，使用仓库内 `bpp3d-application/src/test/resources/gurobi`。
5. strict scanner 复核通过：`STRICT_GENERIC_BOUNDARY_PASS`。
6. 7.2/7.3 增量切片已落地（保持主流程签名不变）：
   - `Load.kt` 新增 `GenericBpp3dDemandEntry<V>`、`GenericBpp3dItemDemand<V>`、`GenericBpp3dMaterialDemand<V>`、`GenericLoad<V>`。
   - 增加 `Bpp3dDemandEntry <-> GenericBpp3dDemandEntry<InfraNumber>` 转换函数与 `Load.toGenericLoad()` 适配入口。
   - 现有 `Load`/`Bpp3dDemandEntry` 仍作为运行时主路径，避免一次性破坏。
7. 增量验证：
   - `mvn -f pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -ExecutionPolicy Bypass -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f pom.xml -pl bpp3d-application -am -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
8. `DemandConstraint` 已切换到泛型 load 入口：
   - 引入 `DemandEntry<V>` 抽象契约，`Bpp3dDemandEntry` 与 `GenericBpp3dDemandEntry<V>` 统一实现该契约。
   - `Load` 现在继承 `GenericLoad<InfraNumber>`，`DemandConstraint` 参数改为 `GenericLoad<InfraNumber>` 与 `List<DemandEntry<InfraNumber>>`。
   - 现有 application / layer-assignment 调用保持兼容，编译与回归门槛保持通过。
9. application/executor 的 demand entries 入参已提升到泛型契约：
   - `ColumnGenerationApplicationRequest` 与 `solveQuantityDemands` 的 `demandEntries` 改为 `List<DemandEntry<InfraNumber>>?`。
   - `ColumnGenerationStandardExecutors` 内部主字段改为 `List<DemandEntry<InfraNumber>>`，并已直接传递到 `ImpreciseLoad/PreciseLoad`。
   - 继续保持 `compile + scanner + GurobiColumnGenerationTest` 通过。
10. item/material demand 已完成泛型契约化：
   - 新增 `ItemDemand<V>`、`MaterialDemand<V>`，并由 `Bpp3dItemDemand`、`Bpp3dMaterialDemand`、`GenericBpp3dItemDemand<V>`、`GenericBpp3dMaterialDemand<V>` 统一实现。
   - `demandEntriesFromLabeledItemDemands` 与 `demandEntriesFromLabeledMaterialDemands` 改为接收契约类型（`List<ItemDemand<InfraNumber>>` / `List<MaterialDemand<InfraNumber>>`）。
   - `LoadQuantityBridge` 对应 `toModelItemDemands` / `toModelMaterialDemands` 返回值改为契约列表。
   - 本轮仍保持 `compile + scanner + GurobiColumnGenerationTest` 通过。
11. `Load` 主链进一步收口到 demand 契约：
   - `AbstractLoad.loadCoefficient` 入参从 `Bpp3dDemandEntry` 提升为 `DemandEntry<InfraNumber>`。
   - `ImpreciseLoad` / `PreciseLoad` 的 `demandEntries` 字段类型提升为 `List<DemandEntry<InfraNumber>>`。
   - `ColumnGenerationStandardExecutors` 删除 `modelDemandEntries` 回落桥接，直接把契约列表传入 `ImpreciseLoad` / `PreciseLoad`。
   - 增量验证：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true`、`scripts/generic-boundary-check.ps1`、`GurobiColumnGenerationTest` 均通过。
12. `Load.kt` demand entries 工厂返回类型提升到契约：
   - `demandEntriesFromItems` / `demandEntriesFromItemRanges` / `demandEntriesFromMaterial*` / `demandEntriesFrom*Demands` 返回类型统一改为 `List<DemandEntry<InfraNumber>>`。
   - 保留 `Bpp3dDemandEntry` 作为当前具体实现，调用侧不再依赖具体列表签名。
   - 同步测试签名：`ItemDemandConstraintModeKeyTest` 与 `GurobiColumnGenerationTest` 中相关 `List<Bpp3dDemandEntry>` 字段改为契约类型。
   - 增量验证：`compile + scanner + GurobiColumnGenerationTest` 均通过。
13. `Load.kt` 主类型进一步收口为单一定义泛型：
   - `Bpp3dDemandEntry<V>`、`Bpp3dItemDemand<V>`、`Bpp3dMaterialDemand<V>` 升级为泛型主定义，并补充 `InfraBpp3d*` 类型别名。
   - 移除并合并 `GenericBpp3dDemandEntry<V>`、`GenericBpp3dItemDemand<V>`、`GenericBpp3dMaterialDemand<V>`，保留 `toGenericDemandEntry` / `toModelDemandEntry` 兼容扩展函数。
   - `Load.toGenericLoad()` 直接输出 `List<Bpp3dDemandEntry<InfraNumber>>`，不再依赖独立 `GenericBpp3d*` 结构。
14. 清理 `LegacyScalars.kt` 文件名残留与测试豁免：
   - `bpp3d-domain-item-context` 将 `LegacyScalars.kt` 重命名为 `ItemScalars.kt`（函数签名保持不变）。
   - `Bpp3dGenericBoundaryTest` 删除 `LegacyScalars.kt` 白名单，边界检查不再依赖该例外。
15. 本轮增量验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=Bpp3dGenericBoundaryTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`）。
16. `Load` 主契约继续收口：
   - 移除 `GenericLoad<V>` 中间层，统一为泛型 `Load<V>` 并新增 `typealias InfraLoad = Load<InfraNumber>`。
   - `DemandConstraint` 入参从 `GenericLoad<InfraNumber>` 切换为 `Load<InfraNumber>`。
   - 同步 `ItemDemandConstraintModeKeyTest` 的匿名 load 类型签名到 `Load<InfraNumber>`。
17. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`）。
18. layer-assignment 命名收口：
   - `LoadQuantityBridge.kt` 重命名为 `LoadQuantityAdapters.kt`，保留函数签名与调用不变。
   - 增量验证：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am compile -Dgpg.skip=true` 通过。
19. layer-assignment 转换层进一步收口：
   - 将 `LoadQuantityAdapters.kt` 中 `toModelItems` / `toModelItemRanges` / `toModelLayers` / `toModelItemDemands` / `toModelMaterialDemands` / `toModelMaterialWeightDemandsByKey` 全部并入 `Load.kt`。
   - 删除独立文件 `LoadQuantityAdapters.kt`，减少主流程桥接文件层级。
20. application 转换层进一步收口：
   - 将 `ApplicationRequestDemandSlices.kt` 中 `DemandSlices` 与 `toDemandSlices` 并入 `ColumnGenerationApplicationService.kt`（私有工具）。
   - 删除独立文件 `ApplicationRequestDemandSlices.kt`。
21. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
22. executor 入口收口：
   - 删除 `ColumnGenerationStandardExecutors` 的 quantity 重载工厂：`fromQuantityItems` 与 quantity 版 `fromDemandEntries`。
   - executor 保留单一模型入参入口（`List<Pair<Item, UInt64>>` + `List<DemandEntry<InfraNumber>>`）。
   - 同步测试：`ColumnGenerationAlgorithmTest` 中对应工厂测试改为先 `toModel` 再调用模型入口。
23. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
24. application quantity 入口收口：
   - 删除 `ColumnGenerationApplicationRequest.fromQuantityDemands`。
   - 删除 `ColumnGenerationApplicationService.solveQuantityDemands`。
   - 同步测试：`ColumnGenerationAlgorithmTest` 中 quantity 入口用例改为测试内先 `toModel`，再调用 `ColumnGenerationApplicationRequest` 与 `service.solve` 主路径。
25. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
26. layer-assignment 主路径去 `api` 依赖：
   - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt` 删除 `QuantityItem/QuantityMaterial/QuantityBinLayer` 相关 import 与所有 quantity 重载入口（`toModel*`、`demandEntriesFrom*` quantity 版、`ImpreciseLoad/PreciseLoad` quantity 版 companion 工厂）。
   - `Load.kt` 保留单一 model 主路径（`Item/Material/BinLayer`）与 `DemandEntry<InfraNumber>` 契约入口。
   - 复核 `bpp3d-domain-layer-assignment-context/src/main` 已无 `domain.item.api` 依赖（`NO_API_IMPORT_IN_LAYER_ASSIGNMENT_MAIN`）。
27. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
28. `api` 标量依赖继续收口（跨上下文主路径）：
   - `bpp3d-domain-bla-context/src/main/.../BottomUpLeftJustifiedAlgorithm.kt` 用 `infraZero/infraNegativeInfinity` 替换 `itemZero/itemNegativeInfinity`。
   - `bpp3d-domain-block-loading-context/src/main/.../ComplexBlockGenerator.kt` 用 `infraInfinity` 替换 `itemInfinity`。
   - `bpp3d-domain-block-loading-context/src/main/.../DepthFirstSearchAlgorithm.kt` 用 `infraZero` 替换 `itemZero`。
   - `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt` 用 `infraInfinity/infraZero/infraScalar` 替换 `itemInfinity/itemZero/itemScalar`。
   - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt` 用 `infraScalar` 替换 `itemScalar`。
29. 主路径依赖复核与增量验证：
   - `bpp3d-domain-bla-context` + `bpp3d-domain-block-loading-context` + `bpp3d-domain-layer-generation-context` 的 `src/main` 已无 `domain.item.api` 依赖（`NO_API_IMPORT_IN_BLA_BLOCK_LAYERGEN_MAIN`）。
   - `bpp3d-domain-item-context/src/main` 之外已无 `domain.item.api` 依赖（`NO_API_IMPORT_OUTSIDE_ITEM_CONTEXT_MAIN`）。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
30. `domain-item-context` 主路径继续去 `api` 依赖：
   - `bpp3d-domain-item-context/src/main/.../model` 与 `.../service` 统一改为直接使用 `infraZero/infraOne/infraInfinity/infraNegativeInfinity/infraScalar`。
   - 新增 `domain.item.model.ItemCuboid` 别名；`domain.item.api.ItemScalars` 仅保留兼容别名转发。
   - 复核 `bpp3d-domain-item-context/src/main/.../model` 与 `.../service` 已无 `domain.item.api` 依赖（`NO_API_IMPORT_IN_ITEM_MODEL_SERVICE_MAIN`）。
31. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am compile -Dgpg.skip=true` 通过。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
32. `Material/PackageShape/Package` 小步别名试探与回滚结论：
   - 在 `domain.item.api.QuantityDomainApi` 中尝试将 `Material<V>`、`PackageShape<V>`、`Package<V>` 改为 `domain.item.model.Generic*` 的 `typealias` 兼容层。
   - 结果：`bpp3d-domain-item-context` `test-compile` 出现大量 `CapturedType` 泛型推断错误（典型集中在 `QuantityDemandReducedCostGenericTest`、`QuantityDemandStatisticsGenericTest`、`QuantityDomainAliasExampleTest`），与此前整包 typealias 失败结论一致。
   - 决策：回滚 `QuantityDomainApi` 到原实现，保留 `QuantityDomainModels.kt` 作为后续迁移候选，不在当前阶段推进 `api` 侧 `typealias` 收口。
33. 回滚后稳定性复核：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
34. `model` 侧补齐泛型统计主路径（不触碰 `QuantityDomainApi`）：
   - 新增 `bpp3d-domain-item-context/src/main/.../model/GenericDemandStatistics.kt`，提供：
     - `GenericBpp3dDemandKey` / `GenericBpp3dDemandValue`
     - `GenericItem` / `GenericItemPlacement` / `GenericBinLayer` 及 `Iterable<GenericBinLayer>` 的 `statistics` 扩展
   - 数值缩放逻辑统一基于 infrastructure 标量（`infraScalar` + `toFltX`），避免新增对 `domain.item.api` 标量函数依赖。
35. 外部调用方迁移（测试层，小步）：
   - `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationFltXProofTest.kt` 从 `domain.item.api.*` 迁移到 `domain.item.model.Generic*` + `model.statistics`。
   - `bpp3d-domain-layer-assignment-context/src/test/.../FltXDirectCompileProofTest.kt` 从 `domain.item.api.*` 迁移到 `domain.item.model.Generic*` + `model.statistics`。
   - 复核：`item-context` 之外的 `domain.item.api` 引用已进一步收缩为 `bpp3d-application` 的 quantity 兼容测试用例。
36. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=FltXDirectCompileProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
37. `application` quantity 测试夹具迁移（仅测试层）：
   - `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt` 中 `QuantityItem/QuantityBinLayer/QuantityItemPlacement/QuantityMaterial/QuantityPackage/QuantityPackageShape` 的 import 来源由 `domain.item.api` 切换为 `domain.item.model.Generic*`（别名名保持不变，测试代码主体不改）。
   - 复核：`item-context` 之外已无 `domain.item.api` import；当前 `domain.item.api` 引用仅保留在 `domain-item-context` 内部（main + api 兼容测试）与 `domain-item-context/src/test/.../item/model/MaterialDemandReducedCostTest.kt` 的 `itemScalar` 引用。
38. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
39. `domain-item-context` 测试侧标量依赖收口：
   - `bpp3d-domain-item-context/src/test/.../MaterialDemandReducedCostTest.kt` 将 `domain.item.api.itemScalar` 替换为 `infrastructure.infraScalar`。
   - 复核：`bpp3d-domain-item-context/src/test` 已无 `itemScalar` 直接引用。
40. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=MaterialDemandReducedCostTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 1, Failures: 0, Errors: 0, Skipped: 0`）。
   - `rg "domain\\.item\\.api" ospf-kotlin-framework-bpp3d -g"*.kt"` 显示 `domain.item.api` 引用已收敛到 `bpp3d-domain-item-context` 内部（main + api 兼容测试）。
41. `api` 统计路径内部标量依赖收口：
   - `bpp3d-domain-item-context/src/main/.../api/QuantityDemandStatistics.kt` 将 `quantityScale` 中 `itemScalar(amount)` 替换为 `infraScalar(amount)`，避免 `api` 统计逻辑对兼容标量函数的反向依赖。
   - 复核：`rg "itemScalar\\(" ospf-kotlin-framework-bpp3d -g"*.kt"` 结果仅剩 `ItemScalars.kt` 中兼容函数定义。
42. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=QuantityDemandStatisticsGenericTest,QuantityDemandReducedCostGenericTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
43. 兼容标量函数使用面复核：
   - `rg "itemZero\\(|itemOne\\(|itemTwo\\(|itemInfinity\\(|itemNegativeInfinity\\(|itemScalar\\(" ospf-kotlin-framework-bpp3d -g"*.kt"` 结果仅命中 `bpp3d-domain-item-context/src/main/.../api/ItemScalars.kt`。
   - 结论：`item*` 系列函数已退化为纯兼容出口，不再参与仓库内部主流程实现。
44. `model` 侧补齐泛型 reduced-cost 主路径：
   - 新增 `bpp3d-domain-item-context/src/main/.../model/GenericDemandReducedCost.kt`，提供：
     - `GenericDemandShadowPriceKey`
     - `GenericItem<V>.reducedCost(...)`
     - `GenericBinLayer<V>.reducedCost(...)`
   - 实现沿用与 `api.QuantityDemandReducedCost` 等价的统计聚合逻辑，作为后续移除 `api` 并行实现的承接点。
45. `model` 侧新增对应单测：
   - 新增 `bpp3d-domain-item-context/src/test/.../model/GenericDemandReducedCostTest.kt`，覆盖：
     - `InfraNumber` 材料需求 reduced-cost
     - `FltX` 层级重量需求 reduced-cost
46. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=GenericDemandReducedCostTest,QuantityDemandReducedCostGenericTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
47. layer-assignment 双轨命名清理（主路径）：
   - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt` 删除未被调用的 `toGenericDemandEntry` 两个扩展（`Bpp3dDemandEntry<InfraNumber>` 与 `DemandEntry<InfraNumber>` 版本）。
   - 保留 `toModelDemandEntry` 作为唯一需求条目归一化入口，减少 generic/model 双命名并行噪音。
48. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am compile -Dgpg.skip=true` 通过。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=FltXDirectCompileProofTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
49. layer-assignment 需求条目归一化逻辑去重：
   - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt` 中 `DemandEntry<InfraNumber>.toModelDemandEntry()` 的 `Bpp3dDemandEntry` 分支改为直接复用 `Bpp3dDemandEntry<InfraNumber>.toModelDemandEntry()`，去掉重复字段拷贝实现。
50. 本轮补充验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am compile -Dgpg.skip=true` 通过。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=FltXDirectCompileProofTest,ItemDemandConstraintModeKeyTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `pwsh -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
51. 交接说明（2026-05-29）：
   - 当前稳定基线：
     - `QuantityDomainApi.kt` 保持原实现（未推进 `typealias` 化），避免触发 `CapturedType` 泛型推断回归。
     - `item*` 兼容标量函数仅保留在 `domain-item-context/src/main/.../api/ItemScalars.kt` 作为兼容出口。
   - 已补齐的 `model` 承接能力：
     - `GenericDemandStatistics.kt`
     - `GenericDemandReducedCost.kt`
     - 以及对应 `model` 侧测试。
   - 下会话建议入口：
      1. 优先将 `domain.item.api` 兼容测试用例逐步复制/迁移到 `domain.item.model`，先做到“行为等价双测”；
      2. 双测稳定后，再评估裁剪 `api` 并行实现；
      3. 每步都保持 `mvn --%` + `generic-boundary-check` 验证闭环。
52. `model` 双测补齐（承接 51 的建议入口）：
   - `bpp3d-domain-item-context/src/test/.../model/GenericDemandReducedCostTest.kt` 新增 `reducedCostShouldSupportGenericShadowPriceKeyMap`，补齐与 `api` 侧 `QuantityDemandReducedCostGenericTest` 等价的 shadow-price map 场景。
   - 新增 `bpp3d-domain-item-context/src/test/.../model/GenericDemandStatisticsTest.kt`，覆盖 `Flt64/FltX` 两条统计路径（`ItemAmount`、`ItemMaterialAmount`、`ItemMaterialWeight`）并校验层级聚合结果。
   - 本轮验证：
     - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=GenericDemandReducedCostTest,GenericDemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`）。
     - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
53. `model` 主路径补齐泛型别名与 alias 双测：
   - 新增 `bpp3d-domain-item-context/src/main/.../model/GenericQuantityDomainAliases.kt`，在 `domain.item.model` 下提供 `InfraNumber*/FltX*` 泛型别名（`GenericMaterial/GenericPackageShape/GenericPackage/GenericItem/GenericItemPlacement/GenericBinLayer`），作为 `api` 兼容别名的主路径承接。
   - 新增 `bpp3d-domain-item-context/src/test/.../model/GenericDomainAliasExampleTest.kt`，覆盖 `InfraNumber/FltX` 两条 alias 构造路径并验证 `toModel` 行为，与 `api` 侧 `QuantityDomainAliasExampleTest` 形成行为等价双测。
   - 本轮验证：
     - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=GenericDomainAliasExampleTest,QuantityDomainAliasExampleTest,GenericDemandReducedCostTest,GenericDemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`）。
     - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
54. item-context 模块全量回归复核：
   - 执行 `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am test -Dgpg.skip=true`，`bpp3d-domain-item-context` 全量测试通过（`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`）。
   - 说明：本轮新增的 `GenericDemandStatisticsTest`、`GenericDemandReducedCostTest`（含 shadow-price map 场景）和 `GenericDomainAliasExampleTest` 均已纳入全量回归并通过。
55. `api <-> model.Generic*` 适配层补齐（为后续裁剪 `api` 并行实现做准备）：
   - 新增 `bpp3d-domain-item-context/src/main/.../api/QuantityDomainModelAdapters.kt`，提供 `Material/PackageShape/Package/Item/ItemPlacement/BinLayer` 与 `model.Generic*` 的双向转换函数：
     - `toGenericModel(...)`
     - `toApiModel(...)`
   - 新增 `bpp3d-domain-item-context/src/test/.../api/QuantityDomainModelAdaptersTest.kt`，覆盖 `InfraNumber/FltX` 两条路径的往返转换与 `toModel` 等价性校验。
56. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=QuantityDomainModelAdaptersTest,GenericDomainAliasExampleTest,QuantityDomainAliasExampleTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am test -Dgpg.skip=true` 通过（`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
57. `api` 统计/降价并行实现收口（委托 `model`）：
   - `bpp3d-domain-item-context/src/main/.../api/QuantityDemandStatistics.kt` 已改为通过 `toGenericModel()` 委托 `model.GenericDemandStatistics`，仅保留 `api` 对外类型签名与键值类型映射。
   - `bpp3d-domain-item-context/src/main/.../api/QuantityDemandReducedCost.kt` 已改为通过 `toGenericModel()` 委托 `model.GenericDemandReducedCost`，仅保留 `api` 对外函数签名与 demand-key / shadow-price-key 映射。
58. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=QuantityDemandReducedCostGenericTest,QuantityDemandStatisticsGenericTest,QuantityDomainModelAdaptersTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am test -Dgpg.skip=true` 通过（`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。

待继续：

1. 将 `Material<V>`、`PackageShape<V>`、`Package<V>`、`Item<V>`、`BinLayer<V>` 等泛型模型提升为唯一正式主模型。
2. 将 `Load<V>` 泛型化为正式主模型，并清理剩余 `InfraNumber` 绑定实现。
3. 将 application、layer-generation、layer-assignment、block-loading、BLA、packing planner 调用方迁移到正式泛型模型。
4. 完成最终验收条目并将 `refactor.md` 收口为最终完成状态。
59. `domain-item-context` 移除 `domain.item.api` 并行结构（主/测）：
   - 删除 `bpp3d-domain-item-context/src/main/.../domain/item/api` 下全部实现文件：`QuantityDomainApi.kt`、`QuantityDomainAliases.kt`、`QuantityDemandStatistics.kt`、`QuantityDemandReducedCost.kt`、`ItemScalars.kt`。
   - 删除 `bpp3d-domain-item-context/src/test/.../domain/item/api` 下全部兼容测试与夹具：`QuantityDemandStatisticsGenericTest.kt`、`QuantityDemandReducedCostGenericTest.kt`、`QuantityDomainAliasExampleTest.kt`、`QuantityDomainModelAdaptersTest.kt`、`TestNumberAliases.kt`。
   - 复核：`rg "domain\\.item\\.api" ospf-kotlin-framework-bpp3d -g"*.kt"` 无命中。
60. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am clean test -Dgpg.skip=true` 通过（`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
61. 全链路编译恢复（依赖工件对齐）：
   - 先执行 `mvn --% -f ospf-kotlin-core/pom.xml -Dmaven.test.skip=true install -Dgpg.skip=true` 与 `mvn --% -f ospf-kotlin-framework/pom.xml -Dmaven.test.skip=true install -Dgpg.skip=true`，将本地 `ospf-kotlin-core` / `ospf-kotlin-framework` 工件对齐到当前源码主工件（跳过 test-compile）。
   - 执行 `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
62. 主回归复核：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
63. 验收条目复核补记：
   - `rg "Legacy|legacy|toLegacy" .../bpp3d-domain-bla-context/src/main .../bpp3d-domain-block-loading-context/src/main .../bpp3d-domain-layer-assignment-context/src/main .../bpp3d-domain-layer-generation-context/src/main .../bpp3d-domain-packing-context/src/main -g"*.kt"` 无命中，对应勾选「block-loading 与 BLA 不依赖 legacy helper」。
   - `Test-Path ospf-kotlin-framework-bpp3d/daily.md` 返回 `False`，对应勾选「`daily.md` 保持删除状态」。
64. layer-assignment demand 条目主链收口与回归修复：
   - `Load<V>` 的 `demandEntries` 主字段与 `AbstractLoad.loadCoefficient(...)`、`ImpreciseLoad/PreciseLoad` 入参统一收口为 `List<Bpp3dDemandEntry<V>>`（`DemandConstraint` 同步对齐）。
   - 修复 `ItemDemandConstraintModeKeyTest` 中匿名 `Load` 的覆写签名，避免 `List<DemandEntry<InfraNumber>>` 与 `List<Bpp3dDemandEntry<InfraNumber>>` 不匹配。
   - `ColumnGenerationStandardExecutors` 保持外部 `List<DemandEntry<InfraNumber>>` 入参不变，内部新增 `modelDemandEntries = demandEntries.map { it.toModelDemandEntry() }` 归一化后再传入 `ImpreciseLoad/PreciseLoad` 与 `DemandConstraint`，控制改动面。
65. 运行期回归故障修复（StackOverflow）：
   - `Load.kt` 中 `DemandEntry<InfraNumber>.toModelDemandEntry()` 原先在 `Bpp3dDemandEntry` 分支触发扩展函数重载递归，导致 `ColumnGenerationAlgorithmTest` 出现 `StackOverflow`。
   - 现改为无分支直接拷贝构造 `Bpp3dDemandEntry(...)`，消除递归路径并保持语义不变。
66. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=ItemDemandConstraintModeKeyTest,FltXDirectCompileProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
67. application demand 条目入参收口到主模型：
   - `ColumnGenerationApplicationRequest.demandEntries` 从 `List<DemandEntry<InfraNumber>>?` 收口为 `List<Bpp3dDemandEntry<InfraNumber>>?`。
   - `ColumnGenerationStandardExecutors` 的 `demandEntries` 主字段与 `fromDemandEntries(...)` 入参同步收口为 `List<Bpp3dDemandEntry<InfraNumber>>`。
   - 删除 executor 内部 `modelDemandEntries = demandEntries.map { it.toModelDemandEntry() }` 归一化桥接，`ImpreciseLoad/PreciseLoad/DemandConstraint` 直接使用主模型 demand 条目。
68. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
69. Gurobi profile 用例签名对齐：
   - `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt` 中 `CsvDrivenScenario.demandEntries` 从 `List<DemandEntry<...>>` 收口为 `List<Bpp3dDemandEntry<...>>`，与 `ColumnGenerationStandardExecutors.fromDemandEntries(...)` 新签名保持一致。
   - 修复同文件别名类型参数引用：`Bpp3dDemandEntry<InfraNumber>` 调整为 `Bpp3dDemandEntry<Flt64>`（该文件内 `InfraNumber` 已别名为 `Flt64`）。
70. Gurobi profile 验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
71. layer-assignment demand 抽象收口：
   - `Load.kt` 移除 `DemandEntry<V>` 抽象接口，`Bpp3dDemandEntry<V>` 作为唯一 demand 条目主模型。
   - 删除 `toModelDemandEntry()` 适配函数（`Bpp3dDemandEntry<InfraNumber>.toModelDemandEntry` 与 `DemandEntry<InfraNumber>.toModelDemandEntry`），避免无效桥接与重载歧义回归。
72. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=ItemDemandConstraintModeKeyTest,FltXDirectCompileProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
73. demand concrete-mode 判定收口（保留显式 mode 兼容）：
   - `bpp3d-domain-item-context/src/main/.../DemandStatistics.kt` 新增：
     - `Bpp3dDemandKey.toConcreteMode(isDiscrete)`
     - `Bpp3dDemandMode.toConcreteMode(key, isDiscrete)`
     其中语义 mode（`Item` / `Material`）按 `key + isDiscrete` 推导，显式具体 mode（`ItemAmount` / `ItemWeight` / `ItemMaterialAmount` / `ItemMaterialWeight`）保持优先；若 mode 与 key 类型不匹配，则回退到 `key + isDiscrete`。
   - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt`：
     - `Bpp3dDemandEntry.quantityDomain` 从构造参数改为基于 `quantityUnit` 的动态推导属性，避免手工覆写。
     - `loadCoefficient(...)` 的统计 mode 解析改为 `demand.mode.toConcreteMode(demand.key, isDiscrete)`。
   - `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt` 与
     `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt` 同步改为上述统一 concrete-mode 解析路径。
74. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=UnifiedDemandEntriesTest,MaterialDemandEntriesTest,ItemDemandConstraintModeKeyTest,FltXDirectCompileProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
75. `DemandMode` / 离散域 / dimension 验收条目复核：
   - `DemandMode` 的 concrete 选择统一经 `Bpp3dDemandMode.toConcreteMode(key, isDiscrete)`，`isDiscrete` 仅来源于 `quantityUnit` 域信息：
     - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt`
     - `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
     - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
   - `Bpp3dDemandEntry.quantityDomain` 改为按 `quantityUnit` 动态推导，不再由外部参数写入：
     - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt`
   - `rg --line-number -g "*.kt" "dimension|dimensionSymbol|getDimension|quantity\\.dimension|unit\\.quantity\\.dimension" ospf-kotlin-framework-bpp3d` 仅命中测试目录（`src/test`），`src/main` 无命中。
76. packing demand 泛型化切片（面向 7.3 的增量）：
   - `bpp3d-domain-packing-context/src/main/.../MaterialPackingPlan.kt`：
     - `MaterialPackingDemand` 提升为 `MaterialPackingDemand<V : FloatingNumber<V>>`；
     - 新增 `InfraMaterialPackingDemand` 别名（`MaterialPackingDemand<MaterialPackingScalar>`）。
   - `bpp3d-domain-packing-context/src/main/.../MaterialPacker.kt`：
     - `plan(...)` 入参改为 `List<MaterialPackingDemand<*>>`，兼容不同数值实现的重量需求类型。
   - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt`：
     - `materialDemands` 本地集合改为 `ArrayList<MaterialPackingDemand<*>>()`，与新签名对齐。
77. 本轮补充验证：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=MaterialPackerTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
78. `PackingProgram<V>` 与 packing candidate 主链泛型化：
   - `bpp3d-domain-item-context/src/main/.../Package.kt`：
     - `PackingProgram` 升级为 `PackingProgram<V : FloatingNumber<V>>`；
     - `Package.program` 与相关工厂/校验签名改为 `PackingProgram<*>`，保持运行时兼容；
     - `innerPackageWithMaterialQuantities(...)` 直接返回 `PackingProgram<V>`。
   - `bpp3d-domain-packing-context/src/main/.../MaterialPackingPlan.kt`：
     - `MaterialPackingProgramCandidate` 升级为 `MaterialPackingProgramCandidate<V : FloatingNumber<V>>`；
     - 新增 `InfraMaterialPackingProgramCandidate` 别名；
     - `PackageSelection` / `MaterialPackingAssignment` 的 `candidate` 字段改为 `MaterialPackingProgramCandidate<*>`。
   - `bpp3d-domain-packing-context/src/main/.../service/MaterialPacker.kt` 与 `MaterialPackingSolverExecutor.kt`：
     - `plan(...)` 与 `MaterialPackingMipRequest` / `solve(...)` 入口统一为泛型 `V`；
     - `MaterialPacker.plan(...)` 主签名改为 `List<MaterialPackingDemand<V>>` + `List<MaterialPackingProgramCandidate<V>>`。
   - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt`：
     - request 的 `materialPackingCandidates` 与 `layerGenerationProgramDemands` 改为 `MaterialPackingProgramCandidate<InfraNumber>`；
     - `materialDemands` 本地集合改为 `ArrayList<MaterialPackingDemand<InfraNumber>>()`。
79. 本轮补充验证（packing program 泛型化后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=PackingProgramMaterialValueTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 5, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=MaterialPackerTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest,PackingProgramLayerCandidateAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
80. `Package<V>` 主定义泛型化（与 `PackingProgram<V>` 对齐）：
   - `bpp3d-domain-item-context/src/main/.../Package.kt`：
     - `Package` 升级为 `Package<V : FloatingNumber<V>>`；
     - `program` 字段由 `PackingProgram<*>?` 收口为 `PackingProgram<V>?`；
     - `packages` 字段改为 `List<Package<V>>?`；
     - `PackingProgram.actualPackage(...)` 返回类型收口为 `Package<V>`；
     - `Package` 工厂函数按场景分流：`innerPackage(shape, ...) -> Package<InfraNumber>`，`inner/outerPackage(program, ...) -> Package<V>`。
   - 受影响调用侧签名对齐：
     - `ActualItem.pack` 与其构造器参数改为 `Package<*>`（`Item.kt`）；
     - `GenericPackage.toModel()` 返回类型改为 `Package<InfraNumber>`（`QuantityDomainModels.kt`）；
     - `MaterialAttributeKey.match(...)` 入参改为 `Package<*>`（`MaterialAttribute.kt`）；
     - `MaterialPackingPlan.packages` 改为 `List<Package<*>>`，`MaterialPacker.PackageSlot.pack` 改为 `Package<*>`。
81. 本轮补充验证（`Package<V>` 泛型化后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=PackingProgramMaterialValueTest,DemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=MaterialPackerTest,PackerAndRendererAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest,PackingProgramLayerCandidateAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
82. `Material<V>` 主定义泛型化（与当前主链 `InfraNumber` 语义兼容）：
   - `bpp3d-domain-item-context/src/main/.../Material.kt`：
     - `Material` 升级为 `Material<V : FloatingNumber<V>>`；
     - 保留默认重量构造行为（`defaultMaterialWeight()`），避免历史调用点需要同步补参。
   - 主链签名对齐（不改变主流程语义，仅补足类型参数）：
     - `bpp3d-domain-item-context/src/main/.../Package.kt`：`materialCatalog`、`actualPackage(...)`、`Package.materials` 及 companion 工厂参数统一收口到 `Material<InfraNumber>`；
     - `bpp3d-domain-item-context/src/main/.../QuantityDomainModels.kt`：`toModel()` 与 `materialCache` 统一收口到 `Material<InfraNumber>`；
     - `bpp3d-domain-layer-assignment-context/src/main/.../Load.kt`：`demandEntriesFromMaterial*` 系列入参统一为 `Material<InfraNumber>`；
     - `bpp3d-domain-packing-context/src/main/.../MaterialPackingPlan.kt` 与 `MaterialPacker.kt`：需求与目录类型按 packing scalar 语义收口（`Material<MaterialPackingScalar>`）；
     - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt` 与 `PackingProgramLayerCandidateAdapter.kt`：request/catalog 与适配器签名补全 `Material<InfraNumber>`。
   - 测试与 Gurobi profile 签名对齐：
     - 修复 `src/test` 与 `src/gurobi-test` 中裸 `Material` 类型注解，补全为显式类型参数（`InfraNumber` 或 `Flt64`）。
83. 本轮补充验证（`Material<V>` 泛型化后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=PackingProgramMaterialValueTest,DemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=MaterialPackerTest,PackerAndRendererAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 8, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest,PackingProgramLayerCandidateAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
84. `PackageShape<V>` 主定义泛型化（并完成主链调用侧收口）：
   - `bpp3d-domain-item-context/src/main/.../Package.kt`：
     - `PackageBottomShape` 与 `PackageShape` 升级为 `<V : FloatingNumber<V>>`；
     - `PackingProgram<V>.shape`、`Package<V>.shape` 与对应工厂函数签名同步收口到 `PackageShape<V>`（`innerPackage(shape, ...)` 保持 `PackageShape<InfraNumber>` 返回 `Package<InfraNumber>`）；
   - `bpp3d-domain-item-context/src/main/.../Item.kt`：
     - `ItemPattern.shape` 改为 `PackageShape<InfraNumber>`；
     - `ActualItem` 保持 `pack: Package<*>` 兼容，新增 `Quantity<*> -> Quantity<InfraNumber>` 转换以承接泛型 `pack` 尺寸；
   - `bpp3d-domain-item-context/src/main/.../QuantityDomainModels.kt`：
     - `GenericPackageShape<V>.toModel()` 返回类型收口为 `PackageShape<InfraNumber>`；
   - `bpp3d-domain-packing-context/src/main/.../PackageSolutionLikeAdapter.kt`：
     - `PackageSolutionLikeNode.shape` 收口为 `PackageShape<InfraNumber>`；
   - `bpp3d-domain-packing-context/src/main/.../MaterialPacker.kt` 与 `bpp3d-application/src/main/.../PackingProgramLayerCandidateAdapter.kt`：
     - 新增 `Quantity<*> -> Quantity<InfraNumber>` 转换，消除 `PackageShape<V>` 传播后的 `CapturedType(*)` 赋值冲突。
85. 本轮补充验证（`PackageShape<V>` 泛型化后）：
   - `mvn --% -f pom.xml -pl bpp3d-domain-item-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f pom.xml -pl bpp3d-domain-packing-context -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `mvn --% -f pom.xml -pl bpp3d-domain-item-context -am -Dtest=PackingProgramMaterialValueTest,DemandStatisticsTest,MaterialDemandReducedCostTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f pom.xml -pl bpp3d-domain-packing-context -am -Dtest=MaterialPackerTest,PackerAndRendererAdapterTest,PackageSolutionLikeAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 11, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest,PackingProgramLayerCandidateAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 21, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 10`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/generic-boundary-check.ps1 -ProjectRoot .` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
86. layer-generation 泛型输入适配入口（面向 7.3 的增量）：
   - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt` 新增
     `bpp3dLayerGenerationRequestFromGeneric<T, V>(...)`：
     - 入参直接接收 `List<GenericItem<T>>` 与 `List<GenericBinLayer<T>>`；
     - 在函数内部统一使用共享 `materialCache/itemCache` 转换为 `Bpp3dLayerGenerationRequest<V>` 的现有模型字段（`items`、`existingLayers`）；
     - 调用侧不再需要手写 `toModel()` 展开循环，减少 `Item<V>` 进入 layer-generation 入口前的样板转换代码。
87. 本轮补充验证（layer-generation 泛型输入适配后）：
   - `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationFltXProofTest.kt` 新增
     `genericRequestAdapterShouldConvertGenericItemsAndLayers`，验证新入口可将 `GenericItem<FltX>` 与 `GenericBinLayer<FltX>` 正确收敛到模型请求。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am compile -Dgpg.skip=true` 通过。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
88. layer-generation 请求构建主路径收口（application/executor 接入）：
   - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt` 新增
     `bpp3dLayerGenerationRequest<V>(...)`（模型输入工厂），统一封装 `Bpp3dLayerGenerationRequest` 的字段组装。
   - `bpp3dLayerGenerationRequestFromGeneric<T, V>(...)` 改为委托 `bpp3dLayerGenerationRequest<V>(...)`，
     使 generic 输入路径与模型输入路径复用同一请求构建主链。
   - `bpp3d-application/src/main/.../ColumnGenerationStandardExecutors.kt` 的 `requestBuilder()` 改为调用
     `bpp3dLayerGenerationRequest(...)`，不再直接手写 `Bpp3dLayerGenerationRequest(...)` 构造。
   - `bpp3d-application/src/main/.../ColumnGenerationAlgorithm.kt` 默认分支（未注入自定义 `layerRequestBuilder` 时）
     改为调用 `bpp3dLayerGenerationRequest(...)`，与 executor 主路径保持一致。
89. 本轮补充验证（请求构建主路径收口后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
90. application 泛型请求入口补齐（减少调用侧手写 `toModel`）：
   - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt` 新增
     `columnGenerationApplicationRequestFromGeneric<T>(...)`：
     - 入参直接接收 `List<Pair<GenericItem<T>, UInt64>>`、`List<Pair<GenericMaterial<T>, UInt64>>`、
       `List<Pair<GenericMaterial<T>, Quantity<T>>>` 与 `List<GenericBinLayer<T>>`；
     - 函数内统一通过共享 `materialCache/itemCache` 转换为模型侧
       `ColumnGenerationApplicationRequest` 字段（`itemDemands`、`materialAmountDemands`、
       `materialWeightDemands`、`programMaterialCatalog`、`initialColumns`）；
     - 新增 `toInfraQuantity` 局部转换，统一将泛型重量需求收敛到 `Quantity<InfraNumber>`；
     - 对外保留 `demandEntries`、`generators`、`cgConfig`、`executorConfig` 等原语义参数，便于无缝替换现有入口。
   - `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt`：
     - `applicationRequestFactoryShouldSupportQuantityDemands` 改为调用
       `columnGenerationApplicationRequestFromGeneric(...)`，并补充断言同一 `GenericMaterial`
       在 amount/weight 转换后复用同一模型实例（`===`）；
     - `applicationServiceShouldSupportModelDemandEntryPoint` 改为通过
       `columnGenerationApplicationRequestFromGeneric(...)` 构建请求，移除测试内手写
       `materialCache/itemCache + toModel` 样板代码。
91. 本轮补充验证（application 泛型请求入口补齐后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
92. application service 泛型入口补齐（request/service 直连）：
   - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt` 新增
     `ColumnGenerationGenericApplicationRequest<T>` 与扩展
     `toModelRequest(...)`，把 generic 请求字段统一转换到模型
     `ColumnGenerationApplicationRequest`。
   - `ColumnGenerationApplicationService` 新增重载：
     `suspend fun <T : FloatingNumber<T>> solve(request: ColumnGenerationGenericApplicationRequest<T>, ...)`，
     内部直接委托现有模型入口 `solve(ColumnGenerationApplicationRequest, ...)`。
   - 现有 `columnGenerationApplicationRequestFromGeneric<T>(...)` 继续保留，用于仅做请求转换的调用场景；
     新增重载用于 application service 直接接收 generic request，减少调用端手动组装中间模型请求。
   - `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt` 的
     `applicationServiceShouldSupportModelDemandEntryPoint` 切换为
     `ColumnGenerationGenericApplicationRequest` + `service.solve(...)` 泛型入口调用。
93. 本轮补充验证（application service 泛型入口补齐后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 14, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
94. executor 泛型工厂入口补齐（application/executor 直连继续收口）：
   - `bpp3d-application/src/main/.../ColumnGenerationStandardExecutors.kt` 在 companion 新增
     `fromGenericDemandEntries<T>(...)`：
     - 入参直接接收 `List<Pair<GenericItem<T>, UInt64>>`；
     - 函数内统一通过共享 `materialCache/itemCache` 转换为模型 `itemDemands`；
     - 转换后复用现有 `fromDemandEntries(...)` 主链，保持求解行为与 demand-entry 语义不变。
   - `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt` 新增
     `standardExecutorsFactoryShouldSupportGenericItemDemands`，验证 executor 可直接由 generic item demand 构建。
95. 本轮补充验证（executor 泛型工厂入口补齐后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
96. algorithm 泛型货物入口补齐（application algorithm 直连）：
   - `bpp3d-application/src/main/.../ColumnGenerationAlgorithm.kt` 新增扩展：
     `solveGeneric<T>(items: List<GenericItem<T>>, ...)`；
     - 入参直接接收 generic item 列表；
     - 内部通过共享 `materialCache/itemCache` 转换为模型 `Item` 后复用既有
       `solve(items: List<Item>, ...)` 主链。
   - `bpp3d-application/src/test/.../ColumnGenerationAlgorithmTest.kt` 新增
     `algorithmShouldSupportGenericItemEntryPoint`，验证 algorithm 可直接接入 generic item 列表并稳定完成一轮 LP + 无列退出流程。
97. 本轮补充验证（algorithm 泛型货物入口补齐后）：
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
98. layer-generation 程序候选适配下沉到领域（候选 -> 货物）：
   - `bpp3d-domain-layer-generation-context` 新增
     `LayerGenerationProgramCandidateAdapters.kt`：
     - 增加 `MaterialPackingProgramCandidate<*>.toLayerGenerationItem(...)`；
     - 增加 `layerGenerationItemDemandsFromPrograms(...)`，统一把 `List<Pair<candidate, amount>>` 转换为 `List<Pair<Item, UInt64>>`。
   - `bpp3d-domain-layer-generation-context/pom.xml` 新增对 `bpp3d-domain-packing-context` 依赖，用于直接承载 packing candidate 到 layer-generation item 的领域转换逻辑。
   - `bpp3d-application/src/main/.../ColumnGenerationApplicationService.kt` 改为调用
     `layerGenerationItemDemandsFromPrograms(...)`，不再在 application service 内手写 `mapIndexed + toLayerGenerationItem` 转换循环。
   - 删除 `bpp3d-application/src/main/.../PackingProgramLayerCandidateAdapter.kt`，application 主源码不再保留同名桥接扩展。
   - application 侧测试调用改为直接引用 `domain.layer_generation.toLayerGenerationItem`。
99. 本轮补充验证（程序候选适配下沉后）：
   - `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationProgramCandidateAdaptersTest.kt` 新增：
     - `programCandidateShouldConvertToLayerGenerationItem`；
     - `programDemandsShouldConvertToItemDemandsInOrder`。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest,LayerGenerationProgramCandidateAdaptersTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,PackingProgramLayerCandidateAdapterTest,MaterialPackingApplicationIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 23, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
100. 主流程泛型直连补齐（analyzer + layer-generation 程序候选）：
   - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationProgramCandidateAdapters.kt` 新增：
     - `layerGenerationItemsFromPrograms(...)`，直接把程序需求展开为层生成 `items` 列表；
   - `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt` 新增：
     - `bpp3dLayerGenerationRequestFromProgramDemands(...)`；
     - `Bpp3dLayerGenerator.generateFromProgramDemands(...)`；
     - 使 layer-generation 可以直接由 `PackingProgram<V>` candidate 需求构建请求并执行；
   - `bpp3d-application/src/main/.../ColumnGenerationPackingAnalyzer.kt` 新增：
     - `ColumnGenerationPackingAnalyzer.analyzeFromGeneric(...)`；
     - 使 analyzer 可直接接收 `List<GenericBinLayer<T>>` 执行分析。
101. 本轮补充验证（主流程泛型直连补齐后）：
   - `bpp3d-domain-layer-generation-context/src/test/.../LayerGenerationProgramCandidateAdaptersTest.kt` 新增：
     - `requestFactoryShouldSupportProgramDemandsDirectly`；
     - `generatorShouldSupportProgramDemandEntryPoint`。
   - `bpp3d-application/src/test/.../ColumnGenerationPackingAnalyzerGenericEntryPointTest.kt` 新增：
     - `analyzerShouldSupportGenericLayerEntryPoint`。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest,LayerGenerationProgramCandidateAdaptersTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 15, Failures: 0, Errors: 0, Skipped: 0`）。
   - `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest,MaterialPackingApplicationIntegrationTest,PackingProgramLayerCandidateAdapterTest,ColumnGenerationPackingAnalyzerGenericEntryPointTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true` 通过（`Tests run: 24, Failures: 0, Errors: 0, Skipped: 0`）。
   - `powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d` 输出 `STRICT_GENERIC_BOUNDARY_PASS`。
102. 历史“最终状态”结论（作废）：
   - 旧记录中的“7.1 / 7.2 / 7.3 / 7.4 全部勾选”与“`refactor.md` 已最终完成”已被 2026-05-30 复核结论覆盖。
103. 2026-05-30 复核结论（当前有效）：
   - `scripts/generic-boundary-check.ps1` 已修复漏检（新增文件名扫描与中缀命名扫描），复跑结果为 `STRICT_GENERIC_BOUNDARY_FAIL: 14`；
   - `GurobiColumnGenerationTest` 在 `-Dbpp3d.gurobi.cg.test.enabled=false` 下为 `Skipped: 10`，不能作为真实 Gurobi 回归通过结论；
   - 当前不能按“纯泛型主链完成”验收，需继续按第 3/4/5 节清单推进收口。
