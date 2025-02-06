package fuookami.ospf.kotlin.utils.operator

interface PartialEq<in Self> {
    infix fun partialEq(rhs: Self): Boolean?
}


interface Eq<in Self> : PartialEq<Self> {
    infix fun eq(rhs: Self): Boolean {
        return (this partialEq rhs)!!
    }

    infix fun neq(rhs: Self): Boolean {
        return !(this eq rhs)
    }
}

@JvmName("notNullPartialEqNullable")
infix fun <T : PartialEq<T>> T.partialEq(rhs: T?): Boolean? {
    return if (rhs == null) {
        false
    } else {
        this.partialEq(rhs)
    }
}

@JvmName("nullablePartialEqNotNull")
infix fun <T : PartialEq<T>> T?.partialEq(rhs: T): Boolean? {
    return if (this == null) {
        false
    } else {
        this.partialEq(rhs)
    }
}

@JvmName("nullablePartialEqNullable")
infix fun <T : PartialEq<T>> T?.partialEq(rhs: T?): Boolean? {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this partialEq rhs
    } else {
        false
    }
}

@JvmName("notNullEqNullable")
infix fun <T : Eq<T>> T.eq(rhs: T?): Boolean {
    return if (rhs == null) {
        false
    } else {
        this.eq(rhs)
    }
}

@JvmName("nullableEqNotNull")
infix fun <T: Eq<T>> T?.eq(rhs: T): Boolean {
    return if (this == null) {
        false
    } else {
        this.eq(rhs)
    }
}

@JvmName("nullableEqNullable")
infix fun <T : Eq<T>> T?.eq(rhs: T?): Boolean {
    return if (this == null && rhs == null) {
        true
    } else if (this != null && rhs != null) {
        this eq rhs
    } else {
        false
    }
}

@JvmName("nullableNeqNotNull")
infix fun <T : Eq<T>> T?.neq(rhs: T): Boolean {
    return if (this == null) {
        true
    } else {
        this.neq(rhs)
    }
}

@JvmName("notNullNeqNullable")
infix fun <T : Eq<T>> T.neq(rhs: T?): Boolean {
    return if (rhs == null) {
        true
    } else {
        this.neq(rhs)
    }
}

@JvmName("nullableNeqNullable")
infix fun <T : Eq<T>> T?.neq(rhs: T?): Boolean {
    return if (this == null && rhs == null) {
        false
    } else if (this != null && rhs != null) {
        this neq rhs
    } else {
        true
    }
}
