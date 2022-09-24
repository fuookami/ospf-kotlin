package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

sealed class IndependentItem<T, Type : VariableType<T>>(
    type: Type,
    name: String = "",
    constants: RealNumberConstants<T>
) : Item<T, Type>(type, name, constants) where T : RealNumber<T>, T : NumberField<T> {
    override val dimension: Int = 0
    override val identifier: UInt64 = IdentifierGenerator.gen()
    override val index: Int = 0
    override val vectorView: IntArray = intArrayOf(0)
}

class BinVar(name: String = "") : IndependentItem<UInt8, Binary>(Binary, name, UInt8)
class TerVar(name: String = "") : IndependentItem<UInt8, Ternary>(Ternary, name, UInt8)
class BTerVar(name: String = "") : IndependentItem<Int8, BalancedTernary>(BalancedTernary, name, Int8)
class PctVar(name: String = "") : IndependentItem<Flt64, Percentage>(Percentage, name, Flt64)
class IntVar(name: String = "") : IndependentItem<Int64, Integer>(Integer, name, Int64)
class UIntVar(name: String = "") : IndependentItem<UInt64, UInteger>(UInteger, name, UInt64)
class RealVar(name: String = "") : IndependentItem<Flt64, Continues>(Continues, name, Flt64)
class URealVar(name: String = "") : IndependentItem<Flt64, UContinues>(UContinues, name, Flt64)
