package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import java.security.MessageDigest
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlTable
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.ConfigurationMigration
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.HyperHeuristicConfig

/** 配置文件格式 / Configuration format. */
enum class ConfigurationFormat { Json, Yaml, Toml }

/** JSON/YAML/TOML共用配置与校验，不启用反射多态 / Shared JSON/YAML/TOML configuration without reflective polymorphism. */
object ConfigurationFormats {
    private val json = Json { encodeDefaults = true }

    /** 读取完整配置 / Read complete configuration.
     * @param text 配置文本 / Configuration text
     * @param format 文件格式 / File format
     * @param migrations 显式旧版本迁移 / Explicit source-version migrations
     * @return 已校验配置 / Validated configuration
     */
    fun decode(
        text: String,
        format: ConfigurationFormat,
        migrations: Map<Int, ConfigurationMigration> = emptyMap()
    ): Ret<HyperHeuristicConfig> {
        if (text.toByteArray(Charsets.UTF_8).size > 4 * 1024 * 1024) return searchInvalid("configuration exceeds 4 MiB")
        if (format == ConfigurationFormat.Toml) {
            return searchBoundary { Toml.parse(text) }.then { parsed ->
                if (parsed.hasErrors()) return@then searchInvalid(parsed.errors().joinToString("; "))
                // Jackson TOML读取会丢失负零及浮点类型；标准解析后直接保留Double / Jackson TOML reading loses signed zero and float types; preserve Double from the standard parser.
                searchBoundary { ObjectMapper().valueToTree<JsonNode>(tomlValue(parsed)) }
            }.then { decodeTree(it, migrations) }
        }
        return searchBoundary {
            val mapper = mapper(format)
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            val tree = mapper.readTree(text)
            tree
        }.then { decodeTree(it, migrations) }
    }

    private fun decodeTree(tree: JsonNode, migrations: Map<Int, ConfigurationMigration>): Ret<HyperHeuristicConfig> {
        if (!finite(tree)) return searchInvalid("配置含非有限数 / Non-finite configuration number")
        return searchBoundary { json.parseToJsonElement(tree.toString()) }.then { document ->
            val record = document as? JsonObject ?: return@then searchInvalid("configuration must be an object")
            migrateDocument(record, migrations).then { migrated ->
                searchBoundary { json.decodeFromJsonElement<HyperHeuristicConfig>(migrated) }.then { config -> config.validate().map { config } }
            }
        }
    }

    private fun tomlValue(value: Any?): Any? = when (value) {
        is TomlTable -> value.toMap().mapValues { tomlValue(it.value) }
        is TomlArray -> value.toList().map { tomlValue(it) }
        is java.time.OffsetDateTime -> value.format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        is java.time.LocalDateTime -> value.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        is java.time.LocalTime -> value.format(java.time.format.DateTimeFormatter.ISO_LOCAL_TIME)
        is java.time.temporal.TemporalAccessor -> value.toString()
        else -> value
    }

    /** 输出完整配置 / Write complete configuration.
     * @param config 配置 / Configuration
     * @param format 格式 / Format
     * @return 文本 / Text
     */
    fun encode(config: HyperHeuristicConfig, format: ConfigurationFormat): Ret<String> {
        return config.validate().then {
            searchBoundary {
                val tree = ObjectMapper().readTree(json.encodeToString(config))
                if (format == ConfigurationFormat.Toml) removeAbsent(tree)
                tree
            }.then { tree ->
                if (format == ConfigurationFormat.Toml && containsNull(tree)) return@then searchInvalid("TOML不支持参数null / TOML cannot represent null parameters")
                searchBoundary { mapper(format).writeValueAsString(tree) }
            }
        }
    }

    private fun mapper(format: ConfigurationFormat): ObjectMapper = when (format) {
        ConfigurationFormat.Json -> ObjectMapper()
        ConfigurationFormat.Yaml -> ObjectMapper(YAMLFactory())
        ConfigurationFormat.Toml -> ObjectMapper(TomlFactory())
    }

    private fun finite(value: JsonNode): Boolean = when {
        value.isFloatingPointNumber -> value.doubleValue().isFinite()
        value.isContainerNode -> value.all { finite(it) }
        else -> true
    }

    private fun containsNull(value: JsonNode): Boolean = value.isNull || value.isContainerNode && value.any { containsNull(it) }

    private fun removeAbsent(root: JsonNode) {
        fun omit(record: JsonNode?, names: List<String>) {
            val fields = record as? com.fasterxml.jackson.databind.node.ObjectNode ?: return
            for (name in names) if (fields.get(name)?.isNull == true) fields.remove(name)
        }
        fun algorithm(record: JsonNode) {
            omit(record, listOf("scalarization", "constraints"))
            omit(record.get("constraints"), listOf("initialEpsilon"))
            record.get("children")?.forEach { algorithm(it) }
        }
        // 仅移除模型声明的可缺省字段，不吞掉业务参数中的null / Omit only model-declared optional fields, never null domain parameters.
        omit(root.get("budget"), listOf("evaluations", "decisions", "steps", "timeMillis"))
        omit(root.get("learning"), listOf("trajectoryPath"))
        omit(root.get("persistence"), listOf("checkpointPath"))
        root.get("algorithms")?.forEach { algorithm(it) }
    }
}

internal fun canonicalJson(value: JsonElement): String = when (value) {
    is JsonObject -> value.keys.sorted().joinToString(prefix = "{", postfix = "}") { key -> JsonPrimitive(key).toString() + ":" + canonicalJson(value.getValue(key)) }
    is JsonArray -> value.joinToString(prefix = "[", postfix = "]") { canonicalJson(it) }
    else -> value.toString()
}

internal fun fingerprint(value: JsonElement): String {
    return MessageDigest.getInstance("SHA-256").digest(canonicalJson(value).toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}

internal fun migrateDocument(document: JsonObject, migrations: Map<Int, ConfigurationMigration>): Ret<JsonObject> {
    val version = document["version"]?.jsonPrimitive?.intOrNull ?: 1
    if (version == 1) return Ok(document)
    val migration = migrations[version] ?: return searchInvalid("unsupported document version: $version")
    return searchBoundary { migration.migrate(document) }.then { it }.then { migrated ->
        if (migrated["version"]?.jsonPrimitive?.intOrNull != 1) searchInvalid("migration must produce version 1") else Ok(migrated)
    }
}
