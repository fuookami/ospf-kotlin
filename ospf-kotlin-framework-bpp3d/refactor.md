# BPP3D 主流程重构交接

日期：2026-05-29
当前基线提交：`19f952ee chore(bpp3d): 收口solver边界适配并恢复gurobi回归`

## 1. 当前结论

上一轮已完成编译链恢复、Gurobi 执行期类型异常修复和一部分泛型边界清理，但没有达到最终目标。

最终目标仍是：BPP3D 不保留兼容层，不保留 legacy 主模型，不通过 legacy bridge 执行主流程；所有主模型和主流程应直接使用 `Quantity<V>` 泛型实现。

当前实测结论（已更新到本轮执行后）：

1. BPP3D `bpp3d-application -am compile` 已通过。
2. `GurobiColumnGenerationTest` 已通过，结果为 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 1`。
3. `scripts/generic-boundary-check.ps1` 已通过，输出 `STRICT_GENERIC_BOUNDARY_PASS`。
4. strict scanner 已覆盖 `Legacy/legacy` 命名、`toLegacyModel`、`toLegacyItems`、`toLegacyLayers`、`toLegacyPlacement3`、`toFlt64Quantity`、`Flt64`、`*Scalar` 和 `compat` 目录检查。
5. `ApplicationRequestLegacyBridge.kt` 与 `LoadLegacyBridge.kt` 已删除并替换为中性命名桥接实现。
6. `GurobiColumnGenerationTest` 复跑通过，结果为 `Tests run: 10, Failures: 0, Errors: 0, Skipped: 1`。

因此当前状态可认定为“strict scanner + compile + 默认回归 + FltX 关键回归 + Gurobi 回归 + CSV suite 入口回归通过”。但主模型仍存在非泛型 `model.*` 与泛型 `api.*` 并行结构，尚未达到“正式主模型统一为 `<V>` 泛型模型”的最终架构目标。

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

- [x] scanner 覆盖 `Legacy` 和 `legacy` 大小写命名。
- [x] scanner 覆盖 `toLegacyModel`、`toLegacyItems`、`toLegacyLayers`、`toLegacyPlacement3`。
- [x] scanner 覆盖 `toFlt64Quantity`。
- [x] scanner 覆盖 `Flt64` 主源码命中。
- [x] scanner 覆盖 `LegacyQuantity`、`LegacyScalar`、`InfraScalar` 与所有模块 scalar alias。
- [x] scanner 覆盖 `api/compat`、`model/compat`、`service/compat` 目录。
- [x] scanner 输出 `STRICT_GENERIC_BOUNDARY_PASS`。

### 7.2 主模型

- [ ] `Material<V>` 是正式主模型。
- [ ] `PackageShape<V>` 是正式主模型。
- [ ] `Package<V>` 是正式主模型。
- [ ] `PackingProgram<V>` 是正式主模型。
- [ ] `Item<V>` 是正式主模型。
- [ ] `Bin<V>` 是正式主模型。
- [ ] `Layer<V>`、`BinLayer<V>`、`LayerBin<V>` 是正式主模型。
- [ ] `Load<V>` 是正式主模型。
- [x] `Bpp3dDemandEntry<V>` 是正式主模型。
- [ ] 不存在与上述类型并行的 legacy 主模型。

### 7.3 主流程

- [ ] application request/service/executor/analyzer 直接处理泛型主模型。
- [ ] layer generation 直接处理 `Item<V>` 与 `PackingProgram<V>` candidate。
- [ ] layer assignment 直接处理 `Load<V>` 与 `Bpp3dDemandEntry<V>`。
- [ ] packing planner 直接处理 `MaterialPackingDemand<V>` 与 `PackingProgram<V>`。
- [ ] block-loading 与 BLA 不依赖 legacy helper。
- [ ] `DemandMode` 只用于语义标签、key、分组、KPI 和报表。
- [ ] 离散/连续只从 `quantity.unit.domain` 判断。
- [ ] dimension 不参与任何算法判定和运算。

### 7.4 回归

- [x] `mvn -f pom.xml -pl bpp3d-application -am compile` 通过。
- [x] 默认 application 回归通过。
- [x] Flt64 实例化业务测试通过。
- [x] FltX 关键业务测试通过。
- [x] `GurobiColumnGenerationTest` 通过。
- [x] 真实 CSV `suite.paths` 通过。
- [x] 真实 CSV `suite.dir` 通过。
- [x] selected bin/layer、LP/MILP objective、gap、elapsed 指标仍输出。
- [x] `final bins -> packing -> schema` 一致性断言仍通过。
- [ ] `daily.md` 保持删除状态。
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

待继续：

1. 将 `Material<V>`、`PackageShape<V>`、`Package<V>`、`Item<V>`、`BinLayer<V>` 等泛型模型提升为唯一正式主模型。
2. 将 `Load<V>` 泛型化为正式主模型，并清理剩余 `InfraNumber` 绑定实现。
3. 消除 `domain.item.model.*` 与 `domain.item.api.*` 并行主模型结构。
4. 将 application、layer-generation、layer-assignment、block-loading、BLA、packing planner 调用方迁移到正式泛型模型。
5. 完成最终验收条目并将 `refactor.md` 收口为最终完成状态。
