# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-08

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的已完成状态与下一轮执行计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，继续推进 BPP3D 从 cuboid-only 业务模型收敛到 shape-aware / generic shape 模型。

## 1. 已完成事项摘要

1. 已完成竖直圆柱、横向圆柱 axis-aware generated path、已知坐标终态路径、真实几何校验、renderer metadata、depth boundary 和 CSV/Gurobi shape metadata 的基础闭环。
2. 已完成圆柱能力矩阵、unsupported contract、generated candidate provenance、cuboid-only search/merge、支撑语义、packing program/material packing shape metadata 和脚本门禁的主要收口。
3. 已完成离散半径/直径候选能力；连续半径优化、任意 3D 旋转和无法证明支撑安全的路径继续保持非生产能力。
4. 已完成 BPP3D focused tests、边界脚本、文档矩阵和触发式验收口径的同步维护，BPP3D 改动保持独立。
5. 已补充 CSV/Gurobi shape metadata focused 验收：material-width-amount 横向圆柱轴长解释、连续半径/直径区间误用、非法半径区间和 grouped-layer 横向圆柱手工层绕过均进入可复现测试；`shape-boundary-check.ps1` 已增加对应离散半径 guard。

## 2. 总目标与下一轮方向

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D shape-aware / generic shape 生产契约的跨层收口。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 下一轮主目标

下一轮选择 **CSV/Gurobi/final validation/dataset 全链路收口** 作为主目标。

目标是在保留当前 X/Z 横向圆柱 axis-aware generated path 和 known-coordinate final path 的前提下，一次性扩展 application 输入、CSV dataset suite、Gurobi column generation、final MILP 后验校验、renderer fixture 口径和文档矩阵。所有非法输入、非法列、非法 shape spec、混轴、缺失半径、连续半径误用、cuboid-only 路径误入和 final geometry 失败场景都必须进入可复现验收。

### 2.3 下一轮能力边界

1. 已开放的固定半径/离散半径 X/Z 横向圆柱 circle-packing generated path 与 known-coordinate final path 必须保持生产能力。
2. 连续半径优化、任意 3D 旋转、隐式混轴同层生成、横向圆柱 stacking/hanging 自动支撑生成默认仍非生产能力。
3. CSV、application DTO、program demand 和 material packing 只能传递 shape metadata，不能绕过 generated candidate provenance、known-coordinate final geometry 或 cuboid-only contract。
4. final MILP、renderer schema、depth boundary、same-layer axis 和 real packing geometry 后验校验必须保持语义分离。
5. 若 renderer DTO、fixture、adapter 或显示语义无变化，不触发外部 renderer 修改；若发生变化，必须同步外部 renderer 验收。

## 3. 下一轮事项

### 3.1 CSV 与 Application 输入收口

1. 审计 grouped-layer、material-width-amount、program demand 和 application direct input 的 shape metadata 入口。
2. 扩展 CSV dataset suite，覆盖横向圆柱非法路径、混轴同层、缺失半径、非法半径区间、连续半径误用、未知列、重复列、文件名 schema mismatch 和 cuboid-only 路径误入。
3. 保证 material-width-amount、packing program candidate 和 application DTO 对 `PackageShapeSpec` 的解释一致。
4. 对所有 schema/shape 配置错误补统一错误文案和 focused negative tests。

### 3.2 Gurobi 与 Column Generation 收口

1. 审计 column generation 候选生成、列选择、program demand、shadow price scoring 和 final layer placement adapter 的 shape-aware 合同。
2. 保证 X/Z 横向圆柱只从 verified axis-aware generated candidate 进入 application layer placement。
3. 扩展触发式 Gurobi suite，覆盖合法横向圆柱、非法横向圆柱、动态半径、program demand shape metadata 和 final validation 失败场景。
4. 若 Gurobi 不可用，必须保留 skip/trigger 语义并给出可复现的非 Gurobi fallback 验收。

### 3.3 Final Validation 与 Renderer 口径收口

1. 保证 final MILP 后仍执行 same-layer axis、depth boundary 和 `PackingGeometryGuard` 后验校验。
2. 扩展 final packing/renderer fixture，覆盖 X/Z 横向圆柱、动态半径实际体积、全长支撑、无支撑拒绝和混轴拒绝。
3. 若 renderer schema 无变化，仅更新仓内测试与 README；若 schema 有变化，同步外部 renderer build/typecheck/Rust/视觉验收。
4. 保持 known-coordinate final path 与 generated candidate path 的白名单分离。

### 3.4 边界脚本与文档

1. 扩展 `shape-boundary-check.ps1`，覆盖 CSV/application/Gurobi/final validation 的绕过风险。
2. 保持 `generic-boundary-check.ps1`、`geometry-boundary-check.ps1` 和 stale allowlist 检查有效。
3. README、README_ch 与 `refactor.md` 同步说明支持矩阵、unsupported 矩阵和验收入口。
4. 清理已被脚本覆盖的临时说明，避免文档与代码能力口径分叉。

## 4. 下一轮执行计划

1. **入口审计**：列出 application DTO、CSV loader、dataset suite、program demand、material packing 和 Gurobi test 的 shape metadata 入口。
2. **非法场景补齐**：先补 dataset/negative tests，确保非法 shape、非法列、混轴和 cuboid-only 误入都稳定失败。
3. **合法场景回归**：扩展合法 X/Z generated candidate、dynamic radius、program demand 和 final renderer fixture 覆盖。
4. **后验校验加固**：确认 final MILP 后的 same-layer axis、depth boundary 和 packing geometry guard 不可绕过。
5. **文档与门禁同步**：更新 README、README_ch、refactor.md 和边界脚本。
6. **独立提交**：执行必跑门禁与触发式 Gurobi/renderer 验收，只提交 BPP3D 本轮改动。

## 5. 修改清单

1. `bpp3d-application/src/main/**/*`
2. `bpp3d-application/src/test/**/*`
3. `bpp3d-application/src/gurobi-test/**/*`
4. `bpp3d-application/src/test/resources/gurobi/*`
5. `bpp3d-domain-layer-generation-context/src/main/**/*`
6. `bpp3d-domain-layer-generation-context/src/test/**/*`
7. `bpp3d-domain-layer-assignment-context/src/main/**/*`
8. `bpp3d-domain-layer-assignment-context/src/test/**/*`
9. `bpp3d-domain-packing-context/src/main/**/*`
10. `bpp3d-domain-packing-context/src/test/**/*`
11. `bpp3d-domain-item-context/src/main/**/*`
12. `bpp3d-domain-item-context/src/test/**/*`
13. `bpp3d-infrastructure/src/main/**/*`
14. `bpp3d-infrastructure/src/test/**/*`
15. `bpp3d-infrastructure/src/test/resources/renderer/*`
16. `scripts/*.ps1`
17. `README.md`
18. `README_ch.md`
19. `refactor.md`
20. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 6. 验收标准

1. 已开放的 X/Z 横向圆柱 generated candidate path 与 known-coordinate final path 不回退。
2. CSV/application/program/material packing 的 shape metadata 与 item path 能力一致。
3. Gurobi dataset suite 覆盖合法路径、非法路径、schema 错误、混轴、动态半径和 final validation 失败场景。
4. generated candidate path、known-coordinate final path、renderer final path 和 cuboid-only search path 没有白名单复用或绕过。
5. same-layer axis、横向圆柱支撑、depth boundary、final geometry 和 renderer schema 均有回归覆盖。
6. 所有 unsupported 路径使用共享 contract 文案，且有 negative tests 和脚本门禁保护。
7. README、README_ch、refactor.md 与代码能力口径一致。
8. BPP3D 必跑门禁全部通过。
9. 触发式 Gurobi 验收全部通过或明确记录环境性 skip 条件。
10. 修改 renderer DTO、fixture、packing renderer adapter 或显示语义时，外部 renderer build/typecheck/Rust/视觉验收全部完成并记录。
11. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

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
