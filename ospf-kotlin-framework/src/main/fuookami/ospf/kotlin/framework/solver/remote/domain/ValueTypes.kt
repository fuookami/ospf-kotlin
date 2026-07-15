@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 远程求解值类型
 * Remote solve value types
 *
 * 定义远程求解领域中的强类型 ID、路径和序列化器。
 * Defines strongly-typed IDs, paths, and serializers for the remote solve domain.
*/
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

/**
 * 毫秒 Duration 序列化器。
 * Millisecond Duration serializer.
*/
data object RemoteSolverMillisecondsDurationSerializer : KSerializer<Duration> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteSolverMillisecondsDuration", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Duration {
        return decoder.decodeLong().toDuration(DurationUnit.MILLISECONDS)
    }

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMilliseconds)
    }
}

/**
 * epoch millis Instant 序列化器。
 * Epoch millis Instant serializer.
*/
data object RemoteSolverEpochMillisecondsInstantSerializer : KSerializer<Instant> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("RemoteSolverEpochMillisecondsInstant", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.fromEpochMilliseconds(decoder.decodeLong())
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeLong(value.toEpochMilliseconds())
    }
}

/**
 * 任务 ID。
 * Task ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class TaskId(val value: String) {
    init {
        require(value.isNotBlank()) { "TaskId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [TaskId] instance / 规范化的 [TaskId] 实例
        */
        fun of(value: String): TaskId {
            return TaskId(value.trim())
        }
    }
}

/**
 * 切片 ID。
 * Slice ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class SliceId(val value: String) {
    init {
        require(value.isNotBlank()) { "SliceId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [SliceId] instance / 规范化的 [SliceId] 实例
        */
        fun of(value: String): SliceId {
            return SliceId(value.trim())
        }
    }
}

/**
 * 节点 ID。
 * Node ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class NodeId(val value: String) {
    init {
        require(value.isNotBlank()) { "NodeId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [NodeId] instance / 规范化的 [NodeId] 实例
        */
        fun of(value: String): NodeId {
            return NodeId(value.trim())
        }
    }
}

/**
 * 租户 ID。
 * Tenant ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class TenantId(val value: String) {
    init {
        require(value.isNotBlank()) { "TenantId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [TenantId] instance / 规范化的 [TenantId] 实例
        */
        fun of(value: String): TenantId {
            return TenantId(value.trim())
        }
    }
}

/**
 * 请求 ID。
 * Request ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class RequestId(val value: String) {
    init {
        require(value.isNotBlank()) { "RequestId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [RequestId] instance / 规范化的 [RequestId] 实例
        */
        fun of(value: String): RequestId {
            return RequestId(value.trim())
        }
    }
}

/**
 * 执行句柄 ID。
 * Execution handle ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class HandleId(val value: String) {
    init {
        require(value.isNotBlank()) { "HandleId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [HandleId] instance / 规范化的 [HandleId] 实例
        */
        fun of(value: String): HandleId {
            return HandleId(value.trim())
        }
    }
}

/**
 * Trace ID。
 * Trace ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class TraceId(val value: String) {
    init {
        require(value.isNotBlank()) { "TraceId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [TraceId] instance / 规范化的 [TraceId] 实例
        */
        fun of(value: String): TraceId {
            return TraceId(value.trim())
        }
    }
}

/**
 * 对象路径。
 * Object path.
 *
 * @property value 原始路径 / Raw path
*/
@Serializable
@JvmInline
value class ObjectPath(val value: String) {
    init {
        require(value.isNotBlank()) { "ObjectPath must not be blank." }
        require(!value.contains('\u0000')) { "ObjectPath must not contain NUL." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace, converting backslashes to forward slashes, and removing leading slashes.
         * 通过修剪空白、转换反斜杠为正斜杠并移除前导斜杠创建规范化实例。
         *
         * @param value the raw path string / 原始路径字符串
         * @return the normalized [ObjectPath] instance / 规范化的 [ObjectPath] 实例
        */
        fun of(value: String): ObjectPath {
            val normalized = value.trim().replace('\\', '/').trimStart('/')
            return ObjectPath(normalized)
        }
    }
}

/**
 * 对象版本。
 * Object version.
 *
 * @property value 原始版本 / Raw version
*/
@Serializable
@JvmInline
value class ObjectVersion(val value: String) {
    init {
        require(value.isNotBlank()) { "ObjectVersion must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [ObjectVersion] instance / 规范化的 [ObjectVersion] 实例
        */
        fun of(value: String): ObjectVersion {
            return ObjectVersion(value.trim())
        }
    }
}

/**
 * 对象 ETag。
 * Object ETag.
 *
 * @property value 原始 ETag / Raw ETag
*/
@Serializable
@JvmInline
value class ObjectEtag(val value: String) {
    init {
        require(value.isNotBlank()) { "ObjectEtag must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [ObjectEtag] instance / 规范化的 [ObjectEtag] 实例
        */
        fun of(value: String): ObjectEtag {
            return ObjectEtag(value.trim())
        }
    }
}

/**
 * 求解器类型名。
 * Solver type name.
 *
 * @property value 类型名 / Type name
*/
@Serializable
@JvmInline
value class SolverTypeName(val value: String) {
    init {
        require(value.isNotBlank()) { "SolverTypeName must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [SolverTypeName] instance / 规范化的 [SolverTypeName] 实例
        */
        fun of(value: String): SolverTypeName {
            return SolverTypeName(value.trim())
        }
    }
}

/**
 * 目标类型名。
 * Target type name.
 *
 * @property value 类型名 / Type name
*/
@Serializable
@JvmInline
value class TargetTypeName(val value: String) {
    init {
        require(value.isNotBlank()) { "TargetTypeName must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [TargetTypeName] instance / 规范化的 [TargetTypeName] 实例
        */
        fun of(value: String): TargetTypeName {
            return TargetTypeName(value.trim())
        }
    }
}

/**
 * 预算范围 ID。
 * Budget scope ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class BudgetScopeId(val value: String) {
    init {
        require(value.isNotBlank()) { "BudgetScopeId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [BudgetScopeId] instance / 规范化的 [BudgetScopeId] 实例
        */
        fun of(value: String): BudgetScopeId {
            return BudgetScopeId(value.trim())
        }
    }
}

/**
 * 操作者 ID。
 * Operator ID.
 *
 * @property value 原始 ID 值 / Raw ID value
*/
@Serializable
@JvmInline
value class OperatorId(val value: String) {
    init {
        require(value.isNotBlank()) { "OperatorId must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [OperatorId] instance / 规范化的 [OperatorId] 实例
        */
        fun of(value: String): OperatorId {
            return OperatorId(value.trim())
        }
    }
}

/**
 * 操作来源。
 * Operation source.
 *
 * @property value 来源值 / Source value
*/
@Serializable
@JvmInline
value class OperationSource(val value: String) {
    init {
        require(value.isNotBlank()) { "OperationSource must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [OperationSource] instance / 规范化的 [OperationSource] 实例
        */
        fun of(value: String): OperationSource {
            return OperationSource(value.trim())
        }
    }
}

/**
 * 原因代码。
 * Reason code.
 *
 * @property value 原因值 / Reason value
*/
@Serializable
@JvmInline
value class ReasonCode(val value: String) {
    init {
        require(value.isNotBlank()) { "ReasonCode must not be blank." }
    }

    override fun toString(): String {
        return value
    }

    companion object {
        /**
         * Creates a normalized instance by trimming whitespace.
         * 通过修剪空白创建规范化实例。
         *
         * @param value the raw string value / 原始字符串值
         * @return the normalized [ReasonCode] instance / 规范化的 [ReasonCode] 实例
        */
        fun of(value: String): ReasonCode {
            return ReasonCode(value.trim())
        }
    }
}
