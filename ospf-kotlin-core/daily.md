# P26 工程健康第四批交接（2026-05-22）

## 最新结论

`math`、`multiarray`、`quantities`、`core`、`core-plugin`、`framework`、`benchmark` 的迁移收口与 P18-P25 工程健康目标已经闭环。P25-1 到 P25-5 均已完成，P25 审阅中发现的交付整洁性问题已处理：`daily.md` 补充了公开 helper 口径，内存清理残留说明已校正，新拆分文件中的单语/乱码注释已清理，旧的 `non-default-errors*.txt` 诊断残留已删除。

P26 已完成，P26-1 到 P26-4 已闭环；P27 与 P28 也已完成，P27-1 到 P27-4、P28-1 到 P28-7 均已闭环。P28 在不重新打开 P18-P27 迁移目标的前提下，完成了 example 测试侧 converter 样板收敛、测试 warning 小清理、framework/heuristic unchecked cast 局部收敛、benchmark trend 错误分支 smoke 增强，以及 CodeHeap 警告处理方案沉淀。

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

## P28：工程健康收敛大批次

状态：已完成（2026-05-22）。

目标：在 P27 已闭环的基础上减少后续碎片迭代，把剩余低风险工程健康项合并到一个较大的收敛批次中处理。P28 不重启 P18-P27 的迁移目标，不恢复旧兼容层，不改 main 公共 API，不要求真实外部 solver license 作为默认阻塞项。

总体原则：

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；P28 默认只处理 test、script、文档、warning 与局部实现卫生。
3. 不做全仓机械 suppress；如必须新增 suppress，必须靠近最小作用域，并用中英双语注释说明可证明不变量。
4. benchmark 趋势比较继续只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
5. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
6. 写注释时遵守项目规则：中英双语；不添加版权声明。

### P28-1：P27 收尾记录校准

状态：已完成（2026-05-22）。

目标：确认 P27 补充收尾记录与真实工作区一致，避免交接文档落后于实际改动。

详细步骤：

1. 核对 P27-4 的补充收尾记录是否覆盖未使用 import 清理与 `core test-compile` 复核。
2. 如后续又补做 P27 相关小修，继续在 P27-4 下追加补充记录，不混入 P28 实现项。
3. 执行 `git diff --check`。

验收标准：

1. `daily.md` 能准确说明 P27 最终状态。
2. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 复核 P27-4 补充收尾记录，确认已覆盖：
   - 未使用 import 清理说明；
   - `mvn -pl ospf-kotlin-core -am "-DskipTests" test-compile` 复核结果；
   - `git diff --check` 复核结果。
2. 本批未新增 P27 范围的小修，P27 内容保持在 P27 段落内记录。
3. `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P28-2：example 测试侧 converter 样板继续收敛

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - 新增 `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/test/TestConverters.kt`。
   - 更新：
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/FrameworkDemoTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/HeuristicDemoTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/QuadraticTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/core_demo/CoreDemoBuildOnlyStructureTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/linear_function/ConditionalFunctionBuildOnlyStructureTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/linear_function/LinearFunctionBuildOnlyStructureTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/linear_function/LinearFunctionSmokeAssertions.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/linear_function/SemiTest.kt`
     - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/quadratic_function/SemiTest.kt`
2. 变更内容：
   - 在 example test source set 引入共享 `flt64TestConverter`。
   - 收敛 9 处重复 `flt64Converter` 样板并清理相关未使用 import。
   - 未建立跨模块 test 依赖，core 与 example 各自维护本地 test helper。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
4. baseline / violation 变化说明：
   - P6/P7 的 `P6-1` 结果仍为 PASS，violation 列表新增 `fuookami/ospf/kotlin/example/test/TestConverters.kt`。
   - 原因是 example 侧引入了与 core 侧一致的共享 helper，这属于“样板收敛后的集中定义”，不是业务回流。

### P28-3：测试 warning 小清理

状态：已完成（2026-05-22）。

目标：清理当前构建中已经暴露、低风险且语义明确的测试 warning，降低后续构建噪音。

详细步骤：

1. `LocalMonthSerializerTest` 中 deprecated `dayOfMonth` 改为推荐的 `day`。
2. `VectorTest` 中 “check for instance is always true” 的断言改为有意义的行为断言，或删除无价值断言。
3. 仅处理能明确证明不改变测试意图的 warning，不追求全仓 warning 清零。

验收标准：

1. `mvn -pl ospf-kotlin-utils,ospf-kotlin-multiarray -am "-DskipTests" test-compile` 或按实际模块拆分执行通过。
2. 上述 warning 不再出现。
3. 不削弱测试有效性。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-utils/src/test/fuookami/ospf/kotlin/utils/serialization/LocalMonthSerializerTest.kt`
   - `ospf-kotlin-multiarray/src/test/fuookami/ospf/kotlin/multiarray/VectorTest.kt`
2. 变更内容：
   - 将 `LocalMonthSerializerTest` 的 `dayOfMonth` 替换为推荐属性 `day`。
   - 将 `VectorTest` 中 `_a is DummyIndex.All` 的恒真断言改为 `lenOf` 行为断言。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-utils,ospf-kotlin-multiarray -am "-DskipTests" test-compile`：通过。
   - 目标 warning 未再出现。

### P28-4：framework 与 heuristic unchecked cast 局部收敛

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/log/LogRecord.kt`
   - `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/persistence/RequestRecord.kt`
   - `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic/src/main/fuookami/ospf/kotlin/core/solver/heuristic/sca/SCA.kt`
2. 变更内容：
   - 在 framework 侧将运行时 serializer cast 收敛到局部 helper，并补充中英双语不变量注释。
   - 在 SCA 中将 population / best / token-bound 的 unchecked cast 收敛到最小作用域 helper，并补充中英双语不变量注释。
   - 未改变公开 API、算法参数、终止条件或返回类型。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-framework,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。

### P28-5：benchmark trend 脚本健壮性补充

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
2. 变更内容：
   - 补充错误分支 smoke：
     - 缺失 baseline 文件；
     - 非法 JSON；
     - 缺少 `primaryMetric`；
     - baseline/current benchmark 不匹配（报告 `missing in current` / `new benchmark in current`）。
   - 保持成功路径字段断言与目录模式断言不回退。
   - 保持 fixture 极小，不引入真实 JMH 长跑。
3. 验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P28-6：Maven CodeHeap 警告处理方案

状态：已完成（2026-05-22，完成评估与建议沉淀）。

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

实际执行记录（2026-05-22）：

1. 复现记录：
   - 在 `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 与 `mvn -pl ospf-kotlin-framework,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-heuristic -am "-DskipTests" compile` 中均可见：
     - `CodeHeap 'non-profiled nmethods' is full`
   - 构建结果仍为 SUCCESS，属于 JVM JIT 编译缓存容量警告，不是编译失败。
2. 处理决策：
   - 本批不改 `.mvn/jvm.config`，避免在未验证 CI 收益前引入全局环境副作用。
   - 采用 opt-in 建议：仅在本地长链路编译时按需设置 `MAVEN_OPTS`，例如：
     - `-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m`
3. 回退方式：
   - 该建议为环境变量级临时配置，删除 `MAVEN_OPTS` 即可回退。

### P28-7：门禁与交接文档同步

状态：已完成（2026-05-22）。

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

实际执行记录（2026-05-22）：

1. 已回填 P28-1 至 P28-6 的状态、修改清单与验收结果。
2. 已记录 P6/P7 baseline/violation 变化原因（example test helper 新增）。
3. 本批未改 README / README_ch，互链不受影响。
4. 已执行收尾验收：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P29 工程健康收尾批次

状态：已完成（2026-05-22）。

目标：在 P28 已闭环的基础上继续减少后续迭代轮次，集中处理 guard 输出口径、benchmark 脚本显式输入校验、framework serializer 回归测试、example warning、CodeHeap opt-in 验证与剩余 warning 候选池。P29 不重启 P18-P28 的迁移目标，不恢复旧兼容层，不改 main 公共 API，不引入 solver license 依赖。

总体原则：

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；P29 默认只处理 test、script、文档、warning 与局部实现卫生。
3. 不做全仓机械 suppress；如必须新增 suppress，必须靠近最小作用域，并用中英双语注释说明可证明不变量。
4. benchmark 趋势比较继续只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
5. CodeHeap 处理优先采用 opt-in 验证，不贸然修改 `.mvn/jvm.config`。
6. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
7. 写注释时遵守项目规则：中英双语；不添加版权声明。

### P29-1：P6/P7 guard 输出口径优化

状态：已完成（2026-05-22）。

目标：减少 `PASS` 结果中继续打印 `Violations` 的误读风险，尤其是 P28 后 `P6-1` 只剩集中 test helper 的场景。

详细步骤：

1. 检查 `check-c8-guards.ps1` 中 P6/P7 输出 `Violations` 的逻辑。
2. 对 baseline 内允许存在的匹配项改用更中性的文案，例如 `Tracked occurrences`。
3. 如存在明确允许的集中 helper，可考虑把 `TestConverters.kt` 纳入说明或 allowlist，但不要掩盖真实回流。
4. 更新 `daily.md` 记录输出口径变化，避免后续会话误判。

验收标准：

1. P6/P7 静态门禁通过。
2. guard 输出不再用 `Violations` 描述已经允许且未超 baseline 的条目。
3. 不削弱 P6/P7 对新增样板回流的拦截能力。
4. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-core/scripts/check-c8-guards.ps1`
2. 变更内容：
   - 在 `P6-1` 输出中按 `converterNewViolations` 分支选择文案：
     - `> 0`：保持 `Violations`；
     - `= 0` 且存在 baseline 命中：改为 `Tracked occurrences`。
3. 验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
4. 口径说明：
   - 该调整仅影响展示文案，不改变 baseline、计数与拦截阈值。

### P29-2：benchmark 脚本显式输入校验

状态：已完成（2026-05-22）。

目标：将 `compare-benchmark-results.ps1` 对非法 JSON、缺字段等输入错误的处理从 PowerShell 运行时异常改为稳定、可读的显式错误信息。

详细步骤：

1. 为 JSON 解析失败增加明确错误信息，包含输入文件路径。
2. 为缺少 `benchmark`、`primaryMetric`、`score`、`scoreError`、`scoreUnit` 等关键字段增加显式校验。
3. 保持现有参数语义兼容，不改变成功报告格式。
4. 同步增强 `test-compare-benchmark-results.ps1` 中对应错误分支断言。
5. 保持 fixture 极小，不引入真实 JMH 长跑。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1` 通过。
2. 成功路径报告字段不回退。
3. 错误分支信息稳定且可读。
4. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-benchmark/scripts/compare-benchmark-results.ps1`
   - `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
2. 变更内容：
   - 新增 `Get-OptionalPropertyValue`，避免 strict mode 下直接访问缺失属性触发非预期异常。
   - `Read-BenchmarkResults` 增加显式校验：
     - 非法 JSON（含路径）；
     - 空 payload 与空数组；
     - 缺少 `benchmark`；
     - 缺少 `primaryMetric`；
     - 缺少 `primaryMetric.score`；
     - 缺少 `primaryMetric.scoreError`；
     - 缺少 `primaryMetric.scoreUnit`。
   - smoke 断言同步更新，并补缺少 `benchmark` 与缺少 `primaryMetric.score` 用例。
3. 验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。

### P29-3：framework serializer 回归测试

状态：已完成（2026-05-22）。

目标：为 P28 中 framework runtime serializer cast helper 的不变量补轻量回归测试，避免只靠 suppress 注释承载风险。

详细步骤：

1. 为 `LogRecordPO` 的序列化路径补测试，覆盖 value 写入/读取或至少写出 payload 的基本稳定性。
2. 为 `RequestRecordPO` 与 `ResponseRecordPO` 的 request/response 序列化路径补测试。
3. 测试只覆盖 serializer helper 的边界行为，不扩大框架功能。
4. 如现有依赖或构造成本较高，优先选择最小 DTO / PO fixture。

验收标准：

1. `mvn -pl ospf-kotlin-framework -am "-DskipTests" test-compile` 通过。
2. 如新增测试可低成本运行，执行对应测试或模块测试。
3. 不引入外部服务、数据库或 solver license 依赖。
4. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - 新增 `ospf-kotlin-framework/src/test/fuookami/ospf/kotlin/framework/persistence/SerializerRuntimeHelperRegressionTest.kt`
2. 覆盖内容：
   - `LogRecordPO` 的 `rpo` 序列化/反序列化回读；
   - `RequestRecordPO` 的 `rpo` 序列化/反序列化回读；
   - `ResponseRecordPO` 的 `rpo` 序列化/反序列化回读。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-framework -am "-DskipTests" test-compile`：通过。
   - `mvn -pl ospf-kotlin-framework -am "-Dtest=SerializerRuntimeHelperRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：通过（`Tests run: 3, Failures: 0, Errors: 0, Skipped: 0`）。

### P29-4：example warning 清理

状态：已完成（2026-05-22）。

目标：清理 P28 验收中继续暴露的低风险 example warning，进一步降低构建噪音。

详细步骤：

1. 清理 `QuadraticFunctionSolveTest.kt` 中两处 “Check for instance is always true” 的恒真类型断言，改为行为断言或删除无价值断言。
2. 评估 example main 源 deprecated `Instant` typealias warning，优先处理低风险 import/typealias 使用点。
3. 不做大范围业务模型重写，不改变 example 语义。
4. 如 `Instant` warning 涉及大量业务时间类型迁移，记录保留原因与后续建议，不强行清零。

验收标准：

1. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 通过。
2. 已处理 warning 不再出现。
3. 未削弱测试有效性，未改变 example 业务语义。
4. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 实际修改清单：
   - `ospf-kotlin-example/src/test/fuookami/ospf/kotlin/example/quadratic_function/QuadraticFunctionSolveTest.kt`
2. 变更内容：
   - 清理两处恒真类型断言 `output is FeasibleSolverOutput<*>`。
   - 新增局部 helper `asFeasibleOutput(output: Any)`，把 `@Suppress("UNCHECKED_CAST")` 收敛到最小作用域。
   - 将断言改为行为断言 `output.solution.isNotEmpty()`。
3. 验收命令与结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。
4. 保留项说明：
   - example main 源仍有较多 `Instant` typealias deprecated warning（集中在 demo2/demo4 与其 domain/infrastructure），涉及范围较大；P29 不做大规模时间类型迁移，转入候选池。

### P29-5：CodeHeap opt-in 验证

状态：已完成（2026-05-22）。

目标：验证 P28 记录的 `MAVEN_OPTS` opt-in CodeHeap 参数是否能稳定消除本地长链路构建中的 CodeHeap 警告。

详细步骤：

1. 使用临时环境变量运行一条长链路构建，例如：
   - `MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m"`
2. 对比是否仍出现 `CodeHeap 'non-profiled nmethods' is full` 警告。
3. 如有效，只更新文档/交接建议；暂不改 `.mvn/jvm.config`。
4. 如无效，记录结果与可能原因，不继续扩大范围。

验收标准：

1. `daily.md` 记录 opt-in 参数验证结果。
2. 验证命令构建成功或失败原因清晰。
3. 不修改全局 JVM 配置，除非单独记录充分理由、影响范围和回退方式。

实际执行记录（2026-05-22）：

1. 验证命令：
   - `$env:MAVEN_OPTS='-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m'`
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`
2. 结果：
   - 构建成功（`P29_MVN_EXIT=0`）。
   - 警告仍存在（`P29_CODEHEAP_HITS=2`，见 `tmp-p29-codeheap-optin.log`）。
3. 结论：
   - 当前 opt-in 参数对本仓长链路编译不充分，P29 不改 `.mvn/jvm.config`，保留为后续候选。

### P29-6：残留 warning 扫描与候选池

状态：已完成（2026-05-22）。

目标：梳理 P29 后剩余 warning，区分已清理、仍保留、后续候选，避免后续会话重复扫描。

详细步骤：

1. 汇总目标构建中的剩余 warning。
2. 标注哪些 warning 已在 P29 清理，哪些因风险或范围原因保留。
3. 将可后续处理但不适合 P29 的项放入候选池。
4. 回填 P29 实际修改清单、验收命令、残留风险到 `daily.md`。

验收标准：

1. `daily.md` 能作为下一轮会话的真实交接来源。
2. P6/P7 静态门禁通过。
3. `git diff --check` 通过。

实际执行记录（2026-05-22）：

1. 已清理项：
   - P6/P7 输出误读口径（`Violations` -> `Tracked occurrences`，仅 baseline 命中时）。
   - benchmark 脚本输入校验缺口（JSON/字段显式错误）。
   - framework serializer runtime helper 回归测试缺口。
   - example 测试恒真类型断言 warning（`QuadraticFunctionSolveTest.kt`）。
2. 保留项与原因：
   - example main `Instant` typealias deprecated warning：跨 demo2/demo4 大范围迁移，超出 P29 风险边界。
   - example demo2 `BendersSolver.kt` 一处 unchecked cast warning：需结合 demo2 求解流程与输出约束审视，不在 P29 直接改。
   - demo4 两处 `shadowPriceMap` 命名不一致 warning：属于接口命名一致性问题，需评估影响后单独处理。
   - 长链路 Maven CodeHeap warning：opt-in 参数验证后仍出现，暂不做全局 JVM 配置变更。
3. 建议候选池（下一轮可选）：
   - 候选 A：分批迁移 example demo2/demo4 的时间类型到 `kotlin.time.Instant` 或统一桥接层；每批控制在单模块/单上下文。
   - 候选 B：为 `BendersSolver.kt` unchecked cast 增加局部安全封装或结构性类型收窄。
   - 候选 C：统一 `CGPipeline` override 参数命名，消除 named-argument 风险 warning。
   - 候选 D：在不改 `.mvn/jvm.config` 前提下尝试其他 opt-in CodeCache 组合（例如再增 `ReservedCodeCacheSize`）并留存对比日志。
4. 收尾验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P30：example 与构建体验收尾批次

状态：已完成（2026-05-22）。

目标：在 P29 已闭环的基础上批量处理候选池中仍有价值的残留项，重点覆盖 example 时间类型 warning、demo2 Benders 输出类型收窄、demo4 CGPipeline 参数命名一致性、benchmark 数值边界 smoke、warning 汇总脚本/报告、CodeHeap opt-in 再验证与候选池重分层。P30 不重启 P18-P29 的迁移目标，不恢复旧兼容层，不改 core 主线公共 API，不引入 solver license 依赖。

总体原则：

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不破坏既有公开 API；P30 默认只处理 example、script、test、文档、warning 与构建体验。
3. 不做全仓机械 suppress；如必须新增 suppress，必须靠近最小作用域，并用中英双语注释说明可证明不变量。
4. example 时间类型迁移必须分批评估，不做跨 demo 的大范围业务模型重写。
5. benchmark 趋势比较继续只生成报告和 artifact，不把绝对性能数值作为跨机器硬失败条件。
6. CodeHeap 处理继续优先采用 opt-in 验证，不贸然修改 `.mvn/jvm.config`。
7. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。
8. 写注释时遵守项目规则：中英双语；不添加版权声明。

### P30-1：demo4 CGPipeline 参数命名一致性修正

状态：建议优先执行。

目标：消除 demo4 中 override 参数名与 supertype `shadowPriceMap` 不一致导致的 named-argument 风险 warning。

详细步骤：

1. 定位 P29 验收输出中的 demo4 warning 文件，例如 `FleetBalanceLimit.kt` 与 `FlightLinkLimit.kt`。
2. 将 override 参数名与 supertype 保持一致。
3. 搜索 named-argument 调用点，确认没有依赖旧参数名的调用。
4. 不改变 CGPipeline 行为与返回语义。

验收标准：

1. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 通过。
2. 对应 named-argument warning 不再出现。
3. 不改变 example demo4 业务语义。
4. `git diff --check` 通过。

### P30-2：demo2 BendersSolver 输出类型收窄

状态：建议执行。

目标：收敛 demo2 `BendersSolver.kt` 中 `SolverOutput?` 到 `FeasibleSolverOutput<Flt64>` 的 unchecked cast warning。

详细步骤：

1. 阅读 demo2 Benders 求解流程，确认输出状态与 feasible output 的关系。
2. 优先使用局部 helper、状态检查或结构性类型收窄表达不变量。
3. 如必须保留 suppress，必须靠近最小作用域，并补中英双语不变量说明。
4. 不改 solver API，不改变 demo2 求解语义。

验收标准：

1. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 通过。
2. `BendersSolver.kt` unchecked cast warning 不再出现，或保留原因被明确记录。
3. 不引入 solver license 作为默认验收依赖。
4. `git diff --check` 通过。

### P30-3：example Instant warning 分批迁移

状态：建议执行，允许按实际风险缩小范围。

目标：处理 example demo2/demo4 中 deprecated `Instant` typealias warning，优先完成低风险迁移或形成清晰保留策略。

详细步骤：

1. 先扫描 demo2/demo4 的 `Instant` warning 来源，区分 import/typealias、DTO、domain model、infrastructure 边界。
2. 优先处理可直接迁移到 `kotlin.time.Instant` 的低风险位置。
3. 如发现跨模块序列化、持久化或 DTO 兼容语义较复杂，先建立统一桥接入口或记录保留原因。
4. 建议 P30 至少完成 demo2；如 demo4 牵涉过大，可留入候选池。
5. 不做跨 demo 的业务时间模型重写。

验收标准：

1. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 通过。
2. 已处理范围内的 deprecated `Instant` warning 不再出现。
3. 未改变 DTO 序列化、domain model 与 example 行为语义。
4. `daily.md` 记录完成范围与保留范围。

### P30-4：benchmark 数值边界 smoke

状态：建议执行。

目标：在 P29 字段校验基础上，为 benchmark trend 脚本补充数值边界输入 smoke，提升脚本稳健性。

详细步骤：

1. 覆盖非数字 score / scoreError、空 unit、重复 benchmark key 等低成本场景。
2. 明确哪些场景应失败，哪些场景应生成报告并标注 note。
3. 保持 fixture 极小，不引入真实 JMH 长跑。
4. 不改变成功报告的既有字段语义。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1` 通过。
2. 新增错误或报告分支有稳定断言。
3. CI 中 `benchmark-smoke` 仍为脚本级 smoke，不引入 medium/large benchmark。
4. `git diff --check` 通过。

### P30-5：warning 汇总脚本或报告

状态：建议评估后执行。

目标：降低人工从 Maven 输出中整理 warning 的成本，形成轻量 warning 汇总报告或脚本。

详细步骤：

1. 评估是否新增脚本从构建日志中提取 `[WARNING]` 行并按模块/文件聚合。
2. 脚本只生成报告，不设硬门禁。
3. 如新增脚本，使用 `pwsh.exe` 兼容方式，避免依赖外部工具。
4. 如不新增脚本，也应在 `daily.md` 中给出人工汇总格式。

验收标准：

1. warning 汇总方式可复用，且不阻塞构建。
2. 如新增脚本，脚本 smoke 通过。
3. `git diff --check` 通过。

### P30-6：CodeHeap opt-in 参数再验证

状态：建议执行。

目标：在 P29 opt-in 参数无效的基础上，尝试其他 CodeCache 参数组合并留存对比结果。

详细步骤：

1. 尝试更大的 opt-in 组合，例如提高 `ReservedCodeCacheSize`，并按需调整 profiled/non-profiled 分配。
2. 使用长链路构建验证是否仍出现 `CodeHeap 'non-profiled nmethods' is full`。
3. 只记录 opt-in 方案和对比结果，不直接修改 `.mvn/jvm.config`。
4. 如仍无效，停止扩大范围并记录候选原因。

验收标准：

1. `daily.md` 记录参数组合、验证命令、构建结果与 CodeHeap 命中情况。
2. 不修改全局 JVM 配置，除非单独记录充分理由、影响范围和回退方式。
3. 验证失败或无效时不影响 P30 其他交付。

### P30-7：daily.md 收尾与候选池重分层

状态：收尾项。

目标：将 P30 后仍保留的项按风险和处理方式分层，避免后续会话重复扫描和重复决策。

详细步骤：

1. 回填 P30 各子项实际修改清单、验收命令与结果。
2. 将剩余 warning / 构建体验问题分成：
   - 可安全继续清理；
   - 需要业务语义确认；
   - 构建环境问题；
   - 暂不处理。
3. 标注下一轮建议是否继续、暂停或转入 issue。
4. 最后执行 P6/P7 与 `git diff --check`。

验收标准：

1. `daily.md` 能作为下一轮会话的真实交接来源。
2. P6/P7 静态门禁通过。
3. `git diff --check` 通过。

## 推荐执行顺序

1. P30-1：先修 demo4 CGPipeline 参数命名一致性，低风险且能快速减少 warning。
2. P30-2：收敛 demo2 `BendersSolver.kt` unchecked cast。
3. P30-3：分批处理 example `Instant` warning，至少尝试完成 demo2。
4. P30-4：补 benchmark 数值边界 smoke。
5. P30-5：评估并建立 warning 汇总脚本或报告。
6. P30-6：再验证 CodeHeap opt-in 参数组合。
7. P30-7：最后同步 daily.md 与候选池分层。

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

1. P30 不要求真实外部 solver license，默认以编译、静态扫描、结构测试和脚本 smoke 为验收基础。
2. `pwsh.exe` 在部分本地环境可能不可用；如不可用，可临时使用 Windows PowerShell 执行同等命令，并在验收记录中说明。
3. benchmark 仍然只做趋势报告和 artifact 留存，不设置性能硬门禁。
4. 若后续批次修改 README / README_ch，必须保持两者互链与内容同步。
5. 每个 P30 子任务完成后，应在本文件补充状态、实际修改清单、验收命令和结果，再提交独立 commit。

## P30 实际执行记录（2026-05-22）

### P30-1：demo4 CGPipeline 参数命名一致性修正

状态：已完成（2026-05-22）。

1. 实际修改清单：
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_compilation/service/limits/FleetBalanceLimit.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/bunch_compilation/service/limits/FlightLinkLimit.kt`
2. 变更内容：
   - 将 `refresh(...)` override 参数名从 `map` 统一为 `shadowPriceMap`，并同步函数体引用。
3. 验收结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过（与 P30-2/P30-3 一并验证）。

### P30-2：demo2 BendersSolver 输出类型收窄

状态：已完成（2026-05-22）。

1. 实际修改清单：
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/infrastructure/BendersSolver.kt`
2. 变更内容：
   - 新增 `requireFeasibleMasterOutput(output: SolverOutput): Ret<FeasibleSolverOutput<Flt64>>`，通过结构性检查收窄 master 输出类型。
   - 替换原有 unchecked cast，并补 `masterOutput == null` 显式失败分支。
3. 验收结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。

### P30-3：example Instant warning 分批迁移

状态：已完成（2026-05-22，按风险缩小到 demo2 范围）。

1. 实际修改清单：
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/FullLoadApplication.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/PredistributionApplication.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/WeightRecommendationApplication.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/domain/stowage/model/Flight.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/domain/stowage/model/Item.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/infrastructure/dto/RunningHeartBeatDTO.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/domain/loading_effectiveness/service/limits/ItemAheadLoadLimit.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo2/domain/loading_effectiveness/service/limits/ItemReweighNeededLimit.kt`
2. 变更内容：
   - 将 demo2 范围内可低风险迁移的 `kotlinx.datetime.Instant` 改为 `kotlin.time.Instant`。
   - 对涉及 `kotlin.time.Instant` 的 `loading_effectiveness` limit 文件补 `@file:OptIn(kotlin.time.ExperimentalTime::class)`。
3. 验收结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。
4. 保留范围：
   - demo4 `Instant` deprecated warning 仍保留，转入后续候选池。

### P30-4：benchmark 数值边界 smoke

状态：已完成（2026-05-22）。

1. 实际修改清单：
   - `ospf-kotlin-benchmark/scripts/compare-benchmark-results.ps1`
   - `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
2. 变更内容：
   - 脚本新增 `score` / `scoreError` 数值类型显式校验，失败时给出可读错误信息。
   - 脚本新增重复 benchmark key（参数归一后）显式失败，避免静默覆盖。
   - smoke 新增用例：非数字 `score`、非数字 `scoreError`、空 `scoreUnit`、重复 benchmark key。
3. 验收结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。

### P30-5：warning 汇总脚本或报告

状态：已完成（2026-05-22）。

1. 实际修改清单：
   - 新增 `ospf-kotlin-core/scripts/summarize-maven-warnings.ps1`
   - 新增 `ospf-kotlin-core/scripts/test-summarize-maven-warnings.ps1`
2. 变更内容：
   - 新增 Maven warning 汇总脚本：读取构建日志，按 `module + file-hint`（无文件时降级到归一化消息）聚合并输出报告。
   - 提供 Markdown/Text 输出，可选 `-Output` 落盘。
   - 新增最小 smoke 用例，断言总 warning 数、聚合组数与重复项合并行为。
3. 验收结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\tmp-p30-codeheap-optin.log -Output .\tmp-p30-warning-summary.md -Format markdown`：通过（本地报告生成成功）。

### P30-6：CodeHeap opt-in 参数再验证

状态：已完成（2026-05-22）。

1. 验证命令：
   - `$env:MAVEN_OPTS='-XX:ReservedCodeCacheSize=768m -XX:NonProfiledCodeHeapSize=256m -XX:ProfiledCodeHeapSize=256m'`
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`
2. 结果：
   - 构建成功（`P30_MVN_EXIT=0`）。
   - CodeHeap 告警仍存在（`P30_CODEHEAP_HITS=2`，日志：`tmp-p30-codeheap-optin.log`）。
   - 同时出现 `CodeHeap 'profiled nmethods' is full` 与 `CodeHeap 'non-profiled nmethods' is full`。
3. 结论：
   - 本轮更大 opt-in 组合仍不足以彻底消除长链路构建告警。
   - 不修改 `.mvn/jvm.config`，保留为构建环境候选项。

### P30-7：daily.md 收尾与候选池重分层

状态：已完成（2026-05-22）。

1. 残留项分层：
   - 可安全继续清理：
     - example demo4 `Instant` deprecated warning（可按文件或子域继续分批迁移）。
     - 部分 framework named-argument `shadowPriceMap` 命名不一致 warning（不在本轮 example 范围内）。
   - 需要业务语义确认：
     - demo4 时间模型若涉及跨模型/序列化兼容改造时的统一迁移策略。
   - 构建环境问题：
     - 长链路构建中 CodeHeap 告警（opt-in 提升后仍命中）。
   - 暂不处理：
     - framework 既有 unchecked cast 与 named-argument warning（超出 P30 目标范围）。
2. 收尾验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P31：warning 与构建体验综合收束批次

状态：已完成（2026-05-22）。

目标：在 P30 已完成且可提交的基础上扩大单轮处理范围，集中收束仍有价值的 warning、benchmark 趋势脚本边界、warning 汇总可观测性和构建环境记录。P31 仍不恢复旧兼容层，不修改 core 主线公共 API，不引入外部 solver license 依赖，不直接改 `.mvn/jvm.config`；默认以 example/framework 局部清理、脚本 smoke、CI artifact 与交接记录为主要交付。

### P31 总边界

1. 默认允许修改：
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/**`
   - `ospf-kotlin-framework-gantt-scheduling/**`
   - `ospf-kotlin-benchmark/scripts/**`
   - `ospf-kotlin-core/scripts/**`
   - `.github/workflows/**`
   - `ospf-kotlin-core/daily.md`
2. 默认不修改：
   - core/math 主线公共 API；
   - solver 插件 license 相关配置；
   - `.mvn/jvm.config`；
   - README / README_ch，除非本轮确实需要同步文档入口。
3. 若 demo4 时间模型迁移牵涉跨模块序列化兼容或业务含义，应先缩小到可编译的局部文件，并在 P31 记录保留原因。
4. P31 的 CI / script 增强只做报告与 artifact 留存，不设置 warning 或性能硬门禁。

### P31-1：example demo4 Instant deprecated warning 分批迁移

状态：已完成（2026-05-22）。

目标：处理 P30 保留的 demo4 `kotlinx.datetime.Instant` deprecated warning，优先覆盖构建日志中已暴露的 task/model 与 infrastructure 时间别名相关文件。

建议优先文件：

1. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/Aircraft.kt`
2. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightCycle.kt`
3. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightLeg.kt`
4. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightTask.kt`
5. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightTaskBunch.kt`
6. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/Maintenance.kt`
7. `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/infrastructure/Instant.kt`

详细步骤：

1. 先用 `rg -n "kotlinx\.datetime\.Instant|typealias Instant|fromEpochMilliseconds" ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4` 确认实际残留面。
2. 优先将局部模型字段迁移到 `kotlin.time.Instant`，必要时补最小范围的 `@file:OptIn(kotlin.time.ExperimentalTime::class)`。
3. 若 `infrastructure/Instant.kt` 是 demo4 自定义兼容入口，优先评估能否收窄或改为 `kotlin.time.Instant` 别名；若会扩大影响，保留并记录原因。
4. 避免在 demo4 时间语义不清晰时做跨模型大重构。

验收标准：

1. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile` 通过。
2. demo4 目标文件中的 `kotlinx.datetime.Instant` deprecated warning 明显下降；若未清零，`daily.md` 记录剩余文件和原因。
3. 不引入新的 source set、兼容层或旧 frontend/backend import。

### P31-2：framework CGPipeline shadowPriceMap 参数 warning 清理

状态：已完成（2026-05-22）。

目标：处理 P30 复核日志中仍出现的 framework named-argument warning，优先清理 `CGPipeline.refresh(...)` override 参数名与 supertype 不一致的问题。

建议优先文件：

1. `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/service/limits/ConsumptionQuantityConstraint.kt`
2. `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/service/limits/ProduceQuantityConstraint.kt`

详细步骤：

1. 使用 `rg -n "override suspend fun refresh\(|shadowPriceMap|named arguments" ospf-kotlin-framework-gantt-scheduling` 定位同类 warning。
2. 将 override 参数名统一为 `shadowPriceMap`，并同步函数体引用。
3. 仅处理命名一致性，不顺手改 unchecked cast 或业务逻辑。
4. 若出现更多同类文件，可在同一批中一并处理，但应保持变更机械、可审查。

验收标准：

1. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile` 通过。
2. 如改动涉及更多 gantt-scheduling 模块，补跑对应模块 compile。
3. named-argument warning 数量下降，并在 `daily.md` 记录剩余项。

### P31-3：warning 汇总脚本接入 CI artifact

状态：已完成（2026-05-22）。

目标：将 P30 新增的 `summarize-maven-warnings.ps1` 从本地工具提升为 CI 可观测性工具，生成 artifact 或 summary，减少后续人工翻 Maven 日志。

详细步骤：

1. 搜索现有 workflow：`rg -n "check-c8-guards|mvn |upload-artifact|benchmark|warnings" .github/workflows ospf-kotlin-core/scripts`。
2. 选择最合适的现有 workflow 接入 warning summary，优先复用已有构建日志文件；如当前 workflow 没有日志文件，使用最小侵入方式 tee 到临时 log。
3. 生成 `maven-warning-summary.md`，通过 `actions/upload-artifact` 上传，或写入 GitHub step summary。
4. CI 行为只报告，不失败，不把 warning 当硬门禁。
5. 保留脚本 smoke：`test-summarize-maven-warnings.ps1`。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1` 通过。
2. workflow yaml 语法和路径静态检查通过，至少用 `rg` 确认脚本路径、报告路径、artifact 名称一致。
3. `daily.md` 记录接入的 workflow、artifact 名称和是否为非阻塞报告。

### P31-4：benchmark trend smoke 报告语义补强

状态：已完成（2026-05-22）。

目标：在 P30 数值边界基础上继续补报告语义断言，覆盖常见趋势解释场景，避免后续趋势脚本回归。

建议新增 smoke：

1. baseline/current `scoreUnit` 不一致时报告 `unit changed`。
2. baseline 缺失 benchmark、current 新增 benchmark 时报告 `new benchmark in current`。
3. current 缺失 benchmark 时报告 `missing in current`。
4. baseline score 为 0 时 percent delta 为 `n/a`。
5. 可选：`NaN` / `Infinity` 输入若仍允许，应明确报告为 `n/a`；若决定不允许，应在 `Convert-MetricDouble` 中显式失败并补 smoke。

详细步骤：

1. 扩展 `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`，尽量复用现有临时 JSON helper。
2. 只在必要时改 `compare-benchmark-results.ps1`，优先补 smoke 覆盖既有行为。
3. 若调整 `NaN` / `Infinity` 策略，应在 `daily.md` 明确记录取舍。

验收标准：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1` 通过。
2. 新增 smoke 断言报告文本中的关键字段，而不是只断言命令成功。
3. benchmark 趋势仍不设置性能硬门禁。

### P31-5：CodeHeap 告警专项记录与 warning summary 联动

状态：已完成（2026-05-22）。

目标：在不修改默认 JVM 配置的前提下，将 CodeHeap 告警从“散落在 Maven 输出里”变为可记录、可比较的构建环境事实。

详细步骤：

1. 复用或扩展 `summarize-maven-warnings.ps1` 的输出，确认 CodeHeap 相关 warning 是否会进入报告。
2. 如 Maven/HotSpot warning 不带 `[WARNING]` 前缀导致脚本抓不到，可选择：
   - 在 P31 仅记录“暂不覆盖 HotSpot VM warning”的原因；
   - 或扩展脚本识别 `Java HotSpot(TM) 64-Bit Server VM warning:` 与 `[warning][codecache]` 行，并补 smoke。
3. 不修改 `.mvn/jvm.config`。
4. 如复跑长链路构建，记录命令、是否设置 `MAVEN_OPTS`、CodeHeap 命中次数和命中阶段。

验收标准：

1. `daily.md` 明确记录 CodeHeap 是否被 summary 覆盖。
2. 若扩展脚本，`test-summarize-maven-warnings.ps1` 必须新增并通过 CodeHeap warning smoke。
3. 不因 CodeHeap 告警让 CI 失败。

### P31-6：daily.md 收尾与候选池重分层

状态：已完成（2026-05-22）。

目标：让 P31 完成后能直接作为下一会话交接来源，避免继续重复扫描 P30/P31 已判断过的问题。

详细步骤：

1. 回填 P31 各子项实际修改清单、验收命令与结果。
2. 将剩余项重新分层：
   - 已清零或显著下降；
   - 可安全继续批量清理；
   - 需要业务语义确认；
   - 构建环境问题；
   - 暂不处理。
3. 明确是否建议继续 P32；若继续，尽量给出更大范围但边界清晰的草案。

推荐执行顺序：

1. P31-4：先补 benchmark trend smoke，低风险且反馈快。
2. P31-2：清理 framework `shadowPriceMap` 命名 warning。
3. P31-1：迁移 demo4 Instant warning，遇到业务边界时及时缩小范围。
4. P31-3：接入 warning summary CI artifact。
5. P31-5：处理或记录 CodeHeap 与 summary 的覆盖关系。
6. P31-6：最后同步 daily.md 与候选池。

推荐验收命令：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`
2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile`
4. `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
7. `git diff --check`

交接注意事项：

1. P31 可以跨 example、framework、benchmark、core scripts 和 CI workflow，但每个子项都要保持机械、可验收。
2. 不要把 warning summary 变成硬门禁；本轮目标是可观测性，不是扩大 CI 阻断面。
3. demo4 Instant 迁移若影响序列化或业务时间语义，优先记录保留项，不要强行一口气重构。
4. framework unchecked cast 暂不纳入 P31，除非它和 `shadowPriceMap` 命名修正处在同一行且无法分离。
5. 每个 P31 子任务完成后，应在本文件补充状态、实际修改清单、验收命令和结果，再提交独立 commit。

## P31 实际执行记录（2026-05-22）

### P31-1：example demo4 Instant deprecated warning 分批迁移

1. 实际修改清单：
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/Aircraft.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightCycle.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightLeg.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightTask.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/FlightTaskBunch.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/domain/task/model/Maintenance.kt`
   - `ospf-kotlin-example/src/main/fuookami/ospf/kotlin/example/framework_demo/demo4/infrastructure/Instant.kt`
2. 变更内容：
   - 将上述文件中的 `Instant` 来源从 `kotlinx.datetime.Instant` 收敛到 `kotlin.time.Instant`。
   - 对仍需 `kotlinx.datetime` 的类型（例如 `LocalDate`、`TimeZone`、`toJavaZoneId`）保留最小 import，不做跨模型重构。
3. 验收结果：
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。
   - 本轮编译日志中未再出现 demo4 这批 `Instant` deprecated warning。

### P31-2：framework CGPipeline shadowPriceMap 参数 warning 清理

1. 实际修改清单：
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/service/limits/ConsumptionQuantityConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/produce/service/limits/ProduceQuantityConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/resource/service/limits/ResourceCapacityConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/ExecutorCompilationConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskAdvanceEarliestEndTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskAdvanceTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskCompilationConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskDelayLastEndTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskDelayTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskOverMaxAdvanceTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskOverMaxDelayTimeConstraint.kt`
   - `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/task_compilation/service/limits/TaskStepConflictConstraint.kt`
2. 变更内容：
   - 将 `override fun refresh(...)` 的参数名从 `map` 统一为 `shadowPriceMap`。
   - 同步函数体中的 `map.put(...)` 调用为 `shadowPriceMap.put(...)`。
   - 不改 unchecked cast 与业务逻辑。
3. 验收结果：
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile`：通过。
   - 该链路日志中已不再出现 `The corresponding parameter in the supertype 'CGPipeline' is named 'shadowPriceMap'` warning。

### P31-3：warning 汇总脚本接入 CI artifact

1. 实际修改清单：
   - `.github/workflows/core-refactor-guards.yml`
2. 变更内容：
   - 在 `benchmark-smoke` job 的 `Compile Benchmark Module` 步骤中 tee Maven 输出到 `ospf-kotlin-core/target/ci/maven-benchmark-compile.log`。
   - 新增 `Build Maven Warning Summary` 步骤，调用 `summarize-maven-warnings.ps1` 生成 `ospf-kotlin-core/target/ci/maven-warning-summary.md`，并追加到 `GITHUB_STEP_SUMMARY`。
   - 将 summary 与 compile log 一并纳入 `benchmark-smoke-results` artifact。
   - 保持非阻塞报告语义，不将 warning 变成硬门禁。
3. 验收结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
   - workflow 路径与产物路径静态核对通过。

### P31-4：benchmark trend smoke 报告语义补强

1. 实际修改清单：
   - `ospf-kotlin-benchmark/scripts/test-compare-benchmark-results.ps1`
2. 变更内容：
   - 新增 `unit changed` 报告断言与单位变化文本断言（`ops/s -> ms/op`）。
   - 新增 baseline 为 0 时 delta `n/a` 的断言。
   - 新增 `scoreError = NaN` 场景断言，确认报告显示 `error unavailable` 且误差渲染为 `n/a`。
   - 保持 P30 已有 `new benchmark in current` / `missing in current` 语义断言。
3. 验收结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。

### P31-5：CodeHeap 告警专项记录与 warning summary 联动

1. 实际修改清单：
   - `ospf-kotlin-core/scripts/summarize-maven-warnings.ps1`
   - `ospf-kotlin-core/scripts/test-summarize-maven-warnings.ps1`
2. 变更内容：
   - warning summary 脚本新增对非 Maven 标准 warning 行的识别：
     - `\[warning\]\[codecache\] ...`
     - `Java HotSpot(TM) 64-Bit Server VM warning: ...`
   - 补 ANSI 转义清理，避免带色输出导致匹配丢失。
   - smoke 新增 CodeHeap 输入断言，确认两类来源可聚合到同一告警键。
3. 验收结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
   - 使用 `tmp-p30-codeheap-optin.log` 验证，summary 已出现 `CodeHeap 'non-profiled nmethods' is full...` 聚合项（`P31_CODEHEAP_ROWS=1`）。
4. 保留策略：
   - 不修改 `.mvn/jvm.config`，CodeHeap 仍作为构建环境问题记录，不作为 CI 失败条件。

### P31-6：daily.md 收尾与候选池重分层

1. 残留项分层：
   - 已清零或显著下降：
     - demo4 目标批次 `Instant` deprecated warning 清理完成。
     - framework `CGPipeline.refresh` named-argument warning 清理完成。
   - 可安全继续批量清理：
     - framework 各 context 中既有 unchecked cast warning（可继续局部收敛）。
   - 需要业务语义确认：
     - demo4 时间模型若继续扩展到更多上下文，需确认跨 DTO/序列化语义边界。
   - 构建环境问题：
     - CodeHeap 告警在长链路构建中仍会出现（已纳入 summary 观测）。
   - 暂不处理：
     - `.mvn/jvm.config` 全局 JVM 参数调整。
2. 收尾验收命令与结果：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-benchmark\scripts\test-compare-benchmark-results.ps1`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile`：通过。
   - `mvn -pl ospf-kotlin-example -am "-DskipTests" test-compile`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
   - `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

## P32：framework warning 与构建观测深化批次

状态：已完成（2026-05-22）

目标：在 P31 基础上继续扩大单轮有效范围，完成 framework `unchecked cast` 局部收敛、warning summary 可读性增强、CI warning artifact 命名与留存策略整理，以及 CodeHeap 观测记录标准化。P32 全程未修改 core/math 主线公共 API、未引入 solver license 变更、未修改 `.mvn/jvm.config`。

### P32-1：gantt-scheduling service/limits ShadowPriceKey cast 分层收敛

状态：已完成。

实际修改：

1. 新增 3 个局部 helper（最小作用域，文件级 `@Suppress("UNCHECKED_CAST")`，含中英不变量注释）：
   - `.../task_compilation/service/limits/ShadowPriceKeyCastSupport.kt`
   - `.../resource/service/limits/ShadowPriceKeyCastSupport.kt`
   - `.../produce/service/limits/ShadowPriceKeyCastSupport.kt`
2. 将目标 `service/limits` 文件中 `constraint.args as? XxxShadowPriceKey...` 统一替换为 `shadowPriceKeyOf<...>(constraint.args)`，并保留不匹配分支 `?: continue`：
   - task-compilation context：`ExecutorCompilationConstraint.kt`、`TaskAdvanceEarliestEndTimeConstraint.kt`、`TaskAdvanceTimeConstraint.kt`、`TaskCompilationConstraint.kt`、`TaskDelayLastEndTimeConstraint.kt`、`TaskDelayTimeConstraint.kt`、`TaskOverMaxAdvanceTimeConstraint.kt`、`TaskOverMaxDelayTimeConstraint.kt`
   - resource context：`ResourceCapacityConstraint.kt`
   - produce context：`ConsumptionQuantityConstraint.kt`、`ProduceQuantityConstraint.kt`

结果：

1. 目标目录内 `constraint.args as? XxxShadowPriceKey` 形态从 11 处降为 0 处。
2. 相关模块编译通过（见“P32 验收命令与结果”）。

### P32-2：SolutionAnalyzer 与 SlotBasedCapacityPreSolver 高价值 warning 收敛

状态：已完成。

实际修改：

1. `SolutionAnalyzer.kt`：
   - 新增命名 helper `analyzedTaskOf(...)`，将 3 处 `as T` 裸 cast 收敛到单点，最小作用域 `@Suppress("UNCHECKED_CAST")`，含中英不变量注释。
2. `SlotBasedCapacityPreSolver.kt`：
   - 删除 `Error<Any> -> Error<ErrorCode>` 与 `List<Error<Any>> -> List<Error<ErrorCode>>` 的直接不安全 cast。
   - 新增 `errorCodeErrorOf(...)` / `errorCodeErrorsOf(...)` 局部封装：优先保留原 `ErrorCode`；若 solver 返回非 `ErrorCode`，降级为 `ErrorCode.Unknown` 并保留原始错误文本。

结果：

1. `SolutionAnalyzer.kt` 3 条 warning + `SlotBasedCapacityPreSolver.kt` 2 条 warning 已收敛。
2. 对应模块编译通过（见“P32 验收命令与结果”）。

### P32-3：warning summary 可读性与稳定排序增强

状态：已完成。

实际修改：

1. `summarize-maven-warnings.ps1`：
   - 新增参数 `-Top`（默认 `0` 表示显示全部）。
   - 报告新增 `Displayed groups` 字段。
   - 分组结果排序明确并固定为：`Count desc -> Module asc -> Key asc`。
2. `test-summarize-maven-warnings.ps1`：
   - 增加排序断言、`-Top` 行为断言。
   - 覆盖混合 `[WARNING]`、ANSI、HotSpot、`[warning][codecache]` 的 smoke。

结果：

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1` 通过。
2. CodeHeap / HotSpot 模块归属策略保持 `$currentModule`（HotSpot 行归到最近 Maven module）。

### P32-4：CI warning artifact 命名与留存策略整理

状态：已完成。

实际修改（`.github/workflows/core-refactor-guards.yml`）：

1. artifact 名称从 `benchmark-smoke-results` 调整为 `benchmark-smoke-and-warning-summary`。
2. 保持 artifact 路径包含：
   - `ospf-kotlin-benchmark/target/benchmark-results`
   - `ospf-kotlin-core/target/ci/maven-warning-summary.md`
   - `ospf-kotlin-core/target/ci/maven-benchmark-compile.log`
3. 新增 `retention-days: 14`。
4. 保持 `if-no-files-found: error`。

### P32-5：CodeHeap 观测记录标准化

状态：已完成。

观测命令与数据：

1. 构建命令：
   - `mvn -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`（输出落盘到 `ospf-kotlin-core/target/ci/maven-benchmark-compile.log`）。
2. summary 命令：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\ospf-kotlin-core\target\ci\maven-benchmark-compile.log -Output .\ospf-kotlin-core\target\ci\maven-warning-summary.md -Format markdown`
3. `MAVEN_OPTS`：本轮未显式设置（沿用环境默认）。
4. 本次 summary 结果：
   - Total warnings：4
   - Unique groups：2
   - CodeHeap/HotSpot 命中：
     - `CodeHeap 'non-profiled nmethods' is full. Compiler has been disabled.`：2 次
     - `Try increasing the code heap size using -XX:NonProfiledCodeHeapSize=`：2 次
   - 归属 module：`ospf-kotlin-core`
5. 影响评估：构建结果 `BUILD SUCCESS`，未阻断编译。

### P32-6：daily.md 收尾与候选池重分层

状态：已完成。

warning 候选池重分层（P32 收尾）：

1. 已收敛：
   - `service/limits` 目标 ShadowPriceKey cast（11 处）；
   - `SolutionAnalyzer.kt` 3 处 task cast；
   - `SlotBasedCapacityPreSolver.kt` 2 处 Error cast；
   - warning summary 排序与 `-Top` 可读性增强；
   - CI warning artifact 命名与留存策略落地。
2. 可继续批量清理（优先）：
   - `gantt-scheduling-domain-resource-context` / `gantt-scheduling-domain-produce-context` model 层剩余 unchecked cast。
3. 需业务/泛型语义确认：
   - `IterativeTaskCompilation<IT, T, ...>` 里 `IT -> T` 的长期建模约束是否可在类型系统中进一步显式化（当前通过局部不变量封装处理）。
4. 暂不处理：
   - `.mvn/jvm.config` JVM 参数调整；
   - 将 warning 提升为 CI 硬门禁。

P33 建议主线：

1. 优先处理 `gantt-scheduling` model 层高频 unchecked cast（resource/produce/infrastructure）。
2. 若继续处理 CodeHeap，仅做观测对比与统计，不改全局 JVM 配置。

## P33：framework model 层 warning 收束批次

状态：已完成（2026-05-22）

目标：在 P32 已收敛 `service/limits` 和 service 层高价值 warning 的基础上，继续收敛 framework gantt-scheduling 的 model/infrastructure 层 unchecked cast。P33 未改 core/math 主线 API，未改 `.mvn/jvm.config`，未扩展 warning summary 脚本功能，CI 仍维持观测口径。

### P33-1：resource model ExpressionRange cast 收敛

状态：已完成。

实际修改：

1. 文件：`.../domain/resource/model/Resource.kt`
2. 新增局部 helper：
   - `setResourceSlackUpperBoundAsFlt64(range: ExpressionRange<*>, upperBound: Flt64)`
   - 最小作用域 `@Suppress("UNCHECKED_CAST")`，含中英双语不变量说明。
3. 将两处散点 cast：
   - `slack.helperVariables.last().range as ExpressionRange<Flt64>`
   - `slack.helperVariables.first().range as ExpressionRange<Flt64>`
   收敛到 helper 调用。

结果：

1. `Resource.kt` 2 条目标 warning 收敛（从散点转为单点封装并加说明）。
2. 编译通过（见“P33 验收命令与结果”）。

### P33-2：produce model CapacityActionProduce cast 收敛

状态：已完成。

实际修改：

1. 文件：`.../domain/produce/model/CapacityActionProduce.kt`
   - 新增两个 helper（最小作用域 suppress + 中英注释）：
     - `unitProduceMapOf(action: ProductionAction)`
     - `unitConsumptionMapOf(action: ProductionAction)`
   - `CapacityColumn.produce(...)` / `CapacityColumn.consumption(...)` 改为复用上述 helper，移除裸 cast。
2. 文件：`.../domain/produce/model/BunchCapacitySchedulingProduce.kt`
   - `addColumns(...)` 中改为 `unitProduceMapOf<P>(action)?.get(product)`。
3. 文件：`.../domain/produce/model/PlanCapacitySchedulingProduce.kt`
   - 构造流程中改为 `unitProduceMapOf<P>(action)?.get(product)`。

结果：

1. 三个目标文件内的 4 条裸 cast warning 收敛到 helper 封装点。
2. 编译通过（见“P33 验收命令与结果”）。

### P33-3：WorkingCalendar Productivity cast 单点评估

状态：已完成。

实际修改：

1. 文件：`.../infrastructure/WorkingCalendar.kt`
2. 在 `ProductivityCalendar` 内新增局部 helper：
   - `rebuiltProductivityOf(source: P, timeWindow: TimeRange): P`
   - 最小作用域 `@Suppress("UNCHECKED_CAST")`，含中英双语不变量说明。
3. 将 `it.new(timeWindow = time) as P` 改为 helper 调用。

结果：

1. `WorkingCalendar.kt` 该单点 warning 收敛到命名 helper。
2. 编译通过（见“P33 验收命令与结果”）。

### P33-4：warning summary 复用前后对比

状态：已完成。

执行命令：

1. 生成编译日志：
   `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -DskipTests compile`
   日志：`ospf-kotlin-core/target/ci/maven-p33-framework-model-compile.log`
2. 生成 summary：
   `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\ospf-kotlin-core\target\ci\maven-p33-framework-model-compile.log -Output .\ospf-kotlin-core\target\ci\maven-p33-warning-summary.md -Format markdown -Top 20`

对比摘要（P33 目标点）：

1. P33 前（基于 P32 编译日志）：
   - `Resource.kt`：2 条
   - produce 3 文件：4 条
   - `WorkingCalendar.kt`：1 条
   - 合计：7 条目标 warning
2. P33 后（本轮 summary + 编译输出）：
   - 上述目标 warning：0 条
   - summary 仅剩 CodeHeap/HotSpot 分组（2 组，4 行）

### P33-5：CodeHeap 观测延续

状态：已完成。

记录：

1. 本轮主要验收编译均出现 CodeHeap/HotSpot 警告（构建命令见“P33 验收命令与结果”）。
2. `MAVEN_OPTS`：本轮未显式设置（沿用环境默认）。
3. summary（`maven-p33-warning-summary.md`）统计：
   - Total warnings：4
   - Unique groups：2
   - `CodeHeap 'non-profiled nmethods' is full...`：2 次
   - `Try increasing the code heap size using -XX:NonProfiledCodeHeapSize=`：2 次
   - 归属 module：`ospf-kotlin-core`
4. 影响评估：构建均 `BUILD SUCCESS`，未阻断。

### P33-6：daily.md 收尾与候选池重分层

状态：已完成。

剩余 warning 候选池分层：

1. 已收敛：
   - resource model `ExpressionRange<Flt64>` 2 处；
   - produce model `CapacityActionProduce` 4 处；
   - infrastructure `WorkingCalendar` 1 处；
   - 以上共 7 条目标 warning。
2. 可继续低风险收敛：
   - framework 其余 model 文件中同构的 material/resource 泛型 cast（优先同包 helper 化）。
3. 需要泛型/业务语义确认：
   - 若后续要彻底移除 helper 内 suppress，需要评估 `ProductionAction` 与 `Productivity` 的上层泛型绑定方式是否可增强。
4. 暂不处理：
   - `.mvn/jvm.config` JVM 参数调整；
   - 将 warning 提升为 CI 硬门禁。

P34 建议主线：

1. 聚焦 framework model 里剩余具体文件 warning，不重复 P31-P33 已清理项。
2. CodeHeap 继续观测口径，不做全局 JVM 改参。

## P34：framework warning 防回流与维护化批次

状态：已完成（2026-05-22）

目标：将 P31-P33 的 warning 清理成果固化为防回流 guard，并通过既有 warning summary 与 CodeHeap 观测口径验证当前状态。不做泛型体系重构，不改 `.mvn/jvm.config`，不把普通 warning/CodeHeap 提升为硬门禁。

### P34-1：framework 已清理裸 cast 模式防回流 guard

状态：已完成。

实际修改：

1. 文件：`ospf-kotlin-core/scripts/check-c8-guards.ps1`
2. 新增通用辅助函数：
   - `Get-KtRegexMatches`
   - `Split-MatchesByAllowlist`
   - `Format-RegexHitPreview`
3. 新增 P34 guard 小节（P6/P7 均执行）：
   - `P34-1-1`：禁止 `constraint.args as?/as XxxShadowPriceKey` 回流；
   - `P34-1-2`：`CapacityActionProduce` cast 仅允许 helper 行（精确 allowlist）；
   - `P34-1-3`：`as ExpressionRange<Flt64>` 仅允许 `Resource.kt` helper 行（精确 allowlist）；
   - `P34-1-4`：泛型 `as P` 生产率重建 cast 仅允许 `WorkingCalendar.kt` helper 行（精确 allowlist）；
   - `P34-1-5`：禁止 `result.error as Error<ErrorCode>` / `result.errors as List<Error<ErrorCode>>` 旧写法回流。
4. guard 扫描范围限定在 `ospf-kotlin-framework-gantt-scheduling/**/src/main/**/*.kt`，命中输出带路径与行号。

结果：

1. `P34-1-1` 到 `P34-1-5` 在 P6/P7 下全部通过。
2. allowlist 为精确文件+行内容规则，无宽泛目录豁免。

### P34-2：P32/P33 helper 命名、作用域与注释复核

状态：已完成。

复核对象：

1. `shadowPriceKeyOf(...)`
2. `unitProduceMapOf(...)`
3. `unitConsumptionMapOf(...)`
4. `setResourceSlackUpperBoundAsFlt64(...)`
5. `rebuiltProductivityOf(...)`
6. `analyzedTaskOf(...)`
7. `errorCodeErrorOf(...)` / `errorCodeErrorsOf(...)`

复核结论：

1. 可见性符合最小原则：跨文件复用为 `internal`，仅类内/文件内使用为 `private`。
2. `@Suppress("UNCHECKED_CAST")` 作用域已最小化（函数级或专用 helper 文件级）。
3. 注释均包含中英双语不变量说明，且指向具体构造路径；本轮无需额外小修。

### P34-3：warning summary 复用做防回流观测

状态：已完成。

执行命令：

1. 编译日志生成（覆盖目标 5 模块）：
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-bunch-compilation-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -DskipTests compile`
   - 日志：`ospf-kotlin-core/target/ci/maven-p34-framework-guard-compile.log`
2. summary 生成：
   - `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\ospf-kotlin-core\target\ci\maven-p34-framework-guard-compile.log -Output .\ospf-kotlin-core\target\ci\maven-p34-warning-summary.md -Format markdown -Top 20`

观测结论：

1. summary 中不再出现：
   - P31 `shadowPriceMap` named-argument warning；
   - P32 `service/limits` ShadowPriceKey cast warning；
   - P33 model/infrastructure 目标 cast warning。
2. 当前 summary 仅剩 CodeHeap/HotSpot 两组（共 4 行）。

### P34-4：CodeHeap 观测口径固化

状态：已完成。

记录：

1. 本轮 `MAVEN_OPTS`：未显式设置（沿用环境默认）。
2. `maven-p34-warning-summary.md` 统计：
   - Total warnings：4
   - Unique groups：2
   - `CodeHeap 'non-profiled nmethods' is full...`：2 次
   - `Try increasing the code heap size using -XX:NonProfiledCodeHeapSize=`：2 次
   - 归属 module：`ospf-kotlin-core`
3. CodeHeap warning 未阻塞构建（构建结果 `BUILD SUCCESS`）。
4. `.mvn/jvm.config` 未修改，且本轮不建议新增 JVM 参数调优动作（除非 CI 环境出现新的可复现证据）。

### P34-5：daily.md 收尾与候选池重分层

状态：已完成。

候选池分层：

1. 已防回流：
   - P31-P33 已清理 cast/错误转换模式已落地 guard；
   - warning summary 与 CodeHeap 观测口径已固化。
2. 可继续低风险清理：
   - framework 其他未纳入 P31-P33 闭环的零散 warning（按具体新证据逐条处理）。
3. 需大范围泛型设计确认：
   - 若目标是“彻底去除 helper 内 suppress”，需重估上层泛型建模约束，不适合健康批次直接推进。
4. 暂不处理：
   - `.mvn/jvm.config` 调整；
   - warning/CodeHeap 硬门禁化。

P35 建议：

1. 若无新增高价值 warning 或明确 guard 缺口，建议暂停工程健康批次，转入提交/发布准备。
2. 若继续，仅处理“新回流”或“新出现”的具体问题，不重复扫描 P31-P34 已闭环范围。

## P35：guard 与 warning 观测维护化收口批次

状态：计划中，交接给后续会话执行。

目标：在 P34 已完成且可提交的基础上，做最后一轮维护化收口。P35 不继续做无边界 warning 扫描，不重启 P31-P34 已清理的代码迁移目标，不修改 `.mvn/jvm.config`，不把 warning/CodeHeap 提升为 CI 硬门禁；重点是让 P34 guard 与 warning summary 自身更稳定、更容易维护，并给工程健康批次形成暂停条件。

执行前置条件：

1. 先提交 P34 当前改动，建议提交信息：`chore(p34): guard framework warning cleanup backflow`。
2. P35 从干净工作区开始；若提交前发现 P34 仍有未解决 diff，先完成 P34 收尾，不把 P34/P35 混入同一提交。
3. 保持 P35 变更范围集中在脚本、脚本 smoke、CI 观测和 `daily.md`；除非发现真实新回流，不主动改业务 Kotlin 代码。

### P35-1：P34 guard 路径与多行回流健壮性

目标：让 P34 新增 guard 在 Windows/Unix 路径、单行/多行回流形态下都稳定工作。

建议步骤：

1. 复核 `check-c8-guards.ps1` 中 `Get-KtRegexMatches` 的相对路径输出，必要时统一转换为 `/`，避免 `"/src/main/"` 过滤受路径分隔符影响。
2. 复核 P34-1-1 到 P34-1-5 的 regex 是否覆盖历史回流的关键形态：
   - `constraint.args as? XxxShadowPriceKey`
   - `as? CapacityActionProduce<...>`
   - `as ExpressionRange<Flt64>`
   - 泛型 `as P`
   - `result.error/result.errors` 原始 `Error<ErrorCode>` cast
3. 只增强当前 guard 稳定性，不新增更宽泛的业务约束。

验收：

1. P6/P7 下 P34-1-1 到 P34-1-5 全部通过。
2. guard 失败时能输出命中文件、行号与行内容。

### P35-2：P34 guard smoke 覆盖

目标：为 P34 防回流模式增加轻量 smoke，防止后续修改 guard 时静默失效。

建议步骤：

1. 评估是否在现有 `check-c8-guards.ps1` 附近新增脚本 smoke，或新增 `test-check-c8-guards.ps1` 的最小用例。
2. smoke 至少覆盖：
   - 应拦截：ShadowPriceKey 直转 cast、散点 `CapacityActionProduce` cast、散点 `ExpressionRange<Flt64>` cast、散点 `as P`、`result.error/result.errors` 原始 Error cast。
   - 应放行：P34 精确 allowlist helper 行。
3. 若现有 guard 结构不适合直接注入临时文件，优先抽出可测试的匹配/allowlist helper；不要为了 smoke 大改 guard 主流程。

验收：

1. 新增或扩展的 smoke 脚本通过。
2. P6/P7 仍通过。

### P35-3：CodeHeap warning summary 归属优化

目标：避免 CodeHeap/HotSpot VM warning 被误归属到最近 Maven module，改成更清晰的构建环境归属。

建议步骤：

1. 调整 `summarize-maven-warnings.ps1`：对 `Java HotSpot(TM) 64-Bit Server VM warning:` 与 `[warning][codecache]` 行使用虚拟 module，例如 `build-environment` 或 `jvm-codecache`。
2. 同步更新 `test-summarize-maven-warnings.ps1`，覆盖 HotSpot 与 codecache 两类输入。
3. 保持报告语义为观测，不让 CodeHeap warning 影响退出码。

验收：

1. `test-summarize-maven-warnings.ps1` 通过。
2. 基于 P34 或新生成 Maven 日志的 summary 中，CodeHeap/HotSpot 不再显示为 `ospf-kotlin-core` 业务模块。

### P35-4：CI artifact 与报告口径复核

目标：确认 warning summary 在 CI 中的展示、artifact 名称和保留策略与当前维护化目标一致。

建议步骤：

1. 复核 `.github/workflows/core-refactor-guards.yml` 中 warning summary 步骤。
2. 确认 artifact 名称、包含文件与 `retention-days` 仍合理。
3. 确认 CI 只上传/展示 summary，不因普通 warning 或 CodeHeap warning 失败。
4. 如需修改，保持最小变更，并同步 `daily.md`。

验收：

1. workflow YAML 语法保持有效。
2. 本地相关脚本 smoke 通过。

### P35-5：工程健康批次暂停条件回填

目标：明确 P35 后不再继续规划 P36 的默认条件，减少后续重复迭代。

建议步骤：

1. 在 `daily.md` 回填 P35 实际执行记录、验收结果和剩余项。
2. 若 P35 后 warning summary 仍只剩 CodeHeap/HotSpot，则明确记录：
   - 工程健康批次暂停；
   - 后续只处理新回流、新 warning 或 CI 新证据；
   - 不再进行泛化 warning 清扫。
3. 记录是否仍建议不修改 `.mvn/jvm.config`，以及原因。

验收：

1. `daily.md` 有明确 P35 收尾状态。
2. 候选池不再默认推荐 P36；只有出现新证据时再开新批次。

### P35 建议执行顺序

1. 提交 P34。
2. P35-1：先加固 P34 guard 路径与多行匹配边界。
3. P35-2：补 guard smoke，确认 allowlist 与 violation 都可测。
4. P35-3：优化 CodeHeap/HotSpot warning summary 归属。
5. P35-4：复核 CI artifact/report 口径。
6. P35-5：回填 `daily.md` 并写明工程健康批次暂停条件。

### P35 建议验收命令

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`
4. 如新增 guard smoke：`pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-check-c8-guards.ps1`
5. `git diff --check`

### P34 验收命令与结果

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-bunch-compilation-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -DskipTests compile`：通过。
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\ospf-kotlin-core\target\ci\maven-p34-framework-guard-compile.log -Output .\ospf-kotlin-core\target\ci\maven-p34-warning-summary.md -Format markdown -Top 20`：通过。
6. `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P33 验收命令与结果

1. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context -am "-DskipTests" compile`：通过。
2. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile`：通过。
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am "-DskipTests" compile`：通过。
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-infrastructure -am -DskipTests compile`：通过（用于 P33 summary 日志）。
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\summarize-maven-warnings.ps1 -LogPath .\ospf-kotlin-core\target\ci\maven-p33-framework-model-compile.log -Output .\ospf-kotlin-core\target\ci\maven-p33-warning-summary.md -Format markdown -Top 20`：通过。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
8. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
9. `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。

### P32 验收命令与结果

1. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\test-summarize-maven-warnings.ps1`：通过。
2. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am "-DskipTests" compile`：通过。
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-resource-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-produce-context -am "-DskipTests" compile`：通过。
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-bunch-compilation-context,ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am "-DskipTests" compile`：通过。
5. `mvn -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`：通过。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
7. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
8. `git diff --check`：通过（存在 LF/CRLF 提示警告，不影响检查结果）。
