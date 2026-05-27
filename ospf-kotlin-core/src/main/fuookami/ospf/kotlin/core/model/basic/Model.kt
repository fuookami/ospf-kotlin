/**
 * 模型核心接口
 * Core model interfaces
 */
@file:Suppress("unused")

package fuookami.ospf.kotlin.core.model.basic

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.QuadraticInequalityOf
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection
import fuookami.ospf.kotlin.core.token.LinearFlattenData
import fuookami.ospf.kotlin.core.token.QuadraticFlattenData
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.QuadraticIntermediateSymbol
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem

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

    fun remove(item: AbstractVariableItem<*, *>)

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addVariablesLists")
    fun add(items: Iterable<Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariables")
    fun <K> add(items: Map<K, AbstractVariableItem<*, *>>): Try {
        return add(items.values)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapVariablesLists")
    fun <K> add(items: Map<K, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Variables")
    fun <K1, K2> add(items: MultiMap2<K1, K2, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2VariableLists")
    fun <K1, K2> add(items: MultiMap2<K1, K2, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Variables")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3VariableLists")
    fun <K1, K2, K3> add(items: MultiMap3<K1, K2, K3, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Variables")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, AbstractVariableItem<*, *>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values })
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4VariableLists")
    fun <K1, K2, K3, K4> add(items: MultiMap4<K1, K2, K3, K4, Iterable<AbstractVariableItem<*, *>>>): Try {
        return add(items.values.flatMap { it.values }.flatMap { it.values }.flatMap { it.values }.flatten())
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariable")
    fun add(item: Quantity<AbstractVariableItem<*, *>>): Try {
        return add(item.value)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addQuantityVariables")
    fun add(items: Iterable<Quantity<AbstractVariableItem<*, *>>>): Try {
        return add(items.map { it.value })
    }

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

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapQuantityIntermediateSymbols")
    fun <K> add(symbols: Map<K, Quantity<IntermediateSymbol<*>>>): Try {
        return add(symbols.values)
    }

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

    fun remove(item: Quantity<AbstractVariableItem<*, *>>) {
        return remove(item.value)
    }

    fun addObject(
        category: ObjectCategory,
        variable: AbstractVariableItem<*, *>,
        name: String? = null,
        displayName: String? = null
    ): Try

    fun <T : RealNumber<T>> addObject(
        category: ObjectCategory,
        constant: T,
        name: String? = null,
        displayName: String? = null
    ): Try

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

    fun setSolution(solution: List<V>)
    fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>)
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
     * Add objective using LinearFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads ==========

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
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        withRangeSet: Boolean? = null
    ): Try

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
     * Add objective using QuadraticFlattenData<V>.
     */
    fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData<V>,
        name: String = "",
        displayName: String? = null
    ): Try

    // ========== math.symbol type overloads for Quadratic ==========

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
