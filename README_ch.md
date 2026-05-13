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

## 寮€婧愬崗璁?

ospf-kotlin 鏄牴鎹?Apache 璁稿彲璇?2.0 鐗堟湰鐨勬潯娆捐鍙殑銆?

鏇村淇℃伅璇锋煡鐪?[LICENSE](LICENSE)銆?

