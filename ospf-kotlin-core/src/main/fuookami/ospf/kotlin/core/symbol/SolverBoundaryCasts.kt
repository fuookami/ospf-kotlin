/**
 * 求解器边界类型转换 / Solver boundary type casts
 *
 * 提供运行时求解器边界的 Flt64 类型转换工具，将星投影引用转换回 Flt64 以调用泛型方法。
 *
 * Provides runtime solver-boundary Flt64 type cast utilities, converting star-projected
 * references back to Flt64 for calling generic methods.
*/
package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.*

/**
 * 集中的求解器边界类型转换。
 * Centralized solver-boundary type casts.
 *
 * 运行时所有求解器边界实例都使用 V=Flt64；这里把星投影引用转换回 Flt64，以便调用泛型方法。
 * 转换由于 JVM 类型擦除是安全的但未检查的。
 *
 * At runtime, all solver-boundary instances use V=Flt64. These casts convert
 * star-projected (V=*) references back to Flt64 so that generic methods can be called.
 * The casts are safe but unchecked due to JVM type erasure.
 *
 * 本对象是框架中所有 UNCHECKED_CAST 抑制的唯一位置。请勿在其他地方添加 @Suppress("UNCHECKED_CAST")。
 * This object is the single location for all UNCHECKED_CAST suppressions
 * in the framework. Do not add @Suppress("UNCHECKED_CAST") elsewhere.
*/
@Suppress("UNCHECKED_CAST")
internal object SolverBoundaryCasts {

    /** Flt64 恒等值转换器 / Flt64 identity value converter */
    private val solverValueConverter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

    /**
     * 注册辅助令牌（星投影版本）。
     * Register auxiliary tokens (star-projected version).
     *
     * @param symbol 中间符号实例 / Intermediate symbol instance
     * @param tokens 令牌集合实例 / Token collection instance
     * @return 操作结果 / Operation result
    */
    fun registerAuxiliaryTokensStar(
        symbol: Any,
        tokens: Any
    ): Try {
        return (symbol as IntermediateSymbol<Flt64>).registerAuxiliaryTokens(tokens as AddableTokenCollection<Flt64>)
    }

    /**
     * 准备符号值（星投影版本）。
     * Prepare symbol value (star-projected version).
     *
     * @param symbol 中间符号实例 / Intermediate symbol instance
     * @param fixedValues 固定值映射（可空） / Fixed values map (nullable)
     * @param tokenTable 令牌表 / Token table
     * @return 求值结果 / Evaluation result
    */
    fun prepareStar(
        symbol: Any,
        fixedValues: Map<Symbol, Flt64>?,
        tokenTable: AbstractTokenTable<Flt64>
    ): Flt64? {
        val sym = symbol as IntermediateSymbol<Flt64>
        return if (fixedValues.isNullOrEmpty()) {
            sym.prepare(null, tokenTable, solverValueConverter)
        } else {
            sym.prepare(fixedValues, tokenTable, solverValueConverter)
        }
    }

    /**
     * 注册线性约束（星投影版本）。
     * Register linear constraints (star-projected version).
     *
     * @param symbol 数学函数符号实例 / Math function symbol instance
     * @param model 线性机制模型实例 / Linear mechanism model instance
     * @return 操作结果 / Operation result
    */
    fun registerConstraintsLinearStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as MathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractLinearMechanismModel<Flt64>)
    }

    /**
     * 注册二次约束（星投影版本）。
     * Register quadratic constraints (star-projected version).
     *
     * @param symbol 二次数学函数符号实例 / Quadratic math function symbol instance
     * @param model 二次机制模型实例 / Quadratic mechanism model instance
     * @return 操作结果 / Operation result
    */
    fun registerConstraintsQuadraticStar(
        symbol: Any,
        model: Any
    ): Try {
        return (symbol as QuadraticMathFunctionSymbolBase<Flt64>).registerConstraints(model as AbstractQuadraticMechanismModel<Flt64>)
    }

    /**
     * 将机制模型转换为线性机制模型（Flt64 类型）。
     * Cast mechanism model to linear mechanism model (Flt64 type).
     *
     * @param model 机制模型 / Mechanism model
     * @return 线性机制模型 / Linear mechanism model
    */
    fun <V> castLinearMechanismModelStar(model: MechanismModel<V>): LinearMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMechanismModel<Flt64>
    }

    /**
     * 将机制模型转换为二次机制模型（Flt64 类型）。
     * Cast mechanism model to quadratic mechanism model (Flt64 type).
     *
     * @param model 机制模型 / Mechanism model
     * @return 二次机制模型 / Quadratic mechanism model
    */
    fun <V> castQuadraticMechanismModelStar(model: MechanismModel<V>): QuadraticMechanismModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMechanismModel<Flt64>
    }

    /**
     * 将线性元模型转换为 Flt64 类型。
     * Cast linear meta model to Flt64 type.
     *
     * @param model 线性元模型 / Linear meta model
     * @return Flt64 类型的线性元模型 / Linear meta model of Flt64 type
    */
    fun <V> castLinearMetaModelStar(model: LinearMetaModel<V>): LinearMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as LinearMetaModel<Flt64>
    }

    /**
     * 将二次元模型转换为 Flt64 类型。
     * Cast quadratic meta model to Flt64 type.
     *
     * @param model 二次元模型 / Quadratic meta model
     * @return Flt64 类型的二次元模型 / Quadratic meta model of Flt64 type
    */
    fun <V> castQuadraticMetaModelStar(model: QuadraticMetaModel<V>): QuadraticMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return model as QuadraticMetaModel<Flt64>
    }

    /**
     * 将线性约束实现转换为 Flt64 类型约束。
     * Cast linear constraint implementation to Flt64 constraint.
     *
     * @param constraint 线性约束实现 / Linear constraint implementation
     * @return Flt64 类型的线性约束 / Linear constraint of Flt64 type
    */
    fun <V> linearConstraintAsFlt64(constraint: LinearConstraintImpl<V>): Constraint<Flt64, Linear>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<Flt64, Linear>
    }

    /**
     * 将线性约束实现列表转换为 Flt64 类型约束列表。
     * Cast list of linear constraint implementations to Flt64 constraints.
     *
     * @param constraints 线性约束实现列表 / List of linear constraint implementations
     * @return Flt64 类型的线性约束列表 / List of Flt64 linear constraints
    */
    fun <V> linearConstraintsAsFlt64(constraints: List<LinearConstraintImpl<V>>): List<Constraint<Flt64, Linear>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<Flt64, Linear>>
    }

    /**
     * 将二次约束实现转换为 Flt64 类型约束。
     * Cast quadratic constraint implementation to Flt64 constraint.
     *
     * @param constraint 二次约束实现 / Quadratic constraint implementation
     * @return Flt64 类型的二次约束 / Quadratic constraint of Flt64 type
    */
    fun <V> quadraticConstraintAsFlt64(constraint: QuadraticConstraintImpl<V>): Constraint<Flt64, Quadratic>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraint as Constraint<Flt64, Quadratic>
    }

    /**
     * 将二次约束实现列表转换为 Flt64 类型约束列表。
     * Cast list of quadratic constraint implementations to Flt64 constraints.
     *
     * @param constraints 二次约束实现列表 / List of quadratic constraint implementations
     * @return Flt64 类型的二次约束列表 / List of Flt64 quadratic constraints
    */
    fun <V> quadraticConstraintsAsFlt64(constraints: List<QuadraticConstraintImpl<V>>): List<Constraint<Flt64, Quadratic>>
        where V : RealNumber<V>, V : NumberField<V> {
        return constraints as List<Constraint<Flt64, Quadratic>>
    }

    /**
     * 将对象安全转换为线性不等式。
     * Safely cast object to linear inequality.
     *
     * @param cut 待转换的对象 / Object to cast
     * @return 线性不等式，若类型不匹配则返回 null / Linear inequality, or null if type mismatch
    */
    fun <V> linearInequalityAs(cut: Any): LinearInequality<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? LinearInequality<V>
    }

    /**
     * 将对象安全转换为二次不等式。
     * Safely cast object to quadratic inequality.
     *
     * @param cut 待转换的对象 / Object to cast
     * @return 二次不等式，若类型不匹配则返回 null / Quadratic inequality, or null if type mismatch
    */
    fun <V> quadraticInequalityAs(cut: Any): QuadraticInequalityOf<V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return cut as? QuadraticInequalityOf<V>
    }

    /**
     * 将令牌表的令牌列表转换为 Flt64 类型。
     * Cast token table's token list to Flt64 type.
     *
     * @param tokenTable 令牌表 / Token table
     * @return Flt64 类型的令牌列表 / Flt64 token list
    */
    fun <V> tokenListAsFlt64(tokenTable: AbstractTokenTable<V>): AbstractTokenList<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable.tokenList as AbstractTokenList<Flt64>
    }

    /**
     * 将可空令牌表的令牌列表转换为 Flt64 类型。
     * Cast nullable token table's token list to Flt64 type.
     *
     * @param tokenTable 令牌表（可空） / Token table (nullable)
     * @return Flt64 类型的令牌列表（可空） / Flt64 token list (nullable)
    */
    fun <V> tokenListAsFlt64OrNull(tokenTable: AbstractTokenTable<V>?): AbstractTokenList<Flt64>?
        where V : RealNumber<V>, V : NumberField<V> {
        return tokenTable?.let { tokenListAsFlt64(it) }
    }

    /**
     * 使用转换器将 Flt64 值映射转换为目标类型值映射。
     * Convert Flt64 value map to target type value map using converter.
     *
     * @param values Flt64 类型的值映射 / Flt64 value map
     * @param converter 值转换器 / Value converter
     * @return 目标类型的值映射 / Target type value map
    */
    fun <V> mapValues(values: Map<Symbol, Flt64>, converter: IntoValue<V>): Map<Symbol, V>
        where V : RealNumber<V>, V : NumberField<V> {
        return values.mapValues { converter.intoValue(it.value) }
    }

    /**
     * 将依赖符号转换为目标类型中间符号。
     * Cast dependency symbol to target type intermediate symbol.
     *
     * @param dependency 依赖中间符号 / Dependency intermediate symbol
     * @return 目标类型的中间符号 / Target type intermediate symbol
    */
    fun <V> dependencyAsIntermediate(dependency: IntermediateSymbol<*>): IntermediateSymbol<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return dependency as IntermediateSymbol<V>
    }

    /**
     * 将可变线性多项式转换为 Flt64 类型。
     * Convert mutable linear polynomial to Flt64 type.
     *
     * @param polynomial 可变线性多项式 / Mutable linear polynomial
     * @return Flt64 类型的可变线性多项式 / Flt64 mutable linear polynomial
    */
    fun <V> linearPolynomialAsFlt64(polynomial: MutableLinearPolynomial<V>): MutableLinearPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return MutableLinearPolynomial(
            monomials = polynomial.monomials.map { monomial ->
                LinearMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol = monomial.symbol
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    /**
     * 将可变二次多项式转换为 Flt64 类型。
     * Convert mutable quadratic polynomial to Flt64 type.
     *
     * @param polynomial 可变二次多项式 / Mutable quadratic polynomial
     * @return Flt64 类型的可变二次多项式 / Flt64 mutable quadratic polynomial
    */
    fun <V> quadraticPolynomialAsFlt64(polynomial: MutableQuadraticPolynomial<V>): MutableQuadraticPolynomial<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
        return MutableQuadraticPolynomial(
            monomials = polynomial.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    /**
     * 将对象安全转换为 Flt64 类型令牌表。
     * Safely cast object to Flt64 token table.
     *
     * @param tokenTable 待转换的对象（可空） / Object to cast (nullable)
     * @return Flt64 类型令牌表（可空） / Flt64 token table (nullable)
    */
    fun tokenTableAsFlt64OrNull(tokenTable: Any?): AbstractTokenTable<Flt64>? {
        return tokenTable as? AbstractTokenTable<Flt64>
    }

    /**
     * 将符号安全转换为目标类型中间符号。
     * Safely cast symbol to target type intermediate symbol.
     *
     * @param symbol 符号（可空） / Symbol (nullable)
     * @return 目标类型的中间符号（可空） / Target type intermediate symbol (nullable)
    */
    fun <V> symbolAsIntermediateStar(symbol: Symbol?): IntermediateSymbol<out V>?
        where V : RealNumber<V>, V : NumberField<V> {
        return symbol as? IntermediateSymbol<out V>
    }

    /**
     * 获取线性中间符号的 Flt64 扁平化单项式数据。
     * Get Flt64 flattened monomial data for linear intermediate symbol.
     *
     * @param symbol 线性中间符号实例 / Linear intermediate symbol instance
     * @return Flt64 类型的线性扁平化数据 / Flt64 linear flatten data
    */
    fun linearSolverFlattenedMonomials(symbol: Any): LinearFlattenData<Flt64> {
        val polynomial = (symbol as LinearIntermediateSymbol<*>).toLinearPolynomial()
        return LinearFlattenData(
            monomials = polynomial.monomials.map { monomial ->
                LinearMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol = monomial.symbol
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    /**
     * 获取二次中间符号的 Flt64 扁平化单项式数据。
     * Get Flt64 flattened monomial data for quadratic intermediate symbol.
     *
     * @param symbol 二次中间符号实例 / Quadratic intermediate symbol instance
     * @return Flt64 类型的二次扁平化数据 / Flt64 quadratic flatten data
    */
    fun quadraticSolverFlattenedMonomials(symbol: Any): QuadraticFlattenData<Flt64> {
        val polynomial = (symbol as QuadraticIntermediateSymbol<*>).toQuadraticPolynomial()
        return QuadraticFlattenData(
            monomials = polynomial.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient.toFlt64(),
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = polynomial.constant.toFlt64()
        )
    }

    /**
     * 将 Flt64 值域转换为目标类型的表达式值域。
     * Convert Flt64 value range to target type expression range.
     *
     * @param rangeFlt64 Flt64 值域（可空） / Flt64 value range (nullable)
     * @return 目标类型的表达式值域 / Target type expression range
    */
    @Suppress("UNCHECKED_CAST")
    fun <V> expressionRangeFromFlt64(rangeFlt64: ValueRange<Flt64>?): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(rangeFlt64 as ValueRange<V>?, Flt64 as RealNumberConstants<V>)
    }

    /**
     * 创建目标类型的完整表达式值域（无界）。
     * Create full (unbounded) expression range for target type.
     *
     * @return 完整的表达式值域 / Full expression range
    */
    @Suppress("UNCHECKED_CAST")
    fun <V> fullExpressionRange(): ExpressionRange<V>
        where V : RealNumber<V>, V : NumberField<V> {
        return ExpressionRange(Flt64 as RealNumberConstants<V>)
    }

    /**
     * 获取中间符号的 Flt64 类型表达式值域。
     * Get Flt64 expression range for intermediate symbol.
     *
     * @param symbol 中间符号实例 / Intermediate symbol instance
     * @return Flt64 类型的表达式值域（可空） / Flt64 expression range (nullable)
    */
    fun rangeAsFlt64(symbol: Any): ExpressionRange<Flt64>? {
        val flt64Symbol = symbol as IntermediateSymbol<Flt64>
        return flt64Symbol.range as ExpressionRange<Flt64>?
    }
}

/**
 * 为求解器将线性元模型转换为 Flt64 类型。
 * Cast linear meta model to Flt64 type for solver.
 *
 * @param model 线性元模型 / Linear meta model
 * @return Flt64 类型的线性元模型 / Flt64 linear meta model
*/
fun <V> castLinearMetaModelForSolver(model: LinearMetaModel<V>): LinearMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castLinearMetaModelStar(model)
}

/**
 * 为求解器将二次元模型转换为 Flt64 类型。
 * Cast quadratic meta model to Flt64 type for solver.
 *
 * @param model 二次元模型 / Quadratic meta model
 * @return Flt64 类型的二次元模型 / Flt64 quadratic meta model
*/
fun <V> castQuadraticMetaModelForSolver(model: QuadraticMetaModel<V>): QuadraticMetaModel<Flt64>
        where V : RealNumber<V>, V : NumberField<V> {
    return SolverBoundaryCasts.castQuadraticMetaModelStar(model)
}
