# BPP3D 下一轮交接

日期：2026-06-10

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 从 cuboid-only 业务模型收敛到 fully generic shape 生产模型，完成连续半径优化与横向圆柱 stacking/hanging 自动支撑能力，并最终移除所有仅为兼容旧 cuboid-only 抽象而保留的兼容层，不保留兼容层。

所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不能开放的能力必须以统一 guarded contract、负例测试、文档说明和脚本门禁收口。

## 2. 已完成事项摘要

1. 已完成长方体、竖直圆柱、X/Z 横向圆柱、固定半径、离散半径和已选择连续半径结果的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑覆盖、unsupported fallback 和真实几何校验的主要闭环。
4. 已完成连续半径 guarded contract、solver 原型、solver 上下文、注册计划、诊断信息、边界校验和 renderer actual-radius 回写主链路。
5. 已完成 cuboid-only 兼容层多轮删除、剩余兼容层审计、调用方确认和回流门禁。
6. 已完成横向圆柱支撑正负例覆盖确认、Gurobi dataset suite 覆盖确认、完整 BPP3D 测试和边界脚本阶段性验收。
7. 已完成外部 renderer 原生圆柱支持、BPP3D `BoundingCuboid` renderer DTO 兼容映射移除、Tauri release 构建和人工视觉联调验收。

## 3. 当前边界

1. 已开放路径不得退化为 cuboid-only fallback 或外接长方体近似。
2. 连续半径 production-ready 路径已有 solver selection 和 renderer actual-radius 回写；真正 interval-only 连续半径仍保持 guarded。
3. 横向圆柱 generated supported-stack/hanging 已覆盖当前验证子集，仍必须保留 provenance、真实坐标、支撑线/区间和 final validation。
4. 无坐标 hanging、局部径向支撑、底部圆柱支撑、隐式混轴同层生成和任意 3D 旋转仍不是当前开放范围。
5. renderer 阻断已解除；BPP3D 不再需要为了外部显示将圆柱降级为 `BoundingCuboid`。
6. interval-only 连续半径不能改走离散化候选生成；下一轮应保持连续变量语义，采用保守分段线性近似方案。

## 4. 剩余工作量

距离总目标约剩余 <1%。剩余工作集中在 solver-native interval-only 连续半径的可生产近似闭环。

该问题的核心不是 DTO 或 renderer，而是半径 `r` 作为 solver 连续变量后会影响 `r^2` 体积、footprint、bounding size、支撑覆盖、碰撞和最终校验。现有 LP/MILP 链路不能直接表达这些非线性几何关系；下一轮目标是实现保守分段线性近似，而不是新增离散化计算模块。

## 5. 下一轮目标

下一轮目标是开放或准开放 `interval-only continuous radius v1`：使用真实连续半径 solver 变量 `r`，用分段线性近似表达 `r^2` 和体积相关项，用 `r_max` 保守 envelope 保障几何安全，并将 solver-selected radius 回写到 final validation、packing snapshot、Gurobi result 和 renderer `actualVolume`。

若分段线性近似无法在当前 core/model API 下完整落地，必须保持 typed gap、guard、负例测试、诊断信息和文档说明，不允许静默降级为固定半径、离散半径或 cuboid-only 外接盒近似。

## 6. 下一轮事项

1. **PWL contract 定义**：定义 interval-only 连续半径 v1 的分段线性近似合同，明确 `r`、`d = 2r`、`q ~= r^2`、`actualVolume ~= pi * h * q` 的变量、单位、误差和 metadata 口径。
2. **breakpoint 策略**：设计 breakpoint 生成规则、最大段数、误差上限、CSV/Gurobi 参数入口和默认策略；禁止把该策略实现为现有离散半径候选生成的替代入口。
3. **solver 注册实现**：注册连续半径变量、PWL/lambda/SOS2 或等价线性约束、上下界、目标函数接入、互斥协议和 solver-selected radius 提取。
4. **保守几何 envelope**：用 `r_max` 建模 placement footprint、bounding dimensions、支撑覆盖和碰撞安全边界；final validation 用 solver-selected `r` 计算真实圆柱 metadata 和 actual volume。
5. **结果回写闭环**：将 solver-selected `r`、`d`、近似/真实 volume、PWL 误差和 envelope 信息写入 RMP/final solve info、packing snapshot、Gurobi result、renderer DTO 和 schema KPI。
6. **互斥与诊断**：复核固定半径、离散半径、selected continuous radius、interval-only PWL continuous radius 的优先级、互斥协议、错误信息和 typed gap。
7. **测试与数据集**：补齐 PWL 正负例、边界半径、误差上限、unsupported bypass、final validation、renderer actualVolume 和 Gurobi dataset suite。
8. **文档与门禁**：同步 README、README_ch、refactor.md、daily.md 和边界脚本，明确 interval-only v1 是 conservative PWL approximation，并禁止 silent downgrade。

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

外部工程 `E:\workspace\ospf\framework\bpp3d-interface-renderer` 已完成原生圆柱支持并提交；下一轮仅在 renderer DTO、fixture、adapter 或显示语义确实变化时修改。

## 8. 验收标准

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. BPP3D 不再依赖 `BoundingCuboid` 作为 renderer 显示兼容映射。
3. interval-only continuous radius v1 若开放，必须使用真实连续半径 solver 变量，不得改为离散半径候选生成。
4. PWL 近似必须明确 breakpoint、误差上限、变量注册、目标函数、selection 回写和诊断信息。
5. 几何 envelope 必须保守，placement、碰撞、支撑覆盖不得因 solver-selected radius 变大而失效。
6. final validation 必须用 solver-selected radius 计算真实 radius、diameter、actualVolume 和 renderer metadata。
7. 固定半径、离散半径、selected continuous radius、interval-only PWL continuous radius 的 metadata 优先级和互斥协议必须清晰。
8. 若 interval-only PWL 路径仍未开放，必须保持 typed gap、solver prototype、solver context、registration plan、blocked reason、negative tests 和脚本门禁。
9. unsupported 范围必须明确拒绝或 guarded，不允许静默降级。
10. CSV、application DTO、program demand、material packing、Gurobi result、final packing 和 renderer metadata 解释一致。
11. README、README_ch、refactor.md、daily.md 与代码能力口径一致。
12. BPP3D 必跑门禁全部通过。
13. 触发式 Gurobi 验收全部通过，或明确记录环境性 skip 条件。
14. 修改外部 renderer 时，外部 renderer build、typecheck、Rust 检查、Tauri build 和必要视觉验收全部完成并记录。
15. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

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
npx tauri build
```

## 11. 本轮验收记录

1. BPP3D 自动验收已通过：generic boundary、shape boundary、geometry boundary、geometry module dry-run、`git diff --check -- ospf-kotlin-framework-bpp3d`、`mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`。
2. renderer 自动验收已通过：`npm run build`、`npx vue-tsc --noEmit`、`cargo check`、`cargo test`、`npx tauri build`。
3. renderer 人工视觉联调已通过：混合长方体/圆柱、unsupported/BoundingCuboid 兼容样例、legacy cuboid 样例均正常。
4. 保留 warning：geometry module dry-run 的 internal baseline warning、BPP3D 既有 Kotlin/JVM warning、renderer Vite chunk size warning、Rust resolver/static mut warning。

## 12. 下一会话启动建议

1. 先执行 `git status --short`，确认 BPP3D 与非目标模块改动边界。
2. 读取 `refactor.md` 和 `daily.md`，以本文件的 interval-only PWL approximation 目标为准。
3. 优先做 PWL contract 和 breakpoint 策略，不要从离散半径候选生成模块复制语义。
4. 每开放一段能力，都同步负例、脚本门禁、README/README_ch 和 Gurobi/renderer 验收记录。
5. 结束前更新 `refactor.md`、`daily.md`，记录实际完成项、剩余工作量和验收结果。
