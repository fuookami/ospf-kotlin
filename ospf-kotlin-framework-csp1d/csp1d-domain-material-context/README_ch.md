# csp1d-domain-material-context

[English](README.md)

CSP1D 框架物料领域上下文。定义产品、物料、设备、切割方案、需求和领域策略的核心领域模型。

## 职责

- 定义产品、物料、设备和配规领域实体
- 定义切割方案模型（切片、需求贡献、已用/剩余幅宽）
- 定义产品需求模型，支持离散/连续物理量
- 定义领域计算上下文和策略接口，支持下游扩展
- 定义影子价格键层级和轻量级影子价格映射
- 提供物理量算术抽象和 solver 值转换工具
- 提供渲染映射器用于 UI 序列化

## 核心类型

| 类型 | 描述 |
|------|------|
| `Product<V>` | 产品，支持宽度、长度、单位重量和动态长度 |
| `Material<V>` | 分切物料，含幅宽范围、设备绑定和批次数 |
| `Machine<V>` | 分切设备，含批次数上限、幅宽范围和产能 |
| `Costar<V>` | 配规/副产物，填充剩余宽度（无需求贡献） |
| `CuttingPlan<V>` | 切割方案，含切片、需求贡献和已用/剩余幅宽 |
| `ProductDemand<V>` | 产品需求，含物理量和口径标签（卷数/重量/张数） |
| `Csp1dDomainPolicy<V>` | 领域策略接口，用于宽度可行性和方案判断 |
| `Csp1dShadowPriceKey` | 影子价格键层级（需求、物料、设备、产出） |
| `ShadowPriceMap<V>` | 轻量级影子价格容器，用于定价 |
| `QuantityArithmetic<V>` | 物理量加减抽象，适配领域数值类型 |

## 依赖

- `fuookami.ospf.kotlin.utils` — 函数式工具
- `fuookami.ospf.kotlin.math` — 代数数值类型
- `fuookami.ospf.kotlin.quantities` — 物理量类型
- `fuookami.ospf.kotlin.framework` — 影子价格框架
- `csp1d-infrastructure` — 渲染 DTO
