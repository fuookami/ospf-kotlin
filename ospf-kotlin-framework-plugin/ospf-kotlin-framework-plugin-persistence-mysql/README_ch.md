# ospf-kotlin-framework-plugin-persistence-mysql

:us: [English](README.md) | :cn: 简体中文

MySQL 持久化插件，提供受管理的 Ktorm、MyBatis-Plus 和 Spring JdbcClient 适配器。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MySQLClientKey` | data class | 客户端查找键（名称 + 数据库） |
| `MySQLConfigBuilder` | data class | `MySQLConfig` 的流式构建器 |
| `MySQLConfig` | data class | MySQL 连接配置（URL、名称、数据库、凭据、连接池设置） |
| `MySQL` | object | 客户端管理器；按键共享 DBCP 数据源，并提供三种适配器入口 |
| `MySQLJdbcClientDialect` | object | JdbcClient SQL 方言，负责标识符引用、分页和空值排序 |
| `MySQL.initKtorm` / `getKtormBackend` | 工厂 | 返回带 MySQL 表达式方言的 `Ret<KtormBackend>` |
| `MySQL.initMybatis` / `getMybatisBackend` | 工厂 | 返回带 MySQL MyBatis 方言的 `Ret<MybatisBackend>` |
| `MySQL.initJdbcClient` / `getJdbcClient` | 工厂 | 返回带 MySQL JdbcClient 方言的 `Ret<JdbcClientBackend>` |

## 快速开始

```kotlin
val database = MySQL.init {
    url = "localhost:3306"
    name = "my-app"
    database = "production"
    userName = "root"
    password = "secret"
}!!

// 配合 KtormRepository 使用
class OrderRepository(
    database: Database,
    table: Table<*>
) : KtormRepository<Order>(database, table, ...) {
    ...
}
```

JdbcClient 入口返回 `Ret`，并复用 Ktorm 使用的 DBCP 数据源缓存：

```kotlin
val backend = MySQL.initJdbcClient {
    url = "localhost:3306"
    name = "my-app"
    database = "production"
    userName = "root"
    password = "secret"
}

if (backend.ok) {
    val jdbcClient = backend.value!!.client
}
```

新代码可使用 `MySQL.initKtorm { ... }` 获取带匹配表达式方言的 `Ret<KtormBackend>`，或使用 `MySQL.initMybatis { ... }` 获取 `Ret<MybatisBackend>`。旧的 `MySQL.init` 和返回可空值的 Ktorm 查找重载仍保留以兼容现有代码。三个新适配器工厂及其按键/名称查找均以结构化 `Ret` 返回结果。

工厂创建的是延迟连接的 JdbcClient，不会检测数据库是否可达；连接错误会由仓储操作以 `Ret` 返回。使用 `MySQL.getJdbcClient(MySQLClientKey("my-app", "production"))` 按键精确查找；未找到时返回失败的 `Ret`。同一 key 下的三种适配器共用 DBCP 数据源。应用不再使用连接池时调用 `MySQL.close(key)`；重复关闭也会成功，关闭后该 key 已创建的 Ktorm、MyBatis 和 JdbcClient 后端都会受影响。

## 连接池

使用 Apache Commons DBCP2，可配置 `maxTotal`、`maxIdle` 和 `maxOpenPreparedStatements`。额外连接属性可通过 `MySQLConfigBuilder.properties` 设置。JdbcClient 配置错误以结构化错误返回，错误消息不包含凭据。

## 真实数据库集成测试

三类真实数据库测试（`MySQLMybatisIntegrationTest`、`MySQLKtormLiveIntegrationTest`、`MySQLJdbcClientLiveIntegrationTest`）在未设置 `OSPF_TEST_MYSQL_HOST` 时会跳过。请设置 `OSPF_TEST_MYSQL_USER` 和 `OSPF_TEST_MYSQL_PASSWORD`；`OSPF_TEST_MYSQL_PORT` 可选，默认 `3306`。测试账户需要创建和删除数据库的权限，并能在临时数据库中建表和执行 CRUD。MyBatis 与 JdbcClient 测试会校验 MySQL 8.4。每个测试都会创建随机命名的数据库并在结束后删除，不会使用现有业务数据库。

```powershell
$env:OSPF_TEST_MYSQL_HOST = "127.0.0.1"
$env:OSPF_TEST_MYSQL_PORT = "3306"
$env:OSPF_TEST_MYSQL_USER = "<测试账户>"
$env:OSPF_TEST_MYSQL_PASSWORD = "<测试密码>"
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-mysql -am '-Dtest=MySQLMybatisIntegrationTest,MySQLKtormLiveIntegrationTest,MySQLJdbcClientLiveIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

设置主机后，缺少凭据、连接失败、执行版本校验的测试遇到版本不匹配或权限不足，都会使相应测试失败。
