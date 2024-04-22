package fuookami.ospf.kotlin.core.frontend.model.mechanism

sealed interface Object

class SingleObject(
    val category: ObjectCategory,
    subObjects: List<SubObject>
) : Object {
    internal val _subObjects: MutableList<SubObject> = subObjects.toMutableList()
    val subObjects: List<SubObject> by ::_subObjects
}

class MultiObject {}
