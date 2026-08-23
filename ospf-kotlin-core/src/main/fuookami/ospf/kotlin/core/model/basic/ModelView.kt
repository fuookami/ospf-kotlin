/**
 * 模型视图 / Model view
*/
package fuookami.ospf.kotlin.core.model.basic

import java.io.*
import java.nio.file.Path
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.solver.report.ConstraintId
import fuookami.ospf.kotlin.core.solver.report.ModelElementOrigin
import fuookami.ospf.kotlin.core.solver.report.ModelElementScope
import fuookami.ospf.kotlin.core.solver.report.ObjectiveId
import fuookami.ospf.kotlin.core.solver.report.VariableId
import fuookami.ospf.kotlin.core.variable.*

/**
 * 变量松弛信息，关联变量与其约束或界限。 / Variable slack information associating a variable with its constraint or bounds.
 *
 * @property constraint 关联的约束（可为 null） / The associated constraint (nullable)
 * @property lowerBound 下界变量（可为 null） / Lower bound variable (nullable)
 * @property upperBound 上界变量（可为 null） / Upper bound variable (nullable)
*/
data class VariableSlack(
    val constraint: Constraint<Flt64, *>? = null,
    val lowerBound: Variable? = null,
    val upperBound: Variable? = null
)

/**
 * 求解器中的变量，封装变量的界限、类型和初始值等属性。 / Variable in the solver, encapsulating bounds, type, and initial value properties.
 *
 * @property index         变量在求解器中的索引 / Variable index in the solver
 * @param    lowerBound    下界 / Lower bound
 * @param    upperBound    上界 / Upper bound
 * @param    type          变量类型 / Variable type
 * @property origin        原始抽象变量项 / The originating abstract variable item
 * @property dualOrigin    对偶约束来源 / The dual constraint origin
 * @property slack         松弛信息 / Slack information
 * @property name          变量名 / Variable name
 * @property initialResult 初始值（可为 null） / Initial value (nullable)
 * @property id            稳定变量 ID；为空时为 model-local / Stable variable ID; null means model-local
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOrigin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema 版本 / Identity schema version
*/
class Variable(
    val index: Int,
    lowerBound: Flt64,
    upperBound: Flt64,
    type: VariableType<*>,
    val origin: AbstractVariableItem<*, *>?,
    val dualOrigin: Constraint<Flt64, *>? = null,
    val slack: VariableSlack? = null,
    val name: String,
    val initialResult: Flt64? = null,
    val id: VariableId? = null,
    val identityScope: ModelElementScope = ModelElementScope.ModelLocal,
    val identityOrigin: ModelElementOrigin? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
) : Cloneable, Copyable<Variable> {
    internal var _lowerBound = lowerBound
    internal var _upperBound = upperBound
    internal var _type = type

    val lowerBound by ::_lowerBound
    val upperBound by ::_upperBound
    val type by ::_type

    /** 是否无界（正负方向均自由） / Whether the variable is free (unbounded in both directions) */
    val free: Boolean
        get() {
            return negativeFree && positiveFree
        }

    /** 是否已归一化（正方向或负方向归一化） / Whether the variable is normalized (in either direction) */
    val normalized: Boolean
        get() {
            return negativeNormalized || positiveNormalized
        }

    /** 负方向是否归一化（负自由且上界为零） / Whether negative-normalized (negative-free and upper bound is zero) */
    val negativeNormalized: Boolean
        get() {
            return negativeFree && upperBound eq Flt64.zero
        }

    /** 负方向是否自由（下界为负无穷） / Whether negative-free (lower bound is negative infinity) */
    val negativeFree: Boolean
        get() {
            return (lowerBound eq Flt64.negativeInfinity || lowerBound leq -Flt64.decimalPrecision.reciprocal())
        }

    /** 正方向是否归一化（正自由且下界为零） / Whether positive-normalized (positive-free and lower bound is zero) */
    val positiveNormalized: Boolean
        get() {
            return positiveFree && lowerBound eq Flt64.zero
        }

    /** 正方向是否自由（上界为正无穷） / Whether positive-free (upper bound is positive infinity) */
    val positiveFree: Boolean
        get() {
            return (upperBound eq Flt64.infinity || upperBound geq Flt64.decimalPrecision.reciprocal())
        }

    override fun copy() = Variable(
        index = index,
        lowerBound = lowerBound,
        upperBound = upperBound,
        type = type,
        origin = origin,
        dualOrigin = dualOrigin,
        slack = slack,
        name = name,
        initialResult = initialResult,
        id = id,
        identityScope = identityScope,
        identityOrigin = identityOrigin,
        identityNamespace = identityNamespace,
        identitySchemaVersion = identitySchemaVersion,
        identityProvenance = identityProvenance
    )
    override fun clone() = copy()

    override fun toString() = name
}

/**
 * 模型单元格接口，持有系数值。 / Model cell interface holding a coefficient value.
*/
interface ModelCell<Self : ModelCell<Self>> {
    val coefficient: Flt64

    /** 返回系数取反后的新单元格 / Return a new cell with the coefficient negated */
    operator fun unaryMinus(): Self
}

/**
 * 约束单元格接口，扩展 ModelCell 增加行索引。 / Constraint cell interface extending ModelCell with a row index.
*/
interface ConstraintCell<Self : ConstraintCell<Self>> : ModelCell<Self> {
    val rowIndex: Int
}

/**
 * 约束来源枚举，标识约束的产生途径。 / Constraint source enumeration indicating how a constraint was produced.
*/
enum class ConstraintSource {
    /** 原始约束 / Original constraint */
    Origin,
    /** 下界约束 / Lower bound constraint */
    LowerBound,
    /** 上界约束 / Upper bound constraint */
    UpperBound,
    /** 对偶约束 / Dual constraint */
    Dual,
    /** Farkas 对偶约束 / Farkas dual constraint */
    FarkasDual,
    /** 可行性约束 / Feasibility constraint */
    Feasibility,
    /** 弹性约束 / Elastic constraint */
    Elastic,
    /** 弹性下界约束 / Elastic lower bound constraint */
    ElasticLowerBound,
    /** 弹性上界约束 / Elastic upper bound constraint */
    ElasticUpperBound,
    /** 弹性松弛二元约束 / Elastic slack binary constraint */
    ElasticSlackBinary,
    /** 弹性松弛最小最大约束 / Elastic slack minmax constraint */
    ElasticSlackMinmax
}

/**
 * 模型约束的抽象基类，管理约束的左端、符号、右端、名称和来源。 / Abstract base class for model constraints, managing LHS, signs, RHS, names, and sources.
 *
 * @property constraintCount 约束数量 / Number of constraints
 * @param    signs           约束关系列表 / List of constraint relations
 * @param    rhs             右端值列表 / List of right-hand side values
 * @param    names           约束名称列表 / List of constraint names
 * @param    sources         约束来源列表 / List of constraint sources
 * @param    ids             稳定约束 ID 列表 / List of stable constraint IDs
 * @param    identityScopes  每行身份作用域 / Identity scope for each row
 * @param    identityOrigins 每行稳定身份来源 / Stable identity origin for each row
 * @param    identityProvenance 每行完整身份来源集合 / Complete identity provenance for each row
 * @param    identityMetadataValidationOverride 复制或过滤时保留的身份元数据校验结果 / Identity metadata validation result preserved across copies or filters
 * @property ids             稳定约束 ID 列表 / Stable constraint ID list
 * @property identityScopes 每行身份作用域 / Identity scope for each row
 * @property identityOrigins 每行稳定身份来源 / Stable identity origin for each row
 * @property identityProvenance 每行完整身份来源集合 / Complete identity provenance for each row
*/
abstract class ModelConstraint<ConCell>(
    val constraintCount: Int,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>,
    ids: List<ConstraintId> = emptyList(),
    identityNamespace: String? = null,
    identitySchemaVersion: String? = null,
    identityScopes: List<ModelElementScope> = emptyList(),
    identityOrigins: List<ModelElementOrigin?> = emptyList(),
    identityProvenance: List<List<ModelElementOrigin>> = emptyList(),
    identityMetadataValidationOverride: Try? = null
) : Cloneable, Copyable<ModelConstraint<ConCell>>, AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    internal val _signs = signs.toMutableList()
    internal val _rhs = rhs.toMutableList()
    internal val _names = names.toMutableList()
    internal val _sources = sources.toMutableList()
    internal val _ids = ids.toMutableList()
    private val idsInput = ids
    private val identityScopesInput = identityScopes
    private val identityOriginsInput = identityOrigins
    private val identityProvenanceInput = identityProvenance

    abstract val lhs: List<List<ConCell>>
    val signs: List<ConstraintRelation> by ::_signs
    val rhs: List<Flt64> by ::_rhs
    val names: List<String> by ::_names
    val sources: List<ConstraintSource> by ::_sources
    /** Stable row IDs; an empty list keeps the model-local fallback active. / 稳定行 ID；为空时继续使用 model-local fallback。 */
    val ids: List<ConstraintId> by ::_ids

    /** Identity namespace carried by the originating registry. / 来源注册表携带的身份命名空间。 */
    var identityNamespace: String? = identityNamespace

    /** Identity schema version carried by the originating registry. / 来源注册表携带的身份 schema 版本。 */
    var identitySchemaVersion: String? = identitySchemaVersion

    private val _identityScopes = when {
        identityScopes.isEmpty() -> MutableList(constraintCount) { ModelElementScope.ModelLocal }
        identityScopes.size == constraintCount -> identityScopes.toMutableList()
        else -> identityScopes.toMutableList()
    }

    private val _identityOrigins = when {
        identityOrigins.isEmpty() -> MutableList<ModelElementOrigin?>(constraintCount) { null }
        identityOrigins.size == constraintCount -> identityOrigins.toMutableList()
        else -> identityOrigins.toMutableList()
    }

    /** Per-row identity scopes. / 每行身份作用域。 */
    val identityScopes: List<ModelElementScope> by ::_identityScopes

    /** Per-row stable identity origins. / 每行稳定身份来源。 */
    val identityOrigins: List<ModelElementOrigin?> by ::_identityOrigins

    private val _identityProvenance = when {
        identityProvenance.isEmpty() -> MutableList(constraintCount) { index ->
            listOfNotNull(_identityOrigins.getOrNull(index))
        }
        identityProvenance.size == constraintCount -> identityProvenance.map { it.toList() }.toMutableList()
        else -> identityProvenance.map { it.toList() }.toMutableList()
    }

    /** Per-row complete identity provenance. / 每行完整身份来源集合。 */
    val identityProvenance: List<List<ModelElementOrigin>> by ::_identityProvenance

    /**
     * Validate identity metadata list shapes before any fallback is consumed. /
     * 在任何回退值被消费前校验身份元数据列表形状。
     *
     * An empty list means that the corresponding metadata is intentionally model-local and
     * remains a valid compatibility input. A non-empty list must describe every row; otherwise
     * returning row-level fallback values would silently discard identity evidence. /
     * 空列表表示调用方明确使用 model-local 兼容默认值，仍然有效；非空列表必须覆盖每一行，
     * 否则返回按行回退值会静默丢弃身份证据。
     */
    val identityMetadataValidation: Try = identityMetadataValidationOverride ?: when {
        idsInput.isNotEmpty() && idsInput.size != constraintCount -> Failed(
            ErrorCode.IllegalArgument,
            "约束 ID 列表长度与约束数不一致 / Constraint ID list length disagrees with constraint count"
        )
        identityScopesInput.isNotEmpty() && identityScopesInput.size != constraintCount -> Failed(
            ErrorCode.IllegalArgument,
            "约束身份 scope 列表长度与约束数不一致 / Constraint identity scope list length disagrees with constraint count"
        )
        identityOriginsInput.isNotEmpty() && identityOriginsInput.size != constraintCount -> Failed(
            ErrorCode.IllegalArgument,
            "约束身份 origin 列表长度与约束数不一致 / Constraint identity origin list length disagrees with constraint count"
        )
        identityProvenanceInput.isNotEmpty() && identityProvenanceInput.size != constraintCount -> Failed(
            ErrorCode.IllegalArgument,
            "约束身份 provenance 列表长度与约束数不一致 / Constraint identity provenance list length disagrees with constraint count"
        )
        else -> ok
    }

    /** Return a row identity scope with model-local fallback. / 返回行身份作用域，缺失时回退为 model-local。
     *
     * @param index constraint row index / 约束行索引
     * @return row identity scope / 行身份作用域
     */
    fun identityScopeAt(index: Int): ModelElementScope =
        identityScopes.getOrNull(index) ?: ModelElementScope.ModelLocal

    /** Return a row identity origin when one was registered. / 返回已注册的行身份来源。
     *
     * @param index constraint row index / 约束行索引
     * @return row identity origin or null / 行身份来源或 null
     */
    fun identityOriginAt(index: Int): ModelElementOrigin? = identityOrigins.getOrNull(index)

    /** Return all source origins for a row. / 返回约束行的全部来源身份。
     *
     * @param index constraint row index / 约束行索引
     * @return row provenance / 行来源集合
     */
    fun identityProvenanceAt(index: Int): List<ModelElementOrigin> =
        identityProvenance.getOrNull(index).orEmpty()

    val size: Int get() = rhs.size
    val indices: IntRange get() = rhs.indices

    override fun clone() = copy()

    override fun close() {
        _signs.clear()
        _rhs.clear()
        _names.clear()
        _sources.clear()
        _ids.clear()
        _identityScopes.clear()
        _identityOrigins.clear()
        _identityProvenance.clear()
    }
}

/**
 * 目标函数，包含优化方向、目标单元格列表和常数项。 / Objective function containing optimization direction, objective cell list, and constant.
 *
 * @property category  优化方向 / Optimization direction
 * @property objective 目标单元格列表 / List of objective cells
 * @property constant  常数项 / Constant term
 * @property id        稳定目标 ID；为空时为 model-local / Stable objective ID; null means model-local
 * @property identityScope 身份作用域 / Identity scope
 * @property identityOrigin 稳定身份来源 / Stable identity origin
 * @property identityProvenance 完整身份来源集合 / Complete identity provenance
 * @property identityNamespace 身份命名空间 / Identity namespace
 * @property identitySchemaVersion 身份 schema 版本 / Identity schema version
*/
class Objective<C : Copyable<C>>(
    val category: ObjectCategory,
    val objective: List<C>,
    val constant: Flt64 = Flt64(0.0),
    val id: ObjectiveId? = null,
    val identityScope: ModelElementScope = ModelElementScope.ModelLocal,
    val identityOrigin: ModelElementOrigin? = null,
    val identityNamespace: String? = null,
    val identitySchemaVersion: String? = null,
    val identityProvenance: List<ModelElementOrigin> = emptyList()
) : Cloneable, Copyable<Objective<C>> {
    override fun copy() = Objective(
        category = category,
        objective = objective.toList(),
        constant = constant,
        id = id,
        identityScope = identityScope,
        identityOrigin = identityOrigin,
        identityNamespace = identityNamespace,
        identitySchemaVersion = identitySchemaVersion,
        identityProvenance = identityProvenance
    )
    override fun clone() = copy()
}

/**
 * 基本模型视图接口，提供变量、约束、名称及导出能力。 / Basic model view interface providing variables, constraints, name, and export capability.
*/
interface BasicModelView<ConCell> : AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    val variables: List<Variable>
    val constraints: ModelConstraint<ConCell>
    val name: String

    /** Stable identity namespace carried by the model artifact. / 模型 artifact 携带的稳定身份命名空间。 */
    val identityNamespace: String?
        get() = constraints.identityNamespace

    /** Stable identity schema version carried by the model artifact. / 模型 artifact 携带的稳定身份 schema 版本。 */
    val identitySchemaVersion: String?
        get() = constraints.identitySchemaVersion

    /** 是否包含连续变量 / Whether the model contains continuous variables */
    val containsContinuous: Boolean
        get() {
            return variables.any { it.type.isContinuousType }
        }

    /** 是否包含二进制变量 / Whether the model contains binary variables */
    val containsBinary: Boolean
        get() {
            return variables.any { it.type.isBinaryType }
        }

    /** 是否包含整数变量 / Whether the model contains integer variables */
    val containsInteger: Boolean
        get() {
            return variables.any { it.type.isIntegerType }
        }

    /** 是否包含非二进制整数变量 / Whether the model contains non-binary integer variables */
    val containsNotBinaryInteger: Boolean
        get() {
            return variables.any { it.type.isNotBinaryIntegerType }
        }

    /**
     * 使用默认路径和指定格式导出模型。 / Export the model using the default path and specified format.
     *
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
    */
    fun export(format: ModelFileFormat): Try {
        return export(Path("."), format)
    }

    /**
     * 使用指定文件名和格式导出模型到当前目录。 / Export the model to the current directory using the given file name and format.
     *
     * @param name   文件名 / File name
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
    */
    fun export(name: String, format: ModelFileFormat): Try {
        return export(Path(".").resolve(name), format)
    }

    /**
     * 使用指定路径和格式导出模型到文件。 / Export the model to a file at the given path using the specified format.
     *
     * @param path   导出路径 / Export path
     * @param format 文件格式 / File format
     * @return 导出结果 / Export result
    */
    fun export(path: Path, format: ModelFileFormat): Try {
        val file = if (path.isDirectory()) {
            path.resolve("$name.${format}").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        val result = when (format) {
            ModelFileFormat.LP -> {
                exportLP(writer)
            }
        }
        writer.flush()
        writer.close()
        return result
    }

    /**
     * 将模型以 LP 格式写入给定输出流。 / Write the model in LP format to the given output stream.
     *
     * @param writer 输出流写入器 / Output stream writer
     * @return 导出结果 / Export result
    */
    fun exportLP(writer: OutputStreamWriter): Try

    override fun close() {
        constraints.close()
    }
}

/**
 * 完整模型视图接口，在 BasicModelView 基础上增加目标函数。 / Full model view interface adding an objective function on top of BasicModelView.
*/
interface ModelView<ConCell, ObjCell> : BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell>, ObjCell : ModelCell<ObjCell>, ObjCell : Copyable<ObjCell> {
    val objective: Objective<ObjCell>

    /**
     * Resolve model-root identity metadata from every model element category. /
     * 从模型的所有元素类别汇总模型根身份 metadata。
     */
    override val identityNamespace: String?
        get() = resolveModelIdentityMetadata(
            preferred = constraints.identityNamespace,
            candidates = variables.map { it.identityNamespace } + listOf(objective.identityNamespace)
        )

    /**
     * Resolve model-root identity schema from every model element category. /
     * 从模型的所有元素类别汇总模型根身份 schema。
     */
    override val identitySchemaVersion: String?
        get() = resolveModelIdentityMetadata(
            preferred = constraints.identitySchemaVersion,
            candidates = variables.map { it.identitySchemaVersion } + listOf(objective.identitySchemaVersion)
        )
}

private fun resolveModelIdentityMetadata(
    preferred: String?,
    candidates: List<String?>
): String? {
    val values = buildList {
        preferred?.takeIf { it.isNotBlank() }?.let(::add)
        candidates.mapNotNullTo(this) { it?.takeIf(String::isNotBlank) }
    }.distinct()
    return values.singleOrNull()
}
