# csp1d-domain-produce-context

[English](README.md)

CSP1D 框架产出领域上下文。管理主问题变量、中间符号和约束/目标注册。

## 职责

- 管理 x 变量（方案使用车次）和 batch 中间符号
- 注册需求、物料、设备批次和设备产能中间符号
- 支持列生成增量 addColumns，含 flush + asMutable
- 定义建模扩展点接口（Pipeline、CGPipeline、IncrementalPipeline）
- 定义策略接口（目标、生成、定价、流程、提取）
- 定义扩展包容器，统一承载所有策略类别

## 核心类型

| 类型 | 描述 |
|------|------|
| `ProduceAggregation<V>` | 聚合根，管理 x 变量和约束中间符号 |
| `Csp1dAggregation<V>` | 聚合根接口，注册到 MetaModel |
| `Csp1dModelContext<V>` | 模型注册和求解结果提取接口 |
| `Csp1dIterativeContext<V>` | 列生成上下文，含 addColumns 和影子价格提取 |
| `Csp1dModelingExtension<V>` | 承载额外管线，在求解各阶段注入 |
| `Csp1dFlowPolicy<V>` | 流程控制策略（初始方案过滤、等价判断、终止、partial） |
| `Csp1dExtractionPolicy<V>` | 求解结果丰富策略（自定义 KPI 详情） |
| `Csp1dExtensionSet<V>` | 统一容器，承载所有扩展策略 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.core` — MetaModel、变量、表达式
- `fuookami.ospf.kotlin.framework` — Pipeline、CGPipeline
- `csp1d-domain-material-context` — 领域实体
- `csp1d-domain-cutting-plan-generation-context` — canonical key
