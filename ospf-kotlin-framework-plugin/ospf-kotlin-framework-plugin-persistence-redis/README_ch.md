# ospf-kotlin-framework-plugin-persistence-redis

:us: [English](README.md) | :cn: 简体中文

Redis 哨兵客户端管理插件，支持结构化数据和序列化扩展。

## 公开 API

### 客户端管理

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `RedisClientKey` | data class | 客户端查找键（名称 + 数据库编号） |
| `RedisConfigBuilder` | data class | `RedisConfig` 的流式构建器 |
| `RedisConfig` | data class | Redis 哨兵连接配置 |
| `RedisClient` | data class | Jedis 连接封装，实现 `AutoCloseable` |
| `Redis` | object | 客户端管理器；按键索引连接池实例 |

### 扩展函数 — 写入

| 符号 | 说明 |
| --- | --- |
| `RedisClient.set(name, value: String, ex?)` | 设置字符串键值 |
| `RedisClient.set(name, value: List<String>, ex?)` | 设置列表键值 |
| `RedisClient.set(name, value: Set<String>, ex?)` | 设置集合键值 |
| `RedisClient.set(name, value: Map<String, String>, ex?)` | 设置哈希表键值 |
| `RedisClient.set(name, value: T, ex?)` | 设置序列化对象（默认序列化器） |
| `RedisClient.set(name, serializer, value: T, ex?)` | 使用自定义序列化 lambda 设置 |

### 扩展函数 — 读取

| 符号 | 说明 |
| --- | --- |
| `RedisClient.get(name): String?` | 获取字符串值 |
| `RedisClient.getList(name): List<String>?` | 获取列表值 |
| `RedisClient.getSet(name): Set<String>?` | 获取集合值 |
| `RedisClient.getMap(name): Map<String, String>?` | 获取哈希表值 |
| `RedisClient.getObj(name): T?` | 获取反序列化对象（默认反序列化器） |
| `RedisClient.getObj(name, deserializer): T?` | 使用自定义反序列化 lambda 获取 |

### 上下文辅助

| 符号 | 说明 |
| --- | --- |
| `RedisContext` | `RedisClient` 的 RAII 封装 |
| `useRedis(block)` | 使用默认客户端执行块 |
| `useRedis(key, block)` | 按键使用客户端执行块 |
| `useRedis(name, block)` | 按名称使用客户端执行块 |
| `useRedis(name, database, block)` | 按名称和数据库编号使用客户端执行块 |

## 快速开始

```kotlin
// 初始化
val client = Redis.init {
    urls = listOf("localhost:26379")
    name = "my-app"
    masterName = "mymaster"
    database = 0
    password = "secret"
}!!

// 字符串
client.set("user:1", "Alice")
val name = client.get("user:1")

// 列表
client.set("queue:pending", listOf("task-1", "task-2"))
val tasks = client.getList("queue:pending")

// 序列化对象
client.set("order:1", orderDto)
val order = client.getObj<OrderDTO>("order:1")

// 上下文辅助
useRedis("my-app") {
    client.set("key", "value")
}
```
