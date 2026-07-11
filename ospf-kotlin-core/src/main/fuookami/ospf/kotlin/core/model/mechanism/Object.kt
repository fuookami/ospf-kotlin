/**
 * 目标对象
 * Objective object
*/
package fuookami.ospf.kotlin.core.model.mechanism

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory

/** 任意子目标类型别名 / Any sub-object type alias */
typealias AnySubObject = SubObject<out RealNumber<*>>

/** 目标对象密封接口 / Objective object sealed interface */
sealed interface Object

/**
 * 单目标对象，包含一个目标分类下的子目标列表。
 * Single objective object containing a list of sub-objectives under one category.
 *
 * @param Obj 子目标类型 / The sub-objective type
 * @property category   目标分类 / The objective category
 * @property subObjects 子目标列表 / List of sub-objectives
*/
class SingleObject<out Obj : AnySubObject>(
    val category: ObjectCategory,
    subObjects: List<@UnsafeVariance Obj>
) : Object {
    @Suppress("UNCHECKED_CAST")
    internal val _subObjects: MutableList<AnySubObject> = (subObjects as List<AnySubObject>).toMutableList()
    @Suppress("UNCHECKED_CAST")
    val subObjects: List<Obj> get() = _subObjects as List<Obj>
}

/** 多目标对象（预留） / Multi-objective object (placeholder) */
class MultiObject {}
