/**
 * 回调模型
 * Call-back model
 */
package fuookami.ospf.kotlin.core.model.callback

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 回调模型策略接口，定义目标比较和初始解生成。
 * Call-back model policy interface defining objective comparison and initial solution generation.
 *
 * @param V 数值类型 / The numeric type
 */
interface CallBackModelPolicy<V> where V : RealNumber<V>, V : NumberField<V> {
    /** 三路比较器 / The three-way comparator */
    val comparator: ThreeWayComparator<V>

    /**
     * 比较两个目标值的优先级顺序，任一为 null 时另一个更优。
     * Compare the ordering of two objective values; when either is null, the other is preferred.
     *
     * @param lhs 左侧目标值（可为 null） / The left-hand side objective value (nullable)
     * @param rhs 右侧目标值（可为 null） / The right-hand side objective value (nullable)
     * @return 比较结果，任一为 null 时另一个更优 / The comparison result; when either is null, the other is preferred
     */
    fun compareObjective(lhs: V?, rhs: V?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            comparator(lhs, rhs)
        } else {
            null
        }
    }

    /**
     * 根据指定的初始解数量和变量数量生成初始解列表。
     * Generate a list of initial solutions based on the given solution count and variable count.
     *
     * @param initialSolutionAmount 初始解数量 / The number of initial solutions
     * @param variableAmount 变量数量 / The number of variables
     * @return 初始解列表 / The list of initial solutions
     */
    fun initialSolutions(initialSolutionAmount: UInt64, variableAmount: UInt64): List<Solution<V>> {
        return emptyList()
    }
}

/**
 * 函数式回调模型策略，通过比较器和初始解生成器实现。
 * Functional call-back model policy implemented via comparator and initial solution generator.
 *
 * @property objectiveComparator       目标比较器 / Objective comparator
 * @property _initialSolutionsGenerator 初始解生成器（可为 null） / Initial solution generator (nullable)
 */
class FunctionalCallBackModelPolicy<V>(
    val objectiveComparator: PartialComparator<V>,
    private val _initialSolutionsGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null
) : CallBackModelPolicy<V> where V : RealNumber<V>, V : NumberField<V> {

    override val comparator: ThreeWayComparator<V> = { lhs, rhs ->
        if (objectiveComparator(lhs, rhs) == true || objectiveComparator(rhs, lhs) == false) {
            Order.Less(-1)
        } else if (objectiveComparator(lhs, rhs) == false || objectiveComparator(rhs, lhs) == true) {
            Order.Greater(1)
        } else {
            Order.Equal
        }
    }

    /**
     * 比较两个目标值的优先级顺序，使用自定义目标比较器。
     * Compare the ordering of two objective values using the custom objective comparator.
     *
     * @param lhs 左侧目标值（可为 null） / The left-hand side objective value (nullable)
     * @param rhs 右侧目标值（可为 null） / The right-hand side objective value (nullable)
     * @return 比较结果 / The comparison result
     */
    override fun compareObjective(lhs: V?, rhs: V?): Order? {
        return if (lhs != null && rhs == null) {
            Order.Less()
        } else if (lhs == null && rhs != null) {
            Order.Greater()
        } else if (lhs != null && rhs != null) {
            if (objectiveComparator(lhs, rhs) == true) {
                Order.Less()
            } else if (objectiveComparator(rhs, lhs) == true) {
                Order.Greater()
            } else {
                Order.Equal
            }
        } else {
            null
        }
    }

    /**
     * 根据指定的初始解数量和变量数量生成初始解列表。
     * Generate a list of initial solutions based on the given solution count and variable count.
     *
     * @param initialSolutionAmount 初始解数量 / The number of initial solutions
     * @param variableAmount 变量数量 / The number of variables
     * @return 初始解列表 / The list of initial solutions
     */
    override fun initialSolutions(
        initialSolutionAmount: UInt64,
        variableAmount: UInt64
    ): List<Solution<V>> {
        val gen = _initialSolutionsGenerator ?: return emptyList()
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until variableAmount).map {
                gen(Pair(solution, it))
            }
        }
    }
}

/**
 * 回调模型实现，支持通过回调函数添加约束和目标。
 * Call-back model implementation supporting constraint and objective addition via callback functions.
 *
 * @param V 数值类型 / The numeric type
 * @param category 模型类别（非 val/var） / The model category (non-val/var)
 * @property objectCategory 优化方向 / Optimization direction
 * @property tokens 令牌表 / The token table
 * @property _constraints 约束列表 / The constraint list
 * @property _objectiveFunctions 目标函数列表 / The objective function list
 * @property policy 回调模型策略 / The call-back model policy
 * @property _converter 值转换器 / The value converter
 */
class CallBackModel<V> internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val tokens: AbstractMutableTokenTable<V> = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution<V>>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<V?, Solution<V>>, String>> = ArrayList(),
    private val policy: CallBackModelPolicy<V>,
    private val _converter: IntoValue<V>
) : CallBackModelInterface<V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /**
         * 根据优化方向创建目标比较器。
         * Create an objective comparator based on the optimization direction.
         *
         * @param category 优化方向 / The optimization direction
         * @param converter 值转换器 / The value converter
         * @return 部分比较器 / The partial comparator
         */
        private fun <V> dumpObjectiveComparator(
            category: ObjectCategory,
            converter: IntoValue<V>
        ): PartialComparator<V> where V : RealNumber<V>, V : NumberField<V> = when (category) {
            ObjectCategory.Maximum -> { lhs, rhs -> converter.fromValue(lhs) geq converter.fromValue(rhs) }
            ObjectCategory.Minimum -> { lhs, rhs -> converter.fromValue(lhs) leq converter.fromValue(rhs) }
        }

        /**
         * 创建回调模型（使用自动目标比较器）。
         * Create a call-back model with auto objective comparator.
         *
         * @param objectCategory          优化方向 / The optimization direction
         * @param initialSolutionGenerator 初始解生成器（可为 null） / The initial solution generator (nullable)
         * @param converter               值转换器 / The value converter
         * @return 回调模型实例 / The call-back model instance
         */
        operator fun <V> invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = CallBackModel(
            objectCategory = objectCategory,
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = dumpObjectiveComparator(objectCategory, converter),
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = converter
        )

        /**
         * 创建回调模型（使用自定义目标比较器）。
         * Create a call-back model with custom objective comparator.
         *
         * @param objectiveComparator     目标比较器 / The objective comparator
         * @param initialSolutionGenerator 初始解生成器（可为 null） / The initial solution generator (nullable)
         * @param converter               值转换器 / The value converter
         * @return 回调模型实例 / The call-back model instance
         */
        operator fun <V> invoke(
            objectiveComparator: PartialComparator<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = CallBackModel(
            policy = FunctionalCallBackModelPolicy(
                objectiveComparator = objectiveComparator,
                _initialSolutionsGenerator = initialSolutionGenerator
            ),
            _converter = converter
        )

        /**
         * 从抽象元模型创建回调模型。
         * Create a call-back model from an abstract meta model.
         *
         * @param model                   抽象元模型 / The abstract meta model
         * @param initialSolutionGenerator 初始解生成器 / The initial solution generator
         * @param converter               值转换器 / The value converter
         * @return 回调模型实例 / The call-back model instance
         */
        operator fun <V> invoke(
            model: AbstractMetaModel<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ): CallBackModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val tokens = model.tokens.copy()
            val constraints = model.constraints.map { constraint ->
                Pair(
                    { solution: Solution<V> -> constraint.isTrue(solution, tokens) },
                    constraint.toString()
                )
            }.toMutableList()
            val objectiveFunction = model.subObjects.map { objective ->
                Pair(
                    { solution: Solution<V> ->
                        if (objective.category == model.objectCategory) {
                            objective.evaluate(solution)
                        } else {
                            objective.evaluate(solution)?.let { -it }
                        }
                    },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                category = model.category,
                objectCategory = model.objectCategory,
                tokens = tokens,
                _constraints = constraints,
                _objectiveFunctions = objectiveFunction,
                policy = FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectCategory, converter),
                    _initialSolutionsGenerator = initialSolutionGenerator
                ),
                _converter = converter
            )
        }

        /**
         * 从单目标机制模型创建回调模型。
         * Create a call-back model from a single-objective mechanism model.
         *
         * @param model                   单目标机制模型 / The single-objective mechanism model
         * @param initialSolutionGenerator 初始解生成器 / The initial solution generator
         * @param concurrent              是否使用并发符号表 / Whether to use a concurrent token table
         * @param converter               值转换器 / The value converter
         * @return 回调模型实例 / The call-back model instance
         */
        operator fun <V> invoke(
            model: SingleObjectMechanismModel<V>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            concurrent: Boolean = true,
            converter: IntoValue<V>
        ): CallBackModel<V> where V : RealNumber<V>, V : NumberField<V> {
            val tokens = if (concurrent) {
                ConcurrentManualAddTokenTable<V>(model.tokens.category)
            } else {
                ManualTokenTable<V>(model.tokens.category)
            }
            val constraints = model.constraints.map { constraint ->
                val impl = constraint as ConstraintImpl<V, *>
                Pair(
                    { solution: Solution<V> -> impl.isTrue(solution) },
                    constraint.name
                )
            }.toMutableList()
            val subObjects = model.objectFunction.subObjects as List<SubObject<V>>
            val objectiveFunction = subObjects.map { objective ->
                Pair(
                    { solution: Solution<V> ->
                        if (objective.category == model.objectFunction.category) {
                            objective.evaluate(solution)
                        } else {
                            objective.evaluate(solution)?.let { -it }
                        }
                    },
                    objective.name
                )
            }.toMutableList()
            return CallBackModel(
                category = Nonlinear,
                objectCategory = model.objectFunction.category,
                tokens = tokens,
                _constraints = constraints,
                _objectiveFunctions = objectiveFunction,
                policy = FunctionalCallBackModelPolicy(
                    dumpObjectiveComparator(model.objectFunction.category, converter),
                    _initialSolutionsGenerator = initialSolutionGenerator
                ),
                _converter = converter
            )
        }

    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    override fun converter(): IntoValue<V> = _converter

    override fun negativeInfinity(): V = _converter.negativeInfinity

    override fun infinity(): V = _converter.infinity

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution<V>> {
        return policy.initialSolutions(initialSolutionAmount, UInt64(tokens.tokensInSolver.size))
    }

    /**
     * 比较两个非空目标值的优先级顺序。
     * Compare the ordering of two non-null objective values.
     *
     * @param lhs 左侧目标值 / The left-hand side objective value
     * @param rhs 右侧目标值 / The right-hand side objective value
     * @return 比较结果 / The comparison result
     */
    override fun compareObjective(lhs: V, rhs: V): Order {
        return policy.comparator(lhs, rhs)
    }

    /**
     * 比较两个可空目标值的优先级顺序，null 视为最差。
     * Compare the ordering of two nullable objective values, treating null as worst.
     *
     * @param lhs 左侧目标值（可为 null） / The left-hand side objective value (nullable)
     * @param rhs 右侧目标值（可为 null） / The right-hand side objective value (nullable)
     * @return 比较结果 / The comparison result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("compareObjectiveNullable")
    override fun compareObjective(lhs: V?, rhs: V?): Order? {
        return policy.compareObjective(lhs, rhs)
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        tokens.add(item)
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        tokens.add(items)
        return ok
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    /**
     * 添加 Flt64 线性不等式约束。
     * Add a Flt64 linear inequality constraint.
     *
     * @param inequality  线性不等式输入 / The linear inequality input
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     */
    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: Flt64LinearConstraintInput,
        name: String?,
        displayName: String?
    ) {
        _constraints.add(
            Pair(
                { solution: Solution<V> -> inequality.isTrue(solution, _converter, tokens) },
                name ?: String()
            )
        )
    }

    /**
     * 添加泛型线性不等式约束。
     * Add a generic linear inequality constraint.
     *
     * @param inequality  线性不等式输入 / The linear inequality input
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     */
    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: LinearConstraintInput<V>,
        name: String?,
        displayName: String?
    ) {
        _constraints.add(
            Pair(
                { solution: Solution<V> -> inequality.isTrue(solution, tokens) },
                name ?: String()
            )
        )
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            func = { solution: Solution<V> -> tokens.find(variable)?.result },
            name = name,
            displayName = displayName
        )
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        val vConstant = _converter.intoValue(constant.toFlt64())
        return addObject(
            category = category,
            func = { solution: Solution<V> -> vConstant },
            name = name,
            displayName = displayName
        )
    }

    /**
     * 通过回调函数添加目标子项，根据类别自动取反。
     * Add an objective sub-item via a callback function, automatically negating based on category.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param func        目标回调函数 / The objective callback function
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<V?, Solution<V>>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution<V> ->
                    if (category == objectCategory) {
                        func(solution)
                    } else {
                        func(solution)?.let { -it }
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    /**
     * 添加最大化目标子项。
     * Add a maximization objective sub-item.
     *
     * @param func        目标回调函数 / The objective callback function
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    fun maximize(
        func: Extractor<V?, Solution<V>>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            func = func,
            name = name,
            displayName = displayName
        )
    }

    /**
     * 添加最小化目标子项。
     * Add a minimization objective sub-item.
     *
     * @param func        目标回调函数 / The objective callback function
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    fun minimize(
        func: Extractor<V?, Solution<V>>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            func = func,
            name = name,
            displayName = displayName
        )
    }

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}

/**
 * 多目标回调模型实现。
 * Multi-objective call-back model implementation.
 *
 * @param V 数值类型 / The numeric type
 * @param category 模型类别（非 val/var） / The model category (non-val/var)
 * @property objectCategory 优化方向 / Optimization direction
 * @property objectiveLocation 多目标位置列表 / List of multi-objective locations
 * @property tokens 令牌表 / The token table
 * @property _constraints 约束列表 / The constraint list
 * @property _objectiveFunctions 目标函数列表 / The objective function list
 * @property _initialSolutionsGenerator 初始解生成器（可为 null） / Initial solution generator (nullable)
 * @property _converter 值转换器 / The value converter
 */
class MultiObjectCallBackModel<V> internal constructor(
    category: Category = Nonlinear,
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    override val objectiveLocation: List<MultiObjectLocation<V>>,
    override val tokens: AbstractMutableTokenTable<V> = ManualTokenTable(category),
    private val _constraints: MutableList<Pair<Extractor<Boolean?, Solution<V>>, String>> = ArrayList(),
    private val _objectiveFunctions: MutableList<Pair<Extractor<List<Pair<MultiObjectLocation<V>, V>>?, Solution<V>>, String>> = ArrayList(),
    private val _initialSolutionsGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
    private val _converter: IntoValue<V>
) : MultiObjectiveModelInterface<V> where V : RealNumber<V>, V : NumberField<V> {
    companion object {
        /**
         * 创建多目标回调模型。
         * Create a multi-objective call-back model.
         *
         * @param objectCategory          优化方向 / The optimization direction
         * @param objectiveLocation       多目标位置列表 / The list of multi-objective locations
         * @param initialSolutionGenerator 初始解生成器（可为 null） / The initial solution generator (nullable)
         * @param converter               值转换器 / The value converter
         * @return 多目标回调模型实例 / The multi-objective call-back model instance
         */
        operator fun <V> invoke(
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            objectiveLocation: List<MultiObjectLocation<V>>,
            initialSolutionGenerator: Extractor<V, Pair<UInt64, UInt64>>? = null,
            converter: IntoValue<V>
        ) where V : RealNumber<V>, V : NumberField<V> = MultiObjectCallBackModel(
            objectCategory = objectCategory,
            objectiveLocation = objectiveLocation,
            _initialSolutionsGenerator = initialSolutionGenerator,
            _converter = converter
        )

    }

    init {
        require(objectiveLocation.isNotEmpty()) {
            "objectiveLocation can not be empty."
        }
    }

    override val constraints by ::_constraints
    override val objectiveFunctions by ::_objectiveFunctions

    override fun converter(): IntoValue<V> = _converter

    override fun negativeInfinity(): V = _converter.negativeInfinity

    override fun infinity(): V = _converter.infinity

    private val priorityToIndex = objectiveLocation
        .withIndex()
        .associate { (index, location) -> location.priority to index }

  private val defaultLocation: MultiObjectLocation<V>
        get() = objectiveLocation.first()

    override fun initialSolutions(initialSolutionAmount: UInt64): List<Solution<V>> {
        val gen = _initialSolutionsGenerator ?: return emptyList()
        return (UInt64.zero until initialSolutionAmount).map { solution ->
            (UInt64.zero until UInt64(tokens.tokensInSolver.size)).map { variable ->
                gen(solution to variable)
            }
        }
    }

    override fun objectiveValue(obj: List<Pair<MultiObjectLocation<V>, V>>): List<V> {
        val value = MutableList(objectiveSize) { _converter.zero }
        for ((location, objective) in obj) {
            val index = priorityToIndex[location.priority] ?: continue
            value[index] = value[index] + objective * location.weight
        }
        return value
    }

    override fun compareObjective(lhs: List<V>, rhs: List<V>): Order? {
        val size = minOf(lhs.size, rhs.size)
        for (i in 0 until size) {
            val l = _converter.fromValue(lhs[i])
            val r = _converter.fromValue(rhs[i])
            if (l eq r) {
                continue
            }

            return when (objectCategory) {
                ObjectCategory.Minimum -> {
                    if (l ls r) {
                        Order.Less()
                    } else {
                        Order.Greater()
                    }
                }

                ObjectCategory.Maximum -> {
                    if (l gr r) {
                        Order.Less()
                    } else {
                        Order.Greater()
                    }
                }
            }
        }

        return if (lhs.size < rhs.size) {
            Order.Less()
        } else if (lhs.size > rhs.size) {
            Order.Greater()
        } else {
            Order.Equal
        }
    }

    override fun add(item: AbstractVariableItem<*, *>): Try {
        tokens.add(item)
        return ok
    }

    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try {
        tokens.add(items)
        return ok
    }

    override fun remove(item: AbstractVariableItem<*, *>) {
        tokens.remove(item)
    }

    /**
     * 添加 Flt64 线性不等式约束。
     * Add a Flt64 linear inequality constraint.
     *
     * @param inequality  线性不等式输入 / The linear inequality input
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     */
    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: Flt64LinearConstraintInput,
        name: String? = null,
        displayName: String? = null
    ) {
        _constraints.add(
            Pair(
                { solution: Solution<V> -> inequality.isTrue(solution, _converter, tokens) },
                name ?: String()
            )
        )
    }

    /**
     * 添加泛型线性不等式约束。
     * Add a generic linear inequality constraint.
     *
     * @param inequality  线性不等式输入 / The linear inequality input
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     */
    @Suppress("UNUSED_PARAMETER")
    fun addConstraint(
        inequality: LinearConstraintInput<V>,
        name: String? = null,
        displayName: String? = null
    ) {
        _constraints.add(
            Pair(
                { solution: Solution<V> -> inequality.isTrue(solution, tokens) },
                name ?: String()
            )
        )
    }

    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            func = { solution: Solution<V> -> tokens.find(variable)?.result },
            location = defaultLocation,
            name = name,
            displayName = displayName
        )
    }

    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        val vConstant = _converter.intoValue(constant.toFlt64())
        return addObject(
            category = category,
            func = { solution: Solution<V> -> vConstant },
            location = defaultLocation,
            name = name,
            displayName = displayName
        )
    }

    /**
     * 通过回调函数和指定位置添加多目标子项。
     * Add a multi-objective sub-item via a callback function at the specified location.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param func        目标回调函数 / The objective callback function
     * @param location    多目标位置 / The multi-objective location
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    @Suppress("UNUSED_PARAMETER")
    fun addObject(
        category: ObjectCategory,
        func: Extractor<V?, Solution<V>>,
        location: MultiObjectLocation<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        _objectiveFunctions.add(
            Pair(
                { solution: Solution<V> ->
                    func(solution)?.let {
                        val v = if (category == objectCategory) it else -it
                        listOf(location to v)
                    }
                },
                name ?: String()
            )
        )
        return ok
    }

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    override fun flush() {
        tokens.flush()
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }
}
