/**
 * OSPF 远程模型序列化器 / OSPF remote model serializer
*/
package fuookami.ospf.kotlin.framework.solver.remote.adapter.ospf

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingModelSnapshot
import fuookami.ospf.kotlin.core.model.constraint_programming.ConstraintProgrammingSnapshotCodec
import fuookami.ospf.kotlin.core.solver.report.ModelElementKind
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.isGeneratedArtifactModelElementId
import fuookami.ospf.kotlin.framework.solver.remote.domain.*

/**
 * OSPF 远程模型序列化器。 / OSPF remote model serializer.
*/
object OspfRemoteModelSerializer {

    /**
     * 序列化线性三元模型。 / Serialize linear triad model.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @return 序列化线性模型 / Serialized linear model
    */
    fun serialize(model: LinearTriadModelView): Ret<SerializedLinearModel> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!validateRemoteModelIdentity(
                variables = model.variables,
                constraints = model.constraints,
                objective = model.objective,
                modelNamespace = model.identityNamespace,
                modelSchemaVersion = model.identitySchemaVersion
            )
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素身份元数据无效，不能序列化远程模型 / Model element identity metadata is invalid; remote serialization is rejected"
            )
        }
        val serializedVariables = model.variables.toSerializedVariables()
        if (serializedVariables.failed) return propagateRemoteSerializationFailure(serializedVariables)
        return ok(SerializedLinearModel(
            name = model.name,
            variables = serializedVariables.value!!,
            constraints = model.constraints.indices.map { rowIndex ->
                SerializedConstraint(
                    cells = model.constraints.lhs[rowIndex].map { cell ->
                        SerializedConstraintCell(
                            rowIndex = cell.rowIndex,
                            colIndex = cell.colIndex,
                            coefficient = cell.coefficient
                        )
                    },
                    sign = model.constraints.signs[rowIndex].toSerializedSign(),
                    rhs = model.constraints.rhs[rowIndex],
                    name = model.constraints.names[rowIndex].ifEmpty { "cons$rowIndex" },
                    identityId = model.constraints.ids.getOrNull(rowIndex)?.value,
                    identityScope = model.constraints.identityScopeAt(rowIndex).toRemoteIdentityScope(),
                    identityOriginKind = model.constraints.identityOriginAt(rowIndex)?.kind,
                    identityOriginKey = model.constraints.identityOriginAt(rowIndex)?.key,
                    identityNamespace = model.constraints.identityNamespace,
                    identitySchemaVersion = model.constraints.identitySchemaVersion,
                    identityProvenance = model.constraints.identityProvenanceAt(rowIndex)
                        .toSerializedIdentityProvenance(model.constraints.identityOriginAt(rowIndex))
                )
            },
            objective = SerializedObjective(
                category = model.objective.category.toSerializedCategory(),
                cells = model.objective.objective.map { cell ->
                    SerializedObjectiveCell(
                        colIndex = cell.colIndex,
                        coefficient = cell.coefficient
                    )
                },
                constant = model.objective.constant,
                identityId = model.objective.id?.value,
                identityScope = model.objective.identityScope.toRemoteIdentityScope(),
                identityOriginKind = model.objective.identityOrigin?.kind,
                identityOriginKey = model.objective.identityOrigin?.key,
                identityNamespace = model.objective.identityNamespace,
                identitySchemaVersion = model.objective.identitySchemaVersion,
                identityProvenance = model.objective.identityProvenance
                    .toSerializedIdentityProvenance(model.objective.identityOrigin)
            ),
            identityNamespace = model.identityNamespace,
            identitySchemaVersion = model.identitySchemaVersion
        ))
    }

    /**
     * 序列化二次四元模型。 / Serialize quadratic tetrad model.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @return 序列化二次模型 / Serialized quadratic model
    */
    fun serialize(model: QuadraticTetradModelView): Ret<SerializedQuadraticModel> {
        when (val validation = model.identityValidation) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }
        if (!validateRemoteModelIdentity(
                variables = model.variables,
                constraints = model.constraints,
                objective = model.objective,
                modelNamespace = model.identityNamespace,
                modelSchemaVersion = model.identitySchemaVersion
            )
        ) {
            return Failed(
                ErrorCode.IllegalArgument,
                "模型元素身份元数据无效，不能序列化远程模型 / Model element identity metadata is invalid; remote serialization is rejected"
            )
        }
        val serializedVariables = model.variables.toSerializedVariables()
        if (serializedVariables.failed) return propagateRemoteSerializationFailure(serializedVariables)
        return ok(SerializedQuadraticModel(
            name = model.name,
            variables = serializedVariables.value!!,
            linearConstraints = model.constraints.indices.map { rowIndex ->
                SerializedConstraint(
                    cells = model.constraints.lhs[rowIndex].filter { it.colIndex2 == null }.map { cell ->
                        SerializedConstraintCell(
                            rowIndex = cell.rowIndex,
                            colIndex = cell.colIndex1,
                            coefficient = cell.coefficient
                        )
                    },
                    sign = model.constraints.signs[rowIndex].toSerializedSign(),
                    rhs = model.constraints.rhs[rowIndex],
                    name = model.constraints.names[rowIndex].ifEmpty { "cons$rowIndex" },
                    identityId = model.constraints.ids.getOrNull(rowIndex)?.value,
                    identityScope = model.constraints.identityScopeAt(rowIndex).toRemoteIdentityScope(),
                    identityOriginKind = model.constraints.identityOriginAt(rowIndex)?.kind,
                    identityOriginKey = model.constraints.identityOriginAt(rowIndex)?.key,
                    identityNamespace = model.constraints.identityNamespace,
                    identitySchemaVersion = model.constraints.identitySchemaVersion,
                    identityProvenance = model.constraints.identityProvenanceAt(rowIndex)
                        .toSerializedIdentityProvenance(model.constraints.identityOriginAt(rowIndex))
                )
            },
            quadraticConstraints = model.constraints.indices.map { rowIndex ->
                SerializedQuadraticConstraint(
                    linearCells = model.constraints.lhs[rowIndex].filter { it.colIndex2 == null }.map { cell ->
                        SerializedConstraintCell(
                            rowIndex = cell.rowIndex,
                            colIndex = cell.colIndex1,
                            coefficient = cell.coefficient
                        )
                    },
                    quadraticCells = model.constraints.lhs[rowIndex].mapNotNull { cell ->
                        cell.colIndex2?.let { colIndex2 ->
                            SerializedQuadraticConstraintCell(
                                rowIndex = cell.rowIndex,
                                colIndex1 = cell.colIndex1,
                                colIndex2 = colIndex2,
                                coefficient = cell.coefficient
                            )
                        }
                    },
                    sign = model.constraints.signs[rowIndex].toSerializedSign(),
                    rhs = model.constraints.rhs[rowIndex],
                    name = model.constraints.names[rowIndex].ifEmpty { "cons$rowIndex" },
                    identityId = model.constraints.ids.getOrNull(rowIndex)?.value,
                    identityScope = model.constraints.identityScopeAt(rowIndex).toRemoteIdentityScope(),
                    identityOriginKind = model.constraints.identityOriginAt(rowIndex)?.kind,
                    identityOriginKey = model.constraints.identityOriginAt(rowIndex)?.key,
                    identityNamespace = model.constraints.identityNamespace,
                    identitySchemaVersion = model.constraints.identitySchemaVersion,
                    identityProvenance = model.constraints.identityProvenanceAt(rowIndex)
                        .toSerializedIdentityProvenance(model.constraints.identityOriginAt(rowIndex))
                )
            },
            objective = SerializedQuadraticObjective(
                category = model.objective.category.toSerializedCategory(),
                linearCells = model.objective.objective.filter { it.colIndex2 == null }.map { cell ->
                    SerializedObjectiveCell(
                        colIndex = cell.colIndex1,
                        coefficient = cell.coefficient
                    )
                },
                quadraticCells = model.objective.objective.mapNotNull { cell ->
                    cell.colIndex2?.let { colIndex2 ->
                        SerializedQuadraticObjectiveCell(
                            colIndex1 = cell.colIndex1,
                            colIndex2 = colIndex2,
                            coefficient = cell.coefficient
                        )
                    }
                },
                constant = model.objective.constant,
                identityId = model.objective.id?.value,
                identityScope = model.objective.identityScope.toRemoteIdentityScope(),
                identityOriginKind = model.objective.identityOrigin?.kind,
                identityOriginKey = model.objective.identityOrigin?.key,
                identityNamespace = model.objective.identityNamespace,
                identitySchemaVersion = model.objective.identitySchemaVersion,
                identityProvenance = model.objective.identityProvenance
                    .toSerializedIdentityProvenance(model.objective.identityOrigin)
            ),
            identityNamespace = model.identityNamespace,
            identitySchemaVersion = model.identitySchemaVersion
        ))
    }

    /**
     * 序列化为模型数据。 / Serialize to model data.
     *
     * @param model 线性三元模型视图 / Linear triad model view
     * @return 远程模型数据 / Remote model data
    */
    fun modelData(model: LinearTriadModelView): Ret<ModelData> {
        return serialize(model).map { ModelData.linear(it) }
    }

    /**
     * 序列化为模型数据。 / Serialize to model data.
     *
     * @param model 二次四元模型视图 / Quadratic tetrad model view
     * @return 远程模型数据 / Remote model data
    */
    fun modelData(model: QuadraticTetradModelView): Ret<ModelData> {
        return serialize(model).map { ModelData.quadratic(it) }
    }

    /**
     * 序列化 CP snapshot 为原始模型数据。 / Serialize a CP snapshot as raw model data.
     *
     * @param snapshot 不含 native 句柄的 CP snapshot / CP snapshot without native handles
     * @return 远程模型数据或结构化错误 / Remote model data or a structured error
    */
    fun modelData(snapshot: ConstraintProgrammingModelSnapshot): Ret<ModelData> {
        return ConstraintProgrammingSnapshotCodec.encode(snapshot)
            .map { encoded ->
                ModelData.raw(
                    bytes = encoded.encodeToByteArray(),
                    format = "ospf-cp-snapshot-json"
                )
            }
    }
}

/**
 * 转换为远程序列化变量。 / Convert to remote serialized variable.
 *
 * @return 序列化变量 / Serialized variable
*/
fun Variable.toSerializedVariable(): Ret<SerializedVariable> {
    if (!isValidRemoteIdentity(
        kind = ModelElementKind.Variable,
        id = id?.value,
        scope = identityScope,
        origin = identityOrigin,
        provenance = identityProvenance,
        namespace = identityNamespace,
        schemaVersion = identitySchemaVersion
    )) {
        return Failed(
            ErrorCode.IllegalArgument,
            "变量身份元数据无效，不能序列化远程变量 / Invalid variable identity metadata; remote variable serialization is rejected"
        )
    }
    return ok(SerializedVariable(
        index = index,
        name = name,
        lowerBound = lowerBound,
        upperBound = upperBound,
        type = when {
            type.isBinaryType -> SerializedVariableType.BINARY
            type.isIntegerType -> SerializedVariableType.INTEGER
            else -> SerializedVariableType.CONTINUOUS
        },
        identityId = id?.value,
        identityScope = identityScope.toRemoteIdentityScope(),
        identityOriginKind = identityOrigin?.kind,
        identityOriginKey = identityOrigin?.key,
        identityNamespace = identityNamespace,
        identitySchemaVersion = identitySchemaVersion,
        identityProvenance = identityProvenance.toSerializedIdentityProvenance(identityOrigin)
    ))
}

private fun Iterable<Variable>.toSerializedVariables(): Ret<List<SerializedVariable>> {
    val serialized = ArrayList<SerializedVariable>()
    for (variable in this) {
        when (val result = variable.toSerializedVariable()) {
            is Ok -> serialized += result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok(serialized)
}

private fun <T> propagateRemoteSerializationFailure(result: Ret<*>): Ret<T> {
    return when (result) {
        is Failed -> Failed(result.error)
        is Fatal -> Fatal(result.errors)
        else -> Failed(
            ErrorCode.ApplicationError,
            "远程模型序列化结果状态无效 / Invalid remote model serialization result state"
        )
    }
}

/** Validate identity metadata before it crosses the remote protocol boundary. / 在远程协议边界前校验身份元数据。 */
private fun isValidRemoteIdentity(
    kind: ModelElementKind,
    id: String?,
    scope: ModelElementScope,
    origin: ModelElementOrigin?,
    provenance: List<ModelElementOrigin>,
    namespace: String?,
    schemaVersion: String?
): Boolean {
    if (id?.isBlank() == true ||
        origin != null && (origin.kind.isBlank() || origin.key.isBlank()) ||
        provenance.any { it.kind.isBlank() || it.key.isBlank() }
    ) {
        return false
    }
    if (origin != null && provenance.isNotEmpty() && origin !in provenance) {
        return false
    }
    val completeProvenance = if (provenance.isEmpty()) {
        listOfNotNull(origin)
    } else {
        provenance
    }
    return when (scope) {
        ModelElementScope.ModelLocal -> {
            origin == null && provenance.isEmpty() && id?.startsWith("artifact:") != true
        }

        ModelElementScope.Stable -> {
                id?.isNotBlank() == true &&
                !id.startsWith("model-local-") &&
                (!id.startsWith("artifact:") || isGeneratedArtifactModelElementId(id, kind)) &&
                !namespace.isNullOrBlank() &&
                !schemaVersion.isNullOrBlank() &&
                completeProvenance.isNotEmpty() &&
                (origin == null || origin in completeProvenance)
        }
    }
}

private fun validateRemoteModelIdentity(
    variables: List<Variable>,
    constraints: ModelConstraint<*>,
    objective: Objective<*>,
    modelNamespace: String?,
    modelSchemaVersion: String?
): Boolean {
    if (constraints.identityMetadataValidation.failed) {
        return false
    }
    val ids = buildList {
        variables.mapNotNullTo(this) { it.id?.value }
        constraints.ids.mapNotNullTo(this) { it.value }
        objective.id?.value?.let(::add)
    }
    if (ids.size != ids.distinct().size) {
        return false
    }
    val elements = buildList {
        variables.forEach { variable ->
            add(variable.identityScope to (variable.identityNamespace to variable.identitySchemaVersion))
        }
        constraints.indices.forEach { index ->
            add(constraints.identityScopeAt(index) to (constraints.identityNamespace to constraints.identitySchemaVersion))
        }
        add(objective.identityScope to (objective.identityNamespace to objective.identitySchemaVersion))
    }
    val namespaces = buildList {
        modelNamespace?.takeIf { it.isNotBlank() }?.let(::add)
        elements.mapNotNullTo(this) { it.second.first?.takeIf { value -> value.isNotBlank() } }
    }.distinct()
    val schemaVersions = buildList {
        modelSchemaVersion?.takeIf { it.isNotBlank() }?.let(::add)
        elements.mapNotNullTo(this) { it.second.second?.takeIf { value -> value.isNotBlank() } }
    }.distinct()
    if (namespaces.size > 1 || schemaVersions.size > 1) {
        return false
    }
    if (elements.any { (scope, metadata) ->
            scope == ModelElementScope.Stable &&
                (modelNamespace.isNullOrBlank() || modelSchemaVersion.isNullOrBlank() ||
                    metadata.first != modelNamespace || metadata.second != modelSchemaVersion)
        }
    ) {
        return false
    }
    return variables.all { variable ->
        isValidRemoteIdentity(
            kind = ModelElementKind.Variable,
            id = variable.id?.value,
            scope = variable.identityScope,
            origin = variable.identityOrigin,
            provenance = variable.identityProvenance,
            namespace = variable.identityNamespace,
            schemaVersion = variable.identitySchemaVersion
        )
    } && constraints.indices.all { index ->
        isValidRemoteIdentity(
            kind = ModelElementKind.Constraint,
            id = constraints.ids.getOrNull(index)?.value,
            scope = constraints.identityScopeAt(index),
            origin = constraints.identityOriginAt(index),
            provenance = constraints.identityProvenanceAt(index),
            namespace = constraints.identityNamespace,
            schemaVersion = constraints.identitySchemaVersion
        )
    } && isValidRemoteIdentity(
        kind = ModelElementKind.Objective,
        id = objective.id?.value,
        scope = objective.identityScope,
        origin = objective.identityOrigin,
        provenance = objective.identityProvenance,
        namespace = objective.identityNamespace,
        schemaVersion = objective.identitySchemaVersion
    )
}

private fun List<ModelElementOrigin>.toSerializedIdentityProvenance(
    primaryOrigin: ModelElementOrigin?
): List<SerializedModelElementOrigin> {
    val effectiveProvenance = if (isEmpty()) listOfNotNull(primaryOrigin) else this
    return effectiveProvenance
        .distinct()
        .sortedWith(compareBy({ it.kind }, { it.key }))
        .map { origin ->
            SerializedModelElementOrigin(
                kind = origin.kind,
                key = origin.key
            )
        }
}

/** Encode the protocol scope without relying on enum-name formatting. /
 * 不依赖枚举名称格式化，显式编码协议 scope。
 */
private fun ModelElementScope.toRemoteIdentityScope(): String {
    return when (this) {
        ModelElementScope.Stable -> "STABLE"
        ModelElementScope.ModelLocal -> "MODEL_LOCAL"
    }
}

/**
 * 转换为远程序列化约束符号。 / Convert to remote serialized constraint sign.
 *
 * @return 序列化约束符号 / Serialized constraint sign
*/
fun ConstraintRelation.toSerializedSign(): SerializedConstraintSign {
    return when (this) {
        ConstraintRelation.LessEqual -> SerializedConstraintSign.LESS_EQUAL
        ConstraintRelation.GreaterEqual -> SerializedConstraintSign.GREATER_EQUAL
        ConstraintRelation.Equal -> SerializedConstraintSign.EQUAL
    }
}

/**
 * 转换为远程序列化目标类型。 / Convert to remote serialized objective category.
 *
 * @return 序列化目标类型 / Serialized objective category
*/
fun ObjectCategory.toSerializedCategory(): SerializedObjectiveCategory {
    return when (this) {
        ObjectCategory.Minimum -> SerializedObjectiveCategory.MINIMIZE
        ObjectCategory.Maximum -> SerializedObjectiveCategory.MAXIMIZE
    }
}
