# csp1d-infrastructure

[English](README.md)

CSP1D 框架基础设施层。包含跨领域层和应用层共享的枚举和 DTO。

## 职责

- 定义渲染 DTO（`RenderCuttingPlanDTO`、`RenderSchemaDTO`）用于 UI 序列化
- 定义共享枚举（`CuttingPlanProductOrder`）供领域模型使用

## 核心类型

| 类型 | 描述 |
|------|------|
| `CuttingPlanProductOrder` | 切割方案产品排序方式（升序、降序、用户自定义） |
| `RenderCuttingPlanProductionDTO` | 切割方案中单个产出项的可序列化 DTO |
| `RenderCuttingPlanDTO` | 完整切割方案渲染的可序列化 DTO |
| `RenderSchemaDTO` | 顶层渲染 Schema，包含 KPI 指标和切割方案列表 |
| `RenderProductionType` | 区分产品与配规的枚举 |

## 依赖

本模块无内部 CSP1D 依赖，仅依赖 `fuookami.ospf.kotlin.math`（数值类型）和 `kotlinx.serialization`（DTO 注解）。
