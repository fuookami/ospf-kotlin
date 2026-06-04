# BPP3D 形状泛型化与圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-04

本文档用于交接 BPP3D “形状泛型化 + 圆柱支持”重构。当前状态不是长期终局完成，而是长方体主链向 shape-aware 主链迁移后的阶段性收敛。

## 1. 总目标

把 BPP3D 从“长方体作为隐含前提”的主流程，推进为能稳定表达长方体与圆柱的 shape-aware 装载链路。

目标边界：

1. 支持 `Cuboid` 与固定 `Axis3.Y` 的竖直圆柱。
2. 圆柱路径使用真实几何或显式 unsupported，不把外接盒当作最终几何判定。
3. 长方体路径、旧 DTO 字段、旧 CSV 入口和现有应用流程不回退。
4. `Cuboid` 逐步退回为 placement/projection 基础设施兼容类型，不再作为业务层唯一几何真相。
5. 动态直径/半径分两个版本推进：先实现离散动态半径候选生成，再评估连续半径优化原型。
6. `Axis3.X` / `Axis3.Z` 横向圆柱先按窄口径开放：仅允许已知坐标的最终装箱/渲染路径通过真实 3D 几何 guard 后接受，仍不支持任意角度圆柱。
7. 支持可选的 depth 边界层硬约束：可分别限制第一个 layer 和最后一个 layer 的圆柱轴向或长方体朝向。
8. 长期目标才考虑去除 `Item : Cuboid<Item>`、重写 `QuantityPlacement2/3` 和 `CuboidView` 体系。

## 2. 已完成事项摘要

以下只保留结论，不保留实现流水账；需要查细节时查看 git 历史、测试记录和对应模块。

1. 竖直圆柱 `Axis3.Y` 已接入主链，shape metadata、真实体积、renderer DTO、loading rate 与 application 入口具备阶段性支持。
2. `PackingShape3` 已成为 domain 层 shape capability 入口，`Item` 已能从 `packingShape` 派生形状能力。
3. 不支持真实圆柱几何的旧搜索路径已显式 unsupported，避免静默退回外接盒判定。
4. `QuantityPlacement2/3(...)` 直写构造已集中到 `PlacementFactory`，业务调用侧对 placement 泛型的直接暴露已收敛。
5. `DemandConstraint` / `VolumeMinimization` 已提供 Item 专用顶层入口，application 调用侧不再直接依赖泛型基类 factory。
6. `Cuboid<*>` / `AbstractCuboid<...>` / `CuboidView<*>` / `QuantityPlacement*` 相关 compat 绑定已由 `shape-boundary-check.ps1` allowlist 分类约束；底层结构性绑定仍保留。
7. 离散动态半径/直径输入语义已闭合，并已接入候选生成、CSV 解析和相关回归。
8. 深度边界层轴向/朝向策略已接入 application 最终已放置结果硬校验；MILP 原生建模评估结论为暂不进入当前默认 MILP/候选生成链路。
9. `PackingGeometryGuard` 已接入最终 packing/rendering 路径，终态会复核容器边界、圆柱/长方体碰撞、圆柱/圆柱碰撞和横向圆柱贴地语义。
10. `Axis3.X` / `Axis3.Z` 横向圆柱已在最终装箱/渲染的已知坐标路径中开放；候选生成、block loading、LayerPlacementAdapter、stacking、hanging 和 circle packing 仍保持 Y-only 或 unsupported。
11. Renderer DTO 契约、`Cuboid + Axis3.Y Cylinder` fixture、X/Y/Z 三轴圆柱坐标 fixture、README 口径已完成阶段性补齐；外部 renderer 已由其它工程处理完成，并已复核构建与类型检查，实际显示效果仍需人工打开 fixture 或实际输出核对。
12. 连续半径优化已完成边界评估：不进入生产默认链路，后续只能按独立原型或候选生成增强重新建模。
13. 本轮门禁脚本、BPP3D 全量测试、外部 renderer build/type/check 已复核通过；Gurobi 求解回归和 CSV dataset suite 属于历史复核，后续提交前如有相关改动需重新执行。

## 3. 未完成事项

### 3.1 Cuboid 底层基础设施去绑定

**事项**

`Item : Cuboid<Item>`、`QuantityPlacement2/3`、`CuboidView`、`Bin`、`Container3`、`Layer`、`Block` 等底层 placement/projection 体系仍以 cuboid 为核心。下一轮目标是继续减少业务层与应用层对这些基础设施类型的感知，不要求一次性重写底层。

**计划**

1. 复跑并复核 `shape-boundary-check.ps1`，确认 allowlist 没有被新改动扩大。
2. 将仍暴露 cuboid 语义的 domain/service API 继续改为 shape capability 或明确 cuboid-only。
3. 对不得迁移的基础设施绑定补足 KDoc、命名和 unsupported 语义。
4. 只在局部边界推进，避免重写 `QuantityPlacement2/3`、`CuboidView` 和 projection 核心。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
3. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
4. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
5. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
6. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
7. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
8. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
9. `scripts/shape-boundary-check.ps1`

**验收标准**

1. application / domain service 不新增散落 `Cuboid` / `AbstractCuboid` / `CuboidView` / `QuantityPlacement*` 泛型暴露。
2. `QuantityPlacement2/3(...)` 直写构造只允许在基础设施内部和 `PlacementFactory`。
3. application 不直接调用 `DemandConstraint.forItem` / `VolumeMinimization.forItem`。
4. 仍保留的 cuboid 绑定必须有清晰边界说明。

### 3.2 横向圆柱从终态窄口径扩展

**事项**

`Axis3.X` / `Axis3.Z` 横向圆柱当前只在最终 packing/rendering 已知坐标路径中开放。下一轮如继续开放，需要把候选生成、支撑、stacking、hanging 和 LayerPlacementAdapter 的真实几何语义逐步补齐；缺失语义的路径继续 unsupported。

**计划**

1. 梳理所有 `only Axis3.Y` 门禁，标记为“可开放”“需支撑语义”“必须继续 unsupported”三类。
2. 为横向圆柱定义 layer 轴向语义，保持同一 `BinLayer` 内单一圆柱轴向。
3. 设计 X/Z 横向圆柱支撑模型，明确“贴地”“全长支撑”“叠放”三类规则。
4. 评估 circle packing 是否能扩展为横向圆柱候选生成，不能复用 Y 轴 XY 平面密排作为最终判定。
5. 对开放路径补正例和负例；未开放路径补显式 unsupported 测试。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
3. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
4. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
5. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
6. `bpp3d-domain-item-context/src/main/.../service/LoadingOrderCalculator.kt`
7. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
8. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
9. `bpp3d-infrastructure/src/main/.../PackingShape.kt`
10. `bpp3d-infrastructure/src/main/.../Placement.kt`

**验收标准**

1. X/Z 只能在真实碰撞、边界和支撑语义覆盖的路径中开放。
2. 缺少支撑或 stacking 语义的路径必须继续 unsupported。
3. 同一 `BinLayer` 内只允许一种圆柱轴向；不同 layer 可以使用不同轴向。
4. 横向圆柱不得复用 Y 轴 circle packing 平面假设作为最终判定。
5. 支撑测试覆盖贴地通过、悬空拒绝、全长支撑通过和局部悬空拒绝。

### 3.3 PackingGeometryGuard 几何能力强化

**事项**

`PackingGeometryGuard` 已覆盖终态 3D 边界和碰撞，但仍是轴对齐圆柱的阶段性 guard。下一轮需要继续补足数值边界、异轴圆柱边界样例、不同半径样例和错误信息可诊断性。

**计划**

1. 补同轴圆柱相切、轻微重叠、不同半径、不同长度的测试。
2. 补异轴圆柱相切、穿插、错位不相交的测试。
3. 补圆柱与长方体角点/边缘临界样例。
4. 明确 tolerance 语义，避免合法相切被误判为冲突。
5. 错误信息中保留 source、bin、item index 和冲突类型。

**修改清单**

1. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
2. `bpp3d-domain-packing-context/src/test/.../PackerAndRendererAdapterTest.kt`
3. `bpp3d-infrastructure/src/test/.../PackingShapeTest.kt`
4. `bpp3d-infrastructure/src/test/.../ContainerShapeTest.kt`

**验收标准**

1. 几何 guard 覆盖圆柱/圆柱同轴、异轴和圆柱/长方体临界样例。
2. 相切应通过，真实重叠应拒绝。
3. 错误信息足够定位 source、bin 和 item。
4. 新增测试不依赖外接盒作为最终碰撞判定。

### 3.4 连续半径优化原型

**事项**

当前生产链路只支持离散半径/直径候选。连续半径优化不进入默认生产链路，但可以在下一轮作为独立原型或候选生成增强进行评估。

**计划**

1. 写出连续半径优化模型草案，包括变量、目标函数、边界约束、非重叠约束和数值精度。
2. 选择小规模场景做原型，不接入默认 application 求解链路。
3. 评估连续结果如何转化为确定半径候选，再交给现有主链。
4. 明确失败 fallback：回退离散候选，不影响现有生产能力。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/PackageShapeSpec.kt`
2. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
3. `bpp3d-domain-layer-generation-context/src/test/.../*`
4. `README.md`
5. `README_ch.md`
6. `refactor.md`

**验收标准**

1. 连续半径优化未实现生产能力时，文档不得写成已完成。
2. 原型不得改变默认求解链路。
3. 离散半径/直径候选继续包含上下边界，并输出确定半径、确定 placement 和确定 actual volume。
4. 半径/直径一致性比较基于归一化 quantity 数值。

### 3.5 深度边界层约束下沉评估

**事项**

深度边界层策略当前是 application 后验硬校验。下一轮仅在确有收益时评估是否下沉到候选生成过滤或 MILP 原生约束；默认继续保持后验硬校验。

**计划**

1. 评估 first / last layer 约束下沉后对可行性解释、候选复用和求解性能的影响。
2. 若进入 MILP，定义 boundary side 变量、圆柱 axis 兼容约束和长方体 orientation 兼容约束。
3. 保持后验校验作为兜底，避免 solver 侧漏约束。
4. 补 CSV/Gurobi 场景级字段的完整回归。

**修改清单**

1. `bpp3d-application/src/main/.../service/ColumnGenerationStandardExecutors.kt`
2. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
3. `bpp3d-application/src/main/.../request-or-csv-mapping/*`
4. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
5. `bpp3d-application/src/test/.../DepthBoundaryLayerOrientationPolicyTest.kt`

**验收标准**

1. 若未下沉，文档必须明确仍是后验硬校验。
2. 字段缺失或为 `null` 时不增加限制。
3. 字段存在但允许集合为空时返回配置错误或不可行，不能解释为“不限制”。
4. 单 layer bin 同时应用 first / last；冲突时不可行。
5. 圆柱检查 axis，长方体检查 orientation。

### 3.6 Renderer 显示一致性人工核对

**事项**

外部 renderer 已由其它工程处理完成，且 build/type/check 已通过；但实际画面显示还未在本仓记录为通过。下一轮需要按外部 renderer `README_ch.md` 的“圆柱坐标设定指南”打开 fixture 或实际求解输出核对。

**计划**

1. 打开 `mixed-shape-renderer-schema.json`，核对长方体与 `Axis3.Y` 圆柱混装显示。
2. 打开 `cylinder-axis-renderer-schema.json`，核对 X/Y/Z 三轴圆柱坐标、外接盒和贴地语义。
3. 使用一次实际求解输出核对 layer 顺序、物体位置和本仓输出坐标一致。
4. 只记录实际打开并核对过的结果，不把 build/type/check 等同于显示通过。

**修改清单**

1. `bpp3d-infrastructure/src/test/resources/renderer/mixed-shape-renderer-schema.json`
2. `bpp3d-infrastructure/src/test/resources/renderer/cylinder-axis-renderer-schema.json`
3. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
4. `README.md`
5. `README_ch.md`
6. `refactor.md`
7. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer\README_ch.md`

**验收标准**

1. 显示核对覆盖圆柱轴向、半径/直径、外接盒尺寸、外接盒最小角点和贴地语义。
2. X/Z 横向圆柱在 `y = 0` 且 `boundingHeight = diameter` 时应贴地显示。
3. 长方体混装、layer 顺序和实际求解输出坐标一致。
4. 未实际打开核对的显示效果不得写成通过。

### 3.7 Gurobi / CSV suite 重新复核

**事项**

Gurobi 求解回归和 CSV dataset suite 最近记录属于历史复核。下一轮若继续修改 application、CSV、shape spec、depth boundary 或 solver 相关代码，需要重新执行并更新记录。

**计划**

1. 使用根 POM 运行 Gurobi 相关测试，避免本地 reactor 模块解析问题。
2. 复跑完整 `GurobiColumnGenerationTest`。
3. 复跑 CSV dataset suite。
4. 若环境不可用，明确记录阻塞原因，不把历史结果写成本轮通过。

**修改清单**

1. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
2. `bpp3d-application/src/test/resources/gurobi/*`
3. `bpp3d-application/src/main/.../request-or-csv-mapping/*`
4. `README.md`
5. `README_ch.md`
6. `refactor.md`

**验收标准**

1. 本轮执行过的 Gurobi / CSV 命令才可写入本轮复核。
2. 未执行时必须保留为历史复核或建议验收。
3. CSV shape 字段、动态半径/直径字段、X/Y/Z axis metadata 和 depth boundary 字段均保持兼容。

### 3.8 文档、门禁与提交隔离

**事项**

工作区当前存在 BPP3D 与既有 CSP1D 改动。下一轮提交前必须隔离无关模块，并继续保持文档、门禁和验证记录一致。

**计划**

1. 每轮开始执行 `git status --short --branch`，记录 BPP3D 与非 BPP3D 改动。
2. 提交前确认只 staged BPP3D 相关文件。
3. 复跑必跑门禁和 `git diff --check -- ospf-kotlin-framework-bpp3d`。
4. 已执行、历史执行、建议执行三类验证分开记录。
5. 提交信息具体说明重构目的、关键边界和验证结果。

**修改清单**

1. `README.md`
2. `README_ch.md`
3. `refactor.md`
4. `scripts/generic-boundary-check.ps1`
5. `scripts/shape-boundary-check.ps1`
6. `scripts/geometry-boundary-check.ps1`
7. `scripts/geometry-module-dry-run.ps1`

**验收标准**

1. 已完成事项只保留高层摘要，不恢复实现流水账。
2. 文档不引用当前代码中不存在的 API。
3. 本轮未执行的测试不能写成通过。
4. 历史验证、最近复核、建议验收必须分开。
5. 未完成的结构性绑定必须继续保留。
6. 非 BPP3D 改动不得混入 BPP3D 提交。

## 4. 建议验证命令

### 4.1 必跑命令

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

### 4.2 建议完整验收

```powershell
mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"
mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

外部 renderer 建议验收：

```powershell
cd E:\workspace\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
```

显示一致性建议验收：

1. 先阅读外部 renderer `E:\workspace\ospf\framework\bpp3d-interface-renderer\README_ch.md` 的“圆柱坐标设定指南”。
2. 使用本仓 `bpp3d-infrastructure/src/test/resources/renderer/mixed-shape-renderer-schema.json` 打开 renderer，核对长方体与 `Axis3.Y` 圆柱混装显示。
3. 使用本仓 `bpp3d-infrastructure/src/test/resources/renderer/cylinder-axis-renderer-schema.json` 核对 X/Y/Z 三轴圆柱坐标、外接盒和贴地语义。
4. 使用一次实际求解输出核对 renderer 中 layer 顺序、物体位置和本仓输出坐标一致。

## 5. 最近复核记录

最近复核日期：2026-06-04

本轮复核：

1. `generic-boundary-check.ps1`：通过。
2. `shape-boundary-check.ps1`：通过。
3. `geometry-boundary-check.ps1`：通过。
4. `geometry-module-dry-run.ps1`：通过，保留 8 个内部基线 warning。
5. `git diff --check -- ospf-kotlin-framework-bpp3d`：通过，仅保留 CRLF 工作区提示。
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：通过；BPP3D reactor build success，225 tests，0 failures；保留 Kotlin/JVM warning 与 JVM CodeHeap warning。
7. 外部 renderer `npm run build`：通过；保留 Vite chunk size warning。
8. 外部 renderer `npx vue-tsc --noEmit`：通过。
9. 外部 renderer `cargo check`：通过；保留 Rust workspace resolver warning 与 `static_mut_refs` warning。

历史复核：

1. application 普通链路测试通过。
2. Gurobi 求解回归通过。
3. CSV dataset suite 通过。
4. renderer DTO / fixture 契约测试通过。
5. 外部 renderer 构建与类型检查通过；实际显示效果一致性仍需按样例单独核对。

后续新增代码后必须重新执行相关验证；未执行的命令不得继续沿用本记录写成通过。
