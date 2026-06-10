# BPP3D 下一轮交接

日期：2026-06-10

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 从 cuboid-only 业务模型收敛到 fully generic shape 生产模型，完成连续半径优化与横向圆柱 stacking/hanging 自动支撑能力，并最终移除所有仅为兼容旧 cuboid-only 抽象而保留的兼容层，不保留兼容层。

所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不能开放的能力必须以统一 guarded contract、负例测试、文档说明和脚本门禁收口。

## 2. 已完成事项摘要

1. 已完成长方体、竖直圆柱、X/Z 横向圆柱、固定/离散半径和已选择连续半径结果的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑覆盖、unsupported fallback 和真实几何校验的主要闭环。
4. 已完成连续半径 guarded contract、solver 变量原型、solver 上下文、注册计划、诊断信息、边界校验和 renderer actual-radius 回写主链路。
5. 已完成多批 cuboid-only 兼容层删除、剩余兼容层全量审计、调用方确认和回流门禁。
6. 已完成横向圆柱支撑正负例覆盖确认、Gurobi dataset suite 覆盖确认、完整 BPP3D 测试和边界脚本阶段性验收。

## 3. 当前边界

1. 已开放路径不得退化为 cuboid-only fallback 或外接长方体近似。
2. 连续半径 production-ready 路径已有 solver selection 和 renderer actual-radius 回写；solver-native interval-only 连续半径变量仍保持 guarded。
3. 横向圆柱 generated supported-stack/hanging 已覆盖当前验证子集，仍必须保留 provenance、真实坐标、支撑线/区间和 final validation。
4. 无坐标 hanging、局部径向支撑、底部圆柱支撑、隐式混轴同层生成和任意 3D 旋转仍不是当前开放范围。
5. `BoundingCuboid` renderer 兼容映射仍保留，等待外部 renderer 升级后再移除。

## 4. 剩余工作量

距离总目标约剩余 <1%。剩余工作主要是两个外部阻断：

1. solver-native interval-only 连续半径完整闭环，需要 core 支持 per-instance bound 或 explicit constant registration。
2. renderer 原生圆柱 shape type 支持，需要外部 renderer 升级后移除 BPP3D 内 `BoundingCuboid` 兼容映射并完成外部验收。

## 5. 下一轮目标

下一轮目标是完成最终外部阻断收口：在 core 具备所需能力后开放 solver-native interval-only 连续半径路径；在 renderer 具备圆柱原生显示能力后移除 `BoundingCuboid` 兼容映射，并完成 BPP3D 与外部 renderer 的端到端验收。

若外部依赖仍未就绪，下一轮只允许补充 guard、诊断、文档和验收记录，不允许静默降级或扩大 unsupported 能力边界。

## 6. 下一轮事项

1. **core 阻断确认**：确认 core 是否已支持 continuous radius 变量所需的 per-instance bound 或 explicit constant registration。
2. **interval-only 闭环实现**：core 能力就绪后，注册真实连续半径 solver 变量，补齐上下界、目标函数、selection 结果、final validation、packing snapshot、renderer 和 Gurobi result 回写。
3. **连续半径互斥诊断**：复核固定半径、离散半径、selected continuous radius、interval-only continuous radius 的 metadata 优先级、互斥协议和错误信息。
4. **renderer 阻断确认**：确认外部 renderer 是否已支持圆柱 shape type、axis、radius/diameter、actualVolume 和相关 algorithm shape semantics。
5. **移除 `BoundingCuboid` 兼容映射**：renderer 就绪后，移除 BPP3D 内为外部 renderer 保留的 `BoundingCuboid` 映射，并同步 DTO、fixture、README 和边界脚本。
6. **Gurobi 验收补齐**：根据实际触发范围执行 focused 和 dataset suite，覆盖连续半径、横向圆柱、unsupported metadata 和 final validation 关键路径。
7. **文档同步**：同步 README、README_ch、refactor.md、daily.md 和外部 renderer daily，明确最终生产范围、仍保留 unsupported 范围和外部阻断状态。
8. **边界脚本收紧**：外部阻断解除后，禁止重新引入 cuboid-only fallback、真实几何绕过、unsupported bypass、deleted alias 回流和 continuous-radius silent downgrade。

## 7. 修改清单

下一轮允许修改范围：

1. `bpp3d-infrastructure/src/main/**/*`
2. `bpp3d-infrastructure/src/test/**/*`
3. `bpp3d-infrastructure/src/test/resources/renderer/**/*`
4. `bpp3d-domain-item-context/src/main/**/*`
5. `bpp3d-domain-item-context/src/test/**/*`
6. `bpp3d-domain-layer-generation-context/src/main/**/*`
7. `bpp3d-domain-layer-generation-context/src/test/**/*`
8. `bpp3d-domain-layer-assignment-context/src/main/**/*`
9. `bpp3d-domain-layer-assignment-context/src/test/**/*`
10. `bpp3d-domain-packing-context/src/main/**/*`
11. `bpp3d-domain-packing-context/src/test/**/*`
12. `bpp3d-application/src/main/**/*`
13. `bpp3d-application/src/test/**/*`
14. `bpp3d-application/src/gurobi-test/**/*`
15. `bpp3d-application/src/test/resources/gurobi/**/*`
16. `scripts/*.ps1`
17. `README.md`
18. `README_ch.md`
19. `refactor.md`
20. `daily.md`
21. 外部工程 `E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer shape type、DTO、fixture、adapter 或显示语义变化时修改。

## 8. 验收标准

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. 任何新开放能力必须具备真实几何、明确支撑、solver、renderer、CSV/Gurobi、文档和测试闭环。
3. 已删除 cuboid-only 兼容入口不得回流；剩余保留项必须继续有调用方、阻断原因和删除条件。
4. solver-native interval-only 连续半径若开放，必须具备真实变量注册、目标函数、selection 回写、actual radius 校验和 renderer actualVolume。
5. solver-native interval-only 连续半径若仍未开放，必须保持 typed gap、solver prototype、solver context、registration plan、blocked reason、negative tests 和脚本门禁。
6. renderer 原生圆柱支持就绪后，BPP3D 不再依赖 `BoundingCuboid` 作为显示兼容映射。
7. unsupported 范围必须明确拒绝或 guarded，不允许静默降级。
8. CSV、application DTO、program demand、material packing、Gurobi result、final packing 和 renderer metadata 解释一致。
9. README、README_ch、refactor.md、daily.md 与代码能力口径一致。
10. BPP3D 必跑门禁全部通过。
11. 触发式 Gurobi 验收全部通过，或明确记录环境性 skip 条件。
12. 修改外部 renderer 时，外部 renderer build、typecheck、Rust 检查和必要视觉验收全部完成并记录。
13. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

## 9. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 10. 触发式验收

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

## 11. 下一会话启动建议

1. 先执行 `git status --short`，确认 BPP3D、外部 renderer 与非目标模块改动边界。
2. 读取 `refactor.md`、本文件和 `E:\workspace\ospf\framework\bpp3d-interface-renderer\daily.md`。
3. 优先确认 core 与 renderer 两个外部阻断是否解除；未解除时只维护 guard、文档和验收记录。
4. 结束前更新 `refactor.md`、本文件和 renderer daily，记录实际完成项、剩余工作量和验收结果。
