package fuookami.ospf.kotlin.utils.functional

import kotlin.reflect.*
import kotlin.collections.*
import fuookami.ospf.kotlin.utils.concept.*

sealed class Variant2<T1, T2>() {
    data class V1<T1, T2>(val value: T1) : Variant2<T1, T2>() {}
    data class V2<T1, T2>(val value: T2) : Variant2<T1, T2>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant2Matcher<T1, T2, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant2Matcher<T1, T2, Ret>(this).if2(callBack);

}

data class Variant2Matcher<T1, T2, Ret>(private val value: Variant2<T1, T2>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant2Matcher<T1, T2, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant2Matcher<T1, T2, Ret> {
        callBack2 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant2.V1 -> {
            callBack1(value.value)
        }

        is Variant2.V2 -> {
            callBack2(value.value)
        }
    }
}

sealed class Variant3<T1, T2, T3>() {
    data class V1<T1, T2, T3>(val value: T1) : Variant3<T1, T2, T3>() {}
    data class V2<T1, T2, T3>(val value: T2) : Variant3<T1, T2, T3>() {}
    data class V3<T1, T2, T3>(val value: T3) : Variant3<T1, T2, T3>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if3(callBack);

}

data class Variant3Matcher<T1, T2, T3, Ret>(private val value: Variant3<T1, T2, T3>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack3 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant3.V1 -> {
            callBack1(value.value)
        }

        is Variant3.V2 -> {
            callBack2(value.value)
        }

        is Variant3.V3 -> {
            callBack3(value.value)
        }
    }
}

sealed class Variant4<T1, T2, T3, T4>() {
    data class V1<T1, T2, T3, T4>(val value: T1) : Variant4<T1, T2, T3, T4>() {}
    data class V2<T1, T2, T3, T4>(val value: T2) : Variant4<T1, T2, T3, T4>() {}
    data class V3<T1, T2, T3, T4>(val value: T3) : Variant4<T1, T2, T3, T4>() {}
    data class V4<T1, T2, T3, T4>(val value: T4) : Variant4<T1, T2, T3, T4>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if4(callBack);

}

data class Variant4Matcher<T1, T2, T3, T4, Ret>(private val value: Variant4<T1, T2, T3, T4>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack4 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant4.V1 -> {
            callBack1(value.value)
        }

        is Variant4.V2 -> {
            callBack2(value.value)
        }

        is Variant4.V3 -> {
            callBack3(value.value)
        }

        is Variant4.V4 -> {
            callBack4(value.value)
        }
    }
}

sealed class Variant5<T1, T2, T3, T4, T5>() {
    data class V1<T1, T2, T3, T4, T5>(val value: T1) : Variant5<T1, T2, T3, T4, T5>() {}
    data class V2<T1, T2, T3, T4, T5>(val value: T2) : Variant5<T1, T2, T3, T4, T5>() {}
    data class V3<T1, T2, T3, T4, T5>(val value: T3) : Variant5<T1, T2, T3, T4, T5>() {}
    data class V4<T1, T2, T3, T4, T5>(val value: T4) : Variant5<T1, T2, T3, T4, T5>() {}
    data class V5<T1, T2, T3, T4, T5>(val value: T5) : Variant5<T1, T2, T3, T4, T5>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if5(callBack);

}

data class Variant5Matcher<T1, T2, T3, T4, T5, Ret>(private val value: Variant5<T1, T2, T3, T4, T5>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack5 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant5.V1 -> {
            callBack1(value.value)
        }

        is Variant5.V2 -> {
            callBack2(value.value)
        }

        is Variant5.V3 -> {
            callBack3(value.value)
        }

        is Variant5.V4 -> {
            callBack4(value.value)
        }

        is Variant5.V5 -> {
            callBack5(value.value)
        }
    }
}

sealed class Variant6<T1, T2, T3, T4, T5, T6>() {
    data class V1<T1, T2, T3, T4, T5, T6>(val value: T1) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    data class V2<T1, T2, T3, T4, T5, T6>(val value: T2) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    data class V3<T1, T2, T3, T4, T5, T6>(val value: T3) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    data class V4<T1, T2, T3, T4, T5, T6>(val value: T4) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    data class V5<T1, T2, T3, T4, T5, T6>(val value: T5) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    data class V6<T1, T2, T3, T4, T5, T6>(val value: T6) : Variant6<T1, T2, T3, T4, T5, T6>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if6(callBack);

}

data class Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(private val value: Variant6<T1, T2, T3, T4, T5, T6>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack6 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant6.V1 -> {
            callBack1(value.value)
        }

        is Variant6.V2 -> {
            callBack2(value.value)
        }

        is Variant6.V3 -> {
            callBack3(value.value)
        }

        is Variant6.V4 -> {
            callBack4(value.value)
        }

        is Variant6.V5 -> {
            callBack5(value.value)
        }

        is Variant6.V6 -> {
            callBack6(value.value)
        }
    }
}

sealed class Variant7<T1, T2, T3, T4, T5, T6, T7>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7>(val value: T1) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V2<T1, T2, T3, T4, T5, T6, T7>(val value: T2) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V3<T1, T2, T3, T4, T5, T6, T7>(val value: T3) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V4<T1, T2, T3, T4, T5, T6, T7>(val value: T4) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V5<T1, T2, T3, T4, T5, T6, T7>(val value: T5) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V6<T1, T2, T3, T4, T5, T6, T7>(val value: T6) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    data class V7<T1, T2, T3, T4, T5, T6, T7>(val value: T7) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if7(callBack);

}

data class Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(private val value: Variant7<T1, T2, T3, T4, T5, T6, T7>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack7 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant7.V1 -> {
            callBack1(value.value)
        }

        is Variant7.V2 -> {
            callBack2(value.value)
        }

        is Variant7.V3 -> {
            callBack3(value.value)
        }

        is Variant7.V4 -> {
            callBack4(value.value)
        }

        is Variant7.V5 -> {
            callBack5(value.value)
        }

        is Variant7.V6 -> {
            callBack6(value.value)
        }

        is Variant7.V7 -> {
            callBack7(value.value)
        }
    }
}

sealed class Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T1) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T2) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T3) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T4) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T5) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T6) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T7) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T8) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if8(callBack);

}

data class Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(private val value: Variant8<T1, T2, T3, T4, T5, T6, T7, T8>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack8 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant8.V1 -> {
            callBack1(value.value)
        }

        is Variant8.V2 -> {
            callBack2(value.value)
        }

        is Variant8.V3 -> {
            callBack3(value.value)
        }

        is Variant8.V4 -> {
            callBack4(value.value)
        }

        is Variant8.V5 -> {
            callBack5(value.value)
        }

        is Variant8.V6 -> {
            callBack6(value.value)
        }

        is Variant8.V7 -> {
            callBack7(value.value)
        }

        is Variant8.V8 -> {
            callBack8(value.value)
        }
    }
}

sealed class Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T1) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T2) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T3) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T4) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T5) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T6) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T7) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T8) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T9) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if9(callBack);

}

data class Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(private val value: Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack9 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant9.V1 -> {
            callBack1(value.value)
        }

        is Variant9.V2 -> {
            callBack2(value.value)
        }

        is Variant9.V3 -> {
            callBack3(value.value)
        }

        is Variant9.V4 -> {
            callBack4(value.value)
        }

        is Variant9.V5 -> {
            callBack5(value.value)
        }

        is Variant9.V6 -> {
            callBack6(value.value)
        }

        is Variant9.V7 -> {
            callBack7(value.value)
        }

        is Variant9.V8 -> {
            callBack8(value.value)
        }

        is Variant9.V9 -> {
            callBack9(value.value)
        }
    }
}

sealed class Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T1) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T2) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T3) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T4) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T5) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T6) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T7) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T8) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T9) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T10) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if10(callBack);

}

data class Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(private val value: Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack10 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant10.V1 -> {
            callBack1(value.value)
        }

        is Variant10.V2 -> {
            callBack2(value.value)
        }

        is Variant10.V3 -> {
            callBack3(value.value)
        }

        is Variant10.V4 -> {
            callBack4(value.value)
        }

        is Variant10.V5 -> {
            callBack5(value.value)
        }

        is Variant10.V6 -> {
            callBack6(value.value)
        }

        is Variant10.V7 -> {
            callBack7(value.value)
        }

        is Variant10.V8 -> {
            callBack8(value.value)
        }

        is Variant10.V9 -> {
            callBack9(value.value)
        }

        is Variant10.V10 -> {
            callBack10(value.value)
        }
    }
}

sealed class Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T1) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T2) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T3) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T4) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T5) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T6) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T7) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T8) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T9) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T10) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T11) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if11(callBack);

}

data class Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(private val value: Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack11 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant11.V1 -> {
            callBack1(value.value)
        }

        is Variant11.V2 -> {
            callBack2(value.value)
        }

        is Variant11.V3 -> {
            callBack3(value.value)
        }

        is Variant11.V4 -> {
            callBack4(value.value)
        }

        is Variant11.V5 -> {
            callBack5(value.value)
        }

        is Variant11.V6 -> {
            callBack6(value.value)
        }

        is Variant11.V7 -> {
            callBack7(value.value)
        }

        is Variant11.V8 -> {
            callBack8(value.value)
        }

        is Variant11.V9 -> {
            callBack9(value.value)
        }

        is Variant11.V10 -> {
            callBack10(value.value)
        }

        is Variant11.V11 -> {
            callBack11(value.value)
        }
    }
}

sealed class Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T1) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T2) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T3) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T4) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T5) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T6) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T7) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T8) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T9) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T10) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T11) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T12) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if12(callBack);

}

data class Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(private val value: Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack12 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant12.V1 -> {
            callBack1(value.value)
        }

        is Variant12.V2 -> {
            callBack2(value.value)
        }

        is Variant12.V3 -> {
            callBack3(value.value)
        }

        is Variant12.V4 -> {
            callBack4(value.value)
        }

        is Variant12.V5 -> {
            callBack5(value.value)
        }

        is Variant12.V6 -> {
            callBack6(value.value)
        }

        is Variant12.V7 -> {
            callBack7(value.value)
        }

        is Variant12.V8 -> {
            callBack8(value.value)
        }

        is Variant12.V9 -> {
            callBack9(value.value)
        }

        is Variant12.V10 -> {
            callBack10(value.value)
        }

        is Variant12.V11 -> {
            callBack11(value.value)
        }

        is Variant12.V12 -> {
            callBack12(value.value)
        }
    }
}

sealed class Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T1) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T2) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T3) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T4) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T5) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T6) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T7) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T8) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T9) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T10) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T11) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T12) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T13) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if12(callBack);

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if13(callBack);

}

data class Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(private val value: Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack13 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant13.V1 -> {
            callBack1(value.value)
        }

        is Variant13.V2 -> {
            callBack2(value.value)
        }

        is Variant13.V3 -> {
            callBack3(value.value)
        }

        is Variant13.V4 -> {
            callBack4(value.value)
        }

        is Variant13.V5 -> {
            callBack5(value.value)
        }

        is Variant13.V6 -> {
            callBack6(value.value)
        }

        is Variant13.V7 -> {
            callBack7(value.value)
        }

        is Variant13.V8 -> {
            callBack8(value.value)
        }

        is Variant13.V9 -> {
            callBack9(value.value)
        }

        is Variant13.V10 -> {
            callBack10(value.value)
        }

        is Variant13.V11 -> {
            callBack11(value.value)
        }

        is Variant13.V12 -> {
            callBack12(value.value)
        }

        is Variant13.V13 -> {
            callBack13(value.value)
        }
    }
}

sealed class Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T1) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T2) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T3) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T4) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T5) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T6) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T7) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T8) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T9) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T10) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T11) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T12) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T13) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T14) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if12(callBack);

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if13(callBack);

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if14(callBack);

}

data class Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(private val value: Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack14 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant14.V1 -> {
            callBack1(value.value)
        }

        is Variant14.V2 -> {
            callBack2(value.value)
        }

        is Variant14.V3 -> {
            callBack3(value.value)
        }

        is Variant14.V4 -> {
            callBack4(value.value)
        }

        is Variant14.V5 -> {
            callBack5(value.value)
        }

        is Variant14.V6 -> {
            callBack6(value.value)
        }

        is Variant14.V7 -> {
            callBack7(value.value)
        }

        is Variant14.V8 -> {
            callBack8(value.value)
        }

        is Variant14.V9 -> {
            callBack9(value.value)
        }

        is Variant14.V10 -> {
            callBack10(value.value)
        }

        is Variant14.V11 -> {
            callBack11(value.value)
        }

        is Variant14.V12 -> {
            callBack12(value.value)
        }

        is Variant14.V13 -> {
            callBack13(value.value)
        }

        is Variant14.V14 -> {
            callBack14(value.value)
        }
    }
}

sealed class Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T1) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T2) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T3) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T4) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T5) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T6) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T7) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T8) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T9) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T10) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T11) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T12) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T13) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T14) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T15) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if12(callBack);

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if13(callBack);

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if14(callBack);

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if15(callBack);

}

data class Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(private val value: Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack15 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant15.V1 -> {
            callBack1(value.value)
        }

        is Variant15.V2 -> {
            callBack2(value.value)
        }

        is Variant15.V3 -> {
            callBack3(value.value)
        }

        is Variant15.V4 -> {
            callBack4(value.value)
        }

        is Variant15.V5 -> {
            callBack5(value.value)
        }

        is Variant15.V6 -> {
            callBack6(value.value)
        }

        is Variant15.V7 -> {
            callBack7(value.value)
        }

        is Variant15.V8 -> {
            callBack8(value.value)
        }

        is Variant15.V9 -> {
            callBack9(value.value)
        }

        is Variant15.V10 -> {
            callBack10(value.value)
        }

        is Variant15.V11 -> {
            callBack11(value.value)
        }

        is Variant15.V12 -> {
            callBack12(value.value)
        }

        is Variant15.V13 -> {
            callBack13(value.value)
        }

        is Variant15.V14 -> {
            callBack14(value.value)
        }

        is Variant15.V15 -> {
            callBack15(value.value)
        }
    }
}

sealed class Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T1) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T2) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T3) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T4) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T5) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T6) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T7) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T8) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T9) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T10) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T11) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T12) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T13) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T14) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T15) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T16) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if12(callBack);

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if13(callBack);

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if14(callBack);

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if15(callBack);

    val is16 get() = this is V16;

    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if16(callBack);

}

data class Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(private val value: Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;
    private lateinit var callBack16: (T16) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack15 = callBack;
        return this;
    }

    fun if16(callBack: (T16) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack16 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant16.V1 -> {
            callBack1(value.value)
        }

        is Variant16.V2 -> {
            callBack2(value.value)
        }

        is Variant16.V3 -> {
            callBack3(value.value)
        }

        is Variant16.V4 -> {
            callBack4(value.value)
        }

        is Variant16.V5 -> {
            callBack5(value.value)
        }

        is Variant16.V6 -> {
            callBack6(value.value)
        }

        is Variant16.V7 -> {
            callBack7(value.value)
        }

        is Variant16.V8 -> {
            callBack8(value.value)
        }

        is Variant16.V9 -> {
            callBack9(value.value)
        }

        is Variant16.V10 -> {
            callBack10(value.value)
        }

        is Variant16.V11 -> {
            callBack11(value.value)
        }

        is Variant16.V12 -> {
            callBack12(value.value)
        }

        is Variant16.V13 -> {
            callBack13(value.value)
        }

        is Variant16.V14 -> {
            callBack14(value.value)
        }

        is Variant16.V15 -> {
            callBack15(value.value)
        }

        is Variant16.V16 -> {
            callBack16(value.value)
        }
    }
}

sealed class Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T1) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T2) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T3) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T4) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T5) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T6) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T7) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T8) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T9) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T10) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T11) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T12) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T13) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T14) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T15) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T16) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T17) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if1(
            callBack
        );

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if2(
            callBack
        );

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if3(
            callBack
        );

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if4(
            callBack
        );

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if5(
            callBack
        );

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if6(
            callBack
        );

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if7(
            callBack
        );

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if8(
            callBack
        );

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if9(
            callBack
        );

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if10(
            callBack
        );

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if11(
            callBack
        );

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if12(
            callBack
        );

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if13(
            callBack
        );

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if14(
            callBack
        );

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if15(
            callBack
        );

    val is16 get() = this is V16;

    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if16(
            callBack
        );

    val is17 get() = this is V17;

    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if17(
            callBack
        );

}

data class Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(private val value: Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;
    private lateinit var callBack16: (T16) -> Ret;
    private lateinit var callBack17: (T17) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack15 = callBack;
        return this;
    }

    fun if16(callBack: (T16) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack16 = callBack;
        return this;
    }

    fun if17(callBack: (T17) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack17 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant17.V1 -> {
            callBack1(value.value)
        }

        is Variant17.V2 -> {
            callBack2(value.value)
        }

        is Variant17.V3 -> {
            callBack3(value.value)
        }

        is Variant17.V4 -> {
            callBack4(value.value)
        }

        is Variant17.V5 -> {
            callBack5(value.value)
        }

        is Variant17.V6 -> {
            callBack6(value.value)
        }

        is Variant17.V7 -> {
            callBack7(value.value)
        }

        is Variant17.V8 -> {
            callBack8(value.value)
        }

        is Variant17.V9 -> {
            callBack9(value.value)
        }

        is Variant17.V10 -> {
            callBack10(value.value)
        }

        is Variant17.V11 -> {
            callBack11(value.value)
        }

        is Variant17.V12 -> {
            callBack12(value.value)
        }

        is Variant17.V13 -> {
            callBack13(value.value)
        }

        is Variant17.V14 -> {
            callBack14(value.value)
        }

        is Variant17.V15 -> {
            callBack15(value.value)
        }

        is Variant17.V16 -> {
            callBack16(value.value)
        }

        is Variant17.V17 -> {
            callBack17(value.value)
        }
    }
}

sealed class Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T1) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T2) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T3) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T4) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T5) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T6) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T7) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T8) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T9) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T10) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T11) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T12) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T13) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T14) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T15) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T16) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T17) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T18) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if1(
            callBack
        );

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if2(
            callBack
        );

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if3(
            callBack
        );

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if4(
            callBack
        );

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if5(
            callBack
        );

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if6(
            callBack
        );

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if7(
            callBack
        );

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if8(
            callBack
        );

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if9(
            callBack
        );

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if10(
            callBack
        );

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if11(
            callBack
        );

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if12(
            callBack
        );

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if13(
            callBack
        );

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if14(
            callBack
        );

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if15(
            callBack
        );

    val is16 get() = this is V16;

    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if16(
            callBack
        );

    val is17 get() = this is V17;

    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if17(
            callBack
        );

    val is18 get() = this is V18;

    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if18(
            callBack
        );

}

data class Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(
    private val value: Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>
) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;
    private lateinit var callBack16: (T16) -> Ret;
    private lateinit var callBack17: (T17) -> Ret;
    private lateinit var callBack18: (T18) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack15 = callBack;
        return this;
    }

    fun if16(callBack: (T16) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack16 = callBack;
        return this;
    }

    fun if17(callBack: (T17) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack17 = callBack;
        return this;
    }

    fun if18(callBack: (T18) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack18 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant18.V1 -> {
            callBack1(value.value)
        }

        is Variant18.V2 -> {
            callBack2(value.value)
        }

        is Variant18.V3 -> {
            callBack3(value.value)
        }

        is Variant18.V4 -> {
            callBack4(value.value)
        }

        is Variant18.V5 -> {
            callBack5(value.value)
        }

        is Variant18.V6 -> {
            callBack6(value.value)
        }

        is Variant18.V7 -> {
            callBack7(value.value)
        }

        is Variant18.V8 -> {
            callBack8(value.value)
        }

        is Variant18.V9 -> {
            callBack9(value.value)
        }

        is Variant18.V10 -> {
            callBack10(value.value)
        }

        is Variant18.V11 -> {
            callBack11(value.value)
        }

        is Variant18.V12 -> {
            callBack12(value.value)
        }

        is Variant18.V13 -> {
            callBack13(value.value)
        }

        is Variant18.V14 -> {
            callBack14(value.value)
        }

        is Variant18.V15 -> {
            callBack15(value.value)
        }

        is Variant18.V16 -> {
            callBack16(value.value)
        }

        is Variant18.V17 -> {
            callBack17(value.value)
        }

        is Variant18.V18 -> {
            callBack18(value.value)
        }
    }
}

sealed class Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T1) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T2) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T3) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T4) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T5) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T6) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T7) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T8) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T9) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T10) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T11) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T12) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T13) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T14) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T15) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T16) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T17) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T18) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    data class V19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T19) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if1(
            callBack
        );

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if2(
            callBack
        );

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if3(
            callBack
        );

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if4(
            callBack
        );

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if5(
            callBack
        );

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if6(
            callBack
        );

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if7(
            callBack
        );

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if8(
            callBack
        );

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if9(
            callBack
        );

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if10(
            callBack
        );

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if11(
            callBack
        );

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if12(
            callBack
        );

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if13(
            callBack
        );

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if14(
            callBack
        );

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if15(
            callBack
        );

    val is16 get() = this is V16;

    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if16(
            callBack
        );

    val is17 get() = this is V17;

    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if17(
            callBack
        );

    val is18 get() = this is V18;

    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if18(
            callBack
        );

    val is19 get() = this is V19;

    val v19
        get() = when (this) {
            is V19 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if19(callBack: (T19) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if19(
            callBack
        );

}

data class Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(
    private val value: Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>
) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;
    private lateinit var callBack16: (T16) -> Ret;
    private lateinit var callBack17: (T17) -> Ret;
    private lateinit var callBack18: (T18) -> Ret;
    private lateinit var callBack19: (T19) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack15 = callBack;
        return this;
    }

    fun if16(callBack: (T16) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack16 = callBack;
        return this;
    }

    fun if17(callBack: (T17) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack17 = callBack;
        return this;
    }

    fun if18(callBack: (T18) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack18 = callBack;
        return this;
    }

    fun if19(callBack: (T19) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack19 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant19.V1 -> {
            callBack1(value.value)
        }

        is Variant19.V2 -> {
            callBack2(value.value)
        }

        is Variant19.V3 -> {
            callBack3(value.value)
        }

        is Variant19.V4 -> {
            callBack4(value.value)
        }

        is Variant19.V5 -> {
            callBack5(value.value)
        }

        is Variant19.V6 -> {
            callBack6(value.value)
        }

        is Variant19.V7 -> {
            callBack7(value.value)
        }

        is Variant19.V8 -> {
            callBack8(value.value)
        }

        is Variant19.V9 -> {
            callBack9(value.value)
        }

        is Variant19.V10 -> {
            callBack10(value.value)
        }

        is Variant19.V11 -> {
            callBack11(value.value)
        }

        is Variant19.V12 -> {
            callBack12(value.value)
        }

        is Variant19.V13 -> {
            callBack13(value.value)
        }

        is Variant19.V14 -> {
            callBack14(value.value)
        }

        is Variant19.V15 -> {
            callBack15(value.value)
        }

        is Variant19.V16 -> {
            callBack16(value.value)
        }

        is Variant19.V17 -> {
            callBack17(value.value)
        }

        is Variant19.V18 -> {
            callBack18(value.value)
        }

        is Variant19.V19 -> {
            callBack19(value.value)
        }
    }
}

sealed class Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T1) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T2) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T3) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T4) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T5) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T6) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T7) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T8) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T9) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T10) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T11) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T12) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T13) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T14) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T15) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T16) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T17) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T18) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T19) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    data class V20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T20) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    val is1 get() = this is V1;

    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if1(callBack);

    val is2 get() = this is V2;

    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if2(callBack);

    val is3 get() = this is V3;

    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if3(callBack);

    val is4 get() = this is V4;

    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if4(callBack);

    val is5 get() = this is V5;

    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if5(callBack);

    val is6 get() = this is V6;

    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if6(callBack);

    val is7 get() = this is V7;

    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if7(callBack);

    val is8 get() = this is V8;

    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if8(callBack);

    val is9 get() = this is V9;

    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if9(callBack);

    val is10 get() = this is V10;

    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if10(callBack);

    val is11 get() = this is V11;

    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if11(callBack);

    val is12 get() = this is V12;

    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if12(callBack);

    val is13 get() = this is V13;

    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if13(callBack);

    val is14 get() = this is V14;

    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if14(callBack);

    val is15 get() = this is V15;

    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if15(callBack);

    val is16 get() = this is V16;

    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if16(callBack);

    val is17 get() = this is V17;

    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if17(callBack);

    val is18 get() = this is V18;

    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if18(callBack);

    val is19 get() = this is V19;

    val v19
        get() = when (this) {
            is V19 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if19(callBack: (T19) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if19(callBack);

    val is20 get() = this is V20;

    val v20
        get() = when (this) {
            is V20 -> {
                this.value
            }

            else -> {
                null
            }
        }

    fun <Ret> if20(callBack: (T20) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if20(callBack);

}

data class Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
    private val value: Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>
) {
    private lateinit var callBack1: (T1) -> Ret;
    private lateinit var callBack2: (T2) -> Ret;
    private lateinit var callBack3: (T3) -> Ret;
    private lateinit var callBack4: (T4) -> Ret;
    private lateinit var callBack5: (T5) -> Ret;
    private lateinit var callBack6: (T6) -> Ret;
    private lateinit var callBack7: (T7) -> Ret;
    private lateinit var callBack8: (T8) -> Ret;
    private lateinit var callBack9: (T9) -> Ret;
    private lateinit var callBack10: (T10) -> Ret;
    private lateinit var callBack11: (T11) -> Ret;
    private lateinit var callBack12: (T12) -> Ret;
    private lateinit var callBack13: (T13) -> Ret;
    private lateinit var callBack14: (T14) -> Ret;
    private lateinit var callBack15: (T15) -> Ret;
    private lateinit var callBack16: (T16) -> Ret;
    private lateinit var callBack17: (T17) -> Ret;
    private lateinit var callBack18: (T18) -> Ret;
    private lateinit var callBack19: (T19) -> Ret;
    private lateinit var callBack20: (T20) -> Ret;

    fun if1(callBack: (T1) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack1 = callBack;
        return this;
    }

    fun if2(callBack: (T2) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack2 = callBack;
        return this;
    }

    fun if3(callBack: (T3) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack3 = callBack;
        return this;
    }

    fun if4(callBack: (T4) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack4 = callBack;
        return this;
    }

    fun if5(callBack: (T5) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack5 = callBack;
        return this;
    }

    fun if6(callBack: (T6) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack6 = callBack;
        return this;
    }

    fun if7(callBack: (T7) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack7 = callBack;
        return this;
    }

    fun if8(callBack: (T8) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack8 = callBack;
        return this;
    }

    fun if9(callBack: (T9) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack9 = callBack;
        return this;
    }

    fun if10(callBack: (T10) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack10 = callBack;
        return this;
    }

    fun if11(callBack: (T11) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack11 = callBack;
        return this;
    }

    fun if12(callBack: (T12) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack12 = callBack;
        return this;
    }

    fun if13(callBack: (T13) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack13 = callBack;
        return this;
    }

    fun if14(callBack: (T14) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack14 = callBack;
        return this;
    }

    fun if15(callBack: (T15) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack15 = callBack;
        return this;
    }

    fun if16(callBack: (T16) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack16 = callBack;
        return this;
    }

    fun if17(callBack: (T17) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack17 = callBack;
        return this;
    }

    fun if18(callBack: (T18) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack18 = callBack;
        return this;
    }

    fun if19(callBack: (T19) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack19 = callBack;
        return this;
    }

    fun if20(callBack: (T20) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack20 = callBack;
        return this;
    }

    @Throws(NullPointerException::class)
    operator fun invoke() = when (value) {
        is Variant20.V1 -> {
            callBack1(value.value)
        }

        is Variant20.V2 -> {
            callBack2(value.value)
        }

        is Variant20.V3 -> {
            callBack3(value.value)
        }

        is Variant20.V4 -> {
            callBack4(value.value)
        }

        is Variant20.V5 -> {
            callBack5(value.value)
        }

        is Variant20.V6 -> {
            callBack6(value.value)
        }

        is Variant20.V7 -> {
            callBack7(value.value)
        }

        is Variant20.V8 -> {
            callBack8(value.value)
        }

        is Variant20.V9 -> {
            callBack9(value.value)
        }

        is Variant20.V10 -> {
            callBack10(value.value)
        }

        is Variant20.V11 -> {
            callBack11(value.value)
        }

        is Variant20.V12 -> {
            callBack12(value.value)
        }

        is Variant20.V13 -> {
            callBack13(value.value)
        }

        is Variant20.V14 -> {
            callBack14(value.value)
        }

        is Variant20.V15 -> {
            callBack15(value.value)
        }

        is Variant20.V16 -> {
            callBack16(value.value)
        }

        is Variant20.V17 -> {
            callBack17(value.value)
        }

        is Variant20.V18 -> {
            callBack18(value.value)
        }

        is Variant20.V19 -> {
            callBack19(value.value)
        }

        is Variant20.V20 -> {
            callBack20(value.value)
        }
    }
}

class Variant(val value: Any, val clazz: KClass<*>) {
    companion object {
        inline fun <reified T : Any> make(value: T) = Variant(value, T::class);
    }

    inline fun <reified T : Any> isA() = clazz == T::class;

    inline fun <reified T : Any> get() = if (isA<T>()) {
        value as T;
    } else {
        null;
    }

    inline fun <reified T : Any, Ret> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
        val ret = VariantMatcher<Ret>(this);
        return ret.ifIs(callBack);
    }
}

class VariantMatcher<Ret>(private val value: Variant) {
    val callBacks: MutableMap<KClass<*>, (Any) -> Ret> = hashMapOf();

    inline fun <reified T : Any> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
        callBacks[T::class] = { value: Any -> callBack(value as T) };
        return this;
    }

    operator fun invoke() = callBacks[value.clazz]?.let { it -> it(value.value) };
}

fun <T1, T2, Ret> match(value: Variant2<T1, T2>, callBack1: (T1) -> Ret, callBack2: (T2) -> Ret): Ret {
    val matcher = value.if1(callBack1).if2(callBack2);
    return matcher();
}

fun <T1, T2, T3, Ret> match(
    value: Variant3<T1, T2, T3>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3);
    return matcher();
}

fun <T1, T2, T3, T4, Ret> match(
    value: Variant4<T1, T2, T3, T4>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4);
    return matcher();
}

fun <T1, T2, T3, T4, T5, Ret> match(
    value: Variant5<T1, T2, T3, T4, T5>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, Ret> match(
    value: Variant6<T1, T2, T3, T4, T5, T6>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, Ret> match(
    value: Variant7<T1, T2, T3, T4, T5, T6, T7>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, Ret> match(
    value: Variant8<T1, T2, T3, T4, T5, T6, T7, T8>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> match(
    value: Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> match(
    value: Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> match(
    value: Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> match(
    value: Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> match(
    value: Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> match(
    value: Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> match(
    value: Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> match(
    value: Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret,
    callBack16: (T16) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15).if16(callBack16);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> match(
    value: Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret,
    callBack16: (T16) -> Ret,
    callBack17: (T17) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> match(
    value: Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret,
    callBack16: (T16) -> Ret,
    callBack17: (T17) -> Ret,
    callBack18: (T18) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17).if18(callBack18);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> match(
    value: Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret,
    callBack16: (T16) -> Ret,
    callBack17: (T17) -> Ret,
    callBack18: (T18) -> Ret,
    callBack19: (T19) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17).if18(callBack18).if19(callBack19);
    return matcher();
}

fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> match(
    value: Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret,
    callBack6: (T6) -> Ret,
    callBack7: (T7) -> Ret,
    callBack8: (T8) -> Ret,
    callBack9: (T9) -> Ret,
    callBack10: (T10) -> Ret,
    callBack11: (T11) -> Ret,
    callBack12: (T12) -> Ret,
    callBack13: (T13) -> Ret,
    callBack14: (T14) -> Ret,
    callBack15: (T15) -> Ret,
    callBack16: (T16) -> Ret,
    callBack17: (T17) -> Ret,
    callBack18: (T18) -> Ret,
    callBack19: (T19) -> Ret,
    callBack20: (T20) -> Ret
): Ret {
    val matcher =
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17).if18(callBack18).if19(callBack19)
            .if20(callBack20);
    return matcher();
}

@JvmName("Variant2Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>> Variant2<T1, T2>.copy(): Variant2<T1, T2> = when (this) {
    is Variant2.V1 -> Variant2.V1(this.value.copy())
    is Variant2.V2 -> Variant2.V2(this.value.copy())
}

@JvmName("Variant3Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>> Variant3<T1, T2, T3>.copy(): Variant3<T1, T2, T3> =
    when (this) {
        is Variant3.V1 -> Variant3.V1(this.value.copy())
        is Variant3.V2 -> Variant3.V2(this.value.copy())
        is Variant3.V3 -> Variant3.V3(this.value.copy())
    }

@JvmName("Variant4Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>> Variant4<T1, T2, T3, T4>.copy(): Variant4<T1, T2, T3, T4> =
    when (this) {
        is Variant4.V1 -> Variant4.V1(this.value.copy())
        is Variant4.V2 -> Variant4.V2(this.value.copy())
        is Variant4.V3 -> Variant4.V3(this.value.copy())
        is Variant4.V4 -> Variant4.V4(this.value.copy())
    }

@JvmName("Variant5Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>> Variant5<T1, T2, T3, T4, T5>.copy(): Variant5<T1, T2, T3, T4, T5> =
    when (this) {
        is Variant5.V1 -> Variant5.V1(this.value.copy())
        is Variant5.V2 -> Variant5.V2(this.value.copy())
        is Variant5.V3 -> Variant5.V3(this.value.copy())
        is Variant5.V4 -> Variant5.V4(this.value.copy())
        is Variant5.V5 -> Variant5.V5(this.value.copy())
    }

@JvmName("Variant6Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>> Variant6<T1, T2, T3, T4, T5, T6>.copy(): Variant6<T1, T2, T3, T4, T5, T6> =
    when (this) {
        is Variant6.V1 -> Variant6.V1(this.value.copy())
        is Variant6.V2 -> Variant6.V2(this.value.copy())
        is Variant6.V3 -> Variant6.V3(this.value.copy())
        is Variant6.V4 -> Variant6.V4(this.value.copy())
        is Variant6.V5 -> Variant6.V5(this.value.copy())
        is Variant6.V6 -> Variant6.V6(this.value.copy())
    }

@JvmName("Variant7Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>> Variant7<T1, T2, T3, T4, T5, T6, T7>.copy(): Variant7<T1, T2, T3, T4, T5, T6, T7> =
    when (this) {
        is Variant7.V1 -> Variant7.V1(this.value.copy())
        is Variant7.V2 -> Variant7.V2(this.value.copy())
        is Variant7.V3 -> Variant7.V3(this.value.copy())
        is Variant7.V4 -> Variant7.V4(this.value.copy())
        is Variant7.V5 -> Variant7.V5(this.value.copy())
        is Variant7.V6 -> Variant7.V6(this.value.copy())
        is Variant7.V7 -> Variant7.V7(this.value.copy())
    }

@JvmName("Variant8Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>> Variant8<T1, T2, T3, T4, T5, T6, T7, T8>.copy(): Variant8<T1, T2, T3, T4, T5, T6, T7, T8> =
    when (this) {
        is Variant8.V1 -> Variant8.V1(this.value.copy())
        is Variant8.V2 -> Variant8.V2(this.value.copy())
        is Variant8.V3 -> Variant8.V3(this.value.copy())
        is Variant8.V4 -> Variant8.V4(this.value.copy())
        is Variant8.V5 -> Variant8.V5(this.value.copy())
        is Variant8.V6 -> Variant8.V6(this.value.copy())
        is Variant8.V7 -> Variant8.V7(this.value.copy())
        is Variant8.V8 -> Variant8.V8(this.value.copy())
    }

@JvmName("Variant9Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>> Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.copy(): Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9> =
    when (this) {
        is Variant9.V1 -> Variant9.V1(this.value.copy())
        is Variant9.V2 -> Variant9.V2(this.value.copy())
        is Variant9.V3 -> Variant9.V3(this.value.copy())
        is Variant9.V4 -> Variant9.V4(this.value.copy())
        is Variant9.V5 -> Variant9.V5(this.value.copy())
        is Variant9.V6 -> Variant9.V6(this.value.copy())
        is Variant9.V7 -> Variant9.V7(this.value.copy())
        is Variant9.V8 -> Variant9.V8(this.value.copy())
        is Variant9.V9 -> Variant9.V9(this.value.copy())
    }

@JvmName("Variant10Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>> Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>.copy(): Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> =
    when (this) {
        is Variant10.V1 -> Variant10.V1(this.value.copy())
        is Variant10.V2 -> Variant10.V2(this.value.copy())
        is Variant10.V3 -> Variant10.V3(this.value.copy())
        is Variant10.V4 -> Variant10.V4(this.value.copy())
        is Variant10.V5 -> Variant10.V5(this.value.copy())
        is Variant10.V6 -> Variant10.V6(this.value.copy())
        is Variant10.V7 -> Variant10.V7(this.value.copy())
        is Variant10.V8 -> Variant10.V8(this.value.copy())
        is Variant10.V9 -> Variant10.V9(this.value.copy())
        is Variant10.V10 -> Variant10.V10(this.value.copy())
    }

@JvmName("Variant11Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>> Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>.copy(): Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> =
    when (this) {
        is Variant11.V1 -> Variant11.V1(this.value.copy())
        is Variant11.V2 -> Variant11.V2(this.value.copy())
        is Variant11.V3 -> Variant11.V3(this.value.copy())
        is Variant11.V4 -> Variant11.V4(this.value.copy())
        is Variant11.V5 -> Variant11.V5(this.value.copy())
        is Variant11.V6 -> Variant11.V6(this.value.copy())
        is Variant11.V7 -> Variant11.V7(this.value.copy())
        is Variant11.V8 -> Variant11.V8(this.value.copy())
        is Variant11.V9 -> Variant11.V9(this.value.copy())
        is Variant11.V10 -> Variant11.V10(this.value.copy())
        is Variant11.V11 -> Variant11.V11(this.value.copy())
    }

@JvmName("Variant12Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>> Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>.copy(): Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> =
    when (this) {
        is Variant12.V1 -> Variant12.V1(this.value.copy())
        is Variant12.V2 -> Variant12.V2(this.value.copy())
        is Variant12.V3 -> Variant12.V3(this.value.copy())
        is Variant12.V4 -> Variant12.V4(this.value.copy())
        is Variant12.V5 -> Variant12.V5(this.value.copy())
        is Variant12.V6 -> Variant12.V6(this.value.copy())
        is Variant12.V7 -> Variant12.V7(this.value.copy())
        is Variant12.V8 -> Variant12.V8(this.value.copy())
        is Variant12.V9 -> Variant12.V9(this.value.copy())
        is Variant12.V10 -> Variant12.V10(this.value.copy())
        is Variant12.V11 -> Variant12.V11(this.value.copy())
        is Variant12.V12 -> Variant12.V12(this.value.copy())
    }

@JvmName("Variant13Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>> Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>.copy(): Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> =
    when (this) {
        is Variant13.V1 -> Variant13.V1(this.value.copy())
        is Variant13.V2 -> Variant13.V2(this.value.copy())
        is Variant13.V3 -> Variant13.V3(this.value.copy())
        is Variant13.V4 -> Variant13.V4(this.value.copy())
        is Variant13.V5 -> Variant13.V5(this.value.copy())
        is Variant13.V6 -> Variant13.V6(this.value.copy())
        is Variant13.V7 -> Variant13.V7(this.value.copy())
        is Variant13.V8 -> Variant13.V8(this.value.copy())
        is Variant13.V9 -> Variant13.V9(this.value.copy())
        is Variant13.V10 -> Variant13.V10(this.value.copy())
        is Variant13.V11 -> Variant13.V11(this.value.copy())
        is Variant13.V12 -> Variant13.V12(this.value.copy())
        is Variant13.V13 -> Variant13.V13(this.value.copy())
    }

@JvmName("Variant14Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>> Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>.copy(): Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> =
    when (this) {
        is Variant14.V1 -> Variant14.V1(this.value.copy())
        is Variant14.V2 -> Variant14.V2(this.value.copy())
        is Variant14.V3 -> Variant14.V3(this.value.copy())
        is Variant14.V4 -> Variant14.V4(this.value.copy())
        is Variant14.V5 -> Variant14.V5(this.value.copy())
        is Variant14.V6 -> Variant14.V6(this.value.copy())
        is Variant14.V7 -> Variant14.V7(this.value.copy())
        is Variant14.V8 -> Variant14.V8(this.value.copy())
        is Variant14.V9 -> Variant14.V9(this.value.copy())
        is Variant14.V10 -> Variant14.V10(this.value.copy())
        is Variant14.V11 -> Variant14.V11(this.value.copy())
        is Variant14.V12 -> Variant14.V12(this.value.copy())
        is Variant14.V13 -> Variant14.V13(this.value.copy())
        is Variant14.V14 -> Variant14.V14(this.value.copy())
    }

@JvmName("Variant15Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>> Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>.copy(): Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> =
    when (this) {
        is Variant15.V1 -> Variant15.V1(this.value.copy())
        is Variant15.V2 -> Variant15.V2(this.value.copy())
        is Variant15.V3 -> Variant15.V3(this.value.copy())
        is Variant15.V4 -> Variant15.V4(this.value.copy())
        is Variant15.V5 -> Variant15.V5(this.value.copy())
        is Variant15.V6 -> Variant15.V6(this.value.copy())
        is Variant15.V7 -> Variant15.V7(this.value.copy())
        is Variant15.V8 -> Variant15.V8(this.value.copy())
        is Variant15.V9 -> Variant15.V9(this.value.copy())
        is Variant15.V10 -> Variant15.V10(this.value.copy())
        is Variant15.V11 -> Variant15.V11(this.value.copy())
        is Variant15.V12 -> Variant15.V12(this.value.copy())
        is Variant15.V13 -> Variant15.V13(this.value.copy())
        is Variant15.V14 -> Variant15.V14(this.value.copy())
        is Variant15.V15 -> Variant15.V15(this.value.copy())
    }

@JvmName("Variant16Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>, T16 : Copyable<T16>> Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>.copy(): Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> =
    when (this) {
        is Variant16.V1 -> Variant16.V1(this.value.copy())
        is Variant16.V2 -> Variant16.V2(this.value.copy())
        is Variant16.V3 -> Variant16.V3(this.value.copy())
        is Variant16.V4 -> Variant16.V4(this.value.copy())
        is Variant16.V5 -> Variant16.V5(this.value.copy())
        is Variant16.V6 -> Variant16.V6(this.value.copy())
        is Variant16.V7 -> Variant16.V7(this.value.copy())
        is Variant16.V8 -> Variant16.V8(this.value.copy())
        is Variant16.V9 -> Variant16.V9(this.value.copy())
        is Variant16.V10 -> Variant16.V10(this.value.copy())
        is Variant16.V11 -> Variant16.V11(this.value.copy())
        is Variant16.V12 -> Variant16.V12(this.value.copy())
        is Variant16.V13 -> Variant16.V13(this.value.copy())
        is Variant16.V14 -> Variant16.V14(this.value.copy())
        is Variant16.V15 -> Variant16.V15(this.value.copy())
        is Variant16.V16 -> Variant16.V16(this.value.copy())
    }

@JvmName("Variant17Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>, T16 : Copyable<T16>, T17 : Copyable<T17>> Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>.copy(): Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> =
    when (this) {
        is Variant17.V1 -> Variant17.V1(this.value.copy())
        is Variant17.V2 -> Variant17.V2(this.value.copy())
        is Variant17.V3 -> Variant17.V3(this.value.copy())
        is Variant17.V4 -> Variant17.V4(this.value.copy())
        is Variant17.V5 -> Variant17.V5(this.value.copy())
        is Variant17.V6 -> Variant17.V6(this.value.copy())
        is Variant17.V7 -> Variant17.V7(this.value.copy())
        is Variant17.V8 -> Variant17.V8(this.value.copy())
        is Variant17.V9 -> Variant17.V9(this.value.copy())
        is Variant17.V10 -> Variant17.V10(this.value.copy())
        is Variant17.V11 -> Variant17.V11(this.value.copy())
        is Variant17.V12 -> Variant17.V12(this.value.copy())
        is Variant17.V13 -> Variant17.V13(this.value.copy())
        is Variant17.V14 -> Variant17.V14(this.value.copy())
        is Variant17.V15 -> Variant17.V15(this.value.copy())
        is Variant17.V16 -> Variant17.V16(this.value.copy())
        is Variant17.V17 -> Variant17.V17(this.value.copy())
    }

@JvmName("Variant18Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>, T16 : Copyable<T16>, T17 : Copyable<T17>, T18 : Copyable<T18>> Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>.copy(): Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> =
    when (this) {
        is Variant18.V1 -> Variant18.V1(this.value.copy())
        is Variant18.V2 -> Variant18.V2(this.value.copy())
        is Variant18.V3 -> Variant18.V3(this.value.copy())
        is Variant18.V4 -> Variant18.V4(this.value.copy())
        is Variant18.V5 -> Variant18.V5(this.value.copy())
        is Variant18.V6 -> Variant18.V6(this.value.copy())
        is Variant18.V7 -> Variant18.V7(this.value.copy())
        is Variant18.V8 -> Variant18.V8(this.value.copy())
        is Variant18.V9 -> Variant18.V9(this.value.copy())
        is Variant18.V10 -> Variant18.V10(this.value.copy())
        is Variant18.V11 -> Variant18.V11(this.value.copy())
        is Variant18.V12 -> Variant18.V12(this.value.copy())
        is Variant18.V13 -> Variant18.V13(this.value.copy())
        is Variant18.V14 -> Variant18.V14(this.value.copy())
        is Variant18.V15 -> Variant18.V15(this.value.copy())
        is Variant18.V16 -> Variant18.V16(this.value.copy())
        is Variant18.V17 -> Variant18.V17(this.value.copy())
        is Variant18.V18 -> Variant18.V18(this.value.copy())
    }

@JvmName("Variant19Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>, T16 : Copyable<T16>, T17 : Copyable<T17>, T18 : Copyable<T18>, T19 : Copyable<T19>> Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>.copy(): Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> =
    when (this) {
        is Variant19.V1 -> Variant19.V1(this.value.copy())
        is Variant19.V2 -> Variant19.V2(this.value.copy())
        is Variant19.V3 -> Variant19.V3(this.value.copy())
        is Variant19.V4 -> Variant19.V4(this.value.copy())
        is Variant19.V5 -> Variant19.V5(this.value.copy())
        is Variant19.V6 -> Variant19.V6(this.value.copy())
        is Variant19.V7 -> Variant19.V7(this.value.copy())
        is Variant19.V8 -> Variant19.V8(this.value.copy())
        is Variant19.V9 -> Variant19.V9(this.value.copy())
        is Variant19.V10 -> Variant19.V10(this.value.copy())
        is Variant19.V11 -> Variant19.V11(this.value.copy())
        is Variant19.V12 -> Variant19.V12(this.value.copy())
        is Variant19.V13 -> Variant19.V13(this.value.copy())
        is Variant19.V14 -> Variant19.V14(this.value.copy())
        is Variant19.V15 -> Variant19.V15(this.value.copy())
        is Variant19.V16 -> Variant19.V16(this.value.copy())
        is Variant19.V17 -> Variant19.V17(this.value.copy())
        is Variant19.V18 -> Variant19.V18(this.value.copy())
        is Variant19.V19 -> Variant19.V19(this.value.copy())
    }

@JvmName("Variant20Copy")
fun <T1 : Copyable<T1>, T2 : Copyable<T2>, T3 : Copyable<T3>, T4 : Copyable<T4>, T5 : Copyable<T5>, T6 : Copyable<T6>, T7 : Copyable<T7>, T8 : Copyable<T8>, T9 : Copyable<T9>, T10 : Copyable<T10>, T11 : Copyable<T11>, T12 : Copyable<T12>, T13 : Copyable<T13>, T14 : Copyable<T14>, T15 : Copyable<T15>, T16 : Copyable<T16>, T17 : Copyable<T17>, T18 : Copyable<T18>, T19 : Copyable<T19>, T20 : Copyable<T20>> Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>.copy(): Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> =
    when (this) {
        is Variant20.V1 -> Variant20.V1(this.value.copy())
        is Variant20.V2 -> Variant20.V2(this.value.copy())
        is Variant20.V3 -> Variant20.V3(this.value.copy())
        is Variant20.V4 -> Variant20.V4(this.value.copy())
        is Variant20.V5 -> Variant20.V5(this.value.copy())
        is Variant20.V6 -> Variant20.V6(this.value.copy())
        is Variant20.V7 -> Variant20.V7(this.value.copy())
        is Variant20.V8 -> Variant20.V8(this.value.copy())
        is Variant20.V9 -> Variant20.V9(this.value.copy())
        is Variant20.V10 -> Variant20.V10(this.value.copy())
        is Variant20.V11 -> Variant20.V11(this.value.copy())
        is Variant20.V12 -> Variant20.V12(this.value.copy())
        is Variant20.V13 -> Variant20.V13(this.value.copy())
        is Variant20.V14 -> Variant20.V14(this.value.copy())
        is Variant20.V15 -> Variant20.V15(this.value.copy())
        is Variant20.V16 -> Variant20.V16(this.value.copy())
        is Variant20.V17 -> Variant20.V17(this.value.copy())
        is Variant20.V18 -> Variant20.V18(this.value.copy())
        is Variant20.V19 -> Variant20.V19(this.value.copy())
        is Variant20.V20 -> Variant20.V20(this.value.copy())
    }

@JvmName("Variant2Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>> Variant2<T1, T2>.move(): Variant2<T1, T2> = when (this) {
    is Variant2.V1 -> Variant2.V1(this.value.move())
    is Variant2.V2 -> Variant2.V2(this.value.move())
}

@JvmName("Variant3Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>> Variant3<T1, T2, T3>.move(): Variant3<T1, T2, T3> =
    when (this) {
        is Variant3.V1 -> Variant3.V1(this.value.move())
        is Variant3.V2 -> Variant3.V2(this.value.move())
        is Variant3.V3 -> Variant3.V3(this.value.move())
    }

@JvmName("Variant4Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>> Variant4<T1, T2, T3, T4>.move(): Variant4<T1, T2, T3, T4> =
    when (this) {
        is Variant4.V1 -> Variant4.V1(this.value.move())
        is Variant4.V2 -> Variant4.V2(this.value.move())
        is Variant4.V3 -> Variant4.V3(this.value.move())
        is Variant4.V4 -> Variant4.V4(this.value.move())
    }

@JvmName("Variant5Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>> Variant5<T1, T2, T3, T4, T5>.move(): Variant5<T1, T2, T3, T4, T5> =
    when (this) {
        is Variant5.V1 -> Variant5.V1(this.value.move())
        is Variant5.V2 -> Variant5.V2(this.value.move())
        is Variant5.V3 -> Variant5.V3(this.value.move())
        is Variant5.V4 -> Variant5.V4(this.value.move())
        is Variant5.V5 -> Variant5.V5(this.value.move())
    }

@JvmName("Variant6Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>> Variant6<T1, T2, T3, T4, T5, T6>.move(): Variant6<T1, T2, T3, T4, T5, T6> =
    when (this) {
        is Variant6.V1 -> Variant6.V1(this.value.move())
        is Variant6.V2 -> Variant6.V2(this.value.move())
        is Variant6.V3 -> Variant6.V3(this.value.move())
        is Variant6.V4 -> Variant6.V4(this.value.move())
        is Variant6.V5 -> Variant6.V5(this.value.move())
        is Variant6.V6 -> Variant6.V6(this.value.move())
    }

@JvmName("Variant7Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>> Variant7<T1, T2, T3, T4, T5, T6, T7>.move(): Variant7<T1, T2, T3, T4, T5, T6, T7> =
    when (this) {
        is Variant7.V1 -> Variant7.V1(this.value.move())
        is Variant7.V2 -> Variant7.V2(this.value.move())
        is Variant7.V3 -> Variant7.V3(this.value.move())
        is Variant7.V4 -> Variant7.V4(this.value.move())
        is Variant7.V5 -> Variant7.V5(this.value.move())
        is Variant7.V6 -> Variant7.V6(this.value.move())
        is Variant7.V7 -> Variant7.V7(this.value.move())
    }

@JvmName("Variant8Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>> Variant8<T1, T2, T3, T4, T5, T6, T7, T8>.move(): Variant8<T1, T2, T3, T4, T5, T6, T7, T8> =
    when (this) {
        is Variant8.V1 -> Variant8.V1(this.value.move())
        is Variant8.V2 -> Variant8.V2(this.value.move())
        is Variant8.V3 -> Variant8.V3(this.value.move())
        is Variant8.V4 -> Variant8.V4(this.value.move())
        is Variant8.V5 -> Variant8.V5(this.value.move())
        is Variant8.V6 -> Variant8.V6(this.value.move())
        is Variant8.V7 -> Variant8.V7(this.value.move())
        is Variant8.V8 -> Variant8.V8(this.value.move())
    }

@JvmName("Variant9Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>> Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>.move(): Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9> =
    when (this) {
        is Variant9.V1 -> Variant9.V1(this.value.move())
        is Variant9.V2 -> Variant9.V2(this.value.move())
        is Variant9.V3 -> Variant9.V3(this.value.move())
        is Variant9.V4 -> Variant9.V4(this.value.move())
        is Variant9.V5 -> Variant9.V5(this.value.move())
        is Variant9.V6 -> Variant9.V6(this.value.move())
        is Variant9.V7 -> Variant9.V7(this.value.move())
        is Variant9.V8 -> Variant9.V8(this.value.move())
        is Variant9.V9 -> Variant9.V9(this.value.move())
    }

@JvmName("Variant10Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>> Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>.move(): Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> =
    when (this) {
        is Variant10.V1 -> Variant10.V1(this.value.move())
        is Variant10.V2 -> Variant10.V2(this.value.move())
        is Variant10.V3 -> Variant10.V3(this.value.move())
        is Variant10.V4 -> Variant10.V4(this.value.move())
        is Variant10.V5 -> Variant10.V5(this.value.move())
        is Variant10.V6 -> Variant10.V6(this.value.move())
        is Variant10.V7 -> Variant10.V7(this.value.move())
        is Variant10.V8 -> Variant10.V8(this.value.move())
        is Variant10.V9 -> Variant10.V9(this.value.move())
        is Variant10.V10 -> Variant10.V10(this.value.move())
    }

@JvmName("Variant11Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>> Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>.move(): Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> =
    when (this) {
        is Variant11.V1 -> Variant11.V1(this.value.move())
        is Variant11.V2 -> Variant11.V2(this.value.move())
        is Variant11.V3 -> Variant11.V3(this.value.move())
        is Variant11.V4 -> Variant11.V4(this.value.move())
        is Variant11.V5 -> Variant11.V5(this.value.move())
        is Variant11.V6 -> Variant11.V6(this.value.move())
        is Variant11.V7 -> Variant11.V7(this.value.move())
        is Variant11.V8 -> Variant11.V8(this.value.move())
        is Variant11.V9 -> Variant11.V9(this.value.move())
        is Variant11.V10 -> Variant11.V10(this.value.move())
        is Variant11.V11 -> Variant11.V11(this.value.move())
    }

@JvmName("Variant12Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>> Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>.move(): Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> =
    when (this) {
        is Variant12.V1 -> Variant12.V1(this.value.move())
        is Variant12.V2 -> Variant12.V2(this.value.move())
        is Variant12.V3 -> Variant12.V3(this.value.move())
        is Variant12.V4 -> Variant12.V4(this.value.move())
        is Variant12.V5 -> Variant12.V5(this.value.move())
        is Variant12.V6 -> Variant12.V6(this.value.move())
        is Variant12.V7 -> Variant12.V7(this.value.move())
        is Variant12.V8 -> Variant12.V8(this.value.move())
        is Variant12.V9 -> Variant12.V9(this.value.move())
        is Variant12.V10 -> Variant12.V10(this.value.move())
        is Variant12.V11 -> Variant12.V11(this.value.move())
        is Variant12.V12 -> Variant12.V12(this.value.move())
    }

@JvmName("Variant13Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>> Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>.move(): Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> =
    when (this) {
        is Variant13.V1 -> Variant13.V1(this.value.move())
        is Variant13.V2 -> Variant13.V2(this.value.move())
        is Variant13.V3 -> Variant13.V3(this.value.move())
        is Variant13.V4 -> Variant13.V4(this.value.move())
        is Variant13.V5 -> Variant13.V5(this.value.move())
        is Variant13.V6 -> Variant13.V6(this.value.move())
        is Variant13.V7 -> Variant13.V7(this.value.move())
        is Variant13.V8 -> Variant13.V8(this.value.move())
        is Variant13.V9 -> Variant13.V9(this.value.move())
        is Variant13.V10 -> Variant13.V10(this.value.move())
        is Variant13.V11 -> Variant13.V11(this.value.move())
        is Variant13.V12 -> Variant13.V12(this.value.move())
        is Variant13.V13 -> Variant13.V13(this.value.move())
    }

@JvmName("Variant14Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>> Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>.move(): Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> =
    when (this) {
        is Variant14.V1 -> Variant14.V1(this.value.move())
        is Variant14.V2 -> Variant14.V2(this.value.move())
        is Variant14.V3 -> Variant14.V3(this.value.move())
        is Variant14.V4 -> Variant14.V4(this.value.move())
        is Variant14.V5 -> Variant14.V5(this.value.move())
        is Variant14.V6 -> Variant14.V6(this.value.move())
        is Variant14.V7 -> Variant14.V7(this.value.move())
        is Variant14.V8 -> Variant14.V8(this.value.move())
        is Variant14.V9 -> Variant14.V9(this.value.move())
        is Variant14.V10 -> Variant14.V10(this.value.move())
        is Variant14.V11 -> Variant14.V11(this.value.move())
        is Variant14.V12 -> Variant14.V12(this.value.move())
        is Variant14.V13 -> Variant14.V13(this.value.move())
        is Variant14.V14 -> Variant14.V14(this.value.move())
    }

@JvmName("Variant15Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>> Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>.move(): Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> =
    when (this) {
        is Variant15.V1 -> Variant15.V1(this.value.move())
        is Variant15.V2 -> Variant15.V2(this.value.move())
        is Variant15.V3 -> Variant15.V3(this.value.move())
        is Variant15.V4 -> Variant15.V4(this.value.move())
        is Variant15.V5 -> Variant15.V5(this.value.move())
        is Variant15.V6 -> Variant15.V6(this.value.move())
        is Variant15.V7 -> Variant15.V7(this.value.move())
        is Variant15.V8 -> Variant15.V8(this.value.move())
        is Variant15.V9 -> Variant15.V9(this.value.move())
        is Variant15.V10 -> Variant15.V10(this.value.move())
        is Variant15.V11 -> Variant15.V11(this.value.move())
        is Variant15.V12 -> Variant15.V12(this.value.move())
        is Variant15.V13 -> Variant15.V13(this.value.move())
        is Variant15.V14 -> Variant15.V14(this.value.move())
        is Variant15.V15 -> Variant15.V15(this.value.move())
    }

@JvmName("Variant16Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>, T16 : Movable<T16>> Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>.move(): Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> =
    when (this) {
        is Variant16.V1 -> Variant16.V1(this.value.move())
        is Variant16.V2 -> Variant16.V2(this.value.move())
        is Variant16.V3 -> Variant16.V3(this.value.move())
        is Variant16.V4 -> Variant16.V4(this.value.move())
        is Variant16.V5 -> Variant16.V5(this.value.move())
        is Variant16.V6 -> Variant16.V6(this.value.move())
        is Variant16.V7 -> Variant16.V7(this.value.move())
        is Variant16.V8 -> Variant16.V8(this.value.move())
        is Variant16.V9 -> Variant16.V9(this.value.move())
        is Variant16.V10 -> Variant16.V10(this.value.move())
        is Variant16.V11 -> Variant16.V11(this.value.move())
        is Variant16.V12 -> Variant16.V12(this.value.move())
        is Variant16.V13 -> Variant16.V13(this.value.move())
        is Variant16.V14 -> Variant16.V14(this.value.move())
        is Variant16.V15 -> Variant16.V15(this.value.move())
        is Variant16.V16 -> Variant16.V16(this.value.move())
    }

@JvmName("Variant17Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>, T16 : Movable<T16>, T17 : Movable<T17>> Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>.move(): Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17> =
    when (this) {
        is Variant17.V1 -> Variant17.V1(this.value.move())
        is Variant17.V2 -> Variant17.V2(this.value.move())
        is Variant17.V3 -> Variant17.V3(this.value.move())
        is Variant17.V4 -> Variant17.V4(this.value.move())
        is Variant17.V5 -> Variant17.V5(this.value.move())
        is Variant17.V6 -> Variant17.V6(this.value.move())
        is Variant17.V7 -> Variant17.V7(this.value.move())
        is Variant17.V8 -> Variant17.V8(this.value.move())
        is Variant17.V9 -> Variant17.V9(this.value.move())
        is Variant17.V10 -> Variant17.V10(this.value.move())
        is Variant17.V11 -> Variant17.V11(this.value.move())
        is Variant17.V12 -> Variant17.V12(this.value.move())
        is Variant17.V13 -> Variant17.V13(this.value.move())
        is Variant17.V14 -> Variant17.V14(this.value.move())
        is Variant17.V15 -> Variant17.V15(this.value.move())
        is Variant17.V16 -> Variant17.V16(this.value.move())
        is Variant17.V17 -> Variant17.V17(this.value.move())
    }

@JvmName("Variant18Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>, T16 : Movable<T16>, T17 : Movable<T17>, T18 : Movable<T18>> Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>.move(): Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18> =
    when (this) {
        is Variant18.V1 -> Variant18.V1(this.value.move())
        is Variant18.V2 -> Variant18.V2(this.value.move())
        is Variant18.V3 -> Variant18.V3(this.value.move())
        is Variant18.V4 -> Variant18.V4(this.value.move())
        is Variant18.V5 -> Variant18.V5(this.value.move())
        is Variant18.V6 -> Variant18.V6(this.value.move())
        is Variant18.V7 -> Variant18.V7(this.value.move())
        is Variant18.V8 -> Variant18.V8(this.value.move())
        is Variant18.V9 -> Variant18.V9(this.value.move())
        is Variant18.V10 -> Variant18.V10(this.value.move())
        is Variant18.V11 -> Variant18.V11(this.value.move())
        is Variant18.V12 -> Variant18.V12(this.value.move())
        is Variant18.V13 -> Variant18.V13(this.value.move())
        is Variant18.V14 -> Variant18.V14(this.value.move())
        is Variant18.V15 -> Variant18.V15(this.value.move())
        is Variant18.V16 -> Variant18.V16(this.value.move())
        is Variant18.V17 -> Variant18.V17(this.value.move())
        is Variant18.V18 -> Variant18.V18(this.value.move())
    }

@JvmName("Variant19Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>, T16 : Movable<T16>, T17 : Movable<T17>, T18 : Movable<T18>, T19 : Movable<T19>> Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>.move(): Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19> =
    when (this) {
        is Variant19.V1 -> Variant19.V1(this.value.move())
        is Variant19.V2 -> Variant19.V2(this.value.move())
        is Variant19.V3 -> Variant19.V3(this.value.move())
        is Variant19.V4 -> Variant19.V4(this.value.move())
        is Variant19.V5 -> Variant19.V5(this.value.move())
        is Variant19.V6 -> Variant19.V6(this.value.move())
        is Variant19.V7 -> Variant19.V7(this.value.move())
        is Variant19.V8 -> Variant19.V8(this.value.move())
        is Variant19.V9 -> Variant19.V9(this.value.move())
        is Variant19.V10 -> Variant19.V10(this.value.move())
        is Variant19.V11 -> Variant19.V11(this.value.move())
        is Variant19.V12 -> Variant19.V12(this.value.move())
        is Variant19.V13 -> Variant19.V13(this.value.move())
        is Variant19.V14 -> Variant19.V14(this.value.move())
        is Variant19.V15 -> Variant19.V15(this.value.move())
        is Variant19.V16 -> Variant19.V16(this.value.move())
        is Variant19.V17 -> Variant19.V17(this.value.move())
        is Variant19.V18 -> Variant19.V18(this.value.move())
        is Variant19.V19 -> Variant19.V19(this.value.move())
    }

@JvmName("Variant20Move")
fun <T1 : Movable<T1>, T2 : Movable<T2>, T3 : Movable<T3>, T4 : Movable<T4>, T5 : Movable<T5>, T6 : Movable<T6>, T7 : Movable<T7>, T8 : Movable<T8>, T9 : Movable<T9>, T10 : Movable<T10>, T11 : Movable<T11>, T12 : Movable<T12>, T13 : Movable<T13>, T14 : Movable<T14>, T15 : Movable<T15>, T16 : Movable<T16>, T17 : Movable<T17>, T18 : Movable<T18>, T19 : Movable<T19>, T20 : Movable<T20>> Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>.move(): Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20> =
    when (this) {
        is Variant20.V1 -> Variant20.V1(this.value.move())
        is Variant20.V2 -> Variant20.V2(this.value.move())
        is Variant20.V3 -> Variant20.V3(this.value.move())
        is Variant20.V4 -> Variant20.V4(this.value.move())
        is Variant20.V5 -> Variant20.V5(this.value.move())
        is Variant20.V6 -> Variant20.V6(this.value.move())
        is Variant20.V7 -> Variant20.V7(this.value.move())
        is Variant20.V8 -> Variant20.V8(this.value.move())
        is Variant20.V9 -> Variant20.V9(this.value.move())
        is Variant20.V10 -> Variant20.V10(this.value.move())
        is Variant20.V11 -> Variant20.V11(this.value.move())
        is Variant20.V12 -> Variant20.V12(this.value.move())
        is Variant20.V13 -> Variant20.V13(this.value.move())
        is Variant20.V14 -> Variant20.V14(this.value.move())
        is Variant20.V15 -> Variant20.V15(this.value.move())
        is Variant20.V16 -> Variant20.V16(this.value.move())
        is Variant20.V17 -> Variant20.V17(this.value.move())
        is Variant20.V18 -> Variant20.V18(this.value.move())
        is Variant20.V19 -> Variant20.V19(this.value.move())
        is Variant20.V20 -> Variant20.V20(this.value.move())
    }
