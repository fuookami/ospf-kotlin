/**
 * 三值逻辑
 * Trivalent Logic
 *
 * 定义 Trivalent 和 BalancedTrivalent 枚举类型，用于表示三值逻辑 (True, False, Unknown)，支持与布尔值和可空布尔值的相互转换。
 * Defines Trivalent and BalancedTrivalent enum types for representing three-valued logic (True, False, Unknown), supporting conversions with Boolean and nullable Boolean values.
 */
package fuookami.ospf.kotlin.math

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.value_range.*

enum class Trivalent(val value: URtn8) {
    True(URtn8.one),
    False(URtn8.zero),
    Unknown(URtn8(UInt8.one, UInt8.two));

    companion object {
        operator fun invoke(value: Boolean): Trivalent {
            return when (value) {
                true -> True
                false -> False
            }
        }

        operator fun invoke(value: Boolean?): Trivalent {
            return when (value) {
                true -> True
                false -> False
                null -> Unknown
            }
        }

        operator fun invoke(value: BalancedTrivalent): Trivalent {
            return when (value) {
                BalancedTrivalent.True -> True
                BalancedTrivalent.False -> False
                BalancedTrivalent.Unknown -> Unknown
            }
        }
    }

    val isTrue: Boolean?
        get() = when (this) {
            True -> true
            False -> false
            Unknown -> null
        }
}

enum class BalancedTrivalent(val value: Int8) {
    True(Int8.one),
    False(-Int8.one),
    Unknown(Int8.zero);

    companion object {
        operator fun invoke(value: Boolean): BalancedTrivalent {
            return when (value) {
                true -> True
                false -> False
            }
        }

        operator fun invoke(value: Boolean?): BalancedTrivalent {
            return when (value) {
                true -> True
                false -> False
                null -> Unknown
            }
        }

        operator fun invoke(value: Trivalent): BalancedTrivalent {
            return when (value) {
                Trivalent.True -> True
                Trivalent.False -> False
                Trivalent.Unknown -> Unknown
            }
        }
    }

    val isTrue: Boolean?
        get() = when (this) {
            True -> true
            False -> false
            Unknown -> null
        }
}




