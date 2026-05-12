/**
 * 符号标识序列化与反序列化
 * Symbol Identity Serialization and Deserialization
 *
 * 提供符号标识的序列化与反序列化功能，支持简单符号、带 ID 符号和复合符号的表达式。
 * Provides serialization and deserialization for symbol identities, supporting simple, ID-bearing, and composite symbol expressions.
 */
package fuookami.ospf.kotlin.math.symbol.serde

import fuookami.ospf.kotlin.math.symbol.IdentifiedSymbol
import fuookami.ospf.kotlin.math.symbol.OwnedSymbol
import fuookami.ospf.kotlin.math.symbol.OwnedSymbolLike
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.SymbolId
import fuookami.ospf.kotlin.utils.serialization.writeJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

const val SerializedSymbolIdentityPrefix = "__ospf_symbol_identity__"

private data class DefaultSymbol(
    override val name: String,
    override val displayName: String? = null
) : Symbol

private fun defaultSymbolOf(name: String): Symbol {
    return DefaultSymbol(name)
}

/**
 * 符号标识表达弌
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
     * 简单符号标诌
     * Simple symbol identity
     */
    @Serializable
    data class Simple(
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 希ID 符号标识
     * ID-bearing symbol identity
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
     */
    @Serializable
    data class Composite(
        val operator: String,
        val arg: SymbolIdentityExpr,
        override val name: String,
        override val displayName: String? = null
    ) : SymbolIdentityExpr

    /**
     * 多参数复合符号标诌
     * Multi-argument composite symbol identity
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

private fun ByteArray.toHexString(): String {
    return joinToString(separator = "") { value -> "%02x".format(value) }
}

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

private fun JsonObject.stringOrNull(key: String): String? {
    val element = this[key] ?: return null
    return try {
        element.jsonPrimitive.content
    } catch (_: Exception) {
        null
    }
}

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

private fun parseIdentityExprOrNull(json: String): SymbolIdentityExpr? {
    return try {
        identityExprFromJsonElement(symbolIdentityJson.parseToJsonElement(json))
    } catch (_: Exception) {
        null
    }
}

fun SymbolIdentityExpr.toSerializedIdentifier(): String {
    if (this is SymbolIdentityExpr.Simple && displayName == null && !name.startsWith(SerializedSymbolIdentityPrefix)) {
        return name
    }
    val payload = writeJson(this).toByteArray(Charsets.UTF_8).toHexString()
    return "$SerializedSymbolIdentityPrefix$payload"
}

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
