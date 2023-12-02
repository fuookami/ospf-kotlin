package fuookami.ospf.kotlin.utils.operator

interface PartialEq<Self> {
    infix fun partialEq(rhs: Self): Boolean?
}


interface Eq<Self> : PartialEq<Self> {
    infix fun eq(rhs: Self): Boolean {
        return (this partialEq rhs)!!
    }

    infix fun neq(rhs: Self): Boolean {
        return !(this eq rhs)
    }
}

infix fun <T : PartialEq<T>> T?.partialEq(rhs: T?): Boolean? {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this partialEq rhs
    } else {
        false
    }
}

infix fun <T : Eq<T>> T?.eq(rhs: T?): Boolean {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this eq rhs
    } else {
        false
    }
}
