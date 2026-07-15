# ospf-kotlin-starter-gantt-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-starter-gantt-scheduling` 是面向甘特调度 framework 栈的 starter artifact。

## 包含模块

本 starter 依赖通用 [ospf-kotlin-starter](../ospf-kotlin-starter/README_ch.md) 以及甘特调度 framework 模块：

| 模块 | 用途 |
| --- | --- |
| `ospf-kotlin-framework-gantt-scheduling-infrastructure` | 时间与调度基础设施 |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-context` | 任务领域模型 |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-generation-context` | 任务生成 |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-compilation-context` | 任务级 MILP 编译 |
| `ospf-kotlin-framework-gantt-scheduling-domain-bunch-generation-context` | 定价与任务组生成 |
| `ospf-kotlin-framework-gantt-scheduling-domain-bunch-compilation-context` | 主问题任务组编译 |
| `ospf-kotlin-framework-gantt-scheduling-domain-capacity-scheduling-context` | 产能调度 |
| `ospf-kotlin-framework-gantt-scheduling-domain-produce-context` | 产出与消耗追踪 |
| `ospf-kotlin-framework-gantt-scheduling-domain-resource-context` | 资源容量建模 |
| `ospf-kotlin-framework-gantt-scheduling-application` | application 层求解流程 |

## 使用方式

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 本地验证

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-gantt-scheduling -am -DskipTests package
```
