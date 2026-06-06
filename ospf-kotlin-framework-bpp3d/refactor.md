# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-06

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与下一轮执行计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 本轮已完成事项摘要

1. 已完成 BPP3D shape-aware / generic shape 关键路径审计，本轮继续保持 X/Z 横向圆柱自动候选生成关闭。
2. 已把泛型 known-coordinate 终态分析入口纳入 depth boundary 后验硬校验，覆盖自动构造 final bins 和显式 final bins 两种入口。
3. 已扩展泛型 known-coordinate 回归，覆盖 X/Z 横向圆柱、multi-bin、混合轴向拒绝和 depth boundary positive/negative 行为。
4. 已扩展 Gurobi CSV 数据集样例，覆盖 depth boundary policy 和动态直径 metadata，并保持旧 CSV、旧长方体链路和竖直圆柱链路兼容。
5. 已同步 README / README_ch，明确 depth boundary 是最终 MILP 后或泛型 known-coordinate final bins 构造后的 application 层硬校验，不是 MILP 原生约束或候选过滤器。
6. 本轮未触发 renderer DTO、fixture、adapter 或显示语义变化；外部 renderer 验收未触发。

## 2. 本轮验证记录

1. `ColumnGenerationPackingAnalyzerGenericEntryPointTest` focused reactor：通过，8 个测试全部通过。
2. `GurobiColumnGenerationTest` CSV focused reactor：通过，新增 2 个 CSV 样例解析测试全部通过。
3. BPP3D 必跑门禁：通过；四个边界/几何脚本通过，`git diff --check -- ospf-kotlin-framework-bpp3d` 通过，BPP3D 全量 reactor `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true` 通过。
4. Gurobi 插件 install：通过；`ospf-kotlin-core-plugin-gurobi` install 成功，Dokka 阶段存在 Kotlin metadata 版本提示但未阻断构建。
5. Gurobi 普通回归：通过；`GurobiColumnGenerationTest` 在 Gurobi enabled 条件下 28 个测试运行，0 失败，0 错误，1 个按条件跳过。
6. Gurobi CSV dataset suite：通过；5 个 CSV 样例完成预检查和求解，`GurobiColumnGenerationTest` 28 个测试运行，0 失败，0 错误，0 跳过。
7. 外部 renderer 自动和人工视觉验收：本轮未触发。

## 3. 总目标与当前能力边界

### 3.1 总目标

下一轮目标是在一次迭代内尽可能完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 3.2 当前保留边界

1. BPP3D 尚未完全泛型化；底层 placement/projection 体系允许保留必要 cuboid 结构性绑定，但业务层不能继续扩散新的 cuboid-only API。
2. X/Z 横向圆柱当前只允许在已知坐标终态 packing/rendering 路径表达和校验。
3. 默认自动候选生成、layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 尚未开放 X/Z 横向圆柱。
4. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
5. known-coordinate placement 是 final validation path，不是 generated candidate path；新增调用必须通过脚本白名单审计。
6. depth boundary 是 application 层后验硬校验；目前覆盖最终 MILP selected bins 和泛型 known-coordinate final bins，尚未下沉为 MILP 原生约束。
7. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。
8. renderer 源码在外部工程，BPP3D 仓内只维护 DTO、fixture、adapter 和文档契约。

## 4. 下一轮目标

1. 一次性完成剩余业务层 shape contract 收口，把可迁移 API 从 cuboid/placement/projection 泄漏收敛到 shape spec、packing shape、item view、domain placement alias 或更窄接口。
2. 对横向圆柱自动候选生成、block loading、DFS/MLHS、stacking 和 hanging 做最终决策：能完整证明的最小子路径开放，不能完整证明的路径统一拒绝。
3. 继续扩大已知坐标终态生产验收路径，覆盖更多支撑、碰撞、multi-bin、mixed shape、depth boundary、renderer 输出和错误信息场景。
4. 统一 application、layer assignment、CSV/Gurobi、dynamic radius/diameter、axis metadata 和 depth boundary policy 的 shape-aware 解释，并补齐非法字段/非法策略负例。
5. 评估并尽量实现 depth boundary MILP 原生下沉；若不下沉，必须写清不可下沉原因，并保留后验硬校验、门禁和回归测试。
6. 若触发 renderer DTO、fixture、adapter 或显示语义变化，同步仓内和外部 renderer，并完成自动与人工视觉验收。
7. 下一轮结束时形成清晰提交边界：BPP3D、本仓其他模块和外部 renderer 分开提交，不混入无关改动。

## 5. 下一轮事项、计划、修改清单与验收标准

### 5.1 Shape Contract 与公开 API 收口

**事项**

统一审计 item-domain、shape-domain、application、layer generation、BLA、block loading、packing、layer assignment 和 renderer adapter 的 shape contract，减少业务层对底层 cuboid/projection/placement 类型的直接暴露。

**计划**

1. 扫描所有 `shape`、`axis`、`cuboid`、`placement`、`projection`、`renderer DTO`、`CSV metadata`、`known-coordinate` 和 `unsupported` 命中，重新分类为保留、迁移、拒绝或测试专用。
2. 清理 stale allowlist；保留项必须写明归属、用途和迁移条件。
3. 把 shape-sensitive 行为集中到 `PackageShapeSpec`、`PackingShape3`、`ItemView`、`ItemPlacement2/3`、domain alias、shape capability 或新的窄接口。
4. 为无法迁移的兼容入口补齐中英双语 KDoc，说明保留原因、调用边界和迁移条件。
5. 升级边界脚本，使新增业务层 cuboid 泄漏、known-coordinate 旁路误用、重复 unsupported 文案、renderer DTO 漂移和过期字段命中可以直接定位。

**修改清单**

1. `scripts/generic-boundary-check.ps1`
2. `scripts/shape-boundary-check.ps1`
3. `scripts/geometry-boundary-check.ps1`
4. `scripts/geometry-module-dry-run.ps1`
5. `bpp3d-domain-item-context/src/main/**/*`
6. `bpp3d-domain-layer-generation-context/src/main/**/*`
7. `bpp3d-domain-bla-context/src/main/**/*`
8. `bpp3d-domain-block-loading-context/src/main/**/*`
9. `bpp3d-domain-packing-context/src/main/**/*`
10. `bpp3d-domain-layer-assignment-context/src/main/**/*`
11. `bpp3d-application/src/main/**/*`
12. `bpp3d-infrastructure/src/main/**/*`
13. `bpp3d-*/src/test/**/*Boundary*Test.kt`

**验收标准**

1. 新增业务代码不直接暴露底层 cuboid/projection 作为 shape 语义。
2. 保留的 cuboid/projection/placement 绑定均有 allowlist 分类和迁移条件。
3. unsupported message 由共享契约统一输出，禁止模块内重复硬编码。
4. 四个边界/几何脚本全部通过，且失败输出能给出违规类别和建议迁移方向。

### 5.2 Item-Domain、支撑语义与业务对象泛型化

**事项**

把尺寸、体积、footprint、top/bottom、支撑、stacking、hanging、placement 构造和 shape capability 收敛到 item-domain / shape-domain API，统一终态对象和候选对象的 shape 解释。

**计划**

1. 复核 `Item`、`Package`、`PackageAttribute`、`ItemContainer`、`Bin`、`Layer`、`Block`、`Pattern`、`PlacementFactory` 的公开属性、typealias 和构造入口。
2. 明确 cuboid、竖直圆柱、横向圆柱在 item-domain 中的能力矩阵和支撑前置条件。
3. 检查 `ItemMerger`、`LoadingOrderCalculator`、`DemandStatistics`、`MaterialDemandReducedCost` 是否仍有隐式长方体假设。
4. 统一 stacking/hanging/top-layer 的圆柱错误信息和 guard 调用入口。
5. 补齐 cuboid、竖直圆柱、横向圆柱终态对象的 domain tests。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
3. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
4. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
5. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
6. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
7. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
8. `bpp3d-domain-item-context/src/main/.../model/Pattern.kt`
9. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
10. `bpp3d-domain-item-context/src/main/.../model/CylinderShapeContract.kt`
11. `bpp3d-domain-item-context/src/main/.../service/*`
12. `bpp3d-domain-item-context/src/test/**/*`

**验收标准**

1. shape-sensitive 行为均由 item-domain / shape-domain API 表达。
2. cuboid、竖直圆柱和横向圆柱终态对象均有 positive/negative tests。
3. stacking/hanging/top-layer 的 unsupported contract 可断言且不重复。
4. item-context focused tests 通过，旧长方体构造和旧测试夹具继续兼容。

### 5.3 候选生成、BLA、Block Loading 与 Search 路径决策

**事项**

对 layer generation、circle packing、BLA、simple block、complex block、DFS、MLHS、stacking 和 hanging 做一次总决策，消除半开放状态。

**计划**

1. 拆分 shape-neutral 逻辑、Y 轴 circle packing 假设、cuboid-only 假设、block 空间拆分假设和 BLA 依赖假设。
2. 为 X/Z 横向圆柱建立 footprint、可放置平面、半径/直径、轴向长度、边界、同层轴向、支撑前置条件和候选去重决策表。
3. 评估是否开放受限自动候选子路径；若开放，必须实现真实 footprint packing、候选去重、同层轴向限制、支撑前置条件和完整 tests。
4. 若不开放横向圆柱自动候选，统一拒绝入口、错误信息、README 支持矩阵、negative tests 和脚本门禁。
5. 评估竖直圆柱是否可以进入更宽的 block loading 或 stacking/hanging 子路径；无法证明的路径保持 shared unsupported。
6. 清理重复 guard，确保 block loading、DFS/MLHS、stacking、hanging 的支持矩阵只有一个来源。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-layer-generation-context/src/main/**/*`
3. `bpp3d-domain-bla-context/src/main/**/*`
4. `bpp3d-domain-block-loading-context/src/main/**/*`
5. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
6. `bpp3d-domain-layer-generation-context/src/test/**/*`
7. `bpp3d-domain-bla-context/src/test/**/*`
8. `bpp3d-domain-block-loading-context/src/test/**/*`
9. `bpp3d-application/src/test/**/*`

**验收标准**

1. 每个候选生成、BLA、block loading 和 search 入口都有明确支持矩阵。
2. Y 轴 circle packing 假设不会被 X/Z 横向圆柱误复用。
3. 新开放路径具备候选、边界、碰撞、支撑、同层轴向、去重和搜索正确性测试。
4. 未开放路径具备统一 unsupported message、negative tests 和脚本保护。
5. layer-generation、BLA、block-loading focused tests 通过。

### 5.4 Packing Final Guard、Known-Coordinate 与 Depth Boundary

**事项**

把已知坐标终态路径作为完整生产验收路径，而不是测试绕过路径；同时评估并尽量下沉 depth boundary 到 MILP。

**计划**

1. 统一 `axis`、orientation、footprint、height/depth/width、bounding box、material identity、layer identity 和 loading order 的解释。
2. 强化 packing final guard 对 X/Z 横向圆柱的 boundary、collision、support、axis mixing、same-layer policy、multi-bin 和 depth boundary 场景校验。
3. 补齐贴地支撑、全长支撑、局部支撑拒绝、跨层 depth boundary、跨 bin mixed shape 和 renderer 输出测试。
4. 设计 depth boundary MILP 原生约束方案，覆盖 first/last layer、multi-bin、mixed shape、横向圆柱和旧数据兼容。
5. 若方案成立，实现建模和 Gurobi 回归；若方案不成立，保留 application 后验硬校验，并记录不可下沉原因和防误用门禁。

**修改清单**

1. `bpp3d-application/src/main/.../service/ColumnGenerationPackingAnalyzer.kt`
2. `bpp3d-application/src/main/.../service/ColumnGenerationApplicationService.kt`
3. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
4. `bpp3d-application/src/main/.../service/DepthBoundaryLayerOrientationPolicy.kt`
5. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
6. `bpp3d-domain-packing-context/src/main/.../service/Packer.kt`
7. `bpp3d-domain-packing-context/src/main/.../service/MaterialPacker.kt`
8. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
9. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
10. `bpp3d-domain-layer-assignment-context/src/main/**/*`
11. `bpp3d-application/src/test/**/*`
12. `bpp3d-domain-packing-context/src/test/**/*`
13. `bpp3d-domain-layer-assignment-context/src/test/**/*`

**验收标准**

1. 已知坐标 X/Z 横向圆柱使用真实几何判定，不以外接盒作为最终可行性证明。
2. explicit bins、generic known-coordinate layers、multi-bin、mixed shape、depth boundary 均有 positive/negative tests。
3. depth boundary 若下沉到 MILP，必须有普通回归和 CSV dataset suite 覆盖。
4. depth boundary 若不下沉，文档、错误信息和测试明确说明仍是后验硬校验。
5. application focused tests、packing focused tests 和 layer-assignment focused tests 通过。

### 5.5 CSV/Gurobi、Dynamic Radius 与 Shape Metadata

**事项**

统一 CSV/Gurobi、application request、layer assignment 和 solver adapter 对 shape identity、axis、radius/diameter、volume、layer key 和 depth boundary policy 的解释。

**计划**

1. 复核 `DemandConstraint`、`VolumeMinimization`、`LayerAggregation`、`MaterialPacker`、Gurobi adapter 对 shape metadata 的使用。
2. 确认 radius/diameter、dynamic radius/diameter、axis、depth boundary policy 在 request、CSV、Gurobi dataset suite 中共用同一解析契约。
3. 扩展 CSV dataset suite，覆盖旧格式、混装、竖直圆柱、横向圆柱 metadata、dynamic radius/diameter、depth boundary 和非法字段。
4. 保持旧 CSV、旧 DTO 和旧长方体 Gurobi 链路兼容。
5. 如果引入连续半径优化，必须先完成数据契约、建模契约、求解契约和 Gurobi 回归；否则继续明确不进入生产链路。

**修改清单**

1. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
2. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
3. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
4. `bpp3d-application/src/main/.../service/ColumnGenerationApplicationService.kt`
5. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
6. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
7. `bpp3d-application/src/test/resources/gurobi/*`
8. `bpp3d-domain-layer-assignment-context/src/test/**/*`
9. `bpp3d-application/src/test/**/*`

**验收标准**

1. layer assignment、application、CSV 和 Gurobi 对 shape metadata 的解释一致。
2. CSV dataset suite 覆盖旧格式、混装、竖直圆柱、横向圆柱 metadata、dynamic radius/diameter、depth boundary 和非法字段。
3. Gurobi 普通回归和 CSV dataset suite 通过。
4. 未开放的连续半径优化具备明确文档和防误用测试。

### 5.6 Renderer、DTO、Fixture 与外部验收

**事项**

在触发 renderer 语义变化时，同步仓内 DTO/fixture 与外部 Rust/TS renderer，并执行自动与人工视觉验收。

**计划**

1. 对齐仓内 renderer DTO 与外部 renderer DTO 的 `algorithmShapeType`、`renderShapeType`、`axis`、`bounding*`、`actualVolume` 和 shape metadata 字段。
2. 补齐混装、X/Y/Z 三轴、横向圆柱贴地、全长支撑、depth boundary、multi-bin、非法支撑和实际求解输出 fixture。
3. 更新仓内 `RendererDTOTest`、packing renderer adapter tests、外部 renderer DTO 反序列化测试和 README 样例。
4. 执行外部 renderer 自动检查和人工视觉确认；人工确认必须区分通过、失败、未执行和未触发。

**修改清单**

1. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
2. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
3. `bpp3d-infrastructure/src/test/resources/renderer/*`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
5. `bpp3d-domain-packing-context/src/test/**/*`
6. `README.md`
7. `README_ch.md`
8. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 仓内 renderer DTO 契约测试通过。
2. 外部 renderer `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test` 通过。
3. 人工视觉确认覆盖混装、X/Y/Z 三轴、横向圆柱贴地、全长支撑、外接盒语义、depth boundary 和实际求解输出。
4. 未实际执行的人工项不得写成通过。

### 5.7 文档、验证与提交隔离

**事项**

下一轮完成时同步 README、README_ch、refactor.md，执行完整门禁，并将 BPP3D、本仓其他模块和外部 renderer 的提交边界分开。

**计划**

1. 修改前记录 `git status --short --branch`，确认 BPP3D、本仓其他模块和外部 renderer 改动范围。
2. 每个子任务结束后执行 focused tests；全部完成后执行必跑门禁。
3. 涉及 application、CSV、shape spec、depth boundary 或 solver 时执行 Gurobi 插件 install、Gurobi 普通回归和 CSV dataset suite。
4. 涉及 renderer DTO、fixture、adapter 或显示语义时执行外部 renderer 自动验收和人工视觉确认。
5. README、README_ch、refactor.md 只记录真实执行结果，不把历史结果写成本轮通过。
6. 提交前只 stage BPP3D 相关文件；外部 renderer 与非 BPP3D 改动单独处理。

**修改清单**

1. `README.md`
2. `README_ch.md`
3. `refactor.md`
4. `scripts/*.ps1`
5. `bpp3d-*/src/test/**/*`
6. 外部 renderer 文档、DTO 和测试文件

**验收标准**

1. 必跑门禁全部通过。
2. 被当前改动触发的完整验收全部实际执行并记录。
3. 文档区分通过、失败、跳过、未触发和环境阻断。
4. BPP3D 提交不包含非 BPP3D 改动。
5. 提交信息具体说明重构目标、关键边界、开放能力、保留 unsupported 能力和验证结果。

## 6. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 7. 触发式完整验收

修改 application、CSV、shape spec、depth boundary 或 solver 相关代码时执行：

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

修改 renderer DTO、renderer fixture、packing renderer adapter 或显示语义时执行：

```powershell
cd E:\workspace\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
cargo test
```

## 8. 下一轮完成定义

1. 第 5 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 新增开放能力具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的能力具备统一错误信息、negative tests 和门禁保护。
4. BPP3D 必跑门禁全部通过。
5. 被实际改动触发的完整验收全部执行并记录。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
