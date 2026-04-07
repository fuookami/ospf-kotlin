# OSPF Kotlin Core Daily

日期：2026-03-30
交接目标：下一个执行环境
Rust 对齐参考：`E:\workspace\ospf-rust`

## 已完成事项（截至当前工作区）

### 1) 统一求解入口与状态/输出主链路
1. 已落地 `SolveOptions` + `SolverExt.solveWithOptions(...)` 统一入口，覆盖 LP/QP 主路径。
2. 旧 `LinearSolver` / `QuadraticSolver` 多重载入口保留兼容，并逐步转发到统一入口。
3. 已完成 `ModelBuildingStage` / `ModelBuildingStatus` 统一状态桥接，旧 callback 仍可用。
4. 统一输出字段已补齐到可行与不可行分支：`iterations/nodeCount/bestBound/mipGap/solveTime`。
5. IIS 分支已支持从最后一次 `SolvingStatus` 回填统一字段，并对 `solveTime` 提供兜底。

### 2) IIS 与中间模型补完
1. `QuadraticTetradModel.elastic()` 已从 TODO 实现为可用逻辑。
2. 线性 IIS 删除过滤已实现。
3. 二次 IIS 已实现 elastic filtering + deletion filtering + snapshot fallback 主流程。
4. 已新增对应回归测试（含 IIS 选项转发、统一字段回填、elastic/deletion 路径）。

### 3) Token 缓存上下文改造（当前阶段）
1. 已新增并接入 `TokenCacheContexts`：
   - `LinearFlattenContext`
   - `QuadraticFlattenContext`
   - `ValueCacheContext`
   - `RangeCacheContext`
2. `TokenTable` flatten 缓存 API 已按线性/二次拆分：
   - `cached/cache/clearLinearFlatten`
   - `cached/cache/clearQuadraticFlatten`
3. flatten 缓存载荷已改为：
   - `LinearFlattenData(monomials: List<math.symbol.monomial.LinearMonomial>, constant)`
   - `QuadraticFlattenData(monomials: List<math.symbol.monomial.QuadraticMonomial>, constant)`
4. `cacheKey` 已统一为 `Any`，并已引入 `TokenCacheKey/newTokenCacheKey` 供非符号对象安全缓存。
5. 中间符号、单项式、多项式、不等式均已接入新缓存通道（当前仍保留 Cell 兼容层做过渡）。

### 5) `math.symbol` 符号重载补齐（本轮新增）
1. 已补齐 `LinearMonomial`/`QuadraticMonomial` 的常用运算符重载（`+ - * / unary-`，含常量、线性/二次多项式互算）。
2. 已补齐 `LinearPolynomial`/`QuadraticPolynomial` 的不可变运算符重载（`+ - * / unary-`，含线性与二次互转场景）。
3. 已补齐 `LinearInequality`/`QuadraticInequality` 的比较重载（`lt/le/eq/ne/ge/gt`），支持 monomial/polynomial/constant 组合。
4. 已新增重载回归测试：`MonomialTest`、`PolynomialTest`、`InequalityTest`。
5. 本轮同时补回 `CanonicalMonomial` / `CanonicalPolynomial` 缺失类型，打通 `generic/operation/serde` 对 canonical 数据层的直接编译依赖。

### 6) 本轮验证记录（增量）
1. `mvn -pl ospf-kotlin-utils -DskipTests compile` 在一次增量链路下可通过（`math.symbol` 本轮改动文件可编译）。
2. 当前工作区存在更大范围历史断点（与本轮重载改动非同一层级），在触发更大范围重编译时会在 `utils/math/*` 与 `symbol/operation/*` 出现大量既有编译错误，导致本轮定向测试命令无法完成收敛验证。

### 4) 现有验证记录（最近一次）
1. `mvn -pl ospf-kotlin-core "-Dtest=TokenCacheContextsTest,PrepareCacheKeyRegressionTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过（8 tests）。
2. `mvn -pl ospf-kotlin-core -am test` 通过（utils 362 / core 77）。
3. `mvn -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-cplex,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-gurobi11,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-hexaly,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mindopt,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-mosek,ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-scip -am -DskipTests compile` 通过。

---

## 未完成事项（已重排，按优先级）

### P0（必须优先，破坏性迁移）
目标：`ospf-kotlin-core` 的单项式/多项式/不等式逐步移除，统一迁移到 `ospf-kotlin-utils/math/symbol` 符号运算库。

1. 彻底移除 `LinearMonomialCell` / `QuadraticMonomialCell` 作为核心数据通道。
2. `FlattenContext` 最终仅保留：
   - `LinearFlattenContext` 对应 `math.symbol.monomial.LinearMonomial`
   - `QuadraticFlattenContext` 对应 `math.symbol.monomial.QuadraticMonomial`
3. 删除 `TokenCacheContext` 中 Cell 与 utils monomial 间的双向转换函数（`to*MonomialCells` 等兼容层）。
4. 将 monomial/polynomial/inequality 的 `cells` 主路径替换为 `math.symbol` 数据结构，允许破坏性 API 迁移。
5. 把 `ValueCacheContext` / `RangeCacheContext` 的使用面统一到中间符号 + 单项式 + 多项式（当前仅中间符号语义最完整）。

### P1（P0 之后）
1. 清理 `frontend/expression/monomial|polynomial|inequality` 中对旧 cell 类型的泛型约束与重载。
2. 将评估/运算逻辑统一收敛到 `math.symbol.operation`，`core` 仅保留薄适配。
3. 清理旧 API 后补齐迁移测试与编译期断言，确保调用侧尽快暴露不兼容点。

### P2（稳定化）
1. 文档更新：补充“破坏性迁移说明 + 新 API 对照表 + 典型改造样例”。
2. 对外模块（framework/plugins/starter）做一轮编译回归，确认新类型链路完整。

---

## 下个环境建议执行顺序
1. 先做 P0-1/P0-2：把 flatten 与缓存载荷里的 cell 彻底拔掉，只保留 `math.symbol.monomial`。
2. 再做 P0-3/P0-4：替换 monomial/polynomial/inequality 的主数据模型与对外签名。
3. 每完成一个子阶段执行：
   - `mvn -pl ospf-kotlin-core -am test`
   - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`（8 plugin 联编）
4. 完成后回写本文件：仅记录“新增完成项 + 剩余缺口 + 回归命令结果”。

---

## 2026-04-07 审阅补充（正确性 / 性能 / 测试 / design 对齐）

### 结论摘要
1. 当前代码仍存在会影响求值正确性的缺陷，需先修复再继续迁移。
2. 当前性能瓶颈主要在缓存键设计、上下文绑定方式、IIS 停止条件缺失。
3. 测试覆盖已改善，但仍缺关键回归：系数保持、对偶/割链路、IIS 配置生效、上下文隔离。
4. 仅完成现有 P0/P1/P2 迁移项，**不足以**声明达到 `E:\workspace\ospf-rust\ospf-rust-core\design.md` 的架构演进结果；还需补齐 context 生命周期、FunctionSymbol context integration、二次 dual/farkas/cut、solver dual 输出等能力。

### 一、审阅意见（按优先级）

#### A. 正确性（P0）
1. 线性单项式 `evaluate(values, ...)` 在 `values` 命中分支丢失系数乘法：  
   - `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/frontend/expression/monomial/LinearMonomial.kt:298-300`
2. 二次单项式 `evaluate(results, ...)` / `evaluate(values, ...)` 多处分支丢失系数或重复乘系数：  
   - `.../QuadraticMonomial.kt:525,535,565-567,592,624`
3. `QuadraticMonomialCell.equals` 类型判断错误，当前误判为 `LinearMonomialCell`：  
   - `.../QuadraticMonomial.kt:433`
4. 二次对偶/法卡斯/割链路未完成（功能正确性缺口而非仅性能问题）：  
   - `backend/intermediate_model/QuadraticTetradModel.kt:901,905`  
   - `frontend/model/mechanism/MechanismModel.kt:788,796`

#### B. 性能与可伸缩性（P1）
1. `ValueCacheContext` 直接使用 `Pair<Any, List<Flt64>?>` 和 `Pair<Any, Map<Symbol, Flt64>>` 作为 key，带来高 hash 成本和对象分配压力：  
   - `frontend/model/mechanism/TokenCacheContext.kt:82-83,90-124`
2. 全局 `symbol -> tokenTable` 绑定表存在跨模型污染风险，并放大并发同步成本：  
   - `TokenCacheContext.kt:237-253`
3. `SolveValueConversionPolicy` 采用 `ThreadLocal`，与协程/异步 (`GlobalScope.future`) 组合下语义脆弱：  
   - `backend/solver/value/SolveValueConversionContext.kt:5-21`  
   - `backend/solver/SolverExt.kt:647+`
4. IIS 删除过滤循环没有消费 `IISConfig.time/threadNum/notImprovementTime`，可能出现长时间无界迭代：  
   - `backend/solver/iis/IISConfig.kt:10-24`  
   - `backend/solver/iis/Linear.kt:326-397`  
   - `backend/solver/iis/Quadratic.kt:446-517`

#### C. 测试缺口（P1）
1. 缺 “系数保持” 反向回归：当前基线测试把现有错误行为当作预期（需改为正确数学语义）。
2. 缺 `QuadraticMonomialCell.equals/hashCode` 合约测试。
3. 缺二次 `dual/farkas/optimal-cut/feasible-cut` 端到端回归。
4. 缺 IIS 配置生效测试（time limit、迭代上限、无改进超时）。
5. 缺多 `TokenTable` 并存时的 context 隔离与重绑定测试。

### 二、与 Rust design.md 的对齐差距

对照 `design.md`（特别是 context 初始化流程与 `Design Delta 2026-03-24`）的关键差距：
1. 缺 `ensure_flatten/value/range_context` 的模型内生命周期管理与结构变化后重绑定语义（Rust 在 `basic_model.rs` 已落地）。
2. 缺 `register_auxiliary_tokens(...)` / `evaluate_from_tokens(...)` 统一钩子语义（当前 Kotlin 仍以 `register(...)` + `prepare(...)` 为主）。
3. solver 输出缺统一 dual 向量通道，不利于对偶/割统一消费。
4. 二次链路 (`dual/farkas/cut`) 未闭环。
5. 启发式 `migration` 模块仍有多处 TODO，与 design 中“阶段 11 已完成”不一致。

> 判定标准：上述 1~4 任一未完成，都不能宣称“达到 design.md 架构演进结果”。

### 三、详细改进计划（交接执行版）

#### Phase 0：建立可回归基线（先做）
1. 固化命令基线：  
   - `mvn -pl ospf-kotlin-core -am test`  
   - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`
2. 先补“应当失败”的回归测试（覆盖当前已知 bug），再修代码。
3. 输出基线报告：记录失败用例、失败原因、影响模块。

验收：基线报告可复现，失败点与本节问题清单逐条对应。

#### Phase 1：正确性热修（阻断项，必须先清零）
1. 修复 `LinearMonomial` / `QuadraticMonomial` 求值系数问题。
2. 修复 `QuadraticMonomialCell.equals` 类型判断。
3. 新增/修正单测：
   - `evaluate(values)` 与 `evaluate(results)` 的系数一致性；
   - 二次项 `x*y` 与线性提升项系数一致性；
   - `equals/hashCode` 对称性、传递性、一致性。

验收：新增测试通过，且不会引入现有 symbol_migration 回归退化。

#### Phase 2：context 架构对齐（Design Delta 核心）
1. 在模型层引入三类 context 的显式生命周期管理（初始化、失效、重绑定）。
2. 将结构变化（add/remove symbol/token）与 context 重绑定绑定在同一事务语义中。
3. 逐步替换全局 `symbolTokenTableContext` 绑定方式，避免跨模型共享状态。
4. 引入 Kotlin 等价钩子：
   - `registerAuxiliaryTokens(...)`
   - `evaluateFromTokens(...)`
   并把默认求值路径改为：token-eval 优先 -> legacy prepare 回退 -> value cache 写回。

验收：多模型并发场景下，缓存与上下文不串扰；新增 context 隔离测试全部通过。

#### Phase 3：完成 Cell -> math.symbol 主通道迁移（承接现有 P0/P1）
1. 删除 `toLinearMonomialCells` / `toQuadraticMonomialCells` 等兼容桥。
2. flatten、polynomial、inequality 全链路改为 `math.symbol` 原生结构。
3. 清理 `core` 中对旧 cell 泛型约束与重载残留。

验收：`ospf-kotlin-core` 对外 API 不再以 Cell 作为核心数据通道。

#### Phase 4：二次 dual/farkas/cut + solver dual 输出闭环
1. 完成 `QuadraticTetradModel.dual()/farkasDual()`。
2. 完成 `QuadraticMechanismModel.generateOptimalCut()/generateFeasibleCut()`。
3. 在统一 solver 输出层补 dual（必要时区分 linear/quadratic dual）。
4. 插件适配层（gurobi/scip/copt...）补 dual 读取并透传。

验收：二次子问题可从 solver 输出直接生成可行割与最优割，并有端到端测试。

#### Phase 5：性能与稳定性优化
1. 重构 value cache key，避免直接使用大对象 `List/Map` 作为哈希键。
2. IIS 真正消费 `time/threadNum/notImprovementTime`，并补停止条件日志。
3. 将 `ThreadLocal` policy 改为协程上下文安全方案（或显式参数透传）。

验收：  
1) IIS 长跑不会无界；  
2) 并发求解下 policy 不串；  
3) 缓存命中率与耗时在基准算例上可观测改善。

#### Phase 6：测试补齐与达标判定
1. 补齐以下测试簇：
   - correctness：系数保持、dual/cut 数学等价；
   - architecture：context 重绑定/隔离；
   - performance-guard：IIS 停止条件与缓存键退化保护；
   - compatibility：8 plugin 编译回归。
2. 更新文档：迁移说明、API 对照、已知不兼容点。

验收（可宣称“达到 design.md”）：
1. Phase 1~5 全部完成且无 P0/P1 缺陷挂起；
2. `design delta` 两个关键钩子已落地并有测试；
3. 二次 dual/farkas/cut 与 solver dual 输出链路闭环；
4. 全量回归命令通过并附结果记录。

### 四、执行顺序（下一环境直接照此推进）
1. 先跑 Phase 0 建基线并提交失败报告。
2. 再做 Phase 1（正确性热修）并立刻回归。
3. 按 Phase 2 -> 3 做架构迁移（每个子阶段都跑核心回归）。
4. 完成 Phase 4 后再做 Phase 5（避免性能优化掩盖功能缺陷）。
5. 最后 Phase 6 收敛，并在本文件回写：
   - 已完成阶段；
   - 剩余阻断项；
   - 回归命令与结果。
