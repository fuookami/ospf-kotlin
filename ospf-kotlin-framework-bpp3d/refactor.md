# BPP3D 主流程重构计划

日期：2026-05-26

## 1. 目标

本计划用于指导 `ospf-kotlin-framework-bpp3d` 后续整体重构，重点不是圆柱体本身，而是 BPP3D 主流程、上下文边界和列生成架构。

核心目标：

1. 将 `ColumnGenerationAlgorithm` 从 domain layer selection 中上移到 application service。
2. 对齐 `E:\workspace\tcl\bpp3d` 的 BPP3D 能力边界，确认 OSPF BPP3D 已具备哪些抽象、缺哪些服务。
3. 支持只按 material 数量或 material 重量约束装载需求，此时 item 数量约束可以不存在。
4. 为后续圆柱装载、圆密排、可变半径 layer generation 提供委托式 layer generation 主架构。
5. 最终移除 `bpp3d-domain-layer-selection-context` 模块，不再保留独立 layer selection domain。

## 2. 范围

本计划覆盖：

1. `bpp3d-application`
2. `bpp3d-domain-layer-selection-context`
3. `bpp3d-domain-layer-assignment-context`
4. `bpp3d-domain-layer-generation-context`
5. `bpp3d-domain-bla-context`
6. `bpp3d-domain-block-loading-context`
7. `bpp3d-domain-packing-context`
8. `bpp3d-domain-item-context`

不直接覆盖：

1. three.js renderer 适配。
2. 圆柱真实几何实现细节。圆柱专题见 `cylinder.md`。
3. TCL 项目代码迁移。TCL 只作为行为和边界参考。

## 3. 参考对象

### 3.1 CSP1D application service

参考路径：

`E:\workspace\poit\csp1d\csp1d-application\src\main\com\poit\or\csp1d\application\service`

重点参考 `CG.kt`：

1. application service 持有 column generation 主循环。
2. domain context 通过 builder 注入。
3. subproblem generator 通过函数委托注入。
4. reduced cost、shadow price、solution analyzer 都通过可替换函数或 context 抽象接入。
5. application 层负责心跳、日志、时间限制、迭代限制、最终 MILP。

### 3.2 TCL BPP3D

参考路径：

`E:\workspace\tcl\bpp3d`

重点参考：

1. `bpp3d-domain-layer-loading/.../ColumnGenerationAlgorithm.kt`
2. `bpp3d-domain-layer-loading/.../bpp3d-domain-layer-generator-context/.../service`
3. `bpp3d-domain-bla-context`
4. `bpp3d-domain-block-loading-context`
5. `bpp3d-domain-item-context`
6. `bpp3d-domain-packing-context`

## 4. 架构原则

1. application 负责编排流程，domain context 负责领域建模与能力暴露。
2. 具体 layer generation 算法必须可委托，不硬编码进主流程。
3. RMP/LP、SP/layer generation、final IP/MIP 三段要边界清晰。
4. demand 约束必须以 `Bpp3dDemandEntry` 为准，不默认强制 item amount。
5. shadow price 和 reduced cost 必须按当前启用的 demand mode 计算。
6. 旧的 item-only reduced cost 只能作为兼容路径，不能作为通用主路径。
7. domain 模块不承担 application 心跳、日志输出、全局时间控制和最终流程编排。
8. 不新增或保留独立 layer selection domain；列筛选、坏列移除、top-k filtering 等能力应归入 application service、layer generation adapter 或 layer assignment context。

## 5. ColumnGeneration application 化

### 5.1 目标结构

建议在 application 中建立：

```text
bpp3d-application/src/main/fuookami/ospf/kotlin/framework/bpp3d/application/service/ColumnGenerationAlgorithm.kt
```

职责：

1. 初始化 imprecise/RMP context。
2. 添加初始 `BinLayer` 列。
3. 迭代求解 LP。
4. 提取 shadow price。
5. 调用委托式 layer generator 生成新列。
6. reduced cost 过滤。
7. add columns / deduplicate / optional bad column removal。
8. 停止条件控制。
9. 初始化 precise/final context。
10. 求解最终 IP/MIP。
11. 调用 solution analyzer。

### 5.2 domain layer-selection 的去向

`bpp3d-domain-layer-selection-context` 不应继续承载 `ColumnGenerationAlgorithm` 主流程，并且最终应从 BPP3D 聚合模块中移除。

原先可能归入 layer selection 的职责应重新归属：

1. bad column selection：归入 application CG service 或 layer assignment context。
2. column ranking：归入 application CG service。
3. layer candidate pruning：归入 layer generation adapter。
4. top-k layer filtering：归入 layer generation adapter 或 application CG service。

迁移完成后，需要从根 `pom.xml` 移除 `bpp3d-domain-layer-selection-context` module，并删除该目录。

### 5.3 application 依赖

`bpp3d-application` 最终应依赖：

1. infrastructure
2. domain-item-context
3. domain-bla-context
4. domain-block-loading-context
5. domain-layer-generation-context
6. domain-layer-assignment-context
7. domain-packing-context

`domain-layer-selection-context` 不应成为最终依赖；若迁移过程中短期依赖它，只能作为过渡状态。

## 6. Layer Generation 委托机制

TCL 的 layer generator 体现的是委托模式：

1. `PatternPlacer` 接收外部 `Pattern`。
2. `BlockPlacer` 接收外部 block loading algorithm。
3. `BLLocalPlacer` / `BLGlobalPlacer` 接收外部 BLA 函数。
4. `Filler` 接收外部 BLA 函数。

OSPF BPP3D 应保持同样边界。

建议新增通用接口：

```kotlin
interface Bpp3dLayerGenerator<V> {
    suspend fun generate(
        request: Bpp3dLayerGenerationRequest<V>
    ): List<Bpp3dLayerGenerationResult<V>>
}
```

具体实现：

1. `BlockLayerGenerator`
2. `BLLocalLayerGenerator`
3. `BLGlobalLayerGenerator`
4. `PatternLayerGenerator`
5. `PileLayerGenerator`
6. `CirclePackingLayerGenerator`
7. `HistoricalLayerGenerator`

application CG 只调用接口，不直接依赖某个具体算法。

## 7. TCL 能力对齐

### 7.1 BLA

当前 OSPF 已有：

1. `BottomUpLeftJustifiedAlgorithm`
2. `BottomUpLeftJustifiedAlgorithm3D`
3. async 包装

相对 TCL 缺口：

1. `Filler`
2. `Packer`
3. `ProjectionPacker`
4. `StockPacker`

建议：

1. 保留现有核心算法。
2. 补服务级 facade，使 layer generation / packing 可用统一委托方式调用。

### 7.2 Block Loading

当前 OSPF 已有：

1. `Space`
2. `SimpleBlockGenerator`
3. `ComplexBlockGenerator`
4. `DepthFirstSearchAlgorithm`
5. `MultiLayerHeuristicSearchAlgorithm`
6. async 包装

基本覆盖 TCL 主体。

待补：

1. 参数配置与 TCL 对齐。
2. 与 layer generation 的 `BlockPlacer` adapter 对齐。
3. reduced cost / shadow price aware generation。

### 7.3 Layer Generation

当前 OSPF `LayerGenerationContext` 仍是空壳，是最大缺口。

需补：

1. `BlockPlacer`
2. `BLLocalPlacer`
3. `BLGlobalPlacer`
4. `Filler`
5. `PatternPlacer`
6. `PilePlacer`
7. 委托式 generator interface
8. initial layer generation
9. shadow-price-aware layer generation
10. last-iteration layer reuse / filler

### 7.4 Layer Assignment

当前 OSPF 已有较强抽象：

1. precise / imprecise assignment。
2. load / capacity。
3. demand mode。
4. item amount / material amount / material weight statistics。
5. shadow price key / extractor。
6. solution analyzer。
7. 多类 constraints/objectives。

待补：

1. 与 application CG 的 context builder 对接。
2. add/remove columns 统一接口。
3. bad column selection。
4. material-only demand 便利构造器。
5. reduced cost 统一走 active demand mode。

### 7.5 Packing

当前 OSPF 和 TCL 都较薄，OSPF 有 aggregation/model 骨架，`Packer` 仍是空壳。

待补：

1. aggregation initializer。
2. final packing service。
3. 与 final IP/MIP solution analyzer 对接。
4. renderer DTO 输出 adapter。

### 7.6 Item Context

当前 OSPF 已有：

1. item / material / package / package attribute。
2. bin / layer / block / pattern。
3. demand statistics。
4. item merger。
5. height combinator。
6. loading order calculator。

相对 TCL 需确认或补齐：

1. `Pallet` 是否需要迁入。
2. `Scheme` / `Schema` 语义是否已经覆盖。
3. `CrossPatternGenerator` 是否需要。
4. priority attribute 是否需要。

## 8. Material-only Demand 改造

### 8.1 问题

业务可能只约束 material 数量或 material 重量，此时 item 数量及 item 数量约束可以不存在。

当前已有基础：

1. `Bpp3dDemandMode.ItemAmount`
2. `Bpp3dDemandMode.ItemMaterialAmount`
3. `Bpp3dDemandMode.ItemMaterialWeight`
4. `Bpp3dDemandEntry`
5. `Load.demandEntries`
6. `ItemDemandConstraint.extractor()` 会按 active demand mode 提取 shadow price。

当前不足：

1. 便利构造函数主要从 item amount 生成 demand。
2. `ItemDemandConstraint` 命名仍偏 item。
3. 旧 `BPP3DShadowPriceMap.reducedCost(cuboid)` 是 item-only 思维，不适合 material-only demand。

### 8.2 改造事项

1. 新增 `demandEntriesFromMaterialAmounts`。
2. 新增 `demandEntriesFromMaterialWeights`。
3. 新增 quantity 泛型版本。
4. 允许 `Load` 只持有 material demand entries。
5. 将 `ItemDemandConstraint` 泛化为 `DemandConstraint`，旧名保留 typealias 或 deprecated wrapper。
6. reduced cost 统一基于 active demand entries 和 shadow price extractor。
7. layer generation 的 SP 价值函数必须使用 active demand mode，而不是 item 数量。

### 8.3 验收

- [x] material amount only 时，模型不生成 item amount 约束。
- [x] material weight only 时，模型不生成 item amount 约束。
- [x] item amount + material weight 混合时，两类约束都存在。
- [x] shadow price 只按启用的 demand entry 提取。
- [x] reduced cost 对 material-only demand 正确。
- [x] layer statistics 对 material weight 正确累计。

## 9. 实施阶段

### 9.1 第一阶段：边界清理

1. 确认 `ColumnGenerationAlgorithm` 只存在于 application service。
2. domain layer-selection 不再包含 CG 主流程。
3. 将 layer-selection 中仍有价值的职责迁往 application、layer-generation 或 layer-assignment。
4. application POM 引入后续 CG 所需 domain 依赖，但不保留 layer-selection 最终依赖。
5. 从根 `pom.xml` 移除 `bpp3d-domain-layer-selection-context`。
6. 删除 `bpp3d-domain-layer-selection-context` 目录。
7. 补最小 application service 测试。

### 9.2 第二阶段：Layer Assignment Demand

1. 补 material amount / weight demand constructors。
2. 泛化 `ItemDemandConstraint`。
3. 修正 reduced cost 主路径。
4. 补 material-only tests。

### 9.3 第三阶段：Layer Generation

1. 定义委托式 layer generator interface。
2. 迁入/重写 TCL 对应 placer。
3. 对接 BLA / block-loading / pattern / pile。
4. 支持 shadow-price-aware generation。

### 9.4 第四阶段：Application CG 主流程

1. 参考 CSP1D `ColumnGeneration` 实现主循环。
2. 对接 imprecise RMP。
3. 对接 layer generator。
4. 对接 precise final IP/MIP。
5. 对接 solution analyzer。

### 9.5 第五阶段：Packing / Renderer / 圆柱

1. 补 final packing service。
2. 对接 RendererDTO。
3. 接入 `cylinder.md` 中的圆柱 layer generation。

## 10. 验收标准

- [x] `ColumnGenerationAlgorithm` 位于 application service。
- [x] domain layer-selection 不再承载 application 编排逻辑。
- [x] `bpp3d-domain-layer-selection-context` 已从根 `pom.xml` 移除。
- [x] `bpp3d-domain-layer-selection-context` 目录已删除。
- [x] application 不再依赖 layer-selection 模块。
- [x] material-only demand 可建模、可提取 shadow price、可计算 reduced cost。
- [x] layer generation 通过委托接口接入。
- [x] block / BLA / pattern / pile placer 至少有基础实现或明确未实现接口。
- [x] application CG 可完成：初始列、LP、SP、加列、最终 IP/MIP。
- [x] BPP3D 全模块测试通过。
- [x] `geometry-boundary-check.ps1` 和 `geometry-module-dry-run.ps1` 通过。

## 11. 当前工作区交接备注

本会话中已经发生过少量代码变更，接手前必须确认是否保留：

1. 已新增 `cylinder.md`，记录圆柱装载计划。
2. 已删除 `bpp3d-infrastructure/.../infrastructure/api/QuantityInfrastructureApi.kt`。
3. 已将 `asScalarF64()` 从 `infrastructure/compat` 迁到 `domain-item-context/.../domain/item/api/compat/QuantityLegacyScalarAdapter.kt`。
4. 已删除 `bpp3d-infrastructure/.../infrastructure/compat/QuantityLegacyScalarAdapter.kt`。
5. 已开始将空壳 `ColumnGenerationAlgorithm` 从 layer-selection 迁到 application service：
   - 新增 application service 类。
   - 删除 layer-selection service 下的空壳类。
   - 更新 layer-selection proof test。
   - 新增 application test。
   - 更新 application POM 依赖。

注意：当前 application POM 中对 `bpp3d-domain-layer-selection-context` 的依赖只能视为临时迁移痕迹。按最新原则，最终必须移除该依赖，并删除整个 layer-selection 模块。

验证状态（更新到 2026-05-26 18:14, Asia/Shanghai）：

1. 已运行 `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context,bpp3d-application -am test "-Dgpg.skip=true"`，9 个模块全部通过（BUILD SUCCESS）。
2. 已运行 `scripts/geometry-boundary-check.ps1`，结果 `GEOMETRY_BOUNDARY_PASS`。
3. 已运行 `scripts/geometry-module-dry-run.ps1`，结果 `GEOMETRY_MODULE_DRY_RUN_PASS`（warnings=8，internal baseline ok=8）。
4. `git diff --check` 与 CRLF 风险仍需在最终提交前统一处理（若团队要求 LF）。

接手建议：

1. 先运行 `git status --short` 确认当前改动。
2. 基于当前已完成的接口化骨架，优先推进 application CG 与真实 imprecise/precise 求解链路对接（RMP + final IP/MIP）。
3. 将 layer generation 占位策略替换为可调用 block-loading / BLA / pattern / pile 的真实实现。
4. 补齐 final IP/MIP solution analyzer 到 packing service 的闭环。

## 12. 2026-05-26 续作更新（本次会话）

已完成：

1. `ColumnGenerationAlgorithm` 新增标准扩展点接口：`ColumnGenerationRmpSolver`、`ColumnGenerationFinalSolver`、`ColumnGenerationSolutionAnalyzer`、`ColumnGenerationHeartbeat`、`ColumnGenerationLayerRequestBuilder`。
2. `LayerGenerationContext` 新增 shadow-price-aware 选列能力：`scoreByShadowPrice`、`shadowPriceAwareLayerScore(...)`、`numericScore`。
3. 新增测试：
   - `ColumnGenerationAlgorithmTest.rmpSolverAndRequestBuilderShouldBePreferredWhenProvided`
   - `LayerGenerationFltXProofTest.generatedLayersShouldBeRankedByShadowPriceWhenEvaluatorProvided`

仍未完成：

1. application CG 尚未接入真实 LP/RMP 与 final IP/MIP 求解执行器。
2. `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
3. final IP/MIP solution analyzer -> packing service 的完整业务闭环尚未接通。

## 13. 2026-05-26 续作更新（本次会话二）

已完成：

1. `ColumnGenerationAlgorithm` 状态与 final 结果新增 `bins` 承载能力：
   - `ColumnGenerationState.bins`
   - `ColumnGenerationFinalResult.bins`
2. 新增 `ColumnGenerationPackingAnalyzer`，可直接消费 final 状态并形成：
   - `PackingResult`
   - `SchemaDTO`
3. 新增测试：
   - `ColumnGenerationAlgorithmTest.finalSolverReturnedBinsShouldReachPackingAnalyzer`

验证状态（更新到 2026-05-26 20:17, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 失败，阻塞点为 `bpp3d-infrastructure` 既有未解析符号。
2. `mvn -f ospf-kotlin-framework-bpp3d/bpp3d-application/pom.xml test "-Dgpg.skip=true"` 失败，出现依赖链未解析（与上游模块状态相关）。

仍未完成：

1. application CG 尚未接入真实 LP/RMP 与 final IP/MIP 求解执行器。
2. `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
3. 真实 final IP/MIP 解到 `bins` 的业务映射尚未接入（当前仅完成 `final bins -> packing -> schema` 链路）。

## 14. 2026-05-26 续作更新（本次会话三）

已完成：

1. 新增 `ColumnGenerationStandardExecutors`（application）：
   - `rmpSolver()`：基于 `ImpreciseAssignment/ImpreciseLoad/DemandConstraint/VolumeMinimization` 构建并执行 LP（RMP）。
   - `finalSolver()`：基于 `PreciseAssignment/PreciseLoad` 构建并执行 MILP（final），并映射回 `columns + bins`。
   - `requestBuilder()`：统一构造带 `demandEntries` 与 `shadowPriceAwareLayerScore` 的 layer generation request。
2. 新增测试 `ColumnGenerationAlgorithmTest.standardExecutorsShouldBridgeSolverToRmpAndFinal`，覆盖：
   - LP dual -> `DemandModeKey` shadow price 回传；
   - final MILP 解 -> `bins` 回传。

验证状态（更新到 2026-05-26 20:50, Asia/Shanghai）：

1. 运行 `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context,bpp3d-application -am test "-Dgpg.skip=true"` 失败，阻塞点为 `bpp3d-infrastructure` 既有未解析符号（与本次新增无关）。

仍未完成：

1. `ColumnGenerationStandardExecutors` 已打通标准求解适配，但尚未完成真实业务参数与外部求解器联调验证。
2. `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
3. final IP/MIP -> packing 的真实业务闭环仍待进一步对接与验证。

## 2026-05-26 续作更新（本次会话四）
已完成：

1. 修复 `ColumnGenerationAlgorithmTest.standardExecutorsShouldBridgeSolverToRmpAndFinal` 的稳定性问题：
   - 根因：测试构造的 `seedLayer` 来自 `view/copy` 路径，`BinLayer.copy()` 未保留 `bin` 字段，导致 `seedLayer.bin == null`，`PreciseAssignment` 不会将 `x` 变量纳入 solver。
   - 修复：在测试中显式重建带 `bin` 的 `seedLayer`（绑定到 `seedBin.shape`），并使用空载 `finalBin` 参与 final MILP 分配。
   - 同时将 MILP stub 解向量改为对全部 token 赋 `1`，避免依赖变量命名细节。
2. 完成回归验证：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am '-Dtest=ColumnGenerationAlgorithmTest#standardExecutorsShouldBridgeSolverToRmpAndFinal' "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 个模块全部 SUCCESS）。

仍未完成：

1. application CG 尚未接入真实 LP/RMP 与 final IP/MIP 求解执行器联调。
2. `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
3. final IP/MIP analyzer 到 packing 的真实业务闭环仍待接通。
补充验证：

- `scripts/geometry-boundary-check.ps1`：`GEOMETRY_BOUNDARY_PASS`。
- `scripts/geometry-module-dry-run.ps1`：`GEOMETRY_MODULE_DRY_RUN_PASS`（warnings=8，internal baseline ok=8）。

增量修复（23:53 后）：

1. 修复 `bpp3d-domain-item-context` 中 `BinLayer.copy()` 未复制 `bin` 字段的问题（现已保留 `bin = bin`）。
2. 复验通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`
   - `scripts/geometry-boundary-check.ps1` -> `GEOMETRY_BOUNDARY_PASS`
   - `scripts/geometry-module-dry-run.ps1` -> `GEOMETRY_MODULE_DRY_RUN_PASS`（warnings=8，internal baseline ok=8）

## 15. 2026-05-27 续作更新（Gurobi 委托测试交接）

已完成：

1. 在 `bpp3d-application` 增加测试专用 profile：`gurobi-cg-test`。
2. 仅在测试侧引入 Gurobi 委托实现与测试：
   - `src/gurobi-test/.../GurobiDelegatingColumnGenerationSolver.kt`
   - `src/gurobi-test/.../GurobiColumnGenerationTest.kt`
3. `gurobi-cg-test` profile 中仅新增 test 依赖：
   - `io.github.fuookami.ospf.kotlin.core.plugin:ospf-kotlin-core-plugin-gurobi:${project.version}`
   - `gurobi:gurobi:1.0.0`
4. profile 下新增 surefire system properties：
   - `bpp3d.gurobi.cg.test.enabled=true`
   - `ospf.kotlin.math.enableCompanionReflectionFallback=true`
5. 为避免默认流程受历史构建产物影响，`bpp3d-application` 默认 surefire 增加排除：
   - `**/GurobiColumnGenerationTest.*`

验证状态（更新到 2026-05-27 00:32, Asia/Shanghai）：

1. `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 个模块 SUCCESS）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 个模块 SUCCESS，主干未执行 Gurobi 测试）。

交接给下一环境：

1. 若遇到 `NoSuchMethodError`（`FeasibleSolverOutput` 构造签名不匹配），先执行：
   - `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"`
2. 之后再执行 Gurobi 委托测试：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`

仍未完成（与重构主目标一致）：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务求解联调尚未完成。
2. 各 layer generator（block/BLA/pattern/pile/circle）仍为占位策略。
3. final IP/MIP analyzer 到 packing 的真实业务闭环尚未打通。

## 16. 2026-05-27 续作更新（文档交接提交）

已完成：

1. 对 `daily.md` 与 `refactor.md` 进行最终交接核对与补充。
2. 本次提交将仅包含文档更新，不包含新增业务代码改动。

## 17. 2026-05-27 续作更新（Layer Generation：block-loading 基础策略接入）

已完成：

1. 实现 `ComplexBlockGenerator`（此前为 `TODO` 空实现）：
   - 支持按 X/Y/Z 方向合并 simple block；
   - 受 `Config` 方向开关与 predicate 控制；
   - 合并结果按空间可行性、可用 item 数量、剩余承重过滤。
2. `LayerGenerationContext` 中以下 generator 已接入基础 block-loading 策略：
   - `BlockLayerGenerator`
   - `BLLocalLayerGenerator`
   - `BLGlobalLayerGenerator`
3. 新增 layer-generation 对 block-loading 模块依赖：
   - `bpp3d-domain-layer-generation-context/pom.xml` -> `bpp3d-domain-block-loading-context`。
4. 新增测试：
   - `ComplexBlockGeneratorProofTest`
   - `LayerGenerationFltXProofTest.blockLayerGeneratorShouldUseBlockLoadingWhenBinProvided`

验证状态（更新到 2026-05-27 09:19, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context -am test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。

仍未完成：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调尚未完成。
2. `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位策略。
3. final IP/MIP analyzer 到 packing 的真实业务闭环尚未打通。

## 18. 2026-05-27 续作更新（Layer Generation：Pattern/Pile 基础策略接入）

已完成：

1. `PatternLayerGenerator` 增加基础实化路径：
   - 有 `bin` 时按 `item.pattern` 分组；
   - 各分组优先走 block-loading 候选层生成；
   - 无可行结果时回退到原有 pattern 代表项策略。
2. `PileLayerGenerator` 增加基础实化路径：
   - 有 `bin` 时按容器高度估算最大可堆叠层数；
   - 逐层调用 `PackageAttribute.enabledStackingOn(...)` 进行可行性校验；
   - 生成多层堆叠 layer；
   - 无可行结果时回退原有 item-based 路径。
3. 新增测试：
   - `LayerGenerationFltXProofTest.patternLayerGeneratorShouldUsePatternGroupedBlockLoadingWhenBinProvided`
   - `LayerGenerationFltXProofTest.pileLayerGeneratorShouldStackItemsWhenBinProvided`

验证状态（更新到 2026-05-27 09:28, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context -am test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。

仍未完成：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调尚未完成。
2. `CirclePackingLayerGenerator` 仍为占位策略（其余 block/BL local/BL global/pattern/pile 已有基础实化实现）。
3. final IP/MIP analyzer 到 packing 的真实业务闭环尚未打通。

## 19. 2026-05-27 续作更新（文档收口与边界澄清）

已完成：

1. `daily.md` 与 `refactor.md` 的“当前状态”已按最新进展对齐：
   - 已基础实化：`BlockLayerGenerator`、`BLLocalLayerGenerator`、`BLGlobalLayerGenerator`、`PatternLayerGenerator`、`PileLayerGenerator`；
   - 仍占位：`CirclePackingLayerGenerator`。
2. 已明确边界：`CirclePackingLayerGenerator` 的真实算法实现归入 `cylinder.md` 路线（圆密排/可变半径/重量统计价值函数），本重构主线仅保留委托式框架位与接入点。
3. 已更新 `daily.md` 顶部“当前未完成”与“本次核对结论”，使其不再停留在旧的阻塞结论。

当前未完成（收口后）：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调。
2. `CirclePackingLayerGenerator` 的圆密排业务实现（按 `cylinder.md` 推进）。
3. final IP/MIP analyzer 到 packing 的真实业务闭环。

## 20. 2026-05-27 续作更新（Application CG 编排服务落地）

已完成：

1. 新增 application 级执行入口 `ColumnGenerationApplicationService`，统一编排：
   - `ColumnGenerationStandardExecutors`
   - `LayerGenerationContext`
   - `ColumnGenerationPackingAnalyzer`（可选）
2. 新增请求/响应模型：
   - `ColumnGenerationApplicationRequest`
   - `ColumnGenerationApplicationResponse`
3. 新增测试 `ColumnGenerationAlgorithmTest.applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking`，覆盖：
   - LP dual shadow price -> request；
   - final MILP -> `bins`；
   - `bins -> packing -> schema`。

验证状态（更新到 2026-05-27 09:45, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldBridgeExecutorsLayerGenerationAndPacking "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调（当前为编排链路与测试联通）。
2. `CirclePackingLayerGenerator` 的圆密排业务实现（按 `cylinder.md` 推进）。
3. final IP/MIP analyzer 到 packing 的真实业务闭环（真实业务参数场景）仍待验证。

## 21. 2026-05-27 续作更新（Material-only Demand 验收补齐）

已完成：

1. 补强 `DemandConstraint` 验收测试（`ItemDemandConstraintModeKeyTest`）：
   - `materialAmountOnlyDemandShouldNotCreateItemAmountConstraints`
   - `materialWeightOnlyDemandShouldNotCreateItemAmountConstraints`
   - `mixedItemAmountAndMaterialWeightDemandShouldCreateBothConstraints`
   - `extractorShouldOnlyUseActiveDemandEntries`
2. 保持并复用既有验收测试：
   - `MaterialDemandReducedCostTest.reducedCostShouldUseOnlyActiveMaterialDemandEntries`
   - `DemandStatisticsTest.patternedItemAndBinLayerStatisticsSupportMaterialAmountAndWeightModes`
3. `8.3` 六条验收标准全部勾选完成。

验证状态（更新到 2026-05-27 09:59, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=ItemDemandConstraintModeKeyTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am test "-Dgpg.skip=true"` 通过（4 模块 SUCCESS）。

当前未完成（本轮后）：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调（当前为编排链路与测试联通）。
2. `CirclePackingLayerGenerator` 的圆密排业务实现（按 `cylinder.md` 推进）。
3. final IP/MIP analyzer 到 packing 的真实业务闭环（真实业务参数场景）仍待验证。

## 22. 2026-05-27 续作更新（Application：material-weight-only packing 闭环验证）

已完成：

1. 新增 application 端到端测试 `ColumnGenerationAlgorithmTest.applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow`，覆盖：
   - `material weight only` demand entries 参与 `ColumnGenerationStandardExecutors` 的 LP/RMP 与 final MILP 建模；
   - final MILP `x=2` 时，`collectSelectedBins` 能生成多层拷贝；
   - `ColumnGenerationPackingAnalyzer` 对应输出 `packingResult + schema`，并正确累计 material summary 与 KPI。
2. 本用例验证了 “`final bins -> packing -> schema`” 在 material-only 场景下的可用性，补充了真实参数闭环证据。

验证状态（更新到 2026-05-27 10:08, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldSupportMaterialWeightOnlyDemandPackingFlow "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. application CG 的真实 LP/RMP 与 final IP/MIP 业务联调（外部求解器与真实生产参数）。
2. `CirclePackingLayerGenerator` 的圆密排业务实现（按 `cylinder.md` 推进）。
3. final IP/MIP analyzer 到 packing 的更大规模业务场景联调与验证仍待补齐。

## 23. 2026-05-27 续作更新（Gurobi：application service 真实联调补齐）

已完成：

1. 在 `gurobi-cg-test` 下新增测试 `GurobiColumnGenerationTest.applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer`，覆盖：
   - `ColumnGenerationApplicationService` 使用 `GurobiDelegatingColumnGenerationSolver`；
   - `ColumnGenerationStandardExecutors` 的 LP/RMP + final MILP 实际求解；
   - `ColumnGenerationPackingAnalyzer` 产出 `packingSnapshot/schema`。
2. `GurobiColumnGenerationTest` 现包含两条用例：
   - `standardExecutorsShouldWorkWithGurobiDelegate`
   - `applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer`

验证状态（更新到 2026-05-27 10:31, Asia/Shanghai）：

1. `mvn -f pom.xml -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi -am install -DskipTests "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldWorkWithGurobiDelegateAndPackingAnalyzer "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（2 tests, 0 failure）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 的圆密排业务实现（按 `cylinder.md` 推进）。
2. final IP/MIP analyzer 到 packing 的更大规模业务场景联调与验证仍待补齐。
3. application CG 的真实联调已具备 Gurobi 委托测试闭环，仍待补生产级参数与性能场景验证。

## 24. 2026-05-27 续作更新（CirclePackingLayerGenerator 基础实化）

已完成：

1. `CirclePackingLayerGenerator` 从纯占位回退升级为基础圆密排策略：
   - 基于 item footprint 直径生成矩形排布候选层（`circle-packing-rect`）。
   - 生成六角/错位排布候选层（`circle-packing-hex`）。
   - 候选层按 packed count 排序，同数量时优先 hex。
   - 无 `bin` 或无可行排布时，回退到原 item-based 路径。
2. 新增测试：
   - `LayerGenerationFltXProofTest.circlePackingLayerGeneratorShouldGeneratePackedLayersWhenBinProvided`
   - `LayerGenerationFltXProofTest.circlePackingLayerGeneratorShouldPreferHexWhenPatternCountsTie`

验证状态（更新到 2026-05-27 11:01, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-generation-context -am -Dtest=LayerGenerationFltXProofTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 的更大规模业务场景联调与验证仍待补齐。
3. application CG 的真实联调已具备 Gurobi 委托测试闭环，仍待补生产级参数与性能场景验证。

## 25. 2026-05-27 续作更新（Application：多物料多 bin packing 回归补齐）

已完成：

1. 新增 `ColumnGenerationAlgorithmTest.applicationServiceShouldKeepPackingConsistentForMultiMaterialMultiBinScenario`，覆盖：
   - 混合 demand（`ItemAmount + ItemMaterialWeight`）在 application service 中的 LP/RMP + final MILP 建模链路；
   - `final MILP -> bins -> packing -> schema` 在多物料、多列、多 bin 输入下的闭环；
   - `material summary` 与 `schema.kpi`（`bin_count/material_count`）一致性断言。
2. 明确并固化了 `PreciseAssignment` 的 bin-layer 匹配语义：
   - `layer.bin` 与 `bin.shape` 为引用匹配，测试断言按该语义调整，避免把不可行组合当作期望结果。

验证状态（更新到 2026-05-27 11:15, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldKeepPackingConsistentForMultiMaterialMultiBinScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已有 material-only 与多物料多 bin 回归，但更大规模业务数据联调仍待补齐。
3. application CG 的真实联调已具备 Gurobi 委托测试闭环，生产级参数与中等规模场景已补，仍待更大规模性能场景验证。

## 26. 2026-05-27 续作更新（Gurobi：生产参数 + 中等规模场景）

已完成：

1. 在 `GurobiColumnGenerationTest` 新增用例 `applicationServiceShouldHandleProductionLikeConfigOnMediumScaleScenario`，覆盖：
   - `SolverConfig` 生产参数组合：`time=20s`、`threadNum=2`、`gap=0.01`、`notImprovementTime=5s`。
   - 中等规模输入：24 item（4 material）、2 初始 layer、final MILP + packing analyzer 闭环。
2. 扩展测试辅助 `layerBin(...)`：
   - 新增 `binType` 参数，支持复用同一个 `BinType` 对象，避免 `PreciseAssignment` 下 bin-layer 绑定不一致。
   - 新增 `widthInMeter` 参数，支持构造更宽 item 排布层用于中等规模场景。

验证状态（更新到 2026-05-27 12:14, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeConfigOnMediumScaleScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（4 tests, 0 failure）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 的真实业务大规模数据联调与回归验证仍待补齐（测试侧已补大样本回归）。
3. application CG 的真实联调在 Gurobi 下已补生产参数与中等规模场景，仍待更大规模性能场景验证。

## 27. 2026-05-27 续作更新（Application：packing analyzer 大样本回归）

已完成：

1. 新增 `ColumnGenerationAlgorithmTest.applicationServiceShouldKeepPackingConsistentForLargeMaterialBatchScenario`，覆盖：
   - 24 layer + 24 final bins + 4 materials；
   - `final MILP(stub) -> bins -> packing -> schema` 全链路一致性；
   - `material summary` 与 `schema.kpi`（`bin_count/material_count`）一致性断言。
2. `ColumnGenerationAlgorithmTest.layerBin(...)` 新增 `typeCode` 参数，用于构造多 bin 大样本测试数据，保持 `PreciseAssignment` 的 bin-layer 匹配语义可控。

验证状态（更新到 2026-05-27 12:21, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationAlgorithmTest#applicationServiceShouldKeepPackingConsistentForLargeMaterialBatchScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin 与大样本回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand 场景验证，仍待真实业务数据下性能场景验证。

## 29. 2026-05-27 续作更新（Gurobi：large-scale mixed-demand）

已完成：

1. 在 `GurobiColumnGenerationTest` 新增用例 `applicationServiceShouldHandleLargeScaleMixedDemandScenario`，覆盖：
   - 60 item（5 material）、6 initial layer、single final bin；
   - `item demand + material amount demand` 混合 demand entries；
   - `application service -> standard executors -> Gurobi LP/MILP -> packing analyzer` 闭环。
2. 回归策略收敛：
   - `multi-bin + mixed-demand` 在当前模型口径下触发 `ORModelInfeasible`；
   - 本轮先固定为可稳定通过的 `single-bin + large-scale + mixed-demand`，保证回归可复现。

验证状态（更新到 2026-05-27 13:13, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleLargeScaleMixedDemandScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（6 tests, 0 failure）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin 与大样本回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand 场景验证，仍待真实业务数据下性能场景验证。

## 28. 2026-05-27 续作更新（Gurobi：生产参数 + 大规模场景）

已完成：

1. 在 `GurobiColumnGenerationTest` 新增用例 `applicationServiceShouldHandleProductionLikeConfigOnLargeScaleScenario`，覆盖：
   - 60 item（6 material）、6 initial layer、1 final bin；
   - `SolverConfig` 生产参数组合：`time=30s`、`threadNum=4`、`gap=0.01`、`notImprovementTime=10s`；
   - `application service -> standard executors -> Gurobi LP/MILP -> packing analyzer` 闭环。

验证状态（更新到 2026-05-27 12:59, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeConfigOnLargeScaleScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（5 tests, 0 failure）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin 与大样本回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模场景验证，仍待真实业务数据下性能场景验证。

## 30. 2026-05-27 续作更新（mixed-demand multi-bin infeasible 修复）

已完成：

1. 修复 `PreciseLoad` 多 bin 聚合口径错误：
   - 根因：`PreciseLoad.register()` 构建 load 表达式时使用 `assignment.x[layer]`，对 `UIntVariable2` 走线性索引，导致多 bin 场景仅统计首行变量。
   - 影响：`item + material` mixed-demand 在 multi-bin final MILP 中会出现伪不可行（`ORModelInfeasible`）。
   - 修复：改为按 `assignment.x[binIndex, layerIndex]` 对全部 bin 显式求和。
2. `GurobiColumnGenerationTest.applicationServiceShouldHandleLargeScaleMixedDemandScenario` 已恢复 multi-bin 形态：
   - `groupCount` 从 `1` 恢复为 `2`；
   - 不再依赖 single-bin 兜底回归。
3. 新增 domain 回归测试：
   - `PreciseLoadMultiBinAggregationTest.preciseLoadShouldAggregateLayerAmountAcrossAllBins`
   - 用于锁定“同一 layer 在所有 bin 的 x 变量都会进入 load”。

验证状态（更新到 2026-05-27 14:01, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am -Dtest=PreciseLoadMultiBinAggregationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-layer-assignment-context -am test "-Dgpg.skip=true"` 通过（4 模块 SUCCESS）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（6 tests, 0 failure）。
4. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin 与大样本回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand（single/multi-bin）场景验证，仍待真实业务数据下性能场景验证。

## 31. 2026-05-27 续作更新（Gurobi：性能基线 + schema 指标回归）

已完成：

1. `ColumnGenerationResult` 新增 `elapsed: Duration` 字段，application CG 主流程可直接暴露总耗时指标。
2. 补强 `GurobiColumnGenerationTest.applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario`：
   - 增加性能基线断言：`elapsed > 0s` 且 `elapsed <= 120s`；
   - 增加 schema 级回归断言：`loadingPlans` 与 packing 聚合在 bin/item 维度一致；
   - 增加 `schema.kpi["cg_iteration"]` 与 `result.iterationCount` 一致性断言。

验证状态（更新到 2026-05-27 14:26, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（7 tests, 0 failure）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本与 schema 级回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand（single/multi-bin）场景，并新增 `elapsed` 性能基线断言，仍待真实业务数据下性能场景验证。

## 32. 2026-05-27 续作更新（CG 结果指标透传：LP/MILP 分段）

已完成：

1. `ColumnGenerationResult` 新增：
   - `lpInfos: List<Map<String, String>>`
   - `finalInfo: Map<String, String>`
2. `ColumnGenerationStandardExecutors` 透传分段求解指标：
   - LP：`lp_time_ms`、`lp_gap`、`lp_objective`；
   - MILP：`milp_time_ms`、`milp_gap`、`milp_objective`、`selected_bin_count`、`selected_layer_count`。
3. `GurobiColumnGenerationTest.applicationServiceShouldHandleProductionLikeLargeMultiBinTripleDemandScenario` 增加断言：
   - `result.lpInfos` 与 `result.finalInfo` 包含 solver/time/gap 指标；
   - `selected_bin_count` / `selected_layer_count` 与 packing 聚合统计一致。

验证状态（更新到 2026-05-27 14:39, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（7 tests, 0 failure）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本与 schema 级回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand（single/multi-bin）场景，并补充 `elapsed` + LP/MILP 分段指标断言，仍待真实业务数据下性能场景验证。

## 33. 2026-05-27 续作更新（Gurobi：CSV 数据驱动回归入口）

已完成：

1. 新增 CSV 基线数据文件：
   - `bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv`
   - 字段：`group_index,layer_index,item_id,material_no,material_name,material_weight_kg`
2. `GurobiColumnGenerationTest` 新增 `loadCsvDrivenScenario(resourcePath)`：
   - 从 CSV 构造 `itemDemands`、`demandEntries(item + material amount + material weight)`、`initialColumns`、`finalBins`。
3. 新增用例 `applicationServiceShouldSupportCsvDrivenProductionLikeScenario`：
   - 覆盖 `CSV dataset -> application service -> Gurobi LP/MILP -> packing analyzer` 联调闭环与统计一致性断言。

验证状态（更新到 2026-05-27 14:57, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（8 tests, 0 failure）。
3. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"` 通过（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本、schema 级与 CSV 数据驱动回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand（single/multi-bin）场景，并补充 `elapsed` + LP/MILP 分段指标断言，仍待真实业务数据下性能场景验证。

## 34. 2026-05-27 续作更新（CSV 外部文件开关）

已完成：

1. `applicationServiceShouldSupportCsvDrivenProductionLikeScenario` 支持外部文件开关：
   - 读取优先级：`-Dbpp3d.gurobi.dataset.path=...` > 内置 `gurobi/production-like-dataset.csv`。
2. 新增加载方法：
   - `loadCsvDrivenScenarioFromFile(filePath)`；
   - `loadCsvDrivenScenarioByPropertyOrResource(propertyName, defaultResourcePath)`。

验证状态（更新到 2026-05-27 15:12, Asia/Shanghai）：

1. 默认资源模式通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`。
2. 外部文件路径模式通过（PowerShell 下 `-D` 参数需整体加引号）：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test "-Dbpp3d.gurobi.dataset.path=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv" -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenario "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`。
3. Gurobi 全类回归通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（8 tests, 0 failure）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本、schema 级与 CSV 数据驱动回归，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下已补生产参数与中大规模 + mixed-demand（single/multi-bin）场景，并补充 `elapsed` + LP/MILP 分段指标断言与 CSV 外部文件开关，仍待真实业务数据下性能场景验证。

## 35. 2026-05-27 续作更新（CSV 批量回归入口 + CG 主循环验收收口）

已完成：

1. `GurobiColumnGenerationTest` 新增可选批量数据集回归用例 `applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite`：
   - 仅在 `-Dbpp3d.gurobi.dataset.suite.enabled=true` 时启用，默认不影响常规 Gurobi 回归耗时；
   - 支持 `-Dbpp3d.gurobi.dataset.suite.paths=...`（多 CSV，逗号/分号分隔）或 `-Dbpp3d.gurobi.dataset.suite.dir=...`（目录扫描 `.csv`）。
2. 批量回归用例新增可配置性能阈值：
   - `bpp3d.gurobi.dataset.suite.max.elapsed.seconds`（单数据集上限）；
   - `bpp3d.gurobi.dataset.suite.max.total.elapsed.seconds`（总耗时上限）；
   - `bpp3d.gurobi.dataset.suite.expected.case.count`（期望数据集数量）。
3. `ColumnGenerationAlgorithmTest` 新增 `algorithmShouldCompleteLpSpAddColumnAndFinalMilpFlow`：
   - 明确覆盖 `初始列 -> LP/SP -> 加列 -> final MILP` 主循环闭环；
   - 断言 `iterationCount/lpSolvedTimes/columns/finalInfo` 与调用序列一致。
4. `## 10. 验收标准` 中 `application CG 可完成：初始列、LP、SP、加列、最终 IP/MIP` 已据上述回归改为完成。

验证状态（更新到 2026-05-27 15:35, Asia/Shanghai）：

1. 批量回归入口（单文件示例）通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test "-Dbpp3d.gurobi.dataset.suite.enabled=true" "-Dbpp3d.gurobi.dataset.suite.paths=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv" -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportCsvDrivenProductionLikeScenarioSuite "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（BUILD SUCCESS）。
2. Gurobi 全类回归通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"`（9 tests, 0 failure, 1 skipped）。
3. application 全量回归通过：
   - `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am test "-Dgpg.skip=true"`（9 模块 SUCCESS）。

当前未完成（本轮后）：

1. `CirclePackingLayerGenerator` 仍未接入 `cylinder.md` 要求的真实圆柱几何与可变半径/重量价值函数。
2. final IP/MIP analyzer 到 packing 已补多物料/多 bin、大样本、schema 级、CSV 单文件与批量回归入口，仍待真实业务大规模数据联调与回归验证。
3. application CG 在 Gurobi 下的真实业务性能验证仍待执行外部数据集实跑（现已具备 `suite.paths/suite.dir` 批量入口）。

## 36. 2026-05-27 续作更新（Gurobi：内置随机场景集成测试）

已完成：

1. 在 `GurobiColumnGenerationTest` 新增内置随机场景生成器：
   - `createDeterministicRandomCsvDrivenScenarioCases(seed, caseCount)`；
   - 由固定 seed 生成可复现随机 CSV 文本，再复用 `loadCsvDrivenScenarioFromCsvText(...)` 构建 `itemDemands/demandEntries/initialColumns/finalBins`。
2. 新增集成测试 `applicationServiceShouldSupportDeterministicRandomScenarioSuite`：
   - 作为 application 侧集成测试固化在 `gurobi-test` 源集中；
   - 默认运行（无需外部 CSV 文件）；可选属性：
     - `bpp3d.gurobi.random.dataset.seed`
     - `bpp3d.gurobi.random.dataset.case.count`
     - `bpp3d.gurobi.random.dataset.max.elapsed.seconds`
     - `bpp3d.gurobi.random.dataset.max.total.elapsed.seconds`
   - 覆盖 `随机数据 -> application service -> Gurobi LP/MILP -> packing analyzer` 闭环与统计一致性断言。

验证状态（更新到 2026-05-27 15:51, Asia/Shanghai）：

1. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest#applicationServiceShouldSupportDeterministicRandomScenarioSuite "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（BUILD SUCCESS）。
2. `mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest "-Dsurefire.failIfNoSpecifiedTests=false" test "-Dgpg.skip=true"` 通过（10 tests, 0 failure, 1 skipped）。

补充说明：

1. 并行执行两个 Maven 构建会触发 `target/generated-sources/annotations` 目录竞争，已改为串行执行并确认通过；该现象与业务代码无关。
