package fuookami.ospf.kotlin.framework.log

import java.util.*
import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.*
import kotlinx.serialization.*
import org.http4k.core.*
import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.context.*

interface Pushing {
    operator fun <T> invoke(serializer: KSerializer<T>, value: T): Response
}

data class LogContextBuilder(
    var app: String = "",
    var version: String = "",
    var requestId: String = "",
    var pushing: Pushing? = null
) {
    operator fun invoke(): LogContext {
        return LogContext(
            app = app,
            version = version,
            requestId = requestId,
            pushing = pushing
        )
    }
}

class LogContext private constructor(
    val app: String,
    val version: String,
    val serviceId: String,
    val pushing: Pushing? = null
) : Cloneable {
    private val logger = logger()

    companion object {
        operator fun invoke(
            app: String = "",
            version: String,
            requestId: String,
            pushing: Pushing? = null
        ): LogContext {
            val uuid = UUID.nameUUIDFromBytes("${app}${version}${requestId}".toByteArray(charset("UTF-8")))
            return LogContext(
                app = app,
                version = version,
                serviceId = uuid.toString(),
                pushing = pushing
            )
        }

        fun Build(builder: LogContextBuilder.() -> Unit): LogContext {
            val context = LogContextBuilder()
            builder(context)
            return context()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun <T> push(
        step: String,
        serializer: KSerializer<T>,
        value: T,
        availableTime: Duration = 90.days
    ) {
        if (pushing != null) {
            val pushing = this.pushing

            GlobalScope.launch(Dispatchers.IO) {
                val record = LogRecordPO(
                    app = app,
                    version = version,
                    serviceId = serviceId,
                    step = step,
                    value = value,
                    availableTime = availableTime
                )
                val ret = pushing(
                    serializer = LogRecordPO.serializer(serializer),
                    value = record
                )
                if (ret.status.successful) {
                    logger.info { "pushing log success" }
                } else {
                    logger.info { "pushing log failed" }
                }
            }
        }
    }
}

val logContext = ContextVar(LogContext(app = "", version = "", requestId = ""))
