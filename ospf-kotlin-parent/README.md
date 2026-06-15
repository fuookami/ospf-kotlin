# ospf-kotlin-parent

:us: English | :cn: [简体中文](README_ch.md)

## Introduction

`ospf-kotlin-parent` is the shared Maven parent for OSPF Kotlin modules. It imports the repository dependency BOM, configures common compiler and packaging plugins, and defines repository-wide build defaults.

## Boundary

This module is a build parent only. It should not host runtime code, domain APIs, or module-specific behavior.

## Responsibility

| Area | Description |
| --- | --- |
| Build defaults | Kotlin/JVM target, compiler flags, source directories, and plugin inheritance |
| Plugin management | Kotlin, compiler, surefire, failsafe, source jar, dokka, deploy, and enforcer plugins |
| Dependency baseline | Imports [ospf-kotlin-dependencies](../ospf-kotlin-dependencies/README.md) |

## Consumers

Most repository modules inherit this parent directly. Module-specific POM files should keep only local coordinates, packaging, dependencies, profiles, and exceptional plugin configuration.

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-parent validate
```
