# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-06

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与下一轮执行计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. 已完成竖直圆柱 MVP、横向圆柱已知坐标终态路径、真实几何校验和 renderer metadata 基础闭环。
2. 已完成 layer generation、circle packing、pile、block loading 和 search 相关路径的圆柱能力边界收口。
3. 已完成 final packing geometry、圆柱 unsupported contract、known-coordinate 边界和业务层泛型泄漏的主要脚本门禁。
4. 已完成 CSV/Gurobi shape metadata、dynamic radius/diameter、depth boundary policy 和 dataset suite 的基础契约收口。
5. 已完成 README、README_ch、refactor.md、focused tests、BPP3D 全量 reactor、Gurobi 回归和 CSV dataset suite 的同步验证。
6. 外部 renderer 本轮未触发 DTO、fixture、adapter 或显示语义变化，因此未执行外部验收。

## 2. 已完成验证摘要

1. BPP3D 必跑门禁已通过。
2. BPP3D 全量 reactor 已通过。
3. Gurobi 插件 install 已通过。
4. Gurobi 普通回归已通过。
5. Gurobi CSV dataset suite 已通过。
6. 外部 renderer 验收未触发。

## 3. 总目标与当前能力边界

### 3.1 总目标

最终目标是在一次或尽可能少的后续迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 3.2 当前保留边界

1. BPP3D 尚未完全泛型化；底层 placement/projection 体系允许保留必要 cuboid 结构性绑定，但业务层不能继续扩散新的 cuboid-only API。
2. X/Z 横向圆柱当前只允许在已知坐标终态 packing/rendering 路径表达和校验。
3. 默认自动候选生成、layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 尚未开放 X/Z 横向圆柱。
4. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
5. known-coordinate placement 是 final validation path，不是 generated candidate path；新增调用必须通过脚本白名单审计。
6. depth boundary 是 application 层后验硬校验；目前覆盖最终 MILP selected bins 和泛型 known-coordinate final bins，尚未下沉为 MILP 原生约束。
7. Gurobi CSV dataset 只接受 grouped-layer 与 material-width-amount 两类显式 schema；重复列和未知列会被拒绝。
8. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。
9. renderer 源码在外部工程，BPP3D 仓内只维护 DTO、fixture、adapter 和文档契约。

## 4. 下一轮目标

下一轮按“最终收口轮”执行，默认一次性覆盖第 5 节全部事项。只有真实技术阻断、外部 renderer 环境阻断或用户明确收窄范围时，才允许把事项拆到后续轮次。

1. 完成 BPP3D shape contract 的最终横向审计和收口，确保 item-domain、shape-domain、packing-domain、layer-generation、BLA、block-loading、layer-assignment、application、CSV/Gurobi 和 renderer adapter 的能力口径一致。
2. 完成所有候选生成和搜索路径的圆柱支持矩阵总决策，明确每条路径是支持、仅终态校验、明确拒绝，还是 cuboid-only。
3. 完成 known-coordinate final path 的生产级验收扩展，覆盖支撑、碰撞、multi-bin、mixed shape、axis policy、depth boundary、renderer 输出和旧数据兼容。
4. 完成 depth boundary MILP 原生下沉评估；可行则实现，不可行则固化后验硬校验原因、门禁和回归。
5. 完成 CSV/Gurobi、dynamic radius/diameter、axis metadata、volume、layer key、request DTO 和 layer assignment 的 shape-aware 解释统一。
6. 若触发 renderer DTO、fixture、adapter 或显示语义变化，同步仓内和外部 renderer，并完成自动与人工视觉验收。
7. 下一轮结束时形成清晰提交边界：BPP3D、本仓其他模块和外部 renderer 分开提交，不混入无关改动。

## 5. 下一轮事项、计划、修改清单与验收标准

### 5.1 全链路 Shape Contract 最终审计与收口

**事项**

1. 审计 `shape`、`axis`、`cuboid`、`projection`、`placement`、`known-coordinate`、`renderer DTO`、`CSV metadata`、`unsupported` 的全部命中。
2. 清理 stale allowlist，保留项必须写明归属、用途、迁移条件和阻断原因。
3. 把 shape-sensitive 行为收敛到 `PackageShapeSpec`、`PackingShape3`、`ItemView`、`ItemPlacement2/3`、domain alias、shape capability 或共享 contract。
4. 统一尺寸、体积、footprint、top/bottom、support surface、stacking、hanging、loading order、material identity、layer identity 和 renderer metadata 的解释来源。
5. 清理重复 unsupported 文案、重复 guard 和重复 axis/orientation 解释。
6. 补齐 cuboid、竖直圆柱、横向圆柱在终态对象、候选对象、支撑对象和渲染对象上的 positive/negative tests。

**计划**

1. 先跑全量 `rg` 审计并按保留、迁移、拒绝、测试专用分类。
2. 优先改业务层公开 API 和跨模块调用点，底层 infrastructure 只保留必要结构性绑定。
3. 将仍需保留的 cuboid/projection/placement 绑定纳入脚本 allowlist，并要求脚本输出迁移建议。
4. 对 item-domain、packing-domain、layer-assignment 和 application 分别补 focused tests。
5. 更新 README、README_ch 和 refactor.md，使文档能力矩阵与代码一致。

**修改清单**

1. `scripts/generic-boundary-check.ps1`
2. `scripts/shape-boundary-check.ps1`
3. `scripts/geometry-boundary-check.ps1`
4. `scripts/geometry-module-dry-run.ps1`
5. `bpp3d-domain-item-context/src/main/**/*`
6. `bpp3d-domain-packing-context/src/main/**/*`
7. `bpp3d-domain-layer-assignment-context/src/main/**/*`
8. `bpp3d-domain-layer-generation-context/src/main/**/*`
9. `bpp3d-domain-bla-context/src/main/**/*`
10. `bpp3d-domain-block-loading-context/src/main/**/*`
11. `bpp3d-application/src/main/**/*`
12. `bpp3d-infrastructure/src/main/**/*`
13. `bpp3d-*/src/test/**/*`
14. `README.md`
15. `README_ch.md`
16. `refactor.md`

**验收标准**

1. 新增业务代码不直接暴露底层 cuboid/projection 作为 shape 语义。
2. 保留的 cuboid/projection/placement 绑定均有 allowlist 分类和迁移条件。
3. unsupported message 由共享契约统一输出。
4. shape-sensitive 行为由明确 API 或共享 contract 表达。
5. 四个边界/几何脚本全部通过。
6. item、packing、layer-assignment、application focused tests 通过。

### 5.2 候选生成、搜索路径与 Final Guard 总决策

**事项**

1. 对 layer generation、circle packing、BLA、simple block、complex block、DFS、MLHS、stacking、hanging、pattern、pile、item-merger 和 final guard 做总决策。
2. 明确 X/Y/Z 圆柱在每条路径上的支持矩阵、拒绝入口、错误信息、测试和脚本门禁。
3. 保持 X/Z 横向圆柱自动候选生成关闭，除非同轮完成真实 footprint packing、支撑、去重、搜索、renderer 和 Gurobi 闭环。
4. 扩大 known-coordinate final path 验收，覆盖贴地支撑、全长支撑、局部支撑拒绝、跨层、multi-bin、mixed shape、axis mixing、same-layer policy 和 renderer 输出。
5. 清理 BLA、block loading、DFS/MLHS、stacking、hanging、pattern、pile 和 item-merger 的重复 guard。

**计划**

1. 建立候选路径和 final path 的支持矩阵表，并同步到 README。
2. 拆分 shape-neutral 逻辑、Y 轴 circle packing 假设、cuboid-only 假设、block 空间拆分假设和 BLA 依赖假设。
3. 对未开放路径统一调用 shared unsupported contract。
4. 对已开放路径补齐真实几何、边界、碰撞、支撑、同层轴向、去重和旧长方体回归。
5. 用脚本保护 known-coordinate final path 与 generated candidate path 的调用边界。

**修改清单**

1. `bpp3d-domain-layer-generation-context/src/main/**/*`
2. `bpp3d-domain-bla-context/src/main/**/*`
3. `bpp3d-domain-block-loading-context/src/main/**/*`
4. `bpp3d-domain-packing-context/src/main/**/*`
5. `bpp3d-domain-item-context/src/main/**/*`
6. `bpp3d-application/src/main/**/*`
7. `bpp3d-domain-layer-generation-context/src/test/**/*`
8. `bpp3d-domain-bla-context/src/test/**/*`
9. `bpp3d-domain-block-loading-context/src/test/**/*`
10. `bpp3d-domain-packing-context/src/test/**/*`
11. `bpp3d-application/src/test/**/*`
12. `README.md`
13. `README_ch.md`

**验收标准**

1. 每个候选生成、BLA、block loading、search 和 final guard 入口都有明确支持矩阵。
2. X/Z 横向圆柱不会进入未闭环的自动候选路径。
3. known-coordinate final path 与 generated candidate path 可被测试和脚本区分。
4. 新开放路径具备完整 positive/negative tests。
5. 未开放路径具备统一 unsupported message、negative tests 和脚本保护。
6. layer-generation、BLA、block-loading、packing、application focused tests 通过。

### 5.3 Depth Boundary、CSV/Gurobi 与 Layer Assignment 终局收口

**事项**

1. 评估并尽量实现 depth boundary MILP 原生下沉。
2. 若不下沉，固化不可下沉原因、application 后验硬校验语义、错误信息、门禁和回归。
3. 统一 layer assignment、application request、CSV parser、Gurobi dataset suite、dynamic radius/diameter、axis metadata、volume 和 layer key 的 shape-aware 解释。
4. 扩展 CSV dataset suite，覆盖旧格式、混装、竖直圆柱、横向圆柱 metadata、dynamic radius/diameter、depth boundary、非法 axis、非法 orientation、非法 policy、缺失字段、重复字段和未知字段。
5. 对连续半径优化做最终生产决策；不能完整闭环则继续保持非生产能力。

**计划**

1. 阅读 `DemandConstraint`、`VolumeMinimization`、`LayerAggregation`、`ColumnGenerationAlgorithm`、`ColumnGenerationApplicationService`、`MaterialPacker` 和 Gurobi adapter。
2. 设计 depth boundary MILP 约束方案，覆盖 first/last layer、multi-bin、mixed shape、横向圆柱和旧数据兼容。
3. 若方案成立，实现建模、回归和 dataset suite；若方案不成立，记录原因并增强后验校验门禁。
4. 扩展 CSV 样例和 focused tests，确保 schema、metadata、policy 和旧格式兼容一致。
5. 同步 README、README_ch 和 refactor.md。

**修改清单**

1. `bpp3d-application/src/main/**/*`
2. `bpp3d-domain-layer-assignment-context/src/main/**/*`
3. `bpp3d-domain-packing-context/src/main/**/*`
4. `bpp3d-domain-item-context/src/main/**/*`
5. `bpp3d-application/src/gurobi-test/**/*`
6. `bpp3d-application/src/test/resources/gurobi/*`
7. `bpp3d-application/src/test/**/*`
8. `bpp3d-domain-layer-assignment-context/src/test/**/*`
9. `README.md`
10. `README_ch.md`
11. `refactor.md`

**验收标准**

1. depth boundary 下沉或不下沉的结论明确、可测试、可文档化。
2. 若下沉到 MILP，普通 Gurobi 回归和 CSV dataset suite 覆盖成功。
3. 若不下沉，后验硬校验覆盖 final MILP selected bins 和 generic known-coordinate bins。
4. layer assignment、application、CSV 和 Gurobi 对 shape metadata 的解释一致。
5. CSV dataset suite 覆盖旧格式、混装、动态半径/直径、depth boundary 和非法字段。
6. 未开放的连续半径优化具备明确文档和防误用测试。

### 5.4 Renderer、文档、外部验收与提交隔离

**事项**

1. 若触发 renderer 语义变化，同步仓内 DTO/fixture、packing renderer adapter、README 样例和外部 Rust/TS renderer。
2. 补齐 mixed shape、X/Y/Z 三轴、横向圆柱贴地、全长支撑、非法支撑、depth boundary、multi-bin、actualVolume、bounding* 和实际求解输出 fixture。
3. 执行外部 renderer 自动检查和人工视觉确认；未触发时明确记录原因。
4. 下一轮完成时同步 README、README_ch、refactor.md，记录真实完成范围、阻断项、未触发项和验证结果。
5. BPP3D、本仓其他模块和外部 renderer 分开提交。

**计划**

1. 对齐仓内 renderer DTO 与外部 renderer DTO 的字段和语义。
2. 需要时更新外部 renderer DTO 反序列化、构建、类型检查、Rust 检查和视觉 fixture。
3. 所有验收结果只记录真实执行结果，不把历史结果写成本轮通过。
4. 提交前只 stage BPP3D 相关文件；外部 renderer 与非 BPP3D 改动单独处理。

**修改清单**

1. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
2. `bpp3d-infrastructure/src/test/.../RendererDTOTest.kt`
3. `bpp3d-infrastructure/src/test/resources/renderer/*`
4. `bpp3d-domain-packing-context/src/main/.../service/PackingRendererAdapter.kt`
5. `bpp3d-domain-packing-context/src/test/**/*`
6. `README.md`
7. `README_ch.md`
8. `refactor.md`
9. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 仓内 renderer DTO 契约测试通过。
2. 外部 renderer 在触发时完成 `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test`。
3. 人工视觉确认在触发时覆盖关键混装和横向圆柱场景。
4. 文档区分通过、失败、跳过、未触发和环境阻断。
5. BPP3D 提交不包含非 BPP3D 改动。

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
