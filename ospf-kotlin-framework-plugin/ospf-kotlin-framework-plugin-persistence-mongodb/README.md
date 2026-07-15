# ospf-kotlin-framework-plugin-persistence-mongodb

:us: English | :cn: [简体中文](README_ch.md)

MongoDB persistence plugin for the OSPF Kotlin framework, providing document repository with expression-to-Bson translation and API request/response persistence.

## Public API

### Client Management

| Symbol | Kind | Description |
| --- | --- | --- |
| `MongoClientKey` | data class | Client lookup key (name + database) |
| `MongoDBConfigBuilder` | data class | Fluent builder for `MongoDBConfig` |
| `MongoDBConfig` | data class | MongoDB connection configuration |
| `MongoDB` | object | Client manager; indexes `MongoClient` instances by key |

### Extension Functions

| Symbol | Description |
| --- | --- |
| `MongoDatabase.insert(collection, data)` | Insert with default serializer |
| `MongoDatabase.insert(collection, serializer, data)` | Insert with KSerializer |
| `MongoDatabase.insert(collection, serializer, data)` | Insert with custom serialization lambda |
| `MongoDatabase.get(collectionName, query)` | Query with default deserializer |
| `MongoDatabase.get(collectionName, deserializer, query)` | Query with KSerializer |
| `MongoDatabase.get(collectionName, deserializer, query)` | Query with custom deserialization lambda |

### API Persistence

| Symbol | Kind | Description |
| --- | --- | --- |
| `MongoPersistenceApiController` | interface | Mixin for persisting API requests/responses to MongoDB |
| `RequestRecordPO` | data class | Internal request record wrapper |
| `ResponseRecordPO` | data class | Internal response record wrapper |

### Expression Translation

| Symbol | Kind | Description |
| --- | --- | --- |
| `MongoFieldNameResolver` | typealias | `PersistenceFieldResolver<String>` |
| `MongoRepository<E>` | abstract class | Base repository implementing `ExpressionRepository<E>` on MongoDB |
| `MongoBooleanTranslator` | class | `BooleanExpression` → `Bson` filter |
| `MongoScalarTranslator` | class | `ScalarExpression<*>` → `$expr` value |
| `MongoOrderByTranslator` | class | `SortBy` → `Bson` sort |
| `MongoUpdateTranslator` | class | `UpdateAssignments` → `Bson` update |

## Quick Start

```kotlin
// Initialize MongoDB
val db = MongoDB.init {
    urls = listOf("localhost:27017")
    name = "my-app"
    database = "production"
    userName = "admin"
    password = "secret"
}!!.getDatabase("production")

// Insert
db.insert("orders", orderDto)

// Query
val results = db.get<OrderDTO>("orders", mapOf("code" to "ORD-001"))

// Repository pattern
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

## API Request/Response Persistence

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
            // process request
            OrderResponse(...)
        }
    }
}
```
