package fuookami.ospf.kotlin.framework.model.symbol

import fuookami.ospf.kotlin.core.model.variable.*

/**
 * 变量适配器，用于建立内部变量与外部变量之间的映射关系。
 * Variable adapter that establishes a mapping relationship between an inner variable and an outer variable.
 * @property inner 内部变量（模型求解器视角） / The inner variable (from the solver's perspective)
 * @property outer 外部变量（用户视角） / The outer variable (from the user's perspective)
 */
data class VariableAdapter<K>(
    val inner: Variable<K>,
    val outer: Variable<K>,
) {
    /**
     * 内部变量，表示适配后的变量包装。
     * Internal variable representing the wrapped variable after adaptation.
     * @property variable 被适配的变量 / The adapted variable
     * @property adapter 对应的变量适配器 / The corresponding variable adapter
     */
    internal class InternalVariable<K>(
        override val variable: Variable<K>,
        override val adapter: VariableAdapter<K>,
    ) : Symbol.Adapter<K, Variable<K>, VariableAdapter<K>> {
        override fun value(): Variable<K> = variable
    }

    /**
     * 适配器变量，表示适配结果的另一种变量包装。
     * Adapter variable representing an alternative variable wrapping of the adaptation result.
     * @property variable 适配后的变量 / The adapted variable
     * @property adapter 对应的变量适配器 / The corresponding variable adapter
     */
    internal class AdapterVariable<K>(
        override val variable: Variable<K>,
        override val adapter: VariableAdapter<K>,
    ) : Symbol.Adapter<K, Variable<K>, VariableAdapter<K>> {
        override fun value(): Variable<K> = variable
    }

    /**
     * 将当前变量适配为内部变量，使用给定的适配器。
     * Adopt the current variable as an internal variable using the given adapter.
     * @param adapter 用于适配的变量适配器 / The variable adapter to use
     * @return 适配后的内部变量 / The adopted internal variable
     */
    private fun <K> Variable<K>.adopt(adapter: VariableAdapter<K>): VariableAdapter.InternalVariable<K> {
        return InternalVariable(this, adapter)
    }
}

/**
 * 从符号列表中查找当前变量对应的适配器。
 * Find the adapter corresponding to the current variable from the given symbols.
 * @param symbols 待搜索的符号列表 / The symbols to search through
 * @return 如果找到则返回对应的变量适配器，否则返回 null / The matching variable adapter, or null if not found
 */
fun <K> Variable<K>.adapter(vararg symbols: Symbol): VariableAdapter<K>? {
    for (symbol in symbols) {
        when (symbol) {
            is Symbol.Adapter<*, *, *> -> {
                @Suppress("UNCHECKED_CAST")
                val adapter = symbol.adapter as? VariableAdapter<K> ?: continue
                if (adapter.inner === this) {
                    return adapter
                }
            }
        }
    }
    return null
}
