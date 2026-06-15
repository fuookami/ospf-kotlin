# ospf-kotlin-framework-bpp1d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-bpp1d` 是规划中的一维装箱框架模块。当前它还是 Maven 模块壳，没有已跟踪的 Kotlin 实现。本 README 用来记录预期边界，使后续实现从一开始就保持与其他 framework 模块一致的架构和文档形态。

## 作用范围

本模块预留给可复用的 BPP1D 领域建模能力，例如物品/箱体定义、装箱策略、候选生成、模型注册和解提取。

不属于本模块的内容：

1. 业务请求 DTO 和租户级运行参数。
2. 求解器后端实现。
3. starter 依赖聚合。

## Public API

当前还没有稳定的运行时 public API。后续 API 应暴露泛型领域模型和 context，而不是 solver 特化的 `Flt64` 模型。

## 建模扩展点

后续实现应遵循 framework 架构规则：

1. `MetaModel` 注册放在 domain context、aggregation、model component 和 pipeline 类型中。
2. 只有在可复用领域内核存在后，application 层才负责求解编排。
3. 为额外约束、目标、影子价格提取和解增强预留扩展点。

## 泛型数值边界

主领域 API 应基于 `V : RealNumber<V>` 或 `V : FloatingNumber<V>` 泛型化。solver adapter 可以特化为 `Flt64`，但转换应集中在 adapter 或注册边界。

## 物理量边界

长度、宽度、重量、容量等有量纲字段应使用 `Quantity<V>` 或模块内统一量纲包装类型。裸数值只用于无量纲评分、比例和系数。

## 相关说明

- [daily.md](daily.md) 记录当前泛型化计划。
- [ospf-kotlin-framework](../ospf-kotlin-framework/README_ch.md) 提供共享求解器和 pipeline 抽象。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-bpp1d -am -DskipTests compile
```
