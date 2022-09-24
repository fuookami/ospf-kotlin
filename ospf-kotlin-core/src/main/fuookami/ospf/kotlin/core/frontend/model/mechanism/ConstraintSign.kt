package fuookami.ospf.kotlin.core.frontend.model.mechanism

private typealias InequalitySign = fuookami.ospf.kotlin.core.frontend.inequality.Sign

class InvalidConstraintSign(
    sign: InequalitySign
) : Throwable() {
    override val message: String = "No matched constraint sign of inequality sign: $sign."
}

enum class Sign {
    LessEqual {
        override fun toString() = "<="
    },
    Equal {
        override fun toString() = "="
    },
    GreaterEqual {
        override fun toString() = ">="
    };

    companion object {
        @Throws(InvalidConstraintSign::class)
        operator fun invoke(sign: InequalitySign) = when (sign) {
            InequalitySign.Less -> LessEqual
            InequalitySign.LessEqual -> LessEqual
            InequalitySign.Equal -> Equal
            InequalitySign.Unequal -> throw InvalidConstraintSign(sign)
            InequalitySign.Greater -> GreaterEqual
            InequalitySign.GreaterEqual -> GreaterEqual
        }
    }
}
