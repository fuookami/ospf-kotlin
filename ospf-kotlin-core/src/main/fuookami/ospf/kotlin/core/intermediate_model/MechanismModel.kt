package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.LinearFunctionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticFunctionSymbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial as MathLinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial as MathLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Flt64LinearInequality as MathLinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequality as MathQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.inequality.le
import fuookami.ospf.kotlin.math.symbol.inequality.ge
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticPolynomial
import fuookami.ospf.kotlin.utils.memoryUseOver
import fuookami.ospf.kotlin.math.operator.pow
import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger

sealed interface MechanismModel : AutoCloseable {
    val name: String
    val constraints: List<Constraint>
    val objectFunction: Object
    val tokens: AbstractTokenTable

    override fun close() {
        tokens.close()
    }
}

interface AbstractLinearMechanismModel : MechanismModel {
    /**
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: MathLinearInequality,
        name: String? = null,
        from: Pair<IntermediateSymbol, Boolean>? = null,
    ): Try

    fun addConstraint(
        relation: MathLinearInequality,
        name: String? = null,
        from: IntermediateSymbol?,
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

interface AbstractQuadraticMechanismModel : AbstractLinearMechanismModel {
    /**
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: MathQuadraticInequality,
        name: String? = null,
        from: Pair<IntermediateSymbol, Boolean>? = null
    ): Try

    fun addConstraint(
        relation: MathQuadraticInequality,
        name: String? = null,
        from: IntermediateSymbol?
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

interface SingleObjectMechanismModel : MechanismModel {
    override val objectFunction: SingleObject<*>
}

class LinearMechanismModel(
    internal val parent: LinearMetaModel,
    override var name: String,
    constraints: List<LinearConstraint>,
    override val objectFunction: SingleObject<LinearSubObject>,
    override val tokens: AbstractTokenTable
) : AbstractLinearMechanismModel, SingleObjectMechanismModel {
    private val logger = logger()

    companion object {
        private val logger = logger()

        @Suppress("DEPRECATION")
        suspend operator fun invoke(
            metaModel: LinearMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModel> {
            logger.info { "Creating LinearMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                } else {
                    coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                }
            } else {
                LinearMechanismModel(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        LinearConstraint(
                            relation = it.inequality,
                            tokens = tokens
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        LinearSubObject(
                            category = it.category,
                            flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant),
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                (symbol as? LinearFunctionSymbol)?.let { sym ->
                    if (fixedVariables.isNullOrEmpty()) {
                        sym.register(model)
                    } else {
                        sym.register(model, fixedVariables.mapKeys { it.key })
                    }
                }

                if (dumpingStatusCallBack != null && i % 100 == 0) {
                    dumpingStatusCallBack(
                        MechanismModelDumpingStatus.dumpingSymbols(
                            ready = UInt64(i),
                            model = metaModel
                        )
                    )
                }
            }
            if (dumpingStatusCallBack != null) {
                dumpingStatusCallBack(
                    MechanismModelDumpingStatus.dumpingSymbols(
                        ready = tokens.symbols.usize,
                        model = metaModel
                    )
                )
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "LinearMechanismModel created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        @Suppress("DEPRECATION")
        private suspend fun dumpAsync(
            metaModel: LinearMetaModel,
            tokens: AbstractTokenTable,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): LinearMechanismModel {
            val factor1 = Flt64(metaModel._relationConstraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val constraints = if (factor1 >= 1) {
                val thisCompletedConstraintAmountLock = Any()
                var thisCompletedConstraintAmount = UInt64.zero
                val segment = pow(UInt64.ten, factor1).toInt()
                (0..(metaModel._relationConstraints.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._relationConstraints
                            .subList(
                                (i * segment),
                                minOf(metaModel._relationConstraints.size, (i + 1) * segment)
                            ).map {
                                LinearConstraint(
                                    relation = it.inequality,
                                    tokens = tokens
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        if (callBack != null) {
                            synchronized(thisCompletedConstraintAmountLock) {
                                thisCompletedConstraintAmount += result.usize
                                callBack(
                                    MechanismModelDumpingStatus.dumpingConstrains(
                                        ready = thisCompletedConstraintAmount,
                                        model = metaModel
                                    )
                                )
                            }
                        }
                        result
                    }
                }
            } else {
                metaModel._relationConstraints.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            LinearConstraint(
                                relation = it.inequality,
                                tokens = tokens
                            )
                        )
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            }
            val factor2 = Flt64(metaModel._subObjects.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val subObjects = if (factor2 >= 1) {
                val segment = pow(UInt64.ten, factor2).toInt()
                (0..(metaModel._subObjects.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._subObjects
                            .subList(
                                (i * segment),
                                minOf(metaModel._subObjects.size, (i + 1) * segment)
                            ).map {
                                LinearSubObject(
                                    category = it.category,
                                    flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant),
                                    tokens = tokens,
                                    name = it.name
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._subObjects.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            LinearSubObject(
                                category = it.category,
                                flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant),
                                tokens = tokens,
                                name = it.name
                            )
                        )
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            }

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return LinearMechanismModel(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.flatMap { it.await() }.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: AbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }
            }
        }
    }

    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        relation: MathLinearInequality,
        name: String?,
        from: Pair<IntermediateSymbol, Boolean>?
    ): Try {
        _constraints.add(
            LinearConstraint(
                relation = relation,
                tokens = tokens,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    @Suppress("DEPRECATION")
    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: LinearDualSolution
    ): List<MathLinearInequality> {
        val constants = constraints.foldIndexed(Flt64.zero) { _, acc, constraint ->
            acc + (dualSolution[constraint] ?: Flt64.zero) * constraint.rhs
        }
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in constraints) {
            val dual = dualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = (dualSolution[constraint] ?: Flt64.zero) * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                }
            }
        }
        val rhs = MathLinearPolynomial(
            monomials = polynomials.map { MathLinearMonomial(it.value, it.key) },
            constant = constants
        )
        return when (this.objectFunction.category) {
            ObjectCategory.Maximum -> {
                listOf((MathLinearPolynomial(listOf(MathLinearMonomial(Flt64.one, objectVariable)), Flt64.zero) le rhs).normalize())
            }

            ObjectCategory.Minimum -> {
                listOf((MathLinearPolynomial(listOf(MathLinearMonomial(Flt64.one, objectVariable)), Flt64.zero) ge rhs).normalize())
            }
        }
    }

    @Suppress("DEPRECATION")
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: LinearDualSolution
    ): List<MathLinearInequality> {
        var value = Flt64.zero
        var constants = Flt64.zero
        val polynomials = HashMap<AbstractVariableItem<*, *>, Flt64>()
        for (constraint in constraints) {
            val dual = farkasDualSolution[constraint] ?: continue
            if (dual eq Flt64.zero) {
                continue
            }

            value += dual * constraint.rhs
            constants += dual * constraint.rhs
            for (cell in constraint.lhs) {
                val variable = cell.token.variable
                if (variable in fixedVariables) {
                    val coefficient = dual * cell.coefficient
                    if (coefficient neq Flt64.zero) {
                        polynomials[variable] = (polynomials[variable] ?: Flt64.zero) - coefficient
                    }
                    value -= dual * cell.coefficient * fixedVariables[variable]!!
                }
            }
        }
        if (value ls Flt64.zero) {
            logger.warn { "farkas dual solution is infeasible, value = ${value}, set negative" }
            constants *= -Flt64.one
            polynomials.replaceAll { _, v -> -v }
        }
        val lhs = MathLinearPolynomial(
            monomials = polynomials.map { MathLinearMonomial(it.value, it.key) },
            constant = constants
        )
        return listOf((lhs le Flt64.zero).normalize())
    }

    override fun close() {
        _constraints.clear()
        super<AbstractLinearMechanismModel>.close()
    }

    override fun toString(): String {
        return name
    }
}

class QuadraticMechanismModel(
    internal val parent: QuadraticMetaModel,
    override var name: String,
    constraints: List<QuadraticConstraint>,
    override val objectFunction: SingleObject<QuadraticSubObject>,
    override val tokens: AbstractTokenTable
) : AbstractQuadraticMechanismModel, SingleObjectMechanismModel {
    private val logger = logger()

    companion object {
        private val logger = logger()

        @Suppress("DEPRECATION")
        suspend operator fun invoke(
            metaModel: QuadraticMetaModel,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModel> {
            logger.info { "Creating QuadraticMechanismModel for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> {
                    result.value
                }

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                } else {
                    coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }
                }
            } else {
                QuadraticMechanismModel(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = metaModel._relationConstraints.map {
                        QuadraticConstraint(
                            relation = it.inequality,
                            tokens = tokens
                        )
                    }.toMutableList(),
                    objectFunction = SingleObject(metaModel.objectCategory, metaModel._subObjects.map {
                        QuadraticSubObject(
                            category = it.category,
                            flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant).toQuadraticFlattenData(),
                            tokens = tokens,
                            name = it.name
                        )
                    }),
                    tokens = tokens
                )
            }
            System.gc()

            logger.trace { "Registering symbols for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                (symbol as? LinearFunctionSymbol)?.let { sym ->
                    if (fixedVariables.isNullOrEmpty()) {
                        sym.register(model)
                    } else {
                        sym.register(model, fixedVariables.mapKeys { it.key })
                    }
                }
                (symbol as? QuadraticFunctionSymbol)?.let { sym ->
                    if (fixedVariables.isNullOrEmpty()) {
                        sym.register(model)
                    } else {
                        sym.register(model, fixedVariables.mapKeys { it.key })
                    }
                }

                if (dumpingStatusCallBack != null && i % 100 == 0) {
                    dumpingStatusCallBack(
                        MechanismModelDumpingStatus.dumpingSymbols(
                            ready = UInt64(i),
                            model = metaModel
                        )
                    )
                }
            }
            if (dumpingStatusCallBack != null) {
                dumpingStatusCallBack(
                    MechanismModelDumpingStatus.dumpingSymbols(
                        ready = tokens.symbols.usize,
                        model = metaModel
                    )
                )
            }
            logger.trace { "Symbols registered for $metaModel" }

            logger.info { "QuadraticMechanismModel created for $metaModel" }
            System.gc()
            return Ok(model)
        }

        @Suppress("DEPRECATION")
        private suspend fun dumpAsync(
            metaModel: QuadraticMetaModel,
            tokens: AbstractTokenTable,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): QuadraticMechanismModel {
            val factor1 = Flt64(metaModel._relationConstraints.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val constraints = if (factor1 >= 1) {
                val thisCompletedConstraintAmountLock = Any()
                var thisCompletedConstraintAmount = UInt64.zero
                val segment = pow(UInt64.ten, factor1).toInt()
                (0..(metaModel._relationConstraints.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._relationConstraints
                            .subList(
                                (i * segment),
                                minOf(metaModel._relationConstraints.size, (i + 1) * segment)
                            ).map {
                                QuadraticConstraint(
                                    relation = it.inequality,
                                    tokens = tokens
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        if (callBack != null) {
                            synchronized(thisCompletedConstraintAmountLock) {
                                thisCompletedConstraintAmount += result.usize
                                callBack(
                                    MechanismModelDumpingStatus.dumpingConstrains(
                                        ready = thisCompletedConstraintAmount,
                                        model = metaModel
                                    )
                                )
                            }
                        }
                        result
                    }
                }
            } else {
                metaModel._relationConstraints.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            QuadraticConstraint(
                                relation = it.inequality,
                                tokens = tokens
                            )
                        )
                        result
                    }
                }
            }
            val factor2 = Flt64(metaModel._subObjects.size / (Runtime.getRuntime().availableProcessors() - 1)).lg()!!.floor().toUInt64().toInt()
            val subObjects = if (factor2 >= 1) {
                val segment = pow(UInt64.ten, factor2).toInt()
                (0..(metaModel._subObjects.size / segment)).map { i ->
                    scope.async(Dispatchers.Default) {
                        val result = metaModel._subObjects
                            .subList(
                                (i * segment),
                                minOf(metaModel._subObjects.size, (i + 1) * segment)
                            ).map {
                                QuadraticSubObject(
                                    category = it.category,
                                    flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant).toQuadraticFlattenData(),
                                    tokens = tokens,
                                    name = it.name
                                )
                            }
                        if (memoryUseOver()) {
                            System.gc()
                        }
                        result
                    }
                }
            } else {
                metaModel._subObjects.map {
                    scope.async(Dispatchers.Default) {
                        val result = listOf(
                            QuadraticSubObject(
                                category = it.category,
                                flattenData = LinearFlattenData(it.polynomial.monomials, it.polynomial.constant).toQuadraticFlattenData(),
                                tokens = tokens,
                                name = it.name
                            )
                        )
                        result
                    }
                }
            }

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return QuadraticMechanismModel(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints.flatMap { it.await() }.toMutableList(),
                objectFunction = SingleObject(metaModel.objectCategory, subObjects.flatMap { it.await() }),
                tokens = tokens
            )
        }

        private suspend fun unfold(
            tokens: AbstractMutableTokenTable,
            fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>? = null,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable> {
            return when (tokens) {
                is MutableTokenTable -> {
                    val temp = tokens.copy() as MutableTokenTable
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(TokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable -> {
                    val temp = tokens.copy() as ConcurrentMutableTokenTable
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = fixedVariables?.mapKeys { it.key },
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(ConcurrentTokenTable(temp))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }
            }
        }
    }

    private val _constraints = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints by ::_constraints

    override fun addConstraint(
        relation: MathLinearInequality,
        name: String?,
        from: Pair<IntermediateSymbol, Boolean>?
    ): Try {
        // Convert MathLinearInequality to MathQuadraticInequality
        val quadraticInequality = MathQuadraticInequality(
            relation.lhs.toQuadraticPolynomial(),
            relation.rhs.toQuadraticPolynomial(),
            relation.comparison
        )
        return addConstraint(
            relation = quadraticInequality,
            name = name,
            from = from
        )
    }

    override fun addConstraint(
        relation: MathQuadraticInequality,
        name: String?,
        from: Pair<IntermediateSymbol, Boolean>?
    ): Try {
        _constraints.add(
            QuadraticConstraint(
                relation = relation,
                tokens = tokens,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )
        )
        return ok
    }

    @Suppress("DEPRECATION", "UNUSED_PARAMETER")
    fun generateOptimalCut(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        dualSolution: QuadraticDualSolution,
    ): Ret<List<MathLinearInequality>> {
        TODO("not implemented yet")
    }

    @Suppress("UNUSED_PARAMETER")
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
        farkasDualSolution: QuadraticDualSolution,
    ): Ret<List<MathLinearInequality>> {
        TODO("not implemented yet")
    }

    override fun close() {
        _constraints.clear()
        super<AbstractQuadraticMechanismModel>.close()
    }

    override fun toString(): String {
        return name
    }
}




