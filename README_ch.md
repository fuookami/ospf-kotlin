# ospf-kotlin

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-green.svg?style=flat)](http://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fuookami.ospf.kotlin/ospf-kotlin)](https://mvnrepository.com/artifact/io.github.fuookami.ospf.kotlin/ospf-kotlin)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-yellow.svg?logo=kotlin)](http://kotlinlang.org)

## 浠嬬粛

ospf-kotlin 鏄?ospf 鐨?Kotlin/JVM 瀹炵幇鐗堟湰锛屾洿璇︾粏鐨勪粙缁嶃€佹枃妗ｄ笌鏍蜂緥鍙互鍙傝€冧富浠撳簱涓庢枃妗ｉ〉闈細

ospf锛歨ttps://github.com/fuookami/ospf

鏂囨。锛歨ttps://fuookami.github.io/ospf/

:us: [English](README.md) | :cn: 简体中文

## 瀹夎

鐗堟湰瑕佹眰:

* JDK: 17+ or 8+
* Maven: 3+

ospf-kotlin 宸茬粡鍙戝竷鍒?maven 涓績浠撳簱锛屽洜姝わ紝濡傛灉浣犱娇鐢?maven 鐨勮瘽锛屽彧闇€瑕佸湪 pom.xml 鏂囦欢閲岄潰娣诲姞涓€涓緷璧栧嵆鍙細

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

濡傛灉浣犻渶瑕佷娇鐢ㄤ竴缁磋绠卞紑鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄤ簩缁磋绠卞紑鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄤ笁缁磋绠卞紑鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄤ竴缁翠笅鏂欏紑鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄤ簩缁翠笅鏂欏紑鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄧ敇鐗瑰浘鎺掔▼寮€鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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

濡傛灉浣犻渶瑕佷娇鐢ㄧ綉缁滄祦瑙勫垝寮€鍙戝寘锛屽垯鍦?pom.xml 鏂囦欢閲屾坊鍔犱緷璧栵細

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


## 泛型 API 迁移

从 `1.1.0` 起，core 建模与求解桥接主链路统一到 V-typed 泛型 API（`Flt64` / `Rtn64` / `FltX` / `RtnX`）。

推荐用法：

1. 使用 `LinearMetaModel<V>` / `QuadraticMetaModel<V>`，并显式提供 `IntoValue<V>`。
2. 优先使用求解输出中的 V-typed 目标值字段：
   - `objValue`
   - `possibleBestObjValue`
   - `bestBoundValue`
3. `obj` / `possibleBestObj` / `bestBound` 作为兼容字段保留，不建议作为新代码主入口。

泛型示例闭环验证（隔离 profile）：

```bash
mvn --% -pl ospf-kotlin-example -Pcore-demo-only -Dtest=CoreDemoTest,GenericNumberDemoTest -Dsurefire.failIfNoSpecifiedTests=false clean test
```

## Benchmark 基线

基准模块：`ospf-kotlin-benchmark`（JMH 1.37），当前覆盖 `multiarray` / `math` / `core` 以及 `core-plugin dump` 热点路径。

编译 benchmark 模块：

```bash
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

执行 `small` 烟测（示例）：

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

执行 `core-plugin dump` 烟测（示例）：

```bash
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*CorePluginDumpBenchmark.prepareVariableDumpingDataHotPath.* small 1 1 1"
```

当前支持 `small`/`medium`/`large` 数据规模。`medium`/`large` 仅用于手动趋势对比，不进入默认 CI 硬门禁。

benchmark 结果文件默认输出到：

1. 默认路径：`ospf-kotlin-benchmark/target/benchmark-results/*.json`
2. 可选自定义路径：`-Dexec.args="... json target/benchmark-results/custom.json"`

CI 会上传 `ospf-kotlin-benchmark/target/benchmark-results/` 目录作为 artifact，仅用于结果留存（包含 `ci-smoke.json`、`baseline-smoke.json`、`current-smoke.json`、`trend-smoke.md`），不作为性能硬门禁。

比较两份 JMH JSON 结果并输出 Markdown 趋势报告：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json
```

当省略 `-Output` 且命名遵循 `baseline-<dataset>.json` / `current-<dataset>.json` 时，脚本会默认在同目录输出 `trend-<dataset>.md`。

也可以使用目录模式：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir .\ospf-kotlin-benchmark\target\benchmark-results `
  -Dataset small
```

如果目录中仅存在一组可匹配的 `baseline-*.json` / `current-*.json`，可省略 `-Dataset`。

如需显式输出路径，也可传入 `-Output`：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -Baseline .\ospf-kotlin-benchmark\target\benchmark-results\baseline-small.json `
  -Current .\ospf-kotlin-benchmark\target\benchmark-results\current-small.json `
  -Output .\ospf-kotlin-benchmark\target\benchmark-results\trend-small.md
```

比较脚本只生成报告，不设置性能硬门禁，因为 JMH 分数对机器环境敏感。

benchmark 轻量 CI smoke 口径（只验证可运行，不比较绝对数值）：

```powershell
mvn --% -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
mvn --% -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java -Dexec.args=".*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json ospf-kotlin-benchmark/target/benchmark-results/ci-smoke.json"
```

P21-1 基线运行环境：

1. JDK：GraalVM JDK 17.0.12
2. Maven：Apache Maven 3.9.12
3. OS：Windows（PowerShell）
4. JVM 参数建议（缓解频繁 CodeHeap warning）：
   - PowerShell（当前会话）：
     - ``$env:MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - Windows 持久化用户变量：
     - ``setx MAVEN_OPTS "-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"``
   - bash/zsh：
     - `export MAVEN_OPTS="-XX:ReservedCodeCacheSize=512m -XX:NonProfiledCodeHeapSize=192m -XX:ProfiledCodeHeapSize=192m"`

## 迁移统一门禁（无兼容层）

使用一条命令执行默认迁移验收门禁：

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1
```

可选：执行 solver-gated 验收（需要 SCIP/JNI 环境）：

```powershell
pwsh.exe -NoProfile -ExecutionPolicy Bypass -File .\ospf-kotlin-core\scripts\check-migration-compat.ps1 -WithSolverIntegration
```

该脚本覆盖：

1. core source-compat 与 math bridge/DSL 测试。
2. example 默认 `compile/test`。
3. `core-demo-only`、`build-only-function-tests`、`business-source-compat`、`framework-starter-compat`。
4. `check-c8-guards.ps1` 的 `P6` 与 `P7` 模式（含 P10/P11/P12/P14/P16/P17 静态门禁）。
5. 无兼容层检查：`ospf-kotlin-example/src` 下不再存在 `non-default-main` / `non-default-test`，默认源码集不允许旧 `core.frontend.*` / `core.backend.*` / `utils.math.*` import 回流，也不允许 `math.symbol.adapter.*`、`FunctionCompat`、`MetaModelFlt64Adapter`、旧 `solver(metaModel)` 与旧无 converter 的 `SlackFunction` 调用回流。

## 迁移入口速查

1. 包路径迁移方向：
   - `core.frontend.*` -> `core.model.*` / `core.variable.*` / `core.symbol.*`
   - `core.backend.*` -> `core.solver.*`
   - 表达式与不等式主入口迁移到 `math.symbol.*`
   - `math.symbol.adapter.*` -> `math.symbol.operation.*`
   - 直接 meta-model solver 调用 -> 显式 dump 到 mechanism/triad 后求解
2. 四类型数值 converter 入口：
   - `Flt64`：`IntoValue.Identity`
   - `FltX`：`FltX.toIntoValue()`
   - `Rtn64`：`Rtn64.toIntoValue()`
   - `RtnX`：`RtnX.toIntoValue()`
3. framework/starter 迁移验收入口：
   - `-Pbusiness-source-compat`：业务 source-compat fixture
   - `-Pframework-starter-compat`：starter/framework 依赖闭包 fixture

## 寮€婧愬崗璁?

ospf-kotlin 鏄牴鎹?Apache 璁稿彲璇?2.0 鐗堟湰鐨勬潯娆捐鍙殑銆?

鏇村淇℃伅璇锋煡鐪?[LICENSE](LICENSE)銆?

