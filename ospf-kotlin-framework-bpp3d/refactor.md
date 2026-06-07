# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-07

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与下一轮执行计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. 已完成竖直圆柱生产基础能力、横向圆柱已知坐标终态能力、真实几何校验、renderer metadata、depth boundary 后验硬校验和 CSV/Gurobi shape metadata 的基础闭环。
2. 已完成圆柱 unsupported contract、known-coordinate final path、renderer final path、depth boundary final path、业务层泛型泄漏和几何模块边界的主要门禁。
3. 已完成 dynamic radius/diameter 离散候选能力；连续半径优化明确保持非生产能力，并有防误用测试保护。
4. 已完成固定半径/离散半径 `Axis3.X` / `Axis3.Z` 横向圆柱 axis-aware circle-packing 候选生成、application/CSV 输入、column generation/Gurobi 选择、final geometry、renderer schema、README 和脚本门禁闭环。
5. 已完成 BPP3D 必跑门禁、focused tests 和触发式 Gurobi 验收的同步验证，且 BPP3D 改动保持独立。

## 2. 总目标与下一轮方向

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 下一轮主目标

下一轮选择 **剩余搜索/生成/支撑路径的 shape-aware contract 全面收口** 作为主目标。

目标是在保留本轮已开放的 X/Z 横向圆柱 axis-aware circle-packing 生产路径的前提下，一次性审计并收口 DFS/MLHS、block loading、BLA placement、pattern、pile、stacking、hanging、item merge、packing program 和 material packing 相关边界。能用现有真实几何与支撑 guard 证明安全的路径继续开放；无法证明的路径统一落到共享 unsupported contract、negative tests 和脚本门禁。

### 2.3 下一轮能力边界

1. 已开放的固定半径/离散半径 X/Z 横向圆柱 circle-packing generated path 必须保持生产能力。
2. 连续半径优化、任意 3D 旋转、隐式混轴同层生成、横向圆柱 stacking/hanging 自动支撑生成默认仍非生产能力，除非本轮能完成完整闭环。
3. DFS/MLHS、block loading、pattern、pile、merge、stacking、hanging 不能绕过 `CylinderShapeContract`、`PackingGeometryContract` 或 generated candidate contract。
4. generated candidate path、known-coordinate final path、renderer final path 和 cuboid-only search path 必须保持语义分离，不能互相复用白名单。
5. 若 renderer DTO、fixture、adapter 或显示语义无变化，不触发外部 renderer 修改；若发生变化，必须同步外部 renderer 验收。

## 3. 下一轮事项

### 3.1 Contract 与边界脚本

1. 梳理所有剩余路径对 `CylinderCapabilityPath`、generated candidate、known-coordinate final、cuboid-only search、support semantics 的使用。
2. 补齐 `CylinderShapeContract` 中缺失的路径分类、统一错误信息和能力状态命名。
3. 扩展 `shape-boundary-check.ps1`，覆盖 DFS/MLHS、block loading、BLA、pattern、pile、merge、stacking、hanging 和 application 入口的绕过风险。
4. 保持 stale allowlist 检查有效，避免门禁只增不减。

### 3.2 生成与搜索路径收口

1. 审计 `BlockLayerGenerator`、`BLLocalLayerGenerator`、`BLGlobalLayerGenerator`、`PatternLayerGenerator`、`PileLayerGenerator`、`SimpleBlockGenerator` 和 DFS/MLHS 搜索路径。
2. 对不能证明 shape-aware 几何正确性的路径统一拒绝圆柱或横向圆柱，并补 negative tests。
3. 对可证明的单件、贴地、固定轴向 generated path，必须通过 shared contract、真实几何 guard、layer placement adapter、final MILP 和 renderer schema 后再开放。
4. 明确 mixed shape / mixed axis / same-layer axis policy，避免隐式混用圆柱轴向。

### 3.3 支撑、堆叠与合并路径

1. 收口 `PackageAttribute.supportPackingShape`、stacking、hanging、pile support、item merge、pattern merge、block merge 和 hollow-square merge 的圆柱边界。
2. 横向圆柱若无全长支撑证明，不允许进入 stacking/hanging 自动生成路径。
3. 已知坐标下的横向圆柱全长长方体支撑能力保留，并补与 generated path 不冲突的回归测试。
4. 材料打包、packing program candidate、CSV program demand 若涉及 shape spec，必须与 item path 的圆柱能力一致。

### 3.4 Application、Gurobi、Renderer 与文档

1. 扩展 CSV/Gurobi dataset suite，覆盖横向圆柱非法路径、混轴同层、缺失半径、连续半径误用、未知列、重复列和 cuboid-only 路径误入。
2. 保证 final MILP 后仍执行 depth boundary、same-layer axis 和 final packing geometry 后验校验。
3. README、README_ch 与脚本门禁同步描述剩余路径的支持矩阵。
4. 若 renderer 输出语义变化，更新仓内 fixture 并执行外部 renderer build/typecheck/Rust/视觉验收。

## 4. 下一轮执行计划

1. **全路径审计**：先列出 DFS/MLHS、block loading、BLA、pattern、pile、merge、stacking、hanging、packing program 和 material packing 的圆柱入口与现有 guard。
2. **共享 contract 收口**：补齐缺失的 `CylinderCapabilityPath` / unsupported message / script gate，先让违规路径以统一错误失败。
3. **可开放路径评估**：只对能完整证明 footprint、support、solver、renderer 的路径开放；否则保持 unsupported，并补 focused negative tests。
4. **Gurobi 与 dataset 扩展**：把非法路径、混轴、动态半径和 CSV schema 场景纳入普通回归或触发式 dataset suite。
5. **文档与验收**：更新 README、README_ch、refactor.md，跑必跑门禁、focused tests、全量 test 和被触发的 Gurobi/renderer 验收。
6. **独立提交**：只提交 BPP3D 本轮改动，不混入非 BPP3D 或未触发的外部 renderer 改动。

## 5. 修改清单

1. `bpp3d-domain-item-context/src/main/**/*`
2. `bpp3d-domain-item-context/src/test/**/*`
3. `bpp3d-domain-layer-generation-context/src/main/**/*`
4. `bpp3d-domain-layer-generation-context/src/test/**/*`
5. `bpp3d-domain-layer-assignment-context/src/main/**/*`
6. `bpp3d-domain-layer-assignment-context/src/test/**/*`
7. `bpp3d-domain-block-loading-context/src/main/**/*`
8. `bpp3d-domain-block-loading-context/src/test/**/*`
9. `bpp3d-domain-bla-context/src/main/**/*`
10. `bpp3d-domain-bla-context/src/test/**/*`
11. `bpp3d-domain-packing-context/src/main/**/*`
12. `bpp3d-domain-packing-context/src/test/**/*`
13. `bpp3d-application/src/main/**/*`
14. `bpp3d-application/src/test/**/*`
15. `bpp3d-application/src/gurobi-test/**/*`
16. `bpp3d-application/src/test/resources/gurobi/*`
17. `bpp3d-infrastructure/src/main/**/*`
18. `bpp3d-infrastructure/src/test/**/*`
19. `bpp3d-infrastructure/src/test/resources/renderer/*`
20. `scripts/*.ps1`
21. `README.md`
22. `README_ch.md`
23. `refactor.md`
24. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 6. 验收标准

1. 已开放的 X/Z 横向圆柱 generated candidate path 不回退，application/CSV/Gurobi/final geometry/renderer schema 仍全部通过。
2. DFS/MLHS、block loading、BLA、pattern、pile、merge、stacking、hanging、packing program 和 material packing 的圆柱边界均已分类：开放、unsupported 或明确阻断。
3. 所有 unsupported 路径使用共享 contract 文案，且有 negative tests 和脚本门禁保护。
4. generated candidate path、known-coordinate final path、renderer final path 和 cuboid-only search path 没有白名单复用或绕过。
5. mixed axis、same-layer axis、横向圆柱支撑、depth boundary 和 final geometry 后验校验均有回归覆盖。
6. README、README_ch、refactor.md 与代码能力口径一致。
7. BPP3D 必跑门禁全部通过。
8. 修改 application、CSV、shape spec、depth boundary、solver 或 Gurobi 相关代码时，触发式 Gurobi 验收全部通过。
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
2. 所有新增开放路径具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的路径具备统一错误信息、negative tests 和门禁保护。
4. 必跑门禁与被触发的完整验收全部通过。
5. README、README_ch、refactor.md 与代码能力口径一致。
6. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
