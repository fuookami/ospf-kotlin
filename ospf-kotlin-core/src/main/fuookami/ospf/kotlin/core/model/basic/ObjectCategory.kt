/**
 * 目标函数分类
 * Objective function category
 */
package fuookami.ospf.kotlin.core.model.basic

/**
 * 目标函数分类枚举，表示优化方向（最大化或最小化）。
 * Objective function category enumeration indicating optimization direction (maximization or minimization).
 */
enum class ObjectCategory {
    Maximum {
        override val reverse get() = Minimum
        override fun toString() = "Maximum"
    },
    Minimum {
        override val reverse get() = Maximum
        override fun toString() = "Minimum"
    };

    /** 反转的目标类型 / Reversed objective category */
    abstract val reverse: ObjectCategory
}
