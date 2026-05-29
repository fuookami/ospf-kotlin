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

因此当前状态可认定为“strict scanner + compile + gurobi 回归通过”，但仍需继续完成最终验收中剩余项（默认应用全回归、FltX 关键回归、真实 CSV suite、最终完成态收口）。

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
- [ ] `Bpp3dDemandEntry<V>` 是正式主模型。
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
- [ ] 默认 application 回归通过。
- [ ] Flt64 实例化业务测试通过。
- [ ] FltX 关键业务测试通过。
- [x] `GurobiColumnGenerationTest` 通过。
- [ ] 真实 CSV `suite.paths` 通过。
- [ ] 真实 CSV `suite.dir` 通过。
- [ ] selected bin/layer、LP/MILP objective、gap、elapsed 指标仍输出。
- [ ] `final bins -> packing -> schema` 一致性断言仍通过。
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

待继续：

1. 默认 application 全量回归。
2. FltX 关键回归。
3. 真实 CSV `suite.paths` 与 `suite.dir` 回归。
4. 完成最终验收条目并将 `refactor.md` 收口为最终完成状态。
