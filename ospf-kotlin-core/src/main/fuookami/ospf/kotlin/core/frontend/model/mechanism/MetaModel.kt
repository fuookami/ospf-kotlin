package fuookami.ospf.kotlin.core.frontend.model.mechanism

import java.io.FileWriter
import java.nio.file.Path
import kotlin.io.path.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

sealed interface MetaModel : Model {
    class SubObject<Poly : Polynomial<Poly, M, Cell>, M : Monomial<M, Cell>, Cell : MonomialCell<Cell>>(
        val parent: MetaModel,
        val category: ObjectCategory,
        val polynomial: Poly,
        val name: String = polynomial.name
    ) {
        fun value(zeroIfNone: Boolean = false): Flt64? {
            return value(parent.tokens, zeroIfNone)
        }

        fun value(results: List<Flt64>, zeroIfNone: Boolean = false): Flt64? {
            return value(results, parent.tokens, zeroIfNone)
        }

        fun value(tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(tokenTable, zeroIfNone)
        }

        fun value(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.evaluate(results, tokenTable, zeroIfNone)
        }
    }

    val name: String
    val constraints: List<Inequality<*, *>>
    override val objectCategory: ObjectCategory
    val subObjects: List<SubObject<*, *, *>>
    val tokens: AbstractMutableTokenTable

    override fun add(item: AbstractVariableItem<*, *>): Try {
        return tokens.add(item)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVars")
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        return tokens.add(items)
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    fun add(symbol: IntermediateSymbol): Try {
        return tokens.add(symbol)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol>): Try {
        return tokens.add(symbols)
    }

    fun remove(symbol: IntermediateSymbol) {
        tokens.remove(symbol)
    }

    fun registerConstraintGroup(name: String)
    fun indicesOfConstraintGroup(name: String): IntRange?

    override fun setSolution(solution: Solution) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, Flt64>) {
        tokens.setSolution(solution)
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }

    fun flush(force: Boolean = false) {
        if (force) {
            tokens.clearSolution()
        }
        tokens.flush()
        for (symbol in tokens.symbols) {
            symbol.flush(force)
        }
        for (constraint in constraints) {
            constraint.flush(force)
        }
        for (objective in subObjects) {
            objective.polynomial.flush(force)
        }
    }

    suspend fun export(): Try {
        return export("$name.opm")
    }

    suspend fun export(name: String): Try {
        return export(Path(".").resolve(name))
    }

    suspend fun export(path: Path, unfold: Boolean = false): Try {
        val file = if (path.isDirectory()) {
            path.resolve("$name.opm").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            withContext(Dispatchers.IO) {
                file.createNewFile()
            }
        }
        val writer = withContext(Dispatchers.IO) {
            FileWriter(file)
        }
        val result = when (file.extension) {
            "opm" -> {
                exportOpm(writer, unfold)
            }

            else -> {
                ok
            }
        }
        withContext(Dispatchers.IO) {
            writer.flush()
            writer.close()
        }
        return result
    }

    private suspend fun exportOpm(writer: FileWriter, unfold: Boolean): Try {
        when (val result = when (tokens) {
            is MutableTokenTable -> {
                val temp = tokens.copy() as MutableTokenTable
                when (val result = tokens.symbols.register(temp)) {
                    is Ok -> {
                        Ok(temp)
                    }

                    is Failed -> {
                        Failed(result.error)
                    }
                }
            }

            is ConcurrentMutableTokenTable -> {
                coroutineScope<Ret<AbstractTokenTable>> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(temp)) {
                        is Ok -> {
                            Ok(temp)
                        }

                        is Failed -> {
                            Failed(result.error)
                        }
                    }
                }
            }
        }) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return withContext(Dispatchers.IO) {
            writer.append("Model Name: $name\n")
            writer.append("\n")

            writer.append("Variables:\n")
            for (token in tokens.tokens.toList().sortedBy { it.solverIndex }) {
                val range = token.range
                writer.append("${token.name}, ${token.type}, ")
                if (range == null) {
                    writer.append("empty\n")
                } else {
                    writer.append("${range}\n")
                }
            }
            writer.append("\n")

            writer.append("Symbols:\n")
            for (symbol in tokens.symbols.toList().sortedBy { it.name }) {
                val range = symbol.range
                writer.append("$symbol = ${symbol.toRawString(unfold)}, ")
                if (range.empty) {
                    writer.append("empty")
                } else {
                    writer.append("${range}\n")
                }
            }
            writer.append("\n")

            writer.append("Objectives:\n")
            for (obj in subObjects) {
                writer.append("${obj.category} ${obj.name}: ${obj.polynomial.toRawString(unfold)} \n")
            }
            writer.append("\n")

            writer.append("Subject to:\n")
            for (constraint in constraints) {
                writer.append("$constraint: ${constraint.toRawString(unfold)}\n")
            }
            writer.append("\n")

            ok
        }
    }
}

interface AbstractLinearMetaModel : MetaModel, LinearModel
interface AbstractQuadraticMetaModel : MetaModel, QuadraticModel

abstract class AbstractMetaModel(
    val category: Category,
    manualTokenAddition: Boolean = true,
    internal val concurrent: Boolean = true
) : MetaModel {
    override val tokens: AbstractMutableTokenTable = if (concurrent) {
        if (manualTokenAddition) {
            ConcurrentManualAddTokenTable(category)
        } else {
            ConcurrentAutoTokenTable(category)
        }
    } else {
        if (manualTokenAddition) {
            ManualTokenTable(category)
        } else {
            AutoTokenTable(category)
        }
    }

    private var currentConstraintGroup: String? = null
    private var currentConstraintGroupIndexLowerBound: Int? = null
    private val constraintGroupIndexMap = HashMap<String, IntRange>()

    override fun registerConstraintGroup(name: String) {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
        }
        currentConstraintGroup = name
        currentConstraintGroupIndexLowerBound = constraints.size
    }

    override fun indicesOfConstraintGroup(name: String): IntRange? {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
            currentConstraintGroup = null
            currentConstraintGroupIndexLowerBound = null
        }
        return constraintGroupIndexMap[name]
    }
}

class LinearMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    manualTokenAddition: Boolean = true,
    concurrent: Boolean = true
) : AbstractMetaModel(Linear, manualTokenAddition, concurrent), AbstractLinearMetaModel {
    internal val _constraints: MutableList<LinearInequality> = ArrayList()
    override val constraints: List<Inequality<*, *>> by ::_constraints
    internal val _subObjects: MutableList<MetaModel.SubObject<LinearPolynomial, LinearMonomial, LinearMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

    override fun addConstraint(
        constraint: LinearInequality,
        name: String?,
        displayName: String?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(constraint)
        return ok
    }

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ): Try {
        val obj = LinearPolynomial(polynomial)
        name?.let { obj.name = it }
        displayName?.let { obj.displayName = it }
        _subObjects.add(MetaModel.SubObject(this, category, obj))
        return ok
    }

    override fun toString(): String {
        return name
    }
}

class QuadraticMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    manualTokenAddition: Boolean = true,
    concurrent: Boolean = true
) : AbstractMetaModel(Quadratic, manualTokenAddition, concurrent), AbstractLinearMetaModel, AbstractQuadraticMetaModel {
    internal val _constraints: MutableList<QuadraticInequality> = ArrayList()
    override val constraints: List<Inequality<*, *>> by ::_constraints
    internal val _subObjects: MutableList<MetaModel.SubObject<QuadraticPolynomial, QuadraticMonomial, QuadraticMonomialCell>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<*, *, *>> by ::_subObjects

    override fun addConstraint(
        constraint: QuadraticInequality,
        name: String?,
        displayName: String?
    ): Try {
        name?.let { constraint.name = it }
        displayName?.let { constraint.name = it }
        _constraints.add(constraint)
        return ok
    }

    override fun addObject(
        category: ObjectCategory,
        polynomial: AbstractQuadraticPolynomial<*>,
        name: String?,
        displayName: String?
    ): Try {
        val obj = QuadraticPolynomial(polynomial)
        name?.let { obj.name = it }
        displayName?.let { obj.displayName = it }
        _subObjects.add(MetaModel.SubObject(this, category, obj))
        return ok
    }
}
