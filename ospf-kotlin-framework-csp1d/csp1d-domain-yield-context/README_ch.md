# csp1d-domain-yield-context

[English](README.md)

CSP1D 框架产出偏差领域上下文。分析产出偏差（欠产/超产），注册产出相关约束和目标。

## 职责

- 按产品+单位聚合需求贡献，分析产出偏差
- 检测欠产（短缺）和超产（盈余）
- 注册产出松弛变量和偏差约束到 MetaModel
- 注册欠产/超产惩罚目标
- 基于 CGPipeline 提取超产上限影子价格

## 核心类型

| 类型 | 描述 |
|------|------|
| `YieldContext<V>` | 从产出和需求分析产出偏差 |
| `YieldAggregation<V>` | 聚合根，管理产出松弛变量和约束 |
| `YieldModelingConfig<V>` | 欠产/超产惩罚和上限配置 |
| `YieldAnalysis<V>` | 分析结果，含欠产/超产列表和产出 |
| `YieldObjectivePipeline<V>` | 目标管线，添加欠产/超产惩罚 |
| `YieldConstraintPipeline<V>` | CGPipeline，超产上限约束和影子价格 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.core` — MetaModel、变量、表达式
- `fuookami.ospf.kotlin.framework` — Pipeline、CGPipeline
- `csp1d-domain-material-context` — 领域实体和影子价格键
- `csp1d-domain-produce-context` — Csp1dAggregation
