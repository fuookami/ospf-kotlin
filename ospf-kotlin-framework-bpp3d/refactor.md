# BPP3D 形状泛型化与圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-04

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的阶段性交接状态。当前已完成 shape-aware 主链收敛，但尚未完成 BPP3D 完全泛型化；下一轮重点是继续拆除业务层可见的长方体基础设施绑定，并完成 renderer 视觉确认与外部验收补齐。

## 1. 已完成事项摘要

1. BPP3D 主流程已能稳定表达长方体、竖直圆柱和已知坐标路径下的 X/Z 横向圆柱。
2. 圆柱终态判定已使用真实几何 guard，不再把外接盒作为最终碰撞、边界或支撑判定。
3. 旧长方体路径、旧 DTO 字段、旧 CSV 入口和现有 application 普通链路保持兼容。
4. `PackingShape3` 已成为 domain 层 shape capability 入口，`Item` 已能从 `packingShape` 派生形状能力。
5. `Cuboid` 已从业务层唯一几何真相收敛为 placement/projection 兼容基础设施。
6. `ItemMerger` 与 `Item.kt` 的部分业务可见 `Cuboid<*>` / `CuboidView<*>` 暴露已收窄为 item-domain `ItemCuboid` 或更窄的 `ItemView` / `ItemPlacement3` 语义。
7. 动态半径/直径已按离散候选进入默认生产链路；连续半径优化不进入当前默认链路。
8. depth boundary 策略已作为 application 后验硬校验接入，暂不下沉为默认 MILP 原生约束或候选生成过滤。
9. renderer DTO、fixture、README 口径和仓内契约测试已完成阶段性补齐。
10. BPP3D 必跑门禁、BPP3D reactor 全量测试、Gurobi 普通回归和 CSV dataset suite 已通过；外部 renderer 自动验收仍受环境影响，不能写成本轮通过。

## 2. 下一轮目标

1. 继续推进 BPP3D 完全泛型化前置工作，减少 application / domain service 对 `Cuboid`、`CuboidView`、`QuantityPlacement2/3` 等底层兼容类型的感知。
2. 明确保留的底层 cuboid 结构性绑定，并将不能迁移的边界写入门禁、KDoc 和 unsupported 语义。
3. 完成 renderer 显示一致性验收，包括外部 renderer 自动构建/类型检查和人工视觉确认。
4. 在依赖环境恢复后重新复核 Gurobi column generation 与 CSV dataset suite。
5. 保持当前阶段性生产链路稳定，不把横向圆柱候选生成、连续半径优化或 depth boundary 下沉误写成默认能力。

静态边界复核：

1. `LayerGenerationContext` 的 circle-packing 候选仍只允许 `Axis3.Y` 圆柱；X/Z 横向圆柱不会进入默认 circle packing 候选生成。
2. `SimpleBlockGenerator` 对圆柱保留 `Axis3.Y` 且 upright-only 的显式 unsupported 语义；side/lie stacking 不开放。
3. `DepthFirstSearchAlgorithm` / `MultiLayerHeuristicSearchAlgorithm` 仍通过 `requireNoCylinderItemsForCuboidSearch` 拒绝圆柱进入 cuboid-only 空间切分路径。
4. `PackingGeometryGuard` 与 `Packer` 保留终态已知坐标路径的真实几何 guard、横向圆柱 full-length support 校验和同 layer 单一圆柱轴向限制。
5. 本轮未开放横向圆柱候选生成、block loading、stacking、hanging 或 circle packing 默认能力。

## 3. 下一轮事项

### 3.1 继续收窄 Cuboid 业务可见边界

**事项**

梳理仍在业务服务、application、测试辅助入口中暴露的 `Cuboid`、`CuboidView`、`AbstractCuboid`、`QuantityPlacement2/3` 类型，优先改为 item-domain 或 shape-domain 语义类型。底层 placement/projection 体系暂不强行重写。

**计划**

1. 复跑 `shape-boundary-check.ps1`，生成当前 allowlist 差异基线。
2. 逐个评估 allowlist 项是否属于底层结构性绑定、兼容扩展，还是可迁移的业务暴露。
3. 对可迁移项改为 `ItemCuboid`、`PackingShape3`、domain-specific placement alias 或更窄的 item-domain API。
4. 对暂不能迁移项补足边界说明和 unsupported 语义，并保持 allowlist 分类清晰。
5. 每次迁移后复跑边界脚本和最小相关测试，避免扩大 blast radius。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
3. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
4. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
5. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
6. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
7. `bpp3d-domain-item-context/src/main/.../service/ItemMerger.kt`
8. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
9. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
10. `scripts/shape-boundary-check.ps1`

**验收标准**

1. 不新增散落的 `Cuboid<*>`、`AbstractCuboid<...>`、`CuboidView<*>`、`QuantityPlacement*` 业务层暴露。
2. 可迁移的业务入口不再依赖长方体泛型基础设施作为公开语义。
3. 不可迁移项必须在 allowlist 中有明确分类，且不扩大现有边界。
4. `shape-boundary-check.ps1` 通过。
5. BPP3D reactor 全量测试通过。

### 3.2 Renderer 自动验收与视觉确认

**事项**

补齐外部 renderer 构建、类型检查、Rust 检查和实际画面核对。视觉确认前必须先询问用户当前环境是否可以人工核对；未实际打开或截图确认的画面效果不得写成通过。

**计划**

1. 先询问用户当前环境是否可以进行 renderer 人工画面核对。
2. 确认外部 renderer 目录为 `E:\workspace\ospf\ospf\framework\bpp3d-interface-renderer`。
3. 恢复外部 renderer 本地依赖后执行 `npm run build`、`npx vue-tsc --noEmit`、`cargo check`。
4. 使用 `mixed-shape-renderer-schema.json` 核对长方体与 Y 轴圆柱混装显示。
5. 使用 `cylinder-axis-renderer-schema.json` 核对 X/Y/Z 三轴圆柱坐标、半径/直径、外接盒尺寸、外接盒最小角点和贴地语义。
6. 使用一次实际求解输出核对 renderer 中 layer 顺序、物体位置和本仓输出坐标一致。
7. 将自动验收、人工视觉验收和无法执行原因分开记录。

**修改清单**

1. `bpp3d-infrastructure/src/test/resources/renderer/mixed-shape-renderer-schema.json`
2. `bpp3d-infrastructure/src/test/resources/renderer/cylinder-axis-renderer-schema.json`
3. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
5. `README.md`
6. `README_ch.md`
7. `refactor.md`
8. 外部工程：`E:\workspace\ospf\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 外部 renderer `npm run build`、`npx vue-tsc --noEmit`、`cargo check` 实际执行并记录结果。
2. 人工视觉确认覆盖混装样例、X/Y/Z 圆柱轴向、外接盒、贴地语义和实际求解输出。
3. X/Z 横向圆柱在 `y = 0` 且 `boundingHeight = diameter` 时贴地显示。
4. renderer layer 顺序、物体位置和本仓输出坐标一致。
5. 未执行或失败的 renderer 项不得写成通过。

### 3.3 横向圆柱从终态窄口径继续评估

**事项**

当前 X/Z 横向圆柱仅在已知坐标的最终 packing/rendering 路径开放。下一轮只评估是否存在低风险扩展点，不默认开放候选生成、block loading、stacking、hanging 或 circle packing。

**计划**

1. 梳理所有 `Axis3.Y` 限制点和 unsupported 路径。
2. 区分最终已知坐标校验、候选生成、支撑、堆叠和密排建模语义。
3. 仅在真实几何语义完整、测试可覆盖时开放局部路径。
4. 对不开放路径保持显式 unsupported。
5. 不复用 Y 轴 circle packing 平面假设作为 X/Z 横向圆柱最终判定。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
3. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
4. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
5. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
6. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
7. 相关测试文件

**验收标准**

1. X/Z 横向圆柱只在碰撞、边界和支撑语义完整的路径开放。
2. 缺少语义的路径继续 unsupported。
3. 同一 `BinLayer` 内仍只允许一种圆柱轴向；不同 layer 可以使用不同轴向。
4. 支撑、碰撞、边界测试覆盖新开放路径。
5. 默认生产链路不因评估产生行为回退。

### 3.4 Gurobi 与 CSV suite 重新复核

**事项**

在 Gurobi 插件依赖和内网 Maven 仓库恢复后，重新执行 Gurobi column generation 与 CSV dataset suite。未执行成功前只能保留历史复核，不得写成本轮通过。

**计划**

1. 先确认 `ospf-kotlin-core-plugin-gurobi:1.1.0` 依赖可解析。
2. 顺序执行 Gurobi 普通回归，避免并发或超时误判。
3. 普通回归通过后再执行 CSV dataset suite。
4. 记录通过、失败、跳过和环境阻断的准确原因。
5. 若修改 application、CSV、shape spec、depth boundary 或 solver 相关代码，必须重新执行本节验收。

**修改清单**

1. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
2. `bpp3d-application/src/test/resources/gurobi/*`
3. `bpp3d-application/src/main/.../request-or-csv-mapping/*`
4. `bpp3d-application/src/main/.../service/*`
5. `README.md`
6. `README_ch.md`
7. `refactor.md`

**验收标准**

1. Gurobi 普通回归实际执行并通过。
2. CSV dataset suite 实际执行并通过。
3. CSV shape 字段、动态半径/直径字段、X/Y/Z axis metadata 和 depth boundary 字段保持兼容。
4. 依赖解析失败、许可证失败或超时必须写成失败或未完成。
5. 不用历史记录覆盖本轮未执行结果。

### 3.5 文档、门禁与提交隔离

**事项**

保持 `refactor.md`、README、门禁脚本和验证记录一致。当前工作区存在非 BPP3D 改动，下一轮提交前必须隔离。

**计划**

1. 提交前执行 `git status --short --branch`，确认 BPP3D 与非 BPP3D 改动范围。
2. 仅 stage 与 BPP3D 重构相关的文件。
3. 每次代码改动后执行必跑门禁。
4. 文档中区分已执行、失败、跳过、历史复核和建议验收。
5. 提交信息明确说明重构目标、关键边界和验证结果。

**修改清单**

1. `README.md`
2. `README_ch.md`
3. `refactor.md`
4. `scripts/generic-boundary-check.ps1`
5. `scripts/shape-boundary-check.ps1`
6. `scripts/geometry-boundary-check.ps1`
7. `scripts/geometry-module-dry-run.ps1`

**验收标准**

1. 文档不引用当前代码中不存在的 API。
2. 未执行或失败的外部项不得写成通过。
3. 历史验证和最近复核分开记录。
4. 非 BPP3D 改动不得混入 BPP3D 提交。
5. `git diff --check -- ospf-kotlin-framework-bpp3d` 通过。

## 4. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 5. 建议完整验收

```powershell
mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"
mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

外部 renderer 自动验收：

```powershell
cd E:\workspace\ospf\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
```

## 6. 最近复核记录

最近复核日期：2026-06-04

本轮复核：

本轮新增进展：

1. `Item.kt` 删除面向所有 `CuboidView<*>` 的 `packageType`、`packageCategory`、`bottomOnly`、`topFlat` 通用兼容扩展。
2. `ItemProjection`、`ItemPlacement2`、`ItemPlacement3` 的 `topFlat` 改为显式读取 `ItemView.topFlat`，保留 orientation-sensitive 语义。
3. `PackageAttribute` 中的直接 `view.topFlat` 读取改为 `ItemPlacement3.topFlat`，避免回落到通用 `CuboidView<*>` 兼容扩展。
4. `shape-boundary-check.ps1` 新增 `CuboidViewWildcardOutOfAllowList`，仅允许基础设施 `Cuboid.kt` 保留结构性 `CuboidView<*>` 绑定。

本轮复核：

1. `generic-boundary-check.ps1`：通过。
2. `shape-boundary-check.ps1`：通过，新增 `CuboidViewWildcardOutOfAllowList` 后仍通过。
3. `geometry-boundary-check.ps1`：通过。
4. `geometry-module-dry-run.ps1`：通过，保留 8 个内部基线 warning。
5. `git diff --check -- ospf-kotlin-framework-bpp3d`：通过，仅保留 CRLF 工作区提示。
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am -Dtest=ItemMergerCylinderTest,MixedShapeGeometryTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：通过；17 tests，0 failures；保留既有 Kotlin warning。
7. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：第一次工具超时未形成结论；随后延长超时重跑通过，BPP3D reactor build success，230 tests，0 failures；保留既有 Kotlin/JVM warning 与 JVM CodeHeap warning。
8. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-packing-context -am -Dtest=PackerAndRendererAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：上一轮通过；本轮未单独重跑，但已由第 7 项 BPP3D reactor 全量覆盖。
9. `mvn -f pom.xml -pl ospf-kotlin-framework-bpp3d/bpp3d-application -am test "-Dgpg.skip=true"`：上一轮失败于进入 BPP3D 前的上游 `ospf-kotlin-utils` 测试；本轮未重跑根 POM application 普通链路，BPP3D application 普通测试已由第 7 项 BPP3D reactor 全量覆盖。
10. 外部 renderer 自动验收：本轮执行，未通过；外部目录 `E:\workspace\ospf\ospf\framework\bpp3d-interface-renderer` 存在但 `node_modules` 缺失，`npm run build` 失败于找不到本地 `vite`，`npx vue-tsc --noEmit` 失败于找不到 `vue`、`three`、`lodash`、Tauri / Vuetify 等依赖类型，`cargo check` 首次 184 秒超时、延长到 424 秒后仍超时，未形成通过结论。
11. `mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true`：通过；确认 `ospf-kotlin-core-plugin-gurobi` 是本仓模块并已安装到本机 Maven 仓库。Dokka 阶段保留 Kotlin metadata version warning。
12. Gurobi 普通回归：通过；在第 11 项安装本地 Gurobi 插件后，执行 `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true`，`GurobiColumnGenerationTest` 26 tests，0 failures，0 errors，1 skipped；保留 JVM CodeHeap warning 与 surefire dumpstream warning。
13. CSV dataset suite：通过；执行 `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`，`GurobiColumnGenerationTest` 26 tests，0 failures，0 errors，0 skipped；3 个 CSV dataset precheck 与求解样例通过，保留 JVM CodeHeap warning 与 surefire dumpstream warning。
14. 外部 renderer 人工画面核对：按用户要求继续跳过；下次处理前必须先询问当前环境是否可以人工核对。
15. 纠正说明：先前直接用 BPP3D 子 POM 跑 Gurobi 会绕过本仓 `ospf-kotlin-core-plugin-gurobi` reactor 模块，导致错误地尝试从内网仓库解析同版本 artifact；本轮已改为先安装本仓插件再复跑 BPP3D Gurobi / CSV 验收。
16. 横向圆柱静态边界复核：通过；候选生成、block loading、stacking、hanging、circle packing 默认能力未开放，终态 packing/rendering 窄口径 guard 仍保留。

历史复核：

1. renderer DTO / fixture 契约测试通过。
2. 外部 renderer 曾经构建与类型检查通过；本轮自动复核未通过。
3. Gurobi column generation 与 CSV dataset suite 曾通过；本轮已重新复核通过。

后续新增代码后必须重新执行相关验证；未执行或失败的命令不得继续沿用历史记录写成通过。
