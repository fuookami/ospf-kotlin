# ospf-kotlin-framework-plugin-message-kafka

:us: English | :cn: [简体中文](README_ch.md)

Kafka message producer/consumer plugin for the OSPF Kotlin framework.

## Public API

| Symbol | Kind | Description |
| --- | --- | --- |
| `KafkaConfigBuilder` | data class | Fluent builder for `KafkaConfig` |
| `KafkaConfig` | data class | Kafka connection configuration (urls, name, credentials) |
| `KafkaMessageRecord` | typealias | Alias for `ConsumerRecord<String, String>` |
| `KafkaClient` | data class | Kafka producer/consumer wrapper with topic and pattern subscription |
| `Kafka` | object | Client manager; indexes `KafkaClient` instances by name |

## Quick Start

```kotlin
// Initialize
val client = Kafka.init {
    urls = listOf("localhost:9092")
    name = "my-app"
}!!

// Send
client.send(
    topic = "orders",
    value = orderDto,
    serializable = { dto -> json.encodeToString(dto) }
)

// Listen
client.listen(
    topic = "orders",
    process = { dto: OrderDTO -> /* handle */ },
    deserializer = { raw -> json.decodeFromString<OrderDTO>(raw) }
)

// Pattern subscription
client.listenPattern(
    pattern = "order-.*",
    process = { msg -> /* handle */ }
)
```

## KafkaClient Methods

| Method | Description |
| --- | --- |
| `send(topic, message, key?)` | Send raw string message |
| `send(topic, value, serializable, key?)` | Send serialized object |
| `listen(topic, process, groupId?)` | Subscribe to single topic |
| `listen(topics, process, groupId?)` | Subscribe to multiple topics |
| `listen(topic, process, deserializer, groupId?)` | Subscribe with deserialization |
| `listenPattern(pattern, process, groupId?)` | Subscribe by regex pattern |

All `listen` overloads come in pairs: one taking `(String) -> Unit` and one taking `(String, KafkaMessageRecord) -> Unit` for record metadata access. Generic variants accept a `deserializer` lambda for type-safe deserialization.
