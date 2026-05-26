# BPP3D 重构日报

日期：2026-05-26
最后核对时间：2026-05-27 00:37（Asia/Shanghai）

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

- [ ] `ColumnGenerationStandardExecutors` 已打通 LP/MILP 标准建模与求解适配，但尚未完成真实业务参数与外部求解器联调验证。
- [ ] `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位实现，未接入真实生成策略。
- [ ] 真实 final IP/MIP 解到 `bins` 的业务映射尚未接入（目前已具备 `final bins -> packing -> schema` 闭环接口与测试）。

## 本次核对结论

- [ ] `daily.md` 中事项未全部完成，仍有 3 项未完成（见上方“当前未完成”）。
- [ ] 本轮验证受当前工作区基础模块编译问题阻塞：
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context,bpp3d-application -am test "-Dgpg.skip=true"` 失败（`bpp3d-infrastructure` 既有未解析符号）。
  - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 失败（`bpp3d-infrastructure` 存在大量既有未解析符号）。
  - `mvn -f ospf-kotlin-framework-bpp3d/bpp3d-application/pom.xml test "-Dgpg.skip=true"` 失败（依赖模块未可用，出现连锁未解析符号）。

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
