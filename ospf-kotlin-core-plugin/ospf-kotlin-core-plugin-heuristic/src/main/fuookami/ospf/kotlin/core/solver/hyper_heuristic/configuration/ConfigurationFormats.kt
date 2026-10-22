package fuookami.ospf.kotlin.core.solver.hyper_heuristic.configuration

import java.security.MessageDigest
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.persistence.ConfigurationMigration
import fuookami.ospf.kotlin.core.solver.hyper_heuristic.HyperHeuristicConfig

/** 配置文件格式 / Configuration format. */
enum class ConfigurationFormat { Json, Yaml }

/** JSON/YAML 共用配置与校验，不启用反射多态 / Shared JSON/YAML configuration without reflective polymorphism. */
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
        if (text.length > 4 * 1024 * 1024) return searchInvalid("configuration exceeds 4 MiB")
        return searchBoundary {
            val mapper = if (format == ConfigurationFormat.Yaml) ObjectMapper(YAMLFactory()) else ObjectMapper()
            mapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
            mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            val tree = mapper.readTree(text)
            json.parseToJsonElement(tree.toString())
        }.then { document ->
            val record = document as? JsonObject ?: return@then searchInvalid("configuration must be an object")
            migrateDocument(record, migrations).then { migrated ->
                searchBoundary { json.decodeFromJsonElement<HyperHeuristicConfig>(migrated) }.then { config -> config.validate().map { config } }
            }
        }
    }

    /** 输出完整配置 / Write complete configuration.
     * @param config 配置 / Configuration
     * @param format 格式 / Format
     * @return 文本 / Text
     */
    fun encode(config: HyperHeuristicConfig, format: ConfigurationFormat): Ret<String> {
        return config.validate().then {
            searchBoundary {
                val text = json.encodeToString(config)
                if (format == ConfigurationFormat.Json) text else {
                    val tree = ObjectMapper().readTree(text)
                    ObjectMapper(YAMLFactory()).writeValueAsString(tree)
                }
            }
        }
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
