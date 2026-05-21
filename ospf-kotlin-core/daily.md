# P22 工程深化交接（2026-05-21）

## 最新结论

`math`、`multiarray`、`quantities`、`core` 已按 P18-P21 目标完成架构收口、泛型化、接口恢复、性能基线、第一批大文件拆分和第二轮工程优化。当前迁移目标已达成，P22 是迁移完成后的工程深化，不再重新打开兼容层或平滑迁移目标。

## 已完成事项摘要

以下只保留已完成结论，不保留逐批执行细节。

1. 已完成从 core 自有表达式体系到 `math.symbol` 正式符号体系的迁移。
2. 已删除旧兼容层与迁移桥接入口，包括旧 adapter/bridge/compat 路径和迁移期命名。
3. 已完成 core 主链路泛型化，保留的 Flt64 命名仅表达真实快捷层、解析器或 solver 边界职责。
4. 已恢复 example 默认构建与项目内 source-compat / profile 验收入口。
5. 已建立 P6/P7 等静态门禁，覆盖兼容层回流、旧命名回流、旧 import 回流、危险 hard-cast、空测试等风险。
6. 已完成 P19 第一轮性能和结构优化，覆盖 multiarray、math、core、core-plugin、framework 关键热点。
7. 已完成 P20 benchmark baseline，`ospf-kotlin-benchmark` 参与默认构建但跳过 install、deploy 和 central publish，JMH 生成与运行仍由 `bench` profile 激活。
8. 已完成 P20 core 大文件第一批职责拆分，抽出 triad/tetrad dump helper，保持公开 API 和行为不变。
9. 已完成 P20 solver 插件第一批公共数据准备抽取，Gurobi/Gurobi11/Cplex/SCIP 已接入公共变量 dump 与约束分块 helper。
10. 已完成 P21 benchmark baseline 扩展，覆盖 small/medium/large 与 core-plugin dump 热点。
11. 已完成 P21 solver 状态/错误码第二轮统一，Gurobi/Gurobi11/Cplex/SCIP 已接入 `SolverStatusSupport`。
12. 已完成 P21 core elastic builder 职责拆分和 `SparseMatrix.transpose()` benchmark 驱动优化。
13. 已完成 P21 技术债与开发体验清理，mosek 主路径 TODO 已语义化，core 主路径乱码注释已清理。

## P22 目标

P22 的目标是做迁移完成后的工程深化，重点处理 solver 残余失败兜底、benchmark 趋势沉淀、core 大文件剩余职责拆分和泛型 warning debt 的局部收敛。

1. 继续消化 solver 插件中 COPT、Hexaly、MindOPT 等残余 `status.errCode!!`，统一使用 `SolverStatus.resolveErrCode()` 兜底。
2. 扩展 benchmark 趋势记录和 CI artifact，保留 small smoke 轻量门禁，不把绝对性能数值作为跨机器硬门禁。
3. 继续拆分 core 大文件剩余职责，优先处理 normalize/copy/clone、flatten/export 辅助逻辑和 memory cleanup 调用点。
4. 做 warning debt 清理，优先收敛可证明安全的泛型 unchecked cast 和重复 warning，不做全仓机械压制。

## 总体原则

1. 不新增兼容层，不恢复旧 bridge/adapter/compat 设计。
2. 不改变公开 API；如必须改变，先在本文件记录理由、影响范围、替代方案和验收方式。
3. P22 是工程深化，不重新定义 P18-P21 的迁移完成口径。
4. benchmark 模块参与默认构建；JMH 生成与运行只在 `bench` profile 下启用，且该模块不参与 install、deploy 和 central publish。
5. solver 公共化只抽 solver 无关的数据准备、状态归一和错误处理逻辑，不强行统一真实 solver API。
6. Flt64 / Double 转换必须留在显式 solver 边界或既有白名单，不能回流到 core 泛型主链路。
7. 大文件拆分只移动内聚职责，不混入语义变化。
8. warning debt 清理不做全仓机械 suppress；新增 suppress 必须靠近最小作用域，并说明可证明不变量。
9. 写注释时遵守项目规则：中英双语；不添加版权声明。
10. README / README_ch 如涉及用户入口、benchmark 使用或 profile 说明，必须同步更新并保持互链。

## P22 事项与步骤

### P22-1：solver 残余错误码兜底清理

状态：已完成（第一批：COPT、Hexaly、MindOPT 线性/二次 solver 残余 `status.errCode!!` 清理）。

目标：把非 P21 目标插件中的 solver 失败错误码兜底统一到 `SolverStatus.resolveErrCode()`，避免 `status.errCode!!` 在失败路径触发无上下文 NPE。

详细步骤：

1. 扫描 `ospf-kotlin-core-plugin` 中残余 `status.errCode!!`。
2. 第一批处理 COPT、Hexaly、MindOPT 的线性/二次 solver。
3. 保持各 solver 原有状态判断、异常捕获和真实 solver API 调用不变。
4. 编译目标插件，不要求真实 solver license 作为默认阻塞项。
5. 扫描确认目标插件残余 `status.errCode!!` 清零。

预计修改清单：

- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly/src/main/...`
- `ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt/src/main/...`
- `ospf-kotlin-core/daily.md`

验收标准：

1. COPT、Hexaly、MindOPT 插件 `-DskipTests compile` 通过。
2. 目标插件 main 源码中不再出现 `status.errCode!!`。
3. P6/P7 静态门禁通过。
4. `git diff --check` 通过。

本批实际修改清单：

1. COPT：
   - `CoptLinearSolver.kt`
   - `CoptQuadraticSolver.kt`
2. Hexaly：
   - `HexalyLinearSolver.kt`
   - `HexalyQuadraticSolver.kt`
3. MindOPT：
   - `MindOPTLinearSolver.kt`
   - `MindOPTQuadraticSolver.kt`
4. 六处 `Failed(Err(status.errCode!!))` 已改为 `Failed(Err(status.resolveErrCode()))`。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt -am -DskipTests compile`：通过。
2. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly -am -DskipTests compile`：通过。
3. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt -am -DskipTests compile`：通过。
4. `Select-String -Pattern 'status\.errCode!!'` 扫描 COPT、Hexaly、MindOPT main 源码：无结果（通过）。
5. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
6. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
7. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P22-2：benchmark 趋势记录与 CI artifact

状态：已完成（CI smoke 固定结果文件并上传 artifact，README 双语同步）。

目标：让 P21 baseline 更容易长期比较，CI 保留 benchmark smoke 输出和可下载 artifact。

详细步骤：

1. 复核 `.github/workflows/core-refactor-guards.yml` 的 benchmark smoke job。
2. 将 benchmark small smoke 输出固定到 `ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json`。
3. 增加 GitHub Actions artifact 上传步骤。
4. 更新 README / README_ch 说明：
   - smoke 只验证可运行。
   - medium/large 只作为手动趋势对比。
   - artifact 不作为性能硬门禁。
5. 保持 benchmark module 参与默认构建但不发布。

预计修改清单：

- `.github/workflows/core-refactor-guards.yml`
- `README.md`
- `README_ch.md`
- `ospf-kotlin-benchmark/src/main/...`（仅当 runner 参数需要补强）
- `ospf-kotlin-core/daily.md`

验收标准：

1. benchmark smoke workflow 语法保持有效。
2. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile` 通过。
3. 至少一个 small smoke 可生成结果文件。
4. README / README_ch 双语同步。
5. `git diff --check` 通过。

本批实际修改清单：

1. 更新 `.github/workflows/core-refactor-guards.yml`：
   - benchmark smoke 命令固定输出文件为 `ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json`。
   - 新增 `actions/upload-artifact@v4`，上传 `benchmark-smoke-results` artifact。
2. 更新 `README.md` 与 `README_ch.md`：
   - benchmark smoke 命令同步为固定输出 `ci-smoke.json`。
   - 明确 artifact 仅用于结果留存，不作为性能硬门禁。
3. `BenchmarkRunner.kt` 无需改动：
   - 现有参数已支持 `result format` 与 `result file path`，可直接满足 CI 固定输出需求。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`：通过。
2. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"`：通过。
3. `Test-Path ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json`：`True`（通过）。
4. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P22-3：core 大文件剩余职责拆分

状态：已完成（拆分 dual/farkasDual 求解辅助职责，保持公开签名与行为不变）。

目标：继续拆分 core 大文件剩余内聚职责，降低维护成本，保持公开 API 与行为不变。

详细步骤：

1. 统计当前核心大文件的函数分布和调用边界。
2. 按职责拆分，优先顺序：
   - normalize / copy / clone。
   - flatten / export 辅助逻辑。
   - memory cleanup 使用点。
3. 每批只拆一个职责，禁止同时做行为优化。
4. 新 helper 默认使用 `internal` 或文件级 `private`，避免扩大 API 面。
5. 拆分后对比 dump、flatten、copy/clone、elastic builder 输出。

预计修改清单：

- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/LinearTriadModel.kt`
- `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/QuadraticTetradModel.kt`
- 新增 core internal helper 文件
- 相关 core tests
- `ospf-kotlin-core/daily.md`

验收标准：

1. 公开 API 不变。
2. core 目标测试通过，至少覆盖 flatten、dump、copy/clone、elastic builder 相关路径。
3. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test` 通过；如实际测试名不存在，记录实际运行到的测试。
4. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile` 通过。
5. P6/P7 静态门禁通过。
6. `git diff --check` 通过。

本批实际修改清单：

1. 新增 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/intermediate/TriadDualSolverSupport.kt`：
   - 承载 `LinearTriadModel` / `QuadraticTetradModel` 原底部顶层函数：
     - `solveDual(model: LinearTriadModel, solver: LinearSolver)`
     - `solveFarkasDual(model: LinearTriadModelView, solver: LinearSolver)`
     - `solveDual(model: QuadraticTetradModel, solver: QuadraticSolver)`
     - `solveFarkasDual(model: QuadraticTetradModelView, solver: QuadraticSolver)`
   - 仅做职责搬移，不修改函数签名、返回类型和错误分支行为。
2. 更新 `LinearTriadModel.kt` 与 `QuadraticTetradModel.kt`：
   - 删除原文件底部对应顶层函数实现。
   - 清理不再使用的 `LinearSolver` / `QuadraticSolver` import。
3. 说明：
   - 上述四个 helper 仍保持顶层 `public` 可见性，因为 `core-plugin`（如 SCIP/MindOPT benders 路径）存在跨模块直接调用，收敛为 `internal` 会破坏现有调用面。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`：通过。
2. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest -Dsurefire.failIfNoSpecifiedTests=false test`：通过（实际执行 24 tests，0 失败）。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
4. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
5. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

### P22-4：warning debt 局部收敛

状态：已完成（第一批：core `FunctionSymbol` unchecked cast warning 局部收敛）。

目标：收敛可证明安全的泛型 unchecked cast warning 和重复 warning，提升维护信号质量。

详细步骤：

1. 从当前编译 warning 中选取高重复、低风险、边界清晰的文件。
2. 优先处理：
   - 可用局部 helper 封装的 unchecked cast。
   - 可用类型收窄或局部变量消除的 always true / deprecated property warning。
   - 不改变泛型能力的 source/test warning。
3. 不做全仓 `@Suppress("UNCHECKED_CAST")` 机械铺设。
4. 每批只处理一个模块或一组同类 warning。
5. 新增 suppress 时必须靠近最小作用域，并能解释不变量。

预计修改清单：

- `ospf-kotlin-utils/src/main/...`
- `ospf-kotlin-math/src/main/...`
- `ospf-kotlin-quantities/src/main/...`
- `ospf-kotlin-core/src/main/...`
- 相关 tests
- `ospf-kotlin-core/daily.md`

验收标准：

1. 修改模块 `compile` 或 `test-compile` 通过。
2. warning 数量或目标 warning 位置明确下降。
3. 不新增兼容层、旧命名、旧 import 或 Flt64 回流。
4. P6/P7 静态门禁通过。
5. `git diff --check` 通过。

本批实际修改清单：

1. 更新 `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function/FunctionSymbol.kt`：
   - 在 `LinearFunctionSymbolAdapter<V>` 中新增 `resultPolynomialOrZero()` 局部 helper，统一封装 `HasResultPolynomial` 到 `LinearPolynomial<V>` 的转换。
   - 将 `@Suppress("UNCHECKED_CAST")` 收敛到最小作用域（仅 helper 函数）。
   - 新增中英双语安全不变量注释，说明本模块内 `HasResultPolynomial` 与 `delegate` 使用同一 `V` 类型参数。
2. 替换 3 处重复 cast/兜底分支为 helper 调用：
   - `flattenedMonomials`
   - `polynomial`
   - `asMutable()`
3. 保持公开 API 与行为不变；仅做 warning debt 局部治理，不做泛型能力和语义调整。

本批验收命令与结果：

1. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`：通过（目标文件 `FunctionSymbol.kt` 原 3 处 unchecked cast warning 已消失）。
2. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`：通过。
3. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`：通过。
4. `git diff --check`：通过（仅 LF/CRLF 提示，无空白错误）。

## 推荐执行顺序

1. P22-1 solver 残余错误码兜底清理。
2. P22-2 benchmark 趋势记录与 CI artifact。
3. P22-3 core 大文件剩余职责拆分。
4. P22-4 warning debt 局部收敛。

## 推荐验收命令

按改动范围选择，不要求每批都完整执行全部命令。

1. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt -am -DskipTests compile`
2. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly -am -DskipTests compile`
3. `mvn --% -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt -am -DskipTests compile`
4. `mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile`
5. `mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1"`
6. `mvn --% -pl ospf-kotlin-core -am -Dtest=SourceCompatTest,MathInequalityFlattenTest,SparseMatrixTransposeTest,FlattenUtilityTest,ModelingPreparationTest,SolverStatusSupportTest -Dsurefire.failIfNoSpecifiedTests=false test`
7. `mvn --% -pl ospf-kotlin-core -am -DskipTests test-compile`
8. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P6`
9. `pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-c8-guards.ps1 -GuardMode P7`
10. `git diff --check`

## 交接注意事项

1. 下一个会话从 P22-1 继续或从 P22-2 开始，不要重新打开 P18-P21 迁移与收口目标。
2. P22 默认是工程深化，不做大范围 API 重设。
3. benchmark 结果用于趋势判断，不把机器相关绝对数值作为硬门禁。
4. 真实外部 solver 不作为默认阻塞项；默认以编译、结构测试和边界转换测试为主。
5. 每个 P22 子任务完成后，更新本文件对应小节的状态、实际修改清单、验收命令和结果，再提交独立 commit。
