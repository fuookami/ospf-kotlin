package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.multi_array.*

class CombinationItem<T, Type : VariableType<T>>(
    private val parent: Combination<T, Type, *>,
    type: Type,
    name: String = "",
    override val index: Int,
    constants: RealNumberConstants<T>
) : Item<T, Type>(type, name, constants) where T : RealNumber<T>, T : NumberField<T> {
    override val dimension: Int = parent.dimension
    override val identifier: UInt64 = parent.identifier
    override val vectorView: IntArray = parent.shape.vector(index)
}

sealed class Combination<T, Type : VariableType<T>, S : Shape>(
    val type: Type,
    val name: String = "",
    private val constants: RealNumberConstants<T>,
    shape: S
) : MultiArray<CombinationItem<T, Type>, S>(shape) where T : RealNumber<T>, T : NumberField<T> {
    val items = this
    val dimension: Int = shape.dimension
    val identifier: UInt64 = IdentifierGenerator.gen()

    init {
        for (i in 0 until shape.size) {
            items[i] = CombinationItem(this, type, "${name}_$i", i, constants)
        }
    }
}

typealias Combination1<T, Type> = Combination<T, Type, Shape1>
typealias Combination2<T, Type> = Combination<T, Type, Shape2>
typealias Combination3<T, Type> = Combination<T, Type, Shape3>
typealias Combination4<T, Type> = Combination<T, Type, Shape4>
typealias DynCombination<T, Type> = Combination<T, Type, DynShape>
typealias CombinationView<T, Type> = MultiArrayView<CombinationItem<T, Type>, *>
typealias CombinationView1<T, Type> = MultiArrayView<CombinationItem<T, Type>, Shape1>
typealias CombinationView2<T, Type> = MultiArrayView<CombinationItem<T, Type>, Shape2>
typealias CombinationView3<T, Type> = MultiArrayView<CombinationItem<T, Type>, Shape3>
typealias CombinationView4<T, Type> = MultiArrayView<CombinationItem<T, Type>, Shape4>
typealias DynCombinationView<T, Type> = MultiArrayView<CombinationItem<T, Type>, DynShape>

class BinVariable1(name: String = "", shape: Shape1) : Combination1<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable2(name: String = "", shape: Shape2) : Combination2<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable3(name: String = "", shape: Shape3) : Combination3<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable4(name: String = "", shape: Shape4) : Combination4<UInt8, Binary>(Binary, name, UInt8, shape)
class BinVariable(name: String = "", shape: DynShape) : DynCombination<UInt8, Binary>(Binary, name, UInt8, shape)
typealias BinVariableView = CombinationView<UInt8, Binary>
typealias BinVariableView1 = CombinationView1<UInt8, Binary>
typealias BinVariableView2 = CombinationView2<UInt8, Binary>
typealias BinVariableView3 = CombinationView3<UInt8, Binary>
typealias BinVariableView4 = CombinationView4<UInt8, Binary>
typealias DynBinVariableView = DynCombinationView<UInt8, Binary>

class TerVariable1(name: String = "", shape: Shape1) : Combination1<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable2(name: String = "", shape: Shape2) : Combination2<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable3(name: String = "", shape: Shape3) : Combination3<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable4(name: String = "", shape: Shape4) : Combination4<UInt8, Ternary>(Ternary, name, UInt8, shape)
class TerVariable(name: String = "", shape: DynShape) : DynCombination<UInt8, Ternary>(Ternary, name, UInt8, shape)
typealias TerVariableView = CombinationView<UInt8, Ternary>
typealias TerVariableView1 = CombinationView1<UInt8, Ternary>
typealias TerVariableView2 = CombinationView2<UInt8, Ternary>
typealias TerVariableView3 = CombinationView3<UInt8, Ternary>
typealias TerVariableView4 = CombinationView4<UInt8, Ternary>
typealias TerBinVariableView = DynCombinationView<UInt8, Ternary>

class BTerVariable1(name: String = "", shape: Shape1) : Combination1<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable2(name: String = "", shape: Shape2) : Combination2<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable3(name: String = "", shape: Shape3) : Combination3<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable4(name: String = "", shape: Shape4) : Combination4<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
class BTerVariable(name: String = "", shape: DynShape) : DynCombination<Int8, BalancedTernary>(BalancedTernary, name, Int8, shape)
typealias BTerVariableView = CombinationView<Int8, BalancedTernary>
typealias BTerVariableView1 = CombinationView1<Int8, BalancedTernary>
typealias BTerVariableView2 = CombinationView2<Int8, BalancedTernary>
typealias BTerVariableView3 = CombinationView3<Int8, BalancedTernary>
typealias BTerVariableView4 = CombinationView4<Int8, BalancedTernary>
typealias DynBTerVariableView = DynCombinationView<Int8, BalancedTernary>

class PctVariable1(name: String = "", shape: Shape1) : Combination1<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable2(name: String = "", shape: Shape2) : Combination2<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable3(name: String = "", shape: Shape3) : Combination3<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable4(name: String = "", shape: Shape4) : Combination4<Flt64, Percentage>(Percentage, name, Flt64, shape)
class PctVariable(name: String = "", shape: DynShape) : DynCombination<Flt64, Percentage>(Percentage, name, Flt64, shape)
typealias PctVariableView = CombinationView<Flt64, Percentage>
typealias PctVariableView1 = CombinationView1<Flt64, Percentage>
typealias PctVariableView2 = CombinationView2<Flt64, Percentage>
typealias PctVariableView3 = CombinationView3<Flt64, Percentage>
typealias PctVariableView4 = CombinationView4<Flt64, Percentage>
typealias DynPctVariableView = DynCombinationView<Flt64, Percentage>

class IntVariable1(name: String = "", shape: Shape1) : Combination1<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable2(name: String = "", shape: Shape2) : Combination2<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable3(name: String = "", shape: Shape3) : Combination3<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable4(name: String = "", shape: Shape4) : Combination4<Int64, Integer>(Integer, name, Int64, shape)
class IntVariable(name: String = "", shape: DynShape) : DynCombination<Int64, Integer>(Integer, name, Int64, shape)
typealias IntVariableView = CombinationView<Int64, Integer>
typealias IntVariableView1 = CombinationView1<Int64, Integer>
typealias IntVariableView2 = CombinationView2<Int64, Integer>
typealias IntVariableView3 = CombinationView3<Int64, Integer>
typealias IntVariableView4 = CombinationView4<Int64, Integer>
typealias DynIntVariableView = DynCombinationView<Int64, Integer>

class UIntVariable1(name: String = "", shape: Shape1) : Combination1<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable2(name: String = "", shape: Shape2) : Combination2<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable3(name: String = "", shape: Shape3) : Combination3<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable4(name: String = "", shape: Shape4) : Combination4<UInt64, UInteger>(UInteger, name, UInt64, shape)
class UIntVariable(name: String = "", shape: DynShape) : DynCombination<UInt64, UInteger>(UInteger, name, UInt64, shape)
typealias UIntVariableView = CombinationView<UInt64, UInteger>
typealias UIntVariableView1 = CombinationView1<UInt64, UInteger>
typealias UIntVariableView2 = CombinationView2<UInt64, UInteger>
typealias UIntVariableView3 = CombinationView3<UInt64, UInteger>
typealias UIntVariableView4 = CombinationView4<UInt64, UInteger>
typealias DynUIntVariableView = DynCombinationView<UInt64, UInteger>

class RealVariable1(name: String = "", shape: Shape1) : Combination1<Flt64, Continues>(Continues, name, Flt64, shape)
class RealVariable2(name: String = "", shape: Shape2) : Combination2<Flt64, Continues>(Continues, name, Flt64, shape)
class RealVariable3(name: String = "", shape: Shape3) : Combination3<Flt64, Continues>(Continues, name, Flt64, shape)
class RealVariable4(name: String = "", shape: Shape4) : Combination4<Flt64, Continues>(Continues, name, Flt64, shape)
class RealVariable(name: String = "", shape: DynShape) : DynCombination<Flt64, Continues>(Continues, name, Flt64, shape)
typealias RealVariableView = CombinationView<Flt64, Continues>
typealias RealVariableView1 = CombinationView1<Flt64, Continues>
typealias RealVariableView2 = CombinationView2<Flt64, Continues>
typealias RealVariableView3 = CombinationView3<Flt64, Continues>
typealias RealVariableView4 = CombinationView4<Flt64, Continues>
typealias DynRealVariableView = DynCombinationView<Flt64, Continues>

class URealVariable1(name: String = "", shape: Shape1) : Combination1<Flt64, UContinues>(UContinues, name, Flt64, shape)
class URealVariable2(name: String = "", shape: Shape2) : Combination2<Flt64, UContinues>(UContinues, name, Flt64, shape)
class URealVariable3(name: String = "", shape: Shape3) : Combination3<Flt64, UContinues>(UContinues, name, Flt64, shape)
class URealVariable4(name: String = "", shape: Shape4) : Combination4<Flt64, UContinues>(UContinues, name, Flt64, shape)
class URealVariable(name: String = "", shape: DynShape) : DynCombination<Flt64, UContinues>(UContinues, name, Flt64, shape)
typealias URealVariableView = CombinationView<Flt64, UContinues>
typealias URealVariableView1 = CombinationView1<Flt64, UContinues>
typealias URealVariableView2 = CombinationView2<Flt64, UContinues>
typealias URealVariableView3 = CombinationView3<Flt64, UContinues>
typealias URealVariableView4 = CombinationView4<Flt64, UContinues>
typealias DynURealVariableView = DynCombinationView<Flt64, UContinues>
