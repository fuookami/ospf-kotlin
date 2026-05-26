# BPP3D 重构日报

日期：2026-05-26
最后核对时间：2026-05-26 18:14（Asia/Shanghai）

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

## 当前未完成（后续事项）

- [ ] `ColumnGenerationAlgorithm` 尚未接入真实 LP/RMP 求解、shadow price 刷新与最终 IP/MIP 求解链路（目前为可扩展编排骨架）。
- [ ] `BlockLayerGenerator` / `BLLocalLayerGenerator` / `BLGlobalLayerGenerator` / `PatternLayerGenerator` / `PileLayerGenerator` / `CirclePackingLayerGenerator` 仍为占位实现，未接入真实生成策略。
- [ ] final IP/MIP solution analyzer 到 packing 的完整闭环尚未接通（目前仅完成 packing service 与 renderer adapter 基础链路）。

## 本次核对结论

- [ ] `daily.md` 中事项未全部完成，仍有 3 项未完成（见上方“当前未完成”）。
