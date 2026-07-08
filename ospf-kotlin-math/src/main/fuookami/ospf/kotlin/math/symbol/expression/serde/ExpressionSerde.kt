/**
 * 表达式序列化与反序列化
 * Expression Serialization and Deserialization
 *
 * 提供 ScalarExpression 和 BooleanExpression 的 JSON 序列化/反序列化。
 * Provides JSON serialization/deserialization for ScalarExpression and BooleanExpression.
 *
 * 基于 kotlinx.serialization 实现。
 * Implemented using kotlinx.serialization.
 */
package fuookami.ospf.kotlin.math.symbol.expression.serde

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import fuookami.ospf.kotlin.math.symbol.serde.*
import fuookami.ospf.kotlin.math.symbol.expression.*
import fuookami.ospf.kotlin.math.Trivalent

// ========== 序列化模型 / Serialization Models ==========

/**
 * Serialization data model for scalar expressions.
 * 中文标量表达式的序列化数据模型。
 */
@Serializable
internal sealed interface ScalarExpressionData {
    val typeName: String

    /**
     * Serialization data for a scalar constant value.
     * 中文标量常量值的序列化数据。
     */
    @Serializable
    @SerialName("Constant")
    data class Constant(val value: JsonElement) : ScalarExpressionData {
        override val typeName = "Constant"
    }

    /**
     * Serialization data for a scalar property path reference.
     * 中文标量属性路径引用的序列化数据。
     */
    @Serializable
    @SerialName("Reference")
    data class Reference(val path: String) : ScalarExpressionData {
        override val typeName = "Reference"
    }

    /**
     * Serialization data for a scalar symbol reference.
     * 中文标量符号引用的序列化数据。
     */
    @Serializable
    @SerialName("SymbolReference")
    data class SymbolReference(val identifier: String) : ScalarExpressionData {
        override val typeName = "SymbolReference"
    }

    /**
     * Serialization data for a unary scalar expression.
     * 中文一元标量表达式的序列化数据。
     */
    @Serializable
    @SerialName("Unary")
    data class Unary(
        val operator: String,
        val operand: ScalarExpressionData
    ) : ScalarExpressionData {
        override val typeName = "Unary"
    }

    /**
     * Serialization data for a binary scalar expression.
     * 中文二元标量表达式的序列化数据。
     */
    @Serializable
    @SerialName("Binary")
    data class Binary(
        val operator: String,
        val left: ScalarExpressionData,
        val right: ScalarExpressionData
    ) : ScalarExpressionData {
        override val typeName = "Binary"
    }

    /**
     * Serialization data for a scalar function call expression.
     * 中文标量函数调用表达式的序列化数据。
     */
    @Serializable
    @SerialName("Function")
    data class Function(
        val name: String,
        val arguments: List<ScalarExpressionData>
    ) : ScalarExpressionData {
        override val typeName = "Function"
    }

    /**
     * Serialization data for a custom scalar expression.
     * 中文自定义标量表达式的序列化数据。
     */
    @Serializable
    @SerialName("Custom")
    data class Custom(
        val payload: String?,
        val description: String?
    ) : ScalarExpressionData {
        override val typeName = "Custom"
    }

    /**
     * Serialization data for a scalar conditional expression.
     * 中文标量条件表达式的序列化数据。
     */
    @Serializable
    @SerialName("Conditional")
    data class Conditional(
        val condition: BooleanExpressionData,
        val thenBranch: ScalarExpressionData,
        val elseBranch: ScalarExpressionData
    ) : ScalarExpressionData {
        override val typeName = "Conditional"
    }

    /**
     * Serialization data for a scalar boolean wrapper expression.
     * 中文标量布尔包装表达式的序列化数据。
     */
    @Serializable
    @SerialName("Boolean")
    data class Boolean(val expr: BooleanExpressionData) : ScalarExpressionData {
        override val typeName = "Boolean"
    }
}

/**
 * Serialization data model for boolean expressions.
 * 中文布尔表达式的序列化数据模型。
 */
@Serializable
internal sealed interface BooleanExpressionData {
    val typeName: String

    /**
     * Serialization data for a boolean constant.
     * 中文布尔常量的序列化数据。
     */
    @Serializable
    @SerialName("BooleanConstant")
    data class BooleanConstant(val value: String) : BooleanExpressionData {
        override val typeName = "BooleanConstant"
    }

    /**
     * Serialization data for a comparison expression.
     * 中文比较表达式的序列化数据。
     */
    @Serializable
    @SerialName("Comparison")
    data class Comparison(
        val operator: String,
        val left: ScalarExpressionData,
        val right: ScalarExpressionData
    ) : BooleanExpressionData {
        override val typeName = "Comparison"
    }

    /**
     * Serialization data for a set membership (in) expression.
     * 中文集合成员判断表达式的序列化数据。
     */
    @Serializable
    @SerialName("In")
    data class In(
        val value: ScalarExpressionData,
        val candidates: List<ScalarExpressionData>,
        val negated: Boolean = false
    ) : BooleanExpressionData {
        override val typeName = "In"
    }

    /**
     * Serialization data for a pattern match expression.
     * 中文模式匹配表达式的序列化数据。
     */
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

    /**
     * Serialization data for a null check expression.
     * 中文空值检查表达式的序列化数据。
     */
    @Serializable
    @SerialName("NullCheck")
    data class NullCheck(
        val path: String,
        @SerialName("nullCheckType") val checkType: String
    ) : BooleanExpressionData {
        override val typeName = "NullCheck"
    }

    /**
     * Serialization data for a logical AND expression.
     * 中文逻辑与表达式的序列化数据。
     */
    @Serializable
    @SerialName("And")
    data class And(val operands: List<BooleanExpressionData>) : BooleanExpressionData {
        override val typeName = "And"
    }

    /**
     * Serialization data for a logical OR expression.
     * 中文逻辑或表达式的序列化数据。
     */
    @Serializable
    @SerialName("Or")
    data class Or(val operands: List<BooleanExpressionData>) : BooleanExpressionData {
        override val typeName = "Or"
    }

    /**
     * Serialization data for a logical NOT expression.
     * 中文逻辑非表达式的序列化数据。
     */
    @Serializable
    @SerialName("Not")
    data class Not(val operand: BooleanExpressionData) : BooleanExpressionData {
        override val typeName = "Not"
    }

    /**
     * Serialization data for a custom boolean expression.
     * 中文自定义布尔表达式的序列化数据。
     */
    @Serializable
    @SerialName("Custom")
    data class Custom(
        val payload: String?,
        val description: String?
    ) : BooleanExpressionData {
        override val typeName = "Custom"
    }
}

// ========== 序列化工具 / Serialization Utilities ==========

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    prettyPrint = false
    classDiscriminator = "type"
}

/**
 * Safely cast ScalarExpression<Any?> to ScalarExpression<Any> at the JSON deserialization boundary.
 * 中文将 ScalarExpression<Any?> 安全转换为 ScalarExpression<Any>，用于 JSON 反序列化边界。
 */
@Suppress("UNCHECKED_CAST")
private fun ScalarExpression<Any?>.asAnyScalarExpression(): ScalarExpression<Any> {
    // JSON 反序列化边界只恢复表达式树结构，调用方按 ScalarExpression<Any> 继续处理。
    // The JSON boundary restores expression-tree shape only; callers continue with ScalarExpression<Any>.
    return this as ScalarExpression<Any>
}

// ========== ScalarExpression 序列化 / ScalarExpression Serialization ==========

/**
 * Convert ScalarExpression to serialization data.
 * 中文将 ScalarExpression 转换为序列化数据。
 *
 * @return the serialization data / 序列化数据
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
    is ScalarConditional<*> -> ScalarExpressionData.Conditional(
        condition = condition.toData(),
        thenBranch = thenBranch.toData(),
        elseBranch = elseBranch.toData()
    )
    is ScalarBoolean<*> -> ScalarExpressionData.Boolean(
        expr = expr.toData()
    )
    is ScalarCustom<*> -> ScalarExpressionData.Custom(value.toString(), description)
}

/**
 * Restore ScalarExpression from serialization data.
 * 中文从序列化数据恢复 ScalarExpression。
 *
 * @return the restored scalar expression / 恢复的标量表达式
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
        ScalarConstant(v).asAnyScalarExpression()
    }
    is ScalarExpressionData.Reference -> ScalarReference<Any>(PropertyPath.parse(path))
    is ScalarExpressionData.SymbolReference -> ScalarSymbolReference<Any>(symbolOfSerializedIdentifier(identifier))
    is ScalarExpressionData.Unary -> ScalarUnary(
        UnaryOperator.valueOf(operator),
        operand.toScalarExpression()
    ).asAnyScalarExpression()
    is ScalarExpressionData.Binary -> ScalarBinary(
        BinaryOperator.valueOf(operator),
        left.toScalarExpression(),
        right.toScalarExpression()
    ).asAnyScalarExpression()
    is ScalarExpressionData.Function -> ScalarFunction(
        name,
        arguments.map { it.toScalarExpression() }
    ).asAnyScalarExpression()
    is ScalarExpressionData.Conditional -> ScalarConditional(
        condition.toBooleanExpression(),
        thenBranch.toScalarExpression(),
        elseBranch.toScalarExpression()
    ).asAnyScalarExpression()
    is ScalarExpressionData.Boolean -> ScalarBoolean<Any>(
        expr.toBooleanExpression()
    ).asAnyScalarExpression()
    is ScalarExpressionData.Custom -> ScalarCustom<Any>(payload ?: Unit, description)
}

// ========== BooleanExpression 序列化 / BooleanExpression Serialization ==========

/**
 * Convert BooleanExpression to serialization data.
 * 中文将 BooleanExpression 转换为序列化数据。
 *
 * @return the serialization data / 序列化数据
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
 * Restore BooleanExpression from serialization data.
 * 中文从序列化数据恢复 BooleanExpression。
 *
 * @return the restored boolean expression / 恢复的布尔表达式
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
 * 将布尔表达式序列化为 JSON 字符串
 * Serialize boolean expression to JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun BooleanExpression.toJsonString(): String {
    return json.encodeToString(BooleanExpressionData.serializer(), this.toData())
}

/**
 * 从 JSON 字符串反序列化布尔表达式
 * Deserialize boolean expression from JSON string
 *
 * @param jsonString JSON 字符串 / JSON string
 * @return 反序列化后的布尔表达式 / Deserialized boolean expression
 */
fun booleanExpressionFromJson(jsonString: String): BooleanExpression {
    val data = json.decodeFromString(BooleanExpressionData.serializer(), jsonString)
    return data.toBooleanExpression()
}

/**
 * 尝试从 JSON 字符串反序列化布尔表达式
 * Try to deserialize boolean expression from JSON string
 *
 * @param jsonString JSON 字符串 / JSON string
 * @return 反序列化后的布尔表达式，失败时返回 null / Deserialized boolean expression, null on failure
 */
fun booleanExpressionFromJsonOrNull(jsonString: String): BooleanExpression? {
    return try {
        booleanExpressionFromJson(jsonString)
    } catch (e: Exception) {
        null
    }
}

/**
 * 将标量表达式序列化为 JSON 字符串
 * Serialize scalar expression to JSON string
 *
 * @return JSON 字符串 / JSON string
 */
fun ScalarExpression<*>.toJsonString(): String {
    return json.encodeToString(ScalarExpressionData.serializer(), this.toData())
}

/**
 * 从 JSON 字符串反序列化标量表达式
 * Deserialize scalar expression from JSON string
 *
 * @param jsonString JSON 字符串 / JSON string
 * @return 反序列化后的标量表达式 / Deserialized scalar expression
 */
fun scalarExpressionFromJson(jsonString: String): ScalarExpression<Any> {
    val data = json.decodeFromString(ScalarExpressionData.serializer(), jsonString)
    return data.toScalarExpression()
}

/**
 * 尝试从 JSON 字符串反序列化标量表达式
 * Try to deserialize scalar expression from JSON string
 *
 * @param jsonString JSON 字符串 / JSON string
 * @return 反序列化后的标量表达式，失败时返回 null / Deserialized scalar expression, null on failure
 */
fun scalarExpressionFromJsonOrNull(jsonString: String): ScalarExpression<Any>? {
    return try {
        scalarExpressionFromJson(jsonString)
    } catch (e: Exception) {
        null
    }
}
