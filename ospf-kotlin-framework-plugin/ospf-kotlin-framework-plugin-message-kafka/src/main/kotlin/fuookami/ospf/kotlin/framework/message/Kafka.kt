package fuookami.ospf.kotlin.framework.message

import java.util.*
import java.util.regex.*
import kotlin.time.*
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import org.apache.kafka.common.serialization.*
import org.apache.kafka.clients.consumer.*
import org.apache.kafka.clients.producer.*

data class KafkaConfigBuilder(
    val urls: List<String>? = null,
    val name: String? = null,
    val userName: String? = null,
    val password: String? = null
) {
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

@Serializable
data class KafkaConfig(
    val urls: List<String>,
    val name: String,
    val userName: String? = null,
    val password: String? = null
)

typealias KafkaMessageRecord = ConsumerRecord<String, String>

data class KafkaClient(
    val config: KafkaConfig
) {
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

    protected fun finalize() {
        producer.close()
        if (::topicConsumer.isInitialized) {
            topicConsumer.close()
        }
        for (consumer in patternConsumers) {
            consumer.close()
        }
    }

    fun <T> send(
        topic: String,
        message: String,
        key: String? = null
    ) {
        producer.send(ProducerRecord(topic, key, message))
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> send(
        topic: String,
        value: T,
        serializable: (T) -> String = { it: T -> ProtoBuf.encodeToHexString(it) },
        key: String? = null
    ) {
        producer.send(ProducerRecord(topic, key, serializable(value)))
    }

    fun listen(
        topic: String,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listen(listOf(topic), { msg, _ -> process(msg) }, groupId)
    }

    fun listen(
        topic: String,
        process: (String, KafkaMessageRecord) -> Unit,
        groupId: String? = null
    ) {
        listen(listOf(topic), process, groupId)
    }

    fun listen(
        topics: List<String>,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listen(topics, { msg, _ -> process(msg) }, groupId)
    }

    @OptIn(DelicateCoroutinesApi::class)
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

            GlobalScope.launch(Dispatchers.IO) {
                while (true) {
                    val records = consumer.poll(100.milliseconds.toJavaDuration())
                    for (record in records) {
                        GlobalScope.launch(Dispatchers.Default) {
                            topicProcessor[record.topic()]?.let { (record.value()) }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topic: String,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(topic, { msg, _ -> process(deserializer(msg)) }, groupId)
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topic: String,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(topic, { msg, record -> process(deserializer(msg), record) }, groupId)
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topics: List<String>,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(topics, { msg, _ -> process(deserializer(msg)) }, groupId)
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listen(
        topics: List<String>,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listen(topics, { msg, record -> process(deserializer(msg), record) }, groupId)
    }

    fun listenPattern(
        pattern: String,
        process: (String) -> Unit,
        groupId: String? = null
    ) {
        listenPattern(pattern, { msg, _ -> process(msg) }, groupId)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun listenPattern(
        pattern: String,
        process: (String, KafkaMessageRecord) -> Unit,
        groupId: String? = null
    ) {
        val consumer = generateConsumer(groupId)
        consumer.subscribe(Pattern.compile(pattern))
        this.patternConsumers.add(consumer)

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                val records = consumer.poll(100.milliseconds.toJavaDuration())
                for (record in records) {
                    GlobalScope.launch(Dispatchers.Default) {
                        process(record.value(), record)
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listenPattern(
        pattern: String,
        crossinline process: (T) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listenPattern(pattern, { msg, _ -> process(deserializer(msg)) }, groupId)
    }

    @OptIn(ExperimentalSerializationApi::class)
    inline fun <reified T> listenPattern(
        pattern: String,
        crossinline process: (T, KafkaMessageRecord) -> Unit,
        crossinline deserializer: (String) -> T = { it: String -> ProtoBuf.decodeFromHexString(it) },
        groupId: String? = null
    ) {
        listenPattern(pattern, { msg, record -> process(deserializer(msg), record) }, groupId)
    }

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

object Kafka {
    @get:Synchronized
    private val clients: MutableMap<String, KafkaClient> = HashMap()

    @Synchronized
    fun init(builder: KafkaConfigBuilder.() -> Unit): KafkaClient? {
        val config = KafkaConfigBuilder()
        builder(config)
        return config()?.let { this(it) }
    }

    @Synchronized
    operator fun invoke(config: KafkaConfig): KafkaClient? {
        if (clients.containsKey(config.name)) {
            return clients[config.name]
        }

        val client = KafkaClient(config)
        clients[config.name] = client
        return client
    }

    @Synchronized
    operator fun invoke(name: String? = null): KafkaClient? {
        return name?.let { clients[it] } ?: clients.values.toList().firstOrNull()
    }
}
