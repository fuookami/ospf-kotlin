# ospf-kotlin-framework-plugin-persistence-mongodb

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 框架的 MongoDB 持久化插件，提供文档仓储（表达式到 Bson 翻译）和 API 请求/响应持久化。

## 公开 API

### 客户端管理

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MongoClientKey` | data class | 客户端查找键（名称 + 数据库） |
| `MongoDBConfigBuilder` | data class | `MongoDBConfig` 的流式构建器 |
| `MongoDBConfig` | data class | MongoDB 连接配置 |
| `MongoDB` | object | 客户端管理器；按键索引 `MongoClient` 实例 |

### 扩展函数

| 符号 | 说明 |
| --- | --- |
| `MongoDatabase.insert(collection, data)` | 使用默认序列化器插入 |
| `MongoDatabase.insert(collection, serializer, data)` | 使用 KSerializer 插入 |
| `MongoDatabase.insert(collection, serializer, data)` | 使用自定义序列化 lambda 插入 |
| `MongoDatabase.get(collectionName, query)` | 使用默认反序列化器查询 |
| `MongoDatabase.get(collectionName, deserializer, query)` | 使用 KSerializer 查询 |
| `MongoDatabase.get(collectionName, deserializer, query)` | 使用自定义反序列化 lambda 查询 |

### API 持久化

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MongoPersistenceApiController` | interface | 将 API 请求/响应持久化到 MongoDB 的混入接口 |
| `RequestRecordPO` | data class | 内部请求记录包装 |
| `ResponseRecordPO` | data class | 内部响应记录包装 |

### 表达式翻译

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `MongoFieldNameResolver` | typealias | `PersistenceFieldResolver<String>` |
| `MongoRepository<E>` | abstract class | 实现 `Ret` 结果型 `ExpressionRepository<E>` 契约的 MongoDB 仓储基类 |
| `MongoBooleanTranslator` | class | `BooleanExpression` → `Ret<Bson?>` 过滤器 |
| `MongoScalarTranslator` | class | `ScalarExpression<*>` → `Ret<Any?>` `$expr` 值 |
| `MongoOrderByTranslator` | class | `SortBy` → `Ret<Bson?>` 排序 |
| `MongoUpdateTranslator` | class | `UpdateAssignments` → `Ret<Bson?>` 更新 |

## 仓储结果

表达式仓储的 `find`、`count`、`exists`、`update` 和 `delete` 均返回 `Ret`。不支持谓词时默认采用 `FailFast`，翻译失败会返回给调用方；`ClientFilter` 也会返回结构化失败，因为 MongoDB 仓储不会在内存中执行谓词过滤。使用 `AlwaysFalse` 时，仅当根谓词不支持时才会生成匹配不到文档的过滤器。三值常量 `Unknown` 始终不匹配文档，即使位于嵌套 `NOT` 中也是如此；MongoDB 翻译会将 `NOT` 下推到 `AND` 和 `OR`，同时保留 `Unknown`。

负数 `limit` 或 `offset` 会返回失败结果；`limit = 0` 会返回 `Ok(emptyList())`，且不查询 MongoDB。只有条件和每一项赋值都翻译成功后才会发送更新。MongoDB 操作、游标遍历和实体映射中的普通异常会转成失败结果；取消异常会继续抛出。

## 快速开始

```kotlin
// 初始化 MongoDB
val db = MongoDB.init {
    urls = listOf("localhost:27017")
    name = "my-app"
    database = "production"
    userName = "admin"
    password = "secret"
}!!.getDatabase("production")

// 插入
db.insert("orders", orderDto)

// 查询
val results = db.get<OrderDTO>("orders", mapOf("code" to "ORD-001"))

// 仓储模式
class OrderRepository(
    database: MongoDatabase
) : MongoRepository<Order>(
    database = database,
    collectionName = "orders",
    resolveFieldName = MongoRepository.simpleFieldResolver()
) {
    override fun mapToEntity(document: Document): Order? = TODO()
}
```

## API 请求/响应持久化

```kotlin
class MyController : MongoPersistenceApiController {
    override val mongoClient: MongoDatabase? = MongoDB()?.getDatabase("api_logs")

    fun handleOrder(request: CreateOrderRequest): OrderResponse {
        return persistenceApiImpl(
            api = "/orders",
            app = "order-service",
            requester = "user-1",
            version = "v1",
            request = request
        ) { req ->
            // 处理请求
            OrderResponse(...)
        }
    }
}
```
