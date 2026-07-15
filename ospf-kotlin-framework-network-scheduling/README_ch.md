# ospf-kotlin-framework-network-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-network-scheduling` 是规划中的网络调度框架模块。当前它还是 Maven 模块壳，没有已跟踪的 Kotlin 实现。

## 作用范围

本模块预留给可复用的网络调度内核：

1. 网络节点、弧、路径、流量、容量和行程时间模型。
2. 带物理量的成本、需求、容量、吞吐和持续时间。
3. 面向未来网络 LP/MIP 或分解流程的 model component 与 pipeline。

项目专属 DTO、租户上下文、心跳逻辑和求解器插件选择属于下游 adapter。

## Public API

当前还没有稳定的运行时 public API。后续 API 应暴露泛型图结构和物理量感知的领域类型。

## 建模扩展点

后续实现应把变量、约束、目标和结果提取注册放在 domain context、aggregation、model component 和 pipeline 中。额外业务规则通过 extra context 或 pipeline hook 接入。

## 泛型数值边界

领域 API 应基于 `V : RealNumber<V>` 或 `V : FloatingNumber<V>` 泛型化。solver adapter 可以转换到 `Flt64`，但转换不应泄漏进可复用领域模型。

## 物理量边界

距离、持续时间、流量、需求、供给、容量、吞吐和成本应使用 `Quantity<V>` 或显式量纲包装类型。裸数值只用于无量纲权重、比例和归一化评分。

## 相关说明

- [daily.md](daily.md) 记录当前泛型化计划。
- [ospf-kotlin-framework](../ospf-kotlin-framework/README_ch.md) 提供共享求解器、pipeline 和远程求解抽象。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-network-scheduling -am -DskipTests compile
```
