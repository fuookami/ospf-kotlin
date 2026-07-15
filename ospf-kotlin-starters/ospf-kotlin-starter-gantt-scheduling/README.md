# ospf-kotlin-starter-gantt-scheduling

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-starter-gantt-scheduling` is the starter artifact for the Gantt scheduling framework stack.

## Included Modules

This starter depends on the general [ospf-kotlin-starter](../ospf-kotlin-starter/README.md) and the Gantt scheduling framework modules:

| Module | Purpose |
| --- | --- |
| `ospf-kotlin-framework-gantt-scheduling-infrastructure` | Time and scheduling infrastructure |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-context` | Task domain models |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-generation-context` | Task generation |
| `ospf-kotlin-framework-gantt-scheduling-domain-task-compilation-context` | Task-level MILP compilation |
| `ospf-kotlin-framework-gantt-scheduling-domain-bunch-generation-context` | Pricing and bunch generation |
| `ospf-kotlin-framework-gantt-scheduling-domain-bunch-compilation-context` | Master problem bunch compilation |
| `ospf-kotlin-framework-gantt-scheduling-domain-capacity-scheduling-context` | Capacity scheduling |
| `ospf-kotlin-framework-gantt-scheduling-domain-produce-context` | Production and consumption tracking |
| `ospf-kotlin-framework-gantt-scheduling-domain-resource-context` | Resource capacity modeling |
| `ospf-kotlin-framework-gantt-scheduling-application` | Application-level solving workflows |

## Usage

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-gantt-scheduling -am -DskipTests package
```
