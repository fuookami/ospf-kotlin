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

- [ ] material amount only 时，模型不生成 item amount 约束。
- [ ] material weight only 时，模型不生成 item amount 约束。
- [ ] item amount + material weight 混合时，两类约束都存在。
- [ ] shadow price 只按启用的 demand entry 提取。
- [ ] reduced cost 对 material-only demand 正确。
- [ ] layer statistics 对 material weight 正确累计。

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

- [ ] `ColumnGenerationAlgorithm` 位于 application service。
- [ ] domain layer-selection 不再承载 application 编排逻辑。
- [ ] `bpp3d-domain-layer-selection-context` 已从根 `pom.xml` 移除。
- [ ] `bpp3d-domain-layer-selection-context` 目录已删除。
- [ ] application 不再依赖 layer-selection 模块。
- [ ] material-only demand 可建模、可提取 shadow price、可计算 reduced cost。
- [ ] layer generation 通过委托接口接入。
- [ ] block / BLA / pattern / pile placer 至少有基础实现或明确未实现接口。
- [ ] application CG 可完成：初始列、LP、SP、加列、最终 IP/MIP。
- [ ] BPP3D 全模块测试通过。
- [ ] `geometry-boundary-check.ps1` 和 `geometry-module-dry-run.ps1` 通过。

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

验证状态：

1. 在迁移 `asScalarF64()` 和删除 infrastructure api/compat 后，曾运行 `mvn -f ospf-kotlin-framework-bpp3d/pom.xml test "-Dgpg.skip=true"`，结果通过。
2. 后续开始迁移 `ColumnGenerationAlgorithm` 后，测试命令被中断，尚未完成新的全量验证。
3. `git diff --check` 此前通过，但有 CRLF 提示。

接手建议：

1. 先运行 `git status --short` 确认当前改动。
2. 决定是否保留已经开始的 application CG 空壳迁移。
3. 若保留，先跑 `mvn -f ospf-kotlin-framework-bpp3d/pom.xml test "-Dgpg.skip=true"`。
4. 再按本计划继续推进 material-only demand 和 layer generation 委托机制。
