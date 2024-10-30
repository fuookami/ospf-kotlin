package fuookami.ospf.kotlin.utils.math.value_range

import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*
import fuookami.ospf.kotlin.utils.operator.*

private data object IntervalSerializer : KSerializer<Interval> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("IntervalType", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Interval) {
        encoder.encodeString(value.toString().lowercase(Locale.getDefault()))
    }

    override fun deserialize(decoder: Decoder): Interval {
        return Interval.valueOf(decoder.decodeString()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() })
    }
}

@Serializable(with = IntervalSerializer::class)
enum class Interval {
    Open {
        override val lowerSign = "("
        override val upperSign = ")"
        override fun union(rhs: Interval) = rhs
        override fun intersect(rhs: Interval) = Open
        override fun outer(rhs: Interval) = false

        override fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs -> lhs partialOrd rhs is Order.Less }
        }

        override fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs -> lhs partialOrd rhs is Order.Greater }
        }
    },
    Closed {
        override val lowerSign = "["
        override val upperSign = "]"
        override fun union(rhs: Interval) = Closed
        override fun intersect(rhs: Interval) = rhs
        override fun outer(rhs: Interval) = rhs == Open

        override fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs ->
                when (lhs partialOrd rhs) {
                    is Order.Less, Order.Equal -> true

                    else -> false
                }
            }
        }

        override fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean {
            return { lhs, rhs ->
                when (lhs partialOrd rhs) {
                    is Order.Greater, Order.Equal -> true

                    else -> false
                }
            }
        }
    };

    abstract val lowerSign: String
    abstract val upperSign: String
    abstract infix fun union(rhs: Interval): Interval
    abstract infix fun intersect(rhs: Interval): Interval
    abstract infix fun outer(rhs: Interval): Boolean

    abstract fun <T : PartialOrd<T>> lowerBoundOperator(): (T, T) -> Boolean
    abstract fun <T : PartialOrd<T>> upperBoundOperator(): (T, T) -> Boolean
}
