/**
 * 符号标识序列化与反序列化
 * Symbol Identity Serialization and Deserialization
 *
 * 提供符号标识的序列化与反序列化功能，支持简单符号、带 ID 符号和复合符号的表达式。
 * Provides serialization and deserialization for symbol identities, supporting simple, ID-bearing, and composite symbol expressions.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable
import fuookami.ospf.kotlin.utils.serialization.writeJson
import fuookami.ospf.kotlin.math.symbol.*

/**
 * 序列化符号标识前缀
 * Serialized symbol identity prefix
 *
 * 用于标识经过序列化编码的符号标识字符串。
 * Used to identify serialized and encoded symbol identity strings.
 */
const val SerializedSymbolIdentityPrefix = "__ospf_symbol_identity__"

private data class DefaultSymbol(
    override val name: String,
    override val displayName: String? = null
) : Symbol

/** 根据名称创建默认符号 / Create a default symbol from a name */
private fun defaultSymbolOf(name: String): Symbol {
    return DefaultSymbol(name)
}

/**
 * 符号标识表达式
 * Symbol identity expression
 *
 * 表示符号标识的序列化表达式，支持简单、带 ID 和复合形式。
 * Represents serialized expressions for symbol identities, supporting simple, ID-bearing, and composite forms.
 */
@Serializable
sealed interface SymbolIdentityExpr {
    val name: String
    val displayName: String?

    /**
     * 简单符号标识
     * Simple symbol identity
     *
     * @property name 符号名称 / Symbol name
     * @property displayName 显示名称 / Display name
     */
    @Serializable
    data class Simple(
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 带 ID 符号标识
     * ID-bearing symbol identity
     *
     * @property name 符号名称 / Symbol name
     * @property id 符号标识 ID / Symbol identity ID
     * @property displayName 显示名称 / Display name
     */
    @Serializable
    data class WithId(
        override val name: String,
        val id: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 复合符号标识
     * Composite symbol identity
     *
     * @property operator 运算符名称 / Operator name
     * @property arg 单参数符号标识表达式 / Single-argument symbol identity expression
     * @property name 符号名称 / Symbol name
     * @property displayName 显示名称 / Display name
     */
    @Serializable
    data class Composite(
        val operator: String,
        val arg: SymbolIdentityExpr,
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 多参数复合符号标识
     * Multi-argument composite symbol identity
     *
     * @property operator 运算符名称 / Operator name
     * @property args 多参数符号标识表达式列表 / List of multi-argument symbol identity expressions
     * @property name 符号名称 / Symbol name
     * @property displayName 显示名称 / Display name
     */
    @Serializable
    data class CompositeMulti(
        val operator: String,
        val args: List<SymbolIdentityExpr>,
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr
}

private data class SerializedIdentitySymbol(
    val identityExpr: SymbolIdentityExpr
) : Symbol, IdentifiedSymbol {
    override val name: String = identityExpr.name
    override val displayName: String? = identityExpr.displayName
    override val symbolId: String = identityExpr.toSerializedIdentifier()
}

/** 将字节数组转换为十六进制字符串 / Convert a byte array to a hex string */
private fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { value -> "%02x".format(value) }
}

/** 将十六进制字符串转换为字节数组，格式无效时返回 null / Convert a hex string to a byte array, or null if invalid */
private fun String.hexToByteArrayOrNull(): ByteArray? {
    if (length % 2 != 0) {
        return null
    }
    return try {
        ByteArray(length / 2) { index ->
            val start = index * 2
            substring(start, start + 2).toInt(16).toByte()
        }
    } catch (_: NumberFormatException) {
        null
    }
}

private val symbolIdentityJson = Json {
    ignoreUnknownKeys = true
}

/**
 * 安全地从 JSON 对象中获取字符串字段
 * Safely get a string field from a JSON object
 *
 * @param key 字段名 / Field name
 * @return 字符串值，不存在或类型不匹配时返回 null / String value, or null if absent or type mismatch
 */
private fun JsonObject.stringOrNull(key: String): String? {
    val element = this[key] ?: return null
    return try {
        element.jsonPrimitive.content
    } catch (_: Exception) {
        null
    }
}

/**
 * 从 JSON 元素递归解析符号标识表达式
 * Recursively parse a symbol identity expression from a JSON element
 *
 * @param element JSON 元素 / JSON element
 * @return 解析后的符号标识表达式，失败时返回 null / Parsed symbol identity expression, or null on failure
 */
private fun identityExprFromJsonElement(element: JsonElement): SymbolIdentityExpr? {
    val objectValue = element as? JsonObject ?: return null
    val name = objectValue.stringOrNull("name")
    val displayName = objectValue.stringOrNull("displayName")
    val operator = objectValue.stringOrNull("operator")
    val id = objectValue.stringOrNull("id")
    val args = objectValue["args"] as? JsonArray
    val arg = objectValue["arg"]
    return when {
        operator != null && args != null -> {
            val parsedArgs = args.mapNotNull(::identityExprFromJsonElement)
            if (parsedArgs.size != args.size) {
                null
            } else {
                SymbolIdentityExpr.CompositeMulti(
                    operator = operator,
                    args = parsedArgs,
                    name = name ?: operator,
                    displayName = displayName
                )
            }
        }

        operator != null && arg != null -> {
            val parsedArg = identityExprFromJsonElement(arg) ?: return null
            SymbolIdentityExpr.Composite(
                operator = operator,
                arg = parsedArg,
                name = name ?: operator,
                displayName = displayName
            )
        }

        id != null && name != null -> {
            SymbolIdentityExpr.WithId(
                name = name,
                id = id,
                displayName = displayName
            )
        }

        name != null -> {
            SymbolIdentityExpr.Simple(
                name = name,
                displayName = displayName
            )
        }

        else -> null
    }
}

/**
 * 从 JSON 字符串解析符号标识表达式，失败时返回 null
 * Parse a symbol identity expression from a JSON string, or null on failure
 *
 * @param json JSON 字符串 / JSON string
 * @return 符号标识表达式 / Symbol identity expression
 */
private fun parseIdentityExprOrNull(json: String): SymbolIdentityExpr? {
    return try {
        identityExprFromJsonElement(symbolIdentityJson.parseToJsonElement(json))
    } catch (_: Exception) {
        null
    }
}

/**
 * 将符号标识表达式序列化为标识符字符串
 * Serializes a symbol identity expression into an identifier string
 *
 * @receiver 符号标识表达式 / Symbol identity expression
 * @return 序列化后的标识符字符串 / Serialized identifier string
 */
fun SymbolIdentityExpr.toSerializedIdentifier(): String {
    if (this is SymbolIdentityExpr.Simple && displayName == null && !name.startsWith(SerializedSymbolIdentityPrefix)) {
        return name
    }
    val payload = writeJson(this).toByteArray(Charsets.UTF_8).toHexString()
    return "$SerializedSymbolIdentityPrefix$payload"
}

/**
 * 将符号转换为符号标识表达式
 * Converts a symbol to a symbol identity expression
 *
 * @receiver 符号 / Symbol
 * @return 符号标识表达式 / Symbol identity expression
 */
fun Symbol.toSymbolIdentityExpr(): SymbolIdentityExpr {
    return when (this) {
        is SerializedIdentitySymbol -> identityExpr
        is OwnedSymbolLike -> SymbolIdentityExpr.WithId(
            name = name,
            id = id.value,
            displayName = displayName
        )
        is IdentifiedSymbol -> SymbolIdentityExpr.WithId(
            name = name,
            id = symbolId,
            displayName = displayName
        )
        else -> SymbolIdentityExpr.Simple(
            name = name,
            displayName = displayName
        )
    }
}

/**
 * 从序列化标识符字符串反序列化为符号
 * Deserializes a serialized identifier string into a symbol
 *
 * @param identifier 序列化的标识符字符串 / Serialized identifier string
 * @return 反序列化后的符号 / Deserialized symbol
 */
fun symbolOfSerializedIdentifier(identifier: String): Symbol {
    if (!identifier.startsWith(SerializedSymbolIdentityPrefix)) {
        return defaultSymbolOf(identifier)
    }
    val payload = identifier.removePrefix(SerializedSymbolIdentityPrefix)
    val bytes = payload.hexToByteArrayOrNull() ?: return defaultSymbolOf(identifier)
    val rawJson = bytes.toString(Charsets.UTF_8)
    val identityExpr = parseIdentityExprOrNull(rawJson) ?: return defaultSymbolOf(identifier)
    return when (identityExpr) {
        is SymbolIdentityExpr.Simple -> defaultSymbolOf(identityExpr.name).let { symbol ->
            if (identityExpr.displayName == null) symbol else DefaultSymbol(identityExpr.name, identityExpr.displayName)
        }

        is SymbolIdentityExpr.WithId -> OwnedSymbol(
            id = SymbolId(identityExpr.id),
            name = identityExpr.name,
            displayName = identityExpr.displayName
        )

        is SymbolIdentityExpr.Composite,
        is SymbolIdentityExpr.CompositeMulti -> SerializedIdentitySymbol(identityExpr)
    }
}
