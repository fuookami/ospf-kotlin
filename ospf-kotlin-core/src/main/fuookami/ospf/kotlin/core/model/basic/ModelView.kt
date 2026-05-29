/**
 * 模型视图
 * Model view
 */
package fuookami.ospf.kotlin.core.model.basic

import java.io.*
import java.nio.file.Path
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.Constraint
import fuookami.ospf.kotlin.core.variable.*

/**
 * 变量松弛信息，关联变量与其约束或界限。
 * Variable slack information associating a variable with its constraint or bounds.
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
 * 求解器中的变量，封装变量的界限、类型和初始值等属性。
 * Variable in the solver, encapsulating bounds, type, and initial value properties.
 *
 * @property index         变量在求解器中的索引 / Variable index in the solver
 * @property lowerBound    下界 / Lower bound
 * @property upperBound    上界 / Upper bound
 * @property type          变量类型 / Variable type
 * @property origin        原始抽象变量项 / The originating abstract variable item
 * @property dualOrigin    对偶约束来源 / The dual constraint origin
 * @property slack         松弛信息 / Slack information
 * @property name          变量名 / Variable name
 * @property initialResult 初始值（可为 null） / Initial value (nullable)
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
    val initialResult: Flt64? = null
) : Cloneable, Copyable<Variable> {
    internal var _lowerBound = lowerBound
    internal var _upperBound = upperBound
    internal var _type = type

    val lowerBound by ::_lowerBound
    val upperBound by ::_upperBound
    val type by ::_type

    val free: Boolean
        get() {
            return negativeFree && positiveFree
        }

    val normalized: Boolean
        get() {
            return negativeNormalized || positiveNormalized
        }

    val negativeNormalized: Boolean
        get() {
            return negativeFree && upperBound eq Flt64.zero
        }

    val negativeFree: Boolean
        get() {
            return (lowerBound eq Flt64.negativeInfinity || lowerBound leq -Flt64.decimalPrecision.reciprocal())
        }

    val positiveNormalized: Boolean
        get() {
            return positiveFree && lowerBound eq Flt64.zero
        }

    val positiveFree: Boolean
        get() {
            return (upperBound eq Flt64.infinity || upperBound geq Flt64.decimalPrecision.reciprocal())
        }

    override fun copy() = Variable(index, lowerBound, upperBound, type, origin, dualOrigin, slack, name, initialResult)
    override fun clone() = copy()

    override fun toString() = name
}

/**
 * 模型单元格接口，持有系数值。
 * Model cell interface holding a coefficient value.
 */
interface ModelCell<Self : ModelCell<Self>> {
    val coefficient: Flt64

    operator fun unaryMinus(): Self
}

/**
 * 约束单元格接口，扩展 ModelCell 增加行索引。
 * Constraint cell interface extending ModelCell with a row index.
 */
interface ConstraintCell<Self : ConstraintCell<Self>> : ModelCell<Self> {
    val rowIndex: Int
}

/**
 * 约束来源枚举，标识约束的产生途径。
 * Constraint source enumeration indicating how a constraint was produced.
 */
enum class ConstraintSource {
    Origin,
    LowerBound,
    UpperBound,
    Dual,
    FarkasDual,
    Feasibility,
    Elastic,
    ElasticLowerBound,
    ElasticUpperBound,
    ElasticSlackBinary,
    ElasticSlackMinmax
}

/**
 * 模型约束的抽象基类，管理约束的左端、符号、右端、名称和来源。
 * Abstract base class for model constraints, managing LHS, signs, RHS, names, and sources.
 *
 * @property constraintCount 约束数量 / Number of constraints
 */
abstract class ModelConstraint<ConCell>(
    val constraintCount: Int,
    signs: List<ConstraintRelation>,
    rhs: List<Flt64>,
    names: List<String>,
    sources: List<ConstraintSource>
) : Cloneable, Copyable<ModelConstraint<ConCell>>, AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    internal val _signs = signs.toMutableList()
    internal val _rhs = rhs.toMutableList()
    internal val _names = names.toMutableList()
    internal val _sources = sources.toMutableList()

    abstract val lhs: List<List<ConCell>>
    val signs: List<ConstraintRelation> by ::_signs
    val rhs: List<Flt64> by ::_rhs
    val names: List<String> by ::_names
    val sources: List<ConstraintSource> by ::_sources

    val size: Int get() = rhs.size
    val indices: IntRange get() = rhs.indices

    override fun clone() = copy()

    override fun close() {
        _signs.clear()
        _rhs.clear()
        _names.clear()
        _sources.clear()
    }
}

/**
 * 目标函数，包含优化方向、目标单元格列表和常数项。
 * Objective function containing optimization direction, objective cell list, and constant.
 *
 * @property category  优化方向 / Optimization direction
 * @property objective 目标单元格列表 / List of objective cells
 * @property constant  常数项 / Constant term
 */
class Objective<C : Copyable<C>>(
    val category: ObjectCategory,
    val objective: List<C>,
    val constant: Flt64 = Flt64(0.0)
) : Cloneable, Copyable<Objective<C>> {
    override fun copy() = Objective(category, objective.toList())
    override fun clone() = copy()
}

/**
 * 基本模型视图接口，提供变量、约束、名称及导出能力。
 * Basic model view interface providing variables, constraints, name, and export capability.
 */
interface BasicModelView<ConCell> : AutoCloseable
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell> {
    val variables: List<Variable>
    val constraints: ModelConstraint<ConCell>
    val name: String

    val containsContinuous: Boolean
        get() {
            return variables.any { it.type.isContinuousType }
        }

    val containsBinary: Boolean
        get() {
            return variables.any { it.type.isBinaryType }
        }

    val containsInteger: Boolean
        get() {
            return variables.any { it.type.isIntegerType }
        }

    val containsNotBinaryInteger: Boolean
        get() {
            return variables.any { it.type.isNotBinaryIntegerType }
        }

    /** 使用默认路径和指定格式导出模型。 / Export the model using the default path and specified format. */
    fun export(format: ModelFileFormat): Try {
        return export(Path("."), format)
    }

    /** 使用指定文件名和格式导出模型到当前目录。 / Export the model to the current directory using the given file name and format. */
    fun export(name: String, format: ModelFileFormat): Try {
        return export(Path(".").resolve(name), format)
    }

    /** 使用指定路径和格式导出模型到文件。 / Export the model to a file at the given path using the specified format. */
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

    /** 将模型以 LP 格式写入给定输出流。 / Write the model in LP format to the given output stream. */
    fun exportLP(writer: OutputStreamWriter): Try

    override fun close() {
        constraints.close()
    }
}

/**
 * 完整模型视图接口，在 BasicModelView 基础上增加目标函数。
 * Full model view interface adding an objective function on top of BasicModelView.
 */
interface ModelView<ConCell, ObjCell> : BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell>, ObjCell : ModelCell<ObjCell>, ObjCell : Copyable<ObjCell> {
    val objective: Objective<ObjCell>
}
