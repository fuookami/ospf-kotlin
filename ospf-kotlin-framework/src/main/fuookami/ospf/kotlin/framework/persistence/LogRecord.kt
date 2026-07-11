@file:OptIn(kotlin.time.ExperimentalTime::class, kotlinx.serialization.InternalSerializationApi::class)

/**
 * 日志记录数据模型
 * Log Record Data Model
 *
 * 提供日志记录的纯数据类定义和 PO 反序列化扩展函数。
 * Provides pure data class definitions and PO deserialization extension functions for log records.
 *
 * ORM 特有的 Entity/Table/DAO/Saving 实现已迁移至 plugin-persistence-ktorm 模块。
 * ORM-specific Entity/Table/DAO/Saving implementations have been migrated to the plugin-persistence-ktorm module.
*/
package fuookami.ospf.kotlin.framework.persistence

import java.io.ByteArrayInputStream
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import fuookami.ospf.kotlin.utils.serialization.readFromJson
import fuookami.ospf.kotlin.framework.log.LogRecordPO
import fuookami.ospf.kotlin.framework.log.LogRecordType

/**
 * 字节数组日志记录持久化数据类
 * Byte array log record persistence data class
 *
 * @property app 应用名 / Application name
 * @property version 版本 / Version
 * @property serviceId 服务标识 / Service identifier
 * @property step 步骤 / Step
 * @property type 日志类型 / Log type
 * @property time 时间 / Time
 * @property availableTime 有效时间 / Available time
 * @property value 日志内容（字节） / Log content (bytes)
*/
data class LogRecordBytePO(
    val app: String,
    val version: String,
    val serviceId: String,
    val step: String,
    val type: LogRecordType,
    val time: LocalDateTime,
    val availableTime: LocalDateTime,
    val value: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogRecordBytePO) return false
        return app == other.app &&
               version == other.version &&
               serviceId == other.serviceId &&
               step == other.step &&
               type == other.type &&
               time == other.time &&
               availableTime == other.availableTime &&
               value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = app.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + serviceId.hashCode()
        result = 31 * result + step.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + time.hashCode()
        result = 31 * result + availableTime.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}

/**
 * 字符串日志记录持久化数据类
 * String log record persistence data class
 *
 * @property app 应用名 / Application name
 * @property version 版本 / Version
 * @property serviceId 服务标识 / Service identifier
 * @property step 步骤 / Step
 * @property type 日志类型 / Log type
 * @property time 时间 / Time
 * @property availableTime 有效时间 / Available time
 * @property value 日志内容（字符串） / Log content (string)
*/
data class LogRecordStringPO(
    val app: String,
    val version: String,
    val serviceId: String,
    val step: String,
    val type: LogRecordType,
    val time: LocalDateTime,
    val availableTime: LocalDateTime,
    val value: String
)

/**
 * 从 LogRecordBytePO 创建 LogRecordPO（使用默认反序列化器）
 * Create LogRecordPO from LogRecordBytePO (using default deserializer)
*/
inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(po: LogRecordBytePO): LogRecordPO<T>? {
    return this(po) {
        val stream = ByteArrayInputStream(it)
        readFromJson(T::class.serializer(), stream)
    }
}

/**
 * 从 LogRecordBytePO 创建 LogRecordPO（使用自定义反序列化函数）
 * Create LogRecordPO from LogRecordBytePO (using custom deserializer)
*/
inline operator fun <reified T : Any> LogRecordPO.Companion.invoke(
    po: LogRecordBytePO,
    deserializer: (ByteArray) -> T
): LogRecordPO<T>? {
    return try {
        LogRecordPO(
            app = po.app,
            version = po.version,
            serviceId = po.serviceId,
            step = po.step,
            type = po.type,
            time = po.time,
            availableTime = po.availableTime,
            value = deserializer(po.value)
        )
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
