# ospf-kotlin-framework-csp2d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-csp2d` 是规划中的二维下料框架模块。当前它还是 Maven 模块壳，没有已跟踪的 Kotlin 实现。

## 作用范围

本模块预留给可复用的 CSP2D 领域建模能力：

1. 板材、产品、切割模式和切割方案模型。
2. 带物理量的坐标、尺寸、面积和损耗表达。
3. 面向未来 MILP 或列生成注册的 model component 与 pipeline。

业务 DTO 协议、公式语言、项目运行参数和求解器插件选择应留在下游 adapter 中。

## Public API

当前还没有稳定的运行时 public API。后续 API 应从泛型、物理量感知的领域类型开始，而不是固定 `Double` 或 `Flt64` 模型。

## 建模扩展点

后续实现应把优化模型装配放在 context、aggregation、model component 和 pipeline 中。application service 负责求解编排与恢复，不直接堆积约束和目标项。

## 泛型数值边界

可复用领域层应基于 `V : RealNumber<V>` 或 `V : FloatingNumber<V>` 泛型化。solver 特定的 `Flt64` 转换应隔离在 adapter 中。

## 物理量边界

坐标、裁切位置、板材尺寸、产品尺寸、已用面积和损耗面积应使用 `Quantity<V>` 或专用二维量纲类型。裸 `V` 只用于无量纲利用率、损耗率、惩罚和评分。

## 相关说明

- [daily.md](daily.md) 记录当前泛型化计划。
- [ospf-kotlin-framework-csp1d](../ospf-kotlin-framework-csp1d/README_ch.md) 提供较成熟的一维下料参考。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-framework-csp2d -am -DskipTests compile
```
