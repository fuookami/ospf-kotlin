@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 日志上下文
 * Log Context
 *
 * 提供日志推送和保存的上下文管理。
 * Provides context management for log pushing and saving.
 */
package fuookami.ospf.kotlin.framework.log

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.utils.context.ContextVar
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.serialization.writeJson

/**
 * 日志推送接口
 * Log pushing interface
 */
interface Pushing {
    /**
     * 推送日志记录（使用序列化器）
     * Push log record (using serializer)
     *
     * @param value 日志记录 / Log record
     * @param serializer 序列化器 / Serializer
     * @param T 日志值类型 / Log value type
     * @return 操作结果 / Operation result
     */
    operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: KSerializer<T>): Try {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return this(value) {
            json.encodeToString(LogRecordPO.serializer(serializer), it)
        }
    }

    /**
     * 推送日志记录（使用自定义序列化函数）
     * Push log record (using custom serialization function)
     *
     * @param value 日志记录 / Log record
     * @param serializer 自定义序列化函数 / Custom serialization function
     * @param T 日志值类型 / Log value type
     * @return 操作结果 / Operation result
     */
    operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: (LogRecordPO<T>) -> String): Try
}

/**
 * 推送日志记录（自动序列化）
 * Push log record (auto serialization)
 *
 * @param value 日志记录 / Log record
 * @param T 日志值类型 / Log value type
 * @return 操作结果 / Operation result
 */
@OptIn(InternalSerializationApi::class)
inline operator fun <reified T : Any> Pushing.invoke(value: LogRecordPO<T>): Try {
    return this(value, T::class.serializer())
}

/**
 * 日志保存接口
 * Log saving interface
 */
interface Saving {
    /**
     * 保存日志记录（使用序列化器）
     * Save log record (using serializer)
     *
     * @param value 日志记录 / Log record
     * @param serializer 序列化器 / Serializer
     * @param T 日志值类型 / Log value type
     * @return 操作结果 / Operation result
     */
    operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: KSerializer<T>): Try

    /**
     * 保存日志记录（使用字符串序列化函数）
     * Save log record (using string serialization function)
     *
     * @param value 日志记录 / Log record
     * @param serializer 字符串序列化函数 / String serialization function
     * @param T 日志值类型 / Log value type
     * @return 操作结果 / Operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsString")
    operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: (T) -> String): Try {
        return ok
    }

    /**
     * 保存日志记录（使用字节数组序列化函数）
     * Save log record (using byte array serialization function)
     *
     * @param value 日志记录 / Log record
     * @param serializer 字节数组序列化函数 / Byte array serialization function
     * @param T 日志值类型 / Log value type
     * @return 操作结果 / Operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsBytes")
    operator fun <T : Any> invoke(value: LogRecordPO<T>, serializer: (T) -> ByteArray): Try {
        return ok
    }
}

/**
 * 保存日志记录（自动序列化）
 * Save log record (auto serialization)
 *
 * @param value 日志记录 / Log record
 * @param T 日志值类型 / Log value type
 * @return 操作结果 / Operation result
 */
@OptIn(InternalSerializationApi::class)
inline operator fun <reified T : Any> Saving.invoke(value: LogRecordPO<T>): Try {
    return this(value, T::class.serializer())
}

/**
 * 日志上下文构建器
 * Log context builder
 *
 * @property app 应用名 / Application name
 * @property version 版本 / Version
 * @property requestId 请求标识 / Request identifier
 * @property pushing 推送器，可为 null / Pusher, nullable
 * @property saving 保存器，可为 null / Saver, nullable
 */
data class LogContextBuilder(
    var app: String = "",
    var version: String = "",
    var requestId: String = "",
    var pushing: Pushing? = null,
    val saving: Saving? = null
) {
    /**
     * 构建日志上下文
     * Build log context
     *
     * @return 日志上下文 / Log context
     */
    operator fun invoke(): LogContext {
        return LogContext(
            app = app,
            version = version,
            requestId = requestId,
            pushing = pushing,
            saving = saving
        )
    }
}

/**
 * 日志上下文
 * Log context
 *
 * @property app 应用名 / Application name
 * @property version 版本 / Version
 * @property serviceId 服务标识 / Service identifier
 * @property pushing 推送器，可为 null / Pusher, nullable
 * @property saving 保存器，可为 null / Saver, nullable
 */
class LogContext private constructor(
    val app: String,
    val version: String,
    val serviceId: String,
    val pushing: Pushing? = null,
    val saving: Saving? = null
) : Cloneable, AutoCloseable {
    private val logger = logger()

    companion object {
        /**
         * 创建日志上下文
         * Create log context
         *
         * @param app 应用名 / Application name
         * @param version 版本 / Version
         * @param requestId 请求标识 / Request identifier
         * @param pushing 推送器，可为 null / Pusher, nullable
         * @param saving 保存器，可为 null / Saver, nullable
         * @return 日志上下文 / Log context
         */
        operator fun invoke(
            app: String = "",
            version: String,
            requestId: String,
            pushing: Pushing? = null,
            saving: Saving? = null
        ): LogContext {
            val uuid = UUID.nameUUIDFromBytes("${app}${version}${requestId}".toByteArray(charset("UTF-8")))
            return LogContext(
                app = app,
                version = version,
                serviceId = uuid.toString(),
                pushing = pushing,
                saving = saving
            )
        }

        /**
         * 通过构建器 DSL 创建日志上下文
         * Create log context via builder DSL
         *
         * @param builder 构建器配置块 / Builder configuration block
         * @return 日志上下文 / Log context
         */
        fun build(builder: LogContextBuilder.() -> Unit): LogContext {
            val context = LogContextBuilder()
            builder(context)
            return context()
        }
    }

    /** 关闭上下文，释放保存器资源 / Close context, release saver resources */
    override fun close() {
        if (saving is AutoCloseable) {
            saving.close()
        }
    }

    /**
     * 推送日志（自动序列化）
     * Push log (auto serialization)
     *
     * @param step 步骤 / Step
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> push(
        step: String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return push(
            step = step,
            serializer = {
                writeJson(
                    serializer = LogRecordPO.serializer(T::class.serializer()),
                    value = it
                )
            },
            value = value,
            type = type,
            availableTime = availableTime
        )
    }

    /**
     * 推送日志（使用序列化器）
     * Push log (using serializer)
     *
     * @param step 步骤 / Step
     * @param serializer 序列化器 / Serializer
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    fun <T : Any> push(
        step: String,
        serializer: KSerializer<T>,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return push(
            step = step,
            serializer = {
                writeJson(
                    serializer = LogRecordPO.serializer(serializer),
                    value = it
                )
            },
            value = value,
            type = type,
            availableTime = availableTime
        )
    }

    /**
     * 推送日志（使用自定义序列化函数）
     * Push log (using custom serialization function)
     *
     * @param step 步骤 / Step
     * @param serializer 自定义序列化函数 / Custom serialization function
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    fun <T : Any> push(
        step: String,
        serializer: (LogRecordPO<T>) -> String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        if (pushing != null) {
            val pushing = this.pushing

            val record = LogRecordPO(
                app = app,
                version = version,
                serviceId = serviceId,
                step = step,
                value = value,
                type = type,
                availableTime = availableTime
            )
            when (val result = pushing(
                serializer = serializer,
                value = record
            )) {
                is Ok -> {
                    logger.info { "pushing log success" }
                }

                is Failed -> {
                    logger.info { "pushing log failed: ${result.error.message}" }
                }

                is Fatal -> {
                    logger.error { "pushing log fatal: ${result.errors.joinToString { it.message }}" }
                }
            }
        }
    }

    /**
     * 保存日志（自动序列化）
     * Save log (auto serialization)
     *
     * @param step 步骤 / Step
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> save(
        step: String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return save(
            step = step,
            serializer = T::class.serializer(),
            value = value,
            type = type,
            availableTime = availableTime
        )
    }

    /**
     * 保存日志（使用序列化器）
     * Save log (using serializer)
     *
     * @param step 步骤 / Step
     * @param serializer 序列化器 / Serializer
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    fun <T : Any> save(
        step: String,
        serializer: KSerializer<T>,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        if (saving != null) {
            val saving = this.saving
            val record = LogRecordPO(
                app = app,
                version = version,
                serviceId = serviceId,
                step = step,
                value = value,
                type = type,
                availableTime = availableTime
            )

            when (val result = saving(
                serializer = serializer,
                value = record
            )) {
                is Ok -> {
                    logger.info { "saving log success" }
                }

                is Failed -> {
                    logger.info { "saving log failed: ${result.error.message}" }
                }

                is Fatal -> {
                    logger.error { "saving log fatal: ${result.errors.joinToString { it.message }}" }
                }
            }
        }
    }

    /**
     * 保存日志（使用字符串序列化函数）
     * Save log (using string serialization function)
     *
     * @param step 步骤 / Step
     * @param serializer 字符串序列化函数 / String serialization function
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    @JvmName("saveAsString")
    fun <T : Any> save(
        step: String,
        serializer: (T) -> String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        if (saving != null) {
            val saving = this.saving
            val record = LogRecordPO(
                app = app,
                version = version,
                serviceId = serviceId,
                step = step,
                value = value,
                type = type,
                availableTime = availableTime
            )

            when (val result = saving(
                serializer = serializer,
                value = record
            )) {
                is Ok -> {
                    logger.info { "saving log success" }
                }

                is Failed -> {
                    logger.info { "saving log failed: ${result.error.message}" }
                }

                is Fatal -> {
                    logger.error { "saving log fatal: ${result.errors.joinToString { it.message }}" }
                }
            }
        }
    }

    /**
     * 保存日志（使用字节数组序列化函数）
     * Save log (using byte array serialization function)
     *
     * @param step 步骤 / Step
     * @param serializer 字节数组序列化函数 / Byte array serialization function
     * @param value 日志值 / Log value
     * @param type 日志类型，默认 Info / Log type, default Info
     * @param availableTime 可用时长，默认 90 天 / Available duration, default 90 days
     * @param T 日志值类型 / Log value type
     */
    @JvmName("saveAsBytes")
    fun <T : Any> save(
        step: String,
        serializer: (T) -> ByteArray,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        if (saving != null) {
            val saving = this.saving
            val record = LogRecordPO(
                app = app,
                version = version,
                serviceId = serviceId,
                step = step,
                value = value,
                type = type,
                availableTime = availableTime
            )

            when (val result = saving(
                serializer = serializer,
                value = record
            )) {
                is Ok -> {
                    logger.info { "saving log success" }
                }

                is Failed -> {
                    logger.info { "saving log failed: ${result.error.message}" }
                }

                is Fatal -> {
                    logger.error { "saving log fatal: ${result.errors.joinToString { it.message }}" }
                }
            }
        }
    }
}

/** 全局日志上下文变量 / Global log context variable */
val logContext = ContextVar(LogContext(app = "", version = "", requestId = ""))