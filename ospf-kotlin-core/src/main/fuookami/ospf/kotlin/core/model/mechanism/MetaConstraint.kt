/**
 * 元约束组与数学约束
 * Meta-constraint group and math constraints
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 元约束组接口，提供在 MetaModel 上注册和查询约束组的能力。
 * Meta-constraint group interface providing constraint group registration and querying on MetaModel.
 */
interface MetaConstraintGroup {
    val lazy: Boolean get() = false
    val name: String

    fun <V> MetaModel<V>.registerConstraintGroup() where V : RealNumber<V>, V : NumberField<V> {
        this.registerConstraintGroup(this@MetaConstraintGroup)
    }

    fun <V> MetaModel<V>.indicesOfConstraintGroup(): IntRange? where V : RealNumber<V>, V : NumberField<V> {
        return this.indicesOfConstraintGroup(this@MetaConstraintGroup)
    }

    fun <V> MetaModel<V>.constraintsOfGroup(): List<MathConstraint> where V : RealNumber<V>, V : NumberField<V> {
        return indicesOfConstraintGroup(this@MetaConstraintGroup)?.let { indices ->
            indices.map { constraints[it] }
        } ?: emptyList()
    }

    /**
     * 以变量项为约束添加到线性元模型。
     * Add a variable item as a constraint to the linear meta model.
     *
     * @param constraint    变量项 / The variable item
     * @param lazy          是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name          约束名称（可为 null） / The constraint name (nullable)
     * @param displayName   约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args          附加参数（可为 null） / Additional arguments (nullable)
     * @param withRangeSet  是否包含范围集 / Whether to include range set
     * @return 操作结果 / The operation result
     */
    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        val lhs = LinearPolynomial(listOf(LinearMonomial(converter.one, constraint)), converter.zero)
        val rhs = LinearPolynomial(emptyList(), converter.one)
        return this.addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 以线性中间符号为约束添加到线性元模型。
     * Add a linear intermediate symbol as a constraint to the linear meta model.
     *
     * @param constraint    线性中间符号 / The linear intermediate symbol
     * @param lazy          是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name          约束名称（可为 null） / The constraint name (nullable)
     * @param displayName   约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args          附加参数（可为 null） / Additional arguments (nullable)
     * @param withRangeSet  是否包含范围集 / Whether to include range set
     * @return 操作结果 / The operation result
     */
    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        constraint: LinearIntermediateSymbol<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        val lhs = constraint.toLinearPolynomial()
        val rhs = LinearPolynomial(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 以变量项集合创建分区约束。
     * Create a partition constraint from a collection of variable items.
     *
     * @param variables   变量项集合 / The collection of variable items
     * @param lazy        是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args        附加参数（可为 null） / Additional arguments (nullable)
     * @return 操作结果 / The operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun <V> AbstractLinearMetaModel<V>.partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        return partition(
            polynomial = LinearPolynomial(
                monomials = variables.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    /**
     * 以线性中间符号集合创建分区约束。
     * Create a partition constraint from a collection of linear intermediate symbols.
     *
     * @param symbols     线性中间符号集合 / The collection of linear intermediate symbols
     * @param lazy        是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args        附加参数（可为 null） / Additional arguments (nullable)
     * @return 操作结果 / The operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun <V> AbstractLinearMetaModel<V>.partition(
        symbols: Iterable<LinearIntermediateSymbol<V>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return partition(
            polynomial = LinearPolynomial(
                monomials = symbols.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    /**
     * 以二次中间符号为约束添加到二次元模型。
     * Add a quadratic intermediate symbol as a constraint to the quadratic meta model.
     *
     * @param constraint    二次中间符号 / The quadratic intermediate symbol
     * @param lazy          是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name          约束名称（可为 null） / The constraint name (nullable)
     * @param displayName   约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args          附加参数（可为 null） / Additional arguments (nullable)
     * @param withRangeSet  是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
     */
    fun <V> AbstractQuadraticMetaModel<V>.addConstraint(
        constraint: QuadraticIntermediateSymbol<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        val lhs = constraint.toQuadraticPolynomial()
        val rhs = QuadraticPolynomial(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(lhs, rhs, Comparison.EQ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    /**
     * 以二次中间符号集合创建分区约束。
     * Create a partition constraint from a collection of quadratic intermediate symbols.
     *
     * @param symbols     二次中间符号集合 / The collection of quadratic intermediate symbols
     * @param lazy        是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args        附加参数（可为 null） / Additional arguments (nullable)
     * @return 操作结果 / The operation result
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun <V> AbstractQuadraticMetaModel<V>.partition(
        symbols: Iterable<QuadraticIntermediateSymbol<V>>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try where V : RealNumber<V>, V : Ring<V>, V : NumberField<V> {
        return partition(
            polynomial = QuadraticPolynomial(
                monomials = symbols.flatMap { it.toQuadraticPolynomial().monomials }.toList(),
                constant = converter.zero
            ),
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    // ========== Math Inequality-based API ==========

    /**
     * 使用数学 LinearInequality<V> 添加约束。
     * Add constraint using math LinearInequality<V>.
     *
     * @param relation    线性不等式 / The linear inequality
     * @param lazy        是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args        附加参数（可为 null） / Additional arguments (nullable)
     * @param withRangeSet 是否包含范围集 / Whether to include range set
     * @return 操作结果 / The operation result
     */
    fun <V> AbstractLinearMetaModel<V>.addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        return this.addConstraint(
            relation = relation,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用数学 QuadraticInequalityOf<V> 添加约束。
     * Add constraint using math QuadraticInequalityOf<V>.
     *
     * @param relation     二次不等式 / The quadratic inequality
     * @param lazy         是否延迟求值（可为 null，使用组默认值）/ Whether lazy evaluation (nullable, uses group default)
     * @param name         约束名称（可为 null） / The constraint name (nullable)
     * @param displayName  约束显示名称（可为 null） / The constraint display name (nullable)
     * @param args         附加参数（可为 null） / Additional arguments (nullable)
     * @param withRangeSet 是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
     */
    fun <V> AbstractQuadraticMetaModel<V>.addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean? = null,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try where V : RealNumber<V>, V : NumberField<V> {
        return this.addConstraint(
            relation = relation,
            group = this@MetaConstraintGroup,
            lazy = lazy ?: this@MetaConstraintGroup.lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }
}

// Group-scoped query helper.
// 约束组作用域查询辅助函数。
fun <V> MetaModel<V>.constraintsOfGroup(group: MetaConstraintGroup): List<MathConstraint>
        where V : RealNumber<V>, V : NumberField<V> {
    return group.run { this@constraintsOfGroup.constraintsOfGroup() }
}

// ========== Math Inequality-based Constraint<Flt64> Types ==========

/**
 * 数学约束通用接口。
 * Common interface for math-based constraints.
 */
interface MathConstraint {
    val group: MetaConstraintGroup?
    val lazy: Boolean
    val args: Any?
    val priority: Int?

    fun <V> isTrue(
        solution: List<V>,
        tokenTable: AbstractTokenTable<V>,
        zeroIfNone: Boolean = false
    ): Boolean? where V : RealNumber<V>, V : NumberField<V>
}

/**
 * 线性不等式约束，使用数学 LinearInequality<V>。
 * Linear inequality constraint using math LinearInequality<V>.
 *
 * @property inequality 线性不等式 / The linear inequality
 * @property converter 值转换器 / Value converter
 * @property constraintName 约束名称 / Constraint name
 * @property constraintDisplayName 约束显示名称 / Constraint display name
 * @property group 约束组 / Constraint group
 * @property lazy 是否延迟求值 / Whether lazy evaluation
 * @property args 附加参数 / Additional arguments
 * @property priority 约束优先级 / Constraint priority
 */
data class LinearInequalityConstraint<V>(
    val inequality: LinearInequality<V>,
    val converter: IntoValue<V>,
    val constraintName: String = "",
    val constraintDisplayName: String? = null,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 将内部线性不等式扁平化为 LinearFlattenData。
     * Flatten the internal linear inequality into LinearFlattenData.
     *
     * 将不等式转换为单项式列表加常量的形式，供约束构建和求值使用。
     * Converts the inequality into a monomial-list-plus-constant form for constraint building and evaluation.
     *
     * @return 包含扁平化线性数据的结果，或错误 / Result containing the flattened linear data, or an error
     */
    fun flattenData(): Ret<LinearFlattenData<V>> {
        return inequality.toLinearFlattenData().fold(
            onSuccess = { flattenData -> ok(flattenData) },
            onFailure = { error ->
                Failed(
                    ErrorCode.IllegalArgument,
                    error.message ?: "Failed to flatten linear inequality."
                )
            }
        )
    }

    val sign: Comparison get() = inequality.comparison
    val name: String get() = constraintName
    val displayName: String? get() = constraintDisplayName

    /**
     * 判断约束条件是否成立。
     * Evaluate whether the constraint condition is satisfied.
     *
     * @param value 待判断的值 / Value to evaluate
     * @return 判断结果 / Evaluation result
     */
    override fun <V1> isTrue(
        solution: List<V1>,
        tokenTable: AbstractTokenTable<V1>,
        zeroIfNone: Boolean
    ): Boolean? where V1 : RealNumber<V1>, V1 : NumberField<V1> {
        val sourceFlattenData = when (val result = flattenData()) {
            is Ok -> result.value
            is Failed -> return null
            is Fatal -> return null
        }
        @Suppress("UNCHECKED_CAST")
        val targetFlattenData = sourceFlattenData as LinearFlattenData<V1>
        val lhsValue = evaluateFlattenDataWithResults(targetFlattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue.toFlt64(), Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

/**
 * 二次不等式约束，使用数学 QuadraticInequalityOf<V>。
 * Quadratic inequality constraint using math QuadraticInequalityOf<V>.
 *
 * @property inequality 二次不等式 / The quadratic inequality
 * @property converter 值转换器 / Value converter
 * @property constraintName 约束名称 / Constraint name
 * @property constraintDisplayName 约束显示名称 / Constraint display name
 * @property group 约束组 / Constraint group
 * @property lazy 是否延迟求值 / Whether lazy evaluation
 * @property args 附加参数 / Additional arguments
 * @property priority 约束优先级 / Constraint priority
 */
data class QuadraticInequalityConstraint<V>(
    val inequality: QuadraticInequalityOf<V>,
    val converter: IntoValue<V>,
    val constraintName: String = "",
    val constraintDisplayName: String? = null,
    override val group: MetaConstraintGroup? = null,
    override val lazy: Boolean = false,
    override val args: Any? = null,
    override val priority: Int? = null
) : MathConstraint where V : RealNumber<V>, V : NumberField<V> {
    val flattenData: QuadraticFlattenData<V> get() = inequality.toQuadraticFlattenData()
    val sign: Comparison get() = inequality.comparison
    val name: String get() = constraintName
    val displayName: String? get() = constraintDisplayName

    override fun <V1> isTrue(
        solution: List<V1>,
        tokenTable: AbstractTokenTable<V1>,
        zeroIfNone: Boolean
    ): Boolean? where V1 : RealNumber<V1>, V1 : NumberField<V1> {
        @Suppress("UNCHECKED_CAST")
        val targetFlattenData = flattenData as QuadraticFlattenData<V1>
        val lhsValue = evaluateQuadraticFlattenDataWithResults(targetFlattenData, solution, tokenTable, zeroIfNone)
            ?: return null
        return sign.compare(lhsValue.toFlt64(), Flt64.zero)
    }

    override fun toString(): String {
        return inequality.toString()
    }
}

// ========== NEW FlattenData-based SubObject Types ==========

/**
 * 二次展平子目标，直接使用 QuadraticFlattenData<V> 作为目标函数。
 * Quadratic flatten sub-objective using QuadraticFlattenData<V> directly for objective functions.
 *
 * @property category 目标分类 / The objective category
 * @property flattenData 展平的二次数据 / Flattened quadratic data
 * @property name 子目标名称 / Sub-objective name
 * @property displayName 子目标显示名称 / Sub-objective display name
 */
data class QuadraticFlattenSubObject<V>(
    val category: ObjectCategory,
    val flattenData: QuadraticFlattenData<V>,
    val name: String = "",
    val displayName: String? = null
) where V : RealNumber<V>, V : NumberField<V>
