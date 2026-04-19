package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_model.ObjectCategory
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableType
import fuookami.ospf.kotlin.utils.concept.Copyable
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory

typealias OriginConstraint<P> = fuookami.ospf.kotlin.core.intermediate_model.ConstraintF64<P>

data class VariableSlack(
    val constraint: OriginConstraint<*>? = null,
    val lowerBound: Variable? = null,
    val upperBound: Variable? = null
)

class Variable(
    val index: Int,
    lowerBound: Flt64,
    upperBound: Flt64,
    type: VariableType<*>,
    val origin: AbstractVariableItem<*, *>?,
    val dualOrigin: OriginConstraint<*>? = null,
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

interface ModelCell<Self : ModelCell<Self>> {
    val coefficient: Flt64

    operator fun unaryMinus(): Self
}

interface ConstraintCell<Self : ConstraintCell<Self>> : ModelCell<Self> {
    val rowIndex: Int
}

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

    @Deprecated("Use sparseLhs on LinearConstraintBatch or QuadraticConstraintBatch instead.", level = DeprecationLevel.WARNING)
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

class Objective<CellF64 : Copyable<CellF64>>(
    val category: ObjectCategory,
    val objective: List<CellF64>,
    val constant: Flt64 = Flt64(0.0)
) : Cloneable, Copyable<Objective<CellF64>> {
    override fun copy() = Objective(category, objective.toList())
    override fun clone() = copy()
}

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

    fun export(format: ModelFileFormat): Try {
        return export(Path("."), format)
    }

    fun export(name: String, format: ModelFileFormat): Try {
        return export(Path(".").resolve(name), format)
    }

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

    fun exportLP(writer: OutputStreamWriter): Try

    override fun close() {
        constraints.close()
    }
}

interface ModelView<ConCell, ObjCell> : BasicModelView<ConCell>
        where ConCell : ConstraintCell<ConCell>, ConCell : Copyable<ConCell>, ObjCell : ModelCell<ObjCell>, ObjCell : Copyable<ObjCell> {
    val objective: Objective<ObjCell>
}



