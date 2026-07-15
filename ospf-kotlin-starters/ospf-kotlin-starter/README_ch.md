# ospf-kotlin-starter

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter` 是 OSPF Kotlin 的通用 starter artifact。它聚合 core 优化建模和 framework 基础设施所需的常用运行时模块。

## 边界

本模块只聚合依赖，不应包含运行时代码、领域逻辑或求解器后端实现。

## 包含模块

| 模块 | 用途 |
| --- | --- |
| `ospf-kotlin-utils` | 通用工具和函数式结果类型 |
| `ospf-kotlin-multiarray` | 多维数组基础 |
| `ospf-kotlin-math` | 数学与符号基础 |
| `ospf-kotlin-quantities` | 物理量与单位支持 |
| `ospf-kotlin-core` | 优化建模核心 |
| `ospf-kotlin-framework` | 共享 framework 抽象 |

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

需要接入具体求解后端时，再额外添加 solver plugin。

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter -am -DskipTests package
```
