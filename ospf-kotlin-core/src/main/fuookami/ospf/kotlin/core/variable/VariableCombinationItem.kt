/**
 * 变量组合项及其多维数组视图，支持各数值类型的维度化变量组合。
 * Variable combination items and their multi-array views, supporting dimensional variable combinations for all numeric types.
 */
package fuookami.ospf.kotlin.core.variable

import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 组合变量项的父级接口，提供维度、标识符和形状信息。
 * Parent interface for combination variable items, providing dimension, identifier, and shape information.
 *
 * @param S 形状类型 / The shape type
 */
interface CombinationVariableItemParent<S : Shape> {
    /** 变量维度 / Variable dimension */
    val dimension: Int
    /** 变量标识符 / Variable identifier */
    val identifier: UInt64
    /** 形状 / Shape */
    val shape: Shape
}

/**
 * 组合变量中的单个变量项，通过父级组合获取维度和标识符。
 * Individual variable item within a combination, obtaining dimension and identifier from the parent combination.
 *
 * @property parent 父级组合变量 / Parent combination variable
 * @param type 变量类型 / Variable type
 * @param name 变量名称 / Variable name
 * @property index 在组合中的索引 / Index within the combination
 * @param constants 数值类型常量 / Numeric type constants
 */
class CombinationVariableItem<T, Type : VariableType<T>>(
    private val parent: CombinationVariableItemParent<*>,
    type: Type,
    name: String,
    override val index: Int,
    constants: RealNumberConstants<T>
) : AbstractVariableItem<T, Type>(type, name, constants) where T : RealNumber<T>, T : NumberField<T> {
    override val dimension by parent::dimension
    override val identifier by parent::identifier
    override val vectorView by lazy { parent.shape.vector(index) }
}

/**
 * 变量组合的密封基类，将多个 CombinationVariableItem 组织为多维数组。
 * Sealed base class for variable combinations, organizing multiple CombinationVariableItems as a multi-array.
 *
 * @property type 变量类型 / Variable type
 * @property name 组合名称 / Combination name
 * @property constants 数值类型常量 / Numeric type constants
 */
sealed class VariableCombination<T, Type : VariableType<T>, S : Shape>(
    val type: Type,
    val name: String,
    private val constants: RealNumberConstants<T>,
    shape: S
) : MultiArray<CombinationVariableItem<T, Type>, S>(shape), CombinationVariableItemParent<S>
        where T : RealNumber<T>, T : NumberField<T> {
    val items get() = this
    override val identifier = IdentifierGenerator.gen()

    init {
        super.init { i, v ->
            CombinationVariableItem(this, type, "${name}_${v.joinToString("_") { "$it" }}", i, constants)
        }
    }
}

/**
 * 带物理单位的变量组合密封基类。
 * Sealed base class for variable combinations with physical units.
 *
 * @property type 变量类型 / Variable type
 * @property name 组合名称 / Combination name
 * @property constants 数值类型常量 / Numeric type constants
 * @param unit 物理单位 / Physical unit
 */
sealed class QuantityVariableCombination<T, Type : VariableType<T>, S : Shape>(
    val type: Type,
    val name: String,
    private val constants: RealNumberConstants<T>,
    shape: S,
    unit: PhysicalUnit
) : MultiArray<Quantity<CombinationVariableItem<T, Type>>, S>(shape), CombinationVariableItemParent<S>
        where T : RealNumber<T>, T : NumberField<T> {
    val items get() = this
    override val identifier = IdentifierGenerator.gen()

    init {
        super.init { i, v ->
            Quantity(CombinationVariableItem(this, type, "${name}_${v.joinToString("_") { "$it" }}", i, constants), unit)
        }
    }
}

/** 一维变量组合类型别名 / 1D variable combination type alias */
typealias VariableCombination1<T, Type> = VariableCombination<T, Type, Shape1>
/** 二维变量组合类型别名 / 2D variable combination type alias */
typealias VariableCombination2<T, Type> = VariableCombination<T, Type, Shape2>
/** 三维变量组合类型别名 / 3D variable combination type alias */
typealias VariableCombination3<T, Type> = VariableCombination<T, Type, Shape3>
/** 四维变量组合类型别名 / 4D variable combination type alias */
typealias VariableCombination4<T, Type> = VariableCombination<T, Type, Shape4>
/** 动态维度变量组合类型别名 / Dynamic dimension variable combination type alias */
typealias DynVariableCombination<T, Type> = VariableCombination<T, Type, DynShape>
/** 变量组合视图类型别名 / Variable combination view type alias */
typealias VariableCombinationView<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, *>
/** 一维变量组合视图类型别名 / 1D variable combination view type alias */
typealias VariableCombinationView1<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape1>
/** 二维变量组合视图类型别名 / 2D variable combination view type alias */
typealias VariableCombinationView2<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape2>
/** 三维变量组合视图类型别名 / 3D variable combination view type alias */
typealias VariableCombinationView3<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape3>
/** 四维变量组合视图类型别名 / 4D variable combination view type alias */
typealias VariableCombinationView4<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape4>
/** 动态维度变量组合视图类型别名 / Dynamic dimension variable combination view type alias */
typealias DynVariableCombinationView<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, DynShape>

/** 一维物理量变量组合类型别名 / 1D quantity variable combination type alias */
typealias QuantityVariableCombination1<T, Type> = QuantityVariableCombination<T, Type, Shape1>
/** 二维物理量变量组合类型别名 / 2D quantity variable combination type alias */
typealias QuantityVariableCombination2<T, Type> = QuantityVariableCombination<T, Type, Shape2>
/** 三维物理量变量组合类型别名 / 3D quantity variable combination type alias */
typealias QuantityVariableCombination3<T, Type> = QuantityVariableCombination<T, Type, Shape3>
/** 四维物理量变量组合类型别名 / 4D quantity variable combination type alias */
typealias QuantityVariableCombination4<T, Type> = QuantityVariableCombination<T, Type, Shape4>
/** 动态维度物理量变量组合类型别名 / Dynamic dimension quantity variable combination type alias */
typealias DynQuantityVariableCombination<T, Type> = QuantityVariableCombination<T, Type, DynShape>
/** 物理量变量组合视图类型别名 / Quantity variable combination view type alias */
typealias QuantityVariableCombinationView<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, *>
/** 一维物理量变量组合视图类型别名 / 1D quantity variable combination view type alias */
typealias QuantityVariableCombinationView1<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, Shape1>
/** 二维物理量变量组合视图类型别名 / 2D quantity variable combination view type alias */
typealias QuantityVariableCombinationView2<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, Shape2>
/** 三维物理量变量组合视图类型别名 / 3D quantity variable combination view type alias */
typealias QuantityVariableCombinationView3<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, Shape3>
/** 四维物理量变量组合视图类型别名 / 4D quantity variable combination view type alias */
typealias QuantityVariableCombinationView4<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, Shape4>
/** 动态维度物理量变量组合视图类型别名 / Dynamic dimension quantity variable combination view type alias */
typealias DynQuantityVariableCombinationView<T, Type> = MultiArrayView<Quantity<CombinationVariableItem<T, Type>>, DynShape>

/** 一维变量组合简化类型别名 / 1D variable combination simplified type alias */
typealias Variable1<Type> = VariableCombination<*, Type, Shape1>
/** 二维变量组合简化类型别名 / 2D variable combination simplified type alias */
typealias Variable2<Type> = VariableCombination<*, Type, Shape2>
/** 三维变量组合简化类型别名 / 3D variable combination simplified type alias */
typealias Variable3<Type> = VariableCombination<*, Type, Shape3>
/** 四维变量组合简化类型别名 / 4D variable combination simplified type alias */
typealias Variable4<Type> = VariableCombination<*, Type, Shape4>
/** 动态维度变量组合简化类型别名 / Dynamic dimension variable combination simplified type alias */
typealias DynVariable<Type> = VariableCombination<*, Type, DynShape>
/** 变量组合视图简化类型别名 / Variable combination view simplified type alias */
typealias VariableView<Type> = VariableCombinationView1<*, Type>
/** 一维变量组合视图简化类型别名 / 1D variable combination view simplified type alias */
typealias VariableView1<Type> = VariableCombinationView1<*, Type>
/** 二维变量组合视图简化类型别名 / 2D variable combination view simplified type alias */
typealias VariableView2<Type> = VariableCombinationView2<*, Type>
/** 三维变量组合视图简化类型别名 / 3D variable combination view simplified type alias */
typealias VariableView3<Type> = VariableCombinationView3<*, Type>
/** 四维变量组合视图简化类型别名 / 4D variable combination view simplified type alias */
typealias VariableView4<Type> = VariableCombinationView4<*, Type>
/** 动态维度变量组合视图简化类型别名 / Dynamic dimension variable combination view simplified type alias */
typealias DynVariableView<Type> = DynVariableCombinationView<*, Type>

/** 一维物理量变量组合简化类型别名 / 1D quantity variable combination simplified type alias */
typealias QuantityVariable1<Type> = QuantityVariableCombination<*, Type, Shape1>
/** 二维物理量变量组合简化类型别名 / 2D quantity variable combination simplified type alias */
typealias QuantityVariable2<Type> = QuantityVariableCombination<*, Type, Shape2>
/** 三维物理量变量组合简化类型别名 / 3D quantity variable combination simplified type alias */
typealias QuantityVariable3<Type> = QuantityVariableCombination<*, Type, Shape3>
/** 四维物理量变量组合简化类型别名 / 4D quantity variable combination simplified type alias */
typealias QuantityVariable4<Type> = QuantityVariableCombination<*, Type, Shape4>
/** 动态维度物理量变量组合简化类型别名 / Dynamic dimension quantity variable combination simplified type alias */
typealias DynQuantityVariable<Type> = QuantityVariableCombination<*, Type, DynShape>
/** 物理量变量组合视图简化类型别名 / Quantity variable combination view simplified type alias */
typealias QuantityVariableView<Type> = QuantityVariableCombinationView1<*, Type>
/** 一维物理量变量组合视图简化类型别名 / 1D quantity variable combination view simplified type alias */
typealias QuantityVariableView1<Type> = QuantityVariableCombinationView1<*, Type>
/** 二维物理量变量组合视图简化类型别名 / 2D quantity variable combination view simplified type alias */
typealias QuantityVariableView2<Type> = QuantityVariableCombinationView2<*, Type>
/** 三维物理量变量组合视图简化类型别名 / 3D quantity variable combination view simplified type alias */
typealias QuantityVariableView3<Type> = QuantityVariableCombinationView3<*, Type>
/** 四维物理量变量组合视图简化类型别名 / 4D quantity variable combination view simplified type alias */
typealias QuantityVariableView4<Type> = QuantityVariableCombinationView4<*, Type>
/** 动态维度物理量变量组合视图简化类型别名 / Dynamic dimension quantity variable combination view simplified type alias */
typealias DynQuantityVariableView<Type> = DynQuantityVariableCombinationView<*, Type>

/**
 * 一维二值变量组合 / 1D binary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BinVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt8, Binary>(Binary, name, UInt8, shape)
/**
 * 二维二值变量组合 / 2D binary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BinVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt8, Binary>(Binary, name, UInt8, shape)
/**
 * 三维二值变量组合 / 3D binary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BinVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt8, Binary>(Binary, name, UInt8, shape)
/**
 * 四维二值变量组合 / 4D binary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BinVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt8, Binary>(Binary, name, UInt8, shape)
/**
 * 动态维度二值变量组合 / Dynamic dimension binary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynBinVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt8, Binary>(Binary, name, UInt8, shape)
/** 二值变量组合视图 / Binary variable combination view */
typealias BinVariableView = VariableCombinationView<UInt8, Binary>
/** 一维二值变量组合视图 / 1D binary variable combination view */
typealias BinVariableView1 = VariableCombinationView1<UInt8, Binary>
/** 二维二值变量组合视图 / 2D binary variable combination view */
typealias BinVariableView2 = VariableCombinationView2<UInt8, Binary>
/** 三维二值变量组合视图 / 3D binary variable combination view */
typealias BinVariableView3 = VariableCombinationView3<UInt8, Binary>
/** 四维二值变量组合视图 / 4D binary variable combination view */
typealias BinVariableView4 = VariableCombinationView4<UInt8, Binary>
/** 动态维度二值变量组合视图 / Dynamic dimension binary variable combination view */
typealias DynBinVariableView = DynVariableCombinationView<UInt8, Binary>

/**
 * 一维二值物理量变量组合 / 1D binary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBinVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<UInt8, Binary>(Binary, name, UInt8, shape, unit)
/**
 * 二维二值物理量变量组合 / 2D binary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBinVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<UInt8, Binary>(Binary, name, UInt8, shape, unit)
/**
 * 三维二值物理量变量组合 / 3D binary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBinVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<UInt8, Binary>(Binary, name, UInt8, shape, unit)
/**
 * 四维二值物理量变量组合 / 4D binary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBinVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<UInt8, Binary>(Binary, name, UInt8, shape, unit)
/**
 * 动态维度二值物理量变量组合 / Dynamic dimension binary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityBinVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<UInt8, Binary>(Binary, name, UInt8, shape, unit)
/** 二值物理量变量组合视图 / Binary quantity variable combination view */
typealias QuantityBinVariableView = QuantityVariableCombinationView<UInt8, Binary>
/** 一维二值物理量变量组合视图 / 1D binary quantity variable combination view */
typealias QuantityBinVariableView1 = QuantityVariableCombinationView1<UInt8, Binary>
/** 二维二值物理量变量组合视图 / 2D binary quantity variable combination view */
typealias QuantityBinVariableView2 = QuantityVariableCombinationView2<UInt8, Binary>
/** 三维二值物理量变量组合视图 / 3D binary quantity variable combination view */
typealias QuantityBinVariableView3 = QuantityVariableCombinationView3<UInt8, Binary>
/** 四维二值物理量变量组合视图 / 4D binary quantity variable combination view */
typealias QuantityBinVariableView4 = QuantityVariableCombinationView4<UInt8, Binary>
/** 动态维度二值物理量变量组合视图 / Dynamic dimension binary quantity variable combination view */
typealias DynQuantityBinVariableView = DynQuantityVariableCombinationView<UInt8, Binary>

/**
 * 一维三值变量组合 / 1D ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class TerVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt8, Ternary>(Ternary, name, UInt8, shape)
/**
 * 二维三值变量组合 / 2D ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class TerVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt8, Ternary>(Ternary, name, UInt8, shape)
/**
 * 三维三值变量组合 / 3D ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class TerVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt8, Ternary>(Ternary, name, UInt8, shape)
/**
 * 四维三值变量组合 / 4D ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class TerVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt8, Ternary>(Ternary, name, UInt8, shape)
/**
 * 动态维度三值变量组合 / Dynamic dimension ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynTerVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt8, Ternary>(Ternary, name, UInt8, shape)
/** 三值变量组合视图 / Ternary variable combination view */
typealias TerVariableView = VariableCombinationView<UInt8, Ternary>
/** 一维三值变量组合视图 / 1D ternary variable combination view */
typealias TerVariableView1 = VariableCombinationView1<UInt8, Ternary>
/** 二维三值变量组合视图 / 2D ternary variable combination view */
typealias TerVariableView2 = VariableCombinationView2<UInt8, Ternary>
/** 三维三值变量组合视图 / 3D ternary variable combination view */
typealias TerVariableView3 = VariableCombinationView3<UInt8, Ternary>
/** 四维三值变量组合视图 / 4D ternary variable combination view */
typealias TerVariableView4 = VariableCombinationView4<UInt8, Ternary>
/** 动态维度三值变量组合视图 / Dynamic dimension ternary variable combination view */
typealias TerBinVariableView = DynVariableCombinationView<UInt8, Ternary>

/**
 * 一维三值物理量变量组合 / 1D ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityTerVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<UInt8, Ternary>(Ternary, name, UInt8, shape, unit)
/**
 * 二维三值物理量变量组合 / 2D ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityTerVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<UInt8, Ternary>(Ternary, name, UInt8, shape, unit)
/**
 * 三维三值物理量变量组合 / 3D ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityTerVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<UInt8, Ternary>(Ternary, name, UInt8, shape, unit)
/**
 * 四维三值物理量变量组合 / 4D ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityTerVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<UInt8, Ternary>(Ternary, name, UInt8, shape, unit)
/**
 * 动态维度三值物理量变量组合 / Dynamic dimension ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityTerVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<UInt8, Ternary>(Ternary, name, UInt8, shape, unit)
/** 三值物理量变量组合视图 / Ternary quantity variable combination view */
typealias QuantityTerVariableView = QuantityVariableCombinationView<UInt8, Ternary>
/** 一维三值物理量变量组合视图 / 1D ternary quantity variable combination view */
typealias QuantityTerVariableView1 = QuantityVariableCombinationView1<UInt8, Ternary>
/** 二维三值物理量变量组合视图 / 2D ternary quantity variable combination view */
typealias QuantityTerVariableView2 = QuantityVariableCombinationView2<UInt8, Ternary>
/** 三维三值物理量变量组合视图 / 3D ternary quantity variable combination view */
typealias QuantityTerVariableView3 = QuantityVariableCombinationView3<UInt8, Ternary>
/** 四维三值物理量变量组合视图 / 4D ternary quantity variable combination view */
typealias QuantityTerVariableView4 = QuantityVariableCombinationView4<UInt8, Ternary>
/** 动态维度三值物理量变量组合视图 / Dynamic dimension ternary quantity variable combination view */
typealias DynQuantityTerVariableView = DynQuantityVariableCombinationView<UInt8, Ternary>

/**
 * 一维平衡三值变量组合 / 1D balanced ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BTerVariable1(name: String = "", shape: Shape1) : VariableCombination1<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
/**
 * 二维平衡三值变量组合 / 2D balanced ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BTerVariable2(name: String = "", shape: Shape2) : VariableCombination2<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
/**
 * 三维平衡三值变量组合 / 3D balanced ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BTerVariable3(name: String = "", shape: Shape3) : VariableCombination3<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
/**
 * 四维平衡三值变量组合 / 4D balanced ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class BTerVariable4(name: String = "", shape: Shape4) : VariableCombination4<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
/**
 * 动态维度平衡三值变量组合 / Dynamic dimension balanced ternary variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynBTerVariable(name: String = "", shape: DynShape) : DynVariableCombination<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
/** 平衡三值变量组合视图 / Balanced ternary variable combination view */
typealias BTerVariableView = VariableCombinationView<Int8, BalancedTernary>
/** 一维平衡三值变量组合视图 / 1D balanced ternary variable combination view */
typealias BTerVariableView1 = VariableCombinationView1<Int8, BalancedTernary>
/** 二维平衡三值变量组合视图 / 2D balanced ternary variable combination view */
typealias BTerVariableView2 = VariableCombinationView2<Int8, BalancedTernary>
/** 三维平衡三值变量组合视图 / 3D balanced ternary variable combination view */
typealias BTerVariableView3 = VariableCombinationView3<Int8, BalancedTernary>
/** 四维平衡三值变量组合视图 / 4D balanced ternary variable combination view */
typealias BTerVariableView4 = VariableCombinationView4<Int8, BalancedTernary>
/** 动态维度平衡三值变量组合视图 / Dynamic dimension balanced ternary variable combination view */
typealias DynBTerVariableView = DynVariableCombinationView<Int8, BalancedTernary>

/**
 * 一维平衡三值物理量变量组合 / 1D balanced ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBTerVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape, unit)
/**
 * 二维平衡三值物理量变量组合 / 2D balanced ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBTerVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape, unit)
/**
 * 三维平衡三值物理量变量组合 / 3D balanced ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBTerVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape, unit)
/**
 * 四维平衡三值物理量变量组合 / 4D balanced ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityBTerVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape, unit)
/**
 * 动态维度平衡三值物理量变量组合 / Dynamic dimension balanced ternary quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityBTerVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) :
    DynQuantityVariableCombination<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape, unit)
/** 平衡三值物理量变量组合视图 / Balanced ternary quantity variable combination view */
typealias QuantityBTerVariableView = QuantityVariableCombinationView<Int8, BalancedTernary>
/** 一维平衡三值物理量变量组合视图 / 1D balanced ternary quantity variable combination view */
typealias QuantityBTerVariableView1 = QuantityVariableCombinationView1<Int8, BalancedTernary>
/** 二维平衡三值物理量变量组合视图 / 2D balanced ternary quantity variable combination view */
typealias QuantityBTerVariableView2 = QuantityVariableCombinationView2<Int8, BalancedTernary>
/** 三维平衡三值物理量变量组合视图 / 3D balanced ternary quantity variable combination view */
typealias QuantityBTerVariableView3 = QuantityVariableCombinationView3<Int8, BalancedTernary>
/** 四维平衡三值物理量变量组合视图 / 4D balanced ternary quantity variable combination view */
typealias QuantityBTerVariableView4 = QuantityVariableCombinationView4<Int8, BalancedTernary>
/** 动态维度平衡三值物理量变量组合视图 / Dynamic dimension balanced ternary quantity variable combination view */
typealias DynQuantityBTerVariableView = DynQuantityVariableCombinationView<Int8, BalancedTernary>

/**
 * 一维百分比变量组合 / 1D percentage variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class PctVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, Percentage>(Percentage, name, Flt64, shape)
/**
 * 二维百分比变量组合 / 2D percentage variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class PctVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, Percentage>(Percentage, name, Flt64, shape)
/**
 * 三维百分比变量组合 / 3D percentage variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class PctVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, Percentage>(Percentage, name, Flt64, shape)
/**
 * 四维百分比变量组合 / 4D percentage variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class PctVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, Percentage>(Percentage, name, Flt64, shape)
/**
 * 动态维度百分比变量组合 / Dynamic dimension percentage variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynPctVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, Percentage>(Percentage, name, Flt64, shape)

/**
 * 一维百分比物理量变量组合 / 1D percentage quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityPctVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<Flt64, Percentage>(Percentage, name, Flt64, shape, unit)
/**
 * 二维百分比物理量变量组合 / 2D percentage quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityPctVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<Flt64, Percentage>(Percentage, name, Flt64, shape, unit)
/**
 * 三维百分比物理量变量组合 / 3D percentage quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityPctVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<Flt64, Percentage>(Percentage, name, Flt64, shape, unit)
/**
 * 四维百分比物理量变量组合 / 4D percentage quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityPctVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<Flt64, Percentage>(Percentage, name, Flt64, shape, unit)
/**
 * 动态维度百分比物理量变量组合 / Dynamic dimension percentage quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityPctVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<Flt64, Percentage>(Percentage, name, Flt64, shape, unit)

/**
 * 一维整数变量组合 / 1D integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class IntVariable1(name: String = "", shape: Shape1) : VariableCombination1<Int64, Integer>(Integer, name, Int64, shape)
/**
 * 二维整数变量组合 / 2D integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class IntVariable2(name: String = "", shape: Shape2) : VariableCombination2<Int64, Integer>(Integer, name, Int64, shape)
/**
 * 三维整数变量组合 / 3D integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class IntVariable3(name: String = "", shape: Shape3) : VariableCombination3<Int64, Integer>(Integer, name, Int64, shape)
/**
 * 四维整数变量组合 / 4D integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class IntVariable4(name: String = "", shape: Shape4) : VariableCombination4<Int64, Integer>(Integer, name, Int64, shape)
/**
 * 动态维度整数变量组合 / Dynamic dimension integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynIntVariable(name: String = "", shape: DynShape) : DynVariableCombination<Int64, Integer>(Integer, name, Int64, shape)
/** 整数变量组合视图 / Integer variable combination view */
typealias IntVariableView = VariableCombinationView<Int64, Integer>
/** 一维整数变量组合视图 / 1D integer variable combination view */
typealias IntVariableView1 = VariableCombinationView1<Int64, Integer>
/** 二维整数变量组合视图 / 2D integer variable combination view */
typealias IntVariableView2 = VariableCombinationView2<Int64, Integer>
/** 三维整数变量组合视图 / 3D integer variable combination view */
typealias IntVariableView3 = VariableCombinationView3<Int64, Integer>
/** 四维整数变量组合视图 / 4D integer variable combination view */
typealias IntVariableView4 = VariableCombinationView4<Int64, Integer>
/** 动态维度整数变量组合视图 / Dynamic dimension integer variable combination view */
typealias DynIntVariableView = DynVariableCombinationView<Int64, Integer>

/**
 * 一维整数物理量变量组合 / 1D integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityIntVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<Int64, Integer>(Integer, name, Int64, shape, unit)
/**
 * 二维整数物理量变量组合 / 2D integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityIntVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<Int64, Integer>(Integer, name, Int64, shape, unit)
/**
 * 三维整数物理量变量组合 / 3D integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityIntVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<Int64, Integer>(Integer, name, Int64, shape, unit)
/**
 * 四维整数物理量变量组合 / 4D integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityIntVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<Int64, Integer>(Integer, name, Int64, shape, unit)
/**
 * 动态维度整数物理量变量组合 / Dynamic dimension integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityIntVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<Int64, Integer>(Integer, name, Int64, shape, unit)
/** 整数物理量变量组合视图 / Integer quantity variable combination view */
typealias QuantityIntVariableView = QuantityVariableCombinationView<Int64, Integer>
/** 一维整数物理量变量组合视图 / 1D integer quantity variable combination view */
typealias QuantityIntVariableView1 = QuantityVariableCombinationView1<Int64, Integer>
/** 二维整数物理量变量组合视图 / 2D integer quantity variable combination view */
typealias QuantityIntVariableView2 = QuantityVariableCombinationView2<Int64, Integer>
/** 三维整数物理量变量组合视图 / 3D integer quantity variable combination view */
typealias QuantityIntVariableView3 = QuantityVariableCombinationView3<Int64, Integer>
/** 四维整数物理量变量组合视图 / 4D integer quantity variable combination view */
typealias QuantityIntVariableView4 = QuantityVariableCombinationView4<Int64, Integer>
/** 动态维度整数物理量变量组合视图 / Dynamic dimension integer quantity variable combination view */
typealias DynQuantityIntVariableView = DynQuantityVariableCombinationView<Int64, Integer>

/**
 * 一维无符号整数变量组合 / 1D unsigned integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class UIntVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt64, UInteger>(UInteger, name, UInt64, shape)
/**
 * 二维无符号整数变量组合 / 2D unsigned integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class UIntVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt64, UInteger>(UInteger, name, UInt64, shape)
/**
 * 三维无符号整数变量组合 / 3D unsigned integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class UIntVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt64, UInteger>(UInteger, name, UInt64, shape)
/**
 * 四维无符号整数变量组合 / 4D unsigned integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class UIntVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt64, UInteger>(UInteger, name, UInt64, shape)
/**
 * 动态维度无符号整数变量组合 / Dynamic dimension unsigned integer variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynUIntVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt64, UInteger>(UInteger, name, UInt64, shape)
/** 无符号整数变量组合视图 / Unsigned integer variable combination view */
typealias UIntVariableView = VariableCombinationView<UInt64, UInteger>
/** 一维无符号整数变量组合视图 / 1D unsigned integer variable combination view */
typealias UIntVariableView1 = VariableCombinationView1<UInt64, UInteger>
/** 二维无符号整数变量组合视图 / 2D unsigned integer variable combination view */
typealias UIntVariableView2 = VariableCombinationView2<UInt64, UInteger>
/** 三维无符号整数变量组合视图 / 3D unsigned integer variable combination view */
typealias UIntVariableView3 = VariableCombinationView3<UInt64, UInteger>
/** 四维无符号整数变量组合视图 / 4D unsigned integer variable combination view */
typealias UIntVariableView4 = VariableCombinationView4<UInt64, UInteger>
/** 动态维度无符号整数变量组合视图 / Dynamic dimension unsigned integer variable combination view */
typealias DynUIntVariableView = DynVariableCombinationView<UInt64, UInteger>

/**
 * 一维无符号整数物理量变量组合 / 1D unsigned integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityUIntVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<UInt64, UInteger>(UInteger, name, UInt64, shape, unit)
/**
 * 二维无符号整数物理量变量组合 / 2D unsigned integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityUIntVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<UInt64, UInteger>(UInteger, name, UInt64, shape, unit)
/**
 * 三维无符号整数物理量变量组合 / 3D unsigned integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityUIntVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<UInt64, UInteger>(UInteger, name, UInt64, shape, unit)
/**
 * 四维无符号整数物理量变量组合 / 4D unsigned integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityUIntVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<UInt64, UInteger>(UInteger, name, UInt64, shape, unit)
/**
 * 动态维度无符号整数物理量变量组合 / Dynamic dimension unsigned integer quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityUIntVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<UInt64, UInteger>(UInteger, name, UInt64, shape, unit)
/** 无符号整数物理量变量组合视图 / Unsigned integer quantity variable combination view */
typealias QuantityUIntVariableView = QuantityVariableCombinationView<UInt64, UInteger>
/** 一维无符号整数物理量变量组合视图 / 1D unsigned integer quantity variable combination view */
typealias QuantityUIntVariableView1 = QuantityVariableCombinationView1<UInt64, UInteger>
/** 二维无符号整数物理量变量组合视图 / 2D unsigned integer quantity variable combination view */
typealias QuantityUIntVariableView2 = QuantityVariableCombinationView2<UInt64, UInteger>
/** 三维无符号整数物理量变量组合视图 / 3D unsigned integer quantity variable combination view */
typealias QuantityUIntVariableView3 = QuantityVariableCombinationView3<UInt64, UInteger>
/** 四维无符号整数物理量变量组合视图 / 4D unsigned integer quantity variable combination view */
typealias QuantityUIntVariableView4 = QuantityVariableCombinationView4<UInt64, UInteger>
/** 动态维度无符号整数物理量变量组合视图 / Dynamic dimension unsigned integer quantity variable combination view */
typealias DynQuantityUIntVariableView = DynQuantityVariableCombinationView<UInt64, UInteger>

/**
 * 一维实数变量组合 / 1D real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class RealVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, Continuous>(Continuous, name, Flt64, shape)
/**
 * 二维实数变量组合 / 2D real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class RealVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, Continuous>(Continuous, name, Flt64, shape)
/**
 * 三维实数变量组合 / 3D real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class RealVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, Continuous>(Continuous, name, Flt64, shape)
/**
 * 四维实数变量组合 / 4D real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class RealVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, Continuous>(Continuous, name, Flt64, shape)
/**
 * 动态维度实数变量组合 / Dynamic dimension real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynRealVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, Continuous>(Continuous, name, Flt64, shape)

/**
 * 一维实数物理量变量组合 / 1D real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityRealVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<Flt64, Continuous>(Continuous, name, Flt64, shape, unit)
/**
 * 二维实数物理量变量组合 / 2D real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityRealVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<Flt64, Continuous>(Continuous, name, Flt64, shape, unit)
/**
 * 三维实数物理量变量组合 / 3D real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityRealVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<Flt64, Continuous>(Continuous, name, Flt64, shape, unit)
/**
 * 四维实数物理量变量组合 / 4D real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityRealVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<Flt64, Continuous>(Continuous, name, Flt64, shape, unit)
/**
 * 动态维度实数物理量变量组合 / Dynamic dimension real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityRealVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<Flt64, Continuous>(Continuous, name, Flt64, shape, unit)

/**
 * 一维非负实数变量组合 / 1D non-negative real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class URealVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
/**
 * 二维非负实数变量组合 / 2D non-negative real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class URealVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
/**
 * 三维非负实数变量组合 / 3D non-negative real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class URealVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
/**
 * 四维非负实数变量组合 / 4D non-negative real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class URealVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
/**
 * 动态维度非负实数变量组合 / Dynamic dimension non-negative real variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 */
class DynURealVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, UContinuous>(UContinuous, name, Flt64, shape)

/**
 * 一维非负实数物理量变量组合 / 1D non-negative real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityURealVariable1(name: String = "", shape: Shape1, unit: PhysicalUnit) : QuantityVariableCombination1<Flt64, UContinuous>(UContinuous, name, Flt64, shape, unit)
/**
 * 二维非负实数物理量变量组合 / 2D non-negative real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityURealVariable2(name: String = "", shape: Shape2, unit: PhysicalUnit) : QuantityVariableCombination2<Flt64, UContinuous>(UContinuous, name, Flt64, shape, unit)
/**
 * 三维非负实数物理量变量组合 / 3D non-negative real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityURealVariable3(name: String = "", shape: Shape3, unit: PhysicalUnit) : QuantityVariableCombination3<Flt64, UContinuous>(UContinuous, name, Flt64, shape, unit)
/**
 * 四维非负实数物理量变量组合 / 4D non-negative real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class QuantityURealVariable4(name: String = "", shape: Shape4, unit: PhysicalUnit) : QuantityVariableCombination4<Flt64, UContinuous>(UContinuous, name, Flt64, shape, unit)
/**
 * 动态维度非负实数物理量变量组合 / Dynamic dimension non-negative real quantity variable combination
 *
 * @param name 组合名称 / Combination name
 * @param shape 形状 / Shape
 * @param unit 物理单位 / Physical unit
 */
class DynQuantityURealVariable(name: String = "", shape: DynShape, unit: PhysicalUnit) : DynQuantityVariableCombination<Flt64, UContinuous>(UContinuous, name, Flt64, shape, unit)
