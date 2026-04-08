# OSPF Kotlin Core 纯重写路线（Big-Bang，math.symbol 单内核）

日期：2026-04-09  
用途：当团队决定不做渐进迁移，而是一次性切换到 `math.symbol` 符号运算体系时使用。

---

## 0. 架构前提（必须遵守）

1. 线性/二次符号运算能力由 `math.symbol` 提供。
2. `core` 只负责：
   - 建立变量（variable）
   - 建立中间值（intermediate symbol）
   - 建立模型（model/mechanism）
3. `core` 不再承载 monomial/polynomial/inequality 的运算实现。
4. 原 `monomial/polynomial` 的附加职责（`cell` 直接使用 `math.symbol.monomial`、`value`、`range`）统一迁移到元模型上下文（`TokenCacheContexts` / `MetaModel` / `MechanismModel`）处理。

这条路线的本质不是“重写 core 表达内核”，而是“删除 core 内重复代数能力，统一回 `math.symbol`”。

---

## 1. 目标与非目标

### 目标（Big-Bang 一次达成）
1. 函数符号层全面切到 `math.symbol + relation/flatten`。
2. `core` 约束构建只保留 relation 路径。
3. 删除旧目录：
   - `frontend/expression/monomial`
   - `frontend/expression/polynomial`
   - `frontend/expression/adapter`
   - `frontend/expression/Expression.kt`
   - `frontend/inequality`

### 非目标（明确禁止）
1. 不在 `core` 新增一套线性/二次代数实现。
2. 不复制 `math.symbol` 的运算符体系到 `core`。
3. 不长期保留 old DSL 与 new DSL 双轨。

---

## 2. 何时采用这条路线

仅在以下条件满足时采用：
1. 接受 1~2 周功能冻结窗口（仅修阻断问题）。
2. 接受一次集中重构风险，换长期结构简化。
3. 团队可连续投入开发，不被频繁中断。

不满足时，继续执行 `daily.md` 的增量路线。

---

## 3. 切换前基线（当天重采样）

记录并冻结以下指标：
1. `frontend.inequality` 外部 import 数。
2. `frontend.expression.monomial/polynomial` 外部 import 数。
3. `expression/symbol` 内 `.cells` 读取数。
4. 核心回归、全量回归、插件编译结果。

这些数字用于评估重写收益、定位回退点。

---

## 4. 执行步骤（B0~B9）

### B0：冻结与分支（0.5 天）
目标：建立可回退的大改环境。

执行：
1. 新建 `rewrite-bigbang` 分支。
2. 冻结主线功能开发。
3. 写入基线扫描与测试结果。

验收：
1. 有明确冻结点 commit。
2. 有完整切换前基线数据。

---

### B1：core 边界 API 定稿（0.5~1 天）
目标：先定边界，再改实现。

执行：
1. 冻结 `core` 输入输出接口：`LinearFlattenData/QuadraticFlattenData`、`LinearRelation/QuadraticRelation`。
2. 明确函数符号到模型层的统一出口：`addConstraint(relation: ...)`。
3. 去掉新方案里对旧 inequality 类型的依赖假设。

验收：
1. API 清单冻结。
2. model/mechanism 对接路径单一且明确。

---

### B2：math.symbol -> core 桥接层重建（1~2 天）
目标：让 `core` 只消费 `math.symbol` 结果，不自行算代数。

执行：
1. 实现从 `math.symbol` 表达结果到 `LinearFlattenData/QuadraticFlattenData` 的桥接。
2. 实现 relation 构建 helper（`eq/leq/geq` -> relation）。
3. 清理/替换依赖旧 monomial/polynomial 的中间转换点。
4. 将 `cell`、`value`、`range` 相关运行期处理集中到元模型上下文服务。

验收：
1. 约束构建可在不触发旧 expression 目录代码的前提下工作。
2. 桥接层只做数据映射，不做重复代数运算。
3. `value/range` 不再由旧表达类型分散维护。

---

### B3：函数符号层一次性改造（2~3 天）
目标：56 个函数符号统一改为“调用 `math.symbol` + relation 构建”。

执行：
1. 统一替换函数符号内部旧 DSL 构造逻辑。
2. 清理 `.cells` 主路径读取，统一改为 `flattenedMonomials`/relation 路径。
3. 将函数符号中的 `value/range` 逻辑改为通过元模型上下文读写。
4. 对 register/evaluate 做行为对齐修复。

验收：
1. `expression/symbol` 不再直接依赖 `frontend.inequality`。
2. `.cells` 不再参与主计算路径。
3. `value/range` 处理路径统一经过元模型上下文。

---

### B4：模型层收口（1 天）
目标：模型侧不再要求旧 inequality 类型。

执行：
1. 将 model/mechanism 主入口固定为 relation API。
2. 将旧 inequality 入口降级为临时 shim（仅应急）。
3. 清理对旧 expression 类型的强耦合 import。

验收：
1. 主流程可仅依赖 relation 跑通。
2. 旧入口不再是默认路径。

---

### B5：一次性切流（0.5~1 天）
目标：主路径切换到新实现，不维持长期双轨。

执行：
1. 全量替换 `src/main` 到新路径调用。
2. 保留最多一个版本周期的应急开关（可选）。
3. 立即处理编译断点。

验收：
1. 默认执行路径只走新方案。
2. 旧路径不可在常规流程被触发。

---

### B6：同窗口删除旧目录（0.5~1 天）
目标：切流后立刻收口，防回流。

执行：
1. 删除第 1 节列出的所有旧目录。
2. 清理残留 import、adapter、兼容胶水。
3. 修复编译到 `ospf-kotlin-core` 全绿。

验收：
1. `src/main` 无旧路径 import。
2. `ospf-kotlin-core` 编译通过。

---

### B7：集中回归（1~2 天）
目标：收敛 big-bang 后的功能偏差。

执行：
1. 跑阶段小回归。
2. 跑全量回归。
3. 按“阻断 > 功能 > 性能”顺序修复。

验收：
1. 小回归全绿。
2. 全量回归目标全绿。

---

### B8：插件编译校验（0.5 天）
目标：验证下游可编译。

执行：
1. 执行插件编译检查。
2. 修复必要 API 透传问题。

验收：
1. 插件编译检查通过，或有已登记非阻断问题清单。

---

### B9：封口门禁与文档（0.5 天）
目标：防止回流并形成交付说明。

执行：
1. 新增 CI 守卫：禁止旧路径 import。
2. 新增守卫：禁止 `.cells` 参与主计算。
3. 更新迁移文档，明确“`core` 不提供符号运算能力”。

验收：
1. 守卫进入 CI 必跑。
2. 文档可指导后续开发。

---

## 5. 回归命令

1. 阶段小回归：
   - `mvn -pl ospf-kotlin-core "-Dtest=MonomialCoefficientPreservationTest,FlattenMigrationGuardTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest,InequalityNormalizeBaselineTest,TokenCacheContextsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
2. 全量回归：
   - `mvn -pl ospf-kotlin-core -am test`
3. 插件编译检查：
   - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`

---

## 6. 止损与回退

硬止损条件（任一触发即暂停）：
1. B3 后函数符号层仍大量依赖旧 inequality 路径。
2. B6 后 1 天内无法恢复 core 可编译状态。
3. B7 后核心回归仍存在阻断级失败。

回退策略：
1. 只回退到 B0 冻结点或上一个稳定阶段。
2. 回退后 24 小时内决策：
   - 继续 big-bang（补资源）
   - 切回 `daily.md` 增量路线

---

## 7. 最终完成标准

1. `core` 不再承载线性/二次符号运算实现。
2. `src/main` 无旧路径 import：
   - `frontend.inequality`
   - `frontend.expression.monomial`
   - `frontend.expression.polynomial`
   - `frontend.expression.adapter`
3. 函数符号主路径完全基于 `math.symbol + relation/flatten`。
4. `mvn -pl ospf-kotlin-core -am test` 通过。
5. 插件编译检查通过。
6. `cell`、`value`、`range` 运行期处理集中在元模型上下文，不再由旧表达类型维护。
