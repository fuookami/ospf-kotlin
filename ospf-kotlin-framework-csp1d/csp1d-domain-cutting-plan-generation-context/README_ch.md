# csp1d-domain-cutting-plan-generation-context

[English](README.md)

CSP1D 框架切割方案生成领域上下文。提供枚举和定价两类生成器，用于产出候选切割方案。

## 职责

- 通过 DFS、N-Sum、N-Same 和 FullSum 枚举器生成初始切割方案
- 通过 reduced cost 定价器生成定价候选方案
- 管理生成约束（刀数、超产长度、幅宽边界）
- 提供 canonical key 去重和 dominance 剪枝
- 支持按物料并行生成，含跨 worker 去重
- 缓存数量计算、宽度索引和切片模板以提升性能

## 缓存构建边界

DFS、N-Sum 和 FullSum 仅在模板缓存开启时创建切片模板记录器，物料并行路径同样如此。
自定义约束关闭模板复用时，不再记录最终会丢弃的叶节点模板；方案预算、截止时间检查、
canonical 身份和约束回调保持既有行为。

宽度索引缓存仍串行处理冷构建，包括自定义幅宽回调。模板缓存的读取与发布仍分离：并发的等价物料
可能重复一次冷搜索，不让 worker 等待其他 worker 完整执行 DFS，也不发布不完整或超时的模板。
按键并发或后台预热需要独立的纯策略及截止时间/取消契约，不能仅从“未使用自定义约束”推断安全。

## 核心类型

| 类型 | 描述 |
|------|------|
| `DFSGenerator<V>` | 栈式深度优先搜索，枚举多产品组合 |
| `NSumGenerator<V>` | 深度受限 DFS，枚举宽度求和组合 |
| `NSameGenerator<V>` | 为每个产品-宽度组合生成单产品方案 |
| `FullSumGenerator<V>` | 枚举所有宽度求和组合 |
| `ReducedCostPricingGenerator<V>` | 基于 shadow price 和 reduced cost 的定价生成器 |
| `CostarFiller<V>` | 用配规切片填充剩余宽度 |
| `GenerationConstraints<V>` | 约束配置（刀数、长度、并行度、dominance） |
| `CuttingPlanConstraint<V>` | 可组合约束谓词，用于剪枝和可行性判断 |
| `CuttingPlanCanonicalKey` | 结构化去重键 |
| `GenerationCollector<V>` | 收集和过滤候选，含去重和 dominance |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.quantities` — 物理量类型
- `csp1d-domain-material-context` — 领域实体
