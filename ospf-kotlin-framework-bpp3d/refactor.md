# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-10（兼容层归零审计 + 横向支撑正负例补齐 + Gurobi dataset suite 收口轮）

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的当前状态与下一轮计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 从 cuboid-only 业务模型收敛到 fully generic shape 生产模型，完成连续半径优化与横向圆柱 stacking/hanging 自动支撑能力，并最终移除所有仅为兼容旧 cuboid-only 抽象而保留的兼容层，不保留兼容层。

## 1. 已完成事项摘要

1. 已完成竖直圆柱、X/Z 轴对齐横向圆柱、固定/离散半径、已选择连续半径结果的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑区间覆盖、unsupported fallback 和真实几何校验的主要闭环。
4. 已完成连续半径未开放路径的类型化缺口合同、solver 变量原型、solver 上下文承载、共享注册计划、边界校验和诊断闭环。
5. 已完成连续半径 solver-native 变量注册闭环：production-ready 变量注册为 `RealVar` solver 变量，使用约束式上下界（constraint-based bounds）和目标等式约束，solver 选出值在 RMP/final info 中以 `continuous_radius_solver_selected_*` 键暴露；interval-only 变量仍通过类型化缺口合同保持 guarded。
6. 已完成多批旧 cuboid-only 兼容别名、低风险入口和 stale allowlist 删除，并加脚本门禁防止回流。
7. 已完成 focused tests、完整 BPP3D 测试、触发式 Gurobi focused、Gurobi dataset suite、文档和边界脚本验收。
8. 已完成连续半径 solver-selected radius renderer 回写闭环：solver 选出的半径从 `ColumnGenerationResult.continuousRadiusSolverResults` 传递到 `ColumnGenerationPackingAnalyzer`，经 `buildContinuousRadiusSelectionResults` 转换为 `CylinderRadiusSelectionResult`，再传入 `PackingRendererAdapter.toSchema(result, continuousRadiusSelectionResults)` 重载，renderer adapter 对有 solver 选出半径的圆柱 item 用其计算 `actualVolume` 和 `radius`/`diameter` DTO 字段。
9. 已完成第二批 cuboid-only compat 扩展属性删除（16 个 `ItemCuboid`/`Projection`/`AnyPlacement` 的 `packageType`/`packageCategory`/`bottomOnly`/`topFlat` compat 扩展属性），保留 `Projection<*,*>.bottomOnly` 作为 shape-generic 属性（因 BLA 算法依赖）；删除 `ItemProjection<*>.bottomOnly` 消除与 `Projection<*,*>.bottomOnly` 的 overload resolution 歧义。
10. 已完成边界脚本新增 deleted cuboid-only compat alias reflux 检测、continuous-radius solver result writeback 检测和 propagation 检测。
11. 已完成兼容层全量审计：无删除候选（所有兼容层均有明确调用方）；2 处 `QuantityPlacement3<*>` 迁移为 `AnyPlacement3`/`ItemPlacement3` domain alias；16 处 `HorizontalCylinderAxisInGenerationOutOfAllowList` 已注册 allowlist 且 guard 已就位；`BoundingCuboid` renderer DTO 需等待外部 renderer 升级。
12. 已完成横向圆柱支撑 11 维度正负例审计确认：单支撑、多支撑、重复窄支撑线、异构支撑、局部支撑拒绝、底部圆柱支撑拒绝、径向支撑错位、overlap、outside bin、axis mismatch、provenance guard 均已有正负例测试覆盖。
13. 已完成 Gurobi dataset suite 覆盖确认：竖直圆柱固定半径、离散半径、连续半径、横向 X/Z 支撑、横向 X/Z hanging、混合形状、深度边界和生产级数据集均已覆盖。renderer DTO 无新增字段，不需触发外部 renderer build 验收。

## 2. 总目标与边界

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D fully generic shape 生产契约的跨层收口。所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；所有 cuboid-only 兼容层必须迁移或删除，最终不保留兼容层。

### 2.2 当前生产范围

1. 长方体生产链路保持兼容，但不能作为新 shape 能力的降级路径。
2. `Axis3.Y` 竖直圆柱和 `Axis3.X` / `Axis3.Z` 轴对齐横向圆柱的固定/离散半径路径已开放。
3. 横向圆柱支持可验证的 generated supported-stack/hanging 子集、known-coordinate final path 和 renderer path。
4. `radiusWeightFunctionKey` 仅在具备类型化 selected-radius 结果、具体半径、边界校验和 actual-radius 回写时开放。
5. solver-native interval-only 连续半径变量仍保持 guarded，不能静默降级为固定半径或离散半径。
6. 任意 3D 旋转、隐式混轴同层生成、局部径向支撑、底部圆柱支撑和无坐标 hanging 面积入口不是当前目标。

### 2.3 剩余工作量

距离总目标仍约剩余 <1%。剩余工作主要是：solver-native interval-only 连续半径变量的完整闭环（需要 core 支持 per-instance bound 或 explicit constant registration，属于外部阻断）、以及外部 renderer 构建验收（需等待 renderer 升级支持圆柱 shape type 后移除 `BoundingCuboid` 兼容映射）。renderer `actualVolume` 的 solver-selected radius 回写路径已在本轮完成。兼容层审计确认无删除候选，所有保留项均有明确调用方和 allowlist 注册。

## 3. 下一轮扩大收口事项

下一轮按“连续半径 solver-native 闭环 + 横向自动支撑扩展 + cuboid-only 兼容层归零推进 + Gurobi/renderer/docs 一次性验收”的范围执行。优先交付生产可开放闭环；不能开放的能力必须收敛为统一 guarded contract、负例测试、文档说明和脚本门禁。

### 3.1 完全泛型化与兼容层归零推进

1. 全量审计 BPP3D 内仍保留的 cuboid-only 类型、外接长方体近似入口、旧 placement/projection/bin 兼容构造、测试 fixture 和脚本 allowlist。
2. 迁移 application、domain、CSV/Gurobi、program demand、material packing、final packing、renderer adapter 和测试 helper 到 shape-generic API。
3. 删除下一批 cuboid-only 兼容层，优先处理无调用入口、只为旧测试保留的 fixture、可由 typed/generic API 替代的构造和 stale allowlist。
4. 对确实无法本轮删除的入口建立最终保留清单，记录调用方、阻断原因、删除条件和目标删除批次。
5. 收紧边界脚本，禁止新增 cuboid-only 生产入口、外接长方体近似绕过真实 shape 几何、已删除别名回流和未登记 allowlist。

### 3.2 连续半径优化生产闭环

1. 解除或绕过当前 core `Flt64` bound / 显式常量注册阻断，注册真实连续半径 solver 变量。
2. 定义连续半径变量的上下界、命名、目标函数、权重函数、互斥协议、selection 结果和回写位置。
3. 将 solver 选出的 actual radius 接入 final validation、renderer `actualVolume`、packing snapshot、Gurobi result 和 dataset suite。
4. 打通固定半径、离散半径、selected continuous radius、interval-only continuous radius 四类 metadata 的优先级和冲突诊断。
5. 若 solver-native 连续半径仍无法完整开放，至少完成一个可运行的大闭环：真实变量注册入模型，或 solver selection 回写 selected-radius 生产结果。
6. 保留 interval-only guard、typed gap、solver prototype、solver context、registration plan、blocked reason、negative tests 和脚本门禁。

### 3.3 横向圆柱 stacking/hanging 自动支撑扩展

1. 扩展 X/Z generated supported-stack/hanging 可验证子集，继续要求真实坐标、支撑线/支撑区间、明确 provenance、final validation 和 renderer metadata 同时存在。
2. 补齐单支撑、多支撑、重复窄支撑线、异构支撑、局部支撑拒绝、底部圆柱支撑拒绝、径向支撑错位、overlap、outside bin、axis mismatch 和 provenance guard。
3. 将自动支撑结果贯通 layer placement、final packing、renderer fixture、Gurobi CSV/dataset suite 和 application focused tests。
4. 对不能完整表达的 hanging 输入保持明确 guard，不允许用面积近似、外接长方体或无坐标入口降级。

### 3.4 Gurobi、renderer、文档与样例收口

1. 扩展 Gurobi focused 和 dataset suite，覆盖连续半径、横向自动支撑、shape-generic DTO、unsupported metadata 和 final validation 失败场景。
2. 若 renderer DTO、fixture、adapter 或显示语义变化，同步外部 renderer build、typecheck、Rust 检查和视觉验收。
3. 同步 README、README_ch、refactor.md、CSV/Gurobi 协议、生产范围、unsupported 范围、兼容层删除清单和验收入口。
4. 删除或合并过时迁移文档，避免多个计划文档并行维护。

## 4. 下一轮执行计划

1. 先做兼容层全量扫描和 allowlist 清点，输出“删除、本轮迁移、最终保留、外部阻断”四类清单。
2. 按低风险到高风险删除 cuboid-only 兼容层，并同步迁移调用方、测试 helper、README 和边界脚本。
3. 推进 continuous radius solver-native 闭环，优先完成变量注册和 selection 回写中可落地的一条主路径。
4. 扩展横向圆柱 generated/final hanging 与 supported-stack 子集，并补齐正负例。
5. 扩展 CSV/Gurobi、application、program/material packing、final validation、renderer fixture 和 dataset suite。
6. 收紧 generic/shape/geometry 边界脚本，新增对 deleted alias、cuboid-only fallback、unsupported bypass 和 continuous-radius silent downgrade 的检测。
7. 更新 README、README_ch 和本文档，确保总目标、当前生产范围、剩余工作量、下一轮验收口径一致。
8. 跑完整 BPP3D 门禁、触发式 Gurobi 验收，以及必要的外部 renderer 验收。
9. 只提交 BPP3D 本轮改动；非 BPP3D 或未触发的外部 renderer 改动不混入。

## 5. 修改清单

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
20. 外部工程 `E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 6. 验收标准

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. 新开放能力必须具备真实几何、支撑、solver、renderer、CSV/Gurobi、文档和测试闭环。
3. cuboid-only 兼容层继续减少，已删除入口不得回流；最终保留项必须有调用方、阻断原因和删除条件。
4. 连续半径若开放 solver-native 路径，必须具备真实变量注册、目标函数、selection 回写、actual radius 校验和 renderer `actualVolume`。
5. 连续半径若仍未完整开放，必须保持 typed gap、solver prototype、solver context、registration plan、blocked reason、negative tests 和脚本门禁。
6. 横向圆柱 supported-stack/hanging 扩展不得绕过 generated provenance、真实支撑线/区间和 final validation。
7. 无坐标 hanging、局部支撑、底部圆柱支撑、混轴同层生成和任意 3D 旋转必须明确拒绝或保持 guarded。
8. CSV、application DTO、program demand、material packing、Gurobi result、final packing 和 renderer metadata 解释一致。
9. Gurobi dataset suite 覆盖新增开放能力和仍保留 unsupported 的关键入口。
10. README、README_ch、refactor.md 与代码能力口径一致，并明确下一轮后剩余工作量。
11. BPP3D 必跑门禁全部通过。
12. 触发式 Gurobi 验收全部通过或明确记录环境性 skip 条件。
13. 修改 renderer DTO、fixture、adapter 或显示语义时，外部 renderer build/typecheck/Rust/视觉验收全部完成并记录。
14. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

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

1. 第 3 节事项完成、明确延后或明确阻断，没有未分类状态。
2. 当前生产范围内的开放路径具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的路径具备统一错误信息、negative tests 和门禁保护。
4. 必跑门禁与被触发的完整验收全部通过。
5. README、README_ch、refactor.md 与代码能力口径一致。
6. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
