/**
 * 元模型
 * Meta model
 */
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.MathFunctionSymbol
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.toQuadraticInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.utils.functional.*
import java.nio.file.Path
import kotlin.io.path.Path

private val solverValueConverter = IntoValue.fromConverter(Flt64)

/**
 * 根据配置创建合适的 [AbstractMutableTokenTable<V>] 工厂函数。
 * 由 [AbstractMetaModel] 用于在传递给 [BasicModel] 超类构造函数之前构建符号表。
 * Factory function to create the appropriate [AbstractMutableTokenTable<V>]
 * based on configuration. Used by [AbstractMetaModel] to construct the token
 * table before passing it to the [BasicModel] superclass constructor.
 */
private fun <V> createTokenTable(
    category: Category,
    concurrent: Boolean,
    manualTokenAddition: Boolean,
    checkTokenExists: Boolean
): AbstractMutableTokenTable<V> where V : RealNumber<V>, V : NumberField<V> {
    return if (concurrent) {
        if (manualTokenAddition) {
            ConcurrentManualAddTokenTable<V>(category, checkTokenExists)
        } else {
            ConcurrentAutoTokenTable<V>(category, checkTokenExists)
        }
    } else {
        if (manualTokenAddition) {
            ManualTokenTable<V>(category, checkTokenExists)
        } else {
            AutoTokenTable<V>(category, checkTokenExists)
        }
    }
}

/**
 * 元模型密封接口
 * Sealed interface for meta models
 *
 * 元模型是优化模型的高层表示，包含约束、子目标和符号表。
 * 用户通过元模型定义优化问题，然后展开为机制模型进行求解。
 * A meta model is a high-level representation of an optimization problem,
 * containing constraints, sub-objectives, and token table.
 * Users define optimization problems through meta models, then unfold them into mechanism models for solving.
 *
 * @param V 数值类型 / The number type
 * @property converter 值转换器 / Value converter
 * @property name 模型名称 / Model name
 * @property constraints 约束列表 / Constraint list
 * @property objectCategory 目标类型（最小化/最大化）/ Objective category (minimize/maximize)
 * @property subObjects 子目标列表 / Sub-objective list
 * @property tokens 可变符号表 / Mutable token table
 * @property symbolDependencies 符号依赖关系 / Symbol dependency map
 */
sealed interface MetaModel<V> : Model<V>, AutoCloseable where V : RealNumber<V>, V : NumberField<V> {
    val converter: IntoValue<V>

    /**
     * 元模型子目标
     * Meta model sub-objective
     *
     * @param V 数值类型 / The number type
     * @property parent 父元模型 / Parent meta model
     * @property category 目标类型 / Objective category
     * @property name 子目标名称 / Sub-objective name
     * @property displayName 显示名称 / Display name
     * @property polynomial 线性多项式 / Linear polynomial
     */
    class SubObject<V>(
        val parent: MetaModel<V>,
        val category: ObjectCategory,
        val name: String,
        val displayName: String? = null,
        val polynomial: LinearPolynomial<V>
    ) where V : RealNumber<V>, V : NumberField<V> {
        /**
         * 使用父模型符号表求值。
         * Evaluate using the parent model token table.
         *
         * @param zeroIfNone 当结果未知时是否返回零 / Whether to return zero when result is unknown
         * @return 求值结果（含常数项），若任一变量结果未知且 zeroIfNone 为 false 则返回 null / The evaluation result (including constant), or null if any variable result is unknown and zeroIfNone is false
         */
        fun evaluate(zeroIfNone: Boolean = false): V? {
            return evaluate(
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        /**
         * 使用指定符号表求值。
         * Evaluate using the specified token table.
         *
         * @param tokenTable 符号表 / The token table
         * @param zeroIfNone 当结果未知时是否返回零 / Whether to return zero when result is unknown
         * @return 求值结果（含常数项）/ The evaluation result (including constant)
         */
        fun evaluate(tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): V? {
            val vZero = polynomial.constant - polynomial.constant
            var result: V? = null
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) vZero else null
                val token = tokenTable.find(variable) ?: return if (zeroIfNone) vZero else null
                val tokenValue = token.result ?: return if (zeroIfNone) vZero else null
                val term = m.coefficient * tokenValue
                result = if (result == null) term else result + term
            }
            return result ?: polynomial.constant
        }

        /**
         * 使用解向量按索引求值。
         * Evaluate using a solution vector by index lookup.
         *
         * @param results    解向量 / The solution vector
         * @param zeroIfNone 当结果未知时是否返回零 / Whether to return zero when result is unknown
         * @return 求值结果（含常数项）/ The evaluation result (including constant)
         */
        fun evaluate(results: List<V>, zeroIfNone: Boolean = false): V? {
            return evaluate(
                results = results,
                tokenTable = parent.tokens,
                zeroIfNone = zeroIfNone
            )
        }

        /**
         * 使用解向量和指定符号表按索引求值。
         * Evaluate using a solution vector and specified token table by index lookup.
         *
         * @param results    解向量 / The solution vector
         * @param tokenTable 符号表 / The token table
         * @param zeroIfNone 当结果未知时是否返回零 / Whether to return zero when result is unknown
         * @return 求值结果（含常数项）/ The evaluation result (including constant)
         */
        fun evaluate(results: List<V>, tokenTable: AbstractTokenTable<V>, zeroIfNone: Boolean = false): V? {
            val vZero = polynomial.constant - polynomial.constant
            var result: V? = null
            for (m in polynomial.monomials) {
                val variable = m.symbol as? AbstractVariableItem<*, *> ?: return if (zeroIfNone) vZero else null
                val idx = tokenTable.indexOf(variable) ?: return if (zeroIfNone) vZero else null
                val tokenValue = results[idx]
                val term = m.coefficient * tokenValue
                result = if (result == null) term else result + term
            }
            return result ?: polynomial.constant
        }

        /**
         * 刷新子目标缓存状态。
         * Flush the sub-objective cache state.
         *
         * @param force 是否强制清除缓存 / Whether to force clear the cache
         */
        fun flush(force: Boolean = false) {
            // Math polynomials don't have caching / 数学多项式无缓存
        }
    }

    val name: String
    val constraints: List<MathConstraint>
    override val objectCategory: ObjectCategory
    val subObjects: List<SubObject<V>>
    val tokens: AbstractMutableTokenTable<V>
    val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>> get() = tokens.symbolDependencies

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

    fun add(symbol: IntermediateSymbol<*>): Try {
        return tokens.add(symbol)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addSymbols")
    fun add(symbols: Iterable<IntermediateSymbol<*>>): Try {
        return tokens.add(symbols)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbols")
    fun <K> add(symbols: Map<K, IntermediateSymbol<*>>): Try {
        return tokens.add(symbols.values)
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMapSymbolLists")
    fun <K> add(symbols: Map<K, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2Symbols")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap2SymbolLists")
    fun <K1, K2> add(symbols: MultiMap2<K1, K2, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3Symbols")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap3SymbolLists")
    fun <K1, K2, K3> add(symbols: MultiMap3<K1, K2, K3, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4Symbols")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, IntermediateSymbol<*>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addMultiMap4SymbolLists")
    fun <K1, K2, K3, K4> add(symbols: MultiMap4<K1, K2, K3, K4, Iterable<IntermediateSymbol<*>>>): Try {
        for (syms in symbols.values) {
            when (val result = add(syms)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }

    fun remove(symbol: IntermediateSymbol<*>) {
        tokens.remove(symbol)
    }

    fun registerConstraintGroup(group: MetaConstraintGroup)
    fun indicesOfConstraintGroup(group: MetaConstraintGroup): IntRange?

    fun constraintsOfGroup(group: MetaConstraintGroup): List<MathConstraint> {
        return indicesOfConstraintGroup(group)?.let { indices ->
            indices.map { constraints[it] }
        } ?: emptyList()
    }

    override fun setSolution(solution: List<V>) {
        tokens.setSolution(solution)
    }

    override fun setSolution(solution: Map<AbstractVariableItem<*, *>, V>) {
        tokens.setSolution(solution)
    }

    override fun clearSolution() {
        tokens.clearSolution()
    }

    /**
     * 刷新元模型状态；当 force 为 true 时同时清除已缓存的求解结果。
     * Flush the meta model state; when [force] is `true`, also clear cached solution data.
     *
     * @param force 是否强制清除缓存 / Whether to force clear the cache
     */
    fun flush(force: Boolean = false) {
        if (force) {
            tokens.clearSolution()
        }
        tokens.flush()
        for (symbol in tokens.symbols) {
            symbol.flush(force)
        }
        for (constraint in constraints) {
            // Math inequality types don't have flush - they reference tokens via polynomial
            // 数学不等式类型无需刷新 - 它们通过多项式引用符号
        }
        for (objective in subObjects) {
            // Math polynomials don't have caching - no-op / 数学多项式无缓存 - 无操作
        }
    }

    /**
     * 导出模型到当前目录默认文件。
     * Export the model to the default file in the current directory.
     *
     * @return 操作结果 / The operation result
     */
    suspend fun export(): Try {
        return export("$name.opm")
    }

    /**
     * 导出模型到指定文件名。
     * Export the model to the specified file name.
     *
     * @param name 文件名 / The file name
     * @return 操作结果 / The operation result
     */
    suspend fun export(name: String): Try {
        return export(Path(".").resolve(name))
    }

    /**
     * 导出模型到指定路径，可选择是否展开。
     * Export the model to the specified path with optional unfolding.
     *
     * @param path   文件路径 / The file path
     * @param unfold 是否展开模型 / Whether to unfold the model
     * @return 操作结果 / The operation result
     */
    suspend fun export(path: String, unfold: Boolean): Try {
        return export(
            Path(".").resolve(name), if (unfold) {
                UInt64.zero
            } else {
                UInt64.maximum
            }
        )
    }

    /**
     * 导出模型到指定路径，指定展开层级。
     * Export the model to the specified path with the given unfold level.
     *
     * @param path   文件路径 / The file path
     * @param unfold 展开层级 / The unfold level
     * @return 操作结果 / The operation result
     */
    suspend fun export(path: String, unfold: UInt64): Try {
        return export(Path(".").resolve(name), unfold)
    }

    /**
     * 导出模型到指定路径对象。
     * Export the model to the specified path object.
     *
     * @param path   文件路径对象 / The file path object
     * @param unfold 展开层级 / The unfold level
     * @return 操作结果 / The operation result
     */
    suspend fun export(path: Path, unfold: UInt64 = UInt64.zero): Try {
        return exportMetaModel(metaModel = this, path = path, unfold = unfold)
    }

    override fun close() {
        tokens.close()
    }
}

/**
 * 线性元模型抽象接口
 * Abstract linear meta model interface
 *
 * 支持添加线性约束和分区约束。
 * Supports adding linear constraints and partition constraints.
 *
 * @param V 数值类型 / The number type
 */
interface AbstractLinearMetaModel<V> : MetaModel<V>, LinearModel<V> where V : RealNumber<V>, V : NumberField<V> {
    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintVariableWithGroup")
    fun addConstraint(
        constraint: AbstractVariableItem<*, *>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val lhs = LinearPolynomial(listOf(LinearMonomial(converter.one, constraint)), converter.zero)
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        val relation = LinearInequality<V>(lhs, rhs, Comparison.EQ)
        return addConstraint(
            relation = relation,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("addConstraintLinearPolynomialWithGroup")
    fun addConstraint(
        constraint: LinearPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val onePoly = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality<V>(constraint, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: LinearIntermediateSymbol<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = false
    ): Try {
        val lhs = constraint.toLinearPolynomial()
        val rhs = LinearPolynomial<V>(emptyList(), converter.one)
        val relation = LinearInequality<V>(lhs, rhs, Comparison.EQ)
        return addConstraint(
            relation = relation,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用数学 LinearInequality 添加约束
     * Add constraint using math LinearInequality
     */
    fun addConstraint(
        relation: LinearInequality<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        priority: Int? = null,
        withRangeSet: Boolean? = false
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionVariables")
    fun partition(
        variables: Iterable<AbstractVariableItem<*, *>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(
                monomials = variables.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionLinearSymbols")
    fun partition(
        symbols: Iterable<LinearIntermediateSymbol<V>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = LinearPolynomial(
                monomials = symbols.map { LinearMonomial(converter.one, it) }.toList(),
                constant = converter.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: LinearPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        val onePoly = LinearPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = LinearInequality<V>(polynomial, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

/**
 * 二次元模型抽象接口
 * Abstract quadratic meta model interface
 *
 * 扩展线性元模型，支持添加二次约束和分区约束。
 * Extends linear meta model, supports adding quadratic constraints and partition constraints.
 *
 * @param V 数值类型 / The number type
 */
interface AbstractQuadraticMetaModel<V> : MetaModel<V>, QuadraticModel<V> where V : RealNumber<V>, V : NumberField<V> {
    fun addConstraint(
        constraint: QuadraticPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val onePoly = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf<V>(constraint, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    fun addConstraint(
        constraint: QuadraticIntermediateSymbol<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        withRangeSet: Boolean? = null
    ): Try {
        val vPoly = constraint.toQuadraticPolynomial()
        val onePoly = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf<V>(vPoly, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用数学 QuadraticInequality 添加约束
     * Add constraint using math QuadraticInequality
     */
    fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null,
        priority: Int? = null,
        withRangeSet: Boolean? = null
    ): Try

    @Suppress("INAPPLICABLE_JVM_NAME")
    @JvmName("partitionQuadraticSymbols")
    fun partition(
        symbols: Iterable<QuadraticIntermediateSymbol<V>>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        return partition(
            polynomial = QuadraticPolynomial(
                monomials = symbols.map { it.toQuadraticPolynomial() }.flatMap { it.monomials }.toList(),
                constant = converter.zero
            ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }

    fun partition(
        polynomial: QuadraticPolynomial<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean = false,
        name: String? = null,
        displayName: String? = null,
        args: Any? = null
    ): Try {
        val onePoly = QuadraticPolynomial<V>(emptyList(), converter.one)
        return addConstraint(
            relation = QuadraticInequalityOf<V>(polynomial, onePoly, Comparison.EQ),
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args
        )
    }
}

/**
 * 元模型配置
 * Meta model configuration
 *
 * @property manualTokenAddition 是否手动添加符号 / Whether to manually add tokens
 * @property concurrent 是否并发 / Whether to use concurrency
 * @property dumpBlocking 是否阻塞式转储 / Whether to use blocking dump
 * @property withRangeSet 是否包含范围集 / Whether to include range set
 * @property checkTokenExists 是否检查符号存在性 / Whether to check token existence
 */
data class MetaModelConfiguration(
    internal val manualTokenAddition: Boolean = true,
    internal val concurrent: Boolean = true,
    internal val dumpBlocking: Boolean = false,
    internal val withRangeSet: Boolean = false,
    internal val checkTokenExists: Boolean = System.getProperty("env", "prod") != "prod"
)

/**
 * 元模型抽象基类
 * Abstract meta model base class
 *
 * 提供元模型的通用实现，包括约束组管理和符号表操作。
 * Provides common implementation for meta models, including constraint group management and token table operations.
 *
 * @param V 数值类型 / The number type
 * @property category 模型类别（线性/二次）/ Model category (linear/quadratic)
 * @property configuration 元模型配置 / Meta model configuration
 * @property converter 值转换器 / Value converter
 */
abstract class AbstractMetaModel<V>(
    val category: Category,
    internal val configuration: MetaModelConfiguration,
    override val converter: IntoValue<V>
) : BasicModel<V>(
    name = "",
    tokens = createTokenTable<V>(category, configuration.concurrent, configuration.manualTokenAddition, configuration.checkTokenExists)
), MetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // add(variable), addSymbol, addSymbolWithDependencies, removeSymbol, addConstraint, flush, close.
    // The MetaModel<V> sealed interface is also implemented; its abstract members
    // (constraints, objectCategory, subObjects, etc.) are provided by concrete subclasses.

    // Resolve diamond inheritance: both BasicModel and MetaModel<V> provide symbolDependencies.
    override val symbolDependencies: Map<IntermediateSymbol<*>, Set<IntermediateSymbol<*>>>
        get() = tokens.symbolDependencies

    // Resolve diamond inheritance: both BasicModel and MetaModel<V> provide add(item).
    // Both delegate to tokens.add(), so the behavior is identical.
    override fun add(item: AbstractVariableItem<*, *>): Try = tokens.add(item)
    override fun add(items: Iterable<AbstractVariableItem<*, *>>): Try = tokens.add(items)

    // Resolve default parameter conflict: both BasicModel.flush and MetaModel.flush
    // declare force=false. Kotlin requires an explicit override without a new default.
    override fun flush(force: Boolean) {
        super<BasicModel>.flush(force)
        // Constraints and sub-objects have no caching in the math-inequality world.
        // 在数学不等式体系中，约束和子目标无缓存。
    }

    override fun close() {
        super<BasicModel>.close()
        tokens.close()
    }

    private var currentConstraintGroup: MetaConstraintGroup? = null
    private var currentConstraintGroupIndexLowerBound: Int? = null
    private val constraintGroupIndexMap = HashMap<MetaConstraintGroup, IntRange>()

    override fun registerConstraintGroup(group: MetaConstraintGroup) {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
        }
        currentConstraintGroup = group
        currentConstraintGroupIndexLowerBound = constraints.size
    }

    override fun indicesOfConstraintGroup(group: MetaConstraintGroup): IntRange? {
        if (currentConstraintGroup != null) {
            assert(currentConstraintGroupIndexLowerBound != null)

            constraintGroupIndexMap[currentConstraintGroup!!] =
                currentConstraintGroupIndexLowerBound!!..<constraints.size
            currentConstraintGroup = null
            currentConstraintGroupIndexLowerBound = null
        }
        return constraintGroupIndexMap[group]
    }
}

/**
 * 线性元模型
 * Linear meta model
 *
 * 用于定义线性优化问题，支持线性约束和线性目标函数。
 * Used to define linear optimization problems, supporting linear constraints and linear objective functions.
 *
 * @param V 数值类型 / The number type
 * @property name 模型名称 / Model name
 * @property objectCategory 目标类型（最小化/最大化）/ Objective category (minimize/maximize)
 * @param configuration 元模型配置 / Meta model configuration
 * @param converter 值转换器 / Value converter
 */
class LinearMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration(),
    converter: IntoValue<V>
) : AbstractMetaModel<V>(fuookami.ospf.kotlin.math.symbol.Linear, configuration, converter), AbstractLinearMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<LinearInequalityConstraint<V>> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<LinearInequalityConstraint<V>> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<V>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<V>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<LinearSubObject<V>> = ArrayList()
    internal val flattenSubObjects: List<LinearSubObject<V>> by ::_flattenSubObjects

    /**
     * 使用线性多项式添加目标子项。
     * Add an objective sub-item using a linear polynomial.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param polynomial  线性多项式 / The linear polynomial
     * @param name        目标名称 / The objective name
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    fun addObject(
        category: ObjectCategory,
        polynomial: LinearPolynomial<V>,
        name: String,
        displayName: String?
    ): Try {
        _subObjects.add(
            MetaModel.SubObject<V>(
                parent = this,
                category = category,
                name = name,
                displayName = displayName,
                polynomial = polynomial
            )
        )
        return ok
    }

    /**
     * 使用 LinearFlattenData 添加目标函数（新 API）
     * Add objective using LinearFlattenData (new API)
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String,
        displayName: String?
    ): Try {
        val subObject = LinearSubObject.invoke(
            category = category,
            flattenData = flattenData,
            tokens = tokens,
            name = name,
            converter = converter
        )
        _flattenSubObjects.add(subObject)
        return ok
    }

    /**
     * 使用数学 LinearInequality 添加约束（LinearModel 接口）
     * Add constraint using math LinearInequality (LinearModel interface)
     */
    override fun addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = relation,
            group = null,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = null,
            priority = null,
            withRangeSet = withRangeSet
        )
    }

    override fun toString(): String {
        return name
    }

    /**
     * 使用数学 LinearInequality 添加约束（新 API）
     * Add constraint using math LinearInequality (new API)
     */
    override fun addConstraint(
        relation: LinearInequality<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        _relationConstraints.add(
            LinearInequalityConstraint<V>(
                inequality = relation,
                converter = converter,
                constraintName = name ?: relation.name,
                constraintDisplayName = displayName ?: relation.displayName,
                group = group,
                lazy = lazy,
                args = args,
                priority = priority
            )
        )
        return ok
    }

    companion object {
        /**
         * 创建默认 Flt64 类型的线性元模型。
         * Create a linear meta model with default Flt64 type.
         *
         * @param name            模型名称 / The model name
         * @param objectCategory  优化方向 / The optimization direction
         * @param configuration   元模型配置 / The meta model configuration
         * @return 线性元模型实例 / The linear meta model instance
         */
        operator fun invoke(
            name: String = "",
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            configuration: MetaModelConfiguration = MetaModelConfiguration()
        ): LinearMetaModel<Flt64> = LinearMetaModel(
            name = name,
            objectCategory = objectCategory,
            configuration = configuration,
            converter = solverValueConverter
        )

        /**
         * 创建自定义类型的线性元模型。
         * Create a linear meta model with custom type.
         *
         * @param name            模型名称 / The model name
         * @param converter       Flt64 值转换器 / The Flt64 value converter
         * @param objectCategory  优化方向 / The optimization direction
         * @param configuration   元模型配置 / The meta model configuration
         * @return 线性元模型实例 / The linear meta model instance
         */
        operator fun <V> invoke(
            name: String,
            converter: Flt64ValueConverter<V>,
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            configuration: MetaModelConfiguration = MetaModelConfiguration()
        ): LinearMetaModel<V> where V : RealNumber<V>, V : NumberField<V> = LinearMetaModel(
            name = name,
            objectCategory = objectCategory,
            configuration = configuration,
            converter = IntoValue.fromConverter(converter)
        )
    }
}

/**
 * 二次元模型
 * Quadratic meta model
 *
 * 用于定义二次优化问题，支持线性和二次约束以及二次目标函数。
 * Used to define quadratic optimization problems, supporting linear and quadratic constraints and quadratic objective functions.
 *
 * @param V 数值类型 / The number type
 * @property name 模型名称 / Model name
 * @property objectCategory 目标类型（最小化/最大化）/ Objective category (minimize/maximize)
 * @param configuration 元模型配置 / Meta model configuration
 * @param converter 值转换器 / Value converter
 */
class QuadraticMetaModel<V>(
    override var name: String = "",
    override val objectCategory: ObjectCategory = ObjectCategory.Minimum,
    configuration: MetaModelConfiguration = MetaModelConfiguration(),
    converter: IntoValue<V>
) : AbstractMetaModel<V>(fuookami.ospf.kotlin.math.symbol.Quadratic, configuration, converter), AbstractLinearMetaModel<V>, AbstractQuadraticMetaModel<V> where V : RealNumber<V>, V : NumberField<V> {
    // Math inequality-based constraints storage
    internal val _relationConstraints: MutableList<QuadraticInequalityConstraint<V>> = ArrayList()
    override val constraints: List<MathConstraint> get() = _relationConstraints
    val relationConstraints: List<QuadraticInequalityConstraint<V>> by ::_relationConstraints

    internal val _subObjects: MutableList<MetaModel.SubObject<V>> = ArrayList()
    override val subObjects: List<MetaModel.SubObject<V>> by ::_subObjects

    // NEW: FlattenData-based sub-objects storage
    internal val _flattenSubObjects: MutableList<QuadraticFlattenSubObject<V>> = ArrayList()
    internal val flattenSubObjects: List<QuadraticFlattenSubObject<V>> by ::_flattenSubObjects

    /**
     * 添加数学 LinearInequality 约束 - 内部转换为 QuadraticInequality
     * Add math LinearInequality constraint - converts to QuadraticInequality internally
     */
    override fun addConstraint(
        relation: LinearInequality<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        // Promote linear inequality to quadratic: each linear monomial c*x becomes quadratic c*x*null
        // 将线性不等式提升为二次：每个线性单项式 c*x 变为二次 c*x*null
        val qLhs = QuadraticPolynomial(
            monomials = relation.lhs.monomials.map { QuadraticMonomial(it.coefficient, it.symbol, null) },
            constant = relation.lhs.constant
        )
        val qRhs = QuadraticPolynomial(
            monomials = relation.rhs.monomials.map { QuadraticMonomial(it.coefficient, it.symbol, null) },
            constant = relation.rhs.constant
        )
        val quadraticRelation = QuadraticInequalityOf<V>(qLhs, qRhs, relation.comparison)
        return addConstraint(
            relation = quadraticRelation,
            group = group,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = args,
            priority = priority,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用数学 LinearInequality 添加约束（LinearModel 接口）
     * Add constraint using math LinearInequality (LinearModel interface)
     */
    override fun addConstraint(
        relation: LinearInequality<V>,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = relation,
            group = null,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = null,
            priority = null,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用数学 QuadraticInequality 添加约束（QuadraticModel 接口）
     * Add constraint using math QuadraticInequality (QuadraticModel interface)
     */
    override fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        withRangeSet: Boolean?
    ): Try {
        return addConstraint(
            relation = relation,
            group = null,
            lazy = lazy,
            name = name,
            displayName = displayName,
            args = null,
            priority = null,
            withRangeSet = withRangeSet
        )
    }

    /**
     * 使用 LinearFlattenData 添加目标函数（新 API - LinearModel 接口），
     * 内部转换为 QuadraticFlattenData。
     * Add objective using LinearFlattenData (new API - LinearModel interface).
     * Converts to QuadraticFlattenData internally.
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: LinearFlattenData<V>,
        name: String,
        displayName: String?
    ): Try {
        return addObject(
            category = category,
            flattenData = flattenData.toQuadraticFlattenData(),
            name = name,
            displayName = displayName
        )
    }

    /**
     * 使用数学 QuadraticInequality 添加约束（新 API）
     * Add constraint using math QuadraticInequality (new API)
     */
    override fun addConstraint(
        relation: QuadraticInequalityOf<V>,
        group: MetaConstraintGroup?,
        lazy: Boolean,
        name: String?,
        displayName: String?,
        args: Any?,
        priority: Int?,
        withRangeSet: Boolean?
    ): Try {
        _relationConstraints.add(
            QuadraticInequalityConstraint<V>(
                inequality = relation,
                converter = converter,
                constraintName = name ?: relation.name,
                constraintDisplayName = displayName ?: relation.displayName,
                group = group,
                lazy = lazy,
                args = args,
                priority = priority
            )
        )
        return ok
    }

    /**
     * 使用二次多项式添加目标子项。
     * Add an objective sub-item using a quadratic polynomial.
     *
     * @param category    目标类别（最小化/最大化） / The objective category (minimize/maximize)
     * @param polynomial  二次多项式 / The quadratic polynomial
     * @param name        目标名称 / The objective name
     * @param displayName 目标显示名称（可为 null） / The objective display name (nullable)
     * @return 操作结果 / The operation result
     */
    fun addObject(
        category: ObjectCategory,
        polynomial: QuadraticPolynomial<V>,
        name: String,
        displayName: String?
    ): Try {
        val flattenData = QuadraticFlattenData<V>(
            monomials = polynomial.monomials,
            constant = polynomial.constant
        )
        _flattenSubObjects.add(
            QuadraticFlattenSubObject(
                category = category,
                flattenData = flattenData,
                name = name,
                displayName = displayName
            )
        )
        // Keep the linear projection for shared object-function handling.
        // 保留线性投影视图，供共享目标函数处理使用。
        // 保留线性投影视图，供共享目标函数流程使用。
        val linearPoly = LinearPolynomial(
            monomials = polynomial.monomials.map { LinearMonomial(it.coefficient, it.symbol1) },
            constant = polynomial.constant
        )
        _subObjects.add(
            MetaModel.SubObject<V>(
                parent = this,
                category = category,
                name = name,
                displayName = displayName,
                polynomial = linearPoly
            )
        )
        return ok
    }

    /**
     * 使用 QuadraticFlattenData 添加目标函数（新 API），
     * 使用转换器将 Flt64 系数转换为 V 类型。
     * Add objective using QuadraticFlattenData (new API).
     * Converts Flt64 coefficients to V-typed using converter.
     */
    override fun addObject(
        category: ObjectCategory,
        flattenData: QuadraticFlattenData<V>,
        name: String,
        displayName: String?
    ): Try {
        _flattenSubObjects.add(
            QuadraticFlattenSubObject(
                category = category,
                flattenData = flattenData,
                name = name,
                displayName = displayName
            )
        )
        return ok
    }

    companion object {
        /**
         * 创建默认 Flt64 类型的二次元模型。
         * Create a quadratic meta model with default Flt64 type.
         *
         * @param name            模型名称 / The model name
         * @param objectCategory  优化方向 / The optimization direction
         * @param configuration   元模型配置 / The meta model configuration
         * @return 二次元模型实例 / The quadratic meta model instance
         */
        operator fun invoke(
            name: String = "",
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            configuration: MetaModelConfiguration = MetaModelConfiguration()
        ): QuadraticMetaModel<Flt64> = QuadraticMetaModel(
            name = name,
            objectCategory = objectCategory,
            configuration = configuration,
            converter = solverValueConverter
        )

        /**
         * 创建自定义类型的二次元模型。
         * Create a quadratic meta model with custom type.
         *
         * @param name            模型名称 / The model name
         * @param converter       Flt64 值转换器 / The Flt64 value converter
         * @param objectCategory  优化方向 / The optimization direction
         * @param configuration   元模型配置 / The meta model configuration
         * @return 二次元模型实例 / The quadratic meta model instance
         */
        operator fun <V> invoke(
            name: String,
            converter: Flt64ValueConverter<V>,
            objectCategory: ObjectCategory = ObjectCategory.Minimum,
            configuration: MetaModelConfiguration = MetaModelConfiguration()
        ): QuadraticMetaModel<V> where V : RealNumber<V>, V : NumberField<V> = QuadraticMetaModel(
            name = name,
            objectCategory = objectCategory,
            configuration = configuration,
            converter = IntoValue.fromConverter(converter)
        )
    }
}
