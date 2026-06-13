@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * Kafka 消息客户端
 * Kafka message client
 *
 * 提供 Kafka 生产者/消费者的封装，支持主题订阅和模式匹配订阅。
 * Provides Kafka producer/consumer wrapper with topic subscription and pattern-matching subscription.
 */
package fuookami.ospf.kotlin.framework.message

import java.util.*
import java.util.regex.Pattern
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromHexString
import kotlinx.serialization.encodeToHexString
import kotlinx.serialization.protobuf.ProtoBuf
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer

/**
 * Kafka 配置构建器
 * Kafka configuration builder
 *
 * @property urls Kafka 代理地址列表 / Kafka broker URL list
 * @property name 客户端名称 / Client name
 * @property userName 用户名（可选）/ Username (optional)
 * @property password 密码（可选）/ Password (optional)
 */
data class KafkaConfigBuilder(
    val urls: List<String>? = null,
    val name: String? = null,
    val userName: String? = null,
    val password: String? = null
) {
    /**
     * 构建 Kafka 配置
     * Build Kafka configuration
     *
     * @return 配置实例，参数不完整时返回 null / Configuration instance, or null if parameters are incomplete
     */
    operator fun invoke(): KafkaConfig? {
        return try {
            KafkaConfig(
                urls = urls!!,
                name = name!!,
                userName = userName,
                password = password!!
            )
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Kafka 配置数据
 * Kafka configuration data
 *
 * @property urls Kafka 代理地址列表 / Kafka broker URL list
 * @property name 客户端名称 / Client name
 * @property userName 用户名（可选）/ Username (optional)
 * @property password 密码（可选）/ Password (optional)
 */
@Serializable
data class KafkaConfig(
    val urls: List<String>,
    val name: String,
    val userName: String? = null,
    val password: String? = null
)

/**
 * Kafka 消息记录类型别名
 * Kafka message record type alias
 */
typealias KafkaMessageRecord = ConsumerRecord<String, String>

/**
 * Kafka 客户端
 * Kafka client
 *
 * 封装 Kafka 生产者和消费者，提供消息发送和订阅功能。
 * Wraps Kafka producer and consumers, providing message sending and subscription functionality.
 *
 * @property config Kafka 配置 / Kafka configuration
 */
data class KafkaClient(
    val config: KafkaConfig
) : AutoCloseable {
    val producer: KafkaProducer<String, String>
    lateinit var topicConsumer: KafkaConsumer<String, String>
    val topicProcessor: MutableMap<String, (String, KafkaMessageRecord) -> Unit> = HashMap()
    val patternConsumers: MutableList<KafkaConsumer<String, String>> = ArrayList()

    init {
        val props = Properties()
        props["bootstrap.servers"] = config.urls.joinToString(",")
        if (config.userName != null && config.password != null) {
            props["sasl.mechanism"] = "SCRAM-SHA-256"
            val jaasTemplate =
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${config.userName}\" password=\"${config.password}\";";
            val jaasCfg = String.format(jaasTemplate, "bj520", "e865c246f6a511e883ef9cb6d56662c");
            props["sasl.jaas.config"] = jaasCfg
        }
        producer = KafkaProducer(props, StringSerializer(), StringSerializer())
    }

    /**
     * 关闭客户端，释放生产者和消费者资源
     * Close client, releasing producer and consumer resources
     */
    override fun close() {
        producer.close()
        if (::topicConsumer.isInitialized) {
            topicConsumer.close()
        }
        for (consumer in patternConsumers) {
            consumer.close()
        }
    }

    /**
     * 发送字符串消息到指定主题
     * Send string message to specified topic
     *
     * @param topic 主题名称 / Topic name
     * @param message 消息内容 / Message content
     * @param key 消息键（可选）/ Message key (optional)
     */
    fun <T> send(
        topic: String,
        message: String,
        key: String? = null
    ) {
        producer.send(ProducerRecord(topic, key, message))
    }

    /**
     * 发送序列化对象消息到指定主题
     * Send serialized object message to specified topic
     *
     * @param T 消息类型 / Message type
     * @param topic 主题名称 / Topic name
     * @param value 消息对象 / Message object
     * @param serializable 序列化函数 / Serialization function
     * @param key 消息键（可选）/ Message key (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> send(
        topic: String,
        value: T,
        serializable: (T) -> String = { it: T -> ProtoBuf.encodeToHexString(it) },
        key: String? = null
    ) {
        producer.send(ProducerRecord(topic, key, serializable(value)))
    }

    /**
     * 订阅单个主题并处理字符串消息
     * Subscribe to a single topic and process string messages
     *
     * @param topic 主题名称 / Topic name
     * @param process 消息处理函数 / Message processor function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listen(
        topic: String,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listen(
            topics = listOf(topic),
            process = { msg, _ -> process(msg) },
            groupId = groupId
        )
    }

    /**
     * 订阅单个主题并处理字符串消息及记录元数据
     * Subscribe to a single topic and process string messages with record metadata
     *
     * @param topic 主题名称 / Topic name
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listen(
        topic: String,
        process: (String, KafkaMessageRecord) -> Unit,
        groupId: String? = null
    ) {
        listen(
            topics = listOf(topic),
            process = process,
            groupId = groupId
        )
    }

    /**
     * 订阅多个主题并处理字符串消息
     * Subscribe to multiple topics and process string messages
     *
     * @param topics 主题名称列表 / Topic name list
     * @param process 消息处理函数 / Message processor function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listen(
        topics: List<String>,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listen(
            topics = topics,
            process = { msg, _ -> process(msg) },
            groupId = groupId
        )
    }

    /**
     * 订阅多个主题并处理字符串消息及记录元数据（核心实现）
     * Subscribe to multiple topics and process string messages with record metadata (core implementation)
     *
     * @param topics 主题名称列表 / Topic name list
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listen(
        topics: List<String>,
        process: (String, KafkaMessageRecord) -> Unit,
        groupId: String? = null
    ) {
        if (::topicConsumer.isInitialized) {
            for (topic in topics) {
                this.topicProcessor[topic] = process
            }
            this.topicConsumer.subscribe(this.topicConsumer.listTopics().keys.toList() + topics)
        } else {
            val consumer = generateConsumer(groupId)
            for (topic in topics) {
                this.topicProcessor[topic] = process
            }
            consumer.subscribe(topics)
            this.topicConsumer = consumer

            frameworkPluginAsyncScope.launch(Dispatchers.IO) {
                while (true) {
                    val records = consumer.poll(100.milliseconds.toJavaDuration())
                    for (record in records) {
                        frameworkPluginAsyncScope.launch(Dispatchers.Default) {
                            topicProcessor[record.topic()]?.let { (record.value()) }
                        }
                    }
                }
            }
        }
    }

    /**
     * 订阅单个主题并反序列化处理消息
     * Subscribe to a single topic and process deserialized messages
     *
     * @param T 消息类型 / Message type
     * @param topic 主题名称 / Topic name
     * @param process 消息处理函数 / Message processor function
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topic: String,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(
            topics = listOf(topic),
            process = { msg, _ -> process(deserializer(msg)) },
            groupId = groupId
        )
    }

    /**
     * 订阅单个主题并反序列化处理消息及记录元数据
     * Subscribe to a single topic and process deserialized messages with record metadata
     *
     * @param T 消息类型 / Message type
     * @param topic 主题名称 / Topic name
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topic: String,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(
            topics = listOf(topic),
            process = { msg, record -> process(deserializer(msg), record) },
            groupId = groupId
        )
    }

    /**
     * 订阅多个主题并反序列化处理消息
     * Subscribe to multiple topics and process deserialized messages
     *
     * @param T 消息类型 / Message type
     * @param topics 主题名称列表 / Topic name list
     * @param process 消息处理函数 / Message processor function
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topics: List<String>,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(
            topics = topics,
            process = { msg, _ -> process(deserializer(msg)) },
            groupId = groupId
        )
    }

    /**
     * 订阅多个主题并反序列化处理消息及记录元数据
     * Subscribe to multiple topics and process deserialized messages with record metadata
     *
     * @param T 消息类型 / Message type
     * @param topics 主题名称列表 / Topic name list
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topics: List<String>,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(
            topics = topics,
            process = { msg, record -> process(deserializer(msg), record) },
            groupId = groupId
        )
    }

    /**
     * 按正则模式订阅主题并处理字符串消息
     * Subscribe to topics by regex pattern and process string messages
     *
     * @param pattern 主题名称正则模式 / Topic name regex pattern
     * @param process 消息处理函数 / Message processor function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listenPattern(
        pattern: String,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listenPattern(
            pattern = pattern,
            process = { msg, _ -> process(msg) },
            groupId = groupId
        )
    }

    /**
     * 按正则模式订阅主题并处理字符串消息及记录元数据
     * Subscribe to topics by regex pattern and process string messages with record metadata
     *
     * @param pattern 主题名称正则模式 / Topic name regex pattern
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    fun listenPattern(
        pattern: String,
        process: (String, KafkaMessageRecord) -> Unit,
        groupId: String? = null
    ) {
        val consumer = generateConsumer(groupId)
        consumer.subscribe(Pattern.compile(pattern))
        this.patternConsumers.add(consumer)

        frameworkPluginAsyncScope.launch(Dispatchers.IO) {
            while (true) {
                val records = consumer.poll(100.milliseconds.toJavaDuration())
                for (record in records) {
                    frameworkPluginAsyncScope.launch(Dispatchers.Default) {
                        process(record.value(), record)
                    }
                }
            }
        }
    }

    /**
     * 按正则模式订阅主题并反序列化处理消息
     * Subscribe to topics by regex pattern and process deserialized messages
     *
     * @param T 消息类型 / Message type
     * @param pattern 主题名称正则模式 / Topic name regex pattern
     * @param process 消息处理函数 / Message processor function
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listenPattern(
        pattern: String,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listenPattern(
            pattern = pattern,
            process = { msg, _ -> process(deserializer(msg)) },
            groupId = groupId
        )
    }

    /**
     * 按正则模式订阅主题并反序列化处理消息及记录元数据
     * Subscribe to topics by regex pattern and process deserialized messages with record metadata
     *
     * @param T 消息类型 / Message type
     * @param pattern 主题名称正则模式 / Topic name regex pattern
     * @param process 消息处理函数（含记录元数据）/ Message processor function (with record metadata)
     * @param deserializer 反序列化函数 / Deserializer function
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     */
    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listenPattern(
        pattern: String,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listenPattern(
            pattern = pattern,
            process = { msg, record -> process(deserializer(msg), record) },
            groupId = groupId
        )
    }

    /**
     * 创建 Kafka 消费者实例
     * Create a Kafka consumer instance
     *
     * @param groupId 消费者组 ID（可选）/ Consumer group ID (optional)
     * @return Kafka 消费者实例 / Kafka consumer instance
     */
    private fun generateConsumer(groupId: String?): KafkaConsumer<String, String> {
        val props = Properties()
        props["bootstrap.servers"] = config.urls.joinToString(",")
        if (config.userName != null && config.password != null) {
            props["sasl.mechanism"] = "SCRAM-SHA-256"
            val jaasTemplate =
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"${config.userName}\" password=\"${config.password}\";";
            val jaasCfg = String.format(jaasTemplate, "bj520", "e865c246f6a511e883ef9cb6d56662c");
            props["sasl.jaas.config"] = jaasCfg
        }
        if (groupId != null) {
            props["group.id"] = groupId
        }

        return KafkaConsumer(props, StringDeserializer(), StringDeserializer())
    }
}

/**
 * Kafka 客户端管理器
 * Kafka client manager
 *
 * 管理多个 Kafka 客户端实例，按名称索引。
 * Manages multiple Kafka client instances, indexed by name.
 */
object Kafka {
    @get:Synchronized
    private val clients: MutableMap<String, KafkaClient> = HashMap()

    /**
     * 初始化并获取 Kafka 客户端
     * Initialize and get Kafka client
     *
     * @param builder 配置构建器 lambda / Configuration builder lambda
     * @return Kafka 客户端实例，初始化失败时返回 null / Kafka client instance, or null if initialization fails
     */
    @Synchronized
    fun init(builder: KafkaConfigBuilder.() -> Unit): KafkaClient? {
        val config = KafkaConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    /**
     * 获取或创建 Kafka 客户端
     * Get or create Kafka client
     *
     * @param config Kafka 配置 / Kafka configuration
     * @return Kafka 客户端实例，创建失败时返回 null / Kafka client instance, or null if creation fails
     */
    @Synchronized
    operator fun invoke(config: KafkaConfig): KafkaClient? {
        if (clients.containsKey(config.name)) {
            return clients[config.name]
        }

        val client = KafkaClient(config)
        clients[config.name] = client
        return client
    }

    /**
     * 按名称获取已注册的 Kafka 客户端
     * Get registered Kafka client by name
     *
     * @param name 客户端名称（为 null 时返回第一个）/ Client name (returns first if null)
     * @return Kafka 客户端实例，未找到时返回 null / Kafka client instance, or null if not found
     */
    @Synchronized
    operator fun invoke(name: String? = null): KafkaClient? {
        return name?.let { clients[it] } ?: clients.values.toList().firstOrNull()
    }
}