# BPP3D 下一轮改进计划

日期：2026-06-11

## 1. 目标

BPP3D 的 shape-generic 生产链路、原生圆柱 renderer、横向圆柱支撑、连续半径 solver-selected 回写和 interval-only PWL 连续半径 v1 已完成端到端闭环。下一轮不再以功能补洞为主，而是把已经可用的连续半径 PWL 能力沉淀为稳定的 framework 建模扩展点，并围绕精度、性能、数据集、renderer 诊断和边界门禁做可选增强。

总目标保持不变：已开放生产入口必须继续以 shape-generic API、真实几何、明确支撑合同和一致 metadata 为边界；不得回退到 cuboid-only fallback、外接长方体显示兼容、离散半径候选替代 interval-only PWL 路径，unsupported 能力必须继续通过 guarded contract、负例测试、文档和脚本门禁收口。

本轮优先目标：

1. 将 PWL 连续半径建模从 application 私有约束拼装收敛到 context / aggregation / model component / pipeline 风格。
2. 保持列生成 LP、final MILP、结果提取、packing analyzer 和 renderer 回写的连续半径口径一致。
3. 为后续 PWL 精度、性能和数据集增强建立可扩展入口，避免继续在 application solver 中堆建模细节。

## 2. 计划

### 阶段零：启动检查与工作边界确认

下一会话开始后先执行以下动作，避免误把其他模块的未提交改动混入 BPP3D：

1. 执行 `git status --short`，确认当前已有非 BPP3D 改动，尤其是 `ospf-kotlin-framework-csp1d` 范围的未提交文件。
2. 执行 `git status --short -- ospf-kotlin-framework-bpp3d`，确认 BPP3D 只包含本交接文档相关改动或已经干净。
3. 读取 `.rules/chore.md`、`.rules/framework-architecture.md` 和本文件，确认新增代码遵循：
   - Kotlin 注释中英双语。
   - 超过 2 个参数的函数调用使用多行命名参数。
   - framework 建模逻辑优先进入 context / aggregation / model component / pipeline。
   - application solver 只做流程编排，不继续堆变量、约束、目标和结果提取细节。
4. 不恢复 `refactor.md`。后续交接与记录全部写入 `daily.md`。

阶段零产出：

1. 明确本轮只处理 `ospf-kotlin-framework-bpp3d`。
2. 明确是否要立即提交当前文档改动；若提交，commit 只包含 `daily.md` 和 `refactor.md` 删除。

### 阶段一：PWL 建模职责审计

目标是先把 PWL 连续半径当前职责分布摸清楚，再动代码。审计不要只看文件名，要顺着列生成生命周期看完整链路。

重点入口：

1. `bpp3d-application/src/main/**/ColumnGenerationAlgorithm.kt`
   - 查看 `ColumnGenerationState`、final solve、solver info/result 回填和 `continuousRadiusSolverResults`、`pwlContinuousRadiusResults` 的传递。
   - 判断 application 是否直接理解 PWL 内部字段。
2. `bpp3d-application/src/main/**/ColumnGenerationStandardExecutors.kt`
   - 查看 PWL solver variables 创建、注册、final solve 后提取的位置。
   - 标记所有 `model.add(...)`、`model.addConstraint(...)`、token 注册、PWL result extraction。
3. `bpp3d-application/src/main/**/ContinuousRadiusSolverRegistrationPlan.kt`
   - 查看 native、PWL、productionReady、blocked 四路径分类。
   - 判断 `PWLContinuousRadiusSolverVariable` 是否应该留在 application service，还是迁到领域建模组件。
4. `bpp3d-application/src/main/**/PWLContinuousRadiusRegistration.kt`
   - 重点标记 PWL radius bound、helper variable 注册、Big-M 约束注册、`extractPWLRadiusValues` 和 `PWLExtractedRadius`。
   - 这是本轮最主要的下沉候选。
5. `bpp3d-application/src/main/**/ColumnGenerationPackingAnalyzer.kt`
   - 查看 `buildContinuousRadiusSelectionResults`、`buildPWLContinuousRadiusSelectionResults`、schema KPI 和 renderer selection 合并。
   - 判断哪些属于 application 编排，哪些属于连续半径结果提取组件。
6. `bpp3d-domain-item-context/src/main/**/CylinderShapeContract.kt`
   - 查看 `ContinuousCylinderRadiusSolverPrototype`、`PWLRadiusSelectionMetadata`、`CylinderRadiusSelectionResult`、`withPWLSolverSelectedRadius`。
   - 判断 metadata 和 domain contract 是否已经足够作为组件输入/输出。
7. `bpp3d-domain-packing-context/src/main/**/PackingRendererAdapter.kt`
   - 只检查 renderer actualVolume / pwlVolume / diagnostics 是否依赖 application 私有类型。
   - 不应把 renderer 回写逻辑迁回 application。
8. `bpp3d-infrastructure/src/main/**/PWLRadiusApproximationConfig.kt`
9. `bpp3d-infrastructure/src/main/**/PWLRadiusSquaredApproximation.kt`
10. `bpp3d-infrastructure/src/main/**/ConservativeRadiusEnvelope.kt`
   - 确认这些仍作为低层算法与几何工具，不承担 application 编排职责。

阶段一产出：

1. 在 `daily.md` 追加“实际审计结果”小节，记录每个职责的当前位置、建议归属和迁移优先级。
2. 明确哪些内容必须迁移，哪些暂时保留但要加门禁。
3. 不在审计 commit 中做大规模重构。

### 阶段二：设计连续半径 PWL 建模组件

目标是确定代码落点，先设计边界再迁移。建议优先选择 `bpp3d-domain-item-context` 或新建/复用与 continuous radius 更贴近的 domain context；如果组件必须依赖 `LinearMetaModel`、core PWL function 或 solver value extraction，可放在领域 context 的 service/model component 中，并让 infrastructure 继续只提供纯计算工具。

建议组件拆分：

1. `ContinuousRadiusModelComponent`
   - 持有 prototypes、native solver variables、PWL solver variables、registration plan。
   - 负责 register 到 `LinearMetaModel<InfraNumber>`。
   - 负责从 solver result 中 extract native/PWL radius results。
2. `PWLContinuousRadiusModelComponent`
   - 专门封装 PWL radius variable、PWL function、helper variables、Big-M constraints、bound constraints。
   - 对外只暴露 `register(...)`、`extract(...)`、`info()` 或类似稳定接口。
3. `ContinuousRadiusRegistrationPlan`
   - 保留四路径分类：native、PWL、productionReady、blocked。
   - 输出 diagnostics，但不要求 application 拼接 PWL 细节。
4. `ContinuousRadiusSelectionExtractor`
   - 从 native result 和 PWL result 构建 `CylinderRadiusSelectionResult`。
   - application analyzer 只接收 selection results，不再重建 PWL metadata。
5. `PWLRadiusModelingConfig`
   - 可以暂时复用 `PWLRadiusApproximationConfig`。
   - 如果要新增业务级配置，不要污染低层 approximation config。

设计约束：

1. 不改变 public DTO 或 CSV 语义，除非后续明确做 dataset/renderer 增强。
2. 不改变 `PWLRadiusSquaredApproximation` 的数学含义：仍是 `q ≈ r²`。
3. 不改变 renderer actualVolume 口径：仍用 solver-selected radius 的真实 `π*r²*h`。
4. 不引入离散半径候选生成。
5. 不把 `Double` 裸值扩散到领域模型；转换集中在 adapter、registration 或 extraction 边界。

阶段二产出：

1. 在 `daily.md` 记录最终组件命名、文件落点和迁移顺序。
2. 若需要，先加空骨架或最小组件测试，再迁移实现。
3. 确认 import 排序、KDoc 和中英双语注释符合 `.rules/chore.md`。

### 阶段三：迁移 PWL 注册链路

目标是把 `PWLContinuousRadiusRegistration.kt` 中的建模细节从 application service 下沉，application 只调用领域组件。

迁移顺序：

1. 先迁移数据结构：
   - `PWLContinuousRadiusSolverVariable`
   - `PWLExtractedRadius`
   - 如合适，`ContinuousRadiusSolverVariable`
   - 如合适，`ContinuousRadiusSolverVariableRegistrationPlan`
2. 再迁移变量创建：
   - `continuousRadiusSolverVariables(...)`
   - `pwlContinuousRadiusSolverVariables(...)`
3. 再迁移注册：
   - radius variable `model.add(...)`
   - lower / upper bound constraints
   - PWL helper variables
   - PWL auxiliary tokens
   - Big-M select-one、segment bounds、segment equality constraints
4. 最后迁移 diagnostics：
   - registration plan info
   - PWL constraint descriptions
   - PWL model scale metrics（如本轮一并做性能 KPI）

迁移后 application 层应变成：

1. 创建 continuous radius component。
2. 调用 component register。
3. solver 完成后调用 component extract。
4. 把 selection results 或 extracted results 传给 analyzer/renderer。

阶段三必须保持：

1. 原有 PWL 正例、负例、Gurobi focused 行为不变。
2. info key 如果已被测试或 README 依赖，不随意改名；若确实改名，同步测试和文档。
3. `UnivariateLinearPiecewiseFunction` 的 Big-M 语义不改变。

### 阶段四：迁移 PWL 提取与 renderer selection 链路

目标是让 `ColumnGenerationPackingAnalyzer.kt` 不再重建 PWL metadata 细节。

具体做法：

1. 把 `buildPWLContinuousRadiusSelectionResults(...)` 的核心逻辑迁到连续半径结果提取组件。
2. analyzer 只做：
   - 获取 native selection results。
   - 获取 PWL selection results。
   - 合并 selection results。
   - 调用 `PackingRendererAdapter.toSchema(...)`。
3. `PWLRadiusSelectionMetadata` 的构建仍由 domain item contract 或连续半径提取组件完成。
4. renderer adapter 保持只消费 `CylinderRadiusSelectionResult`，不依赖 application 私有 extracted type。

阶段四产出：

1. application analyzer 中 PWL 分支明显变薄。
2. PWL metadata 与 renderer info 保持原口径。
3. `actualVolume`、`pwlVolume`、`pwlAbsoluteError`、`pwlRelativeError`、`numSegments`、`isWithinEnvelope` 仍可追踪。

### 阶段五：扩展点测试与回归测试

目标是证明新的建模组件不是只为当前 application 写死。

建议新增测试：

1. 组件注册测试：
   - 给定 interval-only prototype 和 PWL config。
   - 注册到 `LinearMetaModel<InfraNumber>`。
   - 断言 radius variable、result variable、selector variables、bound constraints、segment constraints 都存在。
2. 配置注入测试：
   - 使用不同 `PWLRadiusApproximationConfig`。
   - 断言 segment 数、breakpoints、diagnostics 随配置变化。
3. 提取测试：
   - 构造 solver result map。
   - 断言 `PWLExtractedRadius`、`PWLRadiusSelectionMetadata` 和 `CylinderRadiusSelectionResult` 一致。
4. application 薄编排测试：
   - 断言 application 不需要直接调用 PWL constraint registration。
   - 可以通过脚本门禁辅助完成。
5. 负例保持：
   - Big-M 边界。
   - envelope 溢出。
   - PWL 误差超限。
   - silent downgrade 防护。
   - discrete fallback 防护。

优先复用现有测试：

1. `PWLRadiusSquaredApproximationTest`
2. `ConservativeRadiusEnvelopeTest`
3. `PWLContinuousRadiusNegativeTest`
4. `PWLContinuousRadiusIntegrationNegativeTest`
5. `GurobiColumnGenerationTest`

### 阶段六：边界脚本硬化

目标是把本轮架构收口变成自动门禁。

建议在 `scripts/shape-boundary-check.ps1` 或更合适的边界脚本中增加检查：

1. `PWLApplicationConstraintRegistrationReflux`
   - 检测 application solver 中新增大段 `model.addConstraint` + PWL/Big-M/segment 关键词组合。
   - 允许领域组件文件或明确 allowlist。
2. `PWLDiscreteFallbackReflux`
   - 检测 PWL continuous radius 路径出现 discrete candidate、fallback radius list、silent downgrade 等关键词组合。
3. `BoundingCuboidRendererFallbackReflux`
   - 继续禁止 renderer DTO 或 adapter 恢复 `BoundingCuboid` fallback。
4. `ContinuousRadiusUnsupportedRegression`
   - 检测 interval-only PWL 已开放路径被重新标为 unsupported 或 gap-only。
5. `DeletedCuboidCompatAliasReflux`
   - 继续保留已删除 compat alias 回流检查。

脚本要求：

1. 新增检查必须有清晰错误信息。
2. allowlist 必须具体到文件或稳定标识，不能大范围放行。
3. 文档中记录新增检查名和覆盖边界。

### 阶段七：PWL 精度与性能增强

此阶段在架构迁移稳定后执行，不要和核心迁移混在同一个 commit。

精度增强：

1. 增加“按误差预算推导 segment 数”的入口。
2. 当 `maxSegments` 内无法满足 `relativeErrorTolerance` 时：
   - 返回明确 diagnostics。
   - 不静默降低精度目标。
   - 不改用离散候选。
3. 补充极端区间测试：
   - rMin 接近 0 但仍为正数。
   - rMax / rMin 跨度大。
   - custom breakpoints 覆盖不足。
   - custom breakpoints 非单调。
   - 单位换算后边界接近 tolerance。

性能增强：

1. 记录每个 PWL prototype 的：
   - segment 数。
   - selector variable 数。
   - helper variable 数。
   - constraint 数。
   - max relative error。
2. 在 schema KPI、solver info 或 diagnostics 中输出汇总：
   - total PWL prototypes。
   - total PWL selectors。
   - total PWL constraints。
   - max / avg segment count。
3. 大规模场景下允许配置更少 segment，但必须记录为显式 config decision。

### 阶段八：Gurobi dataset 与 renderer 诊断增强

此阶段只在前面迁移和性能指标稳定后执行。

建议新增 Gurobi dataset：

1. 单个 interval-only vertical cylinder。
2. 多个 interval-only vertical cylinders。
3. mixed cuboid + vertical cylinder + horizontal cylinder。
4. tight bin，验证 conservative envelope 不越界。
5. support-sensitive horizontal cylinder，验证支撑和 envelope 同时成立。
6. 多 material / 多 demand 场景，验证 program demand、material packing、renderer metadata 口径一致。

每个 dataset 需要记录：

1. 文件名和业务意图。
2. 是否应通过。
3. 预期 metadata keys。
4. 预期 PWL diagnostics。
5. actualVolume 与 pwlVolume 的关系。

renderer 侧仅在显示语义变化时修改。若只是 BPP3D 输出更多 info 且 renderer 已能显示 metadata，不需要修改外部 renderer。

## 3. 事项

### 必做事项

1. **确认工作区边界**
   - 执行 `git status --short`。
   - 确认非 BPP3D 改动不纳入本轮 commit。
   - 确认 `refactor.md` 已删除且不恢复。
   - 若本轮从当前状态开始提交文档，先单独提交文档交接改动。

2. **完成 PWL 职责审计**
   - 用 `rg -n "PWL|continuousRadius|UnivariateLinearPiecewiseFunction|model.addConstraint|registerPWL|extractPWL"` 扫描 BPP3D。
   - 按“变量创建、约束注册、token 注册、结果提取、diagnostics、renderer selection、KPI”分类。
   - 在 `daily.md` 追加审计结果，写清楚每类职责当前文件、目标文件、是否迁移。
   - 审计阶段不修改生产代码。

3. **确定组件落点**
   - 选择连续半径 PWL 建模组件所在模块。
   - 推荐优先考虑 domain context / domain service；低层数学和 envelope 保持在 infrastructure。
   - 明确 application、domain、infrastructure 三层依赖方向，避免 domain 反向依赖 application。
   - 在 `daily.md` 记录最终文件规划。

4. **新增连续半径建模组件骨架**
   - 新增 component/context 类，包含 prototypes、config、registration plan、PWL variables。
   - 暴露最小接口：`register(...)`、`extract(...)`、`info()` 或等价方法。
   - 添加 KDoc，公共类/函数按规则写 `@property`、`@param`、`@return`。
   - 函数参数超过 2 个时使用多行命名参数。

5. **迁移 PWL variable 和 registration plan**
   - 将 `PWLContinuousRadiusSolverVariable` 移到新组件或其模型文件。
   - 将 PWL variable 创建逻辑迁出 application service。
   - 保持 native、PWL、productionReady、blocked 四路径互斥分类。
   - 确保 existing info keys 不破坏现有测试。

6. **迁移 PWL 约束注册**
   - 迁移 radius lower / upper bound 约束。
   - 迁移 helper variables 注册。
   - 迁移 auxiliary tokens 注册。
   - 迁移 Big-M select-one、segment lower / upper、segment equality upper / lower 约束。
   - 保持 Big-M 计算逻辑和约束命名稳定，除非同步更新测试。

7. **迁移 PWL result extraction**
   - 迁移 `extractPWLRadiusValues` 和 `PWLExtractedRadius`。
   - 保持 `actualRadiusSquared = r*r`，`solverRadiusSquared = q`。
   - 保持 relative error、absolute error、envelope status 计算口径。
   - 提供 selection result 构建入口，减少 analyzer 中的 PWL 私有逻辑。

8. **瘦身 application executor**
   - 修改 `ColumnGenerationStandardExecutors.kt`，让它只调用 continuous radius component。
   - executor 不应直接知道 PWL segment constraints 的内部结构。
   - executor 可以保留流程编排、错误处理和状态挂载。

9. **瘦身 packing analyzer**
   - 修改 `ColumnGenerationPackingAnalyzer.kt`，让 PWL selection results 由组件或 extractor 提供。
   - analyzer 只合并 selection results 并传给 `PackingRendererAdapter`。
   - 保留 schema KPI，但 KPI 数据来源改为 component diagnostics。

10. **保持 renderer adapter 口径**
    - 检查 `PackingRendererAdapter.kt`。
    - 确认 PWL path 仍使用 `pwlMetadata.actualVolume()`。
    - 确认非 PWL path 仍使用 solver-selected radius。
    - 确认 renderer info 仍包含 PWL volume/error/segment/envelope 诊断。

11. **补充组件级测试**
    - 新增或调整测试，覆盖 component register。
    - 断言 radius variable、PWL result variable、selector variables 和 constraints 注册成功。
    - 断言不同 PWL config 影响 segment 数和 diagnostics。
    - 断言 extraction 结果能构建 `CylinderRadiusSelectionResult`。

12. **保留并复跑既有 PWL 测试**
    - 复跑 `PWLRadiusSquaredApproximationTest`。
    - 复跑 `ConservativeRadiusEnvelopeTest`。
    - 复跑 `PWLContinuousRadiusNegativeTest`。
    - 复跑 `PWLContinuousRadiusIntegrationNegativeTest`。
    - 若测试名或包路径变化，更新 Maven 指定测试命令或记录实际执行方式。

13. **补强边界脚本**
    - 在脚本中新增 PWL application 约束回流检查。
    - 新增 PWL discrete fallback 回流检查。
    - 保留 `BoundingCuboid` fallback 禁止项。
    - 保留 deleted cuboid compat alias 回流检查。
    - 每个检查都提供明确失败消息和具体修复方向。

14. **执行 BPP3D 必跑门禁**
    - 执行 4 个 PowerShell 边界脚本。
    - 执行 `git diff --check -- ospf-kotlin-framework-bpp3d`。
    - 执行 `mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true`。
    - 记录 warning，不把既有 warning 当成失败，但新增 warning 要判断是否由本轮引入。

15. **执行触发式 Gurobi 验收**
    - 本轮若修改 application、solver registration、PWL extraction 或 dataset，必须执行 Gurobi focused。
    - 若新增或修改 dataset suite，必须执行 Gurobi dataset suite。
    - 记录测试数量、失败数、skip 数和环境性 skip 原因。

16. **更新文档**
    - 更新 README.md / README_ch.md，说明 PWL 建模组件和扩展点。
    - 更新 `daily.md`，写清实际完成项、未完成项、验收结果和下一轮建议。
    - 不再新增 `refactor.md`。

17. **提交拆分**
    - 按第 5 节 commit 拆解提交。
    - 每个 commit 只包含对应范围。
    - BPP3D commit 不混入外部 renderer 或 CSP1D 改动。

### 可选增强事项

1. **误差预算推导**
   - 增加按 `relativeErrorTolerance` 推导 segment 数的策略。
   - 若 `maxSegments` 内无法满足误差，输出 diagnostics。
   - 增加正负例测试，确认不静默放宽误差。

2. **极端半径测试**
   - 覆盖 rMin 接近 0 但大于 0。
   - 覆盖 rMax / rMin 跨度大。
   - 覆盖 custom breakpoints 未覆盖区间。
   - 覆盖 custom breakpoints 非单调。
   - 覆盖单位换算后接近 tolerance 的边界。

3. **性能 KPI**
   - 输出 PWL prototype 数。
   - 输出 selector variable 总数。
   - 输出 PWL constraint 总数。
   - 输出 segment max / avg。
   - 在 Gurobi result 或 schema KPI 中记录。

4. **Gurobi dataset 扩展**
   - 增加单 interval-only vertical cylinder。
   - 增加多 interval-only cylinders。
   - 增加 mixed shape dataset。
   - 增加 tight bin + conservative envelope dataset。
   - 增加 support-sensitive horizontal cylinder dataset。

5. **renderer fixture 扩展**
   - 仅在显示语义变化时修改外部 renderer。
   - 增加 PWL 圆柱 fixture。
   - 增加 mixed shape fixture。
   - 增加 extreme radius fixture。
   - 增加 unsupported guard fixture。

6. **core PWL 注册能力评估**
   - 若 core 后续提供可直接注册到 `LinearMetaModel` 的 PWL mechanism，评估替换手动 Big-M 注册。
   - 替换前必须保证 info key、约束语义、Gurobi 结果和 renderer 回写不退化。

## 4. 修改清单

### 允许修改范围

1. `bpp3d-infrastructure/src/main/**/*`
2. `bpp3d-infrastructure/src/test/**/*`
3. `bpp3d-infrastructure/src/test/resources/**/*`
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
19. `daily.md`

### 外部 renderer 修改范围

外部工程 `E:\workspace\ospf\framework\bpp3d-interface-renderer` 已完成原生圆柱支持并提交。下一轮仅在 renderer DTO、fixture、adapter 或显示语义变化时修改，修改后必须单独提交，不得混入 BPP3D commit。

### 不允许事项

1. 不允许重新引入 `refactor.md` 作为主交接文档。
2. 不允许将 interval-only PWL continuous radius 改成离散半径候选生成。
3. 不允许恢复 `BoundingCuboid` renderer fallback。
4. 不允许把已删除的 cuboid-only compat alias 加回生产路径。
5. 不允许把 unsupported 能力静默降级为近似可用路径。
6. 不允许混入 `ospf-kotlin-framework-csp1d` 或其他非 BPP3D 改动。

## 5. Commit 拆解

建议按以下 commit 拆分，便于审查和回滚。

### Commit 1：PWL 架构审计与接口落点

Header 建议：

```text
chore(bpp3d): document PWL modeling architecture boundary
```

内容：

1. 更新 `daily.md` 中实际审计结果。
2. 如需要，补充 README 中 PWL 建模扩展点说明。
3. 不做大规模代码迁移。

验收：

1. `git diff --check -- ospf-kotlin-framework-bpp3d`
2. 文档口径与当前代码一致。

### Commit 2：下沉 PWL 注册与提取组件

Header 建议：

```text
refactor(bpp3d): move PWL radius modeling into domain component
```

内容：

1. 新增或调整连续半径 context / aggregation / model component / pipeline。
2. 迁移 PWL variable、helper variable、bound、Big-M 约束注册逻辑。
3. 迁移 PWL result extraction 和 diagnostics 组装。
4. application solver 仅保留编排调用。

验收：

1. BPP3D 全模块测试通过。
2. PWL 正负例测试通过。
3. Gurobi focused 测试通过。
4. 边界脚本通过。

### Commit 3：PWL 扩展点与边界门禁

Header 建议：

```text
test(bpp3d): cover PWL radius extension boundaries
```

内容：

1. 增加 fake / 最小扩展点测试。
2. 增加 application 层 PWL 约束回流脚本检查。
3. 增加 silent downgrade、discrete fallback、unsupported bypass、BoundingCuboid fallback 检查。

验收：

1. 4 个 BPP3D 边界脚本全部通过。
2. PWL 相关负例测试全部通过。

### Commit 4：PWL 精度与性能增强

Header 建议：

```text
feat(bpp3d): add PWL radius accuracy and performance diagnostics
```

内容：

1. 误差预算推导或 segment 策略增强。
2. PWL 模型规模 KPI 与 diagnostics。
3. 极端半径测试。

验收：

1. 基础设施测试通过。
2. BPP3D 全模块测试通过。
3. Gurobi focused 测试通过。

### Commit 5：Gurobi dataset 与 renderer fixture 增强

Header 建议：

```text
test(bpp3d): expand PWL continuous radius datasets
```

内容：

1. 增加 interval-only continuous radius dataset suite 样例。
2. 如触发 renderer 显示语义变化，单独更新外部 renderer fixture 并独立提交。
3. 更新 README.md、README_ch.md、daily.md。

验收：

1. Gurobi dataset suite 通过。
2. 若修改 renderer，外部 renderer build、typecheck、Rust 检查、Tauri build 和人工视觉验收通过。

## 6. 验收标准

### 必跑门禁

```powershell
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/generic-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/shape-boundary-check.ps1 -ProjectRoot ospf-kotlin-framework-bpp3d
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1 -ProjectRoot .
pwsh.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1 -ProjectRoot .
git diff --check -- ospf-kotlin-framework-bpp3d
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml test -Dgpg.skip=true
```

### 触发式 Gurobi 验收

修改 application、CSV、shape spec、depth boundary、continuous radius、PWL、solver 注册或 result extraction 时执行：

```powershell
mvn --% -f ospf-kotlin-core-plugin/pom.xml -pl ospf-kotlin-core-plugin-gurobi -am install -DskipTests -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false -Dbpp3d.gurobi.cg.test.enabled=true test -Dgpg.skip=true
mvn --% -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dbpp3d.gurobi.cg.test.enabled=true -Dbpp3d.gurobi.dataset.suite.enabled=true -Dbpp3d.gurobi.dataset.suite.dir=ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi -Dtest=GurobiColumnGenerationTest -Dsurefire.failIfNoSpecifiedTests=false test -Dgpg.skip=true
```

### 触发式 renderer 验收

修改 renderer DTO、renderer fixture、packing renderer adapter 或显示语义时执行：

```powershell
cd E:\workspace\ospf\framework\bpp3d-interface-renderer
npm run build
npx vue-tsc --noEmit
cargo check
cargo test
npx tauri build
```

必要时继续执行人工视觉验收。

### 功能与架构验收

1. 已开放的长方体、竖直圆柱、X/Z 横向圆柱生产路径不回退。
2. PWL continuous radius 必须继续使用真实连续半径 solver 变量和 PWL `q ≈ r²`，不得替换为离散半径候选。
3. `actualVolume` 必须继续使用 solver-selected radius 的真实 `π*r²*h`。
4. `pwlVolume`、PWL error、segment count、envelope status 必须继续可诊断。
5. 几何 envelope 必须保守，placement、碰撞和支撑覆盖不得因 solver-selected radius 变大而失效。
6. application solver 不应长期承载 PWL 变量和约束细节；新增建模能力应优先进入 context / aggregation / model component / pipeline。
7. 固定半径、离散半径、selected continuous radius、interval-only PWL continuous radius 的 metadata 优先级和互斥协议必须清晰。
8. unsupported 范围必须明确拒绝或 guarded，不允许静默降级。
9. CSV、application DTO、program demand、material packing、Gurobi result、final packing 和 renderer metadata 解释一致。
10. README.md、README_ch.md、daily.md 与代码能力口径一致。
11. BPP3D 改动独立提交，不混入非 BPP3D 或未触发的外部 renderer 改动。

## 7. 当前状态

1. BPP3D 功能目标已经完成，下一轮是架构收口和增强项。
2. `refactor.md` 已移除，后续交接以 `daily.md` 为主。
3. 外部 renderer 原生圆柱支持已完成并独立提交。
4. 当前仓库中存在非 BPP3D 的 CSP1D 未提交改动，后续 BPP3D commit 不得混入。

## 8. 实际审计结果

日期：2026-06-11，按阶段一要求逐文件审计 PWL 连续半径建模职责分布。

### 8.1 阶段零产出确认

- `git status --short` 确认：BPP3D 范围仅有 `M daily.md` 和 `D refactor.md`；CSP1D 存在多项未提交改动（`Csp1dMilpSolver.kt`、`daily.md` 及多个新增聚合/服务文件），本轮不纳入 BPP3D commit。
- `refactor.md` 已删除（`D` 状态），文件系统不存在，不恢复。

### 8.2 逐文件审计

#### (1) ColumnGenerationAlgorithm.kt — application 算法编排层

**当前位置**：`bpp3d-application/src/main/.../application/service/`

**职责现状**：
- `ColumnGenerationState` 持有 `continuousRadiusSolverPrototypes`、`continuousRadiusSolverResults`（`Map<String, InfraNumber>`）和 `pwlContinuousRadiusResults`（`Map<String, Map<String, InfraNumber>>`），后者标注为 opaque map，application 不解析内部 key。
- final solve 从 `ColumnGenerationFinalResult` 获取 native results（通过 info map 反序列化）和 PWL results（直接传递 opaque map）。
- `ColumnGenerationResult` 将两条路径的结果透传给 analyzer。
- native 路径存在不必要的序列化往返（`InfraNumber → String → Double`），PWL 路径无此问题但内部使用 8 个硬编码 string key 作为隐式契约。

**审计结论**：application 层本身不直接理解 PWL 内部字段，仅做 opaque pass-through。**无需迁移**，但 string-key 契约脆弱性需在后续组件化时消除。

**迁移优先级**：低（保持现状，待组件化后自动改善）。

#### (2) ColumnGenerationStandardExecutors.kt — application 执行器层

**当前位置**：`bpp3d-application/src/main/.../application/service/`

**职责现状**：
- `buildRmpArtifacts()` 调用 `pwlContinuousRadiusSolverVariables()` 创建 PWL 变量，调用 `registerPWLContinuousRadiusVariables()` 注册到模型。
- `finalSolver()` 重复同样流程创建 final MILP 的 PWL 变量和注册。
- `extractContinuousRadiusSolverResults()` 从 model.tokens 提取 native 结果，写入 info map。
- `extractPWLRadiusValuesFromModel()` 从 model.tokens 提取 PWL 结果（r 值、q 值），计算 actualRSquared、pwlAbsoluteError、pwlRelativeError、isWithinEnvelope、numSegments 等诊断指标，返回 `Map<String, Map<String, InfraNumber>>`。
- 深度依赖 `PWLContinuousRadiusSolverVariable`（`internal` 类型）、`UnivariateLinearPiecewiseFunction`、`PWLRadiusSquaredApproximation`、`ConservativeRadiusEnvelope`。

**审计结论**：执行器直接理解 PWL 内部结构，是**本轮最主要的瘦身目标**。变量创建和注册应委托给连续半径建模组件，提取应委托给结果提取组件。

**迁移优先级**：高。

#### (3) ContinuousRadiusSolverRegistrationPlan.kt — application 注册计划层

**当前位置**：`bpp3d-application/src/main/.../application/service/`

**职责现状**：
- 包含 5 个声明：`ContinuousRadiusSolverVariableRegistrationPlan`（诊断聚合）、`continuousRadiusSolverVariableRegistrationPlan()`（四路径分类工厂）、`ContinuousRadiusSolverVariable`（native 变量对）、`PWLContinuousRadiusSolverVariable`（PWL 变量聚合体，持有 prototype + RealVar + UnivariateLinearPiecewiseFunction + approximation + envelope + config）、`continuousRadiusSolverVariables()` 和 `pwlContinuousRadiusSolverVariables()`（变量创建工厂）。
- 四路径分类：native（`isSolverRegisterable`）> PWL（`isPWLRegisterable && !isSolverRegisterable`）> blocked；productionReady 独立跟踪。
- `PWLContinuousRadiusSolverVariable` 依赖 core solver 类型（`UnivariateLinearPiecewiseFunction`、`RealVar`、`LinearPolynomial`），移入 domain 会引入不当依赖。

**审计结论**：`PWLContinuousRadiusSolverVariable` 是 solver 构建关注点，应留在 application 或迁入领域 context/service/model component（若该组件允许依赖 core solver）。四路径分类和变量创建逻辑可迁入 `ContinuousRadiusRegistrationPlan` 组件。

**迁移优先级**：中高。建议将整个文件迁入连续半径建模组件（保留在 application 或 domain-service 中，视组件落点而定）。

#### (4) PWLContinuousRadiusRegistration.kt — application PWL 注册层（本轮最主要下沉候选）

**当前位置**：`bpp3d-application/src/main/.../application/service/`，309 行。

**职责现状**：
- `registerPWLContinuousRadiusVariables()`（lines 35-94）：`model.add(r)`、radius lower/upper bound 约束、helper variable 注册循环、auxiliary token 注册、委托 `registerPWLFunctionConstraints()`。
- `registerPWLFunctionConstraints()`（lines 103-218）：Big-M 值计算 `(rMax - rMin + rMax²) × 2.0`、select-one 约束、每段 4 个 Big-M 约束（seg_lb、seg_ub、seg_eq_ub、seg_eq_lb）。对 N 段总计 `3 + 4N` 个约束。
- `extractPWLRadiusValues()`（lines 228-250）：从预构建 resultMap 提取 r/q 值，计算误差和 envelope 状态，返回 `PWLExtractedRadius` 列表。
- `PWLExtractedRadius`（lines 266-308）：领域值对象，含 `actualVolume()`、`pwlVolume()`、`info()` 方法。

**审计结论**：约 220/309 行（71%）是领域建模逻辑（约束构造、Big-M 计算、结果提取、值对象），仅 ~90 行是合法的编排胶水。**必须迁移**。具体归属：

| 职责 | 当前位置 | 建议归属 | 优先级 |
|------|---------|---------|-------|
| radius bound 约束构造 | application service | `PWLContinuousRadiusModelComponent` 或 constraint builder | 高 |
| Big-M 值计算 | application service | `PWLContinuousRadiusModelComponent` 或 Big-M strategy | 高 |
| select-one + 分段 Big-M 约束 | application service | `PWLContinuousRadiusModelComponent` | 高 |
| helper variable + token 注册 | application service | `PWLContinuousRadiusModelComponent` | 高 |
| `extractPWLRadiusValues()` | application service | `ContinuousRadiusSelectionExtractor` | 高 |
| `PWLExtractedRadius` 值对象 | application service | domain-item-context 或 infrastructure | 中 |

**迁移优先级**：最高。

#### (5) ColumnGenerationPackingAnalyzer.kt — application 分析器层

**当前位置**：`bpp3d-application/src/main/.../application/service/`，289 行。

**职责现状**：
- `buildContinuousRadiusSelectionResults()`（lines 182-196）：native 路径，遍历 prototypes，查找 solver 值，调用 `withSolverSelectedRadius()` 构建 `CylinderRadiusSelectionResult`。
- `buildPWLContinuousRadiusSelectionResults()`（lines 206-240）：PWL 路径，从 opaque map 提取 8 个 string key，构建 `PWLRadiusSelectionMetadata`，调用 `withPWLSolverSelectedRadius()`。Boolean 通过 `> 0.5` 解码，Int 通过 `.toDouble().toInt()` 解码。
- `analyze()` 方法合并两条路径结果（简单 list concatenation），传给 `PackingRendererAdapter.toSchema()`。
- schema KPI 组装引用 `continuousRadiusSolverVariableRegistrationPlan().info()` 产出 10 个诊断 key。

**审计结论**：`buildContinuousRadiusSelectionResults` 和 `buildPWLContinuousRadiusSelectionResults` 是纯提取函数（无副作用、无状态依赖），应迁入 `ContinuousRadiusSelectionExtractor` 组件。analyzer 应只保留编排（获取结果、合并、传给 renderer）。string-key 契约在组件化后应改为类型化接口。

**迁移优先级**：中高。

#### (6) CylinderShapeContract.kt — domain-item-context 领域契约层

**当前位置**：`bpp3d-domain-item-context/src/main/.../domain/item/model/`

**职责现状**：
- `ContinuousCylinderRadiusSolverPrototype`：领域原型值对象，含 `isSolverRegisterable`、`isPWLRegisterable`、`isProductionReady` 判定属性。仅依赖 infrastructure 原始类型和 math 库。
- `PWLRadiusSelectionMetadata`：PWL 诊断元数据，含 `actualVolume()` 和 `pwlVolume()` 方法。仅使用 `InfraNumber`。
- `CylinderRadiusSelectionResult`：统一输出类型，可选 `pwlMetadata`。
- `withSolverSelectedRadius()` / `withPWLSolverSelectedRadius()`：对称工厂方法。

**审计结论**：metadata 和 domain contract 已经足够作为组件输入/输出。零依赖 application 或 solver 类型。**无需迁移**，可作为新组件的稳定接口层。

**迁移优先级**：无需迁移。

#### (7) PackingRendererAdapter.kt — domain-packing-context 领域服务层

**当前位置**：`bpp3d-domain-packing-context/src/main/.../domain/packing/service/`

**职责现状**：
- actualVolume 分支：PWL 路径用 `pwlMetadata.actualVolume()`（真实 π·r²·h），非 PWL 路径用 solver-selected radius 直接算。
- PWL 诊断 info map（7 个 key）：pwl_volume、pwl_absolute_error、pwl_relative_error、pwl_max_relative_error、pwl_num_segments、pwl_within_envelope、pwl_selection_source。
- 仅依赖 domain-model 契约类型和 infrastructure DTO，零依赖 application 私有类型。

**审计结论**：renderer adapter 正确属于 domain-packing-context，不依赖 application 私有类型。**不应迁回 application**，当前归属正确。

**迁移优先级**：无需迁移。

#### (8) PWLRadiusApproximationConfig.kt — infrastructure 配置层

**当前位置**：`bpp3d-infrastructure/src/main/.../infrastructure/`，37 行。

**职责现状**：纯配置值对象（maxSegments、relativeErrorTolerance、breakpointStrategy、customBreakpoints、enableDebugInfo），零 import。

**审计结论**：低层算法配置工具，不承担编排职责。**无需迁移**。可暂时复用为 `PWLRadiusModelingConfig`。

#### (9) PWLRadiusSquaredApproximation.kt — infrastructure 算法层

**当前位置**：`bpp3d-infrastructure/src/main/.../infrastructure/`，307 行。

**职责现状**：纯数学算法（PWL 分段线性逼近 r²），含 Uniform/Adaptive/ErrorDriven 三种 breakpoint 策略，零 import。

**审计结论**：低层数学工具，不承担编排职责。**无需迁移**。

#### (10) ConservativeRadiusEnvelope.kt — infrastructure 几何层

**当前位置**：`bpp3d-infrastructure/src/main/.../infrastructure/`，151 行。

**职责现状**：纯几何工具（conservative/real footprint、bounding、envelope 验证），仅 1 个 math 库 import。

**审计结论**：低层几何工具，不承担编排职责。**无需迁移**。

### 8.3 职责分类汇总

| 职责类别 | 当前文件 | 目标归属 | 是否迁移 |
|---------|---------|---------|---------|
| 变量创建（native + PWL） | ContinuousRadiusSolverRegistrationPlan.kt | ContinuousRadiusModelComponent | 是 |
| 约束注册（radius bound + Big-M + segment） | PWLContinuousRadiusRegistration.kt | PWLContinuousRadiusModelComponent | 是 |
| Token 注册（auxiliary tokens） | PWLContinuousRadiusRegistration.kt | PWLContinuousRadiusModelComponent | 是 |
| 结果提取（extractPWLRadiusValues + extractPWLRadiusValuesFromModel） | PWLContinuousRadiusRegistration.kt + ColumnGenerationStandardExecutors.kt | ContinuousRadiusSelectionExtractor | 是 |
| Selection result 构建 | ColumnGenerationPackingAnalyzer.kt | ContinuousRadiusSelectionExtractor | 是 |
| 四路径分类 + diagnostics | ContinuousRadiusSolverRegistrationPlan.kt | ContinuousRadiusRegistrationPlan | 是 |
| PWLExtractedRadius 值对象 | PWLContinuousRadiusRegistration.kt | domain-item-context 或 infrastructure | 是 |
| Big-M 值计算策略 | PWLContinuousRadiusRegistration.kt | PWLContinuousRadiusModelComponent | 是 |
| Opaque map 传递 | ColumnGenerationAlgorithm.kt | 保持现状（组件化后改为类型化） | 否（自动改善） |
| 执行器编排 | ColumnGenerationStandardExecutors.kt | 保持编排，委托组件 | 瘦身 |
| Analyzer 编排 | ColumnGenerationPackingAnalyzer.kt | 保持编排，消费 selection results | 瘦身 |
| 领域原型 + metadata | CylinderShapeContract.kt | 保持现状 | 否 |
| Renderer adapter | PackingRendererAdapter.kt | 保持现状 | 否 |
| PWL 配置 | PWLRadiusApproximationConfig.kt | 保持现状 | 否 |
| PWL 数学算法 | PWLRadiusSquaredApproximation.kt | 保持现状 | 否 |
| Envelope 几何 | ConservativeRadiusEnvelope.kt | 保持现状 | 否 |

### 8.4 必须迁移项

1. `PWLContinuousRadiusRegistration.kt` 整体（309 行）——变量注册、约束注册、Big-M 建模、结果提取、值对象。
2. `ContinuousRadiusSolverRegistrationPlan.kt` 中的变量创建工厂和四路径分类逻辑。
3. `ColumnGenerationPackingAnalyzer.kt` 中的 `buildContinuousRadiusSelectionResults` 和 `buildPWLContinuousRadiusSelectionResults`。
4. `ColumnGenerationStandardExecutors.kt` 中的 `extractPWLRadiusValuesFromModel`。

### 8.5 暂时保留但需加门禁项

1. `ColumnGenerationAlgorithm.kt` 的 opaque map pass-through——组件化后应改为类型化传递。
2. native 路径 info map 序列化往返——组件化后应改为直接传递。
3. 8 个 string key 隐式契约——组件化后应改为枚举或类型化接口。

### 8.6 rg 扫描摘要

执行 `rg -n "PWL|continuousRadius|UnivariateLinearPiecewiseFunction|model.addConstraint|registerPWL|extractPWL"` 扫描 BPP3D 全目录，命中 547 行。主要集中在：

- `PWLContinuousRadiusRegistration.kt`：变量/约束/token 注册和结果提取的核心实现。
- `ContinuousRadiusSolverRegistrationPlan.kt`：变量创建和分类。
- `ColumnGenerationStandardExecutors.kt`：执行器中的创建/注册/提取调用。
- `ColumnGenerationPackingAnalyzer.kt`：selection result 构建。
- `ColumnGenerationAlgorithm.kt`：opaque map 传递。
- `CylinderShapeContract.kt`：领域原型、metadata、工厂方法。
- `PackingRendererAdapter.kt`：renderer actualVolume 和 PWL info。
- infrastructure 三个工具文件：纯计算，无编排。
- 测试文件（正例/负例/Gurobi）：覆盖 PWL 路径。
- README.md / README_ch.md / daily.md：文档描述。
- 边界脚本：现有 BoundingCuboid / compat alias 检查。

审计阶段不修改生产代码。

### 8.7 阶段二设计决策

日期：2026-06-11

#### 组件落点

最终文件规划：

| 组件 | 文件路径 | 所属模块 |
|------|---------|---------|
| `ContinuousRadiusModelComponent` | `domain/item/model/ContinuousRadiusModelComponent.kt` | `bpp3d-domain-item-context` |
| `PWLExtractedRadius` | 同上（与组件同文件） | `bpp3d-domain-item-context` |
| `ContinuousRadiusSelectionExtractor`（native + PWL + typed） | `domain/item/model/ContinuousRadiusSelectionExtractor.kt` | `bpp3d-domain-item-context` |
| `ContinuousRadiusSolverVariable` / `PWLContinuousRadiusSolverVariable` / `RegistrationPlan` | 保留在 `application/service/ContinuousRadiusSolverRegistrationPlan.kt`（阶段三迁入组件或保持为 internal 委托） | `bpp3d-application` |

选择 `bpp3d-domain-item-context` 的理由：

1. `ContinuousCylinderRadiusSolverPrototype`、`PWLRadiusSelectionMetadata`、`CylinderRadiusSelectionResult` 已在此模块。
2. parent POM 已在所有子模块提供 `ospf-kotlin-core`（`LinearMetaModel`、`RealVar`、`UnivariateLinearPiecewiseFunction`），无需新增依赖。
3. 符合 `.rules/framework-architecture.md` 第 3 节"model component 持有领域变量、中间值、派生表达式和结果解析所需引用"的定位。

#### 组件接口设计

```
ContinuousRadiusModelComponent
├── prototypes: List<ContinuousCylinderRadiusSolverPrototype>
├── config: PWLRadiusApproximationConfig
├── nativeVariables: List<ContinuousRadiusSolverVariable>  (lazy)
├── pwlVariables: List<PWLContinuousRadiusSolverVariable>  (lazy)
├── registrationPlan: ContinuousRadiusSolverVariableRegistrationPlan  (lazy)
├── register(model, ensureTry)          // 注册所有变量+约束
├── extractNativeResults(model)          // -> Map<String, InfraNumber>
├── extractPWLResults(model)             // -> Map<String, Map<String, InfraNumber>> (opaque)
├── extractPWLResultsList(model)         // -> List<PWLExtractedRadius> (typed)
└── info()                               // -> Map<String, String> (诊断)
```

```
ContinuousRadiusSelectionExtractor（顶级函数）
├── buildNativeContinuousRadiusSelectionResults(prototypes, solverResults)
│   -> List<CylinderRadiusSelectionResult>
├── buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
│   -> List<CylinderRadiusSelectionResult>  (从 opaque Map)
└── buildPWLSelectionResultsFromExtracted(prototypes, extractedResults)
    -> List<CylinderRadiusSelectionResult>  (从 typed PWLExtractedRadius)
```

#### Application 层调用方式（阶段三迁移后）

```kotlin
// ColumnGenerationStandardExecutors.finalSolver() 中：
val continuousRadiusComponent = ContinuousRadiusModelComponent(
    prototypes = state.continuousRadiusSolverPrototypes
)
continuousRadiusComponent.register(model, ::ensureTry)

// 求解后：
val nativeResults = continuousRadiusComponent.extractNativeResults(model)
val pwlResults = continuousRadiusComponent.extractPWLResults(model)

// ColumnGenerationPackingAnalyzer.analyze() 中：
val allSelectionResults = buildNativeContinuousRadiusSelectionResults(prototypes, nativeResults)
    + buildPWLContinuousRadiusSelectionResults(prototypes, pwlResults)
```

#### 设计约束确认

1. 不改变 public DTO 或 CSV 语义。
2. 不改变 `PWLRadiusSquaredApproximation` 数学含义（仍是 `q ≈ r²`）。
3. 不改变 renderer `actualVolume` 口径（仍用 `π*r²*h`）。
4. 不引入离散半径候选生成。
5. 不把 `Double` 裸值扩散到领域模型；转换集中在 adapter/registration/extraction 边界。
6. import 排序、KDoc、中英双语注释遵循 `.rules/chore.md`。

#### 骨架文件已创建

1. `bpp3d-domain-item-context/src/main/.../domain/item/model/ContinuousRadiusModelComponent.kt` — 包含 `ContinuousRadiusModelComponent` 类和 `PWLExtractedRadius` 数据类（完整实现）。
2. `bpp3d-domain-item-context/src/main/.../domain/item/model/ContinuousRadiusSelectionExtractor.kt` — 包含三个提取函数（完整实现）。

### 8.8 阶段三至四执行记录

日期：2026-06-11

#### 阶段三：迁移 PWL 注册链路

已完成的变更：

1. `ColumnGenerationStandardExecutors.kt`：
   - `RmpArtifacts` 数据类用 `ContinuousRadiusModelComponent` 替换三个独立列表字段。
   - `buildRmpArtifacts()` 改为创建组件并调用 `component.register(model, ::ensureTry)`。
   - `finalSolver()` 同样改为组件化注册。
   - 移除 `registerContinuousRadiusVariables()`、`extractContinuousRadiusSolverResults()`、`extractPWLRadiusValuesFromModel()` 三个私有方法。
   - 移除 `Comparison`、`LinearInequality`、`LinearMonomial`、`LinearPolynomial`、`ContinuousRadiusSolverVariable`、`PWLContinuousRadiusSolverVariable`、`continuousRadiusSolverVariables`、`pwlContinuousRadiusSolverVariables`、`continuousRadiusSolverVariableRegistrationPlan` 等不再使用的导入。

2. `ColumnGenerationPackingAnalyzer.kt`：
   - 移除私有 `buildPWLContinuousRadiusSelectionResults()` 函数，改用域模块 `buildPWLContinuousRadiusSelectionResults`。

3. `PWLContinuousRadiusRegistration.kt`：
   - `registerPWLContinuousRadiusVariables()` 已成为死代码（无调用方），保留文件供后续清理。

#### 阶段四：迁移 PWL 提取与 renderer selection 链路

已完成的变更：

1. `ColumnGenerationPackingAnalyzer.kt`：
   - 移除私有 `buildContinuousRadiusSelectionResults()` 函数，改用域模块 `buildNativeContinuousRadiusSelectionResults`。
   - analyzer 的 `analyze()` 方法现在只做：获取 native selection results → 获取 PWL selection results → 合并 → 调用 `PackingRendererAdapter.toSchema()`。
   - 移除 `ContinuousCylinderRadiusSolverPrototype`、`withSolverSelectedRadius`、`withPWLSolverSelectedRadius` 等不再使用的导入。

#### 编译验证

全 BPP3D 父 POM 编译通过（9/9 模块 BUILD SUCCESS）。唯一警告为预存的 unchecked cast（`PWLContinuousRadiusRegistration.kt:82`）。

### 8.9 阶段五至六执行记录

日期：2026-06-11

#### 阶段五：扩展点测试

新增测试文件：

1. `ContinuousRadiusModelComponentTest.kt`（431 行，20 个测试）：
   - 组件注册测试：native 变量创建、PWL 变量创建、混合原型分类。
   - 配置注入测试：不同 config 影响段数、断点、误差驱动策略和 debugInfo。
   - 注册计划测试：四路径分类、互斥摘要、PWL 变量诊断。
   - PWLExtractedRadius 值对象测试：体积计算、诊断信息。
   - 四路径互斥测试：native/PWL/blocked/productionReady 属性。
   - 空原型列表和多 PWL 原型测试。

2. `ContinuousRadiusSelectionExtractorTest.kt`（359 行，14 个测试）：
   - Native 选择结果构建测试：正常构建、跳过缺失值、空输入、无 PWL metadata。
   - PWL 选择结果构建测试（opaque Map）：正常构建、跳过未知变量名、envelope 违规。
   - PWL 选择结果构建测试（typed PWLExtractedRadius）：从提取结果直接构建、numSegments 来源、空输入。
   - actualVolume 一致性测试：PWL 路径使用 r²（非 q）、typed 和 opaque 路径结果一致。

测试验证结果：BPP3D 全模块 BUILD SUCCESS，9/9 模块通过。

#### 阶段六：边界脚本硬化

新增边界检查（`shape-boundary-check.ps1`）：

1. **PWLApplicationConstraintRegistrationReflux**：检测 `registerPWLContinuousRadiusVariables`、`registerPWLFunctionConstraints`、`extractPWLRadiusValues` 出现在 application solver 代码中。PWL 注册应通过 `ContinuousRadiusModelComponent`。白名单包含死代码文件 `PWLContinuousRadiusRegistration.kt` 和正确的领域组件 `ContinuousRadiusModelComponent.kt`。

2. **PWLDiscreteFallbackReflux**：检测 `discreteRadiusCandidate`、`fallbackRadiusList`、`silentDowngrade`、`DiscreteBreakpointStrategy` 等关键词出现在生产路径中。白名单包含 `CylinderShapeContract.kt`（其 guard 消息中引用 "discrete radius candidates"）。

3. **ContinuousRadiusUnsupportedRegression**：检测 `isPWLRegisterable = false`、`PWL_PATH_DISABLED`、`pwl_registration_blocked` 等模式出现，防止已开放的 interval-only PWL 路径被重新标为 unsupported。

修复的既有边界检查：

1. **ContinuousCylinderRadiusSolverRegistrationPlanGuardMissing**：路径从 `ContinuousRadiusSolverRegistrationPlan.kt` 更新为 `ContinuousRadiusModelComponent.kt`（类型已迁移）。
2. **ContinuousCylinderRadiusSolverFinalRegistrationPlanUsageMissing**：模式从 `continuousRadiusVariablePlan` 更新为 `continuousRadiusComponent`（executor 已改用组件）。
3. **ContinuousCylinderRadiusSolverRmpRegistrationPlanUsageMissing**：同上。

边界脚本验证：4 个 BPP3D 边界脚本全部通过（`SHAPE_BOUNDARY_PASS`、`STRICT_GENERIC_BOUNDARY_PASS`、`GEOMETRY_BOUNDARY_PASS`、`GEOMETRY_MODULE_DRY_RUN_PASS`）。

### 8.10 阶段七执行记录

日期：2026-06-11

#### 误差预算推导

新增 `SegmentCountDerivation` 数据类和 `PWLRadiusSquaredApproximation.deriveSegmentCount()` 方法：

1. 算法：从 1 段开始，逐步加倍直到满足 tolerance 或达到 maxSegments。
2. 不静默放宽精度目标：若 maxSegments 内无法满足，返回 `meetsTolerance=false`。
3. 诊断信息：`info()` 方法输出 `pwl_derived_segments`、`pwl_derived_achieved_max_rel_error`、`pwl_derived_meets_tolerance`、`pwl_derived_iterations`。

修改文件：`PWLRadiusSquaredApproximation.kt`

#### PWL 模型规模 KPI

新增 `ContinuousRadiusModelComponent.modelScaleInfo()` 方法，输出 9 个 KPI：

| Key | Content |
|-----|---------|
| `pwl_total_prototypes` | PWL 变量总数 |
| `pwl_total_segments` | 所有变量段数之和 |
| `pwl_total_selector_vars` | 二值选择变量总数 |
| `pwl_total_helper_vars` | 辅助变量总数 |
| `pwl_total_constraints` | PWL 约束总数（select-one + 4N per variable） |
| `pwl_max_segments` | 单变量最大段数 |
| `pwl_avg_segments` | 平均段数 |
| `pwl_max_relative_error` | 最大相对误差 |
| `pwl_avg_relative_error` | 平均最大相对误差 |

修改文件：`ContinuousRadiusModelComponent.kt`

#### 极端半径测试

新增 6 个极端半径和误差预算测试：

| Test | Edge Case |
|------|-----------|
| `testPWLWithVerySmallRMin` | rMin=0.01, rMax=1.0，验证 PWL 仍有效 |
| `testPWLWithLargeRatio` | rMin=1.0, rMax=100.0，8 段相对误差超 10% |
| `testPWLWithNarrowInterval` | rMin=5.0, rMax=5.01，1 段足够精确 |
| `testDeriveSegmentCountMeetsTolerance` | 误差预算推导满足 tolerance |
| `testDeriveSegmentCountExceedsMaxSegments` | maxSegments 内无法满足，返回 meetsTolerance=false |
| `testDeriveSegmentCountDoesNotSilentlyRelax` | 不静默放宽误差目标 |

新增 3 个 deriveSegmentCount 测试（`PWLRadiusSquaredApproximationTest.kt`）和 3 个 modelScaleInfo 测试（`ContinuousRadiusModelComponentTest.kt`）。

验证：BPP3D 全模块 BUILD SUCCESS（9/9 模块），4 个边界脚本全部通过。

