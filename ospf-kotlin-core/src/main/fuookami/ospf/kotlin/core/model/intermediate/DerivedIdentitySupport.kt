/** Identity helpers for generated intermediate-model elements. / 派生中间模型元素的身份辅助函数。 */
package fuookami.ospf.kotlin.core.model.intermediate

import fuookami.ospf.kotlin.core.model.basic.Variable
import fuookami.ospf.kotlin.core.model.basic.ModelConstraint
import fuookami.ospf.kotlin.core.model.basic.Objective
import fuookami.ospf.kotlin.core.solver.report.ModelElementKind
import fuookami.ospf.kotlin.core.solver.report.ModelElementId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.solver.report.derivedModelElementIdentity
import fuookami.ospf.kotlin.core.solver.report.isGeneratedArtifactModelElementId
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

internal data class DerivedIdentityMetadata(
    val id: ModelElementId,
    val scope: ModelElementScope,
    val origin: ModelElementOrigin?,
    val provenance: List<ModelElementOrigin>
)

internal fun derivedConstraintIdentity(
    role: String,
    sourceId: String?,
    sourceScope: ModelElementScope,
    sourceOrigin: ModelElementOrigin?,
    sourceProvenance: List<ModelElementOrigin> = emptyList(),
    namespace: String?,
    schemaVersion: String?,
    discriminator: String = "0"
): DerivedIdentityMetadata = derivedModelElementIdentity(
    kind = ModelElementKind.Constraint,
    role = role,
    sourceId = sourceId,
    sourceScope = sourceScope,
    sourceOrigin = sourceOrigin,
    sourceProvenance = sourceProvenance,
    namespace = namespace,
    schemaVersion = schemaVersion,
    discriminator = discriminator
).let { DerivedIdentityMetadata(it.id, it.scope, it.origin, it.provenance) }

internal fun derivedObjectiveIdentity(
    role: String,
    sourceId: String?,
    sourceScope: ModelElementScope,
    sourceOrigin: ModelElementOrigin?,
    sourceProvenance: List<ModelElementOrigin> = emptyList(),
    namespace: String?,
    schemaVersion: String?,
    discriminator: String = "0"
): DerivedIdentityMetadata = derivedModelElementIdentity(
    kind = ModelElementKind.Objective,
    role = role,
    sourceId = sourceId,
    sourceScope = sourceScope,
    sourceOrigin = sourceOrigin,
    sourceProvenance = sourceProvenance,
    namespace = namespace,
    schemaVersion = schemaVersion,
    discriminator = discriminator
).let { DerivedIdentityMetadata(it.id, it.scope, it.origin, it.provenance) }

/**
 * Attach a deterministic identity to a generated variable. /
 * 为派生变量附加确定性身份。
 *
 * @param role 派生变量语义角色 / Generated-variable semantic role
 * @param sourceId 源变量或约束 ID / Source variable or constraint ID
 * @param sourceScope 源身份作用域 / Source identity scope
 * @param sourceOrigin 源身份来源 / Source identity origin
 * @param sourceProvenance 源元素完整来源集合 / Complete source-element provenance
 * @param namespace 身份命名空间 / Identity namespace
 * @param schemaVersion 身份 schema 版本 / Identity schema version
 * @param discriminator 同一来源下的确定性区分键 / Deterministic discriminator within one source
 * @return 带身份的变量副本 / Variable copy carrying identity metadata
 */
internal fun Variable.withDerivedIdentity(
    role: String,
    sourceId: String?,
    sourceScope: ModelElementScope,
    sourceOrigin: ModelElementOrigin?,
    sourceProvenance: List<ModelElementOrigin> = emptyList(),
    namespace: String?,
    schemaVersion: String?,
    discriminator: String
): Variable {
    val identity = derivedModelElementIdentity(
        kind = ModelElementKind.Variable,
        role = role,
        sourceId = sourceId,
        sourceScope = sourceScope,
        sourceOrigin = sourceOrigin,
        sourceProvenance = sourceProvenance,
        namespace = namespace,
        schemaVersion = schemaVersion,
        discriminator = discriminator
    )
    return Variable(
        index = index,
        lowerBound = lowerBound,
        upperBound = upperBound,
        type = type,
        origin = origin,
        dualOrigin = dualOrigin,
        slack = slack,
        name = name,
        initialResult = initialResult,
        id = VariableId(identity.id.value),
        identityScope = identity.scope,
        identityOrigin = identity.origin,
        identityNamespace = namespace,
        identitySchemaVersion = schemaVersion,
        identityProvenance = identity.provenance
    )
}

/**
 * Validate identities after a derived model has been materialized. /
 * 在派生模型物化后重新校验身份。
 *
 * The source registry cannot validate generated rows, so this check is deliberately performed
 * against the complete generated variable/constraint/objective set. /
 * 源注册表无法校验新生成的行，因此这里必须针对派生后的变量、约束和目标全集执行校验。
 *
 * @param variables 派生变量 / Derived variables
 * @param constraints 派生约束 / Derived constraints
 * @param objective 派生目标 / Derived objective
 * @return 身份校验结果 / Identity validation result
 */
internal fun validateDerivedIdentitySet(
    variables: List<Variable>,
    constraints: ModelConstraint<*>,
    objective: Objective<*>,
    allowGeneratedArtifactPrefix: Boolean = false
): Try {
    when (val metadataValidation = constraints.identityMetadataValidation) {
        is Failed -> {
            return Failed(metadataValidation.error)
        }
        is Fatal -> {
            return Fatal(metadataValidation.errors)
        }
        else -> {}
    }
    val identities = ArrayList<Pair<String, String>>()
    val elementMetadata = ArrayList<Pair<String?, String?>>()
    fun addIdentity(
        kind: ModelElementKind,
        id: String?,
        scope: ModelElementScope,
        origin: ModelElementOrigin?,
        provenance: List<ModelElementOrigin>,
        namespace: String?,
        schemaVersion: String?
    ): Try {
        val completeProvenance = if (provenance.isEmpty()) {
            listOfNotNull(origin)
        } else {
            provenance.distinct()
        }
        if (scope == ModelElementScope.Stable) {
            if (id == null) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "稳定派生元素必须有 ID / Stable derived elements must have an ID"
                )
            }
            if (namespace.isNullOrBlank() || schemaVersion.isNullOrBlank()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "稳定派生元素必须有 namespace/schema / Stable derived elements must have namespace and schema"
                )
            }
            if (completeProvenance.isEmpty()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "稳定派生元素必须保留真实来源 / Stable derived elements must retain real provenance"
                )
            }
            if (completeProvenance.any { it.kind.isBlank() || it.key.isBlank() }) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "派生元素来源不能为空 / Derived element provenance must not be blank"
                )
            }
            if (origin != null && provenance.isNotEmpty() && origin !in completeProvenance) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "派生元素主来源必须属于 provenance / Derived primary origin must belong to provenance"
                )
            }
        } else if (origin != null || provenance.isNotEmpty()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "model-local 派生元素不能携带稳定来源 / Model-local derived elements must not carry stable provenance"
            )
        }
        elementMetadata += namespace to schemaVersion
        if (id == null) {
            return ok
        }
        if (id.isBlank()) {
            return Failed(
                ErrorCode.IllegalArgument,
                "派生模型元素 ID 不能为空 / Derived model element ID must not be blank"
            )
        }
        if (id.startsWith("artifact:") &&
            (!allowGeneratedArtifactPrefix || !isGeneratedArtifactModelElementId(id, kind))
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素 ID 使用了非法的 artifact 前缀 / Model element ID uses an invalid artifact prefix"
            )
        }
        if (scope == ModelElementScope.Stable && id.startsWith("model-local-")) {
            return Failed(
                ErrorCode.IllegalArgument,
                "稳定派生元素不能使用 model-local ID / Stable derived elements must not use model-local IDs"
            )
        }
        identities += kind.name to id
        return ok
    }

    variables.forEach { variable ->
        if (addIdentity(
                kind = ModelElementKind.Variable,
                id = variable.id?.value,
                scope = variable.identityScope,
                origin = variable.identityOrigin,
                provenance = variable.identityProvenance,
                namespace = variable.identityNamespace,
                schemaVersion = variable.identitySchemaVersion
            ) is Failed
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "派生变量身份无效 / Invalid derived variable identity"
            )
        }
    }
    constraints.indices.forEach { index ->
        if (addIdentity(
                kind = ModelElementKind.Constraint,
                id = constraints.ids.getOrNull(index)?.value,
                scope = constraints.identityScopeAt(index),
                origin = constraints.identityOriginAt(index),
                provenance = constraints.identityProvenanceAt(index),
                namespace = constraints.identityNamespace,
                schemaVersion = constraints.identitySchemaVersion
            ) is Failed
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "派生约束身份无效 / Invalid derived constraint identity"
            )
        }
    }
    if (addIdentity(
            kind = ModelElementKind.Objective,
            id = objective.id?.value,
            scope = objective.identityScope,
            origin = objective.identityOrigin,
            provenance = objective.identityProvenance,
            namespace = objective.identityNamespace,
            schemaVersion = objective.identitySchemaVersion
    ) is Failed
    ) {
        return Failed(
            ErrorCode.IllegalArgument,
            "派生目标身份无效 / Invalid derived objective identity"
        )
    }

    val namespaces = buildList {
        constraints.identityNamespace?.takeIf { it.isNotBlank() }?.let(::add)
        elementMetadata.mapNotNullTo(this) { it.first?.takeIf(String::isNotBlank) }
    }.distinct()
    val schemaVersions = buildList {
        constraints.identitySchemaVersion?.takeIf { it.isNotBlank() }?.let(::add)
        elementMetadata.mapNotNullTo(this) { it.second?.takeIf(String::isNotBlank) }
    }.distinct()
    val modelNamespace = constraints.identityNamespace?.takeIf { it.isNotBlank() }
    val modelSchemaVersion = constraints.identitySchemaVersion?.takeIf { it.isNotBlank() }
    if (namespaces.size > 1 || (modelNamespace != null && namespaces.any { it != modelNamespace })) {
        return Failed(
            ErrorCode.IllegalArgument,
            "派生模型元素 namespace 不一致 / Derived model element namespaces disagree"
        )
    }
    if (schemaVersions.size > 1 || (modelSchemaVersion != null && schemaVersions.any { it != modelSchemaVersion })) {
        return Failed(
            ErrorCode.IllegalArgument,
            "派生模型元素 schema 不一致 / Derived model element schemas disagree"
        )
    }

    val duplicate = identities.groupBy { it.second }.entries.firstOrNull { it.value.size > 1 }
    return if (duplicate == null) {
        ok
    } else {
        Failed(
            ErrorCode.IllegalArgument,
            "派生模型元素 ID 重复：${duplicate.key} / Duplicate derived model element ID: ${duplicate.key}"
        )
    }
}

internal fun LinearTriadModel.withDerivedIdentityValidation(): LinearTriadModel {
    return copy(
        identityValidation = validateDerivedIdentitySet(
            variables = variables,
            constraints = constraints,
            objective = objective,
            allowGeneratedArtifactPrefix = true
        )
    )
}

internal fun QuadraticTetradModel.withDerivedIdentityValidation(): QuadraticTetradModel {
    return copy(
        identityValidation = validateDerivedIdentitySet(
            variables = variables,
            constraints = constraints,
            objective = objective,
            allowGeneratedArtifactPrefix = true
        )
    )
}

/**
 * Combine registry validation with the materialized element validation. /
 * 合并注册表校验与物化模型元素校验。
 *
 * Registry validation covers namespace and duplicate registrations, while the materialized
 * model validation covers identities that were copied or generated after registration. /
 * 注册表校验覆盖 namespace 与注册重复；物化模型校验覆盖注册后复制或派生的元素身份。
 */
internal fun combineIdentityValidation(
    materializedValidation: Try,
    registryValidation: Try?
): Try {
    return when (registryValidation) {
        is Failed -> Failed(registryValidation.error)
        is Fatal -> Fatal(registryValidation.errors)
        else -> materializedValidation
    }
}

internal fun Variable.identityProvenanceOrOrigin(): List<ModelElementOrigin> =
    identityProvenance.ifEmpty { listOfNotNull(identityOrigin) }

internal fun ModelConstraint<*>.identityProvenanceOrOriginAt(index: Int): List<ModelElementOrigin> =
    identityProvenanceAt(index).ifEmpty { listOfNotNull(identityOriginAt(index)) }

internal fun Objective<*>.identityProvenanceOrOrigin(): List<ModelElementOrigin> =
    identityProvenance.ifEmpty { listOfNotNull(identityOrigin) }

internal fun derivedDiscriminator(
    sourceId: String?,
    stableDiscriminator: String,
    modelLocalDiscriminator: String
): String = if (sourceId == null) modelLocalDiscriminator else stableDiscriminator
