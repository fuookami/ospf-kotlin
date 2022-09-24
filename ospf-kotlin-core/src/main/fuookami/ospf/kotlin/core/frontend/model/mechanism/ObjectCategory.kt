package fuookami.ospf.kotlin.core.frontend.model.mechanism

enum class ObjectCategory {
    Maximum {
        override fun reverse() = Minimum
        override fun toString() = "Maximum"
    },
    Minimum {
        override fun reverse() = Maximum
        override fun toString() = "Minimum"
    };

    abstract fun reverse(): ObjectCategory
}
