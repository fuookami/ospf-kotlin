package fuookami.ospf.kotlin.framework.log

import kotlin.time.*
import kotlin.time.Duration.Companion.days
import kotlinx.datetime.*
import kotlinx.serialization.*
import fuookami.ospf.kotlin.framework.persistence.*

interface LogRecordPO {
    val app: String
    val version: String
    val serviceId: String
    val step: String
    val time: String
    val availableTime: String
}

@Serializable
data class RequestLogRecordPO<T>(
    override val app: String,
    override val version: String,
    override val serviceId: String,
    override val step: String,
    override val time: String,
    override val availableTime: String,
    val request: RequestRecordPO<T>,
): LogRecordPO where T : RequestDTO<T> {
    constructor(
        app: String,
        version: String,
        serviceId: String,
        step: String,
        request: RequestRecordPO<T>,
        availableTime: Duration = 90.days
    ): this(
        app = app,
        version = version,
        serviceId = serviceId,
        step = step,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        availableTime = (Clock.System.now() + availableTime).toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        request = request
    )
}

@Serializable
data class ResponseLogRecordPO<T>(
    override val app: String,
    override val version: String,
    override val serviceId: String,
    override val step: String,
    override val time: String,
    override val availableTime: String,
    val response: ResponseRecordPO<T>
): LogRecordPO where T: ResponseDTO<T> {
    constructor(
        app: String,
        version: String,
        serviceId: String,
        step: String,
        response: ResponseRecordPO<T>,
        availableTime: Duration = 90.days
    ): this(
        app = app,
        version = version,
        serviceId = serviceId,
        step = step,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        availableTime = (Clock.System.now() + availableTime).toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        response = response
    )
}

@Serializable
data class NormalLogRecordPO<T>(
    override val app: String,
    override val version: String,
    override val serviceId: String,
    override val step: String,
    override val time: String,
    override val availableTime: String,
    val value: T
): LogRecordPO {
    constructor(
        app: String,
        version: String,
        serviceId: String,
        step: String,
        value: T,
        availableTime: Duration = 90.days
    ): this(
        app = app,
        version = version,
        serviceId = serviceId,
        step = step,
        time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        availableTime = (Clock.System.now() + availableTime).toLocalDateTime(TimeZone.currentSystemDefault()).toString(),
        value = value
    )
}
