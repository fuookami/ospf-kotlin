# ospf-kotlin-starter-bpp2d（中文）

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-bpp2d` 是面向二维装箱使用场景的 starter artifact。它引入通用
[ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md)，以及
[ospf-kotlin-framework-bpp2d](../../ospf-kotlin-framework-bpp2d/README_ch.md) 的全部模块：

1. `ospf-kotlin-framework-bpp2d-infrastructure`
2. `ospf-kotlin-framework-bpp2d-domain-item-context`
3. `ospf-kotlin-framework-bpp2d-domain-bla-context`
4. `ospf-kotlin-framework-bpp2d-domain-block-loading-context`
5. `ospf-kotlin-framework-bpp2d-domain-layer-generation-context`
6. `ospf-kotlin-framework-bpp2d-domain-layer-assignment-context`
7. `ospf-kotlin-framework-bpp2d-domain-packing-context`
8. `ospf-kotlin-framework-bpp2d-application`

## 边界

本 starter 只负责聚合依赖，不包含任何代码。求解器插件（例如 `ospf-kotlin-core-plugin-gurobi`）**不在**其中，
因为求解器选择属于应用层决策。矩形与圆形排样、列生成与渲染 DTO 输出都在上述框架模块内。

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.1</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp2d -am -DskipTests package
```
