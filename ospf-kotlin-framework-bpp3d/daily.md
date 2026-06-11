# BPP3D 下一轮交接计划

日期：2026-06-11

## 1. 总目标

在不回退既有长方体生产链路、CSV/Gurobi 链路和 renderer 契约的前提下，将 BPP3D 稳定在 fully shape-polymorphic 生产模型上。所有生产入口必须以 shape-polymorphic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径。

已开放能力必须继续具备 solver、final validation、packing snapshot、renderer、CSV/Gurobi、文档和测试闭环；未开放能力必须通过 guarded contract、负例测试、文档和脚本门禁收口。

## 2. 已完成事项摘要

1. 已完成 shape-polymorphic 生产入口、圆柱几何、横向支撑、连续半径 PWL v1、renderer 原生圆柱与 actual-radius 回写闭环。
2. 已完成 cuboid-only 兼容层清理、`BoundingCuboid` renderer 兼容映射移除、关键文档、负例测试和边界脚本收口。
3. 已完成 PWL 连续半径建模职责收敛：BPP3D 只注册领域变量、领域边界约束和 core PWL 函数符号，PWL helper token 与 Big-M 约束由 core mechanism lifecycle 展开。
4. 已完成 core token/function lifecycle 必要修复，并提交为 `95432009a`。
5. 已完成 BPP3D 必跑门禁与全模块测试阶段性验收。

## 3. 当前边界

1. BPP3D 不得在 `LinearMetaModel` 阶段手写或镜像 PWL Big-M 约束。
2. BPP3D 不得手动注册 `pwlFunction.helperVariables` 或调用 `registerAuxiliaryTokens(model.tokens)`。
3. PWL 连续半径必须继续通过 `model.add(pwlSymbol)` 接入 core `UnivariateLinearPiecewiseFunction`。
4. renderer `actualVolume` 必须继续使用 solver-selected radius 的真实 `pi * r^2 * h`，不得回退到 envelope volume 或 PWL volume。
5. unsupported 范围继续 guarded，不允许 silent downgrade。
6. CSP1D 当前有无关未提交改动，下一会话不得混入 BPP3D 提交。

## 4. 下一轮目标

下一轮目标是做 BPP3D 收尾增强，不改变已开放能力边界：

1. 完成 Gurobi focused 与 dataset suite 触发式验收，确认 PWL lifecycle 修复后 solver 端到端行为稳定。
2. 建立 PWL 参数调优基线，形成默认 segment / tolerance / radius interval 的推荐口径。
3. 收敛 PWL 诊断字段，使 Gurobi result、final validation、packing snapshot、renderer metadata 与 README 示例可直接对照。
4. 继续硬化边界脚本，防止 PWL 手写约束、actualVolume 回退、application solver 重新堆建模逻辑等问题回流。
5. 整理 PWL 测试分层与命名，降低后续维护成本。
6. 将 `daily.md` 从阶段性交接逐步转为完成记录，README 补充简短流程说明。

## 5. 下一轮事项

### 5.1 Gurobi 触发式端到端验收

1. 安装 Gurobi core plugin：
   - 执行 `mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true`。
   - 若环境缺少 Gurobi license、native library 或 license server，记录明确 skip 原因，不修改生产代码绕过。
2. 执行 focused CG 测试：
   - 执行 `GurobiColumnGenerationTest` focused 命令。
   - 重点检查 PWL interval-only 连续半径 case 是否仍生成 solver-selected radius、PWL metadata、actual radius 和 renderer actualVolume。
3. 执行 dataset suite：
   - 使用 `bpp3d-application/src/test/resources/gurobi` 全量 dataset。
   - 重点覆盖 grouped-layer、material-width-amount、连续半径、unsupported metadata、横向圆柱和 final validation。
4. 验收输出整理：
   - 在 `daily.md` 记录 focused 与 dataset suite 的测试数量、skip 数量和失败原因。
   - 若失败，优先判断是环境问题、dataset 问题、solver precision 问题还是 lifecycle 回归。
   - 不允许通过禁用 PWL、回退 cuboid-only 或回退 fixed/discrete radius 来消除失败。

### 5.2 PWL 参数调优基线

1. 梳理当前 PWL 配置入口：
   - 查找 `PWLRadiusApproximationConfig`、`PWLBreakpointStrategy`、`PWLRadiusSquaredApproximation` 的调用点。
   - 确认默认 `maxSegments`、relative tolerance、absolute tolerance、Big-M envelope 来源和 debug info 开关。
2. 设计最小 benchmark / regression 数据集：
   - 半径区间小：例如 `[0.5, 2.0]`。
   - 半径区间中：例如 `[2.0, 8.0]`。
   - 半径区间大：例如 `[1.0, 50.0]`。
   - tight bin：接近边界的容器尺寸。
   - multi material / multi demand：验证 PWL 参数不会只对单一 demand 成立。
3. 对比策略：
   - Uniform。
   - Adaptive。
   - ErrorDriven。
   - 每种策略至少比较 segment 数、最大相对误差、最大绝对误差、求解时间和 actualVolume 偏差。
4. 输出推荐：
   - 形成默认 segment 与 tolerance 建议。
   - 标明何时建议开启 debug info。
   - 标明极端 radius interval 是否需要强制提高 segment 或拒绝。
5. 若新增测试：
   - 优先放在 `bpp3d-infrastructure/src/test` 或 `bpp3d-application/src/gurobi-test`。
   - 测试名称体现策略、误差预算和数据规模。

### 5.3 PWL 诊断字段收敛

1. 盘点字段来源：
   - `PWLExtractedRadius.info()`。
   - `PWLRadiusSelectionMetadata`。
   - `ContinuousRadiusModelComponent.modelScaleInfo()`。
   - `ColumnGenerationResult` / final result info。
   - renderer item metadata。
2. 统一字段口径：
   - radius：solver-selected `r`。
   - q / pwlRadiusSquared：PWL function result。
   - actualRadiusSquared：真实 `r^2`。
   - pwlAbsoluteError / pwlRelativeError：误差。
   - pwlVolume：`pi * q * h`。
   - actualVolume：`pi * r^2 * h`。
   - envelope：`rMin` / `rMax` 与是否 within envelope。
3. 避免字段重复与歧义：
   - 同一字段不要在不同层级使用不同含义。
   - 对外 README 示例只展示稳定字段。
   - 内部 debug 字段可保留，但要有前缀。
4. 增加 focused assertion：
   - renderer metadata 中 actualVolume 与 PWL volume 不应混用。
   - Gurobi result info 与 renderer metadata 中 selected radius 一致。
   - final validation 使用 actual radius 而非 envelope radius。

### 5.4 边界脚本硬化

1. 在 `shape-boundary-check.ps1` 中继续保留并扩展 PWL lifecycle 检查：
   - 禁止 `registerPWLFunctionConstraints` 回流。
   - 禁止 `for (helperVar in pwlFunction.helperVariables)` 手动注册回流。
   - 禁止 `registerAuxiliaryTokens(model.tokens)` 回流。
2. 新增或增强 actualVolume 回退检查：
   - 禁止 renderer adapter 在 PWL path 使用 envelope volume 作为 `actualVolume`。
   - 禁止使用 `pwlVolume` 覆盖 `actualVolume`。
   - 允许 `pwlVolume` 作为诊断字段。
3. 新增 application solver 建模边界检查：
   - application 层不得新增 PWL helper variable 注册函数。
   - application 层不得直接拼 PWL Big-M segment constraints。
   - application 层只允许调用 domain component / context 暴露的注册入口。
4. 检查误报：
   - 对脚本新增检查必须先跑全量边界脚本。
   - 若需要 allowlist，必须写明原因，不能泛化到整个目录。

### 5.5 测试分层整理

1. 梳理现有 PWL 测试：
   - infrastructure approximation / negative tests。
   - domain item component lifecycle tests。
   - packing integration negative tests。
   - application / Gurobi tests。
2. 建议命名分层：
   - `PWLRadiusSquaredApproximationTest`：只测数学近似与误差预算。
   - `ContinuousRadiusModelComponentTest`：只测 domain component 注册、分类、lifecycle。
   - `PWLContinuousRadiusIntegrationNegativeTest`：只测跨 domain 的负例和 silent downgrade。
   - `GurobiColumnGenerationTest`：只测 solver 端到端。
3. 清理重复断言：
   - 同一个字段的基础存在性不要在三层重复测。
   - 保留一处主断言，其他层只验证跨层契约。
4. 不做大规模重构：
   - 只调整命名、分组、局部 helper。
   - 不改变已通过测试的业务语义。

### 5.6 文档收尾

1. README / README_ch：
   - 增加简短 PWL 生命周期说明：`MetaModel -> MechanismModel -> Solver -> renderer actual radius`。
   - 明确 `actualVolume`、`pwlVolume`、`envelope` 的区别。
   - 明确 PWL 仍是近似线性化，不是离散半径候选。
2. daily.md：
   - 下一轮结束时改写为完成记录。
   - 记录 Gurobi 验收状态、参数调优结论和仍保留的可选增强。
3. 若改动外部 renderer：
   - 只在 DTO、fixture、adapter 或显示语义变化时修改。
   - 当前计划不需要修改 renderer。

## 6. 执行计划

1. 启动检查：
   - 执行 `git status --short`。
   - 确认只处理 `ospf-kotlin-framework-bpp3d` 与必要 core / core-plugin 验收，不混入 CSP1D。
   - 阅读本文件、README.md、README_ch.md 与 `shape-boundary-check.ps1` 的 PWL 检查段。
2. Gurobi 验收优先：
   - 先跑 core plugin install。
   - 再跑 focused。
   - 最后跑 dataset suite。
   - 失败时先记录并定位，不急于修改。
3. 参数调优：
   - 先做静态代码盘点。
   - 再补最小 benchmark / regression。
   - 最后更新 README 推荐口径。
4. 诊断与边界脚本：
   - 先统一字段命名与断言。
   - 再增强脚本。
   - 每增强一类脚本检查，立即跑 `shape-boundary-check.ps1`。
5. 测试整理：
   - 小步调整，不做跨模块大搬迁。
   - 每次调整后跑 focused test。
6. 收尾：
   - 跑必跑门禁。
   - 跑触发式 Gurobi。
   - 更新 `daily.md` 为实际完成记录。
   - 提交时使用 Conventional Commit，提交信息说明目的、关键变更和验收结果。

## 7. 修改清单

允许修改：

1. `ospf-kotlin-framework-bpp3d/README.md`
2. `ospf-kotlin-framework-bpp3d/README_ch.md`
3. `ospf-kotlin-framework-bpp3d/daily.md`
4. `ospf-kotlin-framework-bpp3d/scripts/*.ps1`
5. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/**/*`
6. `ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/test/**/*`
7. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/main/**/*`
8. `ospf-kotlin-framework-bpp3d/bpp3d-domain-item-context/src/test/**/*`
9. `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/main/**/*`
10. `ospf-kotlin-framework-bpp3d/bpp3d-domain-packing-context/src/test/**/*`
11. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/main/**/*`
12. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/**/*`
13. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/gurobi-test/**/*`
14. `ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/**/*`

谨慎修改：

1. `ospf-kotlin-core/**/*`：仅当 PWL function lifecycle、token table 或 mechanism dump 的通用缺陷再次出现时修改。
2. `ospf-kotlin-core-plugin/**/*`：仅限 Gurobi plugin 安装或验收所需修复。
3. `E:\workspace\ospf\framework\bpp3d-interface-renderer/**/*`：仅当 renderer DTO 或显示语义变化时修改。

禁止混入：

1. `ospf-kotlin-framework-csp1d/**/*`
2. 非 BPP3D 业务模块
3. 与 PWL / shape-polymorphic / renderer / Gurobi 验收无关的格式化 churn

## 8. 验收目标

### 8.1 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

### 8.2 Gurobi 触发式验收

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

### 8.3 功能验收

1. 已开放长方体、竖直圆柱、X/Z 横向圆柱路径不回退。
2. interval-only PWL 连续半径保持 solver-selected radius、PWL metadata、actual radius validation 和 renderer actualVolume 闭环。
3. PWL function constraints 仍由 core mechanism lifecycle 展开。
4. renderer `actualVolume` 不使用 envelope volume 或 PWL volume 替代。
5. diagnostics 字段在 Gurobi result、final validation、packing snapshot 和 renderer metadata 中解释一致。
6. PWL 参数推荐有测试或 benchmark 支撑。
7. README / README_ch / daily.md 与代码能力口径一致。
8. unsupported 能力保持 guarded，不出现 silent downgrade。
9. 新增边界脚本无误报，且能阻止明确回流模式。
10. 所有提交不包含 CSP1D 或非目标模块改动。

## 9. 提交建议

建议拆分为 2 到 3 个提交：

1. `test(bpp3d): validate PWL radius Gurobi datasets`
   - 只包含 Gurobi dataset / focused 验收相关修复和记录。
2. `chore(bpp3d): tune PWL radius diagnostics and boundaries`
   - 包含诊断字段、边界脚本和 README 口径收敛。
3. `test(bpp3d): organize PWL radius regression coverage`
   - 包含测试命名、分层和局部 helper 整理。

如实际改动很小，也可以合并为一个提交，但提交信息必须说明 Gurobi 验收、PWL 参数结论和边界脚本结果。

## 10. 当前状态

最新相关提交：`95432009a`。

当前 BPP3D 生产目标已基本达到。下一轮是收尾增强与最终 Gurobi 验收，不应扩大功能范围。

## 11. 收尾增强执行记录（2026-06-12）

### 已完成

1. **Gurobi 触发式端到端验收**（5.1）
   - Gurobi core plugin install：BUILD SUCCESS
   - Focused CG 测试：55 tests, 0 failures, 1 skipped（环境性 skip）
   - Dataset suite：55 tests, 0 failures, 0 skipped，19 个 dataset 全部通过
   - PWL interval-only、multi-interval、multi-material、tight-bin envelope、horizontal support 场景全部正常
   - solver-selected radius、PWL metadata、actual radius、renderer actualVolume 闭环确认

2. **PWL 参数审计与调优基线**（5.2）
   - 默认配置：`maxSegments=8`, `relativeErrorTolerance=0.01(1%)`, `breakpointStrategy=Uniform`
   - 三种策略：Uniform / Adaptive / ErrorDriven
   - `deriveSegmentCount` 自动倍增（1→2→4→8...）到满足 tolerance 或达到 maxSegments
   - Gurobi dataset suite 在默认配置下求解稳定，无参数调优必要
   - 结论：默认参数推荐保持当前值，极端 radius interval 由 `deriveSegmentCount` 自适应

3. **PWL 诊断字段收敛**（5.3）
   - `PWLRadiusSelectionMetadata` 字段口径统一，无重复或歧义：
     - `solverRadiusSquared`：q ≈ r²（PWL 近似）
     - `actualRadiusSquared`：真实 r²
     - `pwlAbsoluteError` / `pwlRelativeError`：误差
     - `maxPWLRelativeError`：PWL 函数最大相对误差
     - `numSegments`：段数
     - `isWithinEnvelope`：是否在 envelope 内
     - `actualVolume`：π × actualRadiusSquared × h（真实体积）
     - `pwlVolume`：π × solverRadiusSquared × h（PWL 近似体积，诊断字段）
   - renderer adapter 使用 `pwlMetadata.actualVolume(...)` 而非 `pwlVolume(...)` 作为 DTO actualVolume
   - `pwlVolume` 仅作为 renderer item info 诊断字段

4. **边界脚本硬化**（5.4）
   - 新增 `PWLActualVolumeRegression`：禁止 renderer adapter 在 PWL path 使用 envelope volume 或 PWL volume 作为 actualVolume
   - 新增 `ApplicationPWLHelperRegistrationReflux`：禁止 application 层访问 PWL helper variables / selector vars / result vars，只允许 `ContinuousRadiusModelComponent.kt`
   - 4 个边界脚本全部通过
   - 无误报

5. **文档**（5.6）
   - README.md 已包含 PWL lifecycle 说明（第 86 行和第 117 行）
   - README_ch.md 已同步
   - daily.md 更新为本完成记录

### 门禁验收

| 门禁 | 结果 |
|------|------|
| generic-boundary-check.ps1 | PASS |
| shape-boundary-check.ps1 | PASS |
| geometry-boundary-check.ps1 | PASS |
| geometry-module-dry-run.ps1 | PASS（8 warnings） |
| git diff --check | PASS |
| BPP3D 全模块测试 | 56 tests, 0 failures, 0 skipped |
| Gurobi focused CG | 55 tests, 0 failures, 1 skipped |
| Gurobi dataset suite | 55 tests, 0 failures, 0 skipped |

### 总目标达成状态

BPP3D fully shape-polymorphic 生产模型已达到稳定状态：

- ✅ 长方体、竖直圆柱、X/Z 横向圆柱生产路径
- ✅ 固定半径、离散半径、solver-selected 连续半径、interval-only PWL 连续半径
- ✅ PWL 建模通过 core symbol lifecycle（`model.add(pwlSymbol)`）
- ✅ renderer 原生圆柱、actual-radius 回写、actualVolume 使用真实 π·r²·h
- ✅ Gurobi 端到端验收通过（19 dataset）
- ✅ 边界脚本硬化、门禁闭环
- ✅ 诊断字段口径统一、无歧义
- ✅ PWL 参数默认配置合理、自适应调优机制就绪

### 收尾增强补充记录（2026-06-12）

1. **极端 radius interval benchmark / regression**
   - 已在 `PWLRadiusSquaredApproximationTest` 中补充 `[1.0, 50.0]` 极端半径区间回归。
   - 默认 8 段、1% tolerance 下明确返回 `meetsTolerance=false`，不会静默放宽误差目标。
   - 高段数预算下 `deriveSegmentCount` 会继续倍增并降低最大相对误差，可满足 1% tolerance。
   - 结论：默认 8 段仍适合作为生产默认值；极端区间必须依赖 `deriveSegmentCount` 暴露诊断并提高 segment budget，不应隐式降级。

2. **PWL breakpoint 策略对比**
   - 已覆盖 Uniform / Adaptive / ErrorDriven 在 `[1.0, 50.0]` 区间上的基线对比。
   - 三种策略均保持端点覆盖、正向误差诊断和弦线 over-approximation。
   - ErrorDriven 在当前实现下不劣于 Uniform 基线；Adaptive 作为有效策略保留，不强制声明优于 Uniform。
   - 结论：默认策略继续保持 Uniform；ErrorDriven 可作为极端区间调优候选，但不改变当前生产默认。

3. **剩余可选增强**
   - 更多 production-like Gurobi dataset。
   - 如果后续真实业务数据出现极端半径分布，可新增应用层 dataset suite，而不是修改默认 PWL 策略。
