# BPP3D 下一轮交接计划

日期：2026-06-13

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 稳定在 fully shape-polymorphic 生产模型上。所有生产入口必须以 shape-polymorphic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径。

已开放能力必须继续具备 solver、final validation、packing snapshot、renderer、CSV/Gurobi、文档和测试闭环；未开放能力必须通过 guarded contract、负例测试、文档和脚本门禁收口。

对外接口必须是最简洁、最语义化的，不保留任何迁移重构留下的前后缀痕迹（包括 `FltX` 固定别名、`Quantity` 前缀兼容层等）。

## 2. 已完成事项摘要

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

## 3. 当前边界

1. BPP3D 不得在 `LinearMetaModel` 阶段手写或镜像 PWL Big-M 约束。
2. BPP3D 不得手动注册 `pwlFunction.helperVariables` 或调用 `registerAuxiliaryTokens(model.tokens)`。
3. PWL 连续半径必须继续通过 `model.add(pwlSymbol)` 接入 core `UnivariateLinearPiecewiseFunction`。
4. renderer `actualVolume` 必须继续使用 solver-selected radius 的真实 `pi * r^2 * h`，不得回退到 envelope volume 或 PWL volume。
5. unsupported 范围继续 guarded，不允许 silent downgrade。
6. CSP1D 当前有无关未提交改动，下一会话不得混入 BPP3D 提交。
7. 对外接口不得保留 `FltX` 固定别名、`Quantity` 前缀兼容层、迁移前后缀痕迹。

## 4. 下一轮目标

消除 BPP3D 对外 API 中所有兼容层残留，使类型别名从固定 `FltX` 收敛到真正泛型化，确保对外接口最简洁最语义化。

## 5. 下一轮事项

### 5.1 固定 FltX 的自然名别名泛型化（核心事项）

当前 BPP3D 有 23 个 typealias 固定到 `FltX`，与"对外只保留完全泛型化接口"口径冲突。需逐一分析并处理。

**A. Bin 相关（3 个，文件 `Bin.kt:336-340`）**

```
typealias LayerBin = Bin<BinLayer, FltX>
typealias ItemBin = Bin<Item, FltX>
typealias BlockBin = Bin<Block, FltX>
```

方案：删除这 3 个别名。调用方改为直接使用 `Bin<BinLayer, FltX>` 或 `Bin<T, V>` 泛型形式。需确认 `BinLayer` 本身是否仍是 `FltX` 领域模型——如果 `BinLayer` 内部类型参数就是 `FltX`，则调用方 `Bin<BinLayer, FltX>` 是自然的泛型实例化而非兼容层。关键判断标准：别名是否让调用方把 `FltX` 隐藏起来导致无法感知数值类型绑定。

执行步骤：
1. 全局搜索 `LayerBin`、`ItemBin`、`BlockBin` 的所有引用点。
2. 将引用替换为 `Bin<BinLayer, FltX>`、`Bin<Item, FltX>`、`Bin<Block, FltX>`。
3. 删除 3 个 typealias 定义。
4. 编译确认类型推断未被打散。
5. 跑 BPP3D 全量测试。

**B. Placement/Projection 相关（14 个，分布在 `Item.kt`、`Block.kt`、`ItemContainer.kt`、`Layer.kt`）**

```
// Item.kt:527-541
typealias ItemProjection<P> = Projection<Item, FltX, P>
typealias MultipleItemProjection<P> = MultiPileProjection<Item, FltX, P>
typealias AnyPlacement2<P> = QuantityPlacement2<*, FltX, P>
typealias AnySidePlacement2 = AnyPlacement2<Side>
typealias AnyFrontPlacement2 = AnyPlacement2<Front>
typealias AnyPlacement3 = QuantityPlacement3<*, FltX>
typealias ItemPlacement2<P> = QuantityPlacement2<Item, FltX, P>
typealias ItemPlacement3 = QuantityPlacement3<Item, FltX>

// Block.kt:282-286
typealias BlockView = CuboidView<Block, FltX>
typealias BlockPlacement2<P> = QuantityPlacement2<Block, FltX, P>
typealias BlockPlacement3 = QuantityPlacement3<Block, FltX>

// ItemContainer.kt:80-86
typealias ItemContainerPlacement2<S, P> = QuantityPlacement2<S, FltX, P>
typealias ItemContainerSidePlacement2<S> = ItemContainerPlacement2<S, Side>
typealias ItemContainerFrontPlacement2<S> = ItemContainerPlacement2<S, Front>
typealias ItemContainerPlacement3<S> = QuantityPlacement3<S, FltX>

// Layer.kt:202-204
typealias BinLayerView = CuboidView<BinLayer, FltX>
typealias BinLayerPlacement = QuantityPlacement3<BinLayer, FltX>
```

方案：这些别名将 `FltX` 隐藏在自然名后面，需消除。但需区分两种情况：

- **带类型参数的别名**（如 `ItemPlacement2<P>`）：`FltX` 被隐藏，调用方不知道数值类型是 `FltX`。这类需要删除，调用方改为直接使用 `QuantityPlacement2<Item, FltX, P>` 或引入不含 `FltX` 的上层泛型别名（如 `ItemPlacement2<T, V, P>`）。
- **星投影别名**（如 `AnyPlacement3`）：是 `QuantityPlacement3<*, FltX>`，隐藏了 `FltX` 但也约束了通配符。这类别名有实际语义价值（表达"任意物品的 3D 放置"），删除后调用方需要写 `QuantityPlacement3<*, FltX>`，不够简洁。判断是否可通过在 infrastructure 层保留泛型版本（如 `Placement3<T, V>` 已存在于 `Placement.kt:516`）来替代。

执行步骤：
1. 逐个检查每个别名的引用数量和引用位置。
2. 对于引用少（<10 处）的别名，直接删除并替换为展开形式。
3. 对于引用多（>=10 处）的别名，先评估是否保留泛型版本（不含 `FltX` 固定），再逐组替换。
4. 特别注意 `AnyPlacement2`/`AnyPlacement3` 这类星投影别名的语义是否可由 `Placement2<*, V, P>` / `Placement3<*, V>` 替代。
5. 每删除一组别名后编译确认。
6. 跑 BPP3D 全量测试。

**C. ShadowPriceMap（1 个，文件 `ShadowPriceMap.kt:16`）**

```
typealias BPP3DShadowPriceMap = AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>
```

方案：删除别名，调用方改为直接使用 `AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>`。如果引用过多，可保留但改名为不含 `BPP3D` 前缀的更语义化名称。

**D. Infrastructure 层泛型别名（2 个，文件 `Placement.kt:513-516`）**

```
typealias Placement2<T, V, P> = QuantityPlacement2<T, V, P>
typealias Placement3<T, V> = QuantityPlacement3<T, V>
```

这两个别名不含固定 `FltX`，是 `Quantity` 前缀的语义化别名。方案：这两个可以保留——它们是从 `Quantity*` 前缀到更语义化名称的桥接，不含兼容层痕迹。但需确认 `Quantity` 前缀是否属于"迁移重构留下的痕迹"。如果是，则应直接让调用方使用 `QuantityPlacement2`/`QuantityPlacement3`，或在 math 层重命名基础类型。

### 5.2 Quantity 前缀残留审计

已删除的 Quantity 兼容层文件：
- `QuantityCompatibility.kt`（已删除）
- `QuantityGeometrySpike.kt`（已删除）
- `QuantityProjectionPlacementCore.kt`（已删除）
- `InfraAliases.kt`（已删除）

但仍存在的 Quantity 前缀文件和类型：
- `QuantityContainerCore.kt`（infrastructure 层，仍在）
- `QuantityGeometryCore.kt`（infrastructure 层，仍在）
- `QuantityPlacement2` / `QuantityPlacement3`（来自 math 层，非 BPP3D 定义）
- `QuantityDemandStatistics.kt`（domain-item-context 层）
- `QuantityDomainModels.kt`（domain-item-context 层）
- `Bpp3dQuantityBoundaryTest.kt`（测试文件）

方案：
1. 审计 `QuantityContainerCore.kt` 和 `QuantityGeometryCore.kt` 的内容，判断是否是兼容层（桥接旧 Quantity API 到新 API）还是生产代码（真正使用 Quantity 语义）。如果是兼容桥接，删除并让调用方直接使用新 API。
2. `QuantityPlacement2` / `QuantityPlacement3` 来自 math 层（非 BPP3D），BPP3D 不能重命名。BPP3D 内部通过 `Placement2`/`Placement3` 别名或直接使用全名即可。
3. `QuantityDemandStatistics.kt` / `QuantityDomainModels.kt` 需判断是兼容层还是生产类型名。
4. 测试文件 `Bpp3dQuantityBoundaryTest.kt` 如不含兼容层逻辑，仅需重命名去除 `Quantity` 前缀。

### 5.3 边界脚本更新

删除 typealias 后，需同步更新边界脚本：

1. `shape-boundary-check.ps1`：当前有 `DeletedQuantityRectangleAliasReflux` 检查。需新增检查确保已删除的 typealias 不会回流。
2. `generic-boundary-check.ps1`：当前有 `Direction`/`ZOX`/`XOY`/`ZOY` 和 `Scalar` typealias 检查。需新增检查确保 BPP3D 不得新增固定 `FltX` 的 typealias。
3. 新增 `FltXFixedAliasReflux` 检查：禁止 `typealias Xxx = SomeGeneric<Xxx, FltX>` 模式（排除 infrastructure 层 `Placement2`/`Placement3` 等不含固定 `FltX` 的泛型别名）。

### 5.4 测试文件整理

1. 已删除的测试文件需确认其测试覆盖已迁移到其他测试中：
   - `BottomUpLeftJustifiedAlgorithmProofTest.kt`
   - `ComplexBlockGeneratorProofTest.kt`
   - `SimpleBlockGeneratorProofTest.kt`
   - `FltXDirectCompileProofTest.kt`
   - `LayerGenerationFltXProofTest.kt`
2. 重命名含 `Quantity` 前缀的测试文件（如 `Bpp3dQuantityBoundaryTest.kt`）。
3. 确保全量测试通过。

### 5.5 文档与 README 更新

1. README.md / README_ch.md：如有类型别名变更导致对外 API 变化，需同步更新。
2. daily.md：本轮结束时改写为完成记录。

## 6. 执行计划

1. **启动检查**
   - 执行 `git status --short`，确认只处理 `ospf-kotlin-framework-bpp3d`。
   - 阅读本文件、README.md、README_ch.md 与 `shape-boundary-check.ps1`、`generic-boundary-check.ps1`。
   - 确认 `InfraNumber`/`infra*`/`XXXV`/`Typed`/`Generic` 命名模式已清理完毕（上一轮已完成）。

2. **Quantity 兼容层审计与清理（5.2）**
   - 先读 `QuantityContainerCore.kt`、`QuantityGeometryCore.kt`、`QuantityDemandStatistics.kt`、`QuantityDomainModels.kt` 内容。
   - 判断每个文件是兼容层还是生产代码。
   - 删除兼容层文件，让调用方直接使用新 API。
   - 编译确认。

3. **固定 FltX 别名泛型化（5.1）**
   - 按 A→B→C→D 顺序处理。
   - 每组先搜索引用，评估影响面。
   - 每删除一组后编译 + focused test。
   - 全部完成后跑 BPP3D 全量测试。

4. **边界脚本更新（5.3）**
   - 新增 `FltXFixedAliasReflux` 检查。
   - 新增已删除 typealias 回流检查。
   - 跑全量边界脚本确认无误报。

5. **测试整理与文档更新（5.4, 5.5）**
   - 确认已删除测试的覆盖迁移。
   - 重命名含 `Quantity` 前缀的测试文件。
   - 更新 README。
   - 更新 daily.md 为完成记录。

6. **最终验收**
   - 跑必跑门禁（见第 8 节）。
   - 跑 Gurobi 触发式验收（如有 Gurobi 环境）。
   - `git diff --check -- ospf-kotlin-framework-bpp3d`。

## 7. 修改清单

允许修改：

1. `ospf-kotlin-framework-bpp3d/README.md`
2. `ospf-kotlin-framework-bpp3d/README_ch.md`
3. `ospf-kotlin-framework-bpp3d/daily.md`
4. `ospf-kotlin-framework-bpp3d/scripts/*.ps1`
5. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/**/*`
6. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/**/*`
7. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/**/*`
8. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/test/**/*`
9. `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/main/**/*`
10. `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/test/**/*`
11. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/**/*`
12. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/**/*`
13. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/gurobi-test/**/*`
14. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/**/*`
15. `ospf-kotlin-framework-bpp3d/bpp3d-domain-bla-context/src/main/**/*`
16. `ospf-kotlin-framework-bpp3d/bpp3d-domain-block-loading-context/src/main/**/*`
17. `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context/src/main/**/*`
18. `ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-generation-context/src/main/**/*`

谨慎修改：

1. `ospf-kotlin-core/**/*`：仅当 PWL function lifecycle、token table 或 mechanism dump 的通用缺陷再次出现时修改。
2. `ospf-kotlin-core-plugin/**/*`：仅限 Gurobi plugin 安装或验收所需修复。
3. `E:\workspace\ospf\framework\bpp3d-interface-renderer/**/*`：仅当 renderer DTO 或显示语义变化时修改。

禁止混入：

1. `ospf-kotlin-framework-csp1d/**/*`
2. 非 BPP3D 业务模块
3. 与兼容层清理 / 泛型化 / renderer / Gurobi 验收无关的格式化 churn

## 8. 验收目标

### 8.1 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

### 8.2 Gurobi 触发式验收

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

### 8.3 功能验收

1. 已开放长方体、竖直圆柱、X/Z 横向圆柱路径不回退。
2. interval-only PWL 连续半径保持 solver-selected radius、PWL metadata、actual radius validation 和 renderer actualVolume 闭环。
3. PWL function constraints 仍由 core mechanism lifecycle 展开。
4. renderer `actualVolume` 不使用 envelope volume 或 PWL volume 替代。
5. 对外 API 不保留固定 `FltX` 的 typealias（infrastructure 层泛型别名 `Placement2`/`Placement3` 除外）。
6. 对外 API 不保留 `Quantity` 前缀兼容层文件。
7. 对外 API 不保留任何迁移重构留下的前后缀痕迹。
8. 边界脚本能阻止已删除兼容层和固定 `FltX` typealias 回流。
9. 所有提交不包含 CSP1D 或非目标模块改动。

## 9. 提交建议

建议拆分为 2 到 3 个提交：

1. `refactor(bpp3d): remove Quantity compatibility layer and remaining bridge files`
   - 删除 Quantity 兼容层文件、桥接类型、对应测试。
2. `refactor(bpp3d): replace fixed-FltX type aliases with generic or expanded forms`
   - 删除固定 `FltX` 的 typealias，替换引用为展开形式或泛型形式。
3. `chore(bpp3d): update boundary scripts and docs for compatibility layer removal`
   - 边界脚本新增回流检查、README 更新、测试文件重命名。

如实际改动较小，可合并为一个提交，但提交信息必须说明删除了哪些兼容层和别名。

## 10. 当前状态

最新相关提交：`95432009a`。

工作区有 153 个 BPP3D 文件未提交改动（+4125/-8752），包含上一轮的 PWL 架构收口和部分兼容层清理。

上一轮会话在处理 `Bin`/`Placement` 自然名别名泛型化时中断。中断时 assistant 正在重新核对 `Bin`、`ItemContainer`、`Placement` 的类型边界——核心判断是：`LayerBin`、`ItemBin`、`BlockBin` 不能简单加 `<V>` 参数，因为 `BinLayer` 本身仍是 `FltX` 领域模型，别名泛型会变成"看起来泛型、实际不成立"的 API。

用户最后明确要求："Quantity 兼容层也要删除，所有兼容层都要删除，对外接口应当是最简洁最语义化的而不要保留任何迁移重构留下的前后缀痕迹"。

## 11. 待清理 typealias 完整清单

| # | 别名 | 定义位置 | 固定 FltX | 处理方式 |
|---|------|----------|-----------|----------|
| 1 | `LayerBin` | Bin.kt:336 | 是 | 删除，引用改为 `Bin<BinLayer, FltX>` |
| 2 | `ItemBin` | Bin.kt:338 | 是 | 删除，引用改为 `Bin<Item, FltX>` |
| 3 | `BlockBin` | Bin.kt:340 | 是 | 删除，引用改为 `Bin<Block, FltX>` |
| 4 | `BlockView` | Block.kt:282 | 是 | 删除，引用改为 `CuboidView<Block, FltX>` |
| 5 | `BlockPlacement2<P>` | Block.kt:284 | 是 | 删除，引用改为 `QuantityPlacement2<Block, FltX, P>` |
| 6 | `BlockPlacement3` | Block.kt:286 | 是 | 删除，引用改为 `QuantityPlacement3<Block, FltX>` |
| 7 | `ItemProjection<P>` | Item.kt:527 | 是 | 删除，引用改为 `Projection<Item, FltX, P>` |
| 8 | `MultipleItemProjection<P>` | Item.kt:529 | 是 | 删除，引用改为 `MultiPileProjection<Item, FltX, P>` |
| 9 | `AnyPlacement2<P>` | Item.kt:531 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, P>` |
| 10 | `AnySidePlacement2` | Item.kt:533 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, Side>` |
| 11 | `AnyFrontPlacement2` | Item.kt:535 | 是 | 删除，引用改为 `QuantityPlacement2<*, FltX, Front>` |
| 12 | `AnyPlacement3` | Item.kt:537 | 是 | 删除，引用改为 `QuantityPlacement3<*, FltX>` |
| 13 | `ItemPlacement2<P>` | Item.kt:539 | 是 | 删除，引用改为 `QuantityPlacement2<Item, FltX, P>` |
| 14 | `ItemPlacement3` | Item.kt:541 | 是 | 删除，引用改为 `QuantityPlacement3<Item, FltX>` |
| 15 | `ItemContainerPlacement2<S, P>` | ItemContainer.kt:80 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, P>` |
| 16 | `ItemContainerSidePlacement2<S>` | ItemContainer.kt:82 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, Side>` |
| 17 | `ItemContainerFrontPlacement2<S>` | ItemContainer.kt:84 | 是 | 删除，引用改为 `QuantityPlacement2<S, FltX, Front>` |
| 18 | `ItemContainerPlacement3<S>` | ItemContainer.kt:86 | 是 | 删除，引用改为 `QuantityPlacement3<S, FltX>` |
| 19 | `BinLayerView` | Layer.kt:202 | 是 | 删除，引用改为 `CuboidView<BinLayer, FltX>` |
| 20 | `BinLayerPlacement` | Layer.kt:204 | 是 | 删除，引用改为 `QuantityPlacement3<BinLayer, FltX>` |
| 21 | `BPP3DShadowPriceMap` | ShadowPriceMap.kt:16 | 是 | 删除，引用改为 `AbstractBPP3DShadowPriceMap<BPP3DShadowPriceArguments, FltX, Item>` |
| 22 | `Placement2<T, V, P>` | Placement.kt:513 | 否 | 保留（泛型别名，不含固定 FltX） |
| 23 | `Placement3<T, V>` | Placement.kt:516 | 否 | 保留（泛型别名，不含固定 FltX） |

## 12. 待审计 Quantity 前缀文件

| # | 文件 | 模块 | 状态 | 判断 |
|---|------|------|------|------|
| 1 | `QuantityContainerCore.kt` | infrastructure | 存在 | 待审计：兼容层 or 生产代码 |
| 2 | `QuantityGeometryCore.kt` | infrastructure | 存在 | 待审计：兼容层 or 生产代码 |
| 3 | `QuantityDemandStatistics.kt` | domain-item-context | 存在 | 待审计：兼容层 or 生产代码 |
| 4 | `QuantityDomainModels.kt` | domain-item-context | 存在 | 待审计：兼容层 or 生产代码 |
| 5 | `Bpp3dQuantityBoundaryTest.kt` | application test | 存在 | 待重命名 |
| 6 | `QuantityCompatibility.kt` | infrastructure | 已删除 | - |
| 7 | `QuantityGeometrySpike.kt` | infrastructure | 已删除 | - |
| 8 | `QuantityProjectionPlacementCore.kt` | infrastructure | 已删除 | - |
