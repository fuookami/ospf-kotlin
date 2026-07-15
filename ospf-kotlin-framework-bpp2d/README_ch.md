# ospf-kotlin-framework-bpp2d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-bpp2d` 是早期阶段的二维矩形装箱框架模块。当前它包含一个最小的、物理量感知的矩形装箱需求与几何映射领域内核。

## 作用范围

本模块聚焦可复用的 BPP2D 概念：

1. 矩形物品、板材、放置和装箱场景。
2. 带物理量的投影、放置和盒体需求。
3. 映射到共享 quantity geometry 模型，且不依赖 BPP3D 包。

下游请求 DTO、租户参数、项目专属策略、求解器选择和服务编排，应先留在模块外，直到它们沉淀为可复用领域概念。

## Public API

当前生产入口包括：

| API | 职责 |
| --- | --- |
| `RectangleItem2<V>` | 矩形物品尺寸和是否允许旋转 |
| `Sheet2<V>` | 板材尺寸 |
| `Projection2Need<V>` | 宽高投影和面积 |
| `Placement2Need<V>` | 位置与投影 |
| `Box2Need<V>` | 边界、重叠、相交和包含检查 |
| `PlannedRectangle2<V>` | 带可选旋转的物品放置 |
| `PackingScene2<V>` | 板材内检查、面积、利用率和重叠分析 |
| `toGeometryProjection2()` / `toGeometryPlacement2()` / `toGeometryBox2()` | 到 quantity geometry 的稳定适配器 |

## 建模扩展点

本模块尚未暴露求解模型或 application solver。后续加入优化建模时，模型注册应放在 domain context、aggregation、model component 和 pipeline 中，而不是硬编码在 application service。

## 泛型数值边界

当前领域模型基于 `V : FloatingNumber<V>` 泛型化。未来 solver adapter 可以把值转换为 `Flt64`，但可复用领域层应保持泛型。

## 物理量边界

宽度、高度、坐标和面积都通过 `Quantity<V>` 表达。利用率等无量纲值暴露为 `V`。

## 相关说明

- [daily.md](daily.md) 记录实现和泛型化计划。
- [ospf-kotlin-math geometry README](../ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/geometry/README_ch.md) 说明共享几何层。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-bpp2d -am test
```
