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
| `MongoRepository<E>` | abstract class | Base repository implementing the `Ret`-based `ExpressionRepository<E>` contract |
| `MongoBooleanTranslator` | class | `BooleanExpression` → `Ret<Bson?>` filter |
| `MongoScalarTranslator` | class | `ScalarExpression<*>` → `Ret<Any?>` `$expr` value |
| `MongoOrderByTranslator` | class | `SortBy` → `Ret<Bson?>` sort |
| `MongoUpdateTranslator` | class | `UpdateAssignments` → `Ret<Bson?>` update |

## Repository Results

All expression repository operations return `Ret`: `find`, `count`, `exists`, `update`, and `delete`. The default unsupported-predicate policy is `FailFast`; translation failures are returned to the caller, and `ClientFilter` also returns a structured failure because MongoDB repositories do not evaluate predicates in memory. With `AlwaysFalse`, an unsupported predicate at the root becomes a filter that matches no documents. The three-valued `Unknown` constant also matches no documents, including under nested `NOT`; Mongo translation pushes `NOT` through `AND` and `OR` while preserving `Unknown`.

Negative `limit` or `offset` values return a failed result, while `limit = 0` returns `Ok(emptyList())` without querying MongoDB. An update is sent only after its filter and every assignment have translated successfully. MongoDB operation, cursor iteration, and entity-mapping exceptions become failed results; cancellation exceptions are rethrown.

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
