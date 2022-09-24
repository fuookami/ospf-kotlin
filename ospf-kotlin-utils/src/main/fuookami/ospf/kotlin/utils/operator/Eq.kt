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
