# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-05

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的下一轮计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. BPP3D 主链已从长方体基线推进到 shape-aware 主链，支持长方体、竖直圆柱和已知坐标终态路径下的 X/Z 横向圆柱。
2. packing 终态几何、renderer 输出、README、测试夹具和人工视觉确认已形成阶段性闭环。
3. application/generic 已知坐标输入路径已允许 X/Z 横向圆柱进入真实几何校验，默认候选生成路径仍保持竖直圆柱门禁。
4. 业务层主要 shape-aware 入口、圆柱轴向契约、unsupported contract、边界脚本基线和支持矩阵已完成阶段性收敛。
5. 旧长方体路径、旧 DTO、旧 CSV、Gurobi 普通回归和 CSV dataset suite 已保持兼容。
6. 横向圆柱已知坐标生产验收、multi-bin、支撑、未开放路径防误用和 BPP3D 提交隔离已完成。

## 2. 总目标与当前边界

### 2.1 总目标

下一轮目标是在一次迭代内尽可能完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 当前保留边界

1. BPP3D 完全泛型化尚未完成；底层 placement/projection 体系仍允许保留必要 cuboid 结构性绑定。
2. X/Z 横向圆柱目前只允许在已知坐标终态 packing/rendering 路径表达和校验。
3. 默认自动候选生成、layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 尚未开放 X/Z 横向圆柱。
4. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
5. depth boundary 仍是 application 后验硬校验，尚未下沉为 MILP 原生约束。
6. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。

## 3. 下一轮目标

1. 将 item-domain、shape-domain、application、layer generation、BLA、block loading、packing、layer assignment、CSV/Gurobi、renderer 和脚本门禁的 shape contract 做一次跨层收口。
2. 尽量把业务层公开 API 从底层 cuboid/placement/projection 依赖迁移到 `PackageShapeSpec`、`PackingShape3`、`ItemView`、domain placement alias、shape capability 或更窄接口。
3. 对 X/Z 横向圆柱自动候选生成做完整决策：能证明的最小子路径实现并验收；不能证明的路径统一拒绝，且不能影响已知坐标终态路径。
4. 对 block loading、DFS/MLHS、stacking、hanging 做完整决策，消除重复 guard，并把所有保留 unsupported 的路径纳入 negative tests。
5. 统一 layer assignment、CSV/Gurobi、dynamic radius/diameter、axis metadata、depth boundary policy 和 request/fixture 解析口径。
6. 评估并尽量实现 depth boundary MILP 原生下沉；若不下沉，必须记录不可下沉原因并增加防误用门禁。
7. 若触发 renderer DTO、fixture、adapter 或显示语义变化，同步仓内和外部 renderer，并完成自动与人工视觉验收。
8. 下一轮结束时形成 BPP3D 独立提交；非 BPP3D、quantities、CSP1D 和外部 renderer 改动不得混入。

## 4. 下一轮事项、计划、修改清单与验收标准

### 4.1 Shape Contract 审计与门禁升级

**事项**

建立下一轮 shape contract 审计矩阵，覆盖 shape、axis、placement、projection、renderer DTO、CSV metadata、unsupported message 和 stale allowlist，并让脚本可以定位新增违规类别。

**计划**

1. 扫描 BPP3D 中所有 shape、axis、cuboid、placement、projection、renderer DTO、CSV metadata 和 unsupported 文案命中。
2. 将命中分类为基础设施保留、domain typed factory、已知坐标终态路径、候选生成路径、search path、renderer 契约、测试夹具和过期命中。
3. 清理 stale allowlist；仍需保留的 allowlist 必须有归属、用途和迁移条件。
4. 升级脚本失败输出，使新增违规能直接指向违规类别和建议迁移方向。
5. 将脚本结果作为本轮必跑门禁，不沿用历史结果。

**修改清单**

1. `scripts/generic-boundary-check.ps1`
2. `scripts/shape-boundary-check.ps1`
3. `scripts/geometry-boundary-check.ps1`
4. `scripts/geometry-module-dry-run.ps1`
5. `bpp3d-*/src/test/**/*Boundary*Test.kt`
6. `README.md`
7. `README_ch.md`
8. `refactor.md`

**验收标准**

1. 四个边界/几何脚本全部通过。
2. allowlist 无 stale 命中，保留项均有分类和迁移条件。
3. 新增业务层 cuboid 泄漏、重复 unsupported message、renderer DTO 漂移和过期字段命中可被脚本定位。
4. `git diff --check -- ospf-kotlin-framework-bpp3d` 通过。

### 4.2 Item-Domain 与公开 API 泛型化收口

**事项**

继续把尺寸、体积、投影、支撑、top/bottom、stacking、hanging、placement 构造和 shape capability 收敛到 item-domain / shape-domain API，减少业务层直接依赖底层几何类型。

**计划**

1. 复核 `Item`、`Package`、`PackageAttribute`、`ItemContainer`、`Bin`、`Layer`、`Block`、`PlacementFactory` 的公开属性、typealias 和构造入口。
2. 把 shape-sensitive 行为集中到 `PackageShapeSpec`、`PackingShape3`、`ItemView`、`ItemPlacement2/3`、shape capability 或新的窄接口。
3. 检查 `ItemMerger`、`LoadingOrderCalculator`、`DemandStatistics`、`MaterialDemandReducedCost`、`Pattern` 是否仍存在隐式长方体假设。
4. 对无法迁移的兼容入口补中英双语 KDoc，说明保留原因、调用边界和迁移条件。
5. 补齐 cuboid、竖直圆柱、X/Z 横向圆柱终态对象的 domain tests。

**修改清单**

1. `bpp3d-domain-item-context/src/main/.../model/Item.kt`
2. `bpp3d-domain-item-context/src/main/.../model/Package.kt`
3. `bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`
4. `bpp3d-domain-item-context/src/main/.../model/ItemContainer.kt`
5. `bpp3d-domain-item-context/src/main/.../model/Bin.kt`
6. `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
7. `bpp3d-domain-item-context/src/main/.../model/Block.kt`
8. `bpp3d-domain-item-context/src/main/.../model/PlacementFactory.kt`
9. `bpp3d-domain-item-context/src/main/.../service/*`
10. `bpp3d-domain-item-context/src/test/**/*`

**验收标准**

1. 新增业务代码不直接暴露底层 cuboid/projection 作为 shape 语义。
2. shape-sensitive 行为均有 cuboid、竖直圆柱和横向圆柱终态测试覆盖。
3. item-context focused tests 通过。
4. 旧长方体构造、旧测试夹具和旧业务链路继续兼容。

### 4.3 横向圆柱候选生成开放评估与收口

**事项**

对 layer generation、circle packing 和 BLA 做横向圆柱候选开放评估。能证明的最小子路径可以开放；不能证明的路径必须保持 unsupported，并通过测试和脚本防止误开放。

**计划**

1. 拆分 shape-neutral 逻辑、Y 轴 circle packing 假设、cuboid-only 假设和 BLA 依赖假设。
2. 为 X/Z 横向圆柱建立 footprint、可放置平面、半径/直径、轴向长度、边界、同层轴向、支撑前置条件和候选去重决策表。
3. 评估是否只开放外部已知坐标层输入绕过自动候选生成，还是开放受限自动候选子路径。
4. 若开放横向圆柱自动候选，必须实现真实 footprint packing、候选去重、同层轴向限制、支撑前置条件和 negative tests。
5. 若不开放横向圆柱自动候选，统一拒绝入口、错误信息、README 支持矩阵和 tests。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-layer-generation-context/src/main/**/*`
3. `bpp3d-domain-layer-generation-context/src/test/**/*`
4. `bpp3d-domain-bla-context/src/main/**/*`
5. `bpp3d-domain-bla-context/src/test/**/*`
6. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
7. `bpp3d-application/src/test/**/*`
8. `README.md`
9. `README_ch.md`

**验收标准**

1. 每个候选生成入口都有明确支持矩阵。
2. Y 轴 circle packing 假设不会被 X/Z 横向圆柱误复用。
3. 已开放路径有候选、边界、碰撞、支撑和同层轴向测试。
4. 未开放路径有统一 unsupported message、negative tests 和脚本保护。

### 4.4 Block Loading、DFS/MLHS、Stacking 与 Hanging 决策闭环

**事项**

对 block loading、DFS/MLHS、stacking 和 hanging 做完整收口：能实现的最小 shape-aware 子路径实现，不能证明的路径保持明确 unsupported，并消除重复 guard。

**计划**

1. 复核 simple block、complex block、pattern placement、DFS、MLHS、stacking、hanging 对空间拆分、支撑面、投影和层策略的几何假设。
2. 评估竖直圆柱是否能进入更宽的 block loading 或 stacking/hanging 子路径。
3. 评估 X/Z 横向圆柱是否仅允许已知坐标终态，还是可以进入受限 block loading 子路径。
4. 将不能证明的路径统一复用 item-domain 契约，清理模块内重复 message。
5. 补齐 positive/negative tests，覆盖 cuboid、竖直圆柱、横向圆柱、混装和错误信息。

**修改清单**

1. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
2. `bpp3d-domain-block-loading-context/src/main/.../ComplexBlockGenerator.kt`
3. `bpp3d-domain-block-loading-context/src/main/.../CylinderUnsupportedGuard.kt`
4. `bpp3d-domain-block-loading-context/src/main/.../DepthFirstSearchAlgorithm.kt`
5. `bpp3d-domain-block-loading-context/src/main/.../MultiLayerHeuristicSearchAlgorithm.kt`
6. `bpp3d-domain-block-loading-context/src/test/**/*`
7. `bpp3d-domain-item-context/src/main/.../model/CylinderShapeContract.kt`
8. `bpp3d-domain-item-context/src/test/**/*`

**验收标准**

1. block loading、DFS/MLHS、stacking、hanging 的支持矩阵明确。
2. 任何新增开放路径都有真实几何、支撑和搜索正确性测试。
3. 任何保留 unsupported 的路径都有统一错误信息和 negative tests。
4. block-loading focused tests 与 BPP3D reactor 通过。

### 4.5 Packing Final Guard、Depth Boundary 与 Known-Coordinate 强化

**事项**

继续强化已知坐标终态路径，把横向圆柱、混装、multi-bin、depth boundary、支撑和 renderer 输出作为完整生产验收路径，而不是测试绕过路径。

**计划**

1. 统一 `axis`、orientation、footprint、height/depth/width、bounding box、material identity、layer identity 和 loading order 的解释。
2. 强化 packing final guard 对 X/Z 横向圆柱的 boundary、collision、support、axis mixing、same-layer policy、multi-bin 和 depth boundary 场景校验。
3. 补齐贴地支撑、全长支撑、局部支撑拒绝、跨层 depth boundary、跨 bin mixed shape 和 renderer 输出测试。
4. 保持默认自动候选生成关闭，直到 4.3 和 4.4 的证明同时完成。
5. 同步 README、README_ch 和 fixture 文档中的 allowed path / unsupported path 表述。

**修改清单**

1. `bpp3d-application/src/main/.../service/ColumnGenerationPackingAnalyzer.kt`
2. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
3. `bpp3d-application/src/main/.../service/DepthBoundaryLayerOrientationPolicy.kt`
4. `bpp3d-domain-packing-context/src/main/.../service/Packer.kt`
5. `bpp3d-domain-packing-context/src/main/.../service/MaterialPacker.kt`
6. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
7. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
8. `bpp3d-application/src/test/**/*`
9. `bpp3d-domain-packing-context/src/test/**/*`
10. `bpp3d-infrastructure/src/test/resources/renderer/*`

**验收标准**

1. 已知坐标 X/Z 横向圆柱使用真实几何判定，不以外接盒作为最终可行性证明。
2. explicit bins、generic known-coordinate layers、multi-bin、mixed shape、depth boundary 均有 positive/negative tests。
3. unsupported 与 allowed path 错误信息可断言且不互相冲突。
4. application focused tests、packing focused tests 和 renderer DTO tests 通过。

### 4.6 Layer Assignment、CSV/Gurobi 与 Shape Metadata 收敛

**事项**

统一 layer assignment、CSV/Gurobi、dynamic radius/diameter、axis metadata 和 depth boundary policy 的 shape-aware 解释，并评估 depth boundary 是否可以下沉到 MILP 原生约束。

**计划**

1. 复核 `DemandConstraint`、`VolumeMinimization`、`LayerAggregation`、`MaterialPacker`、Gurobi adapter 对 shape identity、axis、volume 和 layer key 的解释。
2. 确认 radius/diameter、dynamic radius/diameter、axis、depth boundary policy 在 request、CSV、Gurobi dataset suite 中共用同一解析契约。
3. 设计 depth boundary MILP 原生约束方案，覆盖 first/last layer、multi-bin、mixed shape、横向圆柱和旧数据兼容。
4. 如果方案成立，实现建模和 Gurobi 回归；如果方案不成立，保留 application 后验硬校验并记录不可下沉原因。
5. 扩展 CSV dataset suite，覆盖旧格式、混装、竖直圆柱、横向圆柱 metadata、depth boundary 和非法字段。

**修改清单**

1. `bpp3d-domain-layer-assignment-context/src/main/.../model/LayerAggregation.kt`
2. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/DemandConstraint.kt`
3. `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/VolumeMinimization.kt`
4. `bpp3d-domain-layer-assignment-context/src/test/**/*`
5. `bpp3d-application/src/main/.../service/ColumnGenerationApplicationService.kt`
6. `bpp3d-application/src/main/.../service/ColumnGenerationAlgorithm.kt`
7. `bpp3d-application/src/main/.../service/DepthBoundaryLayerOrientationPolicy.kt`
8. `bpp3d-application/src/gurobi-test/.../GurobiColumnGenerationTest.kt`
9. `bpp3d-application/src/test/resources/gurobi/*`

**验收标准**

1. layer assignment、application、CSV 和 Gurobi 对 shape metadata 的解释一致。
2. depth boundary 原生下沉若实现，必须有 Gurobi 普通回归和 CSV dataset suite 覆盖。
3. depth boundary 若不下沉，文档、错误信息和测试明确说明仍是后验硬校验。
4. 旧 CSV、旧 DTO 和旧长方体 Gurobi 链路继续兼容。

### 4.7 Renderer、DTO、Fixture 与人工视觉确认

**事项**

如果下一轮触发 shape metadata、axis、renderer adapter、fixture 或终态几何显示语义变化，同步仓内 DTO/fixture 与外部 renderer，并执行自动与人工可视化验收。

**计划**

1. 对齐仓内 renderer DTO 与外部 Rust/TS DTO 的 `algorithmShapeType`、`renderShapeType`、`axis`、`bounding*` 和 shape metadata 字段。
2. 补齐混装、X/Y/Z 三轴、横向圆柱贴地、depth boundary、multi-bin、非法支撑和实际求解输出 fixture。
3. 更新 `RendererDTOTest`、外部 renderer DTO 反序列化测试、README 和人工确认样例说明。
4. 执行外部 renderer 自动检查和人工视觉确认；人工确认必须区分通过、失败、未执行和未触发。

**修改清单**

1. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
2. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
3. `bpp3d-infrastructure/src/test/resources/renderer/*`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
5. `README.md`
6. `README_ch.md`
7. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 仓内 renderer DTO 契约测试通过。
2. 外部 renderer `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test` 通过。
3. 人工视觉确认覆盖混装、X/Y/Z 三轴、横向圆柱贴地、外接盒语义、depth boundary 和实际求解输出。
4. 未实际执行的人工项不得写成通过。

### 4.8 文档、验证与提交隔离

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

## 5. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 6. 触发式完整验收

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

## 7. 下一轮完成定义

1. 第 4 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 新增开放能力具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的能力具备统一错误信息、negative tests 和门禁保护。
4. BPP3D 必跑门禁全部通过。
5. 被实际改动触发的完整验收全部执行并记录。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
