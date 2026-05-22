# P26 工程健康第四批交接（2026-05-22）

## 最新结论

`math`、`multiarray`、`quantities`、`core`、`core-plugin`、`framework`、`benchmark` 的迁移收口与 P18-P25 工程健康目标已经闭环。P25-1 到 P25-5 均已完成，P25 审阅中发现的交付整洁性问题已处理：`daily.md` 补充了公开 helper 口径，内存清理残留说明已校正，新拆分文件中的单语/乱码注释已清理，旧的 `non-default-errors*.txt` 诊断残留已删除。

P26 已完成，P26-1 到 P26-4 已闭环；P27 也已完成，P27-1 到 P27-4 已闭环。P27 在不重新打开 P18-P26 迁移目标的前提下，完成了命令口径修正、heuristic cleanup 结束语义补强、benchmark trend smoke 断言增强，以及测试侧 converter 样板收敛。

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

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverFailureSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverStatusSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverMemoryCleanupSupport.kt`
2. helper 分类结果：
   - 状态/错误构造支持：`environmentLost(...)`、`solvingException(...)`、`modelingException(...)`、`terminated()`、`SolverStatus.resolveErrCode(...)`、`failByStatus(...)`。
   - callback 失败处理：`executeCreatingEnvironmentCallback(...)`、`shouldAbortOnCallbackFailure(...)`。
   - solver 内存清理支持：`cleanupOnSolverMemoryPressure()`、`cleanupAfterSolverRun()`。
   - 跨模块必须公开的插件支持入口：以上 helper 全部属于该类，本批不收窄可见性。
3. 插件侧回流扫描：
   - 扫描 `ospf-kotlin-core-plugin/**.kt` 中同名函数定义，未发现重复本地 helper 回流。
4. 文档与边界口径：
   - 在 3 个 support 文件补齐中英双语 KDoc，显式标注 “plugin support API” 目标、非目标、稳定性口径。
5. 验收命令与结果：
   - `mvn -pl ospf-kotlin-core -am "-DskipTests" compile`：通过。
   - `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am "-DskipTests" compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P26-2：heuristic 插件内存清理残留收敛

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/ga/GA.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/gwo/GWO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/mvo/MVO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/pso/PSO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/saa/SAA.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/sca/SCA.kt`
2. 变更内容：
   - 将 6 处 `if (memoryUseOver()) { System.gc() }` 统一替换为 `cleanupOnSolverMemoryPressure()`。
   - 在 6 个算法求解主流程返回前补 `cleanupAfterSolverRun()`，保持与 solver cleanup 命名化策略一致。
   - 保持算法参数、终止逻辑、随机入口与返回类型不变。
3. 残留检查：
   - `ospf-kotlin-core-plugin-heuristic/src/main` 下已无业务路径直接 `System.gc()` 与 `memoryUseOver(...)` 调用。
4. 验收命令与结果：
   - `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P26-3：benchmark 趋势脚本 smoke 自动化

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - 新增 `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`。
   - 更新 `.github/workflows/core-refactor-guards.yml`，新增 benchmark trend 脚本 smoke 步骤。
2. 覆盖能力：
   - 显式文件模式：`-Baseline` + `-Current` + `-Output`。
   - 目录单匹配模式：`-ResultsDir` 仅一组 dataset 时省略 `-Dataset`。
   - 目录多匹配错误分支：省略 `-Dataset` 时失败并提示显式传参。
   - 报告字段断言：`Input mode`、`Detected dataset`、`Gate policy`。
3. CI 接入评估：
   - 已接入 `core-refactor-guards.yml` 的 `benchmark-smoke` job，执行脚本级 smoke，不引入 medium/large benchmark。
4. README / README_ch 处理：
   - 本批未改变用户入口和脚本参数语义，README / README_ch 无需更新。
5. 验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。
   - `mvn -pl ospf-kotlin-benchmark -am -Pbench "-DskipTests" compile`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P26-4：新拆分 helper 注释与文档卫生

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverFailureSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverStatusSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/SolverMemoryCleanupSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/IntermediateSymbolExpressionSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTableRegistrationSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/ConcurrentTokenTable.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModelCutSupport.kt`
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModelExportSupport.kt`
2. 注释与文档卫生结果：
   - 为 solver support 与核心 helper 文件补齐中英双语边界说明，清理口径含糊注释，未新增版权声明。
   - 本批修改文件中未发现乱码注释回流。
3. 已执行验证：
   - `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P27：工程健康第五批

状态：已完成（2026-05-22）。

目标：在 P26 已闭环的基础上继续消化低风险维护债务，重点处理文档口径、cleanup 语义完整性、benchmark trend smoke 断言强度，以及测试侧重复 converter 样板。P27 不重启 P18-P26 的迁移目标，不做大范围 API 重设，不要求真实外部 solver license 作为默认阻塞项。

总体原则：

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；如必须新增公开 helper，必须记录理由、影响范围、替代方案和验收方式。
3. cleanup 相关改动只做语义收口，不改变 heuristic 算法参数、随机性入口、终止条件和返回类型。
4. benchmark 趋势比较继续只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
5. warning debt 与测试样板清理不做全仓机械替换；优先处理已被 P6/P7 识别且风险低的重复点。
6. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
7. 写注释时遵守项目规则：中英双语；不添加版权声明。

### P27-1：交接文档命令口径修正

状态：已完成（2026-05-22）。

目标：统一 `daily.md` 中 PowerShell 命令口径，遵守 `.rules/chore.md` 中 `pwsh.exe` 的执行要求。

详细步骤：

1. 将推荐验收命令中的 `powershell -NoProfile ...` 修正为 `pwsh.exe -NoProfile ...`。
2. 保留“部分环境可临时使用 Windows PowerShell”的交接注意事项，但明确默认命令仍为 `pwsh.exe`。
3. 执行 `git diff --check`，确认文档变更无空白问题。

验收标准：

1. `daily.md` 推荐命令默认使用 `pwsh.exe`。
2. `.rules/chore.md` 与交接文档口径一致。
3. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 已将推荐验收命令中的 `powershell -NoProfile ...` 修正为 `pwsh.exe -NoProfile ...`。
2. 保留 `pwsh.exe` 不可用时可临时使用 Windows PowerShell 的交接注意事项。
3. `git diff --check` 已执行并通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P27-2：heuristic cleanup 结束语义补强

状态：已完成（2026-05-22）。

目标：评估并补强 heuristic 算法的 `cleanupAfterSolverRun()` 调用语义，使求解主流程在异常退出时也能执行统一清理。

详细步骤：

1. 检查 GA / GWO / MVO / PSO / SAA / SCA 当前 `cleanupAfterSolverRun()` 调用位置。
2. 评估是否应将主循环包入 `try/finally`，确保异常退出时也执行 cleanup。
3. 保持 callback 中断、迭代状态、随机性入口和返回值构造不变。
4. 如某个算法不适合 `finally` cleanup，记录原因。

验收标准：

1. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile` 通过。
2. 6 个 heuristic 算法的正常返回行为不变。
3. 如采用 `try/finally`，异常路径不会吞掉原始异常。
4. P6/P7 静态门禁通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/ga/GA.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/gwo/GWO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/mvo/MVO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/pso/PSO.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/saa/SAA.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/sca/SCA.kt`
2. 变更内容：
   - 将 6 个算法主流程包裹为 `try { ... } finally { cleanupAfterSolverRun() }`。
   - 保持 callback 中断、迭代状态、随机性入口、返回值构造不变。
   - 未添加 `catch`，异常路径不吞原始异常。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P27-3：benchmark trend smoke 断言增强

状态：已完成（2026-05-22）。

目标：增强 `test-compare-benchmark-results.ps1` 对目录多匹配显式 `-Dataset` 成功路径的报告字段断言，降低脚本回归风险。

详细步骤：

1. 在多 dataset 场景显式传入 `-Dataset smoke` 后，读取生成的 Markdown 报告。
2. 断言报告包含 `Input mode`、`Detected dataset` 和 `Gate policy`。
3. 保持 smoke 只使用最小 JSON fixture，不运行 medium/large benchmark。
4. 确认 CI 中 `benchmark-smoke` job 仍然轻量。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1` 通过。
2. `.github/workflows/core-refactor-guards.yml` 不引入长跑 benchmark。
3. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
2. 变更内容：
   - 在“多 dataset 显式 `-Dataset smoke` 成功路径”新增报告字段断言：
     - `Input mode: ``results-dir```
     - `Detected dataset: ``smoke```
     - `Gate policy: report only (no hard performance gate)`
   - 保持 smoke 仅使用最小 fixture，不引入 medium/large benchmark。
3. 验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。
4. CI 轻量性确认：
   - 本批未新增长跑 benchmark；现有 `benchmark-smoke` 仍为脚本级 smoke。

### P27-4：测试侧 Flt64 converter 样板收敛评估

状态：已完成（2026-05-22）。

目标：针对 P6/P7 当前报告的测试侧 `flt64Converter` 样板重复，评估能否提取低风险测试 helper，减少后续维护成本。

详细步骤：

1. 以 P6/P7 输出的 baseline 文件为入口，检查重复 `IntoValue<Flt64>` converter 是否语义完全一致。
2. 如完全一致，优先在测试源码中新增局部 test helper；不要影响 main 源 API。
3. 如存在细微差异，只记录差异和后续处理建议，不做机械合并。
4. 更新 P6/P7 baseline 或白名单前必须说明原因，避免掩盖新债务。

验收标准：

1. `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile` 通过。
2. 相关 core tests 可按改动范围选择执行。
3. P6/P7 静态门禁通过或 baseline 变化有清晰记录。
4. 不新增 main 源公开 API。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - 新增 `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/test/TestConverters.kt`
   - 更新：
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_model/BasicModelEntryTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_model/ConvertMechanismModelTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_model/QuadraticMechanismModelCutTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_model/SemanticEquivalenceTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_symbol/function/FunctionSymbolRegressionTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/intermediate_symbol/function/ProductFunctionTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/symbol_regression/linear_regression/LinearPolynomialBaselineTest.kt`
     - `ospf-kotlin-core/src/test/fuookami/ospf/kotlin/core/symbol_regression/quadratic_regression/QuadraticPolynomialBaselineTest.kt`
2. 变更内容：
   - 在测试源码新增共享 `flt64TestConverter` helper，仅作用于 test source set。
   - 将上述 8 个测试文件的本地 `flt64Converter` 样板统一收敛为 `flt64TestConverter`。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

补充收尾记录（2026-05-22）：

1. 清理 P27-4 替换后残留的未使用 `IntoValue` import；`BasicModelEntryTest.kt` 与 `QuadraticMechanismModelCutTest.kt` 因仍使用 `IntoValue.Identity` 保留必要 import。
2. 重新确认 `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile`：通过（存在既有测试 warning 与 JVM CodeHeap 警告，不影响构建结果）。
3. 重新确认 `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P28 草案：工程健康收敛大批次

状态：草案，交接给下一会话执行。

目标：在 P27 已闭环的基础上减少后续碎片迭代，把剩余低风险工程健康项合并到一个较大的收敛批次中处理。P28 不重启 P18-P27 的迁移目标，不恢复旧兼容层，不改 main 公共 API，不要求真实外部 solver license 作为默认阻塞项。

总体原则：

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；P28 默认只处理 test、script、文档、warning 与局部实现卫生。
3. 不做全仓机械 suppress；如必须新增 suppress，必须靠近最小作用域，并用中英双语注释说明可证明不变量。
4. benchmark 趋势比较继续只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
5. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
6. 写注释时遵守项目规则：中英双语；不添加版权声明。

### P28-1：P27 收尾记录校准

状态：建议优先执行。

目标：确认 P27 补充收尾记录与真实工作区一致，避免交接文档落后于实际改动。

详细步骤：

1. 核对 P27-4 的补充收尾记录是否覆盖未使用 import 清理与 `core test-compile` 复核。
2. 如后续又补做 P27 相关小修，继续在 P27-4 下追加补充记录，不混入 P28 实现项。
3. 执行 `git diff --check`。

验收标准：

1. `daily.md` 能准确说明 P27 最终状态。
2. `git diff --check` 通过。

### P28-2：example 测试侧 converter 样板继续收敛

状态：建议执行。

目标：继续消化 P6/P7 报告中 example 测试侧残留的 `flt64Converter` 样板，延续 P27-4 的测试 helper 收敛方向。

详细步骤：

1. 以 P6/P7 输出为入口，定位 example 测试侧 `flt64Converter` 样板文件。
2. 检查这些 converter 是否语义完全一致；如一致，在 example test source set 中新增局部共享 helper。
3. 替换 example 测试文件中的重复 converter 定义。
4. 如 core test helper 无法跨模块复用，不强行建立跨模块 test 依赖。
5. 清理替换后未使用 import。

验收标准：

1. example 相关测试源码不再保留可安全收敛的重复 converter 样板。
2. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 或等价可用命令通过。
3. P6/P7 静态门禁通过；如 baseline 输出变化，必须记录原因。
4. 不新增 main 源公开 API。

### P28-3：测试 warning 小清理

状态：建议执行。

目标：清理当前构建中已经暴露、低风险且语义明确的测试 warning，降低后续构建噪音。

详细步骤：

1. `LocalMonthSerializerTest` 中 deprecated `dayOfMonth` 改为推荐的 `day`。
2. `VectorTest` 中 “check for instance is always true” 的断言改为有意义的行为断言，或删除无价值断言。
3. 仅处理能明确证明不改变测试意图的 warning，不追求全仓 warning 清零。

验收标准：

1. `mvn -pl ospf-kotlin-utils,ospf-kotlin-multiarray -am "-DskipTests" test-compile` 或按实际模块拆分执行通过。
2. 上述 warning 不再出现。
3. 不削弱测试有效性。

### P28-4：framework 与 heuristic unchecked cast 局部收敛

状态：建议评估后执行。

目标：评估并尽量收敛构建中暴露的 framework 与 heuristic unchecked cast warning，降低 warning debt。

详细步骤：

1. 检查 framework 中 `LogRecord.kt`、`RequestRecord.kt` 的 unchecked cast 是否可用局部 helper 表达类型不变量。
2. 检查 heuristic `SCA.kt` 的 unchecked cast 是否可用局部封装或更窄作用域 suppress 说明不变量。
3. 不能证明类型安全时，不做机械 suppress，只记录原因和后续建议。
4. 新增注释必须中英双语。

验收标准：

1. 相关模块 compile 通过。
2. 已处理 warning 有清晰不变量说明或结构性消除。
3. 未引入运行时类型语义变化。
4. P6/P7 静态门禁通过。

### P28-5：benchmark trend 脚本健壮性补充

状态：建议执行。

目标：在不运行 medium/large benchmark 的前提下，补充 benchmark trend 脚本错误分支 smoke，降低脚本回归风险。

详细步骤：

1. 为缺失文件、非法 JSON、缺少 `primaryMetric`、baseline/current benchmark 不匹配等场景选择低成本覆盖项。
2. 保持 fixture 极小，不引入真实 JMH 长跑。
3. 断言错误信息可读，并确认成功路径报告字段不回退。
4. 如脚本错误信息需要微调，应保持向后兼容的参数语义。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1` 通过。
2. CI 中 `benchmark-smoke` 仍为脚本级 smoke，不引入 medium/large benchmark。
3. `git diff --check` 通过。

### P28-6：Maven CodeHeap 警告处理方案

状态：建议先评估，不建议贸然改全局配置。

目标：针对多次构建出现的 `CodeHeap 'non-profiled nmethods' is full` 警告，形成可执行但低风险的处理方案。

详细步骤：

1. 记录本地复现现象、触发命令和是否影响构建结果。
2. 评估是否只在文档中给出 opt-in JVM 参数建议，或是否需要调整 Maven/JVM 配置。
3. 不建议在未确认 CI 和本地收益前直接修改 `.mvn/jvm.config`。
4. 如决定改配置，必须单独记录理由、影响范围和回退方式。

验收标准：

1. `daily.md` 记录 CodeHeap 警告处理建议或实际配置变更理由。
2. 如修改构建配置，核心编译与 test-compile 至少各通过一次。
3. 不引入依赖 solver license 的默认验收项。

### P28-7：门禁与交接文档同步

状态：收尾项。

目标：将 P28 全部实际修改清单、验收命令、baseline 变化原因与残留风险同步到 `daily.md`。

详细步骤：

1. 每个 P28 子项完成后补充状态、实际修改清单、验收命令与结果。
2. 如 P6/P7 输出中的 baseline 或 violation 列表变化，记录变化原因。
3. 如 README / README_ch 被修改，确认互链仍存在且内容同步。
4. 最后执行推荐验收命令中与改动范围相关的命令。

验收标准：

1. `daily.md` 能作为下一轮会话的真实交接来源。
2. P6/P7 静态门禁通过。
3. `git diff --check` 通过。

## 推荐执行顺序

1. P28-1：先校准 P27 收尾记录，确保交接文档可信。
2. P28-2：继续收敛 example 测试侧 converter 样板，收益明确且风险低。
3. P28-3：清理低风险测试 warning，减少构建噪音。
4. P28-5：补充 benchmark trend 脚本错误分支 smoke，保持轻量。
5. P28-4：评估并局部收敛 unchecked cast warning，避免机械 suppress。
6. P28-6：评估 Maven CodeHeap 警告处理方案，不急于改全局配置。
7. P28-7：最后同步门禁结果、交接记录与残留风险。

## 推荐验收命令

按改动范围选择执行，不要求每一批都完整跑全部命令。

1. `mvn -pl ospf-kotlin-core -am "-DskipTests" compile`
2. `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile`
3. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile`
4. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am "-DskipTests" compile`
5. `mvn -pl ospf-kotlin-benchmark -am -Pbench "-DskipTests" compile`
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 -Baseline <baseline.json> -Current <current.json> -Output <trend.md>`
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 -ResultsDir <results-dir> -Dataset <dataset>`
8. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
9. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
10. `git diff --check`

## 交接注意事项

1. P28 不要求真实外部 solver license，默认以编译、静态扫描、结构测试和脚本 smoke 为验收基础。
2. `pwsh.exe` 在部分本地环境可能不可用；如不可用，可临时使用 Windows PowerShell 执行同等命令，并在验收记录中说明。
3. benchmark 仍然只做趋势报告和 artifact 留存，不设置性能硬门禁。
4. 若 P28 修改 README / README_ch，必须保持两者互链与内容同步。
5. 每个 P28 子任务完成后，应在本文件补充状态、实际修改清单、验收命令和结果，再提交独立 commit。
