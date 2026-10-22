package fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence

import java.nio.file.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration.*

/** 策略与检查点存储接口 / Policy and checkpoint storage interface. */
interface SearchStore {
    /** 保存策略 / Save policy.
     * @param key 存储键 / Storage key
     * @param policy 策略 / Policy
     * @return 保存结果 / Save result
     */
    fun savePolicy(key: String, policy: PolicyArchive): Try

    /** 加载策略 / Load policy.
     * @param key 存储键 / Storage key
     * @return 策略 / Policy
     */
    fun loadPolicy(key: String): Ret<PolicyArchive>

    /** 保存检查点 / Save checkpoint.
     * @param key 存储键 / Storage key
     * @param checkpoint 检查点 / Checkpoint
     * @return 保存结果 / Save result
     */
    fun saveCheckpoint(key: String, checkpoint: RunCheckpoint): Try

    /** 加载检查点 / Load checkpoint.
     * @param key 存储键 / Storage key
     * @return 检查点 / Checkpoint
     */
    fun loadCheckpoint(key: String): Ret<RunCheckpoint>
}

@Serializable
private data class StoredDocument(val version: Int = 1, val kind: String, val checksum: String, val payload: JsonElement)

/** 原子替换文件存储，拒绝路径穿越 / Atomic-replacement file storage rejecting path traversal.
 * @param directory 独占存储目录 / Storage directory
 * @property policyMigrations 策略版本迁移 / Policy version migrations
 * @property checkpointMigrations 检查点版本迁移 / Checkpoint version migrations
 */
class FileSearchStore(
    directory: Path,
    private val policyMigrations: Map<Int, ConfigurationMigration> = emptyMap(),
    private val checkpointMigrations: Map<Int, ConfigurationMigration> = emptyMap()
) : SearchStore {
    private val root = directory.toAbsolutePath().normalize()

    override fun savePolicy(key: String, policy: PolicyArchive): Try = searchBoundary { Json.encodeToJsonElement(policy) }.then { write(key, "policy", it) }
    override fun loadPolicy(key: String): Ret<PolicyArchive> = read(key, "policy").then { value -> migrate(value, policyMigrations) }.then { value -> searchBoundary { Json.decodeFromJsonElement<PolicyArchive>(value) } }
    override fun saveCheckpoint(key: String, checkpoint: RunCheckpoint): Try = searchBoundary { Json.encodeToJsonElement(checkpoint) }.then { write(key, "checkpoint", it) }
    override fun loadCheckpoint(key: String): Ret<RunCheckpoint> = read(key, "checkpoint").then { value -> migrate(value, checkpointMigrations) }.then { value -> searchBoundary { Json.decodeFromJsonElement<RunCheckpoint>(value) } }

    private fun migrate(value: JsonElement, migrations: Map<Int, ConfigurationMigration>): Ret<JsonObject> {
        val document = value as? JsonObject ?: return searchInvalid("stored payload must be an object")
        return migrateDocument(document, migrations)
    }

    private fun target(key: String): Ret<Path> {
        if (key.isBlank() || key.any { it == '/' || it == '\\' || it == ':' } || key == "." || key == "..") return searchInvalid("invalid storage key")
        val target = root.resolve(key).normalize()
        return if (target.startsWith(root) && target != root) Ok(target) else searchInvalid("storage path escapes root")
    }

    private fun write(
        key: String,
        kind: String,
        value: JsonElement
    ): Try {
        return target(key).then { destination ->
            searchBoundary {
                Files.createDirectories(root)
                val temp = Files.createTempFile(root, ".search-", ".tmp")
                try {
                    Files.writeString(temp, Json.encodeToString(StoredDocument(kind = kind, checksum = fingerprint(value), payload = value)))
                    try {
                        Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                    } catch (_: AtomicMoveNotSupportedException) {
                        Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING)
                    }
                } finally {
                    Files.deleteIfExists(temp)
                }
                success
            }
        }
    }

    private fun read(key: String, kind: String): Ret<JsonElement> {
        return target(key).then { source ->
            if (Files.isSymbolicLink(source)) return@then searchInvalid("symbolic-link storage entries are not supported")
            searchBoundary {
                if (Files.size(source) > 64L * 1024 * 1024) null else Json.decodeFromString<StoredDocument>(Files.readString(source))
            }.then { document ->
                if (document == null || document.version != 1 || document.kind != kind || document.checksum != fingerprint(document.payload)) searchInvalid("incompatible or corrupt stored document")
                else Ok(document.payload)
            }
        }
    }
}

/** 配置版本迁移入口，调用方必须显式指定支持的旧版本 / Configuration migration entry requiring explicitly supported source versions. */
fun interface ConfigurationMigration {
    /** 迁移到当前配置结构 / Migrate to the current configuration structure.
     * @param document 旧配置 / Old configuration
     * @return 当前配置 JSON / Current configuration JSON
     */
    fun migrate(document: JsonObject): Ret<JsonObject>
}
