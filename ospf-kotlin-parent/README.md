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

## Test Jar Publishing Toggle

The `maven-jar-plugin` `test-jar` goal produces a `classifier=tests` jar so local `install`
builds can reuse test artifacts across modules. Maven Central does not require this attachment
(only `-sources.jar` and `-javadoc.jar` are mandatory), and no module in the repository depends
on a sibling's `tests.jar`, so it is not published.

The `ospf.test-jar.phase` property controls the phase this execution is bound to and defaults to
`none`, so neither `package` nor `deploy` produces or uploads `tests.jar` unless asked to. Pass
`package` explicitly to produce it locally; test compilation and execution are unaffected either
way:

```powershell
# Publish: plain deploy, no tests.jar (the default)
mvn -B -ntp deploy

# Local: also produce tests.jar for cross-module reuse
mvn -B -ntp install "-Dospf.test-jar.phase=package"
```

## Local Validation

```powershell
mvn -B -ntp -pl ospf-kotlin-parent validate
```
