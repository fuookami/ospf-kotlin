# ospf-kotlin-starter-bpp3d

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-bpp3d` 是面向 BPP3D framework 栈的 starter artifact。

## 包含模块

本 starter 依赖通用 [ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md) 以及 BPP3D framework 模块：

| 模块 | 用途 |
| --- | --- |
| `ospf-kotlin-framework-bpp3d-infrastructure` | 共享几何、DTO 和领域原语 |
| `ospf-kotlin-framework-bpp3d-domain-item-context` | 物品、物料、箱体、层、模式和影子价格模型 |
| `ospf-kotlin-framework-bpp3d-domain-packing-context` | 装箱、校验、渲染输出和解组装 |
| `ospf-kotlin-framework-bpp3d-domain-bla-context` | Bottom-Up Left-Justified 放置 context |
| `ospf-kotlin-framework-bpp3d-domain-block-loading-context` | block 生成与搜索 context |
| `ospf-kotlin-framework-bpp3d-domain-layer-generation-context` | 层候选生成 |
| `ospf-kotlin-framework-bpp3d-domain-layer-assignment-context` | 层分配模型与 pipeline |
| `ospf-kotlin-framework-bpp3d-application` | application 层列生成编排 |

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-bpp3d -am -DskipTests package
```
