# ospf-kotlin-framework-plugin-message-kafka

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 框架的 Kafka 消息生产者/消费者插件。

## 公开 API

| 符号 | 类型 | 说明 |
| --- | --- | --- |
| `KafkaConfigBuilder` | data class | `KafkaConfig` 的流式构建器 |
| `KafkaConfig` | data class | Kafka 连接配置（地址、名称、凭据） |
| `KafkaMessageRecord` | typealias | `ConsumerRecord<String, String>` 的别名 |
| `KafkaClient` | data class | Kafka 生产者/消费者封装，支持主题和模式匹配订阅 |
| `Kafka` | object | 客户端管理器；按名称索引 `KafkaClient` 实例 |

## 快速开始

```kotlin
// 初始化
val client = Kafka.init {
    urls = listOf("localhost:9092")
    name = "my-app"
}!!

// 发送
client.send(
    topic = "orders",
    value = orderDto,
    serializable = { dto -> json.encodeToString(dto) }
)

// 监听
client.listen(
    topic = "orders",
    process = { dto: OrderDTO -> /* 处理 */ },
    deserializer = { raw -> json.decodeFromString<OrderDTO>(raw) }
)

// 模式匹配订阅
client.listenPattern(
    pattern = "order-.*",
    process = { msg -> /* 处理 */ }
)
```

## KafkaClient 方法

| 方法 | 说明 |
| --- | --- |
| `send(topic, message, key?)` | 发送原始字符串消息 |
| `send(topic, value, serializable, key?)` | 发送序列化对象 |
| `listen(topic, process, groupId?)` | 订阅单个主题 |
| `listen(topics, process, groupId?)` | 订阅多个主题 |
| `listen(topic, process, deserializer, groupId?)` | 带反序列化订阅 |
| `listenPattern(pattern, process, groupId?)` | 按正则模式订阅 |

所有 `listen` 重载成对提供：一个接受 `(String) -> Unit`，另一个接受 `(String, KafkaMessageRecord) -> Unit` 以访问记录元数据。泛型变体接受 `deserializer` lambda 实现类型安全反序列化。
