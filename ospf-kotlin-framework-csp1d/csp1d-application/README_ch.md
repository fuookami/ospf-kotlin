# csp1d-application

[English](README.md)

CSP1D 框架应用层。编排问题定义、求解（MILP 和列生成）、恢复、求解结果丰富和 KPI 渲染。

## 职责

- 定义 CSP1D 问题和求解配置（Csp1dProblem、Csp1dSolveConfig）
- 编排普通 MILP 求解（Csp1dMilp）
- 编排列生成求解（Csp1dColumnGeneration）：LP 主问题、定价循环、最终 MILP
- 管理影子价格生命周期（Csp1dShadowPriceLifecycle）
- 支持从先前求解结果热启动恢复（Csp1dRecovery）
- 丰富求解结果的 KPI、trace 和 render 数据
- 支持 Top-K 切割方案选取
- 定义求解结果分析器接口

## 核心类型

| 类型 | 描述 |
|------|------|
| `Csp1dProblem<V>` | 问题定义，含产品、物料、需求和配置 |
| `Csp1dSolveConfig<V>` | 一站式求解配置，含扩展和策略 |
| `Csp1dColumnGeneration<V>` | 列生成求解器，含 LP 主问题、定价和最终 MILP |
| `Csp1dMilp<V>` | 普通 MILP 求解器 |
| `Csp1dMilpSolver<V>` | 底层 MILP 求解器，含模型构建和结果提取 |
| `Csp1dProduceContext<V>` | 模型上下文，桥接 ProduceAggregation 和扩展 |
| `Csp1dShadowPriceLifecycle<V>` | 影子价格刷新和提取，基于 CGPipeline |
| `Csp1dRecovery<V>` | 恢复求解器，支持热启动适配器 |
| `Csp1dSolution<V>` | 求解结果，含产出、KPI、render、状态和 trace |
| `Csp1dColumnGenerationTrace` | 列生成追踪，含终止原因和统计 |
| `Csp1dSolutionAnalyzer<V>` | 自定义求解结果分析器接口 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.core` — MetaModel、solver 输出
- `fuookami.ospf.kotlin.framework` — Pipeline、CGPipeline、ColumnGenerationSolver
- `csp1d-infrastructure` — 渲染 DTO
- `csp1d-domain-material-context` — 领域实体、影子价格键
- `csp1d-domain-cutting-plan-generation-context` — 生成器、定价
- `csp1d-domain-produce-context` — ProduceAggregation、策略、扩展
- `csp1d-domain-yield-context` — 产出偏差分析和建模
- `csp1d-domain-wasting-minimization-context` — 浪费分析
- `csp1d-domain-length-assignment-context` — 长度分配
