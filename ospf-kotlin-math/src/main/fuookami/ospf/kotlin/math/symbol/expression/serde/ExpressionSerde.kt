/**
 * 表达式序列化与反序列�?
 * Expression Serialization and Deserialization
 *
 * 提供 ScalarExpression �?BooleanExpression �?JSON 序列�?反序列化�?
 * Provides JSON serialization/deserialization for ScalarExpression and BooleanExpression.
 *
 * 基于 kotlinx.serialization 实现�?
 * Implemented using kotlinx.serialization.
 */
package fuookami.ospf.kotlin.math.symbol.expression.serde

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent
import fuookami.ospf.kotlin.math.symbol.serde.symbolOfSerializedIdentifier
import fuookami.ospf.kotlin.math.symbol.serde.toSymbolIdentityExpr
import fuookami.ospf.kotlin.math.symbol.serde.toSerializedIdentifier

// ========== 序列化模�?/ Serialization Models ==========

@Serializable
internal sealed interface ScalarExpressionData {
    val typeName: String

    @Serializable
    @SerialName("Constant")
    data class Constant(val value: JsonElement) : ScalarExpressionData {
        override val typeName = "Constant"
    }

    @Serializable
    @SerialName("Reference")
    data class Reference(val path: String) : ScalarExpressionData {
        override val typeName = "Reference"
    }

    @Serializable
    @SerialName("SymbolReference")
    data class SymbolReference(val identifier: String) : ScalarExpressionData {
        override val typeName = "SymbolReference"
    }

    @Serializable
    @SerialName("Unary")
    data class Unary(
        val operator: String,
        val operand: ScalarExpressionData
    ) : ScalarExpressionData {
        override val typeName = "Unary"
    }

    @Serializable
    @SerialName("Binary")
    data class Binary(
        val operator: String,
        val left: ScalarExpressionData,
        val right: ScalarExpressionData
    ) : ScalarExpressionData {
        override val typeName = "Binary"
    }

    @Serializable
    @SerialName("Function")
    data class Function(
        val name: String,
        val arguments: List<ScalarExpressionData>
    ) : ScalarExpressionData {
        override val typeName = "Function"
    }

    @Serializable
    @SerialName("Custom")
    data class Custom(
        val payload: String?,
        val description: String?
    ) : ScalarExpressionData {
        override val typeName = "Custom"
    }
}

@Serializable
internal sealed interface BooleanExpressionData {
    val typeName: String

    @Serializable
    @SerialName("BooleanConstant")
    data class BooleanConstant(val value: String) : BooleanExpressionData {
        override val typeName = "BooleanConstant"
    }

    @Serializable
    @SerialName("Comparison")
    data class Comparison(
        val operator: String,
        val left: ScalarExpressionData,
        val right: ScalarExpressionData
    ) : BooleanExpressionData {
        override val typeName = "Comparison"
    }

    @Serializable
    @SerialName("In")
    data class In(
        val value: ScalarExpressionData,
        val candidates: List<ScalarExpressionData>,
        val negated: Boolean = false
    ) : BooleanExpressionData {
        override val typeName = "In"
    }

    @Serializable
    @SerialName("PatternMatch")
    data class PatternMatch(
        val value: ScalarExpressionData,
        val pattern: ScalarExpressionData,
        val mode: String,
        val negated: Boolean = false
    ) : BooleanExpressionData {
        override val typeName = "PatternMatch"
    }

    @Serializable
    @SerialName("NullCheck")
    data class NullCheck(
        val path: String,
        @SerialName("nullCheckType") val checkType: String
    ) : BooleanExpressionData {
        override val typeName = "NullCheck"
    }

    @Serializable
    @SerialName("And")
    data class And(val operands: List<BooleanExpressionData>) : BooleanExpressionData {
        override val typeName = "And"
    }

    @Serializable
    @SerialName("Or")
    data class Or(val operands: List<BooleanExpressionData>) : BooleanExpressionData {
        override val typeName = "Or"
    }

    @Serializable
    @SerialName("Not")
    data class Not(val operand: BooleanExpressionData) : BooleanExpressionData {
        override val typeName = "Not"
    }

    @Serializable
    @SerialName("Custom")
    data class Custom(
        val payload: String?,
        val description: String?
    ) : BooleanExpressionData {
        override val typeName = "Custom"
    }
}

// ========== 序列化工�?/ Serialization Utilities ==========

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
    classDiscriminator = "type"
}

// ========== ScalarExpression 序列�?/ ScalarExpression Serialization ==========

/**
 * �?ScalarExpression 转换为序列化数据
 * Convert ScalarExpression to serialization data
 */
internal fun ScalarExpression<*>.toData(): ScalarExpressionData = when (this) {
    is ScalarConstant<*> -> ScalarExpressionData.Constant(
        value = when (value) {
            is String -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            null -> JsonNull
            else -> JsonPrimitive(value.toString())
        }
    )
    is ScalarReference<*> -> ScalarExpressionData.Reference(path.value)
    is ScalarSymbolReference<*> -> ScalarExpressionData.SymbolReference(symbol.toSymbolIdentityExpr().toSerializedIdentifier())
    is ScalarUnary<*> -> ScalarExpressionData.Unary(
        operator = operator.name,
        operand = operand.toData()
    )
    is ScalarBinary<*> -> ScalarExpressionData.Binary(
        operator = operator.name,
        left = left.toData(),
        right = right.toData()
    )
    is ScalarFunction<*> -> ScalarExpressionData.Function(
        name = name,
        arguments = arguments.map { it.toData() }
    )
    is ScalarCustom<*> -> ScalarExpressionData.Custom(value.toString(), description)
}

/**
 * 从序列化数据恢复 ScalarExpression
 * Restore ScalarExpression from serialization data
 */
internal fun ScalarExpressionData.toScalarExpression(): ScalarExpression<Any> = when (this) {
    is ScalarExpressionData.Constant -> {
        val v: Any? = when (value) {
            is JsonPrimitive -> when {
                value.isString -> value.contentOrNull
                value.booleanOrNull != null -> value.booleanOrNull
                value.intOrNull != null -> value.intOrNull
                value.longOrNull != null -> value.longOrNull
                value.doubleOrNull != null -> value.doubleOrNull
                else -> value.contentOrNull
            }
            JsonNull -> null
            else -> value.toString()
        }
        ScalarConstant(v) as ScalarExpression<Any>
    }
    is ScalarExpressionData.Reference -> ScalarReference<Any>(PropertyPath.parse(path))
    is ScalarExpressionData.SymbolReference -> ScalarSymbolReference<Any>(symbolOfSerializedIdentifier(identifier))
    is ScalarExpressionData.Unary -> ScalarUnary(
        UnaryOperator.valueOf(operator),
        operand.toScalarExpression()
    ) as ScalarExpression<Any>
    is ScalarExpressionData.Binary -> ScalarBinary(
        BinaryOperator.valueOf(operator),
        left.toScalarExpression(),
        right.toScalarExpression()
    ) as ScalarExpression<Any>
    is ScalarExpressionData.Function -> ScalarFunction(
        name,
        arguments.map { it.toScalarExpression() }
    ) as ScalarExpression<Any>
    is ScalarExpressionData.Custom -> ScalarCustom<Any>(payload ?: Unit, description)
}

// ========== BooleanExpression 序列�?/ BooleanExpression Serialization ==========

/**
 * �?BooleanExpression 转换为序列化数据
 * Convert BooleanExpression to serialization data
 */
internal fun BooleanExpression.toData(): BooleanExpressionData = when (this) {
    is BooleanConstant -> BooleanExpressionData.BooleanConstant(
        value = when (value) {
            Trivalent.True -> "true"
            Trivalent.False -> "false"
            Trivalent.Unknown -> "unknown"
        }
    )
    is Comparison<*> -> BooleanExpressionData.Comparison(
        operator = operator.name,
        left = left.toData(),
        right = right.toData()
    )
    is InExpression<*> -> BooleanExpressionData.In(
        value = value.toData(),
        candidates = candidates.map { it.toData() },
        negated = negated
    )
    is PatternMatch<*> -> BooleanExpressionData.PatternMatch(
        value = value.toData(),
        pattern = pattern.toData(),
        mode = mode.name,
        negated = negated
    )
    is NullCheck -> BooleanExpressionData.NullCheck(
        path = path.value,
        checkType = type.name
    )
    is AndExpression -> BooleanExpressionData.And(
        operands = operands.map { it.toData() }
    )
    is OrExpression -> BooleanExpressionData.Or(
        operands = operands.map { it.toData() }
    )
    is NotExpression -> BooleanExpressionData.Not(
        operand = operand.toData()
    )
    is BooleanCustom -> BooleanExpressionData.Custom(value.toString(), description)
}

/**
 * 从序列化数据恢复 BooleanExpression
 * Restore BooleanExpression from serialization data
 */
internal fun BooleanExpressionData.toBooleanExpression(): BooleanExpression = when (this) {
    is BooleanExpressionData.BooleanConstant -> BooleanConstant(
        when (value.lowercase()) {
            "true" -> Trivalent.True
            "false" -> Trivalent.False
            else -> Trivalent.Unknown
        }
    )
    is BooleanExpressionData.Comparison -> Comparison(
        ComparisonOperator.valueOf(operator),
        left.toScalarExpression(),
        right.toScalarExpression()
    )
    is BooleanExpressionData.In -> InExpression(
        value.toScalarExpression(),
        candidates.map { it.toScalarExpression() },
        negated
    )
    is BooleanExpressionData.PatternMatch -> PatternMatch(
        value.toScalarExpression(),
        pattern.toScalarExpression(),
        PatternMatchMode.valueOf(mode),
        negated
    )
    is BooleanExpressionData.NullCheck -> NullCheck(
        PropertyPath.parse(path),
        NullCheckType.valueOf(checkType)
    )
    is BooleanExpressionData.And -> AndExpression(
        operands.map { it.toBooleanExpression() }
    )
    is BooleanExpressionData.Or -> OrExpression(
        operands.map { it.toBooleanExpression() }
    )
    is BooleanExpressionData.Not -> NotExpression(
        operand.toBooleanExpression()
    )
    is BooleanExpressionData.Custom -> BooleanCustom(payload ?: Unit, description)
}

// ========== 公共 API / Public API ==========

/**
 * 将布尔表达式序列化为 JSON 字符�?
 * Serialize boolean expression to JSON string
 */
fun BooleanExpression.toJsonString(): String {
    return json.encodeToString(BooleanExpressionData.serializer(), this.toData())
}

/**
 * �?JSON 字符串反序列化布尔表达式
 * Deserialize boolean expression from JSON string
 */
fun booleanExpressionFromJson(jsonString: String): BooleanExpression {
    val data = json.decodeFromString(BooleanExpressionData.serializer(), jsonString)
    return data.toBooleanExpression()
}

/**
 * 尝试�?JSON 字符串反序列化布尔表达式
 * Try to deserialize boolean expression from JSON string
 */
fun booleanExpressionFromJsonOrNull(jsonString: String): BooleanExpression? {
    return try {
        booleanExpressionFromJson(jsonString)
    } catch (e: Exception) {
        null
    }
}

/**
 * 将标量表达式序列化为 JSON 字符�?
 * Serialize scalar expression to JSON string
 */
fun ScalarExpression<*>.toJsonString(): String {
    return json.encodeToString(ScalarExpressionData.serializer(), this.toData())
}

/**
 * �?JSON 字符串反序列化标量表达式
 * Deserialize scalar expression from JSON string
 */
fun scalarExpressionFromJson(jsonString: String): ScalarExpression<Any> {
    val data = json.decodeFromString(ScalarExpressionData.serializer(), jsonString)
    return data.toScalarExpression()
}

/**
 * 尝试�?JSON 字符串反序列化标量表达式
 * Try to deserialize scalar expression from JSON string
 */
fun scalarExpressionFromJsonOrNull(jsonString: String): ScalarExpression<Any>? {
    return try {
        scalarExpressionFromJson(jsonString)
    } catch (e: Exception) {
        null
    }
}
