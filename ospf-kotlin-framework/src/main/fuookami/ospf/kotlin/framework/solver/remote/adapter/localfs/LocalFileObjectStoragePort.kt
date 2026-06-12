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

    private fun resolveObjectPath(path: ObjectPath): Path {
        val normalizedPath = normalizeObjectPath(path)
        val resolved = root.resolve(normalizedPath.value).toAbsolutePath().normalize()
        require(resolved.startsWith(root)) {
            "Object path escapes storage root: $path"
        }
        return resolved
    }

    private fun normalizeObjectPath(path: ObjectPath): ObjectPath {
        return ObjectPath.of(path.value)
    }

    private fun metadataPath(path: Path): Path {
        return path.resolveSibling("${path.fileName}.metadata")
    }

    private fun encodeMetadata(metadata: Map<String, String>): String {
        return metadata.entries.joinToString("\n") { (key, value) ->
            "${key.replace("\n", "\\n")}=${value.replace("\n", "\\n")}"
        }
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
