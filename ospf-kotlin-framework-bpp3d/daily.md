# BPP3D 重构日报

日期：2026-05-26
最后核对时间：2026-05-27 15:51（Asia/Shanghai）

## 本次已完成

- [x] `ColumnGenerationAlgorithm` 已迁移到 `bpp3d-application`。
- [x] `bpp3d-domain-layer-selection-context` 已从 `ospf-kotlin-framework-bpp3d/pom.xml` 移除并删除目录。
- [x] `bpp3d-application/pom.xml` 已移除 `layer-selection` 依赖，补齐 item/bla/block-loading/layer-generation/layer-assignment/packing 依赖。
- [x] 新增 material-only demand 构造：`demandEntriesFromMaterialAmounts`、`demandEntriesFromMaterialWeights`（含泛型数量版本）。
- [x] `ItemDemandConstraint` 已泛化为 `DemandConstraint`，并保留兼容 typealias。
- [x] shadow price 提取已改为按 active demand entries（mode+key）提取。
- [x] 新增委托式 layer generation 基础接口与上下文骨架（含多个 generator 占位实现）。
- [x] `ColumnGenerationAlgorithm` 增强为标准执行器接口：
  - `ColumnGenerationRmpSolver`
  - `ColumnGenerationFinalSolver`
  - `ColumnGenerationSolutionAnalyzer`
  - `ColumnGenerationHeartbeat`
  - `ColumnGenerationLayerRequestBuilder`
- [x] layer generation 新增 shadow-price-aware 排序能力：
  - `Bpp3dLayerGenerationRequest.scoreByShadowPrice`
  - `shadowPriceAwareLayerScore(...)`
  - `Bpp3dLayerGenerationResult.numericScore`
- [x] 新增/更新测试并通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context,bpp3d-application -am test "-Dgpg.skip=true"`
  - `geometry-boundary-check.ps1`
  - `geometry-module-dry-run.ps1`
- [x] Packing final service 与 renderer DTO 适配已落地基础实现：
  - `Packer` 已可输出 `PackingResult`（含装载顺序）
  - `PackingRendererAdapter` 已可输出 `SchemaDTO`
- [x] `ColumnGenerationAlgorithm` 支持 final 结果携带 `bins`，并新增 `ColumnGenerationPackingAnalyzer`：
  - `ColumnGenerationState.bins`
  - `ColumnGenerationFinalResult.bins`
  - `ColumnGenerationPackingAnalyzer`（`final bins -> packing -> schema`）
  - `ColumnGenerationAlgorithmTest.finalSolverReturnedBinsShouldReachPackingAnalyzer`
- [x] 新增 `ColumnGenerationStandardExecutors`，将 `ColumnGenerationSolver` 对接到 application CG 的标准执行器：
  - `rmpSolver()`：基于 `ImpreciseAssignment/ImpreciseLoad/DemandConstraint/VolumeMinimization` 执行 LP，并回填 `DemandModeKey -> shadow price`
  - `finalSolver()`：基于 `PreciseAssignment/PreciseLoad` 执行 MILP，并映射回 `columns + bins`
  - `requestBuilder()`：统一构造带 `demandEntries` 和 `shadowPriceAwareLayerScore` 的 layer generation request
- [x] 新增测试 `ColumnGenerationAlgorithmTest.standardExecutorsShouldBridgeSolverToRmpAndFinal`，覆盖默认执行器的 LP dual -> shadow price 与 final bins 回传链路

## 当前未完成（后续事项）

- [ ] `ColumnGenerationStandardExecutors` 与 Gurobi 委托链路已联通，已补生产参数 + 中大规模 + mixed-demand（single/multi-bin）场景回归，并新增 `elapsed` 与 LP/MILP 分段指标（`time/gap`）断言与 CSV 批量回归入口，仍待真实业务数据下的性能场景联调验证。
- [ ] `CirclePackingLayerGenerator` 已接入基础矩形/六角排布策略，但 `cylinder.md` 路线下的真实圆柱几何、可变半径与重量价值函数尚未落地。
- [ ] final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本、schema 级与 CSV 数据驱动回归，仍待真实业务大规模场景联调与回归数据验证。

## 本次核对结论

- [ ] `daily.md` 中事项未全部完成，仍有 3 项未完成（见上方“当前未完成”）。
- [x] 本轮聚焦验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context -am test "-Dgpg.skip=true"`（BUILD SUCCESS）。
- [x] `bpp3d-application` 全量链路已复验通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（BUILD SUCCESS）。

## 2026-05-26 23:48 核对更新（Asia/Shanghai）

- [x] 已修复 `ColumnGenerationAlgorithmTest.standardExecutorsShouldBridgeSolverToRmpAndFinal`：
  - 测试 `seedLayer` 现在显式保留 `bin`，避免 `BinLayer.copy()` 导致 `bin=null` 使 `x` 变量被排除。
  - `finalBin` 使用空载容器，保证 final MILP 可分配。
  - MILP stub 解向量改为对全部 token 赋 `1`，去除对 token 命名约定的脆弱依赖。
- [x] 已验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am '-Dtest=ColumnGenerationAlgorithmTest#standardExecutorsShouldBridgeSolverToRmpAndFinal' "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）
- [ ] 后续未完成项不变：
  - 真实 LP/RMP + final IP/MIP 联调；
  - 各 layer generator 占位策略替换；
  - final IP/MIP analyzer -> packing 业务闭环接通。
- [x] 补充几何脚本验证通过：
  - `scripts/geometry-boundary-check.ps1` -> `GEOMETRY_BOUNDARY_PASS`
  - `scripts/geometry-module-dry-run.ps1` -> `GEOMETRY_MODULE_DRY_RUN_PASS`（warnings=8, internal baseline ok=8）
- [x] 23:53 后追加修复：`BinLayer.copy()` 现保留 `bin` 字段（`bpp3d-domain-item-context/.../Layer.kt`）。
- [x] 追加复验通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`
  - `scripts/geometry-boundary-check.ps1` -> `GEOMETRY_BOUNDARY_PASS`
  - `scripts/geometry-module-dry-run.ps1` -> `GEOMETRY_MODULE_DRY_RUN_PASS`（warnings=8, internal baseline ok=8）

## 2026-05-27 00:32 交接更新（Gurobi 委托，测试侧）

- [x] 本轮改动严格限制在测试侧，不影响主干业务代码：
  - `bpp3d-application/pom.xml` 仅新增测试相关配置（`gurobi-cg-test` profile、test 依赖、test source、surefire 测试开关）。
  - 新增 `src/gurobi-test/.../GurobiDelegatingColumnGenerationSolver.kt`。
  - 新增 `src/gurobi-test/.../GurobiColumnGenerationTest.kt`。
- [x] `gurobi-cg-test` profile 内容：
  - test 依赖：`io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:${project.version}`。
  - test 依赖：`gurobi:gurobi:1.0.0`。
  - 仅在 profile 下加入 `src/gurobi-test` 作为测试源码目录。
  - 设置 surefire 属性：
    - `bpp3d.gurobi.cg.test.enabled=true`
    - `ospf.kotlin.math.enableCompanionReflectionFallback=true`
- [x] 为避免历史测试产物污染默认流程，默认 surefire 已排除 `GurobiColumnGenerationTest`，主干测试不受影响。

### 本轮验证

- [x] `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"` 通过。
- [x] `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。
- [x] `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS，未执行 Gurobi 测试）。

### 交接说明（下一环境）

- [x] 若出现 `NoSuchMethodError`（`FeasibleSolverOutput` 签名不匹配），先执行：
  - `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"`
- [x] Gurobi 委托测试执行命令：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`

### 2026-05-27 00:37 提交前补充

- [x] 已完成交接文档最终核对；本次提交仅包含 `daily.md` 与 `refactor.md` 文档更新，不引入新的业务代码变更。

## 2026-05-27 09:19 续作更新（Layer Generation 实化推进）

- [x] `ComplexBlockGenerator` 已由空实现改为可用实现：
  - 支持按 X/Y/Z 方向合并 simple block（受 `Config` 开关与 predicate 控制）。
  - 合并后按空间可行性、物料数量上界、剩余承重进行过滤。
- [x] `LayerGenerationContext` 中 `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` 已接入基础 block-loading 生成链路：
  - 通过 `SimpleBlockGenerator + ComplexBlockGenerator` 生成 block 候选；
  - 将候选 block 映射为 `BinLayer`，并继续复用既有 shadow-price 排序与去重流程；
  - 当 block 链路无可行结果时，保留原有 item-based fallback。
- [x] `bpp3d-domain-layer-generation-context/pom.xml` 新增对 `bpp3d-domain-block-loading-context` 依赖。
- [x] 新增/更新测试：
  - `ComplexBlockGeneratorProofTest`（覆盖合并成功与承重约束生效）。
  - `LayerGenerationFltXProofTest.blockLayerGeneratorShouldUseBlockLoadingWhenBinProvided`（覆盖有 bin 时的 block-loading 生成路径）。
- [x] 回归验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context -am test "-Dgpg.skip=true"`（BUILD SUCCESS）。

### 仍未完成（本轮后）

- [ ] application CG 的真实 LP/RMP 与 final IP/MIP 业务求解联调尚未完成。
- [ ] `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
- [ ] final IP/MIP analyzer 到 packing 的真实业务闭环尚未打通。

## 2026-05-27 09:28 续作更新（Pattern/Pile 实化推进）

- [x] `PatternLayerGenerator` 已接入基础实化路径：
  - 在有 `bin` 时按 `item.pattern` 分组；
  - 每组优先走 block-loading 候选生成；
  - 无可行结果时回退到原有 pattern 代表项策略。
- [x] `PileLayerGenerator` 已接入基础实化路径：
  - 在有 `bin` 时按容器高度计算可堆叠上限；
  - 通过 `PackageAttribute.enabledStackingOn(...)` 校验逐层可行性；
  - 生成多层堆叠 `BinLayer`；
  - 无可行结果时回退到原有 item-based 策略。
- [x] 新增测试：
  - `LayerGenerationFltXProofTest.patternLayerGeneratorShouldUsePatternGroupedBlockLoadingWhenBinProvided`
  - `LayerGenerationFltXProofTest.pileLayerGeneratorShouldStackItemsWhenBinProvided`
- [x] 回归验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context -am test "-Dgpg.skip=true"`（BUILD SUCCESS）。

### 仍未完成（本轮后）

- [ ] application CG 的真实 LP/RMP 与 final IP/MIP 业务求解联调尚未完成。
- [ ] `CirclePackingLayerGenerator` 仍为占位策略（`Block/BLLocal/BLGlobal/Pattern/Pile` 已有基础实化，后续仍需联调强化）。
- [ ] final IP/MIP analyzer 到 packing 的真实业务闭环尚未打通。

## 2026-05-27 09:35 文档收口更新

- [x] 已完成 `daily.md` 与 `refactor.md` 的状态对齐：
  - 已实化 generator：`Block/BLLocal/BLGlobal/Pattern/Pile`；
  - 仍占位 generator：`CirclePackingLayerGenerator`（按 `cylinder.md` 路线后续推进）。
- [x] 已将顶部“当前未完成/本次核对结论”更新为截至 09:35 的真实状态，避免与历史阶段性记录混淆。

## 2026-05-27 09:45 续作更新（Application CG 编排推进）

- [x] 新增 application 级端到端执行服务：
  - `ColumnGenerationApplicationService`
  - `ColumnGenerationApplicationRequest`
  - `ColumnGenerationApplicationResponse`
  - 可统一编排 `ColumnGenerationStandardExecutors + LayerGenerationContext + (可选) ColumnGenerationPackingAnalyzer`。
- [x] 新增测试 `ColumnGenerationAlgorithmTest.applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking`，覆盖：
  - application service 到 LP dual shadow price 传递；
  - final MILP 结果到 `bins`；
  - `bins -> packing -> schema` 的 analyzer 链路。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`

### 仍未完成（本轮后）

- [ ] application CG 的真实 LP/RMP 与 final IP/MIP 业务求解联调尚未完成（当前为编排链路与 stub/测试联通）。
- [ ] `CirclePackingLayerGenerator` 仍为占位策略（按 `cylinder.md` 推进）。
- [ ] final IP/MIP analyzer 到 packing 的真实业务闭环仍待在真实业务参数下联调验证。

## 2026-05-27 09:59 续作更新（Material-only Demand 验收补齐）

- [x] `ItemDemandConstraintModeKeyTest` 新增/补强 4 条验收测试：
  - `materialAmountOnlyDemandShouldNotCreateItemAmountConstraints`
  - `materialWeightOnlyDemandShouldNotCreateItemAmountConstraints`
  - `mixedItemAmountAndMaterialWeightDemandShouldCreateBothConstraints`
  - `extractorShouldOnlyUseActiveDemandEntries`
- [x] 与既有测试共同覆盖 `refactor.md` 第 8.3 六条验收：
  - `MaterialDemandReducedCostTest.reducedCostShouldUseOnlyActiveMaterialDemandEntries`
  - `DemandStatisticsTest.patternedItemAndBinLayerStatisticsSupportMaterialAmountAndWeightModes`
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=ItemDemandConstraintModeKeyTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am test "-Dgpg.skip=true"`（4 模块 SUCCESS）

## 2026-05-27 10:08 续作更新（Application：material-weight-only packing 闭环验证）

- [x] 新增 `ColumnGenerationAlgorithmTest.applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow`，覆盖：
  - `material weight only` demand entries 贯通 LP/RMP 与 final MILP；
  - final MILP 的多层选择结果可正确映射为 `bins`；
  - `bins -> packing -> schema` 在 material-only 场景下输出正确 KPI 与 material summary。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 10:31 续作更新（Gurobi：application service 真实联调补齐）

- [x] `gurobi-cg-test` 新增 application 级真实求解测试：
  - `GurobiColumnGenerationTest.applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer`
  - 覆盖 `application service -> standard executors -> Gurobi LP/MILP -> packing analyzer` 全链路。
- [x] `GurobiColumnGenerationTest` 现有两条用例均通过：
  - `standardExecutorsShouldWorkWithGurobiDelegate`
  - `applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer`
- [x] 验证通过：
  - `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（2 tests, 0 failure）

## 2026-05-27 11:01 续作更新（CirclePackingLayerGenerator 基础实化）

- [x] `CirclePackingLayerGenerator` 从占位回退改为基础圆密排生成：
  - 有 `bin` 时按 item footprint 直径生成矩形排布（`circle-packing-rect`）。
  - 同时生成六角/错位排布（`circle-packing-hex`）。
  - 对候选层按 packed count 排序，并在同数量时优先 hex。
  - 保留无 `bin` 或无可行排布时的原有 fallback 路径。
- [x] 新增测试：
  - `LayerGenerationFltXProofTest.circlePackingLayerGeneratorShouldGeneratePackedLayersWhenBinProvided`
  - `LayerGenerationFltXProofTest.circlePackingLayerGeneratorShouldPreferHexWhenPatternCountsTie`
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（5 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 11:15 续作更新（Application：多物料多 bin packing 闭环回归）

- [x] 新增 `ColumnGenerationAlgorithmTest.applicationServiceShouldKeepPackingConsistentForMultiMaterialMultiBinScenario`，覆盖：
  - 混合 demand（`item amount + material weight`）在 application service 中贯通 LP/RMP 与 final MILP。
  - `final MILP -> bins -> packing -> schema` 在多物料、多列、多 bin 输入下输出稳定。
  - `material summary` 与 `schema.kpi`（`bin_count/material_count`）一致性校验。
- [x] 补充校验了 `PreciseAssignment` 的 bin-layer 绑定语义（`layer.bin` 与 `bin.shape` 引用匹配）对应的装载结果断言口径。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldKeepPackingConsistentForMultiMaterialMultiBinScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 12:14 续作更新（Gurobi：生产参数 + 中等规模场景验证）

- [x] `GurobiColumnGenerationTest` 新增用例：
  - `applicationServiceShouldHandleProductionLikeConfigOnMediumScaleScenario`
  - 场景覆盖：`SolverConfig(time/threadNum/gap/notImprovementTime)` + 24 item（4 material）+ 2 layer + packing analyzer 闭环。
- [x] `layerBin(...)` 新增 `binType` 与 `widthInMeter` 参数，用于复用同一 `BinType` 并构造更宽层布局，避免多场景测试中的 bin-layer 绑定偏差。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeConfigOnMediumScaleScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（4 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 12:21 续作更新（Application：packing analyzer 大样本回归）

- [x] `ColumnGenerationAlgorithmTest` 新增用例：
  - `applicationServiceShouldKeepPackingConsistentForLargeMaterialBatchScenario`
  - 场景覆盖：24 layer + 24 final bins + 4 materials 的大样本 `final bins -> packing -> schema` 一致性校验。
- [x] `ColumnGenerationAlgorithmTest.layerBin(...)` 新增 `typeCode` 参数，便于构造多 bin 大样本测试数据并保持 bin-layer 绑定语义稳定。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldKeepPackingConsistentForLargeMaterialBatchScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 12:56 续作更新（Gurobi：生产参数 + 大规模场景验证）

- [x] `GurobiColumnGenerationTest` 新增大规模用例：
  - `applicationServiceShouldHandleProductionLikeConfigOnLargeScaleScenario`
  - 场景覆盖：60 item（6 material）+ 6 layer + `SolverConfig(time=30s, threadNum=4, gap=0.01, notImprovementTime=10s)` + packing analyzer 闭环。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeConfigOnLargeScaleScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（5 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 13:13 续作更新（Gurobi：large-scale mixed-demand 回归）

- [x] `GurobiColumnGenerationTest` 新增用例：
  - `applicationServiceShouldHandleLargeScaleMixedDemandScenario`
  - 场景覆盖：60 item（5 material）+ 6 layer + `item demand + material amount demand` 混合建模 + Gurobi LP/MILP + packing analyzer 闭环。
- [x] 回归策略说明：
  - 曾尝试 `multi-bin + mixed-demand`，在当前模型口径下出现 `ORModelInfeasible`；
  - 本轮先收敛为可稳定通过的 `single-bin + large-scale + mixed-demand` 场景，保障持续可回归。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleLargeScaleMixedDemandScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（6 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 14:01 续作更新（mixed-demand multi-bin 可行性修复）

- [x] 修复 `PreciseLoad` 的多 bin 聚合口径问题：
  - 根因：`PreciseLoad` 里 load 表达式使用 `assignment.x[layer]`（二维变量线性索引），多 bin 场景只统计到首行变量，导致 mixed-demand final MILP 可行解被误判为 infeasible。
  - 修复：改为按 `assignment.x[binIndex, layerIndex]` 对全部 bin 显式求和。
- [x] `GurobiColumnGenerationTest.applicationServiceShouldHandleLargeScaleMixedDemandScenario` 已恢复为真正的 multi-bin 回归（`groupCount=2`），不再退化为 single-bin 兜底。
- [x] 新增 domain 级回归测试 `PreciseLoadMultiBinAggregationTest`，锁定“同一 layer 在多 bin 上的 x 变量都进入 load”。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=PreciseLoadMultiBinAggregationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am test "-Dgpg.skip=true"`（4 模块 SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（6 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 14:26 续作更新（Gurobi：性能基线 + schema 指标回归）

- [x] `ColumnGenerationResult` 新增 `elapsed: Duration`，application CG 可直接输出单次求解总耗时。
- [x] `GurobiColumnGenerationTest.applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario` 补强断言：
  - `result.elapsed > 0s` 且 `result.elapsed <= 120s`（生产参数大规模场景性能基线）。
  - `schema.loadingPlans` 的 bin 数与 item 数与 packing 聚合一致。
  - `schema.kpi["cg_iteration"]` 与 `result.iterationCount` 一致。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（7 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 14:39 续作更新（CG 结果指标透传：LP/MILP 分段）

- [x] `ColumnGenerationResult` 新增求解指标透传：
  - `lpInfos: List<Map<String, String>>`
  - `finalInfo: Map<String, String>`
- [x] `ColumnGenerationStandardExecutors` 增强求解信息：
  - LP：`lp_time_ms`、`lp_gap`、`lp_objective`
  - MILP：`milp_time_ms`、`milp_gap`、`milp_objective`、`selected_bin_count`、`selected_layer_count`
- [x] `GurobiColumnGenerationTest.applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario` 新增断言：
  - `result.lpInfos` 与 `result.finalInfo` 中包含 solver/time/gap 指标；
  - `selected_bin_count` / `selected_layer_count` 与 packing 统计一致。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（7 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 14:57 续作更新（Gurobi：CSV 数据驱动回归入口）

- [x] 新增回归数据文件：
  - `bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv`
  - 字段：`group_index,layer_index,item_id,material_no,material_name,material_weight_kg`
- [x] `GurobiColumnGenerationTest` 新增 CSV 场景构造器：
  - `loadCsvDrivenScenario(resourcePath)`，可从资源文件构造 `itemDemands/demandEntries/initialColumns/finalBins`；
  - demand 模式包含 `item + material amount + material weight` 三类约束。
- [x] 新增用例：
  - `applicationServiceShouldSupportCsvDrivenProductionLikeScenario`
  - 覆盖 `CSV dataset -> application service -> Gurobi LP/MILP -> packing analyzer` 全链路。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（8 tests, 0 failure）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 15:12 续作更新（CSV 外部文件开关联调）

- [x] `applicationServiceShouldSupportCsvDrivenProductionLikeScenario` 已支持：
  - 优先读取 `-Dbpp3d.gurobi.dataset.path=...` 指定的外部 CSV；
  - 未指定时回退内置资源 `gurobi/production-like-dataset.csv`。
- [x] 外部文件路径模式验证通过（PowerShell 需整体引用 `-D` 参数）：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test "-Dbpp3d.gurobi.dataset.path=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv" -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
- [x] 默认资源模式与全类回归复验通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（8 tests, 0 failure）

## 2026-05-27 15:35 续作更新（CSV 批量回归入口 + CG 主循环验收收口）

- [x] `GurobiColumnGenerationTest` 新增可选批量数据集回归用例：
  - `applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite`
  - 仅在 `-Dbpp3d.gurobi.dataset.suite.enabled=true` 时执行，默认不影响常规回归耗时。
- [x] 批量数据集来源新增两种入口：
  - `-Dbpp3d.gurobi.dataset.suite.paths=...`（多文件，逗号/分号分隔）
  - `-Dbpp3d.gurobi.dataset.suite.dir=...`（目录扫描 `.csv`）
- [x] 批量回归新增阈值参数：
  - `bpp3d.gurobi.dataset.suite.max.elapsed.seconds`
  - `bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds`
  - `bpp3d.gurobi.dataset.suite.expected.case.count`
- [x] `ColumnGenerationAlgorithmTest` 新增用例：
  - `algorithmShouldCompleteLpSpAddColumnAndFinalMilpFlow`
  - 覆盖并锁定 `初始列 -> LP/SP -> 加列 -> final MILP` 主循环闭环。
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test "-Dbpp3d.gurobi.dataset.suite.enabled=true" "-Dbpp3d.gurobi.dataset.suite.paths=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv" -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 tests, 0 failure, 1 skipped）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）

## 2026-05-27 15:51 续作更新（Gurobi：内置随机场景集成测试）

- [x] `GurobiColumnGenerationTest` 新增可复现随机场景生成器：
  - `createDeterministicRandomCsvDrivenScenarioCases(seed, caseCount)`；
  - 由固定 seed 构造随机 CSV 文本，再复用 `loadCsvDrivenScenarioFromCsvText(...)` 构建完整场景。
- [x] 新增集成测试：
  - `applicationServiceShouldSupportDeterministicRandomScenarioSuite`
  - 该用例默认参与 `gurobi-cg-test`，不依赖外部文件，覆盖 `随机数据 -> application service -> Gurobi LP/MILP -> packing analyzer` 闭环。
- [x] 可选参数：
  - `bpp3d.gurobi.random.dataset.seed`
  - `bpp3d.gurobi.random.dataset.case.count`
  - `bpp3d.gurobi.random.dataset.max.elapsed.seconds`
  - `bpp3d.gurobi.random.dataset.max.total.elapsed.seconds`
- [x] 验证通过：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportDeterministicRandomScenarioSuite "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（10 tests, 0 failure, 1 skipped）
- [x] 补充说明：
  - 并行执行两个 Maven 构建会触发 `target/generated-sources/annotations` 临时目录竞争，串行执行后稳定通过（非代码缺陷）。
