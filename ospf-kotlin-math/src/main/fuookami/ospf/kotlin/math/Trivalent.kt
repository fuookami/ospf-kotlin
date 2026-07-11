/**
 * 三值逻辑
 * Trivalent Logic
 *
 * 定义 Trivalent 和 BalancedTrivalent 枚举类型，用于表示三值逻辑 (True, False, Unknown)，支持与布尔值和可空布尔值的相互转换。
 * Defines Trivalent and BalancedTrivalent enum types for representing three-valued logic (True, False, Unknown), supporting conversions with Boolean and nullable Boolean values.
 *
 * 注意：使用 sealed class 而非 enum 以避免 URtn8 初始化顺序问题。
 * Note: Uses sealed class instead of enum to avoid URtn8 initialization order issues.
*/
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*

/**
 * 三值逻辑类型
 * Three-valued logic type
 *
 * 表示 True、False、Unknown 三种状态。
 * Represents True, False, Unknown states.
 *
 * 使用 sealed class 实现以支持延迟初始化，避免与 URtn8 的循环初始化依赖。
 * Implemented as sealed class to support lazy initialization, avoiding circular initialization dependency with URtn8.
*/
sealed class Trivalent {

    /**
     * 获取数值表示
     * Get numeric representation
    */
    abstract val value: URtn8

    /**
     * 是否为真
     * Whether this is true
     *
     * 返回布尔值，Unknown 返回 null。
     * Returns boolean value, Unknown returns null.
    */
    abstract val isTrue: Boolean?

    /**
     * 真值
     * True value
    */
    data object True : Trivalent() {
        override val value: URtn8 by lazy { URtn8.one }
        override val isTrue: Boolean? = true
    }

    /**
     * 假值
     * False value
    */
    data object False : Trivalent() {
        override val value: URtn8 by lazy { URtn8.zero }
        override val isTrue: Boolean? = false
    }

    /**
     * 未知值
     * Unknown value
    */
    data object Unknown : Trivalent() {
        override val value: URtn8 by lazy { URtn8(UInt8.one, UInt8.two) }
        override val isTrue: Boolean? = null
    }

    companion object {
        /**
         * 从布尔值创建
         * Create from boolean
         *
         * @param value 布尔值 / Boolean value
         * @return 对应的三值逻辑 / Corresponding trivalent value
        */
        operator fun invoke(value: Boolean): Trivalent {
            return when (value) {
                true -> True
                false -> False
            }
        }

        /**
         * 从可空布尔值创建
         * Create from nullable boolean
         *
         * @param value 可空布尔值 / Nullable boolean value
         * @return 对应的三值逻辑 / Corresponding trivalent value
        */
        operator fun invoke(value: Boolean?): Trivalent {
            return when (value) {
                true -> True
                false -> False
                null -> Unknown
            }
        }

        /**
         * 从 BalancedTrivalent 创建
         * Create from BalancedTrivalent
         *
         * @param value 平衡三值逻辑 / Balanced trivalent value
         * @return 对应的三值逻辑 / Corresponding trivalent value
        */
        operator fun invoke(value: BalancedTrivalent): Trivalent {
            return when (value) {
                BalancedTrivalent.True -> True
                BalancedTrivalent.False -> False
                BalancedTrivalent.Unknown -> Unknown
            }
        }
    }
}

/**
 * 平衡三值逻辑类型
 * Balanced three-valued logic type
 *
 * 表示 True、False、Unknown 三种状态，使用 Int8 表示（+1, -1, 0）。
 * Represents True, False, Unknown states using Int8 (+1, -1, 0).
 *
 * 使用 sealed class 实现以支持延迟初始化。
 * Implemented as sealed class to support lazy initialization.
*/
sealed class BalancedTrivalent {

    /**
     * 获取数值表示
     * Get numeric representation
    */
    abstract val value: Int8

    /**
     * 是否为真
     * Whether this is true
    */
    abstract val isTrue: Boolean?

    /**
     * 真值 (+1)
     * True value (+1)
    */
    data object True : BalancedTrivalent() {
        override val value: Int8 by lazy { Int8.one }
        override val isTrue: Boolean? = true
    }

    /**
     * 假值 (-1)
     * False value (-1)
    */
    data object False : BalancedTrivalent() {
        override val value: Int8 by lazy { -Int8.one }
        override val isTrue: Boolean? = false
    }

    /**
     * 未知值 (0)
     * Unknown value (0)
    */
    data object Unknown : BalancedTrivalent() {
        override val value: Int8 by lazy { Int8.zero }
        override val isTrue: Boolean? = null
    }

    companion object {
        /**
         * 从布尔值创建
         * Create from boolean
         *
         * @param value 布尔值 / Boolean value
         * @return 对应的平衡三值逻辑 / Corresponding balanced trivalent value
        */
        operator fun invoke(value: Boolean): BalancedTrivalent {
            return when (value) {
                true -> True
                false -> False
            }
        }

        /**
         * 从可空布尔值创建
         * Create from nullable boolean
         *
         * @param value 可空布尔值 / Nullable boolean value
         * @return 对应的平衡三值逻辑 / Corresponding balanced trivalent value
        */
        operator fun invoke(value: Boolean?): BalancedTrivalent {
            return when (value) {
                true -> True
                false -> False
                null -> Unknown
            }
        }

        /**
         * 从 Trivalent 创建
         * Create from Trivalent
         *
         * @param value 三值逻辑 / Trivalent value
         * @return 对应的平衡三值逻辑 / Corresponding balanced trivalent value
        */
        operator fun invoke(value: Trivalent): BalancedTrivalent {
            return when (value) {
                Trivalent.True -> True
                Trivalent.False -> False
                Trivalent.Unknown -> Unknown
            }
        }
    }
}
