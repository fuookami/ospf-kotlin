# ospf-kotlin

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fuookami.ospf.kotlin/ospf-kotlin)](https://mvnrepository.com/artifact/io.github.fuookami.ospf.kotlin/ospf-kotlin)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.24-yellow.svg?logo=kotlin)](http://kotlinlang.org)

## Introduction

ospf-kotlin is the Kotlin/JVM implementation version of ospf. For more detailed information, documentation or examples, please refer to the main repository and documentation page:

ospf: https://github.com/fuookami/ospf

documentation: https://fuookami.github.io/ospf/

:us: English | :cn: [简体中文](README_ch.md)

## Installation

Requirements:

* JDK: 17+ or 8+
* maven: 3+

ospf-kotlin has been released to the maven central repository. Therefore, if you are using maven, you only need to add a dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the bpp1d development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp1d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp1d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the bpp2d development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp2d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the bpp3d development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-bpp3d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the csp1d development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp1d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the csp2d development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-csp2d-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the gantt scheduling development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-gantt-scheduling-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```

If you need to use the network scheduling development package, add the dependency in the pom.xml file:

```xml
<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling</artifactId>
    <version>1.1.0</version>
</dependency>

<dependency>
    <groupId>io.github.fuookami.ospf.kotlin</groupId>
    <artifactId>ospf-kotlin-starter-network-scheduling-jdk8</artifactId>
    <version>1.1.0</version>
</dependency>
```


## Generic API Migration

From `1.1.0`, the core modeling and solver bridge are aligned to V-typed generic APIs (`Flt64` / `Rtn64` / `FltX` / `RtnX`).

Recommended usage:

1. Use `LinearMetaModel<V>` / `QuadraticMetaModel<V>` with explicit `IntoValue<V>`.
2. Prefer V-typed objective fields from solver output:
   - `objValue`
   - `possibleBestObjValue`
   - `bestBoundValue`
3. Keep legacy `Flt64` fields (`obj`, `possibleBestObj`, `bestBound`) only for compatibility.

Example generic demo verification (isolated profile):

```bash
mvn --% -pl ospf-kotlin-example -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

## Migration Release Gate (P16)

Use one command to run the default migration acceptance gate:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1
```

Optional solver-gated run (requires SCIP/JNI environment):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1 -WithSolverIntegration
```

This script covers:

1. Core source-compat and math bridge/DSL tests.
2. Default example compile/test.
3. `core-demo-only`, `build-only-function-tests`, `business-source-compat`, `framework-starter-compat`.
4. `check-c8-guards.ps1` in `P6` and `P7` modes (including P10/P11/P12/P14/P16 static guards).
5. P16 restoration checks: no `src/non-default-main` / `src/non-default-test` roots and no legacy `core.frontend.*` / `core.backend.*` / `utils.math.*` imports in default example source sets.

## Migration Entry Quick Reference

1. Package migration direction:
   - `core.frontend.*` -> `core.model.*` / `core.variable.*` / `core.intermediate_symbol.*`
   - `core.backend.*` -> `core.solver.*`
   - modeling expressions and inequalities are migrated under `math.symbol.*`
2. Four-number-type converter entry:
   - `Flt64`: `IntoValue.Identity`
   - `FltX`: `FltX.toIntoValue()`
   - `Rtn64`: `Rtn64.toIntoValue()`
   - `RtnX`: `RtnX.toIntoValue()`
3. Starter/framework migration verification entry:
   - run `-Pbusiness-source-compat` for business-facing source-compat fixtures
   - run `-Pframework-starter-compat` for starter/framework dependency-closure fixtures

## License

The ospf-kotlin is licensed under the terms of the Apache License 2.0.

See [LICENSE](LICENSE) for more information.

