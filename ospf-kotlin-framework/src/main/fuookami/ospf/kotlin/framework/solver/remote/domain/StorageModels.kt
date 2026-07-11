@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.solver.remote.domain

import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 对象引用。
 * Object reference.
 *
 * @property path 存储路径 / Storage path
 * @property version 版本标识 / Version identifier
 * @property etag 实体标签 / Entity tag
*/
@Serializable
data class ObjectRef(
    val path: ObjectPath,
    val version: ObjectVersion? = null,
    val etag: ObjectEtag? = null
) {
    companion object {
        /**
         * 从字符串创建对象引用。
         * Create object reference from strings.
         *
         * @param path 对象路径 / Object path
         * @param version 对象版本 / Object version
         * @param etag 对象 ETag / Object ETag
         * @return 对象引用 / Object reference
        */
        fun of(
            path: String,
            version: String? = null,
            etag: String? = null
        ): ObjectRef {
            return ObjectRef(
                path = ObjectPath.of(path),
                version = version?.let { ObjectVersion.of(it) },
                etag = etag?.let { ObjectEtag.of(it) }
            )
        }
    }
}

/**
 * 存储对象。
 * Stored object.
 *
 * @property ref 对象引用 / Object reference
 * @property bytes 对象字节 / Object bytes
 * @property metadata 元数据 / Metadata
 * @property createdAt 创建时间戳 / Created timestamp
*/
data class StoredObject(
    val ref: ObjectRef,
    val bytes: ByteArray,
    val metadata: Map<String, String>,
    val createdAt: Instant
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is StoredObject) {
            return false
        }
        return ref == other.ref &&
            bytes.contentEquals(other.bytes) &&
            metadata == other.metadata &&
            createdAt == other.createdAt
    }

    override fun hashCode(): Int {
        var result = ref.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + createdAt.hashCode()
        return result
    }
}

/**
 * 检查点元数据。
 * Checkpoint metadata.
 *
 * @property taskId 任务 ID / Task ID
 * @property sliceId 切片 ID / Slice ID
 * @property ref 检查点引用 / Checkpoint reference
 * @property createdAt 创建时间戳 / Created timestamp
*/
@Serializable
data class CheckpointMetadata(
    val taskId: TaskId,
    val sliceId: SliceId,
    val ref: ObjectRef,
    @SerialName("createdAtEpochMs")
    @Serializable(with = RemoteSolverEpochMillisecondsInstantSerializer::class)
    val createdAt: Instant
)
