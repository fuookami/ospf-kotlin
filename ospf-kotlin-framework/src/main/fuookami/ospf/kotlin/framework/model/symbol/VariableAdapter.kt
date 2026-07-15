package fuookami.ospf.kotlin.framework.model.symbol

import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.VariableType

/**
 * 变量适配器，用于建立内部变量与外部变量之间的映射关系。
 * Variable adapter that establishes a mapping relationship between an inner variable and an outer variable.
 * @property inner 内部变量（模型求解器视角） / The inner variable (from the solver's perspective)
 * @property outer 外部变量（用户视角） / The outer variable (from the user's perspective)
*/
data class VariableAdapter<Type : VariableType<*>>(
    val inner: AbstractVariableItem<*, Type>,
    val outer: AbstractVariableItem<*, Type>
) {

    /**
     * 内部变量，表示适配后的变量包装。
     * Internal variable representing the wrapped variable after adaptation.
     * @property variable 被适配的变量 / The adapted variable
     * @property adapter 对应的变量适配器 / The corresponding variable adapter
    */
    data class InternalVariable<Type : VariableType<*>>(
        val variable: AbstractVariableItem<*, Type>,
        val adapter: VariableAdapter<Type>
    )

    /**
     * 适配器变量，表示适配结果的另一种变量包装。
     * Adapter variable representing an alternative variable wrapping of the adaptation result.
     * @property variable 适配后的变量 / The adapted variable
     * @property adapter 对应的变量适配器 / The corresponding variable adapter
    */
    data class AdapterVariable<Type : VariableType<*>>(
        val variable: AbstractVariableItem<*, Type>,
        val adapter: VariableAdapter<Type>
    )

    /**
     * 将当前变量适配为内部变量，使用给定的适配器。
     * Adopt the current variable as an internal variable using the given adapter.
     * @param adapter 用于适配的变量适配器 / The variable adapter to use
     * @return 适配后的内部变量 / The adopted internal variable
    */
    fun innerVariable(): InternalVariable<Type> {
        return InternalVariable(inner, this)
    }

    /**
     * 将外部变量包装为适配器变量。
     * Wraps the outer variable as an adapter variable.
     *
     * @return 适配后的外部变量 / The adopted outer variable
    */
    fun outerVariable(): AdapterVariable<Type> {
        return AdapterVariable(outer, this)
    }
}

/**
 * 从适配器列表中查找当前变量对应的适配器。
 * Find the adapter corresponding to the current variable from the given adapters.
 * @param adapters 待搜索的适配器列表 / The adapters to search through
 * @return 如果找到则返回对应的变量适配器，否则返回 null / The matching variable adapter, or null if not found
*/
fun <Type : VariableType<*>> AbstractVariableItem<*, Type>.adapter(
    vararg adapters: VariableAdapter<Type>
): VariableAdapter<Type>? {
    for (adapter in adapters) {
        if (adapter.inner === this || adapter.outer === this) {
            return adapter
        }
    }
    return null
}
