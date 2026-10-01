# ospf-kotlin-framework-plugin-persistence-h2

:us: [English](README.md) | :cn: 简体中文

H2 插件使用 DBCP2 管理共享连接池，为 Ktorm、MyBatis-Plus 和 Spring JdbcClient 提供适配器。

## 快速开始

```kotlin
val backend = H2.initJdbcClient {
    url = "jdbc:h2:mem:orders"
    name = "orders-test"
}

if (backend.ok) {
    val client = backend.value!!.client
}
```

配置 JDBC URL 时使用 `jdbc:h2:` 前缀。构建器默认用户名为 `sa`，密码为空；`properties` 用于增加 JDBC 连接属性。`H2.initJdbcClient`、`H2.initKtorm` 和 `H2.initMybatis` 均返回 `Ret`，并复用同一数据源缓存。JdbcClient 工厂使用延迟连接，不会检测数据库是否可达；连接错误会由仓储操作以 `Ret` 返回。配置错误通过 `Ret` 返回，消息不包含凭据。

## 公开 API

| 符号 | 说明 |
| --- | --- |
| `H2ConfigBuilder` / `H2Config` | 连接信息和 DBCP 连接池配置 |
| `H2ClientKey` | 客户端查找键（名称） |
| `H2.initKtorm` / `getKtormBackend` | 返回带 H2 表达式方言的 `Ret<KtormBackend>` |
| `H2.initMybatis` / `getMybatisBackend` | 返回带 H2 MyBatis 方言的 `Ret<MybatisBackend>` |
| `H2.initJdbcClient` / `getJdbcClient` | 返回带 H2 JdbcClient 方言的 `Ret<JdbcClientBackend>` |
| `H2` | 按键/名称查找和受管理连接池关闭 |
| `H2JdbcClientDialect` | 标识符引用、参数化分页和空值排序 |
| `H2KtormDialect` | 支持 H2 分页的 Ktorm SQL formatter |

同一 key 下创建的三种适配器共用 DBCP 数据源。`H2.close(key)` 会关闭并移除连接池；重复调用也会成功，该 key 先前创建的 Ktorm、MyBatis 和 JdbcClient 后端都无法继续使用连接池。内存数据库在连接池关闭时释放连接。若 JDBC URL 包含 `DB_CLOSE_DELAY=-1`，最后一个连接关闭后 H2 仍会保留内存数据库。
