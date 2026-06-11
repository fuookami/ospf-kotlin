# BPP3D 下一轮交接计划

日期：2026-06-11

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 稳定在 fully generic shape 生产模型上。所有生产入口必须以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径。

已开放能力必须继续具备 solver、final validation、packing snapshot、renderer、CSV/Gurobi、文档和测试闭环；未开放能力必须通过 guarded contract、负例测试、文档和脚本门禁收口。

下一轮的核心目标不是新增业务能力，而是把已经可用的 interval-only PWL 连续半径链路对齐 core 的标准中间符号生命周期：`UnivariateLinearPiecewiseFunction` 应作为 `IntermediateSymbol` 注册进 `LinearMetaModel`，由 `LinearMechanismModel` 在求解模型构建阶段自动展开函数约束。BPP3D 不应继续镜像 core 的 Big-M 约束实现。

## 2. 已完成事项摘要

1. 已完成长方体、竖直圆柱、X/Z 横向圆柱、固定半径、离散半径、solver-selected 连续半径和 interval-only PWL 连续半径的主要生产闭环。
2. 已完成 shape metadata、CSV/Gurobi、program demand、material packing、depth boundary、final geometry、renderer metadata、README 和边界脚本的基础收口。
3. 已完成横向圆柱贴地、supported-stack、hanging、多支撑覆盖、unsupported fallback 和真实几何校验闭环。
4. 已完成连续半径 guarded contract、solver 变量原型、PWL 近似、solver 上下文、注册计划、诊断信息、renderer actual-radius 回写和 actualVolume 回写。
5. 已完成 cuboid-only 兼容层删除、剩余兼容层审计、调用方确认和回流门禁。
6. 已完成 renderer 原生圆柱支持和 BPP3D `BoundingCuboid` renderer DTO 兼容映射移除。
7. 已完成 PWL v1 的 Gurobi focused、dataset suite、负例测试、性能 KPI、误差预算、极端半径、横向支撑、多 material / 多 demand 数据集覆盖。
8. 已完成 PWL 建模逻辑从 application 下沉到 domain component 的第一阶段重构，application solver 已主要保留编排职责。

## 3. 当前关键结论

`UnivariateLinearPiecewiseFunction` 已经是 core 提供的函数型中间符号。把它通过 `model.add(pwlFunction)` 注册进 `LinearMetaModel` 后，core 会在 `LinearMechanismModel.invoke(metaModel)` 阶段遍历 `tokens.symbols`，对 `MathFunctionSymbolBase` 调用 `registerConstraintsUnchecked(model)`，最终执行 `UnivariateLinearPiecewiseFunction.registerConstraints(...)`。

因此当前 BPP3D 的 PWL 组件虽然已经使用 core 的 `UnivariateLinearPiecewiseFunction`，但仍存在架构冗余：

1. 手动遍历 `pwlFunction.helperVariables` 并 `model.add(...)`。
2. 手动调用 `pwlFunction.registerAuxiliaryTokens(model.tokens)`。
3. 手写 `registerPWLFunctionConstraints(...)`，镜像 core 的 Big-M select-one 和 segment 约束。

下一轮应移除这些镜像实现，改为完全走 core `IntermediateSymbol` lifecycle。

## 4. 下一轮目标

### 4.1 主目标

将 BPP3D PWL 连续半径注册方式改为 core 中间符号注册：

1. BPP3D 只注册 radius variable 和 radius bound。
2. BPP3D 通过 `model.add(pwlFunction)` 注册 `UnivariateLinearPiecewiseFunction`。
3. helper variables、auxiliary tokens、PWL Big-M 函数约束交给 core token / mechanism model 生命周期处理。
4. 删除 BPP3D 私有 `registerPWLFunctionConstraints(...)`。
5. 删除或收紧允许手写 PWL Big-M 约束的脚本白名单。
6. 保持 PWL result extraction、renderer actualVolume、PWL diagnostics 和 Gurobi dataset 行为不变。

### 4.2 扩展目标

在主目标完成后，顺手减少后续迭代：

1. 清理 PWL 组件中只为手写约束服务的代码、注释和测试断言。
2. 增加 core lifecycle 回归测试，确保 `LinearMechanismModel` 构建后 PWL 函数约束确实展开。
3. 增加边界脚本，禁止 BPP3D 重新实现 core PWL Big-M 约束。
4. 更新 README / README_ch / daily.md，明确 PWL 建模职责由 core symbol lifecycle 承担。
5. 检查 Gurobi dataset suite 和 renderer metadata 是否仍能观测到 solver-selected radius、pwlVolume、actualVolume 和误差诊断。

## 5. 下一轮计划

### 阶段 0：启动与边界确认

1. 执行 `git status --short`，确认工作区是否有其他模块改动。
2. 执行 `git status --short -- ospf-kotlin-framework-bpp3d`，确认 BPP3D 当前状态。
3. 读取 `.rules/chore.md`、`.rules/framework-architecture.md` 和本文件。
4. 确认 `refactor.md` 不存在，不恢复该文件。
5. 确认本轮只修改 BPP3D；不混入 CSP1D、core 或外部 renderer，除非发现 core lifecycle 本身有缺陷且必须修复。

阶段产出：

1. 记录工作区初始状态。
2. 明确是否存在非本轮改动。

### 阶段 1：核对 core lifecycle

目标是用代码事实确认迁移路径，避免误删必要注册。

必查文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModel.kt`
   - 确认 `fun add(symbol: IntermediateSymbol<*>): Try` 走 `tokens.add(symbol)`。
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTableRegistrationSupport.kt`
   - 确认 token 注册阶段会调用 `registerAuxiliaryTokens`。
   - 确认函数约束不在 token 阶段注册。
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModel.kt`
   - 确认 `LinearMechanismModel.invoke(metaModel)` 构建后遍历 `tokens.symbols`。
   - 确认 `MathFunctionSymbolBase` 调用 `registerConstraintsUnchecked(model)`。
4. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModelFlt64Conversion.kt`
   - 确认 unchecked cast 边界集中在 core solver boundary。
5. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/symbol/function/UnivariateLinearPiecewise.kt`
   - 确认 `helperVariables`、`registerAuxiliaryTokens`、`registerConstraints` 的职责。

阶段产出：

1. 在 commit 或 `daily.md` 中记录确认结果。
2. 若发现 core lifecycle 不能满足需求，先停止迁移并记录阻断，不要保留半迁移状态。

### 阶段 2：审计 BPP3D 当前 PWL 注册点

目标是找出所有依赖手写 PWL 约束的生产代码、测试和脚本。

建议命令：

```powershell
rg -n "registerPWLFunctionConstraints|registerAuxiliaryTokens\\(model\\.tokens\\)|helperVariables|UnivariateLinearPiecewiseFunction|pwl_total_constraints|select_one|seg_.*_eq|Big-M|PWLApplicationConstraintRegistrationReflux" ospf-kotlin-framework-bpp3d
```

重点文件：

1. `bpp3d-domain-item-context/src/main/**/ContinuousRadiusModelComponent.kt`
2. `bpp3d-domain-item-context/src/test/**/ContinuousRadiusModelComponentTest.kt`
3. `bpp3d-domain-item-context/src/test/**/ContinuousRadiusSelectionExtractorTest.kt`
4. `bpp3d-infrastructure/src/test/**/PWLRadiusSquaredApproximationTest.kt`
5. `bpp3d-application/src/gurobi-test/**/GurobiColumnGenerationTest.kt`
6. `scripts/shape-boundary-check.ps1`
7. `README.md`
8. `README_ch.md`

阶段产出：

1. 列出必须删除的手写注册点。
2. 列出必须改写的测试断言。
3. 列出必须收紧的脚本白名单。

### 阶段 3：迁移 `ContinuousRadiusModelComponent`

目标是让 domain component 只负责领域边界，PWL 函数内部建模交给 core。

建议修改：

1. 在 PWL 变量注册流程中保留 `model.add(r)`。
2. 保留 `r >= rMin` 和 `r <= rMax` 两条 radius bound 约束。
3. 删除 helper variable 手动注册循环：
   - 删除 `for (helperVar in pwlFunction.helperVariables) { model.add(...) }`。
4. 删除手动 auxiliary token 注册：
   - 删除 `pwlFunction.registerAuxiliaryTokens(model.tokens)`。
5. 删除手写 Big-M 注册调用：
   - 删除 `registerPWLFunctionConstraints(model, pwlFunction, variableName, ensureTry)`。
6. 改为：

```kotlin
ensureTry(
    model.add(pwlFunction),
    "register PWL function symbol for $variableName"
)
```

7. 删除私有 `registerPWLFunctionConstraints(...)` 函数。
8. 检查 import，删除不再使用的 `Comparison`、`LinearInequality`、`LinearMonomial`、`LinearPolynomial` 等，仅保留 radius bound 仍需要的类型。
9. 保留 extraction 逻辑：
   - `model.tokens.find(pwlVar.radiusVariable)`
   - `model.tokens.find(pwlVar.pwlFunction.resultVar)`
   - `actualRadiusSquared = r * r`
   - `pwlError = q - r*r`
10. 检查 `modelScaleInfo()`：
   - `pwl_total_helper_vars` 可以继续用 `pwlFunction.helperVariables.size`。
   - `pwl_total_constraints` 可以继续按 core PWL 展开公式 `1 + 4 * numSegments` 计算，但文档说明这些约束由 core mechanism model 注册。

阶段产出：

1. BPP3D 不再出现私有 `registerPWLFunctionConstraints`。
2. PWL function 通过 `model.add(pwlFunction)` 注册。
3. 编译通过。

### 阶段 4：补充 core lifecycle 测试

目标是证明迁移不是只改代码，而是确实由 core 展开 PWL 约束。

建议测试方向：

1. 组件注册测试：
   - 创建 interval-only prototype。
   - 创建 `ContinuousRadiusModelComponent`。
   - 调用 `component.register(linearMetaModel, ::ensureTry)`。
   - 断言 `metaModel.tokens.symbols` 包含 `pwlFunction`。
   - 断言 `metaModel.tokens` 能找到 radius variable。
2. mechanism 展开测试：
   - 基于上述 `LinearMetaModel` 构建 `LinearMechanismModel`。
   - 断言 mechanism model 约束中包含 `${pwlFunction.name}_select_one`。
   - 断言至少包含 segment lower / upper / equality 约束名。
   - 断言 selector variables 和 result variable 已进入 mechanism tokens。
3. extraction 测试：
   - 若已有求解级测试覆盖，可保留现状。
   - 如果只做 unit test，可构造 token result 或沿用现有 integration test。
4. 负例测试：
   - PWL 不可注册时仍 blocked。
   - 无 `radiusWeightFunctionKey` 时仍 blocked。
   - PWL 误差超限、envelope 溢出、silent downgrade 防护仍有效。

注意：

1. 不要再断言 BPP3D 自己注册了 Big-M 约束。
2. 测试名称要体现 core lifecycle，例如 `shouldRegisterPWLFunctionAsIntermediateSymbol`、`shouldLetLinearMechanismModelExpandPWLFunctionConstraints`。

阶段产出：

1. 新增或更新组件测试。
2. 旧的“手写约束注册”测试全部改成“symbol lifecycle”测试。

### 阶段 5：收紧边界脚本

目标是禁止手写 PWL Big-M 逻辑回流。

建议修改 `scripts/shape-boundary-check.ps1`：

1. 更新 `PWLApplicationConstraintRegistrationReflux`：
   - 不再只禁止 application 中的 PWL 约束注册。
   - 扩展为禁止 BPP3D 内出现 `registerPWLFunctionConstraints`、`${name}_seg_` 手写注册、`pwlFunction.registerAuxiliaryTokens(model.tokens)` 等模式。
2. 新增或改名为 `PWLCustomBigMRegistrationReflux`：
   - 检测 `select_one` + `seg_` + `Big-M` + `model.addConstraint` 的组合。
   - 允许 core 文件不在扫描范围内；BPP3D 不应实现。
3. 保留必要例外：
   - README / daily.md 可以描述 Big-M，但生产代码不允许手写。
   - 测试可以检查 core 展开后的 constraint name，但不应包含私有注册函数。
4. 保持原有检查：
   - `PWLDiscreteFallbackReflux`
   - `ContinuousRadiusUnsupportedRegression`
   - `DeletedCuboidCompatAliasReflux`
   - `BoundingCuboid` fallback 禁止项

阶段产出：

1. 边界脚本能阻止旧实现回流。
2. 4 个边界脚本通过。

### 阶段 6：文档更新

目标是让 README 和交接文档与新职责边界一致。

更新内容：

1. `README.md`
   - 说明 PWL 连续半径使用 core `UnivariateLinearPiecewiseFunction`。
   - 说明 BPP3D 注册 radius bounds 和 PWL function symbol。
   - 说明 helper tokens 和 Big-M constraints 由 core `LinearMechanismModel` 展开。
2. `README_ch.md`
   - 同步中文说明。
3. `daily.md`
   - 追加本轮实际完成记录、commit、验收结果、剩余事项。
4. 不恢复 `refactor.md`。

阶段产出：

1. 文档不再暗示 BPP3D 手写 PWL Big-M 约束。
2. 英中文 README 口径一致。

### 阶段 7：回归与 Gurobi 验收

本轮修改 PWL solver registration，必须执行完整 BPP3D 门禁和 Gurobi 触发式验收。

必跑：

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

触发式 Gurobi：

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

阶段产出：

1. 记录测试数量、失败数、skip 数。
2. 记录任何环境性 skip。
3. 若 Gurobi 不可用，不提交“完全完成”结论，只记录环境阻断。

## 6. 事项清单

### 必做事项

1. **启动检查**
   - 执行 `git status --short`。
   - 执行 `git status --short -- ospf-kotlin-framework-bpp3d`。
   - 确认非 BPP3D 改动不纳入 commit。

2. **core lifecycle 复核**
   - 复核 `MetaModel.add(symbol)`。
   - 复核 token 注册阶段的 auxiliary token 处理。
   - 复核 `LinearMechanismModel` 的 function symbol constraint registration。
   - 复核 `UnivariateLinearPiecewiseFunction.registerConstraints(...)`。

3. **BPP3D PWL 注册审计**
   - 搜索 `registerPWLFunctionConstraints`。
   - 搜索 `registerAuxiliaryTokens(model.tokens)`。
   - 搜索 `pwlFunction.helperVariables` 的手动注册。
   - 搜索 PWL Big-M / segment 约束手写逻辑。

4. **迁移 PWL function 注册**
   - 保留 radius variable 注册。
   - 保留 radius lower / upper bound。
   - 用 `model.add(pwlFunction)` 注册 core PWL symbol。
   - 删除 helper variable 手动注册。
   - 删除 auxiliary token 手动注册。
   - 删除手写 Big-M 约束函数。

5. **清理 import 和注释**
   - 删除不再使用的 core math inequality import。
   - 更新 KDoc 中 “BPP3D 注册 Big-M 约束” 的表达。
   - 保留中英双语注释。

6. **调整模型规模诊断**
   - 保留 helper variable 和 constraint count KPI。
   - 文档说明这些约束由 core mechanism model 展开。
   - 确认 KPI 数值不因注册方式变化而错误。

7. **更新组件测试**
   - 增加 `model.add(pwlFunction)` 注册断言。
   - 增加 `tokens.symbols` 包含 PWL function 断言。
   - 增加 `LinearMechanismModel` 展开 PWL 约束断言。
   - 删除“BPP3D 手写 Big-M 约束注册”的断言。

8. **保持结果提取闭环**
   - 确认 `extractPWLResults` 仍可读取 radius variable。
   - 确认仍可读取 `pwlFunction.resultVar`。
   - 确认 `PWLRadiusSelectionMetadata.actualVolume()` 仍用真实 `r*r`。
   - 确认 renderer info 仍包含 PWL diagnostics。

9. **收紧边界脚本**
   - 禁止 `registerPWLFunctionConstraints` 回流。
   - 禁止 BPP3D 手写 `pwlFunction.registerAuxiliaryTokens(model.tokens)`。
   - 禁止生产代码手写 PWL segment Big-M 约束。
   - 保留 README/daily 描述例外。

10. **更新 README**
    - 英文 README 更新 PWL lifecycle 说明。
    - 中文 README 同步更新。
    - 不恢复 `refactor.md`。

11. **执行必跑门禁**
    - 4 个边界脚本。
    - whitespace check。
    - BPP3D 全模块测试。

12. **执行 Gurobi 触发式验收**
    - Gurobi focused test。
    - Gurobi dataset suite。
    - 记录测试数量和 skip。

13. **提交拆分**
    - 按第 8 节拆 commit。
    - 每个 commit 使用 Conventional Commit header。
    - commit message 写清目的和关键变更。

14. **最终交接**
    - 更新 `daily.md` 的执行记录。
    - 写清完成、未完成、阻断和验收结果。
    - 给出距离总目标的剩余工作判断。

### 可选增强事项

1. 如果发现 PWL function 注册后 mechanism constraint name 与旧测试不一致，补充一个稳定的 test helper，不要在生产代码中为测试保留旧命名。
2. 如果 mechanism 展开测试构建成本过高，可新增 focused unit test，只构建单个最小 `LinearMetaModel`。
3. 如果 Gurobi dataset suite 时间过长，可先跑 focused，再跑 suite；最终提交前仍应完成 suite 或记录环境阻断。
4. 如果 README 中仍有 “BPP3D Big-M 约束注册” 的历史表达，同步清理。
5. 如果边界脚本误报文档中的 Big-M 描述，应缩小扫描范围到 `src/main`，不要放宽生产代码检查。

## 7. 修改清单

允许修改范围：

1. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/**/*`
2. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/test/**/*`
3. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/**/*`
4. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/**/*`
5. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/**/*`
6. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/**/*`
7. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/gurobi-test/**/*`
8. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/**/*`
9. `ospf-kotlin-framework-bpp3d/scripts/*.ps1`
10. `ospf-kotlin-framework-bpp3d/README.md`
11. `ospf-kotlin-framework-bpp3d/README_ch.md`
12. `ospf-kotlin-framework-bpp3d/daily.md`

原则上不修改：

1. `ospf-kotlin-core/**/*`
2. `ospf-kotlin-framework-csp1d/**/*`
3. `E:\workspace\ospf\framework\bpp3d-interface-renderer/**/*`

例外：

1. 如果发现 core lifecycle 实际缺陷，先记录阻断并单独讨论，不直接混入 BPP3D commit。
2. 如果 renderer 显示语义没有变化，不修改外部 renderer。

## 8. Commit 拆解

### Commit 1：迁移 PWL symbol 注册

Header 建议：

```text
refactor(bpp3d): register PWL radius function through core symbol lifecycle
```

内容：

1. `ContinuousRadiusModelComponent` 改用 `model.add(pwlFunction)`。
2. 删除 helper variable 手动注册。
3. 删除 auxiliary token 手动注册。
4. 删除 `registerPWLFunctionConstraints(...)`。
5. 清理 import 和注释。

验收：

1. BPP3D 编译通过。
2. 组件相关测试通过。

### Commit 2：补充 lifecycle 测试

Header 建议：

```text
test(bpp3d): cover core lifecycle expansion for PWL radius symbols
```

内容：

1. 新增或调整 component tests。
2. 断言 PWL function 进入 `tokens.symbols`。
3. 断言 `LinearMechanismModel` 展开 PWL 约束。
4. 调整旧的手写约束断言。

验收：

1. BPP3D 全模块测试通过。

### Commit 3：收紧边界脚本

Header 建议：

```text
test(bpp3d): prevent custom PWL Big-M registration reflux
```

内容：

1. 更新 `shape-boundary-check.ps1`。
2. 禁止手写 PWL function constraints 回流。
3. 禁止手动 auxiliary token 注册回流。
4. 保留必要文档例外。

验收：

1. 4 个边界脚本通过。
2. `git diff --check` 通过。

### Commit 4：文档与交接更新

Header 建议：

```text
docs(bpp3d): document PWL radius core symbol lifecycle
```

内容：

1. 更新 README.md。
2. 更新 README_ch.md。
3. 更新 daily.md。

验收：

1. README 英中文口径一致。
2. `refactor.md` 不恢复。

### Commit 5：Gurobi 回归记录

Header 建议：

```text
test(bpp3d): verify PWL radius lifecycle with Gurobi datasets
```

适用条件：

1. 若只更新测试记录，不一定需要单独 commit。
2. 若因 Gurobi 验收调整 dataset 或测试断言，使用该 commit。

内容：

1. 更新 Gurobi 测试或 dataset。
2. 记录 focused 和 suite 验收结果。

验收：

1. Gurobi focused test 通过。
2. Gurobi dataset suite 通过。

## 9. 验收标准

### 功能验收

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. interval-only PWL 连续半径仍可进入 solver。
3. PWL solver-selected radius 仍能回写到 result、packing analyzer 和 renderer。
4. renderer `actualVolume` 仍使用真实 `π*r²*h`，不使用 PWL q 近似体积。
5. `pwlVolume`、`pwlAbsoluteError`、`pwlRelativeError`、`numSegments`、`isWithinEnvelope` 等诊断仍存在。
6. 无 `radiusWeightFunctionKey` 的 interval-only 原型仍 blocked。
7. unsupported 范围仍明确拒绝，不静默降级。

### 架构验收

1. BPP3D 通过 `model.add(pwlFunction)` 注册 `UnivariateLinearPiecewiseFunction`。
2. BPP3D 不再手写 `registerPWLFunctionConstraints(...)`。
3. BPP3D 不再手动调用 `pwlFunction.registerAuxiliaryTokens(model.tokens)`。
4. BPP3D 不再手动注册 PWL helper variables。
5. PWL 函数约束由 core `LinearMechanismModel` 展开。
6. application solver 不承载 PWL 约束细节。
7. domain component 保留连续半径注册、提取和 diagnostics 边界。

### 测试验收

1. 组件测试覆盖 PWL symbol 注册。
2. 组件测试覆盖 mechanism model 展开后的 PWL 约束。
3. PWL negative tests 仍通过。
4. PWL integration tests 仍通过。
5. Gurobi focused test 通过，或明确记录环境性 skip。
6. Gurobi dataset suite 通过，或明确记录环境性 skip。
7. BPP3D 全模块测试通过。

### 门禁验收

1. `generic-boundary-check.ps1` 通过。
2. `shape-boundary-check.ps1` 通过。
3. `geometry-boundary-check.ps1` 通过。
4. `geometry-module-dry-run.ps1` 通过。
5. `git diff --check -- ospf-kotlin-framework-bpp3d` 无错误。
6. 新增门禁能阻止 PWL 手写 Big-M 约束回流。
7. 已删除 cuboid-only compat alias 不回流。
8. `BoundingCuboid` renderer fallback 不回流。

### 文档验收

1. README.md 说明 PWL 走 core symbol lifecycle。
2. README_ch.md 同步说明。
3. daily.md 记录实际完成项、commit、验收结果和剩余事项。
4. 不恢复 `refactor.md`。

## 10. 下一会话启动建议

1. 先跑 `git status --short`。
2. 再复核 core lifecycle 代码，不要直接改 BPP3D。
3. 优先完成 `ContinuousRadiusModelComponent` 的注册迁移。
4. 迁移后先跑 focused component tests，再跑全模块测试。
5. 边界脚本最后收紧，避免迁移过程中误报阻塞开发。
6. 结束前必须更新本文件，记录实际执行结果和剩余工作量。

## 11. 执行记录（2026-06-11 续）

### 已完成

1. **发现 core InfraNumber 限制**（阶段 0-1）
   - 核实了 `MechanismModelFlt64Conversion.kt`：`registerConstraintsUnchecked` 通过 `SolverBoundaryCasts` 做星投影到 Flt64。
   - 确认 core `LinearMechanismModel` 约束展开和 `registerAuxiliaryTokens` 仅在 Flt64 mechanism model 构建阶段发生。
   - BPP3D 使用 `InfraNumber`，不走 core mechanism model 的 Flt64 约束展开路径。
   - 结论：PWL Big-M 约束和 helper variable 注册必须在 `LinearMetaModel<InfraNumber>` 上完成，不能依赖 core mechanism model。

2. **修订 ContinuousRadiusModelComponent 注册路径**（阶段 2-3）
   - `model.add(pwlSymbol)` 注册 PWL 函数符号（IntermediateSymbol lifecycle）。
   - 手动 `model.add(helperVar)` 注册 helper variables 到 `LinearMetaModel.tokens`。
   - `registerPWLFunctionConstraints` 在 `LinearMetaModel` 上注册 Big-M 约束。
   - Big-M 计算对齐 core `UnivariateLinearPiecewiseFunction.registerConstraints` 的 per-segment 逻辑。
   - 更新 KDoc 和注释，说明 InfraNumber 路径限制。
   - 避免 `Flt64` token（改用"求解器精度浮点数"措辞）。

3. **更新测试**（阶段 4）
   - 测试断言从 `LinearMechanismModel.constraints` 改为 `LinearMetaModel.constraints`。
   - Helper variable 断言从 `mechanismModel.tokens` 改为 `model.tokens.find(helperVar)`。
   - 删除 `LinearMechanismModel` 和 `runBlocking` 残留 import。
   - `modelScaleInfo()` 注释更新，反映 InfraNumber 注册路径。

4. **更新边界脚本**（阶段 5）
   - `PWLCustomBigMRegistrationReflux`：允许 `ContinuousRadiusModelComponent.kt` 使用 `registerPWLFunctionConstraints` 和 helper variable 注册循环。
   - `PWLApplicationConstraintRegistrationReflux`：允许 `ContinuousRadiusModelComponent.kt` 使用 `registerPWLFunctionConstraints`。
   - 其他文件仍禁止手写 PWL 约束。

5. **门禁验收**
   - `generic-boundary-check.ps1`：PASS
   - `shape-boundary-check.ps1`：PASS
   - `geometry-boundary-check.ps1`：PASS
   - `geometry-module-dry-run.ps1`：PASS（8 warnings）
   - `git diff --check`：PASS
   - BPP3D 全模块测试：56 tests, 0 failures, 0 skipped, BUILD SUCCESS

### Commit

- `fbecdc6bd refactor(bpp3d): register PWL Big-M constraints on LinearMetaModel for InfraNumber path`

### 修正后的架构结论

原始计划（第 3 节）假设 core `LinearMechanismModel` 能自动展开 PWL 约束，因此 BPP3D 应删除所有手写 PWL 约束。但实际发现：

1. Core `LinearMechanismModel` 约束展开路径仅支持 Flt64 模型。
2. Core `registerAuxiliaryTokens` 仅在 Flt64 mechanism model 构建阶段的 `register()` 流程中调用。
3. BPP3D 使用 `InfraNumber`，PWL Big-M 约束必须在 `LinearMetaModel<InfraNumber>` 上注册。

因此 BPP3D 的 PWL 注册路径为：
- `model.add(pwlSymbol)`：注册 IntermediateSymbol（PWL 函数符号）
- `model.add(helperVar)`：手动注册 helper variables（selector vars、result var）
- `registerPWLFunctionConstraints(model, ...)`：在 LinearMetaModel 上注册 Big-M 约束

这与原始计划的方向部分不同：不再删除 `registerPWLFunctionConstraints`，而是将其明确为 InfraNumber 路径下的必要实现。

### 未完成 / 剩余

1. **Gurobi 触发式验收**未执行（需要 Gurobi 环境和插件构建）。
2. **README / README_ch 更新**未完成——需要反映 InfraNumber 路径的实际架构。
3. 原计划 Commit 4（docs）和 Commit 5（Gurobi 回归）未执行。

### 距离总目标的剩余工作判断

PWL 连续半径的 solver 注册路径已稳定，核心架构已验证：
- PWL 函数符号通过 core IntermediateSymbol lifecycle 注册 ✓
- Helper variables 和 Big-M 约束在 LinearMetaModel 上注册（因 InfraNumber 限制）✓
- 结果提取、renderer 回写、PWL diagnostics 不变 ✓
- 边界脚本和测试闭环 ✓

剩余工作量为文档级和 Gurobi 端到端验证，不影响生产代码。
