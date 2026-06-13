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
| `MongoRepository<E>` | abstract class | 基于 MongoDB 实现 `ExpressionRepository<E>` 的仓储基类 |
| `MongoBooleanTranslator` | class | `BooleanExpression` → `Bson` 过滤器 |
| `MongoScalarTranslator` | class | `ScalarExpression<*>` → `$expr` 值 |
| `MongoOrderByTranslator` | class | `SortBy` → `Bson` 排序 |
| `MongoUpdateTranslator` | class | `UpdateAssignments` → `Bson` 更新 |

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
