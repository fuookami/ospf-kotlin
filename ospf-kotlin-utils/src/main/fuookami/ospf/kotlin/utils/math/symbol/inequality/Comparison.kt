package fuookami.ospf.kotlin.utils.math.symbol.inequality

enum class Comparison {
    LT,
    LE,
    EQ,
    NE,
    GE,
    GT;

    val symbol: String
        get() = when (this) {
            LT -> "<"
            LE -> "<="
            EQ -> "="
            NE -> "!="
            GE -> ">="
            GT -> ">"
        }

    val isStrict: Boolean
        get() = this == LT || this == GT || this == NE

    val includesEquality: Boolean
        get() = this == LE || this == EQ || this == GE

    val isLessLike: Boolean
        get() = this == LT || this == LE

    val isGreaterLike: Boolean
        get() = this == GT || this == GE

    fun reverse(): Comparison {
        return when (this) {
            LT -> GT
            LE -> GE
            EQ -> EQ
            NE -> NE
            GE -> LE
            GT -> LT
        }
    }
}
