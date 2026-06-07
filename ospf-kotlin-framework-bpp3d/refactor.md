# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-07

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与终局闭环执行结论。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. 已完成竖直圆柱 MVP、横向圆柱已知坐标终态路径、真实几何校验和 renderer metadata 基础闭环。
2. 已完成 layer generation、circle packing、pile、block loading、DFS/MLHS、pattern 和 item-merger 的圆柱能力边界收口。
3. 已完成圆柱候选路径、cuboid-only 路径、支撑路径、known-coordinate final 路径、renderer final 路径和 depth boundary final 路径的共享能力契约收口。
4. 已完成 final packing geometry、圆柱 unsupported contract、known-coordinate 边界和业务层泛型泄漏的主要脚本门禁。
5. 已完成 CSV/Gurobi shape metadata、dynamic radius/diameter、depth boundary policy 和 dataset suite 的基础契约收口。
6. 已完成 README、README_ch、focused tests、BPP3D 必跑门禁和触发式 Gurobi 验收的同步验证。
7. 外部 renderer 本轮未触发 DTO、fixture、adapter 或显示语义变化，因此未执行外部验收。
8. 已完成 depth boundary MILP 终局决策：不下沉为 MILP 原生约束，继续作为 application 层最终后验硬校验，并补充 final MILP selected bins 的入口回归。

## 2. 总目标与当前能力边界

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 当前保留边界

1. BPP3D 尚未完全泛型化；底层 placement/projection 体系允许保留必要 cuboid 结构性绑定，但业务层不能继续扩散新的 cuboid-only API。
2. X/Z 横向圆柱当前只允许在已知坐标终态 packing/rendering 路径表达和校验。
3. 默认自动候选生成、layer generation、circle packing、BLA、block loading、DFS/MLHS、stacking、hanging 尚未开放 X/Z 横向圆柱。
4. X/Z 横向圆柱不得复用 Y 轴 circle packing 平面假设作为候选生成或最终可行性证明。
5. known-coordinate placement 是 final validation path，不是 generated candidate path；新增调用必须通过脚本白名单审计。
6. depth boundary 是 application 层后验硬校验；目前覆盖最终 MILP selected bins 和泛型 known-coordinate final bins，不下沉为 MILP 原生约束。
7. Gurobi CSV dataset 只接受 grouped-layer 与 material-width-amount 两类显式 schema；重复列和未知列会被拒绝。
8. 连续半径优化不进入默认生产链路，除非先完成数据契约、建模契约和 Gurobi 回归。
9. renderer 源码在外部工程，BPP3D 仓内只维护 DTO、fixture、adapter 和文档契约。

## 3. 终局执行结论

本轮按“终局闭环轮”执行，覆盖第 4 节事项并形成以下结论。

1. depth boundary 明确不下沉为 MILP 原生约束，原因是 first/last depth layer 由最终选中列和堆叠顺序共同决定，MILP 原生化需要把每个箱子的层序、首尾位置和 mixed shape 轴向/朝向选择全部提升为原生变量，当前会显著扩大模型并与横向圆柱 known-coordinate final path 的边界不一致。
2. depth boundary 保留为 application 层最终硬校验，覆盖 final MILP selected bins 和 generic known-coordinate final bins；它不是候选生成过滤器，也不是 RMP 或部分 MILP 约束。
3. CSV/Gurobi、layer assignment、application request、axis metadata、dynamic radius/diameter、volume 和 layer key 的生产口径保持一致；连续半径优化继续保持非生产能力，`radiusWeightFunctionKey` 仅作为 metadata 保留，不触发默认生产链路的连续候选或隐式优化。
4. Known-coordinate final path 已覆盖 mixed shape、multi-bin、depth boundary、横向圆柱支撑、axis mixing、same-layer policy 和旧数据兼容边界。
5. 本轮没有修改 renderer DTO、fixture、packing renderer adapter 或显示语义，因此未触发外部 renderer build/typecheck/Rust/视觉验收。
6. 四个边界/几何脚本用于防止新增 shape-sensitive 行为绕过共享 contract 或 allowlist。

## 4. 事项、修改清单与验收标准

### 4.1 Depth Boundary 与 MILP 终局决策

**事项**

1. 已审计 `DepthBoundaryLayerOrientationPolicy`、`ColumnGenerationApplicationService`、`ColumnGenerationPackingAnalyzer`、`LayerAggregation`、`DemandConstraint`、`VolumeMinimization` 和 Gurobi adapter。
2. 已判断 depth boundary 不适合下沉为 MILP 原生约束；first/last layer、multi-bin、mixed shape、横向圆柱和旧数据兼容由后验硬校验闭环。
3. 已固化不下沉结论、后验硬校验原因、错误信息、门禁和测试。
4. 防止 depth boundary 被误当作候选生成过滤器或部分求解约束。

**修改清单**

1. `bpp3d-application/src/main/**/*`
2. `bpp3d-domain-layer-assignment-context/src/main/**/*`
3. `bpp3d-application/src/test/**/*`
4. `bpp3d-application/src/gurobi-test/**/*`
5. `bpp3d-application/src/test/resources/gurobi/*`
6. `README.md`
7. `README_ch.md`
8. `refactor.md`

**验收标准**

1. depth boundary 不下沉的结论明确、可测试、可文档化。
2. 后验硬校验覆盖 final MILP selected bins 和 generic known-coordinate bins。
3. 文档明确 depth boundary 不是候选生成过滤器，也不是 MILP 原生约束。
4. request 级 policy 与 executor config policy 冲突会被显式拒绝。

### 4.2 CSV/Gurobi 与 Layer Assignment 生产闭环

**事项**

1. 统一 grouped-layer、material-width-amount、旧格式、混装、竖直圆柱、横向圆柱 metadata、dynamic radius/diameter 和 shape policy 的解释。
2. 扩展 CSV dataset suite，覆盖非法 axis、非法 orientation、非法 policy、缺失字段、重复字段、未知字段和 schema 混用。
3. 对连续半径优化做最终生产决策；不能完整闭环则继续保持非生产能力，并已补 `PackageShapeSpecTest.verticalCylinderRadiusWeightFunctionKeyShouldRemainMetadataOnly` 防误用测试。
4. 对 layer assignment 的 material identity、layer identity、volume、actualVolume 和 shape metadata 做一致性回归。

**修改清单**

1. `bpp3d-application/src/main/**/*`
2. `bpp3d-domain-layer-assignment-context/src/main/**/*`
3. `bpp3d-domain-item-context/src/main/**/*`
4. `bpp3d-domain-packing-context/src/main/**/*`
5. `bpp3d-application/src/gurobi-test/**/*`
6. `bpp3d-application/src/test/resources/gurobi/*`
7. `bpp3d-domain-layer-assignment-context/src/test/**/*`
8. `README.md`
9. `README_ch.md`

**验收标准**

1. layer assignment、application、CSV 和 Gurobi 对 shape metadata 的解释一致。
2. CSV dataset suite 覆盖旧格式、混装、动态半径/直径、depth boundary 和非法字段。
3. 未开放的连续半径优化具备明确文档和防误用测试。
4. Gurobi 普通回归和 CSV dataset suite 通过。

### 4.3 Known-Coordinate Final Path 与 Renderer 最终验收

**事项**

1. 扩展 known-coordinate final path 验收，覆盖贴地支撑、全长支撑、局部支撑拒绝、跨层、multi-bin、mixed shape、axis mixing、same-layer policy、depth boundary 和旧数据兼容。
2. 审计 renderer DTO、fixture、packing renderer adapter、README 样例和外部 renderer 字段语义。
3. 若触发 renderer 语义变化，同步外部 Rust/TS renderer，并执行自动与人工视觉验收。
4. 对未触发外部 renderer 的情况记录原因，不把历史结果写成本轮通过。

**修改清单**

1. `bpp3d-domain-packing-context/src/main/**/*`
2. `bpp3d-domain-packing-context/src/test/**/*`
3. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
4. `bpp3d-infrastructure/src/test/**/*`
5. `bpp3d-infrastructure/src/test/resources/renderer/*`
6. `README.md`
7. `README_ch.md`
8. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`

**验收标准**

1. 仓内 renderer DTO 契约测试通过。
2. final packing geometry guard 覆盖 packing 和 renderer 两条入口。
3. 触发外部 renderer 时完成 `npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test` 和人工视觉确认。
4. BPP3D 提交不包含非 BPP3D 或外部 renderer 改动。

### 4.4 脚本门禁与文档终局收口

**事项**

1. 审计 `generic-boundary-check.ps1`、`shape-boundary-check.ps1`、`geometry-boundary-check.ps1` 和 `geometry-module-dry-run.ps1` 的 allowlist。
2. 保留项必须有归属、用途、迁移条件和阻断原因；stale allowlist 必须失败。
3. 新增 shape-sensitive 行为必须进入共享 contract 或明确 allowlist。
4. README、README_ch、refactor.md 与代码能力口径保持一致。

**修改清单**

1. `scripts/*.ps1`
2. `README.md`
3. `README_ch.md`
4. `refactor.md`
5. 受脚本门禁影响的 `bpp3d-*/src/main/**/*`
6. 受脚本门禁影响的 `bpp3d-*/src/test/**/*`

**验收标准**

1. 四个边界/几何脚本全部通过。
2. `git diff --check -- ospf-kotlin-framework-bpp3d` 通过。
3. BPP3D 全量 reactor 通过。
4. 被实际改动触发的 Gurobi 和 renderer 验收全部执行并记录。
5. 本轮 BPP3D 改动独立提交。

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

## 7. 完成定义

1. 第 4 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 新增开放能力具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的能力具备统一错误信息、negative tests 和门禁保护。
4. BPP3D 必跑门禁全部通过。
5. 被实际改动触发的完整验收全部执行并记录。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
