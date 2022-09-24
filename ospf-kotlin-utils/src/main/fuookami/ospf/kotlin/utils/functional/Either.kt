package fuookami.ospf.kotlin.utils.functional

import fuookami.ospf.kotlin.utils.concept.*

sealed class Either<L, R> {
    data class Left<L, R>(var value: L) : Either<L, R>() {
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Left<*, *>) return false

            if (value != other.value) return false

            return true
        }
    }

    data class Right<L, R>(var value: R) : Either<L, R>() {
        override fun hashCode(): Int = value.hashCode()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Left<*, *>) return false

            if (value != other.value) return false

            return true
        }
    }

    fun isLeft() = this is Left
    fun isRight() = this is Right

    fun left(): L? = when (this) {
        is Left -> {
            this.value
        }
        else -> {
            null
        }
    }

    fun right(): R? = when (this) {
        is Right -> {
            this.value
        }
        else -> {
            null
        }
    }

    fun <Ret> ifLeft(callBack: (L) -> Ret) = EitherMatcher<L, R, Ret>(this).ifLeft(callBack)
    fun <Ret> ifRight(callBack: (R) -> Ret) = EitherMatcher<L, R, Ret>(this).ifRight(callBack)
}

class EitherMatcher<L, R, Ret>(private val value: Either<L, R>) {
    private lateinit var leftCallBack: (L) -> Ret
    private lateinit var rightCallBack: (R) -> Ret

    fun ifLeft(callBack: (L) -> Ret): EitherMatcher<L, R, Ret> {
        leftCallBack = callBack
        return this
    }

    fun ifRight(callBack: (R) -> Ret): EitherMatcher<L, R, Ret> {
        rightCallBack = callBack
        return this
    }

    @Throws(NullPointerException::class)
    operator fun invoke(): Ret = when (value) {
        is Either.Left -> {
            leftCallBack(value.value); }
        is Either.Right -> {
            rightCallBack(value.value); }
    }
}

@Throws(NullPointerException::class)
fun <L, R, Ret> match(value: Either<L, R>, leftCallBack: (L) -> Ret, rightCallBack: (R) -> Ret): Ret {
    val matcher = value.ifLeft(leftCallBack).ifRight(rightCallBack)
    return matcher()
}

fun <L : fuookami.ospf.kotlin.utils.concept.Cloneable<L>, R : fuookami.ospf.kotlin.utils.concept.Cloneable<R>> Either<L, R>.clone(
    value: Either<L, R>
): Either<L, R> =
    when (value) {
        is Either.Left -> {
            Either.Left(clone(value.value))
        }
        is Either.Right -> {
            Either.Right(clone(value.value))
        }
    }

fun <L : Movable<L>, R : Movable<R>> Either<L, R>.move(value: Either<L, R>): Either<L, R> = when (value) {
    is Either.Left -> {
        Either.Left(move(value.value))
    }
    is Either.Right -> {
        Either.Right(move(value.value))
    }
}
