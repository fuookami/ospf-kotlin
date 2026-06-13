# ospf-kotlin-framework-plugin-persistence-redis

:us: English | :cn: [简体中文](README_ch.md)

Redis sentinel client management plugin with structured data and serialization extensions.

## Public API

### Client Management

| Symbol | Kind | Description |
| --- | --- | --- |
| `RedisClientKey` | data class | Client lookup key (name + database number) |
| `RedisConfigBuilder` | data class | Fluent builder for `RedisConfig` |
| `RedisConfig` | data class | Redis sentinel connection configuration |
| `RedisClient` | data class | Jedis connection wrapper implementing `AutoCloseable` |
| `Redis` | object | Client manager; indexes connection pool instances by key |

### Extension Functions — Set

| Symbol | Description |
| --- | --- |
| `RedisClient.set(name, value: String, ex?)` | Set string key-value |
| `RedisClient.set(name, value: List<String>, ex?)` | Set list key-value |
| `RedisClient.set(name, value: Set<String>, ex?)` | Set set key-value |
| `RedisClient.set(name, value: Map<String, String>, ex?)` | Set hash map key-value |
| `RedisClient.set(name, value: T, ex?)` | Set serialized object (default serializer) |
| `RedisClient.set(name, serializer, value: T, ex?)` | Set with custom serialization lambda |

### Extension Functions — Get

| Symbol | Description |
| --- | --- |
| `RedisClient.get(name): String?` | Get string value |
| `RedisClient.getList(name): List<String>?` | Get list value |
| `RedisClient.getSet(name): Set<String>?` | Get set value |
| `RedisClient.getMap(name): Map<String, String>?` | Get hash map value |
| `RedisClient.getObj(name): T?` | Get deserialized object (default deserializer) |
| `RedisClient.getObj(name, deserializer): T?` | Get with custom deserialization lambda |

### Context Helpers

| Symbol | Description |
| --- | --- |
| `RedisContext` | RAII wrapper around `RedisClient` |
| `useRedis(block)` | Execute block with default client |
| `useRedis(key, block)` | Execute block with client by key |
| `useRedis(name, block)` | Execute block with client by name |
| `useRedis(name, database, block)` | Execute block with client by name and database |

## Quick Start

```kotlin
// Initialize
val client = Redis.init {
    urls = listOf("localhost:26379")
    name = "my-app"
    masterName = "mymaster"
    database = 0
    password = "secret"
}!!

// String
client.set("user:1", "Alice")
val name = client.get("user:1")

// List
client.set("queue:pending", listOf("task-1", "task-2"))
val tasks = client.getList("queue:pending")

// Serialized object
client.set("order:1", orderDto)
val order = client.getObj<OrderDTO>("order:1")

// Context helper
useRedis("my-app") {
    client.set("key", "value")
}
```
