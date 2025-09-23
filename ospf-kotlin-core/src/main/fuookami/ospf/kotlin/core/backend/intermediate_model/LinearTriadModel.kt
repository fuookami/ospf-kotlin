package fuookami.ospf.kotlin.core.backend.intermediate_model

import java.io.*
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.*
import io.michaelrocks.bimap.*
import fuookami.ospf.kotlin.utils.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.functional.sumOf
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.Sign

class LinearConstraintCell(
    override val rowIndex: Int,
    val colIndex: Int,
    coefficient: Flt64
) : ConstraintCell<LinearConstraintCell>, Cloneable, Copyable<LinearConstraintCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearConstraintCell {
        return LinearConstraintCell(rowIndex, colIndex, -coefficient)
    }

    override fun copy() = LinearConstraintCell(rowIndex, colIndex, coefficient.copy())
    override fun clone() = copy()
}

typealias LinearConstraint = Constraint<LinearConstraintCell>

class LinearObjectiveCell(
    val colIndex: Int,
    coefficient: Flt64
) : Cell<LinearObjectiveCell>, Cloneable, Copyable<LinearObjectiveCell> {
    internal var _coefficient = coefficient
    override val coefficient by ::_coefficient

    override fun unaryMinus(): LinearObjectiveCell {
        return LinearObjectiveCell(colIndex, -coefficient)
    }

    override fun copy() = LinearObjectiveCell(colIndex, coefficient.copy())
    override fun clone() = copy()
}

typealias LinearObjective = Objective<LinearObjectiveCell>

class BasicLinearTriadModel(
    override val variables: List<Variable>,
    override val constraints: LinearConstraint,
    override val name: String
) : BasicModelView<LinearConstraintCell>, Cloneable, Copyable<BasicLinearTriadModel> {
    override fun copy() = BasicLinearTriadModel(
        variables.map { it.copy() },
        constraints.copy(),
        name
    )

    override fun clone() = copy()

    fun normalized(): Boolean {
        return variables.all {
            (it.lowerBound.isNegativeInfinity() || it.lowerBound eq Flt64.zero) && (it.upperBound.isInfinity() || it.upperBound eq Flt64.zero)
        } && constraints.rhs.all {
            it geq Flt64.zero
        }
    }

    fun linearRelax() {
        variables.forEach {
            when (it.type) {
                is Binary -> {
                    it._type = Percentage
                }

                is Ternary, is UInteger -> {
                    it._type = UContinuous
                }

                is BalancedTernary, is fuookami.ospf.kotlin.core.frontend.variable.Integer -> {
                    it._type = Continuous
                }

                else -> {}
            }
        }
    }

    fun normalize() {
        for (variable in variables) {
            if (!(variable.lowerBound.isNegativeInfinity() || variable.lowerBound leq -Flt64.decimalPrecision.reciprocal() || variable.lowerBound eq Flt64.zero)) {
                constraints._lhs.add(
                    listOf(
                        LinearConstraintCell(
                            rowIndex = constraints.size,
                            colIndex = variable.index,
                            coefficient = Flt64.one
                        )
                    )
                )
                constraints._signs.add(Sign.GreaterEqual)
                constraints._rhs.add(variable.lowerBound)
                constraints._names.add("${variable.name}_lb")
                constraints._sources.add(ConstraintSource.NormalizationLowerBound)
                variable._lowerBound = Flt64.negativeInfinity
            }
            if (!(variable.upperBound.isInfinity() || variable.upperBound geq Flt64.decimalPrecision.reciprocal() || variable.upperBound eq Flt64.zero)) {
                constraints._lhs.add(
                    listOf(
                        LinearConstraintCell(
                            rowIndex = constraints.size,
                            colIndex = variable.index,
                            coefficient = Flt64.one
                        )
                    )
                )
                constraints._signs.add(Sign.LessEqual)
                constraints._rhs.add(variable.upperBound)
                constraints._names.add("${variable.name}_ub")
                constraints._sources.add(ConstraintSource.NormalizationUpperBound)
                variable._upperBound = Flt64.infinity
            }
        }
    }

    override fun exportLP(writer: OutputStreamWriter): Try {
        writer.append("Subject To\n")
        for (i in constraints.indices) {
            writer.append(" ${constraints.names[i]}: ")
            var flag = false
            for (j in constraints.lhs[i].indices) {
                if (constraints.lhs[i][j].coefficient eq Flt64.zero) {
                    continue
                }

                val coefficient = if (flag) {
                    if (constraints.lhs[i][j].coefficient leq Flt64.zero) {
                        writer.append(" - ")
                    } else {
                        writer.append(" + ")
                    }
                    abs(constraints.lhs[i][j].coefficient)
                } else {
                    constraints.lhs[i][j].coefficient
                }
                if (coefficient neq Flt64.zero) {
                    if (coefficient neq Flt64.one) {
                        writer.append("$coefficient ")
                    }
                    writer.append("${variables[constraints.lhs[i][j].colIndex]}")
                }
                flag = true
            }
            if (!flag) {
                writer.append("0")
            }
            writer.append(" ${constraints.signs[i]} ${constraints.rhs[i]}\n")
        }
        writer.append("\n")

        writer.append("Bounds\n")
        for (variable in variables) {
            val lowerInf = variable.lowerBound.isNegativeInfinity()
            val upperInf = variable.upperBound.isInfinity()
            if (lowerInf && upperInf) {
                writer.append(" $variable free\n")
            } else if (lowerInf) {
                writer.append(" $variable <= ${variable.upperBound}\n")
            } else if (upperInf) {
                writer.append(" $variable >= ${variable.lowerBound}\n")
            } else {
                if (variable.lowerBound eq variable.upperBound) {
                    writer.append(" $variable = ${variable.lowerBound}\n")
                } else {
                    writer.append(" ${variable.lowerBound} <= $variable <= ${variable.upperBound}\n")
                }
            }
        }
        writer.append("\n")

        if (containsBinary) {
            writer.append("Binaries\n")
            for (variable in variables) {
                if (variable.type.isBinaryType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        if (containsNotBinaryInteger) {
            writer.append("Generals\n")
            for (variable in variables) {
                if (variable.type.isNotBinaryIntegerType) {
                    writer.append(" $variable")
                }
            }
            writer.append("\n")
        }

        writer.append("End\n")
        return ok
    }
}

typealias LinearTriadModelView = ModelView<LinearConstraintCell, LinearObjectiveCell>

data class LinearTriadModel(
    private val impl: BasicLinearTriadModel,
    val tokenIndexMap: BiMap<Token, Int>,
    override val objective: LinearObjective,
) : LinearTriadModelView, Cloneable, Copyable<LinearTriadModel> {
    override val variables: List<Variable> by impl::variables
    override val constraints: LinearConstraint by impl::constraints
    override val name: String by impl::name

    companion object {
        private val logger = logger()

        suspend operator fun invoke(
            model: LinearMechanismModel,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            concurrent: Boolean? = null
        ): LinearTriadModel {
            logger.trace("Creating LinearTriadModel for $model")
            val tokenIndexMap = if (fixedVariables.isNullOrEmpty()) {
                model.tokens.tokenIndexMap
            } else {
                model.tokens.tokenIndexMapWithout(fixedVariables.keys)
            }
            val triadModel = if (concurrent ?: model.concurrent) {
                coroutineScope {
                    val variablePromise = async(Dispatchers.Default) {
                        dumpVariables(model, tokenIndexMap)
                    }
                    val constraintPromise = async(Dispatchers.Default) {
                        dumpConstraintsAsync(model, tokenIndexMap, fixedVariables)
                    }
                    val objectivePromise = async(Dispatchers.Default) {
                        dumpObjectives(model, tokenIndexMap, fixedVariables)
                    }

                    LinearTriadModel(
                        impl = BasicLinearTriadModel(
                            variablePromise.await(),
                            constraintPromise.await(),
                            model.name
                        ),
                        tokenIndexMap = tokenIndexMap,
                        objective = objectivePromise.await()
                    )
                }
            } else {
                LinearTriadModel(
                    impl = BasicLinearTriadModel(
                        dumpVariables(model, tokenIndexMap),
                        dumpConstraints(model, tokenIndexMap, fixedVariables),
                        model.name
                    ),
                    tokenIndexMap = tokenIndexMap,
                    objective = dumpObjectives(model, tokenIndexMap, fixedVariables)
                )
            }

            logger.trace("LinearTriadModel created for $model")
            System.gc()
            return triadModel
        }

        private fun dumpVariables(
            model: LinearMechanismModel,
            tokenIndexMap: BiMap<Token, Int>
        ): List<Variable> {
            val variables = ArrayList<Variable?>()
            for ((_, _) in tokenIndexMap) {
                variables.add(null)
            }
            for ((token, i) in tokenIndexMap) {
                val bounds = model.constraints.filter {
                    it.lhs.all { cell -> cell.token == token }
                }
                val lb = bounds
                    .filter { it.sign == Sign.GreaterEqual || it.sign == Sign.Equal }
                    .maxOfOrNull {
                        val lhs = it.lhs.sumOf { cell -> cell.coefficient }
                        if (lhs neq Flt64.zero) {
                            it.rhs / lhs
                        } else if (it.rhs gr Flt64.zero) {
                            Flt64.infinity
                        } else {
                            Flt64.negativeInfinity
                        }
                    }
                val ub = bounds
                    .filter { it.sign == Sign.LessEqual || it.sign == Sign.Equal }
                    .minOfOrNull {
                        val lhs = it.lhs.sumOf { cell -> cell.coefficient }
                        if (lhs neq Flt64.zero) {
                            it.rhs / lhs
                        } else if (it.rhs gr Flt64.zero) {
                            Flt64.infinity
                        } else {
                            Flt64.negativeInfinity
                        }
                    }
                variables[i] = Variable(
                    index = i,
                    lowerBound = if (lb != null) {
                        max(lb, token.lowerBound!!.value.unwrap())
                    } else {
                        token.lowerBound!!.value.unwrap()
                    },
                    upperBound = if (ub != null) {
                        min(ub, token.upperBound!!.value.unwrap())
                    } else {
                        token.upperBound!!.value.unwrap()
                    },
                    type = token.variable.type,
                    origin = token.variable,
                    name = token.variable.name,
                    initialResult = token.result
                )
            }
            return variables.map { it!! }
        }

        private fun dumpConstraints(
            model: LinearMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): LinearConstraint {
            val notBoundConstraints = model.constraints.filter {
                it.lhs.isEmpty() || it.lhs.any { cell -> cell.token != it.lhs.first().token }
            }

            val constraints = notBoundConstraints.withIndex().map { (index, constraint) ->
                val lhs = ArrayList<LinearConstraintCell>()
                var rhs = constraint.rhs
                for (cell in constraint.lhs) {
                    if (tokenIndexes.containsKey(cell.token)) {
                        lhs.add(
                            LinearConstraintCell(
                                rowIndex = index,
                                colIndex = tokenIndexes[cell.token]!!,
                                coefficient = cell.coefficient.let {
                                    if (it.isInfinity()) {
                                        Flt64.decimalPrecision.reciprocal()
                                    } else if (it.isNegativeInfinity()) {
                                        -Flt64.decimalPrecision.reciprocal()
                                    } else {
                                        it
                                    }
                                }
                            )
                        )
                    } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                        rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                    }
                }
                lhs to rhs
            }

            val lhs = ArrayList<List<LinearConstraintCell>>()
            val signs = ArrayList<Sign>()
            val rhs = ArrayList<Flt64>()
            val names = ArrayList<String>()
            val sources = ArrayList<ConstraintSource>()
            for ((index, constraint) in notBoundConstraints.withIndex()) {
                lhs.add(constraints[index].first)
                signs.add(constraint.sign)
                rhs.add(constraints[index].second)
                names.add(constraint.name)
                sources.add(ConstraintSource.Origin)
            }
            return LinearConstraint(lhs, signs, rhs, names, sources)
        }

        private suspend fun dumpConstraintsAsync(
            model: LinearMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): LinearConstraint {
            val notBoundConstraints = model.constraints.filter {
                it.lhs.isEmpty() || it.lhs.any { cell -> cell.token != it.lhs.first().token }
            }

            return if (Runtime.getRuntime().availableProcessors() > 2 && notBoundConstraints.size > Runtime.getRuntime().availableProcessors()) {
                val factor = Flt64(notBoundConstraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
                val segment = if (factor >= 1) {
                    pow(UInt64.ten, factor).toInt()
                } else {
                    10
                }
                coroutineScope {
                    val constraintPromises = (0..(notBoundConstraints.size / segment)).map {
                        async(Dispatchers.Default) {
                            val constraints = ArrayList<Pair<List<LinearConstraintCell>, Flt64>>()
                            for (i in (it * segment) until minOf(notBoundConstraints.size, (it + 1) * segment)) {
                                val constraint = notBoundConstraints[i]
                                val lhs = ArrayList<LinearConstraintCell>()
                                var rhs = constraint.rhs
                                for (cell in constraint.lhs) {
                                    if (tokenIndexes.containsKey(cell.token)) {
                                        lhs.add(
                                            LinearConstraintCell(
                                                rowIndex = i,
                                                colIndex = tokenIndexes[cell.token]!!,
                                                coefficient = cell.coefficient.let { coefficient ->
                                                    if (coefficient.isInfinity()) {
                                                        Flt64.decimalPrecision.reciprocal()
                                                    } else if (coefficient.isNegativeInfinity()) {
                                                        -Flt64.decimalPrecision.reciprocal()
                                                    } else {
                                                        coefficient
                                                    }
                                                }
                                            )
                                        )
                                    } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                                        rhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                                    }
                                }
                                constraints.add(lhs to rhs)
                            }
                            if (memoryUseOver()) {
                                System.gc()
                            }
                            constraints
                        }
                    }

                    val lhs = ArrayList<List<LinearConstraintCell>>()
                    val signs = ArrayList<Sign>()
                    val rhs = ArrayList<Flt64>()
                    val names = ArrayList<String>()
                    val sources = ArrayList<ConstraintSource>()
                    for ((index, constraint) in notBoundConstraints.withIndex()) {
                        val (thisLhs, thisRhs) = constraintPromises[index / segment].await()[index % segment]
                        lhs.add(thisLhs)
                        signs.add(constraint.sign)
                        rhs.add(thisRhs)
                        names.add(constraint.name)
                        sources.add(ConstraintSource.Origin)
                    }
                    LinearConstraint(lhs, signs, rhs, names, sources)
                }
            } else {
                val lhs = ArrayList<List<LinearConstraintCell>>()
                val signs = ArrayList<Sign>()
                val rhs = ArrayList<Flt64>()
                val names = ArrayList<String>()
                val sources = ArrayList<ConstraintSource>()
                for ((index, constraint) in notBoundConstraints.withIndex()) {
                    val thisLhs = ArrayList<LinearConstraintCell>()
                    var thisRhs = constraint.rhs
                    for (cell in constraint.lhs) {
                        if (tokenIndexes.containsKey(cell.token)) {
                            thisLhs.add(
                                LinearConstraintCell(
                                    rowIndex = index,
                                    colIndex = tokenIndexes[cell.token]!!,
                                    coefficient = cell.coefficient.let {
                                        if (it.isInfinity() || it geq Flt64.decimalPrecision.reciprocal()) {
                                            Flt64.decimalPrecision.reciprocal()
                                        } else if (it.isNegativeInfinity() || it leq -Flt64.decimalPrecision.reciprocal()) {
                                            -Flt64.decimalPrecision.reciprocal()
                                        } else {
                                            it
                                        }
                                    }
                                )
                            )
                        } else if (fixedVariables?.containsKey(cell.token.variable) == true) {
                            thisRhs -= cell.coefficient * fixedVariables[cell.token.variable]!!
                        }
                    }
                    lhs.add(thisLhs)
                    signs.add(constraint.sign)
                    rhs.add(thisRhs)
                    names.add(constraint.name)
                    sources.add(ConstraintSource.Origin)
                }
                System.gc()
                LinearConstraint(lhs, signs, rhs, names, sources)
            }
        }

        private fun dumpObjectives(
            model: LinearMechanismModel,
            tokenIndexes: BiMap<Token, Int>,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null
        ): LinearObjective {
            val objectiveCategory = if (model.objectFunction.subObjects.size == 1) {
                model.objectFunction.subObjects.first().category
            } else {
                model.objectFunction.category
            }
            val coefficient = (0 until tokenIndexes.size).map { Flt64.zero }.toMutableList()
            var constant = Flt64.zero
            for (subObject in model.objectFunction.subObjects) {
                if (subObject.category == objectiveCategory) {
                    for (cell in subObject.cells) {
                        if (fixedVariables?.containsKey(cell.token.variable) == true) {
                            constant += cell.coefficient * fixedVariables[cell.token.variable]!!
                        } else {
                            val index = tokenIndexes[cell.token] ?: continue
                            coefficient[index] = coefficient[index] + cell.coefficient
                        }
                    }
                    constant += subObject.constant
                } else {
                    for (cell in subObject.cells) {
                        if (fixedVariables?.containsKey(cell.token.variable) == true) {
                            constant -= cell.coefficient * fixedVariables[cell.token.variable]!!
                        } else {
                            val index = tokenIndexes[cell.token] ?: continue
                            coefficient[index] = coefficient[index] - cell.coefficient
                        }
                    }
                    constant -= subObject.constant
                }
            }
            val objective = ArrayList<LinearObjectiveCell>()
            for ((_, i) in tokenIndexes) {
                objective.add(
                    LinearObjectiveCell(
                        colIndex = i,
                        coefficient = coefficient[i].let {
                            if (it.isInfinity() || it geq Flt64.decimalPrecision.reciprocal()) {
                                Flt64.decimalPrecision.reciprocal()
                            } else if (it.isNegativeInfinity() || it leq -Flt64.decimalPrecision.reciprocal()) {
                                -Flt64.decimalPrecision.reciprocal()
                            } else {
                                it
                            }
                        }
                    )
                )
            }
            return LinearObjective(objectiveCategory, objective, constant)
        }
    }

    override fun copy() = LinearTriadModel(impl.copy(), tokenIndexMap, objective.copy())
    override fun clone() = copy()

    fun normalized(): Boolean {
        return impl.normalized()
    }

    fun linearRelax(): LinearTriadModel {
        impl.linearRelax()
        return this
    }

    fun normalize(): LinearTriadModel {
        impl.normalize()
        return this
    }

    suspend fun dual(): LinearTriadModel {
        assert(normalized())
        val variables = this.constraints.indices.map {
            var lowerBound = Flt64.negativeInfinity
            var upperBound = Flt64.infinity
            when (this.objective.category) {
                ObjectCategory.Maximum ->  {
                    when (this.constraints.signs[it]) {
                        Sign.LessEqual -> {
                            // ≤ => y ≥ 0
                            lowerBound = Flt64.zero
                        }

                        Sign.GreaterEqual -> {
                            // ≥ => y ≤ 0
                            upperBound = Flt64.zero
                        }

                        else -> {}
                    }
                }

                ObjectCategory.Minimum -> {
                    when (this.constraints.signs[it]) {
                        Sign.LessEqual -> {
                            // ≤ => y ≤ 0
                            upperBound = Flt64.zero
                        }

                        Sign.GreaterEqual -> {
                            // ≥ => y ≥ 0
                            lowerBound = Flt64.zero
                        }

                        else -> {}
                    }
                }
            }

            Variable(
                index = it,
                lowerBound = lowerBound,
                upperBound = upperBound,
                type = Continuous,
                origin = null,
                name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_dual",
                initialResult = Flt64.zero
            )
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }
        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map {
                async(Dispatchers.Default) {
                    coefficients[it].map { cell ->
                        LinearConstraintCell(
                            rowIndex = it,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    }
                }
            }
            constraintPromises.awaitAll()
        }
        val signs = this.variables.map {
            if ((it.lowerBound.isNegativeInfinity() || it.lowerBound leq -Flt64.decimalPrecision.reciprocal()) && (it.upperBound.isInfinity() || it.upperBound geq Flt64.decimalPrecision.reciprocal())) {
                Sign.Equal
            } else if (it.lowerBound.isNegativeInfinity() || it.lowerBound leq -Flt64.decimalPrecision.reciprocal()) {
                // ≤ 0
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        // ≤ 0 => ≤
                        Sign.LessEqual
                    }

                    ObjectCategory.Minimum -> {
                        // ≤ 0 => ≥
                        Sign.GreaterEqual
                    }
                }
            } else if (it.upperBound.isInfinity() || it.upperBound geq Flt64.decimalPrecision.reciprocal()) {
                // ≥ 0
                when (this.objective.category) {
                    ObjectCategory.Maximum -> {
                        // ≥ 0 => ≥
                        Sign.GreaterEqual
                    }

                    ObjectCategory.Minimum -> {
                        // ≥ 0 => ≤
                        Sign.LessEqual
                    }
                }
            } else {
                Sign.Equal
            }
        }
        val rhs = this.variables.map { col ->
            this.objective.obj.find { it.colIndex == col.index }?.coefficient ?: Flt64.zero
        }
        val names = this.variables.map { "${it.name}_dual" }
        val sources = this.variables.map { ConstraintSource.Dual }

        val objective = constraints.indices.map {
            LinearObjectiveCell(
                colIndex = it,
                coefficient = this.constraints.rhs[it]
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables,
                LinearConstraint(lhs, signs, rhs, names, sources),
                "$name-dual"
            ),
            tokenIndexMap = tokenIndexMap,
            objective = LinearObjective(this.objective.category.reverse, objective)
        )
    }

    suspend fun farkasDual(): LinearTriadModel {
        assert(normalized())
        var colIndex = this.constraints.size
        val farkasVariables = ArrayList<Variable>()
        val posFarkasVariables = ArrayList<Variable>()
        val negFarkasVariables = ArrayList<Variable>()
        val slackVariables = ArrayList<Variable>()
        for (i in this.constraints.indices) {
            when (this.constraints.signs[i]) {
                Sign.LessEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    posFarkasVariables.add(variable)
                }

                Sign.GreaterEqual -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.zero,
                        type = Continuous,
                        origin = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)
                    negFarkasVariables.add(variable)
                }

                Sign.Equal -> {
                    val variable = Variable(
                        index = i,
                        lowerBound = Flt64.negativeInfinity,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_farkas",
                        initialResult = Flt64.zero
                    )
                    farkasVariables.add(variable)

                    val posSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_pos_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    val negSlack = Variable(
                        index = colIndex,
                        lowerBound = Flt64.zero,
                        upperBound = Flt64.infinity,
                        type = Continuous,
                        origin = null,
                        name = "${this.constraints.names[i].ifEmpty { "cons${i}" }}_neg_slack",
                        initialResult = Flt64.zero
                    )
                    colIndex += 1

                    slackVariables.add(posSlack)
                    slackVariables.add(negSlack)
                }
            }
        }

        val cellGroups = this.constraints.lhs.flatten().groupBy { it.colIndex }
        val coefficients = this@LinearTriadModel.variables.indices.map {
            cellGroups[it]?.map { cell -> Pair(cell.rowIndex, cell.coefficient) } ?: emptyList()
        }

        val lhs = coroutineScope {
            val constraintPromises = this@LinearTriadModel.variables.indices.map {
                async(Dispatchers.Default) {
                    coefficients[it].map { cell ->
                        LinearConstraintCell(
                            rowIndex = it,
                            colIndex = cell.first,
                            coefficient = cell.second
                        )
                    }
                }
            } + listOf(async(Dispatchers.Default) {
                this@LinearTriadModel.constraints.indices.map {
                    LinearConstraintCell(
                        rowIndex = this@LinearTriadModel.variables.size,
                        colIndex = farkasVariables[it].index,
                        coefficient = this@LinearTriadModel.constraints.rhs[it]
                    )
                }
            })
            val slackConstraintPromises = async(Dispatchers.Default) {
                var rowIndex = this@LinearTriadModel.variables.size + 1
                var i = 0
                this@LinearTriadModel.constraints.indices.mapNotNull {
                    when (this@LinearTriadModel.constraints.signs[it]) {
                        Sign.LessEqual, Sign.GreaterEqual -> {
                            null
                        }

                        Sign.Equal -> {
                            val result = listOf(
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = farkasVariables[it].index,
                                    coefficient = Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i].index,
                                    coefficient = -Flt64.one
                                ),
                                LinearConstraintCell(
                                    rowIndex = rowIndex,
                                    colIndex = slackVariables[2 * i + 1].index,
                                    coefficient = Flt64.one
                                )
                            )
                            i += 1
                            rowIndex += 1
                            result
                        }
                    }
                }
            }
            constraintPromises.awaitAll() + slackConstraintPromises.await()
        }

        val signs = this.variables.indices.map { Sign.GreaterEqual } + listOf(Sign.Equal) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    Sign.Equal
                }
            }
        }
        val rhs = this.variables.indices.map { Flt64.zero } + listOf(-Flt64.one) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    Flt64.zero
                }
            }
        }
        val names = this.variables.map { "${it.name}_farkas_dual" } + listOf("normalization") + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    "${this.constraints.names[it].ifEmpty { "cons${it}" }}_abs"
                }
            }
        }
        val sources = this.variables.map { ConstraintSource.FarkasDual } + listOf(ConstraintSource.FarkasDual) + this.constraints.indices.mapNotNull {
            when (this.constraints.signs[it]) {
                Sign.LessEqual, Sign.GreaterEqual -> {
                    null
                }

                Sign.Equal -> {
                    ConstraintSource.FarkasDual
                }
            }
        }

        val objective = posFarkasVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        } + negFarkasVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = -Flt64.one
            )
        } + slackVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = (farkasVariables + slackVariables).sortedBy { it.index },
                constraints = LinearConstraint(lhs, signs, rhs, names, sources),
                name = "$name-farkas-dual"
            ),
            tokenIndexMap = tokenIndexMap,
            objective = LinearObjective(ObjectCategory.Minimum, objective)
        )
    }

    fun feasibility(): LinearTriadModel {
        assert(normalized())
        var colIndex = this.variables.size
        val slackVariables = ArrayList<Variable>()
        val artifactVariables = ArrayList<Variable>()
        val constraints = LinearConstraint(
            lhs = this.constraints.indices.map {
                when (if (this.constraints.rhs[it] ls Flt64.zero) {
                    this.constraints.signs[it].reverse
                } else {
                    this.constraints.signs[it]
                }) {
                    Sign.LessEqual -> {
                        val slack = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack"
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    Sign.GreaterEqual -> {
                        val slack = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_slack"
                        )
                        colIndex += 1
                        val artifact = Variable(
                            colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact"
                        )
                        colIndex += 1

                        slackVariables.add(slack)
                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = slack.index,
                                coefficient = -Flt64.one
                            ),
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
                                coefficient = Flt64.one
                            )
                        )
                    }

                    Sign.Equal -> {
                        val artifact = Variable(
                            index = colIndex,
                            lowerBound = Flt64.zero,
                            upperBound = Flt64.infinity,
                            type = Continuous,
                            origin = null,
                            name = "${this.constraints.names[it].ifEmpty { "cons${it}" }}_artifact"
                        )
                        colIndex += 1

                        artifactVariables.add(artifact)
                        if (this.constraints.rhs[it] ls Flt64.zero) {
                            this.constraints.lhs[it].map { cell -> -cell }
                        } else {
                            this.constraints.lhs[it]
                        } + listOf(
                            LinearConstraintCell(
                                rowIndex = it,
                                colIndex = artifact.index,
                                coefficient = Flt64.one
                            )
                        )
                    }
                }
            },
            signs = this.constraints.indices.map {
                Sign.Equal
            },
            rhs = this.constraints.indices.map {
                abs(this.constraints.rhs[it])
            },
            names = this.constraints.indices.map {
                this.constraints.names[it].ifEmpty { "cons${it}" }
            },
            sources = this.constraints.indices.map {
                ConstraintSource.Feasibility
            }
        )

        val objective = artifactVariables.map {
            LinearObjectiveCell(
                colIndex = it.index,
                coefficient = Flt64.one
            )
        }

        return LinearTriadModel(
            impl = BasicLinearTriadModel(
                variables = this.variables + (slackVariables + artifactVariables).sortedBy { it.index },
                constraints = constraints,
                name = "$name-feasibility"
            ),
            tokenIndexMap = tokenIndexMap,
            objective = LinearObjective(ObjectCategory.Minimum, objective)
        )
    }

    override fun exportLP(writer: FileWriter): Try {
        writer.write("${objective.category}\n")
        var i = 0
        for (cell in objective.obj) {
            if (cell.coefficient eq Flt64.zero) {
                continue
            }
            val coefficient = if (i != 0) {
                if (cell.coefficient leq Flt64.zero) {
                    writer.append(" - ")
                } else {
                    writer.append(" + ")
                }
                abs(cell.coefficient)
            } else {
                cell.coefficient
            }
            if (coefficient neq Flt64.zero) {
                if (coefficient neq Flt64.one) {
                    writer.append("$coefficient ")
                }
                writer.append("${variables[cell.colIndex]}")
            }
            ++i
        }
        writer.append("\n\n")

        return when (val result = impl.exportLP(writer)) {
            is Failed -> {
                Failed(result.error)
            }

            is Ok -> {
                ok
            }
        }
    }

    override fun toString(): String {
        return name
    }
}
