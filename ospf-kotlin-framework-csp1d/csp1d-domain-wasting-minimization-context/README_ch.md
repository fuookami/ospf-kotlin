# csp1d-domain-wasting-minimization-context

[English](README.md)

CSP1D 框架浪费最小化领域上下文。分析和量化选中切割方案中的浪费（余宽、余料面积）。

## 职责

- 按切割方案分析余宽浪费（按批次数倍增）
- 分析余料面积代理浪费（余宽 × 物料长度）
- 注册浪费惩罚目标（余宽、余料面积、物料成本）

## 核心类型

| 类型 | 描述 |
|------|------|
| `WastingMinimizationContext<V>` | 分析选中切割方案的浪费 |
| `WasteAnalysis<V>` | 分析结果，含余宽/余料浪费列表和汇总 |
| `RestWidthWaste<V>` | 切割方案的余宽浪费记录 |
| `RestMaterialWaste<V>` | 余料面积代理浪费记录 |
| `WasteAggregation<V>` | 浪费目标配置的聚合根 |
| `WasteObjectivePipeline<V>` | 目标管线，添加浪费惩罚项 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.core` — MetaModel、变量
- `fuookami.ospf.kotlin.framework` — Pipeline
- `csp1d-domain-material-context` — 领域实体
- `csp1d-domain-produce-context` — ProduceAggregation、Csp1dAggregation
