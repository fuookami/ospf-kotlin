# csp1d-domain-length-assignment-context

[English](README.md)

CSP1D 框架长度分配领域上下文。处理动态卷长推导和超长检测。

## 职责

- 通过注入的 LengthDerivation 从需求量和产品属性推导卷长
- 检测超长违规（对比 maxOverProduceLength）
- 注册长度分配变量、约束和目标到 MetaModel
- 支持下界/上界约束和超长松弛变量

## 核心类型

| 类型 | 描述 |
|------|------|
| `LengthAssignmentContext<V>` | 使用注入的推导函数执行长度分配 |
| `LengthDerivation<V>` | 从需求推导卷长的函数式接口 |
| `LengthAssignmentInput<V>` | 输入，含动态产品、需求和约束 |
| `LengthAssignmentResult<V>` | 结果，含分配列表和超长记录 |
| `LengthAssignment<V>` | 单个产品的分配卷长和批次数 |
| `LengthAssignmentModelingConfig<V>` | 边界、惩罚和批次最小化配置 |
| `LengthAggregation<V>` | 聚合根，管理长度变量和约束 |
| `LengthConstraintPipeline<V>` | 约束管线，长度边界约束 |
| `LengthObjectivePipeline<V>` | 目标管线，长度相关惩罚 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.core` — MetaModel、变量
- `fuookami.ospf.kotlin.framework` — Pipeline
- `csp1d-domain-material-context` — 领域实体
- `csp1d-domain-produce-context` — Csp1dAggregation、ProduceAggregation
