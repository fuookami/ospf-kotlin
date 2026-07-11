/**
 * 远程求解对象存储端口
 * Remote solver object storage port
*/
package fuookami.ospf.kotlin.framework.solver.remote.port

import fuookami.ospf.kotlin.framework.solver.remote.domain.*

/**
 * 对象存储端口。
 * Object storage port.
*/
interface ObjectStoragePort {

    /**
     * 写入对象。
     * Put object.
     *
     * @param path 存储路径 / Storage path
     * @param bytes 对象字节 / Object bytes
     * @param metadata 元数据 / Metadata
     * @return 对象引用 / Object reference
    */
    suspend fun put(
        path: ObjectPath,
        bytes: ByteArray,
        metadata: Map<String, String> = emptyMap()
    ): ObjectRef

    /**
     * 读取对象。
     * Get object.
     *
     * @param ref 对象引用 / Object reference
     * @return 对象字节，不存在返回 null / Object bytes, null if absent
    */
    suspend fun get(ref: ObjectRef): ByteArray?

    /**
     * 删除对象。
     * Delete object.
     *
     * @param ref 对象引用 / Object reference
     * @return 是否删除成功 / Whether deleted
    */
    suspend fun delete(ref: ObjectRef): Boolean

    /**
     * 判断对象是否存在。
     * Check object existence.
     *
     * @param ref 对象引用 / Object reference
     * @return 是否存在 / Whether exists
    */
    suspend fun exists(ref: ObjectRef): Boolean
}
