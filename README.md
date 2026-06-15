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


## Changelog

See [1.1.0 release notes](changelog/1.1.0.md) for the full change record.

## Benchmark Baseline

Benchmark baseline module: `ospf-kotlin-benchmark` (JMH 1.37), currently covering `multiarray` / `math` / `core` and `core-plugin dump` hot paths.

Compile benchmark module:

```bash
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

Run a `small` smoke benchmark (example):

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

Run a `core-plugin dump` smoke benchmark (example):

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* small 1 1 1"
```

The `small`/`medium`/`large` dataset parameter is available. `medium`/`large` are intended for manual trend comparison, not default CI gates.

Benchmark result files are written to:

1. default path: `ospf-kotlin-benchmark/target/benchmark-results/*.json`
2. custom path (optional): `-Dexec.args="... json target/benchmark-results/custom.json"`

CI uploads `ospf-kotlin-benchmark/target/benchmark-results/` as an artifact for retention only, including `ci-smoke.json`, `baseline-smoke.json`, `current-smoke.json`, and `trend-smoke.md`; it is not a performance gate.

Compare two JMH JSON result files and write a Markdown trend report:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json
```

If `-Output` is omitted and file naming follows `baseline-<dataset>.json` / `current-<dataset>.json`, the script writes `trend-<dataset>.md` to the same directory by default.

You can also use directory mode:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

If the directory contains exactly one matched `baseline-*.json` / `current-*.json` pair, `-Dataset` can be omitted.

You can still pass explicit output path when needed:

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json `
  -Output .\ospf-kotlin-benchmark\target\benchmark-results\trend-small.md
```

The comparison script is report-only. It avoids performance hard-gating because JMH scores are machine-sensitive.

Lightweight CI smoke for benchmark runnable validation (without comparing absolute scores):

```powershell
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

Baseline environment used in P21-1:

1. JDK: GraalVM JDK 17.0.12
2. Maven: Apache Maven 3.9.12
3. OS: Windows (PowerShell)
4. JVM opts recommendation (for frequent CodeHeap warnings):
   - PowerShell (current session):
     - ``$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - Windows persistent user variable:
     - ``setx MAVEN_OPTS "-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - bash/zsh:
     - `export MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"`

## License

The ospf-kotlin is licensed under the terms of the Apache License 2.0.

See [LICENSE](LICENSE) for more information.

