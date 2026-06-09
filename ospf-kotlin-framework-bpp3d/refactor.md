# BPP3D 形状泛型化与圆柱支持重构计划

日期：2026-05-31
最近更新：2026-06-09

本文档记录 BPP3D “形状泛型化 + 圆柱支持”重构的当前状态与下一轮计划。总目标保持不变：在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 从 cuboid-only 业务模型继续收敛到 fully generic shape 生产模型，最终移除仅为兼容旧 cuboid-only 抽象而保留的兼容层，并完成连续半径优化与横向圆柱 stacking/hanging 自动支撑能力。

## 1. 已完成事项摘要

1. 已完成竖直圆柱、X/Z 轴对齐横向圆柱固定/离散半径 generated path、known-coordinate final path 和 renderer path 的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成已选择连续半径结果的最小生产闭环，并将 solver-native 连续半径未开放路径收敛为类型化缺口合同、变量原型和 RMP/final 注册计划；变量原型已进入列生成 solver 上下文、solve info 和 packing snapshot KPI，仅区间型 solver-native 连续半径求解仍保持 guarded。
4. 已开放横向圆柱贴地、单个全长长方体支撑、单条或同类重复窄支撑线 hanging、同类重复多支撑和异构多支撑区间覆盖的 3D stacking/hanging 检查，以及保守的 generated supported-stack/hanging 子集。
5. 已将横向圆柱长方体支撑覆盖收敛为 infrastructure 共享合同，由 generated stacking 和 final packing/rendering 几何门禁共同复用。
6. 已删除四批无调用或低风险的固定数值、shadow、pipeline、projection、物料装箱、layer 和 depth-limit 兼容别名，并加 generic 边界脚本禁止回流。
7. 已将无坐标横向圆柱 hanging 收敛为明确 unsupported contract：横向圆柱必须通过已验证 3D placement 支撑覆盖路径进入。
8. 已补齐 focused tests、application adapter、触发式 Gurobi focused、Gurobi dataset suite、final renderer guard 正负例验证与边界脚本门禁，覆盖已选择连续半径、连续半径变量原型/类型化缺口、横向圆柱 generated provenance、X/Z 支撑覆盖正负例和 unsupported fallback 边界。

## 2. 总目标与能力边界

### 2.1 总目标

最终目标是在尽可能少的后续迭代内完成 BPP3D fully generic shape 生产契约的跨层收口，并在功能等价或能力扩展完成后移除旧 cuboid-only 兼容层。凡是新增开放的能力，必须同时具备几何、支撑、solver、renderer、文档和测试闭环；无法闭环的能力必须收敛为明确 unsupported contract，不能保留半开放状态。

### 2.2 当前生产范围

1. 长方体生产链路保持兼容。
2. `Axis3.Y` 竖直圆柱保持已开放能力。
3. 固定半径/离散半径 `Axis3.X` / `Axis3.Z` 轴对齐横向圆柱支持 axis-aware circle-packing grid generated candidate path、保守的 full-length cuboid supported-stack generated candidate path、单条或同类重复窄支撑线 hanging generated candidate path、同类长方体重复多支撑轴向覆盖 generated candidate path，以及异构长方体多支撑轴向覆盖 generated candidate path。
4. 已知坐标 final packing / renderer path 支持通过真实几何和支撑校验的 `Axis3.X` / `Axis3.Z` 轴对齐横向圆柱。
5. 横向圆柱 3D stacking/hanging 支持贴地、单个全长长方体支撑、单条或同类重复窄支撑线 hanging、同类重复多支撑和异构多支撑区间覆盖子集；generated supported-stack/hanging 仅开放可验证的保守长方体支撑子集；无坐标 hanging 明确要求走已验证 3D placement 支撑覆盖，未覆盖底部支撑线的径向局部支撑和底部圆柱支撑仍不开放。
6. 已选择连续半径结果可开放：`radiusWeightFunctionKey`、CSV `radius_weight_function_key` 必须绑定类型化 selected-radius 结果和具体 `radius` / `radius_meter`，并禁止与离散 step 混用；solver-native 区间型连续半径变量已有类型化变量原型、列生成 solver 上下文承载和 RMP/final 注册计划，但真实模型注册、目标函数求解和无 concrete radius 的 key 必须通过类型化缺口合同保持 guarded，不得静默降级为固定半径或离散半径。
7. CSV、application DTO、program demand、material packing 和 renderer 只能传递已定义的 shape metadata，不能绕过 generated candidate provenance、known-coordinate final geometry 或 cuboid-only contract。

### 2.3 后续必要能力

1. 完全泛型化：逐步移除 cuboid-only 类型约束、外接长方体兼容入口和仅为旧算法保留的适配层，最终以 shape-generic API 作为生产边界。
2. 连续半径优化：在 solver、candidate generation、final validation、renderer actualVolume、CSV/application DTO 和文档中形成生产闭环。
3. 横向圆柱 stacking/hanging 自动支撑：在 generated path、支撑稳定性、final geometry、unsupported fallback、renderer fixture 和验收中形成完整闭环。

### 2.4 明确非目标

1. 任意 3D 旋转不是当前目标，不引入欧拉角、四元数或自由角度 renderer schema。
2. 隐式混轴同层生成不是当前目标；需要混轴能力时必须另行定义显式层/支撑/校验合同。
3. 所有非目标入口必须明确拒绝或保持 unsupported contract，不能静默降级为长方体或竖直圆柱。

## 3. 剩余工作量

距离总目标仍约剩余 5% 工作量。主要剩余工作不是当前 X/Z 固定/离散半径生产链路，而是：

1. solver-native 连续半径真实变量注册、目标函数和 radius selection 求解闭环；已选择半径结果的 final actual-radius / renderer 闭环已经开放，未开放路径已经具备类型化缺口合同、solver 变量原型、列生成 solver 上下文承载、RMP/final 注册计划和模型注册阻断诊断。
2. 横向圆柱 generated stacking/hanging 自动支撑从当前保守长方体 supported-stack/单条或同类重复窄支撑线 hanging 子集继续扩展；X/Z supported-stack/hanging focused、dataset suite、final renderer 多支撑、底部圆柱支撑拒绝、partial-support fallback 拒绝和共享支撑覆盖合同已覆盖。
3. 旧 cuboid-only 兼容层的下一批迁移、删除、调用点门禁和最小保留清单收敛；前四批无调用或低风险兼容别名已删除并加门禁。

## 4. 下一轮扩大收口事项

下一轮按“solver-native 连续半径模型注册阻断解除/selection 回写 + 下一批 shape-generic 迁移/删除 + 横向自动支撑可验证子集扩展 + dataset/renderer 验收”的大收口轮推进，目标是在一次迭代内把剩余工作压到约 3%-4%。下一轮优先交付可生产开放的闭环；只有当 solver、几何、支撑、renderer、CSV/Gurobi 和测试无法同时闭合时，才继续保留 shared guard/unsupported contract。

### 4.1 完全泛型化收口

1. 审计现存 cuboid-only 类型约束、外接长方体兼容入口、原始 `Cuboid` / `QuantityPlacement` / `Bin` 兼容构造和脚本 allowlist。
2. 迁移 application、CSV/Gurobi、program demand、material packing、final packing 和 renderer adapter 中低风险边界到 shape-generic API。
3. 删除下一批 stale cuboid-only 兼容构造、测试 fixture、脚本 allowlist 和外接长方体近似入口；前四批兼容别名已经删除，`placement2Of` 泛型工厂回流门禁仍只允许 typed factory 与 BLA 泛型投影搜索使用。
4. 为短期必须保留的 `Bin`、typed placement、projection 和历史算法边界建立最小保留清单、调用方清单、保留原因、删除条件和下一次删除批次。
5. 在边界脚本中新增或收紧 cuboid-only 回流检测，禁止新增生产入口依赖外接长方体近似绕过真实 shape 几何。
6. 把测试 helper、样例数据和文档术语同步到 shape-generic 口径，避免新测试继续固化 cuboid-only API。

### 4.2 连续半径优化生产闭环

1. 在已有 `ContinuousCylinderRadiusSolverPrototype`、列生成 solver 上下文和 RMP/final 注册计划基础上，先解除 core `Flt64` token-bound / 显式常量注册阻断，再注册真实 solver 半径变量，定义变量上下界、变量命名、目标函数/权重函数语义和 radius selection 结果回写位置。
2. 打通固定半径、离散半径、连续半径三类 radius metadata 的互斥和优先级协议，统一 application DTO、CSV、program demand 和 material packing 的解释。
3. 将 solver 选出的 final actual radius 带入 final validation、renderer `actualVolume`、packing snapshot、Gurobi result 和 dataset suite。
4. 已选择半径生产子集已覆盖 `Axis3.Y` 竖直圆柱，并已具备类型化 selected-radius 结果对象、solver 变量原型、列生成 solver 上下文和 CSV guard 诊断；下一步优先开放 solver-native 连续半径变量并回写 selected radius，若横向连续半径能完整闭环，则同步开放 `Axis3.X` / `Axis3.Z` 子集。
5. 补齐连续半径 positive/negative tests，覆盖合法目标函数、非法区间、重复/冲突 metadata、缺失 solver 支持、禁止静默离散替代、circle-packing 入口 guard、solver prototype 诊断和 renderer volume 回写。
6. Gurobi CSV `radius_weight_function_key` 已从单纯 guard 推进到 selected-radius 生产字段，并通过类型化缺口合同、变量原型、solver 上下文和注册计划覆盖 grouped-layer 与 material-width-amount 两类入口；下一步推进 interval-only continuous radius 到 solver-native 变量。当前阻断点是 column generation 只选择具体 `BinLayer` 列，尚无符号半径变量影响 footprint、volume、支撑覆盖和 renderer `actualVolume` 的统一接口，且 core `Flt64` 变量 bound 转换需要显式常量或 token-bound 支持；若仍无法开放，必须完成 core 注册阻断解除或 radius selection 回写中的一个更大闭环拆分。

### 4.3 横向圆柱 stacking/hanging 自动支撑闭环

1. 保持已开放的 X/Z 保守单支撑、单条或同类重复窄支撑线 hanging、同类重复多支撑和异构多支撑 generated supported-stack/hanging focused/dataset/final-renderer 正例，以及底部圆柱支撑、partial-support fallback、径向支撑线错位负例验收和共享支撑覆盖 helper 门禁。
2. 继续扩展可验证 hanging 子集，优先支持由真实坐标、真实支撑线/支撑区间和明确 provenance 表达的 generated/final path。
3. 保持无坐标 hanging 面积入口、局部支撑、底部圆柱支撑和混轴同层生成 guarded，除非本轮能用完整几何和支撑线语义闭环。
4. 将自动支撑结果接入 layer placement、final validation、renderer fixture、Gurobi CSV/dataset suite 和 application focused tests。
5. 补齐 generated stacking/hanging 的合法与非法 focused tests，覆盖单支撑、多支撑、hanging、局部支撑拒绝、底部圆柱支撑拒绝、overlap、outside bin、axis mismatch 和 provenance guard。
6. 若 solver 输入暂不能表达完整 hanging，则保持 hanging 为明确 guard，不允许通过面积近似或外接长方体降级。

### 4.4 Gurobi、renderer 与文档收口

1. 扩展触发式 Gurobi suite，覆盖连续半径、横向自动支撑、program/material metadata、shape-generic DTO 和 final validation 失败场景。
2. 新增或更新 Gurobi dataset 样例，继续覆盖 continuous radius、horizontal hanging guard、mixed cuboid/cylinder、unsupported metadata 和 supported-stack 失败场景；X/Z horizontal multi-support supported-stack 和 X horizontal repeated narrow-line hanging 正例已覆盖。
3. 保持 Gurobi 环境不可用时的 skip/trigger 语义，并提供非 Gurobi focused fallback。
4. 若 renderer DTO、fixture、adapter 或显示语义发生变化，同步外部 renderer build/typecheck/Rust/视觉验收；若仅新增 fixture，也必须验证 schema 与 actualVolume。
5. 同步 README、README_ch 和本文档，确保生产矩阵、unsupported 矩阵、CSV/Gurobi 协议、shape-generic 边界、cuboid-only 删除清单和验收入口一致。

## 5. 下一轮执行计划

1. 优先在 `Axis3.Y` `ContinuousCylinderRadiusSolverPrototype` 已进入 solver 上下文且 RMP/final 注册计划已暴露的基础上，解除 core bound 注册阻断，注册真实 solver 半径变量，落地变量上下界、目标函数、radius selection 结果对象和 selected radius 回写；已选择半径的 DTO/final/renderer 最小生产合同和类型化缺口合同继续保持。
2. 评估并同步开放 `Axis3.X` / `Axis3.Z` 横向连续半径子集；若横向连续半径不能与支撑、renderer actualVolume 和 CSV/Gurobi 同时闭环，则保留明确 guard。
3. 若 solver-native 连续半径无法完整生产开放，必须至少完成“core token-bound 注册阻断解除并将符号半径变量注册进 solver 模型”或“solver selection 回写 selected-radius 生产结果”中的一个可运行闭环，并把缺失的 footprint/volume 约束、目标函数和 renderer actualVolume 回写拆成下一批实现清单；interval-only guard、solver prototype 诊断、solver context 承载、registration plan 与 negative tests 必须保留。
4. 继续扩展可验证横向 hanging/stacking 子集，要求真实坐标、支撑线/支撑区间、provenance、final/rendering 校验和 unsupported fallback 同时存在；无坐标 hanging、局部支撑、底部圆柱支撑和混轴同层生成继续拒绝。
5. 扩展 generated supported-stack/hanging focused tests，覆盖 X/Z 单支撑、单条或同类重复窄支撑线 hanging、多支撑、局部支撑拒绝、底部圆柱支撑拒绝、径向支撑线错位拒绝、overlap、outside bin、axis mismatch 和 provenance guard。
6. 删除下一批 cuboid-only 兼容层：优先处理无调用别名、旧测试 fixture、裸 `QuantityPlacement` 入口、外接长方体近似入口和 stale allowlist；同时维护必须保留项的调用方和删除条件。material packing、layer 和 depth-limit 兼容别名已删除并加门禁。
7. 扩展 application、CSV/Gurobi、program/material packing、final validation、renderer fixture、dataset suite 和 negative tests，尤其覆盖 solver-native continuous radius 变量、selected radius 回写和 hanging guard。
8. 更新 README、README_ch、refactor.md、生产矩阵、unsupported 矩阵、CSV/Gurobi 协议和兼容层删除清单。
9. 跑完整 BPP3D 门禁、触发式 Gurobi 验收和必要 renderer 验收。
10. 只提交 BPP3D 本轮改动；外部 renderer 仅在 schema 或显示语义变化时单独处理。

## 6. 修改清单

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
13. `bpp3d-infrastructure/src/main/**/*`
14. `bpp3d-infrastructure/src/test/**/*`
15. `bpp3d-infrastructure/src/test/resources/renderer/*`
16. `scripts/*.ps1`
17. `README.md`
18. `README_ch.md`
19. `refactor.md`
20. 外部工程：`E:\workspace\ospf\framework\bpp3d-interface-renderer`，仅在 renderer DTO、fixture、adapter 或显示语义变化时修改。

## 7. 验收标准

1. 已开放的 `Axis3.Y`、`Axis3.X`、`Axis3.Z` 轴对齐圆柱生产路径不回退。
2. 完全泛型化必须继续推进下一批 cuboid-only 兼容层删除，并具备最小保留清单、调用方清单、脚本 allowlist、保留原因和后续删除条件；已删除的旧别名和兼容捷径不得回流。
3. 已选择连续半径生产子集必须保持 final actual radius、typed selected-radius result、typed solver prototype、solver context、renderer `actualVolume`、CSV/application DTO、Gurobi focused 和文档闭环；solver-native 连续变量若开放，必须补齐真实变量注册、目标函数和 radius selection 回写。
4. solver-native 连续半径优化若未能开放，必须保持 interval-only package production、circle-packing candidate、CSV/Gurobi typed gap guard、solver prototype 诊断、solver context 承载、registration plan、blocked reason、negative tests 和脚本门禁，明确记录阻断点，不能静默降级为固定半径或离散半径。
5. 横向圆柱 generated stacking/hanging 必须继续覆盖 X/Z 单支撑、单条或同类重复窄支撑线 hanging、同类重复多支撑和异构多支撑；若扩展 hanging，必须同时具备真实几何、支撑线/支撑区间、solver、renderer 和测试闭环。
6. 横向圆柱无坐标 hanging、局部支撑、底部圆柱支撑和混轴同层生成必须明确拒绝或保持 guarded；已开放的 supported-stack/hanging 不得绕过 generated provenance guard 或已验证 3D placement 支撑覆盖。
7. final MILP 后的 same-layer axis、depth boundary、real geometry、outside bin、overlap、actual radius 和横向圆柱支撑校验不可绕过。
8. CSV、application、generic DTO、program demand、material packing、Gurobi result 和 renderer/final packing 的 shape metadata 解释一致。
9. Gurobi dataset suite 必须覆盖新增开放能力和仍保留 unsupported 的关键入口。
10. README、README_ch、refactor.md 与代码能力口径一致，并明确下一轮后剩余工作量。
11. BPP3D 必跑门禁全部通过。
12. 触发式 Gurobi 验收全部通过或明确记录环境性 skip 条件。
13. 修改 renderer DTO、fixture、packing renderer adapter 或显示语义时，外部 renderer build/typecheck/Rust/视觉验收全部完成并记录。
14. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

## 8. 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

## 9. 触发式完整验收

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

## 10. 完成定义

1. 第 4 节所有事项均完成、明确延后或明确阻断，没有未分类状态。
2. 当前生产范围内的开放路径具备真实几何、支撑、solver、renderer、文档和测试闭环。
3. 保留 unsupported 的路径具备统一错误信息、negative tests 和门禁保护。
4. 必跑门禁与被触发的完整验收全部通过。
5. README、README_ch、refactor.md 与代码能力口径一致。
6. BPP3D 改动独立提交，非 BPP3D 改动和外部 renderer 改动不混入。
