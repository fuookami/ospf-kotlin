package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.core.model.basic.ObjectCategory

sealed interface Object

class SingleObject<Obj : SubObject<*>>(
    val category: ObjectCategory,
    subObjects: List<Obj>
) : Object {
    internal val _subObjects: MutableList<Obj> = subObjects.toMutableList()
    val subObjects: List<Obj> by ::_subObjects
}

class MultiObject {}
