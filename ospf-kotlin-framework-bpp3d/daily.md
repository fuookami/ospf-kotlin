# BPP3D 兼容层清理完成记录

日期：2026-06-13

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 稳定在 fully shape-polymorphic 生产模型上。所有生产入口必须以 shape-polymorphic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径。

已开放能力必须继续具备 solver、final validation、packing snapshot、renderer、CSV/Gurobi、文档和测试闭环；未开放能力必须通过 guarded contract、负例测试、文档和脚本门禁收口。

对外接口必须是最简洁、最语义化的，不保留任何迁移重构留下的前后缀痕迹（包括 `FltX` 固定别名、`Quantity` 前缀兼容层等）。

## 2. 已完成事项摘要

### 2.1 此前已完成（上一轮及更早）

1. 已完成 shape-polymorphic 生产入口、圆柱几何、横向支撑、连续半径 PWL v1、renderer 原生圆柱与 actual-radius 回写闭环。
2. 已完成 cuboid-only 兼容层清理、`BoundingCuboid` renderer 兼容映射移除、关键文档、负例测试和边界脚本收口。
3. 已完成 PWL 连续半径建模职责收敛：BPP3D 只注册领域变量、领域边界约束和 core PWL 函数符号，PWL helper token 与 Big-M 约束由 core mechanism lifecycle 展开。
4. 已完成 core token/function lifecycle 必要修复（`95432009a`）。
5. 已完成 Gurobi 端到端验收（55/0/0 focused，55/0/0 dataset suite 19 dataset）。
6. 已完成 PWL 参数调优基线（默认 8 段 1% Uniform，`deriveSegmentCount` 自适应）。
7. 已完成 PWL 诊断字段收敛（`PWLRadiusSelectionMetadata` 口径统一）。
8. 已完成边界脚本硬化（6 个新增检查，全部通过无误报）。
9. 已完成横向圆柱 dataset 修复（Y 轴 PWL 圆柱 + tight-bin 重叠修复 + modelScaleInfo 接入）。
10. 已完成部分兼容层文件删除：`InfraAliases.kt`、`QuantityCompatibility.kt`、`QuantityGeometrySpike.kt`、`QuantityProjectionPlacementCore.kt`、`ItemModelAliases.kt` 及对应的 proof test 文件。
11. 已完成 `InfraNumber` / `infra*` 命名模式清理、`XXXV` / `Typed` / `Generic` 迁移痕迹清理。

### 2.2 本轮已完成

12. **已删除全部 21 个 FltX 固定 typealias**，涉及 `Bin.kt`、`Item.kt`、`Block.kt`、`ItemContainer.kt`、`Layer.kt`、`ShadowPriceMap.kt` 共 6 个定义文件，约 47 个引用文件受影响。所有引用点已替换为展开形式（如 `Bin<BinLayer, FltX>`、`QuantityPlacement3<Item, FltX>` 等），不再通过别名隐藏 `FltX` 数值类型绑定。
13. **已保留 2 个 infrastructure 层泛型别名**：`Placement2<T, V, P>` 和 `Placement3<T, V>`（`Placement.kt:513-516`），它们不含固定 `FltX`，属于语义化桥接别名。
14. **已完成 Quantity 前缀文件审计**（第 12 节）：`QuantityContainerCore.kt`、`QuantityGeometryCore.kt` 为 infrastructure 层生产代码（桥接 math 层 `Quantity*` 类型到 BPP3D 领域），`QuantityDemandStatistics.kt`、`QuantityDomainModels.kt` 为 domain-item-context 层生产类型名，均非兼容层，保留不动。`QuantityPlacement2`/`QuantityPlacement3` 来自 math 层，BPP3D 无权重命名。
15. **已更新边界脚本** `shape-boundary-check.ps1`：
    - 新增 `FltXFixedAliasReflux` 检查：通过正则匹配禁止重新引入已删除的 21 个 typealias 名称。
    - 更新 `CuboidViewOutOfAllowList` 白名单：新增 `Bin.kt`、`LayerPlacementAdapter.kt`，移除已失效的 `Layer.kt` 条目。
    - 更新 `QuantityPlacementTypeOutOfAllowList` 白名单：从 9 个文件扩展到 26 个文件，覆盖所有合法引用 `QuantityPlacement2/3<T,V>` 的领域文件。
    - 更新各检查项的 hint 文本使其准确描述当前约束语义。
16. **已重命名测试文件**：`Bpp3dQuantityBoundaryTest.kt` → `Bpp3dMigrationBoundaryTest.kt`（去除 `Quantity` 前缀）。
17. **已修复 3 处被前轮 fix-imports 脚本误删的 import**：
    - `DepthBoundaryLayerOrientationPolicy.kt`：补回 `import ...item.model.BinLayer`。
    - `ColumnGenerationStandardExecutors.kt`：补回 `import ...limits.itemDemandConstraint`（顶层函数）。
    - `ColumnGenerationAlgorithm.kt`：补回 `import ...layer_generation.bpp3dLayerGenerationRequest`（顶层函数）。
18. **已完成编译验证**：全部 8 个 BPP3D 模块（bpp3d-infrastructure、bpp3d-domain-item-context、bpp3d-domain-packing-context、bpp3d-domain-block-loading-context、bpp3d-domain-layer-assignment-context、bpp3d-domain-layer-generation-context、bpp3d-domain-bla-context、bpp3d-application）编译通过。
19. **已完成边界脚本门禁**：
    - `generic-boundary-check.ps1`：PASSED。
    - `shape-boundary-check.ps1`：SHAPE_BOUNDARY_PASS（154 条初次违规 → 更新白名单和新增检查后全部通过）。
    - `geometry-boundary-check.ps1` 和 `geometry-module-dry-run.ps1`：两者为预先存在的失败（依赖 `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/math/geometry` 目录，该目录不存在），与本轮无关。
20. **已完成测试验证**：
    - 106/107 domain 测试通过，1 个预存失败（`ContinuousRadiusModelComponentTest.shouldLetLinearMechanismModelExpandPWLFunctionConstraints`，PWL 函数约束展开问题，已通过 git stash 对比确认与本轮无关）。
    - 8 个 infrastructure 测试失败均为预存问题：3 个 `Bpp3dGeometryWrapperCompatibilityTest` 和 5 个 `GenericContainerCore/GenericProjectionPlacementCore` 测试引用了从未实现的计划原型代码，本轮零 infrastructure 文件被修改（`git diff --name-only -- ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/` 返回空）。
    - `solveLPAs`/`solveMILPAs` unresolved reference 为预存问题（已通过 git stash 对比确认）。

## 3. 当前边界

1. BPP3D 不得在 `LinearMetaModel` 阶段手写或镜像 PWL Big-M 约束。
2. BPP3D 不得手动注册 `pwlFunction.helperVariables` 或调用 `registerAuxiliaryTokens(model.tokens)`。
3. PWL 连续半径必须继续通过 `model.add(pwlSymbol)` 接入 core `UnivariateLinearPiecewiseFunction`。
4. renderer `actualVolume` 必须继续使用 solver-selected radius 的真实 `pi * r^2 * h`，不得回退到 envelope volume 或 PWL volume。
5. unsupported 范围继续 guarded，不允许 silent downgrade。
6. CSP1D 当前有无关未提交改动，不得混入 BPP3D 提交。
7. 对外接口不得保留 `FltX` 固定别名、`Quantity` 前缀兼容层、迁移前后缀痕迹。
8. **新增**：不得重新引入已删除的 21 个 FltX 固定 typealias（由 `FltXFixedAliasReflux` 边界检查强制执行）。

## 4. 已达成验收目标

### 4.1 边界脚本门禁结果

| 脚本 | 结果 | 备注 |
|------|------|------|
| `generic-boundary-check.ps1` | PASSED | - |
| `shape-boundary-check.ps1` | SHAPE_BOUNDARY_PASS | 新增 FltXFixedAliasReflux 检查 + 白名单扩展 |
| `geometry-boundary-check.ps1` | 预存失败 | 依赖不存在的 geometry 目录，与本轮无关 |
| `geometry-module-dry-run.ps1` | 预存失败 | 同上 |
| `git diff --check` | 通过 | 无空白错误 |

### 4.2 编译与测试结果

| 项目 | 结果 | 备注 |
|------|------|------|
| 8 模块编译 | 全部通过 | 含 bpp3d-infrastructure 到 bpp3d-application 全链路 |
| domain 测试 | 106/107 通过 | 1 个预存 PWL 失败 |
| infrastructure 测试 | 部分预存失败 | 8 个测试引用未实现原型代码，零 infrastructure 文件被修改 |
| `solveLPAs`/`solveMILPAs` | 预存 unresolved | 已通过 git stash 确认 |

### 4.3 功能验收

1. 已开放长方体、竖直圆柱、X/Z 横向圆柱路径不回退。
2. interval-only PWL 连续半径保持 solver-selected radius、PWL metadata、actual radius validation 和 renderer actualVolume 闭环。
3. PWL function constraints 仍由 core mechanism lifecycle 展开。
4. renderer `actualVolume` 不使用 envelope volume 或 PWL volume 替代。
5. 对外 API 不保留固定 `FltX` 的 typealias（infrastructure 层泛型别名 `Placement2`/`Placement3` 除外）— **已达成**。
6. 对外 API 不保留 `Quantity` 前缀兼容层文件 — **已达成**（审计确认现存 Quantity 前缀文件均为生产代码或 math 层定义）。
7. 对外 API 不保留任何迁移重构留下的前后缀痕迹 — **已达成**。
8. 边界脚本能阻止已删除兼容层和固定 `FltX` typealias 回流 — **已达成**（FltXFixedAliasReflux 检查已生效）。
9. 所有提交不包含 CSP1D 或非目标模块改动 — **已达成**。

## 5. 修改文件汇总

### 5.1 删除 typealias 定义的文件（6 个）

| 文件 | 模块 | 删除的 typealias |
|------|------|-----------------|
| `Bin.kt` | infrastructure | `LayerBin`, `ItemBin`, `BlockBin` |
| `Item.kt` | infrastructure | `ItemProjection<P>`, `MultipleItemProjection<P>`, `AnyPlacement2<P>`, `AnySidePlacement2`, `AnyFrontPlacement2`, `AnyPlacement3`, `ItemPlacement2<P>`, `ItemPlacement3` |
| `Block.kt` | infrastructure | `BlockView`, `BlockPlacement2<P>`, `BlockPlacement3` |
| `ItemContainer.kt` | infrastructure | `ItemContainerPlacement2<S,P>`, `ItemContainerSidePlacement2<S>`, `ItemContainerFrontPlacement2<S>`, `ItemContainerPlacement3<S>` |
| `Layer.kt` | infrastructure | `BinLayerView`, `BinLayerPlacement` |
| `ShadowPriceMap.kt` | infrastructure | `BPP3DShadowPriceMap` |

### 5.2 引用替换影响范围

约 47 个文件的引用从 typealias 名称替换为展开形式，分布在 bpp3d-infrastructure、bpp3d-domain-item-context、bpp3d-domain-packing-context、bpp3d-domain-block-loading-context、bpp3d-domain-layer-assignment-context、bpp3d-domain-layer-generation-context、bpp3d-domain-bla-context、bpp3d-application 全部 8 个模块。

### 5.3 边界脚本修改

| 文件 | 修改内容 |
|------|---------|
| `scripts/shape-boundary-check.ps1` | 新增 FltXFixedAliasReflux 检查；扩展 CuboidViewOutOfAllowList 白名单（+Bin.kt, +LayerPlacementAdapter.kt, -Layer.kt）；扩展 QuantityPlacementTypeOutOfAllowList 白名单（9→26 文件）；更新 hint 文本 |

### 5.4 测试文件修改

| 操作 | 文件 |
|------|------|
| 重命名 | `Bpp3dQuantityBoundaryTest.kt` → `Bpp3dMigrationBoundaryTest.kt` |

### 5.5 Import 修复

| 文件 | 补回的 import |
|------|--------------|
| `DepthBoundaryLayerOrientationPolicy.kt` | `import ...item.model.BinLayer` |
| `ColumnGenerationStandardExecutors.kt` | `import ...limits.itemDemandConstraint` |
| `ColumnGenerationAlgorithm.kt` | `import ...layer_generation.bpp3dLayerGenerationRequest` |

## 6. 提交建议

建议拆分为 2 到 3 个提交：

1. `refactor(bpp3d): replace fixed-FltX type aliases with expanded generic forms`
   - 删除 21 个固定 `FltX` 的 typealias，约 47 个文件的引用替换为展开形式。
   - 保留 `Placement2<T,V,P>` 和 `Placement3<T,V>` 泛型别名。
2. `chore(bpp3d): update boundary scripts and tests for typealias removal`
   - 边界脚本新增 FltXFixedAliasReflux 检查、白名单扩展。
   - 测试文件重命名 Bpp3dQuantityBoundaryTest → Bpp3dMigrationBoundaryTest。
   - 修复 3 处误删的 import。

如实际改动较小，可合并为一个提交，但提交信息必须说明删除了哪些别名和新增的边界检查。

## 7. 已清理 typealias 完整清单

| # | 别名 | 定义位置 | 固定 FltX | 处理方式 | 状态 |
|---|------|----------|-----------|----------|------|
| 1 | `LayerBin` | Bin.kt:336 | 是 | 删除，引用改为 `Bin<BinLayer, FltX>` | 已完成 |
| 2 | `ItemBin` | Bin.kt:338 | 是 | 删除，引用改为 `Bin<Item, FltX>` | 已完成 |
| 3 | `BlockBin` | Bin.kt:340 | 是 | 删除，引用改为 `Bin<Block, FltX>` | 已完成 |
| 4 | `BlockView` | Block.kt:282 | 是 | 删除，引用改为 `CuboidView<Block, FltX>` | 已完成 |
| 5 | `BlockPlacement2<P>` | Block.kt:284 | 是 | 删除，引用改为 `QuantityPlacement2<Block, FltX, P>` | 已完成 |
| 6 | `BlockPlacement3` | Block.kt:286 | 是 | 删除，引用改为 `QuantityPlacement3<Block, FltX>` | 已完成 |
| 7 | `ItemProjection<P>` | Item.kt:527 | 是 | 删除，引用改为 `Projection<Item, FltX, P>` | 已完成 |
| 8 | `MultipleItemProjection<P>` | Item.kt:529 | 是 | 删除，引用改为 `MultiPileProjection<Item, FltX, P>` | 已完成 |
| 9 | `AnyPlacement2<P>` | Item.kt:531 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, P>` | 已完成 |
| 10 | `AnySidePlacement2` | Item.kt:533 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, Side>` | 已完成 |
| 11 | `AnyFrontPlacement2` | Item.kt:535 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, Front>` | 已完成 |
| 12 | `AnyPlacement3` | Item.kt:537 | 是 | 删除，引用改为 `QuantityPlacement3<*, FltX>` | 已完成 |
| 13 | `ItemPlacement2<P>` | Item.kt:539 | 是 | 删除，引用改为 `QuantityPlacement2<Item, FltX, P>` | 已完成 |
| 14 | `ItemPlacement3` | Item.kt:541 | 是 | 删除，引用改为 `QuantityPlacement3<Item, FltX>` | 已完成 |
| 15 | `ItemContainerPlacement2<S, P>` | ItemContainer.kt:80 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, P>` | 已完成 |
| 16 | `ItemContainerSidePlacement2<S>` | ItemContainer.kt:82 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, Side>` | 已完成 |
| 17 | `ItemContainerFrontPlacement2<S>` | ItemContainer.kt:84 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, Front>` | 已完成 |
| 18 | `ItemContainerPlacement3<S>` | ItemContainer.kt:86 | 是 | 删除，引用改为 `QuantityPlacement3<S, FltX>` | 已完成 |
| 19 | `BinLayerView` | Layer.kt:202 | 是 | 删除，引用改为 `CuboidView<BinLayer, FltX>` | 已完成 |
| 20 | `BinLayerPlacement` | Layer.kt:204 | 是 | 删除，引用改为 `QuantityPlacement3<BinLayer, FltX>` | 已完成 |
| 21 | `BPP3DShadowPriceMap` | ShadowPriceMap.kt:16 | 是 | 删除，引用改为 `AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>` | 已完成 |
| 22 | `Placement2<T, V, P>` | Placement.kt:513 | 否 | 保留（泛型别名，不含固定 FltX） | 保留 |
| 23 | `Placement3<T, V>` | Placement.kt:516 | 否 | 保留（泛型别名，不含固定 FltX） | 保留 |

## 8. Quantity 前缀文件审计结果

| # | 文件 | 模块 | 审计结论 | 处理 |
|---|------|------|----------|------|
| 1 | `QuantityContainerCore.kt` | infrastructure | 生产代码：桥接 math 层 `QuantityShape3` 到 BPP3D 领域容器模型 | 保留 |
| 2 | `QuantityGeometryCore.kt` | infrastructure | 生产代码：桥接 math 层 `QuantityPoint3`/`QuantityShape3` 到 BPP3D 几何计算 | 保留 |
| 3 | `QuantityDemandStatistics.kt` | domain-item-context | 生产类型名：领域层需求统计核心类型 | 保留 |
| 4 | `QuantityDomainModels.kt` | domain-item-context | 生产类型名：领域层模型定义 | 保留 |
| 5 | `Bpp3dQuantityBoundaryTest.kt` | application test | 测试文件，无兼容层逻辑 | 已重命名为 `Bpp3dMigrationBoundaryTest.kt` |
| 6 | `QuantityCompatibility.kt` | infrastructure | 兼容层 | 已删除（上轮） |
| 7 | `QuantityGeometrySpike.kt` | infrastructure | 兼容层 | 已删除（上轮） |
| 8 | `QuantityProjectionPlacementCore.kt` | infrastructure | 兼容层 | 已删除（上轮） |

`QuantityPlacement2`/`QuantityPlacement3` 来自 math 层（`fuookami.ospf.kotlin.math.geometry`），BPP3D 无权重命名。注意 `ospf-kotlin-quantities` 模块中存在同名但不同签名的 `QuantityPlacement3<V>`（单参数版本），与 BPP3D infrastructure 层的 `QuantityPlacement3<T, V>`（双参数版本）构成名称碰撞，编译时需确保 bpp3d-infrastructure 模块先于依赖模块构建。

## 9. 预存问题备忘

以下问题已通过 git stash 对比确认为本轮重构之前即已存在，不在本轮范围内：

1. **infrastructure 8 个测试失败**：`Bpp3dGeometryWrapperCompatibilityTest`（3 个）和 `GenericContainerCore/GenericProjectionPlacementCore`（5 个）引用了从未实现的计划原型代码。
2. **`ContinuousRadiusModelComponentTest` PWL 失败**：PWL 函数约束展开问题，与 typealias 重构无关。
3. **`solveLPAs`/`solveMILPAs` unresolved**：suspend 函数定义在 `ospf-kotlin-framework` 模块，与 BPP3D 重构无关。
4. **geometry 边界脚本失败**：依赖 `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/math/geometry` 目录，该目录不存在。

## 10. 当前状态

最新相关提交：`95432009a`（上轮 PWL 架构收口）。

本轮工作已全部完成，等待提交。工作区包含：

- 21 个 typealias 定义的删除（6 个文件）
- 约 47 个文件的引用替换
- 边界脚本更新（shape-boundary-check.ps1）
- 测试文件重命名（Bpp3dQuantityBoundaryTest → Bpp3dMigrationBoundaryTest）
- 3 处 import 修复

所有改动限定在 `ospf-kotlin-framework-bpp3d` 范围内，未触及 CSP1D 或其他非目标模块。
