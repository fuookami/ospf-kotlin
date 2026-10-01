# ospf-kotlin-framework-plugin-persistence-postgresql

:us: [English](README.md) | :cn: 简体中文

PostgreSQL 插件使用 DBCP2 管理共享连接池，为 Ktorm、MyBatis-Plus 和 Spring JdbcClient 提供适配器。

## 快速开始

```kotlin
val backend = PostgreSQL.initJdbcClient {
    url = "localhost:5432"
    name = "orders"
    database = "production"
    userName = "app"
    password = "secret"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

`url` 填 PostgreSQL 主机和可选端口；数据库名称单独配置。`properties` 用于增加 JDBC 连接属性。工厂创建的是延迟连接的 JdbcClient，不会检测数据库是否可达；连接错误会由仓储操作以 `Ret` 返回。配置构建器以结构化 `Ret` 错误报告缺失或无效配置，消息不包含凭据。

## 公开 API

| 符号 | 说明 |
| --- | --- |
| `PostgreSQLConfigBuilder` / `PostgreSQLConfig` | 连接信息和 DBCP 连接池配置 |
| `PostgreSQLClientKey` | 精确客户端查找键（名称 + 数据库） |
| `PostgreSQL.initKtorm` / `getKtormBackend` | 返回带 PostgreSQL 表达式方言的 `Ret<KtormBackend>` |
| `PostgreSQL.initMybatis` / `getMybatisBackend` | 返回带 PostgreSQL MyBatis 方言的 `Ret<MybatisBackend>` |
| `PostgreSQL.initJdbcClient` / `getJdbcClient` | 返回带 PostgreSQL JdbcClient 方言的 `Ret<JdbcClientBackend>` |
| `PostgreSQL` | 按键/名称查找和受管理连接池关闭 |
| `PostgreSQLJdbcClientDialect` | 标识符引用、参数化分页和原生空值排序 |

三个适配器工厂和查找入口都返回结构化 `Ret`。使用 `PostgreSQL.getJdbcClient(key)` 按键精确查找。`PostgreSQL.close(key)` 关闭共享的受管理连接池；连接池已关闭时再次调用仍成功，该 key 先前创建的 Ktorm、MyBatis 和 JdbcClient 后端都无法继续使用连接池。通过外部 `DataSource` 创建的 JdbcClient 由调用方管理生命周期。

## 真实数据库集成测试

三类真实数据库测试（`PostgreSQLMybatisIntegrationTest`、`PostgreSQLKtormLiveIntegrationTest`、`PostgreSQLJdbcClientLiveIntegrationTest`）在未设置 `OSPF_TEST_POSTGRESQL_HOST` 时会跳过。请设置 `OSPF_TEST_POSTGRESQL_USER` 和 `OSPF_TEST_POSTGRESQL_PASSWORD`；`OSPF_TEST_POSTGRESQL_PORT` 可选，默认 `5432`。测试账户需要 PostgreSQL `CREATEDB` 属性，并能在临时数据库中创建 schema、表和执行 CRUD。MyBatis 与 JdbcClient 测试会校验 PostgreSQL 17.11。每个测试都会创建随机命名的数据库并在结束后删除，不会使用现有业务数据库。

```powershell
$env:OSPF_TEST_POSTGRESQL_HOST = "127.0.0.1"
$env:OSPF_TEST_POSTGRESQL_PORT = "5432"
$env:OSPF_TEST_POSTGRESQL_USER = "<测试账户>"
$env:OSPF_TEST_POSTGRESQL_PASSWORD = "<测试密码>"
mvn -pl ospf-kotlin-framework-plugin/ospf-kotlin-framework-plugin-persistence-postgresql -am '-Dtest=PostgreSQLMybatisIntegrationTest,PostgreSQLKtormLiveIntegrationTest,PostgreSQLJdbcClientLiveIntegrationTest' '-Dsurefire.failIfNoSpecifiedTests=false' test
```

设置主机后，缺少凭据、连接失败、执行版本校验的测试遇到版本不匹配或权限不足，都会使相应测试失败。
