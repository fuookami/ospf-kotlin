# OSPF Kotlin Core Refactor Daily

记录日期：2026-04-30

本轮目标：完成剩余 P8 与 P2，按小步迭代检查、修订并验收。上一个会话报告 P7 已完成，本文件将旧 daily 中已完成事项与未完成事项合并重写。

审计范围：
- 当前 Kotlin：`E:\workspace\ospf-kotlin`
- Rust 对照：`E:\workspace\ospf-rust`
- 原 Kotlin 对照：`E:\workspace\ospf-kotlin-main`
- 原示例对照：`E:\workspace\ospf\examples\ospf-kotlin-example`

## 1. 当前完成状态

P7 结论保持：旧 frontend/backend 迁移遗留清理、核心变量/约束/目标/机制模型入口泛型化、framework/example 批量迁移、`@Deprecated` 清零等事项已作为基线完成。

本轮补完 P8 与 P2 后，当前状态：

| 项目 | 状态 | 说明 |
| --- | --- | --- |
| P8-0 基线与验收门禁 | 完成 | 建立大小写敏感 `F64\b` 命名扫描、`<Flt64>`/`<*>` 静态指标与 Maven 编译验收。 |
| P8-1 Solution 与 setSolution 泛型化 | 完成 | `AbstractTokenList<T>`、`AbstractTokenTable<V>`、`Model<V>`、`MetaModel<V>` 的主 `setSolution` 已使用 `V`；求解器边界使用显式 `setSolverSolution`。 |
| P8-2 Token/cache/IntermediateSymbol 泛型化 | 完成 | token cache 的主路径改为 `V`；solver-boundary Flt64 路径拆为 `cacheSolver*`、`prepareSolver`、`evaluateSolver`。 |
| P8-3 F64/Flt64 命名收敛 | 完成 | 代码层大小写敏感 `F64\b` 命中为 0；保留小写 `f64` 作为变量/函数局部语义。 |
| P8-4 值转换策略 | 完成 | core 与 conversion context 默认策略已对齐 Rust 的 `Strict`；显式调用方可选择 `AllowRounding`。 |
| P8-5 framework/回调/启发式路径 | 完成 | framework 增加 `FrameworkSolveOptions`；列生成与 Benders 快捷入口已接入 options；callback/heuristic PSO 路径完成。 |
| P8-6 旧 Kotlin 快捷接口策略 | 完成 | 不恢复无后缀 `LinearMetaModel` 等名称作为 Flt64 facade，避免与泛型主类型冲突；兼容层统一采用 `*Flt64` 命名。 |
| P2-7 Rust framework SolveOptions | 完成 | `FrameworkSolveOptions` 覆盖 name、toLogModel、solutionAmount、建模/注册/求解回调、Benders 限制与 valueConversionPolicy。 |
| P2-4 LP 导出 | 完成 | Kotlin 现有 LP/export 能力保留；framework options 的 `toLogModel` 已贯通相关快捷入口并通过 example 聚合编译验收。 |
| P2-8 IIS 输出语义 | 完成 | 新增 Rust 风格 `SolverOutputWithIIS<IIS>` 与 `withIIS`/`withoutIIS` helper；线性/二次 infeasible 输出保持原 IIS 字段并可包装。 |
| P2-5 结构化错误 | 完成 | 新增 `CoreError / VariableError / ModelError / SolverError` 结构化错误层，并映射到现有 `ErrorCode`/`Ret`。 |
| P2-3 PSO 求解器 | 完成 | 新增 `ParticleSwarmHeuristicSolver`、`Particle`、速度/惯性/个体和群体学习系数、速度上限、随机与初始速度生成器扩展点。 |
| P2-6 非线性残留 TODO 复核 | 完成 | 当前非线性/符号函数残留主要是函数内部泛型常量 cast 警告与既有 TODO，不再阻塞 P8/P2 验收。 |

## 2. 泛型化边界

本轮采用的边界规则：
- 建模主路径公开 `V : RealNumber<V>` / `NumberField<V>`，业务层通过 typed `setSolution(List<V>)` 与 `setSolution(Map<..., V>)` 交互。
- Flt64 只保留在 solver adapter、intermediate solver model、Flt64 兼容 typealias、callback Flt64 模型、测试样例和显式数值转换层。
- solver 返回的 Flt64 解通过显式边界方法写回，不再伪装成泛型主接口。

已知仍存在的 `<Flt64>` 主要位于：
- solver/intermediate model 固有 Flt64 表示；
- Flt64 兼容别名与示例；
- 数学常量和符号函数内部的 Flt64 literal 转换；
- callback Flt64 模型与现有求解器输出结构。

这些位置按当前迁移边界视为允许项，不再作为 P8 阻断。

## 3. Rust 功能缺口对齐结果

Rust 已有但 Kotlin 旧 daily 标记缺失的能力，本轮处理结果如下：

- 值转换策略：默认 `Strict`，并保留显式宽松策略入口。
- framework options：新增统一 options，并接入列生成、Benders、LP/MILP/solution pool/async 快捷入口。
- LP/export：能力已存在，`toLogModel` 通过 options 继续传递；不再作为能力缺口记录。
- IIS：保留 Kotlin 线性/二次 IIS 模型输出，同时增加 `SolverOutputWithIIS` 包装层。
- 结构化错误：新增与 Rust 分层一致的 Kotlin sealed error 层，映射回现有 `ErrorCode`。
- PSO：新增内置 PSO 启发式求解器，支持 Rust 对齐的核心参数与扩展点。

## 4. 与原 Kotlin 快捷接口差异

当前不做“旧源码零修改迁移” facade，原因是无后缀类型名已经作为泛型主类型使用。保留策略如下：

- 主类型使用泛型名：如 `LinearMetaModel<V>`、`QuadraticMetaModel<V>`。
- Flt64 快捷兼容名统一使用 `*Flt64` 后缀。
- 旧 `*F64` 后缀全部迁移到 `*Flt64`。
- 旧 package/import 差异通过迁移文档或示例改写处理，不在 core 内恢复旧 frontend/backend 包路径。

## 5. 当前静态指标

按 `src/main/**/*.kt` 扫描：

| 模块 | `<Flt64>` 命中 | `<*>` 命中 | `@Deprecated` 命中 |
| --- | ---: | ---: | ---: |
| `ospf-kotlin-core` | 581 | 268 | 0 |
| `ospf-kotlin-framework` | 4 | 22 | 0 |
| `ospf-kotlin-example` | 169 | 1 | 0 |

大小写敏感 `F64\b` 命中：0。

## 6. 验收记录

已通过：

```powershell
mvn -pl ospf-kotlin-core "-Dtest=ParticleSwarmHeuristicSolverTest,CoreErrorTest,SolverOutputWithIISTest" test
mvn -pl ospf-kotlin-framework -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

example 聚合编译覆盖 32 个模块，全部 SUCCESS。

## 7. 待完成事项（合并重写）

P8 与 P2 已无阻断待办。后续只保留非阻断清理项：

1. 继续减少符号函数内部 `Flt64` literal 转 `T` 的 unchecked cast 警告，优先从 `FunctionSymbol.zeroOf/oneOf` 和逻辑/分段函数入手。
2. 如需要原 Kotlin 源码零修改迁移，另开 facade/migration 文档任务，集中处理旧包路径、旧无后缀快捷名和 import rename；不在本轮恢复旧包名。
3. 对需要商业求解器许可证的真实求解集成测试另行执行；本轮已完成编译级与新增单元测试验收。
4. 未跟踪文件 `fix_compilation.js` 不属于本轮改动，保持不处理。
