package fuookami.ospf.kotlin.core.solver.report

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.basic.ConstraintRelation as ModelConstraintRelation
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
 * @property name 变量名称 / Variable name
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
    val identitySchemaVersion: String? = null,
    val name: String? = null
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
 * @property name 约束名称 / Constraint name
 * @property source 约束来源 / Constraint source
 * @property priority 约束优先级 / Constraint priority
 * @property lazy 是否延迟约束 / Whether the constraint is lazy
 * @property group 约束组 / Constraint group
 * @property args 约束参数的稳定文本 / Stable text for constraint arguments
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
    val identitySchemaVersion: String? = null,
    val name: String? = null,
    val source: String? = null,
    val priority: Int? = null,
    val lazy: Boolean? = null,
    val group: String? = null,
    val args: String? = null
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
 * @property name 模型名称 / Model name
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
    val name: String? = null,
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
                encode(variable.name ?: ""),
                encode(variable.lowerBound?.let { canonicalNumber(it) } ?: ""),
                encode(variable.upperBound?.let { canonicalNumber(it) } ?: ""),
                variable.scope.name,
                encode(variable.origin?.kind ?: ""),
                encode(variable.origin?.key ?: ""),
                provenanceText(variable.identityProvenance, variable.origin),
                encode(variable.identityNamespace ?: ""),
                encode(variable.identitySchemaVersion ?: "")
            ).joinToString("|")
        }
        val constraintLines = constraints.sortedBy { it.id.value }.map { constraint ->
            val linear = canonicalLinearTerms(constraint.linearTerms).joinToString(",") { term ->
                "${encode(term.variableId.value)}:${encode(term.coefficient)}"
            }
            val quadratic = canonicalQuadraticTerms(constraint.quadraticTerms).joinToString(",") { term ->
                "${encode(term.firstVariableId.value)}:${encode(term.secondVariableId.value)}:${encode(term.coefficient)}"
            }
            listOf(
                "c",
                encode(constraint.id.value),
                encode(constraint.name ?: ""),
                constraint.source ?: "",
                constraint.relation.name,
                encode(canonicalNumber(constraint.rhs)),
                linear,
                quadratic,
                constraint.scope.name,
                encode(constraint.origin?.kind ?: ""),
                encode(constraint.origin?.key ?: ""),
                provenanceText(constraint.identityProvenance, constraint.origin),
                encode(constraint.identityNamespace ?: ""),
                encode(constraint.identitySchemaVersion ?: ""),
                constraint.priority?.toString() ?: "",
                constraint.lazy?.toString() ?: "",
                encode(constraint.group ?: ""),
                encode(constraint.args ?: "")
            ).joinToString("|")
        }
        val objectiveLinear = canonicalLinearTerms(objective.linearTerms).joinToString(",") { term ->
            "${encode(term.variableId.value)}:${encode(term.coefficient)}"
        }
        val objectiveQuadratic = canonicalQuadraticTerms(objective.quadraticTerms).joinToString(",") { term ->
            "${encode(term.firstVariableId.value)}:${encode(term.secondVariableId.value)}:${encode(term.coefficient)}"
        }
        val objectiveLine = listOf(
            "o",
            encode(objective.id.value),
            encode(objective.category),
            encode(canonicalNumber(objective.constant)),
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
            add("name|${encode(name ?: "")}")
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

    /**
     * 校验模型中的所有数值都是有限值。 / Validate that every numeric value in the model is finite.
     *
     * 语义与 Rust `validate_linear_model_for_backend` 对齐：
     * - **NaN 一律拒绝**（任何位置）。
     * - **无穷边界是合法的**，但方向必须正确：下界仅允许 `-∞`，上界仅允许 `+∞`；反向无穷
     *   会被拒绝。无穷表示"该侧无约束"，因此不能当作脏数据。
     * - **系数、右端项、目标常数必须有限**（无穷会直接破坏求解与指纹）。
     *
     * 之所以需要这道校验：若不拦截，`NaN` 会以 `"NaN"` 文本进入指纹，使脏数据看起来像一次
     * 正常求解；数学上无意义的 `+∞` 下界则会静默产生不可解模型。
     *
     * Semantics align with Rust's `validate_linear_model_for_backend`:
     * - **NaN is always rejected**, anywhere.
     * - **Infinite bounds are legitimate** but must point the right way: a lower bound may only
     *   be `-∞` and an upper bound only `+∞`; a reversed infinity is rejected. Infinity means
     *   "unbounded on that side", so it is not corrupt data.
     * - **Coefficients, right-hand sides, and the objective constant must be finite**
     *   (infinity would break both solving and the fingerprint).
     *
     * The check exists because otherwise `NaN` enters the fingerprint as the text `"NaN"`,
     * making corrupt data look like an ordinary run, while a mathematically meaningless `+∞`
     * lower bound would silently yield an unsolvable model.
     *
     * @return 成功或带定位信息的失败 / Success, or a failure identifying the offending element
     */
    fun validateFiniteValues(): Try {
        /** 系数/右端项/常数：必须有限。/ Coefficients, right-hand sides, constants: must be finite. */
        fun requireFinite(value: String?, context: String): Try {
            val parsed = value?.toDoubleOrNull() ?: return ok
            return if (parsed.isFinite()) {
                ok
            } else {
                Failed(
                    ErrorCode.IllegalArgument,
                    "规范化模型包含非有限数值：$context（$value）/ " +
                            "normalized model contains a non-finite value: $context ($value)"
                )
            }
        }

        /**
         * 边界：拒绝 NaN 与方向错误的无穷。 / Bounds: reject NaN and wrongly directed infinity.
         *
         * @param lower 是否为下界 / Whether this is a lower bound
         */
        fun requireValidBound(value: String?, lower: Boolean, context: String): Try {
            val parsed = value?.toDoubleOrNull() ?: return ok
            if (parsed.isNaN()) {
                return Failed(
                    ErrorCode.IllegalArgument,
                    "规范化模型变量边界为 NaN：$context / " +
                            "normalized model variable bound is NaN: $context"
                )
            }
            if (parsed.isInfinite()) {
                val valid = if (lower) parsed < 0.0 else parsed > 0.0
                if (!valid) {
                    return Failed(
                        ErrorCode.IllegalArgument,
                        "规范化模型变量边界无穷方向错误：$context（$value）/ " +
                                "normalized model variable bound has the wrong infinity direction: " +
                                "$context ($value)"
                    )
                }
            }
            return ok
        }

        for (variable in variables) {
            val lower = requireValidBound(
                variable.lowerBound,
                true,
                "变量 ${variable.id.value} 下界 / variable lower bound"
            )
            if (lower is Failed) return lower
            val upper = requireValidBound(
                variable.upperBound,
                false,
                "变量 ${variable.id.value} 上界 / variable upper bound"
            )
            if (upper is Failed) return upper
        }
        for (constraint in constraints) {
            val rhs = requireFinite(constraint.rhs, "约束 ${constraint.id.value} 右端项 / constraint right-hand side")
            if (rhs is Failed) return rhs
            for (term in constraint.linearTerms) {
                val coefficient = requireFinite(
                    term.coefficient,
                    "约束 ${constraint.id.value} 线性系数 / linear coefficient"
                )
                if (coefficient is Failed) return coefficient
            }
            for (term in constraint.quadraticTerms) {
                val coefficient = requireFinite(
                    term.coefficient,
                    "约束 ${constraint.id.value} 二次系数 / quadratic coefficient"
                )
                if (coefficient is Failed) return coefficient
            }
        }
        val constant = requireFinite(objective.constant, "目标常数 / objective constant")
        if (constant is Failed) return constant
        for (term in objective.linearTerms) {
            val coefficient = requireFinite(term.coefficient, "目标线性系数 / objective linear coefficient")
            if (coefficient is Failed) return coefficient
        }
        for (term in objective.quadraticTerms) {
            val coefficient = requireFinite(term.coefficient, "目标二次系数 / objective quadratic coefficient")
            if (coefficient is Failed) return coefficient
        }
        return ok
    }

    private fun encode(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\n", "\\n")
            .replace("|", "\\|")
            .replace(",", "\\,")
            .replace(":", "\\:")
    }

    /**
     * 归一化数值文本。 / Normalize a numeric text.
     *
     * 有符号零统一为 `0.0`，其余保持 JVM 的确定性十进制表示。指纹必须只反映模型的数学
     * 内容，而 `-0.0` 与 `0.0` 数值相等；若不归一化，语义等价的模型会得到不同指纹，进而
     * 破坏缓存复用与 checkpoint 一致性校验。
     *
     * Signed zero is canonicalized to `0.0`; everything else keeps the JVM's deterministic
     * decimal representation. A fingerprint must reflect only the model's mathematical
     * content, and `-0.0` equals `0.0` numerically; without this normalization, semantically
     * equivalent models would fingerprint differently, breaking cache reuse and checkpoint
     * consistency checks.
     */
    private fun canonicalNumber(value: String): String {
        val parsed = value.toBigDecimalOrNull() ?: return value
        return if (parsed.signum() == 0) "0.0" else parsed.stripTrailingZeros().toPlainString()
    }

    /**
     * 解析为十进制数。 / Parse as a decimal number.
     *
     * 使用 [java.math.BigDecimal] 而非 `Double`：core 层禁止 `.toDouble()`（见
     * `CoreToDoubleConversionGuardTest`），且十进制精确求和才能保证 `(0,1)+(0,2)` 与 `(0,3)`
     * 之类的等价模型得到完全相同的系数文本。
     *
     * [java.math.BigDecimal] is used rather than `Double`: the core layer forbids `.toDouble()`
     * (see `CoreToDoubleConversionGuardTest`), and only exact decimal addition guarantees that
     * equivalent models such as `(0,1)+(0,2)` and `(0,3)` produce identical coefficient text.
     */
    private fun decimalOrNull(value: String): java.math.BigDecimal? = value.toBigDecimalOrNull()

    /**
     * 合并同变量的线性项并剔除零系数项。 / Merge linear terms per variable and drop zero coefficients.
     *
     * 只有全部系数都能解析时才合并；否则退化为"仅归一化 + 排序"，避免把无法解析的编码
     * 误合并成错误结果。
     *
     * Terms are merged only when every coefficient parses; otherwise the result degrades to
     * "normalize and sort", so unparseable encodings are never merged into a wrong value.
     */
    private fun canonicalLinearTerms(terms: List<NormalizedLinearTerm>): List<NormalizedLinearTerm> {
        val parsed = terms.map { it.variableId.value to decimalOrNull(it.coefficient) }
        if (parsed.any { it.second == null }) {
            return terms
                .map { NormalizedLinearTerm(it.variableId, canonicalNumber(it.coefficient)) }
                .sortedWith(compareBy({ it.variableId.value }, { it.coefficient }))
        }
        val merged = LinkedHashMap<String, java.math.BigDecimal>()
        for ((id, coefficient) in parsed) {
            val current = merged[id] ?: java.math.BigDecimal.ZERO
            merged[id] = current.add(coefficient!!)
        }
        return merged.entries
            .map { (id, coefficient) -> NormalizedLinearTerm(VariableId(id), canonicalNumber(coefficient.toPlainString())) }
            .filter { decimalOrNull(it.coefficient)?.signum() != 0 }
            .sortedWith(compareBy({ it.variableId.value }, { it.coefficient }))
    }

    /**
     * 合并同变量对的二次项并剔除零系数项，变量对先做规范排序。
     * Merge quadratic terms per variable pair and drop zero coefficients, canonicalizing the
     * pair order first.
     */
    private fun canonicalQuadraticTerms(terms: List<NormalizedQuadraticTerm>): List<NormalizedQuadraticTerm> {
        val canonicalized = terms.map { it.canonicalized() }
        val parsed = canonicalized.map {
            Triple(it.firstVariableId.value, it.secondVariableId.value, decimalOrNull(it.coefficient))
        }
        if (parsed.any { it.third == null }) {
            return canonicalized
                .map {
                    NormalizedQuadraticTerm(
                        it.firstVariableId,
                        it.secondVariableId,
                        canonicalNumber(it.coefficient)
                    )
                }
                .sortedWith(
                    compareBy(
                        { it.firstVariableId.value },
                        { it.secondVariableId.value },
                        { it.coefficient }
                    )
                )
        }
        val merged = LinkedHashMap<Pair<String, String>, java.math.BigDecimal>()
        for ((first, second, coefficient) in parsed) {
            val key = first to second
            val current = merged[key] ?: java.math.BigDecimal.ZERO
            merged[key] = current.add(coefficient!!)
        }
        return merged.entries
            .map { (key, coefficient) ->
                NormalizedQuadraticTerm(
                    VariableId(key.first),
                    VariableId(key.second),
                    canonicalNumber(coefficient.toPlainString())
                )
            }
            .filter { decimalOrNull(it.coefficient)?.signum() != 0 }
            .sortedWith(
                compareBy(
                    { it.firstVariableId.value },
                    { it.secondVariableId.value },
                    { it.coefficient }
                )
            )
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
            name = variable.name,
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
            name = constraints.names.getOrNull(row),
            source = constraints.sources.getOrNull(row)?.name,
            priority = constraints.priorities.getOrNull(row),
            lazy = constraints.origins.getOrNull(row)?.lazy,
            group = constraints.origins.getOrNull(row)?.origin?.group?.name,
            args = stableArgsText(constraints.origins.getOrNull(row)?.origin?.args),
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
        ),
        name = name
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
            name = variable.name,
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
            name = constraints.names.getOrNull(row),
            source = constraints.sources.getOrNull(row)?.name,
            priority = constraints.priorities.getOrNull(row),
            lazy = constraints.origins.getOrNull(row)?.lazy,
            group = constraints.origins.getOrNull(row)?.origin?.group?.name,
            args = stableArgsText(constraints.origins.getOrNull(row)?.origin?.args),
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
        ),
        name = name
    ).withSourceIdentityValidation(constraints.identityMetadataValidation)
}

/**
 * 为任意 `args` 生成**确定性**文本。 / Produce **deterministic** text for an arbitrary `args`.
 *
 * 约束的 `args` 是 `Any?`，可以承载任意对象。直接 `toString()` 对未覆写该方法的类型会落到
 * `Object.toString()`，其身份哈希**每次运行都不同**，把这种字符串写进指纹会让同一模型在两次运行
 * 间得到不同指纹，从而破坏缓存复用与一致性校验——比不纳入指纹更糟。
 *
 * 因此只接受具有稳定表示的类型：字面量与 `enum` 直接用其 `toString()`，`data class` 用编译器生成
 * 的结构化 `toString()`，其余类型只记录**限定类名**（稳定，且足以表达"该约束带有某种参数"这一事实，
 * 同时不引入身份哈希）。
 *
 * A constraint's `args` is `Any?` and may hold any object. A bare `toString()` falls back to
 * `Object.toString()` for types that do not override it, whose identity hash **differs on every run**;
 * writing such a string into the fingerprint would give one model different fingerprints across runs
 * and break cache reuse and consistency checks — worse than omitting the field.
 *
 * Only types with a stable representation are therefore accepted: literals and `enum`s use their
 * `toString()`, `data class`es use the compiler-generated structural `toString()`, and every other type
 * contributes only its **qualified class name** (stable, and enough to record that the constraint
 * carries some argument, without introducing an identity hash).
 *
 * @param args 约束参数 / Constraint arguments
 * @return 确定性文本 / Deterministic text
 */
private fun stableArgsText(args: Any?): String? = when (args) {
    null -> null
    is String, is Number, is Boolean, is Char, is Enum<*> -> args.toString()
    else -> if (args::class.isData) args.toString() else args::class.qualifiedName ?: "unknown"
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
