# P26 工程健康第四批交接（2026-05-22）

## 最新结论

`math`、`multiarray`、`quantities`、`core`、`core-plugin`、`framework`、`benchmark` 的迁移收口与 P18-P25 工程健康目标已经闭环。P25-1 到 P25-5 均已完成，P25 审阅中发现的交付整洁性问题已处理：`daily.md` 补充了公开 helper 口径，内存清理残留说明已校正，新拆分文件中的单语/乱码注释已清理，旧的 `non-default-errors*.txt` 诊断残留已删除。

P26 尚未执行，也未创建 P27。P26 应继续作为低风险工程健康批次推进，不重新打开 P18-P25 的迁移目标，不恢复旧兼容层，不把 benchmark 数值设为硬门禁。

## 已完成事项摘要

以下只保留已完成结论，不保留执行细节。

1. 已完成从 core 自有表达式体系到 `math.symbol` 正式符号体系的迁移。
2. 已删除旧兼容层与迁移桥接入口，包括旧 adapter/bridge/compat 路径和迁移期命名。
3. 已完成 core 主链路泛型化，保留的 Flt64 命名仅表达真实快捷层、解析器或 solver 边界职责。
4. 已恢复 example 默认构建与项目内 source-compat / profile 验收入口。
5. 已建立 P6/P7 静态门禁，覆盖兼容层回流、旧命名回流、旧 import 回流、危险 hard-cast、空测试等风险。
6. 已完成 multiarray、math、quantities、core、core-plugin、framework 的多轮性能和结构优化。
7. 已建立 benchmark baseline、small smoke artifact 与趋势比较脚本，趋势比较只输出报告，不作为性能硬门禁。
8. 已完成 benchmark 模块默认构建参与、不参与发布、`bench` profile 下启用 JMH 生成与运行的边界整理。
9. 已完成 core 大文件多轮职责拆分，覆盖 dump、elastic builder、dual/farkasDual、solver-boundary conversion、TokenTable、IntermediateSymbol、MechanismModel、MetaModel 等辅助职责。
10. 已完成 core 与目标 solver 插件的内存清理和 batch 分段策略收敛。
11. 已完成 math、quantities、utils 的多批 unchecked cast 局部收敛，P25 已消化 math 剩余 7 处高风险 main 源 warning。
12. 已完成 solver 插件公共数据准备、状态归一、错误码兜底、重复 `when` 分支清理、callback/error helper 收敛和状态失败构造统一。
13. 已完成 P25 benchmark 趋势沉淀增强，脚本支持显式文件模式与目录模式，CI artifact 已扩展为结果目录。
14. P25 目标验收已通过：core 编译与目标测试、math 目标测试、benchmark 编译与脚本、目标 solver 插件编译、P6/P7、`git diff --check` 均通过。

## P25 交付口径修正

1. P25 原则中的“不改变公开 API”按“不破坏既有公开 API”执行。P25-4 为支持跨模块 solver 插件复用，新增了 `core.solver` 下的公开 top-level helper，属于加法式插件支持 API，不改变既有调用方语义。
2. 新增公开 helper 的理由：真实 solver 插件模块依赖 core，helper 需要被多个插件复用；放在 core internal 作用域无法跨模块调用，放在单个插件会造成重复或反向依赖。
3. 影响范围：新增 `executeCreatingEnvironmentCallback(...)`、`environmentLost(...)`、`solvingException(...)`、`modelingException(...)`、`terminated()`、`failByStatus(...)`、`cleanupOnSolverMemoryPressure()`、`cleanupAfterSolverRun()` 等插件支持入口；未删除或重命名既有公开 API。
4. 替代方案：若后续希望收窄公开面，可在 P26 中定义清晰的 solver plugin support API 包边界与文档口径，但不应在未记录替代路径前破坏现有插件编译。
5. 内存清理残留口径：`System.gc()` 在 core 主链路和目标 solver 插件中已收敛到命名化策略/helper；`MemoryCleanupPolicy.kt` 与 `SolverMemoryCleanupSupport.kt` 是当前保留点。Heuristic 插件中的历史直调未纳入 P25，建议作为 P26 可选收敛项。

## P26 目标

P26 的目标是在 P25 已闭环的基础上继续降低维护风险，重点处理 P25 审阅后自然暴露的后续优化点：solver 插件支持 API 边界、heuristic 插件内存清理残留、benchmark 趋势脚本自动化验证，以及新拆分 helper 的文档与注释卫生。P26 不重启迁移目标，不做大范围 API 重设，不要求真实外部 solver license 作为默认阻塞项。

1. 明确 `core.solver` 公共 helper 的插件支持 API 边界，避免公共 helper 继续无约束扩散。
2. 收敛 heuristic 插件中残留的直接 `System.gc()`，复用 solver 侧命名化 cleanup helper。
3. 为 benchmark 趋势比较脚本补轻量自动化 smoke，覆盖目录模式与错误分支。
4. 清理新拆分 helper 的注释、KDoc 与文档口径，保持中英双语、无乱码、无版权声明。

## P26 总体原则

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；如必须新增或调整公开 helper，必须记录理由、影响范围、替代方案和验收方式。
3. P26 是工程健康优化，不重新定义 P18-P25 的完成口径。
4. Flt64 / Double 转换必须留在显式 solver 边界或既有白名单，不能回流到 core 泛型主链路。
5. warning debt 清理不做全仓机械 suppress；新增 suppress 必须靠近最小作用域，并用中英双语注释说明可证明不变量。
6. solver 公共化只抽 solver 无关的数据准备、分段、cleanup、callback 失败兜底、状态归一和错误构造逻辑，不强行统一真实 solver API。
7. benchmark 趋势比较只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
8. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
9. 写注释时遵守项目规则：中英双语；不添加版权声明。
10. 每个子任务建议独立 commit；提交前必须记录修改清单与验收命令。

## P26 事项与步骤

### P26-1：solver 插件支持 API 边界整理

状态：未开始。

目标：明确 P25 新增 `core.solver` 公共 helper 的支持范围与长期维护口径，避免插件支持 API 无边界扩散。

详细步骤：

1. 扫描 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver` 中 P25 新增或扩展的公开 top-level helper。
2. 将 helper 分类为：
   - 状态/错误构造支持；
   - callback 失败处理；
   - solver 侧内存清理支持；
   - 仅内部使用但因为跨模块必须公开的插件支持入口。
3. 为保留公开的 helper 补充中英双语 KDoc，说明用途、非目标和稳定性口径。
4. 评估是否需要用文件名、包名或注释显式标识“plugin support API”；不做会破坏当前插件编译的可见性收窄。
5. 扫描插件侧调用点，确认没有重复本地 helper 回流。
6. 如发现可以安全改为 `internal` 的 helper，必须先确认没有跨模块调用，再单独记录。

预计修改清单：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverFailureSupport.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverStatusSupport.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverMemoryCleanupSupport.kt`
4. 相关 solver 插件调用点，仅在需要同步 import 或命名时修改。
5. `ospf-kotlin-core/daily.md`

验收标准：

1. `mvn -pl ospf-kotlin-core -am "-DskipTests" compile` 通过。
2. P25 触及的 solver 插件 `-DskipTests compile` 通过。
3. 新增或保留的公开 helper 均有中英双语 KDoc 或同等说明。
4. 不新增旧兼容层、旧命名、旧 import 或危险 hard-cast。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

### P26-2：heuristic 插件内存清理残留收敛

状态：未开始。

目标：把 heuristic 插件中残留的直接 `System.gc()` 收敛到命名化 solver cleanup helper，降低与 P25 主策略不一致的维护风险。

详细步骤：

1. 扫描 `ospf-kotlin-core-plugin-heuristic` 中的 `System.gc()`、`memoryUseOver(...)` 和求解结束 cleanup 调用点。
2. 优先处理 GA / GWO / MVO / PSO / SAA / SCA 等已知直接 `System.gc()` 路径。
3. 对 memory pressure 场景复用 `cleanupOnSolverMemoryPressure()`，对求解主流程结束复用 `cleanupAfterSolverRun()`。
4. 保持 heuristic 算法逻辑、终止条件、随机性入口和返回状态不变。
5. 如某个算法存在特殊 cleanup 语义，保留并在本文件记录原因。
6. 编译 heuristic 插件，不要求真实外部 solver license。

预计修改清单：

1. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/ga/GA.kt`
2. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/gwo/GWO.kt`
3. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/mvo/MVO.kt`
4. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/pso/PSO.kt`
5. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/saa/SAA.kt`
6. `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/sca/SCA.kt`
7. `ospf-kotlin-core/daily.md`

验收标准：

1. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile` 通过。
2. heuristic 插件 main 源码中不再出现业务路径直接 `System.gc()`，如有残留必须记录原因。
3. 不改变算法公开参数、返回类型、终止状态和随机性入口。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

### P26-3：benchmark 趋势脚本 smoke 自动化

状态：未开始。

目标：把 P25 的 benchmark 趋势脚本能力沉淀为轻量自动化验证，降低后续脚本改动破坏目录模式或报告输出的风险。

详细步骤：

1. 设计不依赖真实 JMH 长跑的脚本级 smoke，使用最小 JSON fixture 或复制已有 small smoke 输出。
2. 覆盖显式文件模式：`-Baseline` + `-Current` + `-Output`。
3. 覆盖目录单匹配模式：`-ResultsDir` 下只有一组 `baseline-*.json` / `current-*.json` 时可省略 `-Dataset`。
4. 覆盖目录多匹配错误分支：多组 dataset 且未传 `-Dataset` 时应失败并提示可用 dataset。
5. 验证输出 Markdown 包含 `Input mode`、`Detected dataset` 和 `Gate policy`。
6. 评估是否把 smoke 加入 `core-refactor-guards.yml`；如加入 CI，必须保持轻量，不运行 medium/large benchmark。

预计修改清单：

1. `ospf-kotlin-benchmark/scripts/compare-benchmark-results.ps1`
2. 新增或更新 `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
3. 如需要，新增 `ospf-kotlin-benchmark/scripts/fixtures/...`
4. `.github/workflows/core-refactor-guards.yml`（仅在接入 CI 时修改）
5. `README.md`
6. `README_ch.md`
7. `ospf-kotlin-core/daily.md`

验收标准：

1. benchmark 趋势脚本 smoke 在本地通过。
2. 显式文件模式与目录单匹配模式均能生成 Markdown 报告。
3. 目录多匹配且未指定 `-Dataset` 时按预期失败，错误信息可读。
4. 如修改 CI，workflow 语法保持可解析，artifact 仍上传结果目录。
5. README / README_ch 如有流程变化必须同步更新并保持互链。
6. `git diff --check` 通过。

### P26-4：新拆分 helper 注释与文档卫生

状态：未开始。

目标：继续清理 P25 拆分后遗留的注释和文档噪音，确保新增/搬移 helper 的注释中英双语、无乱码、无版权声明，并避免无意义注释堆积。

详细步骤：

1. 扫描 P25 新增和大幅搬移的 helper 文件中的注释、KDoc 和 README 片段。
2. 删除仅重复代码含义的空泛注释。
3. 对保留的业务边界说明、类型安全不变量、solver 边界说明补齐中英双语。
4. 搜索并清理乱码片段，例如错误编码显示的中文注释。
5. 确认未添加版权声明。
6. 如果新增脚本扫描规则，确保规则只覆盖新改动或明确白名单，避免一次性扩大到全仓历史债务。

预计修改清单：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/IntermediateSymbolExpressionSupport.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTableRegistrationSupport.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/ConcurrentTokenTable.kt`
4. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModelCutSupport.kt`
5. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModelExportSupport.kt`
6. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/*Support.kt`
7. `README.md`、`README_ch.md`（仅在用户入口或脚本流程说明变化时修改）
8. `ospf-kotlin-core/daily.md`

验收标准：

1. 新增或保留的说明性注释均为中英双语。
2. 被修改文件中不再存在明显乱码注释。
3. 不新增版权声明。
4. `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile` 通过。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

## 推荐执行顺序

1. P26-1：先明确 solver 插件支持 API 边界，避免后续继续扩散。
2. P26-2：再收敛 heuristic 插件内存清理残留，与 P25 cleanup 策略对齐。
3. P26-3：随后把 benchmark 趋势脚本能力固化为轻量 smoke。
4. P26-4：最后做注释与文档卫生清理，避免穿插到功能性改动中。

## 推荐验收命令

按改动范围选择执行，不要求每一批都完整跑全部命令。

1. `mvn -pl ospf-kotlin-core -am "-DskipTests" compile`
2. `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile`
3. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile`
4. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am "-DskipTests" compile`
5. `mvn -pl ospf-kotlin-benchmark -am -Pbench "-DskipTests" compile`
6. `powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 -Baseline <baseline.json> -Current <current.json> -Output <trend.md>`
7. `powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 -ResultsDir <results-dir> -Dataset <dataset>`
8. `powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
9. `powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
10. `git diff --check`

## 交接注意事项

1. P26 不要求真实外部 solver license，默认以编译、静态扫描、结构测试和脚本 smoke 为验收基础。
2. `pwsh.exe` 在部分本地环境可能不可用；如不可用，可临时使用 Windows PowerShell 执行同等命令，并在验收记录中说明。
3. benchmark 仍然只做趋势报告和 artifact 留存，不设置性能硬门禁。
4. 若 P26 修改 README / README_ch，必须保持两者互链与内容同步。
5. 每个 P26 子任务完成后，应在本文件补充状态、实际修改清单、验收命令和结果，再提交独立 commit。
