package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory

sealed interface Object

class SingleObject<out Obj : SubObject<*>>(
    val category: ObjectCategory,
    subObjects: List<@UnsafeVariance Obj>
) : Object {
    @Suppress("UNCHECKED_CAST")
    internal val _subObjects: MutableList<SubObject<*>> = (subObjects as List<SubObject<*>>).toMutableList()
    @Suppress("UNCHECKED_CAST")
    val subObjects: List<Obj> get() = _subObjects as List<Obj>
}

class MultiObject {}
