package fuookami.ospf.kotlin.core.frontend.variable

import fuookami.ospf.kotlin.utils.math.*

sealed class IndependentVariableItem<T, Type : VariableType<T>>(
    type: Type,
    name: String,
    constants: RealNumberConstants<T>
) : AbstractVariableItem<T, Type>(type, name, constants) where T : RealNumber<T>, T : NumberField<T> {
    override val dimension = 0
    override val identifier = IdentifierGenerator.gen()
    override val index = 0
    override val vectorView = intArrayOf(0)

    override fun belongsTo(combination: VariableCombination<*, *, *>): Boolean {
        return false
    }
}

typealias Variable<Type> = IndependentVariableItem<*, Type>

class BinVar(name: String = "") : IndependentVariableItem<UInt8, Binary>(Binary, name, UInt8)
class TerVar(name: String = "") : IndependentVariableItem<UInt8, Ternary>(Ternary, name, UInt8)
class BTerVar(name: String = "") : IndependentVariableItem<Int8, BalancedTernary>(BalancedTernary, name, Int8)
class PctVar(name: String = "") : IndependentVariableItem<Flt64, Percentage>(Percentage, name, Flt64)
class IntVar(name: String = "") : IndependentVariableItem<Int64, Integer>(Integer, name, Int64)
class UIntVar(name: String = "") : IndependentVariableItem<UInt64, UInteger>(UInteger, name, UInt64)
class RealVar(name: String = "") : IndependentVariableItem<Flt64, Continuous>(Continuous, name, Flt64)
class URealVar(name: String = "") : IndependentVariableItem<Flt64, UContinuous>(UContinuous, name, Flt64)
