/**
 * 本地文件对象存储
 * Local file object storage
 */
package fuookami.ospf.kotlin.framework.solver.remote.adapter.localfs

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import fuookami.ospf.kotlin.framework.solver.remote.domain.*
import fuookami.ospf.kotlin.framework.solver.remote.port.ObjectStoragePort

/**
 * 本地文件对象存储端口实现。
 * Local file object storage port implementation.
 *
 * @property root 存储根目录 / Storage root directory
 */
class LocalFileObjectStoragePort(
    root: Path
) : ObjectStoragePort {
    private val root = root.toAbsolutePath().normalize().also {
        it.createDirectories()
    }

    override suspend fun put(
        path: ObjectPath,
        bytes: ByteArray,
        metadata: Map<String, String>
    ): ObjectRef {
        val objectPath = resolveObjectPath(path)
        objectPath.parent?.createDirectories()
        Files.write(objectPath, bytes)
        if (metadata.isNotEmpty()) {
            Files.writeString(metadataPath(objectPath), encodeMetadata(metadata))
        }
        return ObjectRef(
            path = normalizeObjectPath(path),
            version = ObjectVersion.of(Files.getLastModifiedTime(objectPath).toMillis().toString()),
            etag = ObjectEtag.of(sha256(bytes))
        )
    }

    override suspend fun get(ref: ObjectRef): ByteArray? {
        val objectPath = resolveObjectPath(ref.path)
        return if (objectPath.exists()) {
            Files.readAllBytes(objectPath)
        } else {
            null
        }
    }

    override suspend fun delete(ref: ObjectRef): Boolean {
        val objectPath = resolveObjectPath(ref.path)
        val deleted = objectPath.deleteIfExists()
        metadataPath(objectPath).deleteIfExists()
        return deleted
    }

    override suspend fun exists(ref: ObjectRef): Boolean {
        return resolveObjectPath(ref.path).exists()
    }

    /**
     * Resolves an object path to an absolute filesystem path within the storage root.
     * 将对象路径解析为存储根目录内的绝对文件系统路径。
     *
     * @param path 对象路径 / Object path
     * @return 绝对文件系统路径 / Absolute filesystem path
     */
    private fun resolveObjectPath(path: ObjectPath): Path {
        val normalizedPath = normalizeObjectPath(path)
        val resolved = root.resolve(normalizedPath.value).toAbsolutePath().normalize()
        require(resolved.startsWith(root)) {
            "Object path escapes storage root: $path"
        }
        return resolved
    }

    /**
     * Normalizes an object path value.
     * 规范化对象路径值。
     *
     * @param path 对象路径 / Object path
     * @return 规范化后的对象路径 / Normalized object path
     */
    private fun normalizeObjectPath(path: ObjectPath): ObjectPath {
        return ObjectPath.of(path.value)
    }

    /**
     * Computes the metadata file path for a given object path.
     * 计算给定对象路径的元数据文件路径。
     *
     * @param path 对象文件路径 / Object file path
     * @return 元数据文件路径 / Metadata file path
     */
    private fun metadataPath(path: Path): Path {
        return path.resolveSibling("${path.fileName}.metadata")
    }

    /**
     * Encodes metadata map to a string representation.
     * 将元数据映射编码为字符串表示。
     *
     * @param metadata 元数据键值对 / Metadata key-value pairs
     * @return 编码后的字符串 / Encoded string
     */
    private fun encodeMetadata(metadata: Map<String, String>): String {
        return metadata.entries.joinToString("\n") { (key, value) ->
            "${key.replace("\n", "\\n")}=${value.replace("\n", "\\n")}"
        }
    }

    /**
     * Computes the SHA-256 hash of the given bytes.
     * 计算给定字节的 SHA-256 哈希值。
     *
     * @param bytes 字节数组 / Byte array
     * @return 十六进制哈希字符串 / Hexadecimal hash string
     */
    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
