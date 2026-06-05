# BPP3D 形状泛型化与圆柱支持重构交接

日期：2026-05-31
最近更新：2026-06-05

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的交接状态。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。当前已完成主链 shape-aware 收敛，但尚未完成 BPP3D 完全泛型化。

## 1. 已完成事项摘要

1. BPP3D 主链已支持长方体、竖直圆柱，以及已知坐标终态路径下的 X/Z 横向圆柱表达。
2. packing 终态碰撞、边界、支撑和 renderer 输出已收敛到真实几何口径。
3. 旧长方体路径、旧 DTO、旧 CSV、application 普通链路、Gurobi 普通回归和 CSV dataset suite 已保持兼容。
4. 业务层主要 shape-aware 入口已收敛到 `PackingShape3`、`PackageShapeSpec`、`ItemView`、domain placement alias 和 typed factory。
5. 必要的 cuboid/placement/projection 结构性绑定已分类固化到边界脚本基线。
6. 圆柱轴向、朝向、top-layer policy、stacking/hanging 和 cuboid-only 路径的 unsupported contract 已统一到 item-domain。
7. renderer DTO、fixture、文档、自动检查、DTO 反序列化测试和人工视觉确认已完成阶段性闭环。
8. 最近一轮 BPP3D 收口提交号为 `4abc8cb4`。

## 2. 当前保留边界

1. BPP3D 完全泛型化尚未完成；底层 placement/projection 体系仍可保留必要 cuboid 结构性绑定。
2. 默认生产链路仍只生成长方体和竖直圆柱候选；X/Z 横向圆柱目前仅允许在已知坐标终态 packing/rendering 路径表达和校验。
3. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
4. DFS/MLHS space-splitting、block loading、stacking、hanging 缺少完整真实几何与支撑证明前继续显式 unsupported。
5. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。
6. depth boundary 仍为 application 后验硬校验，未下沉为默认 MILP 原生约束。
7. 新增代码后必须重新执行相关验证；未执行、失败、跳过、历史复核和人工确认必须分开记录。

## 3. 本轮执行结果

本轮目标没有改变总方向：继续把 BPP3D 从 cuboid-only 业务模型推进到 shape-aware / generic shape 模型。本轮实际完成范围集中在“横向圆柱已知坐标路径生产化”的 application/generic 输入侧，没有开放默认候选生成、layer generation、block loading、stacking、hanging 或 depth boundary MILP 原生能力。

### 3.1 已完成

1. `ColumnGenerationPackingAnalyzer.analyze(state)` 不再对显式传入的 `state.bins` 做默认候选路径的 `Axis3.Y` 门禁，显式 bins 现在按已知坐标终态输入交给 `Packer` 和 `PackingGeometryGuard` 做真实几何校验。
2. `ColumnGenerationPackingAnalyzer.analyzeFromGeneric(...)` 在未传入 explicit bins 时，会把 generic layers 作为已知坐标输入构造成 `LayerBin`，允许 X/Z 横向圆柱进入 packing/rendering guard。
3. `LayerPlacementAdapter.toLayerPlacement()` 仍保持默认候选路径门禁，只允许竖直圆柱；新增 `toKnownCoordinateLayerPlacement()` 作为已知坐标构造入口，不再复用测试绕过命名。
4. application 测试覆盖了 generic known-coordinate X 轴横向圆柱渲染 DTO 输出，以及同层混合 X/Z 圆柱轴向被拒绝。
5. depth boundary 测试改用已知坐标入口，避免测试继续依赖旧 bypass 命名。
6. README / README_ch 已同步“generic known-coordinate analysis 可接受 X/Z 横向圆柱，但必须坐标已固定并通过真实几何 guard”的能力口径。
7. 为当前 quantities `PhysicalUnit.conversionRule` API 更新了 BPP3D 内部自定义计数单位和测试单位适配，保证 BPP3D reactor 可以在当前工作区依赖下编译测试。

### 3.2 本轮验证

已通过：

1. `pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d`：`STRICT_GENERIC_BOUNDARY_PASS`
2. `pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d`：`SHAPE_BOUNDARY_PASS`
3. `pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .`：`GEOMETRY_BOUNDARY_PASS`
4. `pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .`：`GEOMETRY_MODULE_DRY_RUN_PASS`，warnings=8，internal baseline ok=8
5. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Dtest=ColumnGenerationPackingAnalyzerGenericEntryPointTest,DepthBoundaryLayerOrientationPolicyTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：13 tests passed
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：BPP3D reactor passed
7. `mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true`：Gurobi plugin reactor passed
8. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true`：26 tests，0 failures，0 errors，1 skipped，BPP3D reactor passed
9. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true`：26 tests，0 failures，0 errors，0 skipped，CSV dataset suite passed
10. `git diff --check -- ospf-kotlin-framework-bpp3d`：通过，仅有 CRLF 工作区提示

环境准备：

1. 当前工作区存在 quantities API 并行迁移，本轮验证前已执行 `mvn --% -f ospf-kotlin-quantities/pom.xml install -DskipTests -Dgpg.skip=true` 安装本地 quantities 依赖；该改动不属于 BPP3D 提交范围。

未触发 / 未执行：

1. 本轮未修改 renderer DTO、fixture 或外部 renderer 显示语义，因此未执行外部 renderer `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test` 和人工视觉确认。

### 3.3 仍保留

1. 默认候选生成入口仍通过 `toLayerPlacement()` 拒绝 X/Z 横向圆柱。
2. layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 仍未开放 X/Z 横向圆柱自动能力。
3. depth boundary 仍是 application 后验硬校验，未下沉到 MILP 原生约束。
4. renderer DTO 显示字段未变化，本轮只复用既有 renderer adapter 输出。

## 4. 后续目标

后续目标是在尽可能少的迭代内完成 BPP3D shape-aware 到更完整 generic shape 生产契约的跨层推进。范围可以扩大，但任何新增开放能力必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

1. 将业务层 shape capability 从“可用入口”推进到“跨 domain 稳定契约”，继续压缩 application、domain service 和测试中对底层 cuboid/placement/projection 的直接感知。
2. 系统推进 X/Z 横向圆柱：优先开放已知坐标、外部输入层、终态 packing、renderer、depth boundary 后验校验路径；自动候选生成只在完整证明后开放。
3. 对 layer generation、BLA、block loading、DFS/MLHS、stacking、hanging 做一次完整决策：可证明的子路径实现并验收，不能证明的子路径统一拒绝并补足 negative tests。
4. 评估 depth boundary 从 application 后验校验下沉到 layer assignment / MILP 原生约束；若不下沉，必须形成明确阻断原因和防误用门禁。
5. 将 CSV/request/Gurobi shape metadata、dynamic radius/diameter、axis policy、renderer DTO 和 README 能力口径整理为单一契约。
6. 扩大门禁范围，减少下一轮之后的重复排查成本：边界脚本要能定位新增 cuboid 泄漏、重复 unsupported message、renderer DTO 漂移和 stale allowlist。
7. 完成后形成一个 BPP3D 独立提交；外部 renderer 只提交到外部工程，不混入本仓。

## 5. 后续事项

### 5.1 全量 shape contract 审计与门禁升级

**事项**

建立跨 application、domain、infrastructure、test fixture、renderer 的 shape contract 审计矩阵，并把下一轮新增风险直接纳入脚本门禁。

**计划**

1. 扫描所有 `Cuboid`、`CuboidView`、`AbstractCuboid`、`QuantityPlacement2/3`、`QuantityRectangle2`、`QuantityCuboid3`、`Axis3.Y`、`shape_type`、`renderShapeType` 和圆柱 unsupported 文案。
2. 将命中结果分类为基础设施保留、domain typed factory、已知坐标终态路径、生产候选路径、renderer 契约、测试夹具和过期命中。
3. 升级 `generic-boundary-check.ps1`、`shape-boundary-check.ps1`、`geometry-boundary-check.ps1`、`geometry-module-dry-run.ps1`，让新增违规具备清晰错误原因。
4. 清理 stale allowlist；保留 allowlist 必须有真实命中、归属分类和迁移条件。
5. 将本轮开始前和结束后的门禁结果写回本文档，不沿用历史记录。

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
2. 所有 allowlist 都有分类和真实命中；无 stale allowlist。
3. 新增重复圆柱 unsupported message、renderer DTO 漂移和业务层 cuboid 泄漏能被脚本定位。
4. `git diff --check -- ospf-kotlin-framework-bpp3d` 通过。

### 5.2 item-domain 与业务公开 API 泛型化

**事项**

继续把 item-domain 的尺寸、体积、投影、支撑、top/bottom、stacking、hanging 和 placement 构造语义收敛到 shape-aware API，降低业务代码直接依赖底层几何类型的比例。

**计划**

1. 复核 `Item`、`Package`、`PackageAttribute`、`ItemContainer`、`Bin`、`Layer`、`Block`、`PlacementFactory` 的公开属性、typealias 和构造入口。
2. 把 shape-sensitive 行为集中到 `PackageShapeSpec`、`PackingShape3`、`ItemView`、`ItemPlacement2/3`、shape capability 或新的窄接口。
3. 补齐 cuboid、竖直圆柱、X/Z 横向圆柱在体积、外接盒、投影、支撑、top/bottom、stacking/hanging 上的 domain tests。
4. 检查 `ItemMerger`、`LoadingOrderCalculator`、`DemandStatistics`、`MaterialDemandReducedCost`、`Pattern` 是否仍存在隐式长方体假设。
5. 对不能迁移的兼容入口补中英双语 KDoc，说明保留原因和业务边界。

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
3. `shape-boundary-check.ps1` 和 item-context focused tests 通过。
4. 旧长方体构造和旧测试夹具继续兼容。

### 5.3 横向圆柱已知坐标路径生产化

**事项**

将 X/Z 横向圆柱从“终态可表达”推进到“已知坐标输入可生产验收”的稳定路径，覆盖 application input、layer placement adapter、packing final guard、renderer adapter 和 depth boundary 后验校验。

**计划**

1. 明确已知坐标横向圆柱输入来源：request、CSV fixture、测试夹具、外部 layer input 或 solver 后处理结果。
2. 统一 `axis`、orientation、footprint、height/depth/width、bounding box、material identity、layer identity 的解释。
3. 强化 packing final guard 对 X/Z 横向圆柱的 boundary、collision、support、axis mixing、same-layer policy 和 multi-bin 场景校验。
4. 补齐 mixed shape、X/Y/Z 三轴、贴地支撑、跨层 depth boundary、multi-bin 和 renderer 输出测试。
5. 默认自动候选生成仍保持关闭，除非 5.4 和 5.5 的证明同时完成。

**修改清单**

1. `bpp3d-application/src/main/.../service/LayerPlacementAdapter.kt`
2. `bpp3d-domain-packing-context/src/main/.../service/Packer.kt`
3. `bpp3d-domain-packing-context/src/main/.../service/MaterialPacker.kt`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingGeometryGuard.kt`
5. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
6. `bpp3d-domain-packing-context/src/test/**/*`
7. `bpp3d-application/src/test/**/*`
8. `bpp3d-infrastructure/src/test/resources/renderer/*`

**验收标准**

1. 已知坐标 X/Z 横向圆柱在 packing final guard 中使用真实几何判定。
2. 横向圆柱不再被外接盒误判为最终可行性证明。
3. unsupported 与 allowed path 的错误信息清晰可断言。
4. application focused tests、packing focused tests 和 renderer DTO tests 通过。

### 5.4 layer generation 与 circle packing 候选开放评估

**事项**

对 layer generation 和 circle packing 做横向圆柱候选开放评估。能证明的最小子集可以开放；不能证明的部分必须维持 unsupported，并通过测试防止误开放。

**计划**

1. 拆分 layer generation 中的 shape-neutral 逻辑、Y 轴 circle packing 假设和 cuboid-only 假设。
2. 为 X/Z 横向圆柱建立 footprint、可放置平面、半径/直径、轴向长度、边界和同层轴向规则的决策表。
3. 评估是否允许外部已知坐标层输入绕过自动候选生成，但仍进入统一校验。
4. 若开放横向圆柱自动候选，必须新增真实 footprint packing、候选去重、同层轴向限制、支撑前置条件和 negative tests。
5. 若不开放横向圆柱自动候选，继续通过共享契约拒绝，且错误信息不得散落。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/.../LayerGenerationContext.kt`
2. `bpp3d-domain-layer-generation-context/src/main/**/*`
3. `bpp3d-domain-layer-generation-context/src/test/**/*`
4. `bpp3d-domain-bla-context/src/main/**/*`
5. `bpp3d-domain-bla-context/src/test/**/*`
6. `README.md`
7. `README_ch.md`

**验收标准**

1. 每个候选生成入口都有明确支持矩阵。
2. Y 轴 circle packing 假设不会被 X/Z 横向圆柱误复用。
3. 已开放路径有候选、边界、碰撞和同层轴向测试。
4. 未开放路径有 negative tests 和统一 unsupported message。

### 5.5 block loading、DFS/MLHS、stacking 与 hanging 决策闭环

**事项**

对 block loading、DFS/MLHS、stacking 和 hanging 做一次完整收口：能实现的最小 shape-aware 子路径实现，不能实现的路径保持明确 unsupported，并消除重复 guard。

**计划**

1. 复核 simple block、pattern placement、DFS、MLHS、stacking、hanging 对空间拆分、支撑面、投影和层策略的几何假设。
2. 评估竖直圆柱是否能进入更宽的 block loading 或 stacking/hanging 子路径。
3. 评估 X/Z 横向圆柱是否仅允许已知坐标终态，还是可以进入某个受限 block loading 子路径。
4. 对不能证明的路径统一复用 item-domain 契约，不保留模块内重复 message。
5. 补齐 positive/negative tests，覆盖 cuboid、竖直圆柱、横向圆柱、混装和错误信息。

**修改清单**

1. `bpp3d-domain-block-loading-context/src/main/.../SimpleBlockGenerator.kt`
2. `bpp3d-domain-block-loading-context/src/main/.../CylinderUnsupportedGuard.kt`
3. `bpp3d-domain-block-loading-context/src/main/.../DepthFirstSearchAlgorithm.kt`
4. `bpp3d-domain-block-loading-context/src/main/.../MultiLayerHeuristicSearchAlgorithm.kt`
5. `bpp3d-domain-block-loading-context/src/test/**/*`
6. `bpp3d-domain-item-context/src/main/.../model/CylinderShapeContract.kt`
7. `bpp3d-domain-item-context/src/test/**/*`

**验收标准**

1. block loading、DFS/MLHS、stacking、hanging 的支持矩阵明确。
2. 任何新增开放路径都有真实几何、支撑和搜索正确性测试。
3. 任何保留 unsupported 的路径都有统一错误信息和 negative tests。
4. block-loading focused tests 与 BPP3D reactor 通过。

### 5.6 layer assignment、Gurobi 与 depth boundary 原生下沉评估

**事项**

统一 layer assignment、Gurobi、CSV dataset suite 与 depth boundary policy 的 shape-aware 解释，并评估 depth boundary 是否可以下沉到 MILP 原生约束。

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

### 5.7 renderer、DTO、fixture 与人工视觉确认

**事项**

在下一轮触发任何 shape metadata、axis、renderer adapter 或终态几何语义变化时，同步仓内 DTO/fixture 与外部 renderer，并执行自动与人工可视化验收。

**计划**

1. 对齐仓内 renderer DTO 与外部 Rust/TS DTO 的 `algorithmShapeType`、`renderShapeType`、`axis`、`bounding*` 和 shape metadata 字段。
2. 补齐混装、X/Y/Z 三轴、横向圆柱贴地、depth boundary、multi-bin 和一次实际求解输出 fixture。
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

### 5.8 文档、验证与提交隔离

**事项**

下一轮完成时必须同步 README、README_ch、refactor.md，执行完整门禁，并将 BPP3D、本仓其他模块和外部 renderer 的提交边界分开。

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

## 8. 后续完成定义

1. 第 5 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 新增开放能力具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的能力具备统一错误信息、negative tests 和门禁保护。
4. BPP3D 必跑门禁全部通过。
5. 被实际改动触发的完整验收全部执行并记录。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
