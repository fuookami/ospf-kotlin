# BPP3D 下一轮交接

日期：2026-06-10

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 从 cuboid-only 业务模型收敛到 fully generic shape 生产模型，完成连续半径优化与横向圆柱 stacking/hanging 自动支撑能力，并最终移除所有仅为兼容旧 cuboid-only 抽象而保留的兼容层，不保留兼容层。

所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不能开放的能力必须以统一 guarded contract、负例测试、文档说明和脚本门禁收口。

## 2. 已完成事项摘要

1. 已完成长方体、竖直圆柱、X/Z 横向圆柱、固定/离散半径和已选择连续半径结果的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑覆盖、unsupported fallback 和真实几何校验的主要闭环。
4. 已完成连续半径 guarded contract、solver 变量原型、solver 上下文、注册计划、诊断信息、边界校验和 renderer 回写主链路。
5. 已完成多批 cuboid-only 兼容层删除和回流门禁，兼容层继续向归零推进。
6. 已完成 focused tests、完整 BPP3D 测试、触发式 Gurobi focused、Gurobi dataset suite、文档和边界脚本阶段性验收。

## 3. 当前边界

1. 已开放路径不得退化为 cuboid-only fallback 或外接长方体近似。
2. 连续半径 production-ready 路径已有 solver selection 和 renderer actual-radius 回写；solver-native interval-only 连续半径变量仍保持 guarded。
3. 横向圆柱 generated supported-stack/hanging 仍只开放已验证子集，必须保留 provenance、真实坐标、支撑线/区间和 final validation。
4. 无坐标 hanging、局部径向支撑、底部圆柱支撑、隐式混轴同层生成和任意 3D 旋转仍不是当前开放范围。

## 4. 剩余工作量

距离总目标约剩余 1%。主要剩余工作是 solver-native interval-only 连续半径变量完整闭环、外部 renderer 验收，以及最后一轮 cuboid-only 兼容层归零审计和验收收口。

## 5. 下一轮目标

下一轮目标是完成 BPP3D fully generic shape 生产契约的最后收口：推进连续半径 solver-native interval-only 闭环，扩展横向圆柱自动支撑的已验证子集，继续清除 cuboid-only 兼容层，并完成 Gurobi、renderer、文档和边界脚本的一次性验收。

优先交付可生产开放的闭环；无法开放的路径必须明确阻断原因、保留统一 guard、负例测试和脚本门禁。

## 6. 下一轮事项

1. **兼容层归零审计**：扫描 BPP3D 内剩余 cuboid-only 类型、旧构造、外接长方体近似入口、测试 fixture 和脚本 allowlist，形成删除、迁移、最终保留、外部阻断四类清单。
2. **删除下一批 compat 入口**：优先删除无调用入口、旧测试专用 fixture、可由 typed/generic API 替代的构造和 stale allowlist，并同步迁移调用方。
3. **连续半径闭环推进**：处理 interval-only 连续半径 solver-native 变量的注册、上下界、目标函数、selection 结果、final validation、packing snapshot、renderer 和 Gurobi result 回写。
4. **连续半径冲突诊断**：统一固定半径、离散半径、selected continuous radius、interval-only continuous radius 的 metadata 优先级、互斥协议和错误信息。
5. **横向圆柱自动支撑扩展**：扩展 X/Z generated supported-stack/hanging 可验证子集，覆盖单支撑、多支撑、异构支撑、错位、overlap、outside bin、axis mismatch 和 provenance guard。
6. **unsupported 范围收紧**：对无坐标 hanging、局部支撑、底部圆柱支撑、混轴同层生成和任意 3D 旋转保持明确拒绝，不允许面积近似或外接长方体降级。
7. **Gurobi 与 dataset suite 扩展**：覆盖连续半径、横向自动支撑、shape-generic DTO、unsupported metadata 和 final validation 失败场景。
8. **renderer 验收**：若 renderer DTO、fixture、adapter 或显示语义变化，执行外部 renderer build、typecheck、Rust 检查和必要视觉验收。
9. **文档同步**：同步 README、README_ch、refactor.md、CSV/Gurobi 协议、生产范围、unsupported 范围、兼容层删除清单和验收入口。
10. **边界脚本收紧**：继续禁止 deleted alias 回流、cuboid-only fallback、真实几何绕过、unsupported bypass 和 continuous-radius silent downgrade。

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
21. 外部工程 `E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 8. 验收标准

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. 新开放能力必须具备真实几何、明确支撑、solver、renderer、CSV/Gurobi、文档和测试闭环。
3. cuboid-only 兼容层继续减少，已删除入口不得回流；最终保留项必须记录调用方、阻断原因、删除条件和目标批次。
4. 连续半径若开放 solver-native interval-only 路径，必须具备真实变量注册、目标函数、selection 回写、actual radius 校验和 renderer actualVolume。
5. 连续半径若仍未完整开放，必须保持 typed gap、solver prototype、solver context、registration plan、blocked reason、negative tests 和脚本门禁。
6. 横向圆柱 supported-stack/hanging 扩展不得绕过 generated provenance、真实坐标、支撑线/区间和 final validation。
7. unsupported 范围必须明确拒绝或 guarded，不允许静默降级。
8. CSV、application DTO、program demand、material packing、Gurobi result、final packing 和 renderer metadata 解释一致。
9. Gurobi dataset suite 覆盖新增开放能力和仍保留 unsupported 的关键入口。
10. README、README_ch、refactor.md、daily.md 与代码能力口径一致。
11. BPP3D 必跑门禁全部通过。
12. 触发式 Gurobi 验收全部通过，或明确记录环境性 skip 条件。
13. 修改 renderer DTO、fixture、adapter 或显示语义时，外部 renderer build/typecheck/Rust/视觉验收全部完成并记录。
14. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

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

1. 先执行 `git status --short`，确认 BPP3D 与非 BPP3D 改动边界。
2. 读取 `refactor.md` 和本文件，优先从兼容层归零审计与 interval-only 连续半径闭环开始。
3. 每完成一类开放或 guard 收口，同步测试、脚本和 README/README_ch。
4. 结束前更新 `refactor.md` 与本文件，记录实际完成项、剩余工作量和验收结果。
