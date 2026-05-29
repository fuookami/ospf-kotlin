@file:Suppress("unused")
package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

/**
 * 模型核心接口
 * Core model interfaces
 */

/** 解决方案类型别名 / Solution type alias */
typealias Solution<V> = List<V>

/**
 * 模型核心接口，定义变量注册、目标设置和约束添加的能力。
 * Core model interface defining capabilities for variable registration, objective setting, and constraint addition.
 *
 * @param V 数值类型 / The numeric type
 */
interface Model<V> : AddableTokenCollection<V> where V : RealNumber<V>, V : NumberField<V> {
    val objectCategory: ObjectCategory

    /** 移除变量 / Remove variable */
    fun remove(item: AbstractVariableItem<*, *>)

    /** 添加变量列表（嵌套展平）/ Add variable lists (flattened) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariablesLists")
    fun add(items: Iterable<Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.flatten())
    }

    /** 添加变量映射 / Add variable map */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariables")
    fun <K> add(items: Map<K, AbstractVariableItem<*, *>>): Try {
        return add(items.values)
    }

    /** 添加变量列表映射 / Add variable list map */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariablesLists")
    fun <K> add(items: Map<K, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatten())
    }

    /** 添加二级映射变量 / Add two-level map variables */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Variables")
    fun <K1, K2> add(items: MultiMap2<K1, K2, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values })
    }

    /** 添加二级映射变量列表 / Add two-level map variable lists */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2VariableLists")
    fun <K1, K2> add(items: MultiMap2<K1, K2, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatten())
    }

    /** 添加三级映射变量 / Add three-level map variables */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Variables")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values })
    }

    /** 添加三级映射变量列表 / Add three-level map variable lists */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3VariableLists")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    /** 添加四级映射变量 / Add four-level map variables */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Variables")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values })
    }

    /** 添加四级映射变量列表 / Add four-level map variable lists */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4VariableLists")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    /** 添加数量包装的变量 / Add quantity-wrapped variable */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariable")
    fun add(item: Quantity<AbstractVariableItem<*, *>>): Try {
        return add(item.value)
    }

    /** 添加数量包装的变量列表 / Add quantity-wrapped variable list */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariables")
    fun add(items: Iterable<Quantity<AbstractVariableItem<*, *>>>): Try {
        return add(items.map { it.value })
    }

    /** 添加数量包装的中间符号 / Add quantity-wrapped intermediate symbol */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityIntermediateSymbol")
    fun add(item: Quantity<IntermediateSymbol<*>>): Try {
        return when (this) {
            is MetaModel<*> -> {
                @Suppress("UNCHECKED_CAST")
                (this as MetaModel<V>).add(item.value)
            }

            else -> {
                @Suppress("UNCHECKED_CAST")
                (item.value as IntermediateSymbol<V>).registerAuxiliaryTokens(this)
            }
        }
    }

    /** 添加数量包装的中间符号列表 / Add quantity-wrapped intermediate symbol list */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityIntermediateSymbols")
    fun add(items: Iterable<Quantity<IntermediateSymbol<*>>>): Try {
        for (item in items) {
            when (val result = add(item)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /** 添加映射形式的中间符号 / Add intermediate symbols as a map */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantityIntermediateSymbols")
    fun <K> add(symbols: Map<K, Quantity<IntermediateSymbol<*>>>): Try {
        return add(symbols.values)
    }

    /** 添加映射形式的中间符号列表 / Add intermediate symbol lists as a map */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantityIntermediateSymbolLists")
    fun <K> add(symbols: Map<K, Iterable<Quantity<IntermediateSymbol<*>>>>): Try {
        for (batch in symbols.values) {
            when (val result = add(batch)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /** 移除数量包装的变量 / Remove quantity-wrapped variable */
    fun remove(item: Quantity<AbstractVariableItem<*, *>>) {
        return remove(item.value)
    }

    /** 添加目标（基于变量）/ Add objective (by variable) */
    fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try

    /** 添加目标（基于常量）/ Add objective (by constant) */
    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try

    /** 添加目标（最小化）/ Add objective (minimize) */
    fun minimize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            variable = variable,
            name = name,
            displayName = displayName
        )
    }

    /** 添加目标（最大化）/ Add objective (maximize) */
    fun maximize(
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            variable = variable,
            name = name,
            displayName = displayName
        )
    }

    /** 添加目标（最小化常量）/ Add objective (minimize constant) */
    fun <T : RealNumber<T>> minimize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    /** 添加目标（最大化常量）/ Add objective (maximize constant) */
    fun <T : RealNumber<T>> maximize(
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            constant = constant,
            name = name,
            displayName = displayName
        )
    }

    /** 设置解决方案（列表形式）/ Set solution as a list of values */
    fun setSolution(solution: List<V>)
    /** 设置解决方案（变量-值映射形式）/ Set solution as a variable-to-value map */
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>)
    /** 清除解决方案 / Clear the current solution */
    fun clearSolution()
}

/**
 * 线性模型接口，在 Model 基础上增加线性约束和线性目标的能力。
 * Linear model interface adding linear constraint and objective capabilities on top of Model.
 *
 * @param V 数值类型 / The numeric type
 */
interface LinearModel<V> : Model<V> where V : RealNumber<V>, V : NumberField<V> {
    val converter: IntoValue<V>

    // ========== addConstraint convenience overloads ==========

    /** 添加约束（基于变量）/ Add constraint (by variable) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintVariable")
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val lhs = LinearPolynomial(listOf(LinearMonomial(converter.one, constraint)), converter.zero)
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /** 添加约束（基于线性单项式）/ Add constraint (by LinearMonomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintLinearMonomial")
    fun addConstraint(
        constraint: LinearMonomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val lhs = LinearPolynomial(listOf(constraint), converter.zero)
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /** 添加约束（基于线性多项式）/ Add constraint (by LinearPolynomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintLinearPolynomial")
    fun addConstraint(
        constraint: LinearPolynomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(constraint, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /** 添加约束（基于线性中间符号）/ Add constraint (by LinearIntermediateSymbol) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintLinearSymbol")
    fun addConstraint(
        constraint: LinearIntermediateSymbol<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val lhs = constraint.toLinearPolynomial()
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(lhs, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 添加线性约束（使用数学 LinearInequality）
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try

    // ========== partition overloads ==========

    /** 分区约束（基于变量）/ Partition constraint (by variables) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(
                monomials = variables.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    /** 分区约束（基于线性中间符号）/ Partition constraint (by LinearIntermediateSymbol) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol<V>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(
                monomials = symbols.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    /** 分区约束（基于线性单项式）/ Partition constraint (by LinearMonomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearMonomials")
    fun partition(
        monomials: Iterable<LinearMonomial<V>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(monomials.toList(), converter.zero),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    /** 分区约束（基于线性多项式）/ Partition constraint (by LinearPolynomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearPolynomial")
    fun partition(
        polynomial: LinearPolynomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality(polynomial, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    // ========== addObject overloads ==========

    /** 添加线性目标（基于变量）/ Add linear objective (by variable) */
    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = LinearFlattenData<V>(listOf(LinearMonomial(converter.one, variable)), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加线性目标（基于常量）/ Add linear objective (by constant) */
    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = LinearFlattenData<V>(emptyList(), converter.intoValue(constant.toFlt64())),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * 添加线性目标（使用 LinearFlattenData<V>）
     * Add objective using LinearFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads ==========

    /** 添加目标（最小化线性多项式）/ Add objective (minimize LinearPolynomial) */
    fun minimize(
        polynomial: LinearPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化线性多项式）/ Add objective (maximize LinearPolynomial) */
    fun maximize(
        polynomial: LinearPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最小化线性单项式）/ Add objective (minimize LinearMonomial) */
    fun minimize(
        monomial: LinearMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化线性单项式）/ Add objective (maximize LinearMonomial) */
    fun maximize(
        monomial: LinearMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    /** 添加目标（最小化线性中间符号）/ Add objective (minimize LinearIntermediateSymbol) */
    fun minimize(
        symbol: LinearIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toLinearPolynomial()
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化线性中间符号）/ Add objective (maximize LinearIntermediateSymbol) */
    fun maximize(
        symbol: LinearIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toLinearPolynomial()
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = LinearFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }
}

/**
 * 二次模型接口，在 LinearModel 基础上增加二次约束和二次目标的能力。
 * Quadratic model interface adding quadratic constraint and objective capabilities on top of LinearModel.
 *
 * @param V 数值类型 / The numeric type
 */
interface QuadraticModel<V> : LinearModel<V> where V : RealNumber<V>, V : NumberField<V> {
    /**
     * 添加二次约束（使用数学 QuadraticInequality）
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

    /** 添加二次目标（基于变量）/ Add quadratic objective (by variable) */
    override fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = QuadraticFlattenData<V>(listOf(QuadraticMonomial(converter.one, variable)), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加二次目标（基于常量）/ Add quadratic objective (by constant) */
    override fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String?,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = QuadraticFlattenData<V>(emptyList(), converter.intoValue(constant.toFlt64())),
            name = name ?: "",
            displayName = displayName
        )
    }

    /**
     * 添加二次目标（使用 QuadraticFlattenData<V>）
     * Add objective using QuadraticFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads for Quadratic ==========

    /** 添加目标（最小化二次多项式）/ Add objective (minimize QuadraticPolynomial) */
    fun minimize(
        polynomial: QuadraticPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化二次多项式）/ Add objective (maximize QuadraticPolynomial) */
    fun maximize(
        polynomial: QuadraticPolynomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最小化二次单项式）/ Add objective (minimize QuadraticMonomial) */
    fun minimize(
        monomial: QuadraticMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化二次单项式）/ Add objective (maximize QuadraticMonomial) */
    fun maximize(
        monomial: QuadraticMonomial<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(listOf(monomial), converter.zero),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== Unified entry points ==========

    /** 添加目标（最小化二次中间符号）/ Add objective (minimize QuadraticIntermediateSymbol) */
    fun minimize(
        symbol: QuadraticIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toQuadraticPolynomial()
        return addObject(
            category = ObjectCategory.Minimum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    /** 添加目标（最大化二次中间符号）/ Add objective (maximize QuadraticIntermediateSymbol) */
    fun maximize(
        symbol: QuadraticIntermediateSymbol<V>,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val polynomial = symbol.toQuadraticPolynomial()
        return addObject(
            category = ObjectCategory.Maximum,
            flattenData = QuadraticFlattenData(polynomial.monomials, polynomial.constant),
            name = name ?: "",
            displayName = displayName
        )
    }

    // ========== addConstraint convenience overloads ==========

    /** 添加约束（基于二次单项式）/ Add constraint (by QuadraticMonomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintQuadraticMonomial")
    fun addConstraint(
        constraint: QuadraticMonomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val lhs = QuadraticPolynomial(listOf(constraint), converter.zero)
        val rhs = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(lhs, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /** 添加约束（基于二次多项式）/ Add constraint (by QuadraticPolynomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintQuadraticPolynomial")
    fun addConstraint(
        constraint: QuadraticPolynomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val rhs = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(constraint, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    /** 添加约束（基于二次中间符号）/ Add constraint (by QuadraticIntermediateSymbol) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintQuadraticSymbol")
    fun addConstraint(
        constraint: QuadraticIntermediateSymbol<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val lhs = constraint.toQuadraticPolynomial()
        val rhs = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(lhs, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName,
            withRangeSet = withRangeSet
        )
    }

    // ========== partition overloads ==========

    /** 分区约束（基于二次中间符号）/ Partition constraint (by QuadraticIntermediateSymbol) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol<V>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = QuadraticPolynomial(
                monomials = symbols.map { it.toQuadraticPolynomial() }.flatMap { it.monomials }.toList(),
                constant = converter.zero
            ),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    /** 分区约束（基于二次单项式）/ Partition constraint (by QuadraticMonomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticMonomials")
    fun partition(
        monomials: Iterable<QuadraticMonomial<V>>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        return partition(
            polynomial = QuadraticPolynomial(monomials.toList(), converter.zero),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }

    /** 分区约束（基于二次多项式）/ Partition constraint (by QuadraticPolynomial) */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticPolynomial")
    fun partition(
        polynomial: QuadraticPolynomial<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null
    ): Try {
        val rhs = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf(polynomial, rhs, Comparison.EQ),
            lazy = lazy,
            name = name,
            displayName = displayName
        )
    }
}
