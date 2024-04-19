package fuookami.ospf.kotlin.core.frontend.model.mechanism

import java.io.*
import java.nio.file.*
import kotlin.io.path.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.*

sealed interface MetaModel<I : Inequality<I, Cell, C>, Cell : MonomialCell<Cell, C>, C : Category> : ModelInterface {
    class SubObject<I : Inequality<I, Cell, C>, Cell : MonomialCell<Cell, C>, C : Category>(
        val parent: MetaModel<I, Cell, C>,
        val category: ObjectCategory,
        val polynomial: Polynomial<*, *, Cell, C>,
        val name: String = polynomial.name
    ) {
        fun value(zeroIfNone: Boolean = false): Flt64? {
            return polynomial.value(parent.tokens, zeroIfNone)
        }

        fun value(results: List<Flt64>, zeroIfNone: Boolean = false): Flt64? {
            return polynomial.value(results, parent.tokens, zeroIfNone)
        }
    }

    val name: String
    val constraints: MutableList<I>
    override val objectCategory: ObjectCategory
    val subObjects: MutableList<SubObject<I, Cell, C>>
    val tokens: MutableTokenTable<Cell, C>

    override fun addVar(item: AbstractVariableItem<*, *>) {
        tokens.add(item)
    }

    override fun addVars(items: Iterable<AbstractVariableItem<*, *>>) {
        tokens.add(items)
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    fun addSymbol(symbol: Symbol<Cell, C>) {
        tokens.add(symbol)
    }

    fun addSymbols(symbols: Iterable<Symbol<Cell, C>>) {
        tokens.add(symbols)
    }

    fun addConstraint(
        symbol: LogicFunctionSymbol<Cell, C>,
        name: String? = null,
        displayName: String? = null
    )

    @Suppress("UNCHECKED_CAST")
    override fun addConstraint(
        inequality: Inequality<*, *, *>,
        name: String?,
        displayName: String?
    ) {
        inequality as I

        if (name != null) {
            inequality.name = name
        }
        if (displayName != null) {
            inequality.displayName = name
        }
        constraints.add(inequality)
    }

    fun registerConstraintGroup(name: String)
    fun indicesOfConstraintGroup(name: String): IntRange?

    fun addObject(
        category: ObjectCategory,
        item: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    )

    fun addObject(
        category: ObjectCategory,
        symbol: Symbol<Cell, C>,
        name: String? = null,
        displayName: String? = null
    )

    fun addObject(
        category: ObjectCategory,
        monomial: Monomial<*, Cell, C>,
        name: String? = null,
        displayName: String? = null
    )

    @Suppress("UNCHECKED_CAST")
    override fun addObject(
        category: ObjectCategory,
        polynomial: Polynomial<*, *, *, *>,
        name: String?,
        displayName: String?
    ) {
        polynomial as Polynomial<*, *, Cell, C>

        if (name != null) {
            polynomial.name = name
        }
        if (displayName != null) {
            polynomial.displayName = displayName
        }
        subObjects.add(SubObject(this, category, polynomial))
    }

    fun maximize(
        item: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Maximum, item, name, displayName)
    }

    fun maximize(
        symbol: Symbol<Cell, C>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Maximum, symbol, name, displayName)
    }

    fun maximize(
        monomial: Monomial<*, Cell, C>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Maximum, monomial, name, displayName)
    }

    fun minimize(
        item: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Minimum, item, name, displayName)
    }

    fun minimize(
        symbol: Symbol<Cell, C>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Minimum, symbol, name, displayName)
    }

    fun minimize(
        monomial: Monomial<*, Cell, C>,
        name: String? = null,
        displayName: String? = null
    ) {
        addObject(ObjectCategory.Minimum, monomial, name, displayName)
    }

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
            for (token in tokens.tokens) {
                token._result = null
            }
        }
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

    fun export(): Try {
        return export("$name.opm")
    }

    fun export(name: String): Try {
        return export(Path(".").resolve(name))
    }

    fun export(path: Path, unfold: Boolean = false): Try {
        val file = if (path.isDirectory()) {
            path.resolve("$name.opm").toFile()
        } else {
            path.toFile()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        val writer = FileWriter(file)
        val result = when (file.extension) {
            "opm" -> {
                exportOpm(writer, unfold)
            }

            else -> {
                ok
            }
        }
        writer.flush()
        writer.close()
        return result
    }

    private fun exportOpm(writer: FileWriter, unfold: Boolean): Try {
        writer.append("Model Name: $name\n")
        writer.append("\n")

        writer.append("Variables:\n")
        for (token in tokens.tokens.toList().sortedBy { it.solverIndex }) {
            val range = token.range
            writer.append("${token.name}, ${token.type}, ")
            if (range.empty) {
                writer.append("empty\n")
            } else {
                writer.append("${range.lowerInterval.lowerSign}${range.lowerBound}, ${range.upperBound}${range.upperInterval.upperSign}\n")
            }
        }
        writer.append("\n")

        val temp = tokens.copy()
        for (symbol in temp.symbols.filterIsInstance<FunctionSymbol<Cell, C>>()) {
            symbol.register(temp)
        }

        writer.append("Symbols:\n")
        for (symbol in tokens.symbols.toList().sortedBy { it.name }) {
            val range = symbol.range
            writer.append("$symbol = ${symbol.toRawString(unfold)}, ")
            if (range.empty) {
                writer.append("empty")
            } else {
                writer.append("${range.lowerInterval.lowerSign}${range.lowerBound}, ${range.upperBound}${range.upperInterval.upperSign}\n")
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

        return ok
    }
}

class LinearMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    manualTokenAddition: Boolean = true
) : MetaModel<LinearInequality, LinearMonomialCell, Linear> {
    override val constraints: ArrayList<LinearInequality> = ArrayList()
    override val subObjects: ArrayList<MetaModel.SubObject<LinearInequality, LinearMonomialCell, Linear>> = ArrayList()
    override val tokens: MutableTokenTable<LinearMonomialCell, Linear> = if (manualTokenAddition) {
        ManualAddTokenTable()
    } else {
        AutoAddTokenTable()
    }

    private var currentConstraintGroup: String? = null
    private var currentConstraintGroupIndexLowerBound: Int? = null
    private val constraintGroupIndexMap = HashMap<String, IntRange>()

    override fun addConstraint(
        symbol: LinearLogicFunctionSymbol,
        name: String?,
        displayName: String?
    ) {
        addConstraint(symbol eq Flt64.one, name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        item: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ) {
        addObject(category, LinearPolynomial(item), name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        monomial: Monomial<*, LinearMonomialCell, Linear>,
        name: String?,
        displayName: String?
    ) {
        monomial as LinearMonomial

        addObject(category, LinearPolynomial(monomial), name, displayName)
    }

    override fun addObject(
        category: ObjectCategory,
        symbol: LinearSymbol,
        name: String?,
        displayName: String?
    ) {
        addObject(category, LinearPolynomial(symbol), name, displayName)
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ) {
        addObject(category, LinearPolynomial(constant), name, displayName)
    }

    override fun registerConstraintGroup(name: String) {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!! until constraints.size
        }
        currentConstraintGroup = name
        currentConstraintGroupIndexLowerBound = constraints.size
    }

    override fun indicesOfConstraintGroup(name: String): IntRange? {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!! until constraints.size
            currentConstraintGroup = null
            currentConstraintGroupIndexLowerBound = null
        }
        return constraintGroupIndexMap[name]
    }
}

class QuadraticMetaModel(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    manualTokenAddition: Boolean = true
) : MetaModel<QuadraticInequality, QuadraticMonomialCell, Quadratic> {
    override val constraints: ArrayList<QuadraticInequality> = ArrayList()
    override val subObjects: ArrayList<MetaModel.SubObject<QuadraticInequality, QuadraticMonomialCell, Quadratic>> = ArrayList()
    override val tokens: MutableTokenTable<QuadraticMonomialCell, Quadratic> = if (manualTokenAddition) {
        ManualAddTokenTable()
    } else {
        AutoAddTokenTable()
    }

    private var currentConstraintGroup: String? = null
    private var currentConstraintGroupIndexLowerBound: Int? = null
    private val constraintGroupIndexMap = HashMap<String, IntRange>()

    @JvmName("addConstraintByLinearLogicFunctionSymbol")
    fun addConstraint(
        symbol: LinearLogicFunctionSymbol,
        name: String?,
        displayName: String?
    ) {
        addConstraint(QuadraticPolynomial(symbol) eq Flt64.one, name, displayName)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintByQuadraticLogicFunctionSymbol")
    override fun addConstraint(
        symbol: QuadraticLogicFunctionSymbol,
        name: String?,
        displayName: String?
    ) {
        addConstraint(symbol eq Flt64.one, name, displayName)
    }

    override fun addConstraint(inequality: Inequality<*, *, *>, name: String?, displayName: String?) {
        if (inequality as? LinearInequality != null) {
            if (name != null) {
                inequality.name = name
            }
            if (displayName != null) {
                inequality.displayName = name
            }
            constraints.add(QuadraticInequality(inequality))
        } else {
            inequality as QuadraticInequality

            if (name != null) {
                inequality.name = name
            }
            if (displayName != null) {
                inequality.displayName = name
            }
            constraints.add(inequality)
        }
    }

    override fun addObject(
        category: ObjectCategory,
        item: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ) {
        addObject(category, QuadraticPolynomial(item), name, displayName)
    }

    @JvmName("addLinearMonomialObject")
    fun addObject(
        category: ObjectCategory,
        monomial: Monomial<*, LinearMonomialCell, Linear>,
        name: String?,
        displayName: String?
    ) {
        monomial as LinearMonomial

        addObject(category, QuadraticPolynomial(monomial), name, displayName)
    }

    @JvmName("addMaximizationLinearMonomialObject")
    fun maximize(
        monomial: Monomial<*, LinearMonomialCell, Linear>,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Maximum, monomial, name, displayName)
    }

    @JvmName("addMinimizationLinearMonomialObject")
    fun minimize(
        monomial: Monomial<*, LinearMonomialCell, Linear>,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Minimum, monomial, name, displayName)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuadraticMonomialObject")
    override fun addObject(
        category: ObjectCategory,
        monomial: Monomial<*, QuadraticMonomialCell, Quadratic>,
        name: String?,
        displayName: String?
    ) {
        monomial as QuadraticMonomial

        addObject(category, QuadraticPolynomial(monomial), name, displayName)
    }

    @JvmName("addLinearSymbolObject")
    fun addObject(
        category: ObjectCategory,
        symbol: LinearSymbol,
        name: String?,
        displayName: String?
    ) {
        addObject(category, QuadraticPolynomial(symbol), name, displayName)
    }

    @JvmName("addMaximizationLinearSymbolObject")
    fun maximize(
        symbol: LinearSymbol,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Maximum, symbol, name, displayName)
    }

    @JvmName("addMinimizationLinearSymbolObject")
    fun minimize(
        symbol: LinearSymbol,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Minimum, symbol, name, displayName)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuadraticSymbolObject")
    override fun addObject(
        category: ObjectCategory,
        symbol: QuadraticSymbol,
        name: String?,
        displayName: String?
    ) {
        addObject(category, QuadraticPolynomial(symbol), name, displayName)
    }

    fun addObject(
        category: ObjectCategory,
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ) {
        addObject(category, QuadraticPolynomial(polynomial), name, displayName)
    }

    fun maximize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Maximum, polynomial, name, displayName)
    }

    fun minimize(
        polynomial: AbstractLinearPolynomial<*>,
        name: String?,
        displayName: String?
    ) {
        addObject(ObjectCategory.Minimum, polynomial, name, displayName)
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ) {
        addObject(category, QuadraticPolynomial(constant), name, displayName)
    }

    override fun registerConstraintGroup(name: String) {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!! until constraints.size
        }
        currentConstraintGroup = name
        currentConstraintGroupIndexLowerBound = constraints.size
    }

    override fun indicesOfConstraintGroup(name: String): IntRange? {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!! until constraints.size
            currentConstraintGroup = null
            currentConstraintGroupIndexLowerBound = null
        }
        return constraintGroupIndexMap[name]
    }
}
