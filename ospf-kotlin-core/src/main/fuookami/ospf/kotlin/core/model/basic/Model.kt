/**
 * 模型核心接口与类型定义
 * Core model interfaces and type definitions
*/
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

    /**
     * 移除变量。
     * Remove variable.
     *
     * @param item 要移除的变量项 / The variable item to remove
    */
    fun remove(item: AbstractVariableItem<*, *>)

    /**
     * 添加变量列表（嵌套展平）/ Add variable lists (flattened)
     *
     * @param items the nested variable item lists / 嵌套的变量项列表
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariablesLists")
    fun add(items: Iterable<Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.flatten())
    }

    /**
     * 添加变量映射 / Add variable map
     *
     * @param items the variable item map / 变量项映射
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariables")
    fun <K> add(items: Map<K, AbstractVariableItem<*, *>>): Try {
        return add(items.values)
    }

    /**
     * 添加变量列表映射 / Add variable list map
     *
     * @param items the variable item list map / 变量项列表映射
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariablesLists")
    fun <K> add(items: Map<K, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatten())
    }

    /**
     * 添加二级映射变量 / Add two-level map variables
     *
     * @param items the two-level map of variable items / 二级映射变量项
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Variables")
    fun <K1, K2> add(items: MultiMap2<K1, K2, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values })
    }

    /**
     * 添加二级映射变量列表 / Add two-level map variable lists
     *
     * @param items the two-level map of variable item lists / 二级映射变量项列表
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2VariableLists")
    fun <K1, K2> add(items: MultiMap2<K1, K2, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatten())
    }

    /**
     * 添加三级映射变量 / Add three-level map variables
     *
     * @param items the three-level map of variable items / 三级映射变量项
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Variables")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values })
    }

    /**
     * 添加三级映射变量列表 / Add three-level map variable lists
     *
     * @param items the three-level map of variable item lists / 三级映射变量项列表
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3VariableLists")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    /**
     * 添加四级映射变量 / Add four-level map variables
     *
     * @param items the four-level map of variable items / 四级映射变量项
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Variables")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values })
    }

    /**
     * 添加四级映射变量列表 / Add four-level map variable lists
     *
     * @param items the four-level map of variable item lists / 四级映射变量项列表
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4VariableLists")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    /**
     * 添加数量包装的变量 / Add quantity-wrapped variable
     *
     * @param item the quantity-wrapped variable item / 数量包装的变量项
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariable")
    fun add(item: Quantity<AbstractVariableItem<*, *>>): Try {
        return add(item.value)
    }

    /**
     * 添加数量包装的变量列表 / Add quantity-wrapped variable list
     *
     * @param items the quantity-wrapped variable item list / 数量包装的变量项列表
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariables")
    fun add(items: Iterable<Quantity<AbstractVariableItem<*, *>>>): Try {
        return add(items.map { it.value })
    }

    /**
     * 添加数量包装的中间符号 / Add quantity-wrapped intermediate symbol
     *
     * @param item the quantity-wrapped intermediate symbol / 数量包装的中间符号
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加数量包装的中间符号列表 / Add quantity-wrapped intermediate symbol list
     *
     * @param items the quantity-wrapped intermediate symbol list / 数量包装的中间符号列表
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加映射形式的中间符号 / Add intermediate symbols as a map
     *
     * @param symbols the map of quantity-wrapped intermediate symbols / 数量包装的中间符号映射
     * @return the operation result / 操作结果
    */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantityIntermediateSymbols")
    fun <K> add(symbols: Map<K, Quantity<IntermediateSymbol<*>>>): Try {
        return add(symbols.values)
    }

    /**
     * 添加映射形式的中间符号列表 / Add intermediate symbol lists as a map
     *
     * @param symbols the map of quantity-wrapped intermediate symbol lists / 数量包装的中间符号列表映射
     * @return the operation result / 操作结果
    */
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

    /**
     * 移除数量包装的变量 / Remove quantity-wrapped variable
     *
     * @param item the quantity-wrapped variable item to remove / 要移除的数量包装变量项
    */
    fun remove(item: Quantity<AbstractVariableItem<*, *>>) {
        return remove(item.value)
    }

    /**
     * 添加目标（基于变量）。
     * Add objective (by variable).
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param variable    目标变量 / The objective variable
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
    fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try

    /**
     * 添加目标（基于常量）。
     * Add objective (by constant).
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param constant    常量值 / The constant value
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try

    /**
     * 添加目标（最小化）/ Add objective (minimize)
     *
     * @param variable the objective variable / 目标变量
     * @param name the objective name (nullable) / 目标名称（可为 null）
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加目标（最大化）/ Add objective (maximize)
     *
     * @param variable the objective variable / 目标变量
     * @param name the objective name (nullable) / 目标名称（可为 null）
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加目标（最小化常量）/ Add objective (minimize constant)
     *
     * @param constant the constant value / 常量值
     * @param name the objective name (nullable) / 目标名称（可为 null）
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加目标（最大化常量）/ Add objective (maximize constant)
     *
     * @param constant the constant value / 常量值
     * @param name the objective name (nullable) / 目标名称（可为 null）
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 设置解决方案（列表形式）。
     * Set solution as a list of values.
     *
     * @param solution 解值列表 / The list of solution values
    */
    fun setSolution(solution: List<V>)

    /**
     * 设置解决方案（变量-值映射形式）。
     * Set solution as a variable-to-value map.
     *
     * @param solution 变量到值的映射 / The variable-to-value map
    */
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

    /**
     * 添加约束（基于变量）/ Add constraint (by variable)
     *
     * @param constraint the constraint variable / 约束变量
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @param withRangeSet whether to include range set / 是否包含范围集
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加约束（基于线性单项式）/ Add constraint (by LinearMonomial)
     *
     * @param constraint the linear monomial constraint / 线性单项式约束
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @param withRangeSet whether to include range set / 是否包含范围集
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加约束（基于线性多项式）/ Add constraint (by LinearPolynomial)
     *
     * @param constraint the linear polynomial constraint / 线性多项式约束
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @param withRangeSet whether to include range set / 是否包含范围集
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加约束（基于线性中间符号）/ Add constraint (by LinearIntermediateSymbol)
     *
     * @param constraint the linear intermediate symbol constraint / 线性中间符号约束
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @param withRangeSet whether to include range set / 是否包含范围集
     * @return the operation result / 操作结果
    */
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
     * 添加线性约束（使用数学 LinearInequality）。
     * Add constraint using math LinearInequality.
     *
     * @param relation     线性不等式 / The linear inequality
     * @param lazy         是否延迟求值 / Whether lazy evaluation
     * @param name         约束名称（可为 null） / The constraint name (nullable)
     * @param displayName  约束显示名称（可为 null） / The constraint display name (nullable)
     * @param withRangeSet 是否包含范围集 / Whether to include range set
     * @return 操作结果 / The operation result
    */
    fun addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = false
    ): Try

    // ========== partition overloads ==========

    /**
     * 分区约束（基于变量）/ Partition constraint (by variables)
     *
     * @param variables the partition variables / 分区变量集合
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 分区约束（基于线性中间符号）/ Partition constraint (by LinearIntermediateSymbol)
     *
     * @param symbols the linear intermediate symbols / 线性中间符号集合
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 分区约束（基于线性单项式）/ Partition constraint (by LinearMonomial)
     *
     * @param monomials the linear monomials / 线性单项式集合
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 分区约束（基于线性多项式）/ Partition constraint (by LinearPolynomial)
     *
     * @param polynomial the linear polynomial / 线性多项式
     * @param lazy whether lazy evaluation / 是否延迟求值
     * @param name the constraint name (nullable) / 约束名称（可为 null）
     * @param displayName the constraint display name (nullable) / 约束显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加线性目标（基于变量）/ Add linear objective (by variable)
     *
     * @param category the objective category (minimize/maximize) / 目标类别（最小化/最大化）
     * @param variable the objective variable / 目标变量
     * @param name the objective name / 目标名称
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加线性目标（基于常量）/ Add linear objective (by constant)
     *
     * @param category the objective category (minimize/maximize) / 目标类别（最小化/最大化）
     * @param constant the constant value / 常量值
     * @param name the objective name / 目标名称
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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
     * 添加线性目标（使用 LinearFlattenData<V>）。
     * Add objective using LinearFlattenData<V>.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param flattenData 展平的线性数据 / The flattened linear data
     * @param name        目标名称 / The objective name
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads ==========

    /**
     * 添加目标（最小化线性多项式）/ Add objective (minimize LinearPolynomial)
     *
     * @param polynomial the linear polynomial / 线性多项式
     * @param name the objective name (nullable) / 目标名称（可为 null）
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加目标（最大化线性多项式）。
     * Add objective (maximize LinearPolynomial).
     *
     * @param polynomial  线性多项式 / The linear polynomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最小化线性单项式）。
     * Add objective (minimize LinearMonomial).
     *
     * @param monomial    线性单项式 / The linear monomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最大化线性单项式）。
     * Add objective (maximize LinearMonomial).
     *
     * @param monomial    线性单项式 / The linear monomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最小化线性中间符号）。
     * Add objective (minimize LinearIntermediateSymbol).
     *
     * @param symbol      线性中间符号 / The linear intermediate symbol
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最大化线性中间符号）。
     * Add objective (maximize LinearIntermediateSymbol).
     *
     * @param symbol      线性中间符号 / The linear intermediate symbol
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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
     * 添加二次约束（使用数学 QuadraticInequality）。
     * Add constraint using math QuadraticInequality.
     *
     * @param relation     二次不等式 / The quadratic inequality
     * @param lazy         是否延迟求值 / Whether lazy evaluation
     * @param name         约束名称（可为 null） / The constraint name (nullable)
     * @param displayName  约束显示名称（可为 null） / The constraint display name (nullable)
     * @param withRangeSet 是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
    */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

    /**
     * 添加二次目标（基于变量）/ Add quadratic objective (by variable)
     *
     * @param category the objective category (minimize/maximize) / 目标类别（最小化/最大化）
     * @param variable the objective variable / 目标变量
     * @param name the objective name / 目标名称
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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

    /**
     * 添加二次目标（基于常量）/ Add quadratic objective (by constant)
     *
     * @param category the objective category (minimize/maximize) / 目标类别（最小化/最大化）
     * @param constant the constant value / 常量值
     * @param name the objective name / 目标名称
     * @param displayName the objective display name (nullable) / 目标显示名称（可为 null）
     * @return the operation result / 操作结果
    */
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
     * 添加二次目标（使用 QuadraticFlattenData<V>）。
     * Add objective using QuadraticFlattenData<V>.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param flattenData 展平的二次数据 / The flattened quadratic data
     * @param name        目标名称 / The objective name
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads for Quadratic ==========

    /**
     * 添加目标（最小化二次多项式）。
     * Add objective (minimize QuadraticPolynomial).
     *
     * @param polynomial  二次多项式 / The quadratic polynomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最大化二次多项式）。
     * Add objective (maximize QuadraticPolynomial).
     *
     * @param polynomial  二次多项式 / The quadratic polynomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最小化二次单项式）。
     * Add objective (minimize QuadraticMonomial).
     *
     * @param monomial    二次单项式 / The quadratic monomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最大化二次单项式）。
     * Add objective (maximize QuadraticMonomial).
     *
     * @param monomial    二次单项式 / The quadratic monomial
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最小化二次中间符号）。
     * Add objective (minimize QuadraticIntermediateSymbol).
     *
     * @param symbol      二次中间符号 / The quadratic intermediate symbol
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加目标（最大化二次中间符号）。
     * Add objective (maximize QuadraticIntermediateSymbol).
     *
     * @param symbol      二次中间符号 / The quadratic intermediate symbol
     * @param name        目标名称（可为 null） / The objective name (nullable)
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加约束（基于二次单项式）。
     * Add constraint (by QuadraticMonomial).
     *
     * @param constraint  二次单项式 / The quadratic monomial
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param withRangeSet 是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加约束（基于二次多项式）。
     * Add constraint (by QuadraticPolynomial).
     *
     * @param constraint  二次多项式 / The quadratic polynomial
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param withRangeSet 是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 添加约束（基于二次中间符号）。
     * Add constraint (by QuadraticIntermediateSymbol).
     *
     * @param constraint  二次中间符号 / The quadratic intermediate symbol
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @param withRangeSet 是否包含范围集（可为 null） / Whether to include range set (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 分区约束（基于二次中间符号）。
     * Partition constraint (by QuadraticIntermediateSymbol).
     *
     * @param symbols     二次中间符号集合 / The quadratic intermediate symbols
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 分区约束（基于二次单项式）。
     * Partition constraint (by QuadraticMonomial).
     *
     * @param monomials   二次单项式集合 / The quadratic monomials
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @return 操作结果 / The operation result
    */
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

    /**
     * 分区约束（基于二次多项式）。
     * Partition constraint (by QuadraticPolynomial).
     *
     * @param polynomial  二次多项式 / The quadratic polynomial
     * @param lazy        是否延迟求值 / Whether lazy evaluation
     * @param name        约束名称（可为 null） / The constraint name (nullable)
     * @param displayName 约束显示名称（可为 null） / The constraint display name (nullable)
     * @return 操作结果 / The operation result
    */
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
