package fuookami.ospf.kotlin.framework.log

import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.context.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.serialization.*

interface Pushing {
    operator fun <T: Any> invoke(serializer: KSerializer<T>, value: LogRecordPO<T>): Try {
        val json = Json {
            ignoreUnknownKeys = true
        }
        return this({ json.encodeToString(LogRecordPO.serializer(serializer), it) }, value)
    }

    operator fun <T: Any> invoke(serializer: (LogRecordPO<T>) -> String, value: LogRecordPO<T>): Try
}

@OptIn(InternalSerializationApi::class)
inline operator fun <reified T: Any> Pushing.invoke(value: LogRecordPO<T>): Try {
    return this(T::class.serializer(), value)
}

interface Saving {
    operator fun <T: Any> invoke(serializer: KSerializer<T>, value: LogRecordPO<T>): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsString")
    operator fun <T: Any> invoke(serializer: (T) -> String, value: LogRecordPO<T>): Try {
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("saveAsBytes")
    operator fun <T: Any> invoke(serializer: (T) -> ByteArray, value: LogRecordPO<T>): Try {
        return ok
    }
}

@OptIn(InternalSerializationApi::class)
inline operator fun <reified T: Any> Saving.invoke(value: LogRecordPO<T>): Try {
    return this(T::class.serializer(), value)
}

data class LogContextBuilder(
    var app: String = "",
    var version: String = "",
    var requestId: String = "",
    var pushing: Pushing? = null,
    val saving: Saving? = null
) {
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

class LogContext private constructor(
    val app: String,
    val version: String,
    val serviceId: String,
    val pushing: Pushing? = null,
    val saving: Saving? = null
) : Cloneable {
    private val logger = logger()

    companion object {
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

        fun build(builder: LogContextBuilder.() -> Unit): LogContext {
            val context = LogContextBuilder()
            builder(context)
            return context()
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> push(
        step: String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return push(step, { writeJson(LogRecordPO.serializer(T::class.serializer()), it) }, value, type, availableTime)
    }

    fun <T : Any> push(
        step: String,
        serializer: KSerializer<T>,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return push(step, { writeJson(LogRecordPO.serializer(serializer), it) }, value, type, availableTime)
    }

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
            }
        }
    }

    @OptIn(InternalSerializationApi::class)
    inline fun <reified T : Any> save(
        step: String,
        value: T,
        type: LogRecordType = LogRecordType.Info,
        availableTime: Duration = 90.days
    ) {
        return save(step, T::class.serializer(), value, type, availableTime)
    }

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
            }
        }
    }

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
            }
        }
    }

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
            }
        }
    }
}

val logContext = ContextVar(LogContext(app = "", version = "", requestId = ""))
