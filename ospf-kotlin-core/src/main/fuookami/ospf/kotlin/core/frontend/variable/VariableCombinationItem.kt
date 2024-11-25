package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.multi_array.*

class CombinationVariableItem<T, Type : VariableType<T>>(
    private val parent: VariableCombination<T, Type, *>,
    type: Type,
    name: String,
    override val index: Int,
    constants: RealNumberConstants<T>
) : AbstractVariableItem<T, Type>(type, name, constants) where T : RealNumber<T>, T : NumberField<T> {
    override val dimension = parent.dimension
    override val identifier by parent::identifier
    override val vectorView by lazy { parent.shape.vector(index) }
}

sealed class VariableCombination<T, Type : VariableType<T>, S : Shape>(
    val type: Type,
    val name: String,
    private val constants: RealNumberConstants<T>,
    shape: S
) : MultiArray<CombinationVariableItem<T, Type>, S>(shape) where T : RealNumber<T>, T : NumberField<T> {
    val items get() = this
    val identifier = IdentifierGenerator.gen()

    init {
        super.init { i, v ->
            CombinationVariableItem(this, type, "${name}_${v.joinToString("_") { "$it" }}", i, constants)
        }
    }
}

typealias VariableCombination1<T, Type> = VariableCombination<T, Type, Shape1>
typealias VariableCombination2<T, Type> = VariableCombination<T, Type, Shape2>
typealias VariableCombination3<T, Type> = VariableCombination<T, Type, Shape3>
typealias VariableCombination4<T, Type> = VariableCombination<T, Type, Shape4>
typealias DynVariableCombination<T, Type> = VariableCombination<T, Type, DynShape>
typealias VariableCombinationView<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, *>
typealias VariableCombinationView1<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape1>
typealias VariableCombinationView2<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape2>
typealias VariableCombinationView3<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape3>
typealias VariableCombinationView4<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, Shape4>
typealias DynVariableCombinationView<T, Type> = MultiArrayView<CombinationVariableItem<T, Type>, DynShape>

typealias Variable1<Type> = VariableCombination<*, Type, Shape1>
typealias Variable2<Type> = VariableCombination<*, Type, Shape2>
typealias Variable3<Type> = VariableCombination<*, Type, Shape3>
typealias Variable4<Type> = VariableCombination<*, Type, Shape4>
typealias DynVariable<Type> = VariableCombination<*, Type, DynShape>
typealias VariableView<Type> = VariableCombinationView1<*, Type>
typealias VariableView1<Type> = VariableCombinationView1<*, Type>
typealias VariableView2<Type> = VariableCombinationView2<*, Type>
typealias VariableView3<Type> = VariableCombinationView3<*, Type>
typealias VariableView4<Type> = VariableCombinationView4<*, Type>
typealias DynVariableView<Type> = DynVariableCombinationView<*, Type>

class BinVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt8, Binary>(Binary, name, UInt8, shape)
class DynBinVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt8, Binary>(Binary, name, UInt8, shape)
typealias BinVariableView = VariableCombinationView<UInt8, Binary>
typealias BinVariableView1 = VariableCombinationView1<UInt8, Binary>
typealias BinVariableView2 = VariableCombinationView2<UInt8, Binary>
typealias BinVariableView3 = VariableCombinationView3<UInt8, Binary>
typealias BinVariableView4 = VariableCombinationView4<UInt8, Binary>
typealias DynBinVariableView = DynVariableCombinationView<UInt8, Binary>

class TerVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt8, Ternary>(Ternary, name, UInt8, shape)
class DynTerVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt8, Ternary>(Ternary, name, UInt8, shape)
typealias TerVariableView = VariableCombinationView<UInt8, Ternary>
typealias TerVariableView1 = VariableCombinationView1<UInt8, Ternary>
typealias TerVariableView2 = VariableCombinationView2<UInt8, Ternary>
typealias TerVariableView3 = VariableCombinationView3<UInt8, Ternary>
typealias TerVariableView4 = VariableCombinationView4<UInt8, Ternary>
typealias TerBinVariableView = DynVariableCombinationView<UInt8, Ternary>

class BTerVariable1(name: String = "", shape: Shape1) : VariableCombination1<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable2(name: String = "", shape: Shape2) : VariableCombination2<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable3(name: String = "", shape: Shape3) : VariableCombination3<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable4(name: String = "", shape: Shape4) : VariableCombination4<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class DynBTerVariable(name: String = "", shape: DynShape) : DynVariableCombination<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
typealias BTerVariableView = VariableCombinationView<Int8, BalancedTernary>
typealias BTerVariableView1 = VariableCombinationView1<Int8, BalancedTernary>
typealias BTerVariableView2 = VariableCombinationView2<Int8, BalancedTernary>
typealias BTerVariableView3 = VariableCombinationView3<Int8, BalancedTernary>
typealias BTerVariableView4 = VariableCombinationView4<Int8, BalancedTernary>
typealias DynBTerVariableView = DynVariableCombinationView<Int8, BalancedTernary>

class PctVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, Percentage>(Percentage, name, Flt64, shape)
class DynPctVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, Percentage>(Percentage, name, Flt64, shape)
typealias PctVariableView = VariableCombinationView<Flt64, Percentage>
typealias PctVariableView1 = VariableCombinationView1<Flt64, Percentage>
typealias PctVariableView2 = VariableCombinationView2<Flt64, Percentage>
typealias PctVariableView3 = VariableCombinationView3<Flt64, Percentage>
typealias PctVariableView4 = VariableCombinationView4<Flt64, Percentage>
typealias DynPctVariableView = DynVariableCombinationView<Flt64, Percentage>

class IntVariable1(name: String = "", shape: Shape1) : VariableCombination1<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable2(name: String = "", shape: Shape2) : VariableCombination2<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable3(name: String = "", shape: Shape3) : VariableCombination3<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable4(name: String = "", shape: Shape4) : VariableCombination4<Int64, Integer>(Integer, name, Int64, shape)
class DynIntVariable(name: String = "", shape: DynShape) : DynVariableCombination<Int64, Integer>(Integer, name, Int64, shape)
typealias IntVariableView = VariableCombinationView<Int64, Integer>
typealias IntVariableView1 = VariableCombinationView1<Int64, Integer>
typealias IntVariableView2 = VariableCombinationView2<Int64, Integer>
typealias IntVariableView3 = VariableCombinationView3<Int64, Integer>
typealias IntVariableView4 = VariableCombinationView4<Int64, Integer>
typealias DynIntVariableView = DynVariableCombinationView<Int64, Integer>

class UIntVariable1(name: String = "", shape: Shape1) : VariableCombination1<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable2(name: String = "", shape: Shape2) : VariableCombination2<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable3(name: String = "", shape: Shape3) : VariableCombination3<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable4(name: String = "", shape: Shape4) : VariableCombination4<UInt64, UInteger>(UInteger, name, UInt64, shape)
class DynUIntVariable(name: String = "", shape: DynShape) : DynVariableCombination<UInt64, UInteger>(UInteger, name, UInt64, shape)
typealias UIntVariableView = VariableCombinationView<UInt64, UInteger>
typealias UIntVariableView1 = VariableCombinationView1<UInt64, UInteger>
typealias UIntVariableView2 = VariableCombinationView2<UInt64, UInteger>
typealias UIntVariableView3 = VariableCombinationView3<UInt64, UInteger>
typealias UIntVariableView4 = VariableCombinationView4<UInt64, UInteger>
typealias DynUIntVariableView = DynVariableCombinationView<UInt64, UInteger>

class RealVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, Continuous>(Continuous, name, Flt64, shape)
class RealVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, Continuous>(Continuous, name, Flt64, shape)
class RealVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, Continuous>(Continuous, name, Flt64, shape)
class RealVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, Continuous>(Continuous, name, Flt64, shape)
class DynRealVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, Continuous>(Continuous, name, Flt64, shape)
typealias RealVariableView = VariableCombinationView<Flt64, Continuous>
typealias RealVariableView1 = VariableCombinationView1<Flt64, Continuous>
typealias RealVariableView2 = VariableCombinationView2<Flt64, Continuous>
typealias RealVariableView3 = VariableCombinationView3<Flt64, Continuous>
typealias RealVariableView4 = VariableCombinationView4<Flt64, Continuous>
typealias DynRealVariableView = DynVariableCombinationView<Flt64, Continuous>

class URealVariable1(name: String = "", shape: Shape1) : VariableCombination1<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
class URealVariable2(name: String = "", shape: Shape2) : VariableCombination2<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
class URealVariable3(name: String = "", shape: Shape3) : VariableCombination3<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
class URealVariable4(name: String = "", shape: Shape4) : VariableCombination4<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
class DynURealVariable(name: String = "", shape: DynShape) : DynVariableCombination<Flt64, UContinuous>(UContinuous, name, Flt64, shape)
typealias URealVariableView = VariableCombinationView<Flt64, UContinuous>
typealias URealVariableView1 = VariableCombinationView1<Flt64, UContinuous>
typealias URealVariableView2 = VariableCombinationView2<Flt64, UContinuous>
typealias URealVariableView3 = VariableCombinationView3<Flt64, UContinuous>
typealias URealVariableView4 = VariableCombinationView4<Flt64, UContinuous>
typealias DynURealVariableView = DynVariableCombinationView<Flt64, UContinuous>
