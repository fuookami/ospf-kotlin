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
