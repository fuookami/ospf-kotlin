/**
 * 机制模型
 * Mechanism model
*/
package fuookami.ospf.kotlin.core.model.mechanism

import kotlinx.coroutines.*
import org.apache.logging.log4j.kotlin.logger
import fuookami.ospf.kotlin.core.error.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.usize
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 机制模型密封接口
 * Sealed interface for mechanism models
 *
 * 机制模型是从元模型展开后的求解就绪模型，包含约束列表、目标函数和符号表。
 * A mechanism model is a solver-ready model unfolded from a meta model,
 * containing constraint list, objective function, and token table.
 *
 * @param V 数值类型 / The number type
 * @property name 模型名称 / Model name
 * @property constraints 约束列表 / Constraint list
 * @property objectFunction 目标函数 / Objective function
 * @property tokens 符号表 / Token table
*/
sealed interface MechanismModel<V> : AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val name: String
    val constraints: List<Constraint<V, *>>
    val objectFunction: Object
    val tokens: AbstractTokenTable<V>

    override fun close() {
        tokens.close()
    }
}

/**
 * 线性机制模型抽象接口
 * Abstract linear mechanism model interface
 *
 * 支持添加线性不等式约束。
 * Supports adding linear inequality constraints.
 *
 * @param V 数值类型 / The number type
*/
interface AbstractLinearMechanismModel<V> : MechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 使用数学 LinearInequality 添加约束
     * Add constraint using math LinearInequality
     *
     * @param relation 线性不等式 / Linear inequality
     * @param name 约束名称 / Constraint name
     * @param from 来源中间符号及其惰性标记 / Origin intermediate symbol and its lazy flag
     * @return 添加结果 / Result of adding the constraint
    */
    fun addConstraint(
        relation: LinearInequality<V>,
        name: String? = null,
        from: Pair<IntermediateSymbol<out V>, Boolean>? = null,
    ): Try

    /**
     * 使用数学 LinearInequality 添加约束（简化来源参数）
     * Add constraint using math LinearInequality (simplified from parameter)
     *
     * @param relation 线性不等式 / Linear inequality
     * @param name 约束名称 / Constraint name
     * @param from 来源中间符号 / Origin intermediate symbol
     * @return 添加结果 / Result of adding the constraint
    */
    fun addConstraint(
        relation: LinearInequality<V>,
        name: String? = null,
        from: IntermediateSymbol<out V>?,
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

/**
 * 二次机制模型抽象接口
 * Abstract quadratic mechanism model interface
 *
 * 扩展线性机制模型，支持添加二次不等式约束。
 * Extends linear mechanism model, supports adding quadratic inequality constraints.
 *
 * @param V 数值类型 / The number type
*/
interface AbstractQuadraticMechanismModel<V> : AbstractLinearMechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 使用数学 QuadraticInequality 添加约束
     * Add constraint using math QuadraticInequality
     *
     * @param relation 二次不等式 / Quadratic inequality
     * @param name 约束名称 / Constraint name
     * @param from 来源中间符号及其惰性标记 / Origin intermediate symbol and its lazy flag
     * @return 添加结果 / Result of adding the constraint
    */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String? = null,
        from: Pair<IntermediateSymbol<out V>, Boolean>? = null
    ): Try

    /**
     * 使用数学 QuadraticInequality 添加约束（简化来源参数）
     * Add constraint using math QuadraticInequality (simplified from parameter)
     *
     * @param relation 二次不等式 / Quadratic inequality
     * @param name 约束名称 / Constraint name
     * @param from 来源中间符号 / Origin intermediate symbol
     * @return 添加结果 / Result of adding the constraint
    */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String? = null,
        from: IntermediateSymbol<out V>?
    ): Try {
        return addConstraint(
            relation = relation,
            name = name,
            from = from?.let { it to false }
        )
    }
}

/**
 * 单目标机制模型接口
 * Single-objective mechanism model interface
 *
 * @param V 数值类型 / The number type
 * @property objectFunction 单目标函数 / Single objective function
*/
interface SingleObjectMechanismModel<V> : MechanismModel<V> where V : RealNumber<V>, V : NumberField<V> {
    override val objectFunction: SingleObject<SubObject<V>>
}

/**
 * 校验按 ID 查找对偶值的映射 / Validate dual-by-id mapping
 *
 * 检测重复约束名称和不匹配的对偶值名称，并记录警告日志。
 * Detects duplicate constraint names and unmatched dual value names, logging warnings.
 *
 * @param constraints 约束列表 / Constraint list
 * @param dualById 按约束名称索引的对偶值映射 / Dual value mapping indexed by constraint name
 * @param log 日志记录器 / Logger instance
*/
private fun validateDualById(
    constraints: List<Constraint<*, *>>,
    dualById: Map<String, *>,
    log: org.apache.logging.log4j.kotlin.KotlinLogger
) {
    // Detect duplicate constraint names - multiple constraints sharing the same name
    // would silently reuse the same dual value from the by-id map.
    // 检测重复约束名称 - 同名约束会静默复用 by-id 映射中的同一对偶值。
    val nameCounts = HashMap<String, Int>()
    for (c in constraints) {
        nameCounts[c.name] = (nameCounts[c.name] ?: 0) + 1
    }
    for ((name, count) in nameCounts) {
        if (count > 1) {
            log.warn { "Duplicate constraint name '$name' appears $count times in model; by_id lookup will use the same dual value for all" }
        }
    }
    // Detect names in dualById that don't match any constraint - likely a caller error.
    // 检测 dualById 中不匹配任何约束的名称 - 可能是调用方错误。
    val constraintNames = nameCounts.keys
    for (name in dualById.keys) {
        if (name !in constraintNames) {
            log.warn { "dualSolutionById contains name '$name' which does not match any constraint in the model; it will be ignored" }
        }
    }
}

/**
 * 从线性元模型构建线性约束实现列表。
 * Build a list of linear constraint implementations from a linear meta model.
 *
 * 遍历元模型中的所有关系约束，将其扁平化数据与符号表组合为 LinearConstraintImpl 实例。
 * 遇到第一个错误时立即返回失败。
 * Iterates over all relation constraints in the meta model, combining their flattened data
 * with the token table to produce LinearConstraintImpl instances.
 * Returns failure immediately on the first error.
 *
 * @param V 数值类型 / The number type
 * @param metaModel 线性元模型 / The linear meta model
 * @param tokens 符号表 / The token table
 * @return 包含线性约束列表的结果，或错误 / Result containing the mutable list of linear constraints, or an error
*/
private fun <V> buildConstraints(
    metaModel: LinearMetaModel<V>,
    tokens: AbstractTokenTable<V>
): Ret<MutableList<LinearConstraintImpl<V>>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = ArrayList<LinearConstraintImpl<V>>()
    for (constraint in metaModel._relationConstraints) {
        val flattenData = when (val result = constraint.flattenData()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        when (val result = LinearConstraintImpl(
            relation = LinearRelationImpl(flattenData, constraint.sign),
            tokens = tokens,
            converter = metaModel.converter,
            lazy = constraint.lazy,
            name = constraint.name,
            origin = constraint
        )) {
            is Ok -> constraints.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(constraints)
}

/**
 * 从二次元模型构建二次约束实现列表。
 * Build a list of quadratic constraint implementations from a quadratic meta model.
 *
 * 遍历元模型中的所有关系约束，将其扁平化数据与符号表组合为 QuadraticConstraintImpl 实例。
 * 遇到第一个错误时立即返回失败。
 * Iterates over all relation constraints in the meta model, combining their flattened data
 * with the token table to produce QuadraticConstraintImpl instances.
 * Returns failure immediately on the first error.
 *
 * @param V 数值类型 / The number type
 * @param metaModel 二次元模型 / The quadratic meta model
 * @param tokens 符号表 / The token table
 * @return 包含二次约束列表的结果，或错误 / Result containing the mutable list of quadratic constraints, or an error
*/
private fun <V> buildConstraints(
    metaModel: QuadraticMetaModel<V>,
    tokens: AbstractTokenTable<V>
): Ret<MutableList<QuadraticConstraintImpl<V>>> where V : RealNumber<V>, V : NumberField<V> {
    val constraints = ArrayList<QuadraticConstraintImpl<V>>()
    for (constraint in metaModel._relationConstraints) {
        when (val result = QuadraticConstraintImpl(
            relation = QuadraticRelationImpl(constraint.flattenData, constraint.sign),
            tokens = tokens,
            converter = metaModel.converter,
            lazy = constraint.lazy,
            name = constraint.name,
            origin = constraint
        )) {
            is Ok -> constraints.add(result.value)
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return Ok(constraints)
}

/**
 * 线性机制模型
 * Linear mechanism model
 *
 * 从线性元模型展开后的求解就绪模型，包含线性约束和单目标函数。
 * A solver-ready model unfolded from a linear meta model, containing linear constraints and single objective.
 *
 * @param V 数值类型 / The number type
 * @property parent 父元模型 / Parent meta model
 * @property name 模型名称 / Model name
 * @param constraints 线性约束列表 / Linear constraint list
 * @property objectFunction 单目标函数 / Single objective function
 * @property tokens 符号表 / Token table
*/
class LinearMechanismModel<V>(
    internal val parent: LinearMetaModel<V>,
    override var name: String,
    constraints: List<LinearConstraintImpl<V>>,
    override val objectFunction: SingleObject<LinearSubObject<V>>,
    override val tokens: AbstractTokenTable<V>
) : BasicMechanismModel<V>(name, tokens), AbstractLinearMechanismModel<V>, SingleObjectMechanismModel<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val logger = logger()

    /**
     * 约束存储。从 BasicMechanismModel 继承查询辅助方法（numVariables）。
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
    */
    private val _constraints: MutableList<LinearConstraintImpl<V>> = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints: List<Constraint<V, *>> get() = _constraints
    internal val linearConstraints: List<LinearConstraintImpl<V>> get() = _constraints

    companion object {
        private val logger = logger()

        /**
         * V 类型工厂方法：从 LinearMetaModel<V> 创建 LinearMechanismModel<V>。
         * 使用 V 类型 SubObject 伴随对象重载和 IntoValue<V> 转换器。
         * V-generic factory: create LinearMechanismModel<V> from LinearMetaModel<V>.
         * Uses the V-generic SubObject companion overload with IntoValue<V> converter.
        */
        suspend operator fun <V> invoke(
            metaModel: LinearMetaModel<V>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            logger.info { "Creating LinearMechanismModel<V> for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                toFlt64 = metaModel.converter::fromValue,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    when (val result = runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                } else {
                    when (val result = coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            } else {
                val constraints = when (val result = buildConstraints(
                    metaModel = metaModel,
                    tokens = tokens
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                LinearMechanismModel<V>(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = constraints,
                    objectFunction = SingleObject(metaModel.objectCategory, buildLinearObjectiveSubObjects(metaModel, tokens)),
                    tokens = tokens
                )
            }
            MemoryCleanupPolicy.cleanupAfterModelBuilt()

            logger.trace { "Registering function symbol constraints for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                val result = when (symbol) {
                    is MathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    else -> ok
                }
                when (result) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
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
            logger.trace { "Function symbol constraints registered for $metaModel" }

            logger.info { "LinearMechanismModel<V> created for $metaModel" }
            MemoryCleanupPolicy.cleanupAfterSymbolRegistration()
            return Ok(model)
        }

        /**
         * Asynchronously dumps the linear mechanism model parts (constraints and sub-objectives).
         * 异步转储线性机制模型部件（约束和子目标）。
         *
         * @param metaModel The linear meta model / 线性元模型
         * @param tokens The token table / 符号表
         * @param scope Coroutine scope for async execution / 用于异步执行的协程作用域
         * @param callBack Dumping status callback / 转储状态回调
         * @return The constructed linear mechanism model, or error / 构建的线性机制模型，或错误
        */
        private suspend fun <V> dumpAsync(
            metaModel: LinearMetaModel<V>,
            tokens: AbstractTokenTable<V>,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<LinearMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            @Suppress("UNUSED_PARAMETER")
            val unusedScope = scope
            val constraints = when (val result = buildConstraints(
                metaModel = metaModel,
                tokens = tokens
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val subObjects = buildLinearObjectiveSubObjects(metaModel, tokens)

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return Ok(LinearMechanismModel<V>(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints,
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            ))
        }

        /**
         * Unfolds a mutable token table into an immutable token table by registering symbols and fixed values.
         * 通过注册符号和固定值将可变符号表展开为不可变符号表。
         *
         * @param tokens The mutable token table to unfold / 要展开的可变符号表
         * @param fixedVariables Variables fixed in the sub-problem and their values / 子问题中固定的变量及其值
         * @param toFlt64 Conversion function from V to Flt64 / 从 V 到 Flt64 的转换函数
         * @param callBack Registration status callback / 注册状态回调
         * @return The unfolded immutable token table, or error / 展开后的不可变符号表，或错误
        */
        private suspend fun <V> unfold(
            tokens: AbstractMutableTokenTable<V>,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            toFlt64: (V) -> Flt64,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
            return when (tokens) {
                is MutableTokenTable<V> -> {
                    val temp = copyMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(mechanismTokenTableAs<V, Flt64>(TokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable<V> -> {
                    val temp = copyConcurrentMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(mechanismTokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                else -> {
                    Failed(Err(ErrorCode.ApplicationError, "Unsupported token table type: ${tokens::class.simpleName}"))
                }
            }
        }
    }

    override fun addConstraint(
        relation: LinearInequality<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toLinearFlattenData().getOrElse { return Failed(Err(ErrorCode.IllegalArgument, it.message ?: "Failed to flatten linear inequality")) }
        val constraint = when (val result = LinearConstraintImpl(
                relation = LinearRelationImpl(flattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        _constraints.add(constraint)
        return ok
    }

    /**
     * 生成最优性 cut / Generate optimality cut
     *
     * 基于对偶解为 Benders 分解生成最优性割平面。
     * Generates optimality cuts for Benders decomposition based on the dual solution.
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolution 对偶解，约束到对偶值的映射 / Dual solution, mapping from constraint to dual value
     * @return 线性不等式列表 / List of linear inequalities representing the cut
    */
    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        return buildLinearOptimalCut(
            constraints = linearConstraints,
            objectCategory = this.objectFunction.category,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one
        )
    }

    /**
     * 生成可行性 cut / Generate feasibility cut
     *
     * 基于 Farkas 对偶解为 Benders 分解生成可行性割平面。
     * Generates feasibility cuts for Benders decomposition based on the Farkas dual solution.
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolution Farkas 对偶解，约束到对偶值的映射 / Farkas dual solution, mapping from constraint to dual value
     * @return 线性不等式列表 / List of linear inequalities representing the cut
    */
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: Map<Constraint<V, Linear>, V>
    ): List<LinearInequality<V>> {
        return buildLinearFeasibleCut(
            constraints = linearConstraints,
            fixedVariables = fixedVariables,
            farkasDualSolution = farkasDualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one,
            logger = logger
        )
    }

    /**
     * 将线性不等式 cut 转换为 Flt64 类型 / Convert a linear inequality cut to Flt64 type
     *
     * 将线性不等式中的系数从 V 类型转换为 Flt64 类型。
     * Converts the coefficients of a linear inequality from type V to Flt64.
     *
     * @param cut 待转换的线性不等式 / The linear inequality to convert
     * @return Flt64 类型的线性不等式 / The linear inequality with Flt64 coefficients
    */
    private fun toFlt64LinearCut(cut: LinearInequality<V>): LinearInequality<Flt64> {
        return LinearInequality(
            lhs = LinearPolynomial(
                monomials = cut.lhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                constant = parent.converter.fromValue(cut.lhs.constant)
            ),
            rhs = LinearPolynomial(
                monomials = cut.rhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                constant = parent.converter.fromValue(cut.rhs.constant)
            ),
            comparison = cut.comparison,
            name = cut.name,
            displayName = cut.displayName
        ).normalize()
    }

    /**
     * 按约束名称生成最优性 cut / Generate optimality cut by constraint name
     *
     * 根据 by-id 映射中的对偶值查找对应约束并委托给 [generateOptimalCut]。
     * Looks up constraints by name from the by-id dual mapping and delegates to [generateOptimalCut].
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolutionById 按约束名称索引的对偶解 / Dual solution indexed by constraint name
     * @return 线性不等式列表 / List of linear inequalities representing the cut
    */
    internal fun generateOptimalCutById(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        validateDualById(linearConstraints, dualSolutionById, logger)
        val dualSolution: Map<Constraint<V, Linear>, V> = buildMap {
            for (constraint in linearConstraints) {
                val dual = dualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 按约束名称生成可行性 cut / Generate feasibility cut by constraint name
     *
     * 根据 by-id 映射中的 Farkas 对偶值查找对应约束并委托给 [generateFeasibleCut]。
     * Looks up constraints by name from the by-id Farkas dual mapping and delegates to [generateFeasibleCut].
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById 按约束名称索引的 Farkas 对偶解 / Farkas dual solution indexed by constraint name
     * @return 线性不等式列表 / List of linear inequalities representing the cut
    */
    internal fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, V>
    ): List<LinearInequality<V>> {
        val zero = parent.converter.zero
        validateDualById(linearConstraints, farkasDualSolutionById, logger)
        val dualSolution: Map<Constraint<V, Linear>, V> = buildMap {
            for (constraint in linearConstraints) {
                val dual = farkasDualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateFeasibleCut(fixedVariables, dualSolution)
    }

    /**
     * 生成 Flt64 最优性 cut / Generate Flt64 optimality cut
     *
     * 基于 Flt64 类型对偶解生成最优性割平面。对偶值从求解器原生 Flt64 类型转换后委托给 [generateOptimalCut]，
     * 返回值也转换为 Flt64。
     * Generates optimality cuts from Flt64 dual solution. Converts dual values from solver
     * raw Flt64 type, delegates to [generateOptimalCut], and converts the result back to Flt64.
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolution Flt64 类型对偶解 / Flt64 dual solution mapping from constraint to dual value
     * @return Flt64 线性不等式列表 / List of Flt64 linear inequalities representing the cut
    */
    fun generateFlt64OptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<Flt64>> {
        // 求解器边界：对偶解来自求解器原生类型。 / Solver boundary: dual solution from solver raw type.
        val dualByConstraint: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val flt64Constraint = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = dualSolution[flt64Constraint] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateOptimalCut(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualByConstraint
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * 生成 Flt64 可行性 cut / Generate Flt64 feasibility cut
     *
     * 基于 Flt64 类型 Farkas 对偶解生成可行性割平面。对偶值从求解器原生 Flt64 类型转换后委托给 [generateFeasibleCut]，
     * 返回值也转换为 Flt64。
     * Generates feasibility cuts from Flt64 Farkas dual solution. Converts dual values from
     * solver raw Flt64 type, delegates to [generateFeasibleCut], and converts the result back to Flt64.
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolution Flt64 类型 Farkas 对偶解 / Flt64 Farkas dual solution mapping from constraint to dual value
     * @return Flt64 线性不等式列表 / List of Flt64 linear inequalities representing the cut
    */
    fun generateFlt64FeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: Map<Constraint<Flt64, Linear>, Flt64>
    ): List<LinearInequality<Flt64>> {
        // 求解器边界：Farkas 对偶解来自求解器原生类型。 / Solver boundary: Farkas dual solution from solver raw type.
        val dualByConstraint: MutableMap<Constraint<V, Linear>, V> = LinkedHashMap()
        for (constraint in linearConstraints) {
            val flt64Constraint = SolverBoundaryCasts.linearConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[flt64Constraint] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }

        return generateFeasibleCut(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualByConstraint
        ).map { toFlt64LinearCut(it) }
    }

    /**
     * 从求解器原始对偶输出生成最优 Benders cut。
     * Generate optimal Benders cut from raw solver dual output.
     *
     * 该求解器边界入口接收原始对偶值，并通过 [LinearTriadModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64OptimalCut]。
     * This solver-boundary entry accepts raw dual values, maps them back to
     * Constraint objects through [LinearTriadModelView.tidyDualSolution], then
     * delegates to [generateFlt64OptimalCut].
     *
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * 求解器边界：dualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: dualValues and return type are Flt64 because they represent raw solver output.
    */
    internal fun generateOptimalCutFromOutput(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<Flt64>> {
        val dualSolution = triadModel.tidyDualSolution(dualValues)
        return generateFlt64OptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 从求解器原始 Farkas 对偶输出生成可行 Benders cut。
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * 该求解器边界入口接收原始 Farkas 对偶值，并通过 [LinearTriadModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64FeasibleCut]。
     * This solver-boundary entry accepts raw Farkas dual values, maps them back
     * to Constraint objects through [LinearTriadModelView.tidyDualSolution], then
     * delegates to [generateFlt64FeasibleCut].
     *
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param farkasDualValues raw Farkas dual values from the solver output
     * @param triadModel      the LinearTriadModel containing origin mapping
     * @return list of linear cuts
     *
     * 求解器边界：farkasDualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent raw solver output.
    */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<Flt64>,
        triadModel: LinearTriadModelView
    ): List<LinearInequality<Flt64>> {
        val farkasDualSolution = triadModel.tidyDualSolution(farkasDualValues)
        return generateFlt64FeasibleCut(fixedVariables, farkasDualSolution)
    }

    /**
     * 约束数量 / Number of constraints
     *
     * 返回模型中当前存储的线性约束总数。
     * Returns the total number of linear constraints currently stored in the model.
    */
    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}

/**
 * 二次机制模型
 * Quadratic mechanism model
 *
 * 从二次元模型展开后的求解就绪模型，包含二次约束和单目标函数。
 * A solver-ready model unfolded from a quadratic meta model, containing quadratic constraints and single objective.
 *
 * @param V 数值类型 / The number type
 * @property parent 父元模型 / Parent meta model
 * @property name 模型名称 / Model name
 * @param constraints 二次约束列表 / Quadratic constraint list
 * @property objectFunction 单目标函数 / Single objective function
 * @property tokens 符号表 / Token table
*/
class QuadraticMechanismModel<V>(
    internal val parent: QuadraticMetaModel<V>,
    override var name: String,
    constraints: List<QuadraticConstraintImpl<V>>,
    override val objectFunction: SingleObject<QuadraticSubObject<V>>,
    override val tokens: AbstractTokenTable<V>
) : BasicMechanismModel<V>(name, tokens), AbstractQuadraticMechanismModel<V>, SingleObjectMechanismModel<V>
        where V : RealNumber<V>, V : NumberField<V> {
    private val logger = logger()

    /**
     * 约束存储。从 BasicMechanismModel 继承查询辅助方法（numVariables）。
     * Constraints storage. Inherits query helpers (numVariables) from BasicMechanismModel.
    */
    private val _constraints: MutableList<QuadraticConstraintImpl<V>> = constraints.toMutableList()
    internal val concurrent by parent.configuration::concurrent
    override val constraints: List<Constraint<V, *>> get() = _constraints
    internal val quadraticConstraints: List<QuadraticConstraintImpl<V>> get() = _constraints

    companion object {
        private val logger = logger()

        /**
         * V 类型工厂方法：从 QuadraticMetaModel<V> 创建 QuadraticMechanismModel<V>。
         * 使用 V 类型 SubObject 伴随对象重载和 IntoValue<V> 转换器。
         * V-generic factory: create QuadraticMechanismModel<V> from QuadraticMetaModel<V>.
         * Uses the V-generic SubObject companion overload with IntoValue<V> converter.
        */
        suspend operator fun <V> invoke(
            metaModel: QuadraticMetaModel<V>,
            concurrent: Boolean? = null,
            blocking: Boolean? = null,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            registrationStatusCallBack: RegistrationStatusCallBack? = null,
            dumpingStatusCallBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            logger.info { "Creating QuadraticMechanismModel<V> for $metaModel" }

            logger.trace { "Unfolding tokens for $metaModel" }
            val tokens = when (val result = unfold(
                tokens = metaModel.tokens,
                fixedVariables = fixedVariables,
                toFlt64 = metaModel.converter::fromValue,
                callBack = registrationStatusCallBack
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            logger.trace { "Tokens unfolded for $metaModel" }

            val model = if (Runtime.getRuntime().availableProcessors() > 2 && concurrent ?: metaModel.configuration.concurrent) {
                if (blocking ?: metaModel.configuration.dumpBlocking) {
                    when (val result = runBlocking {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                } else {
                    when (val result = coroutineScope {
                        dumpAsync(
                            metaModel = metaModel,
                            tokens = tokens,
                            scope = this,
                            callBack = dumpingStatusCallBack
                        )
                    }) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
            } else {
                val constraints = when (val result = buildConstraints(
                    metaModel = metaModel,
                    tokens = tokens
                )) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                QuadraticMechanismModel<V>(
                    parent = metaModel,
                    name = metaModel.name,
                    constraints = constraints,
                    objectFunction = SingleObject(metaModel.objectCategory, buildQuadraticObjectiveSubObjects(metaModel, tokens)),
                    tokens = tokens
                )
            }
            MemoryCleanupPolicy.cleanupAfterModelBuilt()

            logger.trace { "Registering function symbol constraints for $metaModel" }
            for ((i, symbol) in tokens.symbols.withIndex()) {
                val result = when (symbol) {
                    is QuadraticMathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    is MathFunctionSymbolBase<*> -> symbol.registerConstraintsUnchecked(model)
                    else -> ok
                }
                when (result) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
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
            logger.trace { "Function symbol constraints registered for $metaModel" }

            logger.info { "QuadraticMechanismModel<V> created for $metaModel" }
            MemoryCleanupPolicy.cleanupAfterSymbolRegistration()
            return Ok(model)
        }

        /**
         * Asynchronously dumps the quadratic mechanism model parts (constraints and sub-objectives).
         * 异步转储二次机制模型部件（约束和子目标）。
         *
         * @param metaModel The quadratic meta model / 二次元模型
         * @param tokens The token table / 符号表
         * @param scope Coroutine scope for async execution / 用于异步执行的协程作用域
         * @param callBack Dumping status callback / 转储状态回调
         * @return The constructed quadratic mechanism model, or error / 构建的二次机制模型，或错误
        */
        private suspend fun <V> dumpAsync(
            metaModel: QuadraticMetaModel<V>,
            tokens: AbstractTokenTable<V>,
            scope: CoroutineScope,
            callBack: MechanismModelDumpingStatusCallBack? = null
        ): Ret<QuadraticMechanismModel<V>> where V : RealNumber<V>, V : NumberField<V> {
            @Suppress("UNUSED_PARAMETER")
            val unusedScope = scope
            val constraints = when (val result = buildConstraints(
                metaModel = metaModel,
                tokens = tokens
            )) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val subObjects = buildQuadraticObjectiveSubObjects(metaModel, tokens)

            if (callBack != null) {
                callBack(
                    MechanismModelDumpingStatus.dumpingConstrains(
                        ready = metaModel.constraints.usize,
                        model = metaModel
                    )
                )
            }

            return Ok(QuadraticMechanismModel<V>(
                parent = metaModel,
                name = metaModel.name,
                constraints = constraints,
                objectFunction = SingleObject(metaModel.objectCategory, subObjects),
                tokens = tokens
            ))
        }

        /**
         * Unfolds a mutable token table into an immutable token table by registering symbols and fixed values.
         * 通过注册符号和固定值将可变符号表展开为不可变符号表。
         *
         * @param tokens The mutable token table to unfold / 要展开的可变符号表
         * @param fixedVariables Variables fixed in the sub-problem and their values / 子问题中固定的变量及其值
         * @param toFlt64 Conversion function from V to Flt64 / 从 V 到 Flt64 的转换函数
         * @param callBack Registration status callback / 注册状态回调
         * @return The unfolded immutable token table, or error / 展开后的不可变符号表，或错误
        */
        private suspend fun <V> unfold(
            tokens: AbstractMutableTokenTable<V>,
            fixedVariables: Map<AbstractVariableItem<*, *>, V>? = null,
            toFlt64: (V) -> Flt64,
            callBack: RegistrationStatusCallBack? = null
        ): Ret<AbstractTokenTable<V>> where V : RealNumber<V>, V : NumberField<V> {
            return when (tokens) {
                is MutableTokenTable<V> -> {
                    val temp = copyMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(mechanismTokenTableAs<V, Flt64>(TokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                is ConcurrentMutableTokenTable<V> -> {
                    val temp = copyConcurrentMutableTokenTableAsFlt64(tokens)
                    when (val result = tokens.symbols.register(
                        tokenTable = temp,
                        fixedValues = toSolverFixedValues(fixedVariables, toFlt64),
                        callBack = callBack
                    )) {
                        is Ok -> {
                            Ok(mechanismTokenTableAs<V, Flt64>(ConcurrentTokenTable(temp)))
                        }

                        is Failed -> {
                            Failed(result.error)
                        }

                        is Fatal -> {
                            Fatal(result.errors)
                        }
                    }
                }

                else -> {
                    Failed(Err(ErrorCode.ApplicationError, "Unsupported token table type: ${tokens::class.simpleName}"))
                }
            }
        }
    }

    override fun addConstraint(
        relation: LinearInequality<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toLinearFlattenData().getOrElse { return Failed(Err(ErrorCode.IllegalArgument, it.message ?: "Failed to flatten linear inequality")) }
        // Promote linear flatten data to quadratic (each linear monomial c*x becomes quadratic c*x*null)
        // 将线性扁平化数据提升为二次（每个线性单项式 c*x 变为二次 c*x*null）
        val qMonomials = flattenData.monomials.map { QuadraticMonomial(it.coefficient, it.symbol, null) }
        val qFlattenData = QuadraticFlattenData<V>(qMonomials, flattenData.constant)
        val constraint = when (val result = QuadraticConstraintImpl(
                relation = QuadraticRelationImpl(qFlattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        _constraints.add(constraint)
        return ok
    }

    override fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        name: String?,
        from: Pair<IntermediateSymbol<out V>, Boolean>?
    ): Try {
        val flattenData = relation.toQuadraticFlattenData()
        val constraint = when (val result = QuadraticConstraintImpl(
                relation = QuadraticRelationImpl(flattenData, relation.comparison),
                tokens = tokens,
                converter = parent.converter,
                lazy = false,
                name = name.orEmpty(),
                from = from
            )) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        _constraints.add(constraint)
        return ok
    }

    /**
     * 生成最优性 cut / Generate optimality cut
     *
     * 基于对偶解为 Benders 分解生成最优性割平面（线性或二次）。
     * Generates optimality cuts (linear or quadratic) for Benders decomposition based on the dual solution.
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolution 对偶解，约束到对偶值的映射 / Dual solution, mapping from constraint to dual value
     * @return 割平面列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
    */
    fun generateOptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        return buildQuadraticOptimalCut(
            constraints = quadraticConstraints,
            objectCategory = this.objectFunction.category,
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one
        )
    }

    /**
     * 生成可行性 cut / Generate feasibility cut
     *
     * 基于 Farkas 对偶解为 Benders 分解生成可行性割平面（线性或二次）。
     * Generates feasibility cuts (linear or quadratic) for Benders decomposition based on the Farkas dual solution.
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolution Farkas 对偶解，约束到对偶值的映射 / Farkas dual solution, mapping from constraint to dual value
     * @return 割平面列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
    */
    fun generateFeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: Map<Constraint<V, Quadratic>, V>
    ): List<Any> {
        return buildQuadraticFeasibleCut(
            constraints = quadraticConstraints,
            fixedVariables = fixedVariables,
            farkasDualSolution = farkasDualSolution,
            zero = parent.converter.zero,
            one = parent.converter.one,
            logger = logger
        )
    }

    /**
     * 将割平面转换为 Flt64 类型 / Convert a cut to Flt64 type
     *
     * 将线性或二次不等式割平面中的系数从 V 类型转换为 Flt64 类型。
     * Converts the coefficients of a linear or quadratic inequality cut from type V to Flt64.
     *
     * @param cut 待转换的割平面（线性或二次不等式）/ The cut to convert (linear or quadratic inequality)
     * @return Flt64 类型的割平面 / The cut with Flt64 coefficients
    */
    private fun toFlt64Cut(cut: Any): Any {
        val linearCut = SolverBoundaryCasts.linearInequalityAs<V>(cut)
        if (linearCut != null) {
            return (LinearInequality(
                lhs = LinearPolynomial(
                    monomials = linearCut.lhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                    constant = parent.converter.fromValue(linearCut.lhs.constant)
                ),
                rhs = LinearPolynomial(
                    monomials = linearCut.rhs.monomials.map { LinearMonomial(parent.converter.fromValue(it.coefficient), it.symbol) },
                    constant = parent.converter.fromValue(linearCut.rhs.constant)
                ),
                comparison = linearCut.comparison,
                name = linearCut.name,
                displayName = linearCut.displayName
            ) as LinearInequality<Flt64>).normalize()
        }

        val quadraticCut = SolverBoundaryCasts.quadraticInequalityAs<V>(cut)
        if (quadraticCut != null) {
            val flt64Cut = QuadraticInequalityOf(
                lhs = QuadraticPolynomial(
                    monomials = quadraticCut.lhs.monomials.map { QuadraticMonomial(parent.converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                    constant = parent.converter.fromValue(quadraticCut.lhs.constant)
                ),
                rhs = QuadraticPolynomial(
                    monomials = quadraticCut.rhs.monomials.map { QuadraticMonomial(parent.converter.fromValue(it.coefficient), it.symbol1, it.symbol2) },
                    constant = parent.converter.fromValue(quadraticCut.rhs.constant)
                ),
                comparison = quadraticCut.comparison,
                name = quadraticCut.name,
                displayName = quadraticCut.displayName
            )
            return flt64Cut.normalize()
        }

        return cut
    }

    /**
     * 按约束名称生成最优性 cut / Generate optimality cut by constraint name
     *
     * 根据 by-id 映射中的对偶值查找对应约束并委托给 [generateOptimalCut]。
     * Looks up constraints by name from the by-id dual mapping and delegates to [generateOptimalCut].
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolutionById 按约束名称索引的对偶解 / Dual solution indexed by constraint name
     * @return 割平面列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
    */
    internal fun generateOptimalCutById(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolutionById: Map<String, V>
    ): List<Any> {
        val zero = parent.converter.zero
        validateDualById(quadraticConstraints, dualSolutionById, logger)
        val dualSolution: Map<Constraint<V, Quadratic>, V> = buildMap {
            for (constraint in quadraticConstraints) {
                val dual = dualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateOptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 按约束名称生成可行性 cut / Generate feasibility cut by constraint name
     *
     * 根据 by-id 映射中的 Farkas 对偶值查找对应约束并委托给 [generateFeasibleCut]。
     * Looks up constraints by name from the by-id Farkas dual mapping and delegates to [generateFeasibleCut].
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolutionById 按约束名称索引的 Farkas 对偶解 / Farkas dual solution indexed by constraint name
     * @return 割平面列表（线性或二次不等式）/ List of cuts (linear or quadratic inequalities)
    */
    internal fun generateFeasibleCutById(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolutionById: Map<String, V>
    ): List<Any> {
        val zero = parent.converter.zero
        validateDualById(quadraticConstraints, farkasDualSolutionById, logger)
        val dualSolution: Map<Constraint<V, Quadratic>, V> = buildMap {
            for (constraint in quadraticConstraints) {
                val dual = farkasDualSolutionById[constraint.name]
                if (dual != null && dual neq zero) {
                    put(constraint, dual)
                }
            }
        }
        return generateFeasibleCut(fixedVariables, dualSolution)
    }

    /**
     * 生成 Flt64 最优性 cut / Generate Flt64 optimality cut
     *
     * 基于 Flt64 类型对偶解生成最优性割平面。对偶值从求解器原生 Flt64 类型转换后委托给 [generateOptimalCut]，
     * 返回值也转换为 Flt64。
     * Generates optimality cuts from Flt64 dual solution. Converts dual values from solver
     * raw Flt64 type, delegates to [generateOptimalCut], and converts the result back to Flt64.
     *
     * @param objectVariable 目标变量（theta）/ The objective variable (theta) to project onto
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param dualSolution Flt64 类型对偶解 / Flt64 dual solution mapping from constraint to dual value
     * @return Flt64 割平面列表 / List of Flt64 cuts (linear or quadratic inequalities)
    */
    fun generateFlt64OptimalCut(
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualSolution: Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        val dualByConstraint: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val flt64Constraint = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = dualSolution[flt64Constraint] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cuts = generateOptimalCut(
            objectVariable = objectVariable,
            fixedVariables = fixedVariables,
            dualSolution = dualByConstraint
        )
        val cutsFlt64 = cuts.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    /**
     * 生成 Flt64 可行性 cut / Generate Flt64 feasibility cut
     *
     * 基于 Flt64 类型 Farkas 对偶解生成可行性割平面。对偶值从求解器原生 Flt64 类型转换后委托给 [generateFeasibleCut]，
     * 返回值也转换为 Flt64。
     * Generates feasibility cuts from Flt64 Farkas dual solution. Converts dual values from
     * solver raw Flt64 type, delegates to [generateFeasibleCut], and converts the result back to Flt64.
     *
     * @param fixedVariables 子问题中固定的变量及其值 / Variables fixed in the sub-problem and their values
     * @param farkasDualSolution Flt64 类型 Farkas 对偶解 / Flt64 Farkas dual solution mapping from constraint to dual value
     * @return Flt64 割平面列表 / List of Flt64 cuts (linear or quadratic inequalities)
    */
    fun generateFlt64FeasibleCut(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualSolution: Map<Constraint<Flt64, Quadratic>, Flt64>,
    ): Ret<List<Any>> {
        val dualByConstraint: MutableMap<Constraint<V, Quadratic>, V> = LinkedHashMap()
        for (constraint in quadraticConstraints) {
            val flt64Constraint = SolverBoundaryCasts.quadraticConstraintAsFlt64(constraint)
            val dual = farkasDualSolution[flt64Constraint] ?: continue
            if (dual neq Flt64.zero) {
                dualByConstraint[constraint] = parent.converter.intoValue(dual)
            }
        }
        val cuts = generateFeasibleCut(
            fixedVariables = fixedVariables,
            farkasDualSolution = dualByConstraint
        )
        val cutsFlt64 = cuts.map { cut -> toFlt64Cut(cut) }
        return Ok(cutsFlt64)
    }

    /**
     * 从求解器原始对偶输出生成最优 Benders cut。
     * Generate optimal Benders cut from raw solver dual output.
     *
     * 该求解器边界入口接收原始对偶值，并通过 [QuadraticTetradModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64OptimalCut]。
     * This solver-boundary entry accepts raw dual values, maps them back to
     * Constraint objects through [QuadraticTetradModelView.tidyDualSolution], then
     * delegates to [generateFlt64OptimalCut].
     *
     * @param objective       the objective value of the sub-problem solution
     * @param objectVariable  the objective variable (theta) to project onto
     * @param fixedVariables  variables fixed in the sub-problem and their values
     * @param dualValues      raw dual values from the solver output
     * @param tetradModel     the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * 求解器边界：dualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: dualValues and return type are Flt64 because they represent raw solver output.
    */
    internal fun generateOptimalCutFromOutput(
        objective: Flt64,
        objectVariable: AbstractVariableItem<*, *>,
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        dualValues: List<Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val dualSolution = tetradModel.tidyDualSolution(dualValues)
        return generateFlt64OptimalCut(objectVariable, fixedVariables, dualSolution)
    }

    /**
     * 从求解器原始 Farkas 对偶输出生成可行 Benders cut。
     * Generate feasible Benders cut from raw solver Farkas dual output.
     *
     * 该求解器边界入口接收原始 Farkas 对偶值，并通过 [QuadraticTetradModelView.tidyDualSolution]
     * 映射回约束对象，随后委托给 [generateFlt64FeasibleCut]。
     * This solver-boundary entry accepts raw Farkas dual values, maps them back
     * to Constraint objects through [QuadraticTetradModelView.tidyDualSolution], then
     * delegates to [generateFlt64FeasibleCut].
     *
     * @param fixedVariables    variables fixed in the sub-problem and their values
     * @param farkasDualValues  raw Farkas dual values from the solver output
     * @param tetradModel       the QuadraticTetradModel containing origin mapping
     * @return list of cuts (linear or quadratic inequalities)
     *
     * 求解器边界：farkasDualValues 与返回值使用 Flt64，因为它们表示求解器原始输出。
     * Solver boundary: farkasDualValues and return type are Flt64 because they represent raw solver output.
    */
    internal fun generateFeasibleCutFromOutput(
        fixedVariables: Map<AbstractVariableItem<*, *>, V>,
        farkasDualValues: List<Flt64>,
        tetradModel: QuadraticTetradModelView
    ): Ret<List<Any>> {
        val farkasDualSolution = tetradModel.tidyDualSolution(farkasDualValues)
        return generateFlt64FeasibleCut(fixedVariables, farkasDualSolution)
    }

    /**
     * 约束数量 / Number of constraints
     *
     * 返回模型中当前存储的二次约束总数。
     * Returns the total number of quadratic constraints currently stored in the model.
    */
    val numConstraints: Int get() = _constraints.size

    override fun close() {
        tokens.close()
    }

    override fun toString(): String {
        return name
    }
}
