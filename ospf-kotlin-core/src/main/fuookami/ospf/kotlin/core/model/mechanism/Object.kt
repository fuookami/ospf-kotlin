package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber

typealias AnySubObject = SubObject<out RealNumber<*>>

sealed interface Object

class SingleObject<out Obj : AnySubObject>(
    val category: ObjectCategory,
    subObjects: List<@UnsafeVariance Obj>
) : Object {
    @Suppress("UNCHECKED_CAST")
    internal val _subObjects: MutableList<AnySubObject> = (subObjects as List<AnySubObject>).toMutableList()
    @Suppress("UNCHECKED_CAST")
    val subObjects: List<Obj> get() = _subObjects as List<Obj>
}

class MultiObject {}
