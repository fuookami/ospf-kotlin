# CSP1D 泛型化计划

日期：2026-06-10

## 1. 总目标

将 `ospf-kotlin-framework-csp1d` 建设为相对抽象、可复用的一维分切开发包。目标不是把 `poit/csp1d` 原样搬入 framework，而是沉淀一维分切的通用内核，并把项目接口、运行参数、DTO 协议、公式语言、solver 插件选择等下游适配内容留在业务侧。

当前抽象口径以 `csp1d-domain-material-context` 已提供的实体能力为边界：只保留 `Product`、`ProductDemand`、`Production`、`Costar`、`Material`、`Machine`、`CuttingPlanSlice`、`CuttingPlan`、需求贡献和当前增强上下文能够表达的部分；不把 POIT 中尚未进入 material model 的缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、公式语言、训练平台和业务 DTO 当作当前阶段的必备模型。

## 2. 开发包边界

1. **通用核心层**：material、cutting plan、produce 主问题、solver/quantity adapter。
2. **通用增强层**：yield、length assignment、wasting minimization、schedule variant。
3. **应用编排层**：普通 MILP、列生成、recovery/warm start、Top-K、KPI、render DTO。
4. **下游适配层**：PO/DTO、公式语言、接口服务、控制台、项目运行参数、心跳、租户上下文、solver 插件选择、训练平台和历史样本管理。

暂不单独拆 `csp1d-domain-schedule-context`。schedule 相关能力先作为 produce、yield、length assignment、wasting minimization 的 variant 保留；当重复接口稳定后再抽出公共层。

## 3. 已完成事项

已完成事项只保留阶段级摘要，不保留逐类、逐断言、逐命令的历史细节。

1. 已完成 CSP1D framework 的基础模块、通用领域模型、应用入口和 PO/DTO 无关的问题输入主路径。
2. 已完成 material、cutting plan、produce 主问题、物理量、泛型数值、需求贡献和增强上下文的核心语义收口。
3. 已完成普通 MILP、列生成、LP shadow price、pricing、最终 MILP、Top-K、KPI/render 和 trace 的 application 主链路。
4. 已完成初始方案生成主能力、生成统计、benchmark 快照、缓存、剪枝、并行和 dominance 的阶段性性能增强。
5. 已完成 application public 使用面第一阶段，包括 builder、统一 solve config、partial/failed 状态、recovery/warm start、plan-pool adapter、native initial solution 和列生成 recovery。
6. 已完成 demo3 示例主路径迁移，示例不再维护手写 RMP/SP。
7. 已完成当前模型与 POIT CSP1D 边界复核，延后能力继续作为下游适配或未来 material model 扩展能力。
8. 已完成 application failure/partial 边界收口，异常安全、失败状态、LP failure trace、pricing 统计 trace 和 render KPI 已进入 public 输出口径。
9. 已完成 README/README_ch 对 public API、能力边界、failure/partial 语义、`LpInfeasible` 推断语义和新增 KPI key 的同步说明。
10. 已完成 application acceptance、CSP1D 门禁搜索和 `git diff --check` 的阶段性验证；完整 reactor、当前 generation 目标测试和 Gurobi profile 仍需最终验收。

## 4. 需要修正的事项

当前没有已确认的 material-context 建模偏差。以下交付风险已在本轮验证中确认：

1. ~~`LpInfeasible` 当前基于首次 LP null 返回推断，不是 solver 层确定不可行状态~~：文档已说明该边界，README 口径与测试和 trace 输出一致，已验收。
2. ~~当前 generation benchmark 源码已包含新增目标测试，但当前源码对应的测试报告仍需重新生成~~：本轮已重新执行全部 58 个 generation 测试和 50 个 application acceptance 测试，均通过。
3. ~~完整 CSP1D reactor、Gurobi profile 编译或 smoke 仍未形成可信验收记录~~：Gurobi profile `test-compile` 已通过，Gurobi 10 环境下真实 solver smoke 已通过。
4. ~~工作区存在大量非 CSP1D 改动~~：已审计，CSP1D 96 文件变更均在修改清单范围内，非 CSP1D 变更不纳入验收。
5. ~~demo3 与 README/README_ch 需复核~~：已复核，demo3 使用 framework API，无手写 RMP/SP，与 README 描述一致。

剩余风险（按优先级）：
1. ~~Maven 本地仓库缓存导致 application acceptance surefire 12 个 error~~：已修复，清理旧 artifact 并 `mvn install -am` 后重跑，50/50 通过。
2. ~~produce-context 缺少 surefire 报告~~：已确认该模块无测试源码，无需 surefire 报告。
3. ~~Gurobi 端到端 smoke 未执行~~：当前 Gurobi 10 环境下已执行目标真实 solver smoke，7/7 通过。
4. ~~文档完整性：`yieldConfig` DSL 示例和 `warmStartPlanUsages` 构造参数文档可后续补充~~：已在 README/README_ch 的 Basic Use / 基本使用 章节补充 `yieldConfig` DSL 示例（含 `underProductionPenalty`、`overProductionPenalty`、`overProductionUpperBound`）和 `warmStartPlanUsages` 构造参数示例（含 `CuttingPlanUsage` 配对）。
5. ~~延后能力边界仍需复核~~：已移除 render DTO 中未使用的 `DefectCostar` 残留，缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、公式语言、训练平台、历史样本服务等延后能力不进入当前领域主路径。

## 5. 下一轮目标

在不扩大到 POIT 业务 DTO 和未建模实体的前提下，完成 CSP1D framework 的最终交付验收：跑通当前源码对应的验证基线，确认真实 solver 边界，复核 public API、README/README_ch、demo3 和 trace/KPI/render 输出一致性，并确保 CSP1D 范围与非 CSP1D 工作区改动清晰隔离。

## 6. 下一轮事项

1. **工作区范围收口**：审计当前 diff，区分 CSP1D 验收范围和非 CSP1D 改动，确保提交、验证和结论不混入无关模块。
2. **生成器验证收口**：重新执行当前 `GeneratorMediumScaleBaselineTest` 和 generation 目标测试，确认缓存、剪枝、并行、dominance 和 benchmark 快照不改变 canonical 结果集合。
3. **application 验证收口**：重新执行 application acceptance，确认 failure/partial、LP null 推断、recovery/warm start、trace/KPI/render 与 README 口径一致。
4. **真实后端路径收口**：执行 Gurobi profile `test-compile`；环境允许时补真实 solver smoke，不可用时记录明确原因。
5. **public API 与文档复核**：复核 README/README_ch、demo3 和最小使用样例，确保仅依赖 CSP1D framework API，不恢复业务 DTO 或手写 RMP/SP。
6. **延后能力边界维护**：缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、训练平台、历史样本服务和业务 DTO 继续只出现在边界说明中，不进入当前领域主路径。

## 7. 下一轮计划

1. 读取当前 diff，先确认 CSP1D 范围是否干净，非 CSP1D 改动只记录不处理。
2. 运行当前 generation 目标测试和 benchmark 快照测试，失败时优先修正统计口径或快照。
3. 运行 application acceptance，失败时优先修正 failure/partial、trace/KPI/render 或 README 口径。
4. 运行 CSP1D 局部 reactor 和 Gurobi profile；若 Maven 或 Gurobi 环境阻塞，记录命令、失败原因和替代验证。
5. 执行门禁搜索和 `git diff --check`，将实际验证结果写回本文件。

## 8. 修改清单

下一轮允许在以下范围内集中修改，仍避免触碰无关 framework 模块。

1. `ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context`
2. `ospf-kotlin-framework-csp1d/csp1d-application`
3. `ospf-kotlin-framework-csp1d/csp1d-domain-material-context`
4. `ospf-kotlin-framework-csp1d/csp1d-domain-produce-context`
5. `ospf-kotlin-framework-csp1d/csp1d-domain-yield-context`
6. `ospf-kotlin-framework-csp1d/csp1d-domain-length-assignment-context`
7. `ospf-kotlin-framework-csp1d/csp1d-domain-wasting-minimization-context`
8. `ospf-kotlin-framework-csp1d/csp1d-infrastructure`
9. `ospf-kotlin-framework-csp1d/README.md`
10. `ospf-kotlin-framework-csp1d/README_ch.md`
11. `ospf-kotlin-framework-csp1d/daily.md`
12. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo3`

## 9. 验收标准

1. 当前 material-context 能表达的模型语义不偏离总目标，不引入 POIT DTO、运行参数、公式语言、训练平台或 solver 插件选择逻辑。
2. 生成器增强不改变 DFS、N-Same、N-Sum、FullSum 的 canonical 结果集合；缓存、剪枝、并行和 dominance 均有稳定统计和回归测试。
3. benchmark 覆盖中等规模、混合需求单位、扩展规模、更多约束组合、设备可行性和更高候选上限；耗时只作为趋势观察，数量类统计作为稳定验收。
4. 普通 MILP、列生成、recovery、warm start 在完整解、空解、部分解、失败、fallback 禁用和 LP null 场景下有明确状态、trace 和 failureMessage。
5. `LpInfeasible` 的 public 语义必须稳定表述为首次 LP null 的推断；若要改为确定不可行判定，必须先由 solver 适配层暴露可靠状态。
6. initial/pricing 生成统计、终止原因、partial/failed 状态、LP failure message 和 enhancement 结果在 solution、render、trace 和 README/README_ch 中一致。
7. demo3 不维护手写 RMP/SP，README/README_ch 能说明当前 public API、能力边界、配置、failure/partial 语义和验证命令。
8. 延后能力只出现在文档边界中，不在当前领域主路径中出现半成品字段或业务适配逻辑。
9. CSP1D 门禁搜索无领域主路径违规命中，`git diff --check -- ospf-kotlin-framework-csp1d` 通过。
10. 至少通过 CSP1D 局部测试、application acceptance、生成器目标测试和 Gurobi profile `test-compile`；环境允许时补 Gurobi 端到端 smoke。

## 10. 验证基线

下一轮最小验证集如下，执行后需要记录实际结果，不复用历史报告。

1. CSP1D 局部 reactor 测试：覆盖 material、generation、enhancement 和 application。
2. 生成器目标测试：覆盖 DFS、N-Same、N-Sum、FullSum、parallelism、benchmark snapshot 和新增剪枝/缓存统计。
3. application acceptance：覆盖 MILP、列生成、Top-K、KPI/render、recovery、warm start 和 partial solution。
4. Gurobi profile：至少执行 `-Pgurobi-cg-test -DskipTests test-compile`，环境可用时执行目标 smoke。
5. 门禁搜索：
   - `rg -n "com\\.poit|framework\\.bpp3d" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "rollDemand|weightDemand|sheetDemand" ospf-kotlin-framework-csp1d -g "*.kt" -g "!**/src/test/**" -g "!**/src/gurobi-test/**"`
   - `rg -n "ProduceSolver|SimpleProduceSolver" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "candidatePlans" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `rg -n "println\\(" ospf-kotlin-framework-csp1d -g "*.kt"`
   - `git diff --check -- ospf-kotlin-framework-csp1d`

## 11. 验证结果（2026-06-10）

### 工作区范围收口

- CSP1D 变更文件：96 个，全部在修改清单范围内。
- 非 CSP1D 变更文件：1983 个，不纳入 CSP1D 验收。
- demo3 变更：`Main.kt` 1 个文件，在修改清单范围内。

### 生成器验证收口

- `GeneratorMediumScaleBaselineTest`：8 测试通过，0 失败。
- 全部 generation context 测试：58 测试通过，0 失败。
  - ConstraintsTest: 8 通过
  - CuttingPlanCanonicalKeyTest: 3 通过
  - CostarFillerTest: 4 通过
  - DFSGeneratorTest: 5 通过
  - FullSumGeneratorTest: 5 通过
  - GeneratorMediumScaleBaselineTest: 8 通过
  - GeneratorParallelismTest: 1 通过
  - NSameGeneratorTest: 10 通过
  - NSumGeneratorTest: 3 通过
  - ReducedCostPricingGeneratorTest: 11 通过
- 缓存、剪枝、并行、dominance 和 benchmark 快照不改变 canonical 结果集合。

### application 验证收口

- 首轮 Maven surefire：`Tests run: 50, Failures: 0, Errors: 12`，12 个 error 均为 `NoSuchMethodError: Csp1dPricingGenerator.generateWithReport(Csp1dPricingInput)`。
- 错误原因：本地 Maven 仓库中 generation-context JAR 为旧版本；清理旧 artifact 并执行 `mvn install -am -Dgpg.skip=true` 后重跑。
- **修复后 Maven surefire：`Tests run: 50, Failures: 0, Errors: 0, Skipped: 0`，全部通过 ✅**
- failure/partial、LP null 推断、recovery/warm start、trace/KPI/render 语义与 README 口径一致。

### 其他 CSP1D 子模块测试

- `csp1d-domain-material-context`：17 测试通过（surefire 报告确认）。
- `csp1d-domain-produce-context`：该模块无测试源码（`src/test` 为空），Maven 报告 `No tests to run`，无需 surefire 报告。
- `csp1d-domain-yield-context`：6 测试通过（surefire 报告确认）。
- `csp1d-domain-length-assignment-context`：4 测试通过（surefire 报告确认）。
- `csp1d-domain-wasting-minimization-context`：3 测试通过（surefire 报告确认）。

### 真实后端路径收口

- Gurobi profile `test-compile`：通过（`-Pgurobi-cg-test -DskipTests test-compile` 成功）。
- Gurobi 真实 solver smoke：当前 Gurobi 10 环境下执行 `Csp1dColumnGenerationRealSolverTest` 目标 7 个用例，`Tests run: 7, Failures: 0, Errors: 0, Skipped: 0`，reactor `BUILD SUCCESS`。
- Maven surefire 输出 `Corrupted channel by directly writing to native stream in forked JVM` warning，并生成 dumpstream 文件；测试结果和 reactor 构建均为通过，作为非阻塞提示记录。

### 门禁搜索

| 检查项 | 结果 |
|--------|------|
| `com.poit\|framework.bpp3d` | 无命中 ✅ |
| `rollDemand\|weightDemand\|sheetDemand` | 仅 gurobi-test 目录命中（使用 `legacyRoll`/`legacySheet` 工厂方法，属于允许的测试范围）✅ |
| `ProduceSolver\|SimpleProduceSolver` | 无命中 ✅ |
| `candidatePlans` | 无命中 ✅ |
| `println(` | 无命中 ✅ |
| `git diff --check` | 通过 ✅ |

### public API 与文档复核

- README/README_ch 与当前 public API 基本一致。
- 关键语义准确：failure/partial/LpInfeasible 描述、生成统计字段、recovery/warm start 能力。
- 无废弃 API 引用（ProduceSolver、candidatePlans、业务 DTO 均未出现）。
- 文档完整性建议已处理：README/README_ch 已补充 `yieldConfig` DSL 示例和 `warmStartPlanUsages` 构造参数示例。
- demo3 使用 `Csp1dProblem<Flt64>`、framework generators、`ReducedCostPricingGenerator`、`Csp1dColumnGeneration`，无手写 RMP/SP，与 README 描述一致。

### 遗留问题

1. ~~Maven 本地仓库缓存导致 application acceptance surefire 12 个 error~~：已修复，清理旧 artifact 并 `mvn install -am` 后重跑，50/50 通过。
2. ~~produce-context 缺少 surefire 报告~~：已确认该模块无测试源码，`No tests to run`，无需 surefire 报告。
3. ~~Gurobi 端到端 smoke 未执行~~：当前 Gurobi 10 环境下已执行目标真实 solver smoke，7/7 通过。
4. ~~文档完整性：`yieldConfig` DSL 示例和 `warmStartPlanUsages` 构造参数文档可后续补充~~：已在 README/README_ch 补充。
5. ~~延后能力边界仍需复核~~：已移除 render DTO 中未使用的 `DefectCostar` 残留，当前领域主路径不引入缺陷、分段、位置约束、`unitBatch`、物料级 costar 属性、公式语言、训练平台或历史样本服务。
