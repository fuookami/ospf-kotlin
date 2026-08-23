package fuookami.ospf.kotlin.core.solver.report

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation as ModelConstraintRelation
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.intermediate.LinearTriadModelView
import fuookami.ospf.kotlin.core.model.intermediate.QuadraticTetradModelView

/**
 * 规范化变量及其稳定身份 / Normalized variable and stable identity.
 *
 * @property id 稳定变量标识 / Stable variable identifier
 * @property type 变量类型 / Variable type
 * @property lowerBound 下界编码 / Lower-bound encoding
 * @property upperBound 上界编码 / Upper-bound encoding
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定来源 / Stable origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 元素身份命名空间 / Element identity namespace
 * @property identitySchemaVersion 元素身份 schema / Element identity schema
 */
data class NormalizedVariable(
    val id: VariableId,
    val type: String,
    val lowerBound: String? = null,
    val upperBound: String? = null,
    val scope: ModelElementScope = ModelElementScope.ModelLocal,
    val origin: ModelElementOrigin? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList(),
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null
)

/** 规范化线性项 / Normalized linear term */
data class NormalizedLinearTerm(
    val variableId: VariableId,
    val coefficient: String
)

/** 规范化二次项 / Normalized quadratic term */
data class NormalizedQuadraticTerm(
    val firstVariableId: VariableId,
    val secondVariableId: VariableId,
    val coefficient: String
)

/**
 * 规范化约束及其稳定身份 / Normalized constraint and stable identity.
 *
 * @property id 稳定约束标识 / Stable constraint identifier
 * @property relation 约束关系 / Constraint relation
 * @property rhs 右端项编码 / Right-hand-side encoding
 * @property linearTerms 线性项 / Linear terms
 * @property quadraticTerms 二次项 / Quadratic terms
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定来源 / Stable origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 元素身份命名空间 / Element identity namespace
 * @property identitySchemaVersion 元素身份 schema / Element identity schema
 */
data class NormalizedConstraint(
    val id: ConstraintId,
    val relation: ConstraintRelation,
    val rhs: String,
    val linearTerms: List<NormalizedLinearTerm>,
    val quadraticTerms: List<NormalizedQuadraticTerm> = emptyList(),
    val scope: ModelElementScope = ModelElementScope.ModelLocal,
    val origin: ModelElementOrigin? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList(),
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null
)

/**
 * 规范化目标及其稳定身份 / Normalized objective and stable identity.
 *
 * @property id 稳定目标标识 / Stable objective identifier
 * @property category 目标方向 / Objective category
 * @property constant 常数项编码 / Constant-term encoding
 * @property linearTerms 线性项 / Linear terms
 * @property quadraticTerms 二次项 / Quadratic terms
 * @property scope 身份作用域 / Identity scope
 * @property origin 稳定来源 / Stable origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 元素身份命名空间 / Element identity namespace
 * @property identitySchemaVersion 元素身份 schema / Element identity schema
 */
data class NormalizedObjective(
    val id: ObjectiveId,
    val category: String,
    val constant: String,
    val linearTerms: List<NormalizedLinearTerm>,
    val quadraticTerms: List<NormalizedQuadraticTerm> = emptyList(),
    val scope: ModelElementScope = ModelElementScope.ModelLocal,
    val origin: ModelElementOrigin? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList(),
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null
)

/**
 * 带版本的规范化数学模型。 / Versioned normalized mathematical model.
 *
 * 数值字段必须由 adapter 使用确定的十进制或特殊值编码，不接受 JVM 对象字符串。 / Numeric fields must use deterministic decimal or special-value encoding supplied by the adapter.
 *
 * @property schemaVersion 规范化模型 schema / Normalized-model schema
 * @property modelType 模型类型 / Model type
 * @property variables 规范化变量 / Normalized variables
 * @property constraints 规范化约束 / Normalized constraints
 * @property objective 规范化目标 / Normalized objective
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema / Identity schema
 * @property identityValidation 身份元数据校验结果 / Identity metadata validation result
 */
data class NormalizedMathematicalModel(
    val schemaVersion: String = "1.1",
    val modelType: SolverModelType,
    val variables: List<NormalizedVariable>,
    val constraints: List<NormalizedConstraint>,
    val objective: NormalizedObjective,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityValidation: Try = validateNormalizedIdentityMetadata(
        identityNamespace = identityNamespace,
        identitySchemaVersion = identitySchemaVersion,
        variables = variables,
        constraints = constraints,
        objective = objective
    )
) {
    /** Return the structured identity validation result. / 返回结构化身份校验结果。 */
    fun validateIdentity(): Try = identityValidation

    /** 生成确定性规范文本 / Produce deterministic canonical text */
    fun canonicalText(): String {
        val variableLines = variables.sortedBy { it.id.value }.map { variable ->
            listOf(
                "v",
                encode(variable.id.value),
                encode(variable.type),
                encode(variable.lowerBound ?: ""),
                encode(variable.upperBound ?: ""),
                variable.scope.name,
                encode(variable.origin?.kind ?: ""),
                encode(variable.origin?.key ?: ""),
                provenanceText(variable.identityProvenance, variable.origin),
                encode(variable.identityNamespace ?: ""),
                encode(variable.identitySchemaVersion ?: "")
            ).joinToString("|")
        }
        val constraintLines = constraints.sortedBy { it.id.value }.map { constraint ->
            val linear = constraint.linearTerms.sortedWith(
                compareBy({ it.variableId.value }, { it.coefficient })
            ).joinToString(",") { term ->
                "${encode(term.variableId.value)}:${encode(term.coefficient)}"
            }
            val quadratic = constraint.quadraticTerms.map { it.canonicalized() }.sortedWith(
                compareBy({ it.firstVariableId.value }, { it.secondVariableId.value }, { it.coefficient })
            ).joinToString(",") { term ->
                "${encode(term.firstVariableId.value)}:${encode(term.secondVariableId.value)}:${encode(term.coefficient)}"
            }
            listOf(
                "c",
                encode(constraint.id.value),
                constraint.relation.name,
                encode(constraint.rhs),
                linear,
                quadratic,
                constraint.scope.name,
                encode(constraint.origin?.kind ?: ""),
                encode(constraint.origin?.key ?: ""),
                provenanceText(constraint.identityProvenance, constraint.origin),
                encode(constraint.identityNamespace ?: ""),
                encode(constraint.identitySchemaVersion ?: "")
            ).joinToString("|")
        }
        val objectiveLinear = objective.linearTerms.sortedWith(
            compareBy({ it.variableId.value }, { it.coefficient })
        ).joinToString(",") { term ->
            "${encode(term.variableId.value)}:${encode(term.coefficient)}"
        }
        val objectiveQuadratic = objective.quadraticTerms.map { it.canonicalized() }.sortedWith(
            compareBy({ it.firstVariableId.value }, { it.secondVariableId.value }, { it.coefficient })
        ).joinToString(",") { term ->
            "${encode(term.firstVariableId.value)}:${encode(term.secondVariableId.value)}:${encode(term.coefficient)}"
        }
        val objectiveLine = listOf(
            "o",
            encode(objective.id.value),
            encode(objective.category),
            encode(objective.constant),
            objectiveLinear,
            objectiveQuadratic,
            objective.scope.name,
            encode(objective.origin?.kind ?: ""),
            encode(objective.origin?.key ?: ""),
            provenanceText(objective.identityProvenance, objective.origin),
            encode(objective.identityNamespace ?: ""),
            encode(objective.identitySchemaVersion ?: "")
        ).joinToString("|")
        return buildList {
            add("schema|${encode(schemaVersion)}")
            add("type|${modelType.name}")
            add("identity-namespace|${encode(identityNamespace ?: "")}")
            add("identity-schema|${encode(identitySchemaVersion ?: "")}")
            addAll(variableLines)
            addAll(constraintLines)
            add(objectiveLine)
        }.joinToString("\n")
    }

    /** 生成模型审计指纹 / Produce the model audit fingerprint */
    fun fingerprint(): AuditFingerprint {
        return SolveFingerprinting.sha256(canonicalText(), schemaVersion)
    }

    private fun encode(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("|", "\\|")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }

    private fun provenanceText(
        provenance: List<ModelElementOrigin>,
        origin: ModelElementOrigin?
    ): String {
        return (provenance + listOfNotNull(origin))
            .distinct()
            .sortedWith(compareBy({ it.kind }, { it.key }))
            .joinToString(",") { source ->
                "${encode(source.kind)}:${encode(source.key)}"
            }
    }
}

/**
 * Create a normalized model from a linear triad view. /
 * 从线性三元模型视图创建规范化模型。
 *
 * Unregistered elements intentionally receive model-local identifiers; callers that need
 * cross-rebuild identity must register them before constructing the triad. /
 * 未注册元素有意使用 model-local 标识；需要跨重建身份的调用方必须在构造 triad 前完成注册。
 *
 * @receiver linear triad view / 线性三元模型视图
 * @return normalized linear model / 规范化线性模型
 */
fun LinearTriadModelView.toNormalizedMathematicalModel(): NormalizedMathematicalModel {
    val normalizedVariables = variables.map { variable ->
        NormalizedVariable(
            id = variable.id ?: VariableId("model-local-variable:${variable.index}"),
            type = variable.type::class.qualifiedName ?: variable.type::class.simpleName ?: "unknown",
            lowerBound = variable.lowerBound.toString(),
            upperBound = variable.upperBound.toString(),
            scope = variable.id?.let { variable.identityScope } ?: ModelElementScope.ModelLocal,
            origin = variable.identityOrigin,
            identityProvenance = variable.identityProvenance.ifEmpty {
                listOfNotNull(variable.identityOrigin)
            },
            identityNamespace = variable.identityNamespace,
            identitySchemaVersion = variable.identitySchemaVersion
        )
    }
    val normalizedConstraints = constraints.indices.map { row ->
        NormalizedConstraint(
            id = constraints.ids.getOrNull(row) ?: ConstraintId("model-local-constraint:$row"),
            relation = constraints.signs[row].toReportRelation(),
            rhs = constraints.rhs[row].toString(),
            linearTerms = constraints.lhs[row].map { cell ->
                NormalizedLinearTerm(
                    variableId = variables.getOrNull(cell.colIndex)?.id
                        ?: VariableId("model-local-variable:${cell.colIndex}"),
                    coefficient = cell.coefficient.toString()
                )
            },
            scope = constraints.ids.getOrNull(row)?.let { constraints.identityScopeAt(row) }
                ?: ModelElementScope.ModelLocal,
            origin = constraints.identityOriginAt(row),
            identityProvenance = constraints.identityProvenanceAt(row).ifEmpty {
                listOfNotNull(constraints.identityOriginAt(row))
            },
            identityNamespace = constraints.identityNamespace,
            identitySchemaVersion = constraints.identitySchemaVersion
        )
    }
    val normalizedObjective = NormalizedObjective(
        id = objective.id ?: ObjectiveId("model-local-objective:0"),
        category = objective.category.toNormalizedCategory(),
        constant = objective.constant.toString(),
        linearTerms = objective.objective.map { cell ->
            NormalizedLinearTerm(
                variableId = variables.getOrNull(cell.colIndex)?.id
                    ?: VariableId("model-local-variable:${cell.colIndex}"),
                coefficient = cell.coefficient.toString()
            )
        },
        scope = objective.id?.let { objective.identityScope } ?: ModelElementScope.ModelLocal,
        origin = objective.identityOrigin,
        identityProvenance = objective.identityProvenance.ifEmpty {
            listOfNotNull(objective.identityOrigin)
        },
        identityNamespace = objective.identityNamespace,
        identitySchemaVersion = objective.identitySchemaVersion
    )
    return NormalizedMathematicalModel(
        modelType = if (containsInteger) SolverModelType.MIP else SolverModelType.LP,
        variables = normalizedVariables,
        constraints = normalizedConstraints,
        objective = normalizedObjective,
        identityNamespace = resolveIdentityMetadata(
            preferred = constraints.identityNamespace,
            candidates = variables.map { it.identityNamespace } + listOf(objective.identityNamespace)
        ),
        identitySchemaVersion = resolveIdentityMetadata(
            preferred = constraints.identitySchemaVersion,
            candidates = variables.map { it.identitySchemaVersion } + listOf(objective.identitySchemaVersion)
        )
    ).withSourceIdentityValidation(constraints.identityMetadataValidation)
}

/**
 * Create a normalized model from a quadratic tetrad view. /
 * 从二次四元模型视图创建规范化模型。
 *
 * @receiver quadratic tetrad view / 二次四元模型视图
 * @return normalized quadratic model / 规范化二次模型
 */
fun QuadraticTetradModelView.toNormalizedMathematicalModel(): NormalizedMathematicalModel {
    val normalizedVariables = variables.map { variable ->
        NormalizedVariable(
            id = variable.id ?: VariableId("model-local-variable:${variable.index}"),
            type = variable.type::class.qualifiedName ?: variable.type::class.simpleName ?: "unknown",
            lowerBound = variable.lowerBound.toString(),
            upperBound = variable.upperBound.toString(),
            scope = variable.id?.let { variable.identityScope } ?: ModelElementScope.ModelLocal,
            origin = variable.identityOrigin,
            identityProvenance = variable.identityProvenance.ifEmpty {
                listOfNotNull(variable.identityOrigin)
            },
            identityNamespace = variable.identityNamespace,
            identitySchemaVersion = variable.identitySchemaVersion
        )
    }
    val normalizedConstraints = constraints.indices.map { row ->
        val linearTerms = ArrayList<NormalizedLinearTerm>()
        val quadraticTerms = ArrayList<NormalizedQuadraticTerm>()
        constraints.lhs[row].forEach { cell ->
            val first = variables.getOrNull(cell.colIndex1)?.id
                ?: VariableId("model-local-variable:${cell.colIndex1}")
            if (cell.colIndex2 == null) {
                linearTerms += NormalizedLinearTerm(first, cell.coefficient.toString())
            } else {
                val second = variables.getOrNull(cell.colIndex2)?.id
                    ?: VariableId("model-local-variable:${cell.colIndex2}")
                quadraticTerms += normalizedQuadraticTerm(first, second, cell.coefficient.toString())
            }
        }
        NormalizedConstraint(
            id = constraints.ids.getOrNull(row) ?: ConstraintId("model-local-constraint:$row"),
            relation = constraints.signs[row].toReportRelation(),
            rhs = constraints.rhs[row].toString(),
            linearTerms = linearTerms,
            quadraticTerms = quadraticTerms,
            scope = constraints.ids.getOrNull(row)?.let { constraints.identityScopeAt(row) }
                ?: ModelElementScope.ModelLocal,
            origin = constraints.identityOriginAt(row),
            identityProvenance = constraints.identityProvenanceAt(row).ifEmpty {
                listOfNotNull(constraints.identityOriginAt(row))
            },
            identityNamespace = constraints.identityNamespace,
            identitySchemaVersion = constraints.identitySchemaVersion
        )
    }
    val linearTerms = ArrayList<NormalizedLinearTerm>()
    val quadraticTerms = ArrayList<NormalizedQuadraticTerm>()
    objective.objective.forEach { cell ->
        val first = variables.getOrNull(cell.colIndex1)?.id
            ?: VariableId("model-local-variable:${cell.colIndex1}")
        if (cell.colIndex2 == null) {
            linearTerms += NormalizedLinearTerm(first, cell.coefficient.toString())
        } else {
            val second = variables.getOrNull(cell.colIndex2)?.id
                ?: VariableId("model-local-variable:${cell.colIndex2}")
            quadraticTerms += normalizedQuadraticTerm(first, second, cell.coefficient.toString())
        }
    }
    return NormalizedMathematicalModel(
        modelType = if (constraints.lhs.flatten().any { it.colIndex2 != null }) {
            SolverModelType.QCP
        } else {
            SolverModelType.QP
        },
        variables = normalizedVariables,
        constraints = normalizedConstraints,
        objective = NormalizedObjective(
            id = objective.id ?: ObjectiveId("model-local-objective:0"),
            category = objective.category.toNormalizedCategory(),
            constant = objective.constant.toString(),
            linearTerms = linearTerms,
            quadraticTerms = quadraticTerms,
            scope = objective.id?.let { objective.identityScope } ?: ModelElementScope.ModelLocal,
            origin = objective.identityOrigin,
            identityProvenance = objective.identityProvenance.ifEmpty {
                listOfNotNull(objective.identityOrigin)
            },
            identityNamespace = objective.identityNamespace,
            identitySchemaVersion = objective.identitySchemaVersion
        ),
        identityNamespace = resolveIdentityMetadata(
            preferred = constraints.identityNamespace,
            candidates = variables.map { it.identityNamespace } + listOf(objective.identityNamespace)
        ),
        identitySchemaVersion = resolveIdentityMetadata(
            preferred = constraints.identitySchemaVersion,
            candidates = variables.map { it.identitySchemaVersion } + listOf(objective.identitySchemaVersion)
        )
    ).withSourceIdentityValidation(constraints.identityMetadataValidation)
}

private fun ModelConstraintRelation.toReportRelation(): ConstraintRelation = when (this) {
    ModelConstraintRelation.LessEqual -> ConstraintRelation.LessEqual
    ModelConstraintRelation.Equal -> ConstraintRelation.Equal
    ModelConstraintRelation.GreaterEqual -> ConstraintRelation.GreaterEqual
}

private fun ObjectCategory.toNormalizedCategory(): String = name

private fun resolveIdentityMetadata(
    preferred: String?,
    candidates: List<String?>
): String? {
    val values = buildList {
        preferred?.takeIf { it.isNotBlank() }?.let(::add)
        candidates.mapNotNullTo(this) { it?.takeIf(String::isNotBlank) }
    }.distinct().sorted()
    return values.singleOrNull()
}

private fun NormalizedMathematicalModel.withSourceIdentityValidation(
    sourceValidation: Try
): NormalizedMathematicalModel {
    return when (sourceValidation) {
        is Failed -> copy(identityValidation = Failed(sourceValidation.error))
        is Fatal -> copy(identityValidation = Fatal(sourceValidation.errors))
        else -> this
    }
}

private fun normalizedQuadraticTerm(
    firstVariableId: VariableId,
    secondVariableId: VariableId,
    coefficient: String
): NormalizedQuadraticTerm {
    return if (firstVariableId.value <= secondVariableId.value) {
        NormalizedQuadraticTerm(firstVariableId, secondVariableId, coefficient)
    } else {
        NormalizedQuadraticTerm(secondVariableId, firstVariableId, coefficient)
    }
}

private fun NormalizedQuadraticTerm.canonicalized(): NormalizedQuadraticTerm {
    return normalizedQuadraticTerm(firstVariableId, secondVariableId, coefficient)
}

private fun validateNormalizedIdentityMetadata(
    identityNamespace: String?,
    identitySchemaVersion: String?,
    variables: List<NormalizedVariable>,
    constraints: List<NormalizedConstraint>,
    objective: NormalizedObjective
): Try {
    val namespaces = buildList {
        identityNamespace?.takeIf { it.isNotBlank() }?.let(::add)
        variables.mapNotNullTo(this) { it.identityNamespace?.takeIf(String::isNotBlank) }
        constraints.mapNotNullTo(this) { it.identityNamespace?.takeIf(String::isNotBlank) }
        objective.identityNamespace?.takeIf { it.isNotBlank() }?.let(::add)
    }.distinct()
    if (namespaces.size > 1) {
        return Failed(
            ErrorCode.IllegalArgument,
            "规范化模型 namespace 不一致 / Normalized model namespaces disagree"
        )
    }
    val schemaVersions = buildList {
        identitySchemaVersion?.takeIf { it.isNotBlank() }?.let(::add)
        variables.mapNotNullTo(this) { it.identitySchemaVersion?.takeIf(String::isNotBlank) }
        constraints.mapNotNullTo(this) { it.identitySchemaVersion?.takeIf(String::isNotBlank) }
        objective.identitySchemaVersion?.takeIf { it.isNotBlank() }?.let(::add)
    }.distinct()
    return if (schemaVersions.size > 1) {
        Failed(
            ErrorCode.IllegalArgument,
            "规范化模型 schema 不一致 / Normalized model schemas disagree"
        )
    } else {
        ok
    }
}
