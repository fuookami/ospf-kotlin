# BPP3D 下一轮交接计划

日期：2026-06-11

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 稳定在 fully generic shape 生产模型上。所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径。

已开放能力必须继续具备 solver、final validation、packing snapshot、renderer、CSV/Gurobi、文档和测试闭环；未开放能力必须通过 guarded contract、负例测试、文档和脚本门禁收口。

## 2. 已完成事项摘要

1. 已完成长方体、竖直圆柱、X/Z 横向圆柱、固定半径、离散半径、solver-selected 连续半径和 interval-only PWL 连续半径的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑覆盖、unsupported fallback 和真实几何校验闭环。
4. 已完成连续半径 guarded contract、solver 变量原型、PWL 近似、solver 上下文、注册计划、诊断信息、renderer actual-radius 回写和 actualVolume 回写。
5. 已完成 cuboid-only 兼容层删除、剩余兼容层审计、调用方确认和回流门禁。
6. 已完成 renderer 原生圆柱支持和 BPP3D `BoundingCuboid` renderer DTO 兼容映射移除。
7. 已完成 PWL v1 的 Gurobi focused、dataset suite、负例测试、性能 KPI、误差预算、极端半径、横向支撑、多 material / 多 demand 数据集覆盖。
8. 已完成 PWL 建模逻辑从 application 下沉到 domain component，application solver 主要保留编排职责。

## 3. 最终架构结论

PWL 连续半径应遵循 core 分层生命周期：

1. `MetaModel` 只注册领域变量、显式业务约束、目标和函数符号。
2. `UnivariateLinearPiecewiseFunction` 通过 `LinearFunctionSymbolAdapter` 注册为 `IntermediateSymbol`。
3. `solveLPAs` / `solveMILPAs` 会把 generic `LinearMetaModel<V>` 送入 solver Flt64 dump pipeline。
4. `LinearMechanismModel` 构建阶段注册函数符号 helper tokens，并调用函数符号的 `registerConstraints(...)` 展开 Big-M 约束。
5. 因此 BPP3D 不应在 `LinearMetaModel` 中镜像 `UnivariateLinearPiecewiseFunction.registerConstraints(...)`。

当前正确路径：

1. `model.add(r)`：注册 PWL 连续半径变量。
2. `model.addConstraint(...)`：注册半径上下界，这是领域显式约束。
3. `model.add(pwlSymbol)`：注册 core PWL 函数符号。
4. helper variables 和 PWL Big-M 分段约束由 core mechanism model lifecycle 展开。
5. 结果提取仍从 radius variable 和 `pwlFunction.resultVar` 读取 solver result。

## 4. 本轮修正

1. `ContinuousRadiusModelComponent` 已删除 PWL helper variable 手动注册。
2. `ContinuousRadiusModelComponent` 已删除 `registerPWLFunctionConstraints(...)` 私有镜像实现。
3. `ContinuousRadiusModelComponent` 只保留 radius variable、radius bound 和 `model.add(pwlSymbol)`。
4. 组件测试改为断言 `LinearMetaModel` 不包含 PWL 内部约束，`LinearMechanismModel` 构建后包含 PWL select-one 和 segment constraints。
5. 边界脚本改为禁止 BPP3D 任何位置回流 `registerPWLFunctionConstraints`、手动 helper loop 或 `registerAuxiliaryTokens(model.tokens)`。
6. README.md / README_ch.md 已修正为 core mechanism lifecycle 口径。

## 5. 本轮已验证

已执行：

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

结果：

1. `generic-boundary-check.ps1`：PASS。
2. `shape-boundary-check.ps1`：PASS。
3. `geometry-boundary-check.ps1`：PASS。
4. `geometry-module-dry-run.ps1`：PASS，保留 8 个 internal baseline warning。
5. `git diff --check -- ospf-kotlin-framework-bpp3d`：PASS。
6. `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`：PASS。

本轮涉及 core token/function lifecycle 修复，已额外执行：

```powershell
mvn --% -f ospf-kotlin-core/pom.xml -Dtest=FunctionSymbolRegressionTest test -Dgpg.skip=true
mvn --% -f ospf-kotlin-core/pom.xml install -Dmaven.test.skip=true -Dgpg.skip=true
```

结果：

1. `FunctionSymbolRegressionTest`：PASS。
2. core main artifact install：PASS；安装时跳过测试编译，规避既有测试源码编译缓存问题。

触发式 Gurobi 待执行：

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

## 6. 验收标准

1. PWL 函数符号继续通过 `model.add(pwlSymbol)` 注册。
2. PWL helper variables 不在 BPP3D `LinearMetaModel` 注册阶段手动注册。
3. PWL Big-M 约束不在 BPP3D `LinearMetaModel` 注册阶段手写。
4. `LinearMechanismModel` 构建后能看到 PWL select-one 和 segment constraints。
5. 除 core 函数符号实现外，BPP3D 不出现 `registerPWLFunctionConstraints`。
6. application solver 不恢复旧 PWL 注册函数。
7. renderer `actualVolume` 继续使用真实 `π*r²*h`。
8. PWL diagnostics 继续包含 `pwlVolume`、误差、段数和 envelope 状态。
9. 4 个边界脚本通过。
10. BPP3D 全模块测试通过。
11. Gurobi focused 和 dataset suite 在可用环境中通过。

## 7. 距离总目标的剩余工作判断

生产代码目标已基本达到。当前剩余工作是完成 Gurobi 触发式验收并提交。若 Gurobi 验收通过，BPP3D 当前重构目标可视为完成；后续只剩可选增强。
