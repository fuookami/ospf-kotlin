package fuookami.ospf.kotlin.utils.math

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

    val isTrue: Boolean? get() = when (this) {
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

    val isTrue: Boolean? get() = when (this) {
        True -> true
        False -> false
        Unknown -> null
    }
}
