/** Deterministic identities for generated model elements. / 派生模型元素的确定性身份。 */
package fuookami.ospf.kotlin.core.solver.report

import java.util.Locale

/**
 * Build an identity for an element generated from a source model element. /
 * 为由源模型元素派生出的元素构造身份。
 *
 * The generated ID contains only the semantic role and source ID. It never uses object identity,
 * registration order, display name, or JVM hash code. / 派生 ID 只包含语义角色和源 ID，绝不使用
 * 对象地址、注册顺序、展示名称或 JVM hash code。
 *
 * @param kind 派生元素类别 / Generated element kind
 * @param role 派生元素语义角色 / Semantic role of the generated element
 * @param sourceId 源元素 ID；model-local 元素可传 null / Source element ID; null for a model-local source
 * @param sourceScope 源元素身份作用域 / Source identity scope
 * @param sourceOrigin 源元素来源 / Source origin
 * @param sourceProvenance 源元素的完整来源集合 / Complete provenance of the source element
 * @param namespace 身份命名空间 / Identity namespace
 * @param schemaVersion 身份 schema 版本 / Identity schema version
 * @param discriminator 同一源元素产生多个同角色元素时的确定性区分键 / Deterministic discriminator for multiple elements of one role
 * @return 派生元素身份 / Generated element identity
 */
fun derivedModelElementIdentity(
    kind: ModelElementKind,
    role: String,
    sourceId: String?,
    sourceScope: ModelElementScope,
    sourceOrigin: ModelElementOrigin?,
    sourceProvenance: List<ModelElementOrigin> = emptyList(),
    namespace: String?,
    schemaVersion: String?,
    discriminator: String = "0"
): ModelElementIdentity {
    val sourceKey = sourceId ?: "model-local:$discriminator"
    val safeRole = encodeIdentityRole(role.trim().ifBlank { "generated" })
    val encodedSourceKey = encodeIdentityPart(sourceKey)
    val encodedDiscriminator = encodeIdentityPart(discriminator)
    val sourceClaimsStableIdentity = sourceScope == ModelElementScope.Stable &&
        sourceId != null && !sourceId.startsWith("model-local-") &&
        (!sourceId.startsWith("artifact:") || isGeneratedArtifactModelElementId(sourceId))
    val generatedPrefix = if (sourceClaimsStableIdentity) "artifact" else "model-local-derived"
    val generatedId = ModelElementId(
        "$generatedPrefix:$safeRole:${kind.name.lowercase()}:$encodedSourceKey:$encodedDiscriminator"
    )
    val provenance = if (sourceClaimsStableIdentity) {
        (sourceProvenance + listOfNotNull(sourceOrigin))
            .distinct()
            .sortedWith(compareBy({ it.kind }, { it.key }))
    } else {
        emptyList()
    }
    return ModelElementIdentity(
        kind = kind,
        id = generatedId,
        scope = if (sourceClaimsStableIdentity) ModelElementScope.Stable else ModelElementScope.ModelLocal,
        origin = if (sourceClaimsStableIdentity) sourceOrigin else null,
        provenance = provenance
    )
}

/**
 * Build a deterministic identity for an aggregate generated from multiple source elements. /
 * 为由多个源元素聚合生成的模型元素构造确定性身份。
 *
 * The aggregate is Stable only when every source has a valid Stable identity and complete
 * provenance. The source IDs are length-prefixed before concatenation, so delimiter characters
 * in user IDs cannot make two source sets collide. / 只有每个源元素都具有有效 Stable 身份和完整
 * provenance 时，聚合元素才保持 Stable。源 ID 拼接前使用长度前缀编码，因此用户 ID 中的分隔符
 * 不会导致两个源集合发生碰撞。
 *
 * @param kind 聚合元素类别 / Aggregate element kind
 * @param role 聚合元素语义角色 / Aggregate semantic role
 * @param sourceIdentities 源元素身份列表 / Source element identities
 * @param namespace 身份命名空间 / Identity namespace
 * @param schemaVersion 身份 schema 版本 / Identity schema version
 * @return 聚合身份；源身份不完整时返回 null / Aggregate identity, or null for incomplete sources
 */
internal fun aggregateModelElementIdentity(
    kind: ModelElementKind,
    role: String,
    sourceIdentities: List<ModelElementIdentity>,
    namespace: String?,
    schemaVersion: String?
): ModelElementIdentity? {
    if (sourceIdentities.isEmpty() || sourceIdentities.any { identity ->
            identity.kind != kind ||
                identity.scope != ModelElementScope.Stable ||
                identity.id.value.isBlank() ||
                identity.id.value.startsWith("model-local-") ||
                (identity.id.value.startsWith("artifact:") &&
                    !isGeneratedArtifactModelElementId(identity.id.value, kind)) ||
                (identity.provenance.ifEmpty { listOfNotNull(identity.origin) }).isEmpty()
        }) {
        return null
    }
    val provenance = sourceIdentities
        .flatMap { identity -> identity.provenance.ifEmpty { listOfNotNull(identity.origin) } }
        .distinct()
        .sortedWith(compareBy({ it.kind }, { it.key }))
    if (provenance.isEmpty() || provenance.any { it.kind.isBlank() || it.key.isBlank() }) {
        return null
    }
    val sourceKey = sourceIdentities
        .map { it.id.value }
        .sorted()
        .joinToString(separator = "") { value -> encodeIdentityPart(value) }
    return derivedModelElementIdentity(
        kind = kind,
        role = role,
        sourceId = "aggregate:$sourceKey",
        sourceScope = ModelElementScope.Stable,
        sourceOrigin = null,
        sourceProvenance = provenance,
        namespace = namespace,
        schemaVersion = schemaVersion
    )
}

/**
 * Check whether an artifact ID uses the generated identity grammar. /
 * 校验 artifact ID 是否符合派生身份的生成语法。
 *
 * User-defined stable IDs must not occupy the reserved `artifact:` namespace. The grammar is
 * intentionally checked here so derived-model validation and remote serialization agree on the
 * same boundary. / 用户定义的 stable ID 不得占用保留的 `artifact:` 命名空间；在此统一校验
 * 生成语法，使派生模型校验与远程序列化遵循同一边界。
 *
 * @param id 待校验的模型元素 ID / Model-element ID to validate
 * @param expectedKind 可选的期望元素类别 / Optional expected element kind
 * @return 是否为合法派生 artifact ID / Whether the ID is a valid generated artifact ID
 */
fun isGeneratedArtifactModelElementId(
    id: String,
    expectedKind: ModelElementKind? = null
): Boolean {
    val parts = id.split(':')
    if (parts.size != 5 || parts[0] != "artifact") {
        return false
    }
    val role = parts[1]
    val kind = parts[2]
    if (!isEncodedIdentityRole(role) || !isEncodedIdentityPart(parts[3]) || !isEncodedIdentityPart(parts[4])) {
        return false
    }
    val parsedKind = when (kind) {
        ModelElementKind.Variable.name.lowercase(Locale.ROOT) -> ModelElementKind.Variable
        ModelElementKind.Constraint.name.lowercase(Locale.ROOT) -> ModelElementKind.Constraint
        ModelElementKind.Objective.name.lowercase(Locale.ROOT) -> ModelElementKind.Objective
        else -> return false
    }
    return expectedKind == null || parsedKind == expectedKind
}

private fun encodeIdentityPart(value: String): String {
    val bytes = value.toByteArray(Charsets.UTF_8)
    val hexadecimal = bytes.joinToString(separator = "") { byte ->
        "%02x".format(byte.toInt() and 0xFF)
    }
    return "${bytes.size}h$hexadecimal"
}

private fun encodeIdentityRole(value: String): String {
    if (value.isNotEmpty() && value.first() != '~' && value.all { character ->
            character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
                character == '.' || character == '_' || character == '-'
        }
    ) {
        return value
    }
    return "~${encodeIdentityPart(value)}"
}

private fun isEncodedIdentityRole(value: String): Boolean {
    return if (value.startsWith("~")) {
        isEncodedIdentityPart(value.removePrefix("~"))
    } else {
        value.isNotEmpty() && value.all { character ->
            character in 'A'..'Z' || character in 'a'..'z' || character in '0'..'9' ||
                character == '.' || character == '_' || character == '-'
        }
    }
}

private fun isEncodedIdentityPart(value: String): Boolean {
    val separator = value.indexOf('h')
    if (separator <= 0) {
        return false
    }
    val byteCount = value.substring(0, separator).toIntOrNull() ?: return false
    val hexadecimal = value.substring(separator + 1)
    return hexadecimal.length == byteCount * 2 && hexadecimal.all { character ->
        character in '0'..'9' || character in 'a'..'f'
    }
}
