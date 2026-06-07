# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-07

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与下一轮执行计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. 已完成竖直圆柱生产基础能力、横向圆柱已知坐标终态能力、真实几何校验、renderer metadata、depth boundary 后验硬校验和 CSV/Gurobi shape metadata 的基础闭环。
2. 已完成圆柱 unsupported contract、known-coordinate final path、renderer final path、depth boundary final path、业务层泛型泄漏和几何模块边界的主要门禁。
3. 已完成 dynamic radius/diameter 离散候选能力；连续半径优化明确保持非生产能力，并有防误用测试保护。
4. 已完成 README、README_ch、focused tests、BPP3D 必跑门禁和触发式 Gurobi 验收同步验证。
5. 已完成上一轮 BPP3D 改动独立提交，未混入非 BPP3D 或外部 renderer 改动。

## 2. 总目标与下一轮方向

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 下一轮主目标

下一轮选择 **固定半径 X/Z 横向圆柱自动候选生成与 column generation 生产闭环** 作为主目标。

目标是在不开放连续半径优化、不回退既有 cuboid/Y 轴圆柱能力的前提下，把当前仅限 known-coordinate final path 的 X/Z 横向圆柱，推进到 application/CSV 输入、layer generation 候选、layer assignment/Gurobi 求解、final packing geometry、renderer 输出、文档和脚本门禁的生产闭环。

### 2.3 下一轮能力边界

1. 本轮只开放固定半径或离散半径候选的 X/Z 横向圆柱；连续半径优化继续保持非生产能力。
2. X/Z 横向圆柱候选生成必须使用轴向感知 footprint 和最终真实 3D 几何校验，不能复用 Y 轴 circle packing 平面假设作为可行性证明。
3. 自动生成路径应优先覆盖 floor-supported 横向圆柱；若支持叠放，必须通过既有 full-length support guard 或新增共享支撑 contract 证明。
4. generated candidate path 不能绕用 known-coordinate placement 白名单；新增入口必须有独立命名、门禁和测试。
5. DFS/MLHS、block loading、pile、stacking、hanging 若无法在本轮完整闭环，必须继续保持显式 unsupported contract，并补充与新开放路径不冲突的 negative tests。
6. renderer DTO 如无字段语义变化，不触发外部 renderer 改动；若 DTO、fixture、adapter 或显示语义变化，则必须同步外部 renderer 验收。

## 3. 下一轮事项

### 3.1 横向圆柱生产契约

1. 梳理 `CylinderShapeContract`、`PackingGeometryContract`、`LayerPlacementAdapter`、`LayerGenerationContext`、`ColumnGenerationPackingAnalyzer` 和 renderer adapter 的路径边界。
2. 为 X/Z 横向圆柱新增 generated candidate path 契约，区分 generated layer、known-coordinate final、renderer final 和 unsupported search paths。
3. 统一错误信息与 capability path，避免 application、domain、renderer 各自复制横向圆柱 unsupported 文案。
4. 更新脚本门禁，允许新生产路径通过共享 contract 开放，同时继续阻断绕过入口。

### 3.2 Layer Generation 与候选生成

1. 设计并实现 X/Z 横向圆柱固定半径候选生成策略。
2. 候选生成使用轴向感知的保守 footprint；最终 packing/rendering 必须继续由 `PackingGeometryGuard` 做真实圆柱几何校验。
3. 支持同一 layer 内同轴横向圆柱的基础候选；mixed axis 同层仍需明确策略，不能隐式混用。
4. layer key、source、volume、actualVolume、radius、diameter、axis metadata 必须保持可追踪，避免和 cuboid/Y 轴圆柱候选碰撞。
5. 补充 layer generation tests，覆盖 X 轴、Z 轴、动态离散半径、bin 边界、overlap、same-layer axis policy 和 unsupported stacking/hanging。

### 3.3 Application、CSV/Gurobi 与求解闭环

1. CSV grouped-layer 与 material-width-amount schema 中的 `axis = X/Z` 从 metadata-only 推进为可参与自动候选生成的生产输入。
2. application request、executor config、layer assignment、Gurobi adapter 和 dataset suite 对 X/Z 横向圆柱解释一致。
3. final MILP selected bins 必须经过 depth boundary 和 final packing geometry 双重后验校验。
4. Gurobi 普通回归和 CSV dataset suite 覆盖横向圆柱可行样例、非法 axis/orientation/policy、缺失半径、schema 混用、未知列和重复列。
5. 如果求解器原生结构无法完整表达某些横向圆柱属性，必须明确由后验硬校验兜底，并在文档与测试中固化。

### 3.4 Packing、Renderer 与文档

1. final packing geometry guard 覆盖自动生成的 X/Z 横向圆柱 selected bins，不只覆盖手工 known-coordinate bins。
2. renderer schema 输出保持 `shapeType`、`renderShapeType`、`algorithmShapeType`、`axis`、`radius`、`diameter`、`bounding*` 和 `actualVolume` 一致。
3. README、README_ch 更新 X/Z 横向圆柱从 known-coordinate-only 到 generated candidate path 的新能力矩阵。
4. 若 renderer DTO 或显示语义变化，更新仓内 fixture 并执行外部 renderer build/typecheck/Rust/视觉验收。

## 4. 下一轮执行计划

1. **契约审计与门禁调整**：先更新 capability path、unsupported contract 和脚本 allowlist/stale 检查，确保后续代码只能从共享入口开放 X/Z 横向圆柱。
2. **候选生成实现**：实现 axis-aware horizontal cylinder candidate generation，优先覆盖 floor-supported fixed/discrete-radius X/Z 圆柱。
3. **求解链路接入**：串联 application request、CSV parser、layer assignment、Gurobi dataset suite 和 final selected bins 校验。
4. **packing/renderer 回归**：确认自动生成结果进入 final geometry guard 和 renderer schema，不降低 known-coordinate final path 的既有能力。
5. **文档与验收**：更新 README、README_ch、refactor.md，跑必跑门禁和触发式 Gurobi/renderer 验收，最后将 BPP3D 改动独立提交。

## 5. 修改清单

1. `bpp3d-domain-item-context/src/main/**/*`
2. `bpp3d-domain-item-context/src/test/**/*`
3. `bpp3d-domain-layer-generation-context/src/main/**/*`
4. `bpp3d-domain-layer-generation-context/src/test/**/*`
5. `bpp3d-domain-layer-assignment-context/src/main/**/*`
6. `bpp3d-domain-layer-assignment-context/src/test/**/*`
7. `bpp3d-domain-packing-context/src/main/**/*`
8. `bpp3d-domain-packing-context/src/test/**/*`
9. `bpp3d-application/src/main/**/*`
10. `bpp3d-application/src/test/**/*`
11. `bpp3d-application/src/gurobi-test/**/*`
12. `bpp3d-application/src/test/resources/gurobi/*`
13. `bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`
14. `bpp3d-infrastructure/src/test/**/*`
15. `bpp3d-infrastructure/src/test/resources/renderer/*`
16. `scripts/*.ps1`
17. `README.md`
18. `README_ch.md`
19. `refactor.md`
20. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 6. 验收标准

1. X/Z 横向圆柱固定半径或离散半径候选可从 application/CSV 输入进入 layer generation，并可被 column generation/Gurobi 选择。
2. 自动生成的 X/Z 横向圆柱 selected bins 通过 final packing geometry guard、depth boundary final validation 和 renderer schema 输出。
3. X/Z 横向圆柱不再是全局 generated path unsupported，但 DFS/MLHS、block loading、pile、stacking、hanging 中未开放的路径仍有明确 negative tests 和统一错误信息。
4. layer key、source、volume、actualVolume、axis、radius、diameter 和 renderer metadata 在 layer generation、layer assignment、application、CSV/Gurobi、packing 和 renderer 之间一致。
5. `known-coordinate final path` 仍保持 final validation path 语义，不被 generated candidate path 复用或绕过。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 必跑门禁全部通过。
8. 修改 application、CSV、shape spec、depth boundary 或 solver 相关代码时，触发式 Gurobi 验收全部通过。
9. 修改 renderer DTO、fixture、packing renderer adapter 或显示语义时，外部 renderer build/typecheck/Rust/视觉验收全部完成并记录。
10. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

## 7. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 8. 触发式完整验收

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

## 9. 完成定义

1. 第 3 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 新增开放的 X/Z 横向圆柱 generated candidate path 具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的路径具备统一错误信息、negative tests 和门禁保护。
4. 必跑门禁与被触发的完整验收全部通过。
5. README、README_ch、refactor.md 与代码能力口径一致。
6. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
