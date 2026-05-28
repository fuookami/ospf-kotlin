/**
 * 变体类型
 *
 * Variant types representing a value that can be one of multiple types.
 * Provides Variant2 through Variant10 for representing 2-10 possible types.
 * Similar to sealed classes but with type-safe extraction and pattern matching.
 * 变体类型，表示可以是多种类型之一的值。
 * 提供 Variant2 到 Variant10，用于表示 2-10 种可能的类型。
 * 类似于密封类，但提供类型安全的提取和模式匹配。
 *
 * Each VariantN contains:
 * - VN data classes for each variant type
 * - isN properties for checking the variant type
 * - vN properties for safe value extraction (returns null if not the expected type)
 * - ifN functions for pattern matching
 * - VariantNMatcher classes for fluent pattern matching API
 *
 * 每个 VariantN 包含：
 * - VN 数据类表示每种变体类型
 * - isN 属性用于检查变体类型
 * - vN 属性用于安全提取值（如果不是预期类型则返回 null）
 * - ifN 函数用于模式匹配
 * - VariantNMatcher 类用于流式模式匹配 API
 */
package fuookami.ospf.kotlin.utils.functional

import kotlin.reflect.KClass
import fuookami.ospf.kotlin.utils.concept.*

/**
 * 二元变体类型
 *
 * Sealed class representing a value that can be either type T1 or T2.
 * 密封类，表示可以是类型 T1 或 T2 的值。
 *
 * @param T1 第一种可能类型的类型 / The type of the first possible type
 * @param T2 第二种可能类型的类型 / The type of the second possible type
 */
sealed class Variant2<T1, T2>() {
    /**
     * V1 子类 - 第一种类型的变体
     *
     * Represents a value of type T1.
     * 表示类型 T1 的值。
     *
     * @param value 携带的 T1 类型值 / The carried value of type T1
     */
    data class V1<T1, T2>(val value: T1) : Variant2<T1, T2>() {}

    /**
     * V2 子类 - 第二种类型的变体
     *
     * Represents a value of type T2.
     * 表示类型 T2 的值。
     *
     * @param value 携带的 T2 类型值 / The carried value of type T2
     */
    data class V2<T1, T2>(val value: T2) : Variant2<T1, T2>() {}

    /**
     * 是否为 V1 类型
     *
     * Returns true if this is a V1 value.
     * 如果是 V1 值则返回 true。
     */
    val is1 get() = this is V1

    /**
     * 获取 V1 值（如果存在）
     *
     * Returns the T1 value if this is V1, otherwise null.
     * 如果是 V1 则返回 T1 值，否则返回 null。
     */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /**
     * 如果是 V1 则创建匹配器
     *
     * Creates a matcher with a callback for the V1 case.
     * 为 V1 情况创建带有回调的匹配器。
     *
     * @param Ret 返回值类型 / The return type
     * @param callBack V1 值的处理函数 / The handler function for V1 value
     * @return Variant2 匹配器 / A Variant2 matcher
     */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant2Matcher<T1, T2, Ret>(this).if1(callBack)

    /**
     * 是否为 V2 类型
     *
     * Returns true if this is a V2 value.
     * 如果是 V2 值则返回 true。
     */
    val is2 get() = this is V2

    /**
     * 获取 V2 值（如果存在）
     *
     * Returns the T2 value if this is V2, otherwise null.
     * 如果是 V2 则返回 T2 值，否则返回 null。
     */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /**
     * 如果是 V2 则创建匹配器
     *
     * Creates a matcher with a callback for the V2 case.
     * 为 V2 情况创建带有回调的匹配器。
     *
     * @param Ret 返回值类型 / The return type
     * @param callBack V2 值的处理函数 / The handler function for V2 value
     * @return Variant2 匹配器 / A Variant2 matcher
     */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant2Matcher<T1, T2, Ret>(this).if2(callBack)

}

/**
 * 二元变体匹配器
 *
 * Matcher class for pattern matching on Variant2 values with fluent API.
 * 用于 Variant2 值模式匹配的匹配器类，提供流式 API。
 *
 * @param T1 第一种可能类型的类型 / The type of the first possible type
 * @param T2 第二种可能类型的类型 / The type of the second possible type
 * @param Ret 返回值类型 / The return type
 * @param value 要匹配的 Variant2 值 / The Variant2 value to match
 */
data class Variant2Matcher<T1, T2, Ret>(private val value: Variant2<T1, T2>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret

    /**
     * 设置 V1 分支的回调
     *
     * Sets the callback for the V1 branch.
     * 设置 V1 分支的回调函数。
     *
     * @param callBack T1 值的处理函数 / The handler function for T1 value
     * @return 匹配器本身 / The matcher itself
     */
    fun if1(callBack: (T1) -> Ret): Variant2Matcher<T1, T2, Ret> {
        callBack1 = callBack
        return this
    }

    /**
     * 设置 V2 分支的回调
     *
     * Sets the callback for the V2 branch.
     * 设置 V2 分支的回调函数。
     *
     * @param callBack T2 值的处理函数 / The handler function for T2 value
     * @return 匹配器本身 / The matcher itself
     */
    fun if2(callBack: (T2) -> Ret): Variant2Matcher<T1, T2, Ret> {
        callBack2 = callBack
        return this
    }

    /**
     * 执行匹配并返回结果
     *
     * Executes the matching and returns the result based on which variant is present.
     * 执行匹配并根据存在的变体返回结果。
     *
     * @return 匹配结果 / The matching result
     * @throws NullPointerException 如果未设置相应的回调 / If the corresponding callback is not set
     */
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

/**
 * 三元变体类型
 *
 * Sealed class representing a value that can be one of three types: T1, T2, or T3.
 * 密封类，表示可以是三种类型 T1、T2 或 T3 之一的值。
 *
 * @param T1 第一种可能类型的类型 / The type of the first possible type
 * @param T2 第二种可能类型的类型 / The type of the second possible type
 * @param T3 第三种可能类型的类型 / The type of the third possible type
 */
sealed class Variant3<T1, T2, T3>() {
    /**
     * V1 子类 - 第一种类型的变体
     *
     * Represents a value of type T1.
     * 表示类型 T1 的值。
     */
    data class V1<T1, T2, T3>(val value: T1) : Variant3<T1, T2, T3>() {}
    /**
     * V2 子类 - 第二种类型的变体
     *
     * Represents a value of type T2.
     * 表示类型 T2 的值。
     */
    data class V2<T1, T2, T3>(val value: T2) : Variant3<T1, T2, T3>() {}
    /**
     * V3 子类 - 第三种类型的变体
     *
     * Represents a value of type T3.
     * 表示类型 T3 的值。
     */
    data class V3<T1, T2, T3>(val value: T3) : Variant3<T1, T2, T3>() {}

    /** 是否为 V1 类型 / Returns true if this is a V1 value */
    val is1 get() = this is V1

    /** 获取 V1 值（如果存在）/ Returns the T1 value if this is V1, otherwise null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 如果是 V1 则创建匹配器 / Creates a matcher for V1 case */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if1(callBack)

    /** 是否为 V2 类型 / Returns true if this is a V2 value */
    val is2 get() = this is V2

    /** 获取 V2 值（如果存在）/ Returns the T2 value if this is V2, otherwise null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 如果是 V2 则创建匹配器 / Creates a matcher for V2 case */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if2(callBack)

    /** 是否为 V3 类型 / Returns true if this is a V3 value */
    val is3 get() = this is V3

    /** 获取 V3 值（如果存在）/ Returns the T3 value if this is V3, otherwise null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 如果是 V3 则创建匹配器 / Creates a matcher for V3 case */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant3Matcher<T1, T2, T3, Ret>(this).if3(callBack)

}

/**
 * 三元变体匹配器
 *
 * Matcher class for pattern matching on Variant3 values.
 * 用于 Variant3 值模式匹配的匹配器类。
 */
data class Variant3Matcher<T1, T2, T3, Ret>(private val value: Variant3<T1, T2, T3>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun if1(callBack: (T1) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack1 = callBack
        return this
    }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun if2(callBack: (T2) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack2 = callBack
        return this
    }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun if3(callBack: (T3) -> Ret): Variant3Matcher<T1, T2, T3, Ret> {
        callBack3 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Execute match and return result */
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

/**
 * 四元变体类型
 *
 * Sealed class representing a value that can be one of four types.
 * 密封类，表示可以是四种类型之一的值。
 */
sealed class Variant4<T1, T2, T3, T4>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4>(val value: T1) : Variant4<T1, T2, T3, T4>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4>(val value: T2) : Variant4<T1, T2, T3, T4>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4>(val value: T3) : Variant4<T1, T2, T3, T4>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4>(val value: T4) : Variant4<T1, T2, T3, T4>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant4Matcher<T1, T2, T3, T4, Ret>(this).if4(callBack)

}

/**
 * 四元变体匹配器
 *
 * Matcher class for pattern matching on Variant4 values.
 * 用于 Variant4 值模式匹配的匹配器类。
 */
data class Variant4Matcher<T1, T2, T3, T4, Ret>(private val value: Variant4<T1, T2, T3, T4>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant4Matcher<T1, T2, T3, T4, Ret> {
        callBack4 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 五元变体类型
 *
 * Sealed class representing a value that can be one of five types.
 * 密封类，表示可以是五种类型之一的值。
 */
sealed class Variant5<T1, T2, T3, T4, T5>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5>(val value: T1) : Variant5<T1, T2, T3, T4, T5>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5>(val value: T2) : Variant5<T1, T2, T3, T4, T5>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5>(val value: T3) : Variant5<T1, T2, T3, T4, T5>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5>(val value: T4) : Variant5<T1, T2, T3, T4, T5>() {}
    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5>(val value: T5) : Variant5<T1, T2, T3, T4, T5>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) = Variant5Matcher<T1, T2, T3, T4, T5, Ret>(this).if5(callBack)

}

/**
 * 五元变体匹配器
 *
 * Matcher class for pattern matching on Variant5 values.
 * 用于 Variant5 值模式匹配的匹配器类。
 */
data class Variant5Matcher<T1, T2, T3, T4, T5, Ret>(private val value: Variant5<T1, T2, T3, T4, T5>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant5Matcher<T1, T2, T3, T4, T5, Ret> {
        callBack5 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 六元变体类型
 *
 * Sealed class representing a value that can be one of six types.
 * 密封类，表示可以是六种类型之一的值。
 */
sealed class Variant6<T1, T2, T3, T4, T5, T6>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6>(val value: T1) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6>(val value: T2) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6>(val value: T3) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6>(val value: T4) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6>(val value: T5) : Variant6<T1, T2, T3, T4, T5, T6>() {}
    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6>(val value: T6) : Variant6<T1, T2, T3, T4, T5, T6>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) = Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(this).if6(callBack)

}

/**
 * 六元变体匹配器
 *
 * Matcher class for pattern matching on Variant6 values.
 * 用于 Variant6 值模式匹配的匹配器类。
 */
data class Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret>(private val value: Variant6<T1, T2, T3, T4, T5, T6>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant6Matcher<T1, T2, T3, T4, T5, T6, Ret> {
        callBack6 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 七元变体类型
 *
 * Sealed class representing a value that can be one of seven types.
 * 密封类，表示可以是七种类型之一的值。
 */
sealed class Variant7<T1, T2, T3, T4, T5, T6, T7>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7>(val value: T1) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7>(val value: T2) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7>(val value: T3) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7>(val value: T4) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7>(val value: T5) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7>(val value: T6) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}
    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7>(val value: T7) : Variant7<T1, T2, T3, T4, T5, T6, T7>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) = Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(this).if7(callBack)

}

/**
 * 七元变体匹配器
 *
 * Matcher class for pattern matching on Variant7 values.
 * 用于 Variant7 值模式匹配的匹配器类。
 */
data class Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret>(private val value: Variant7<T1, T2, T3, T4, T5, T6, T7>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant7Matcher<T1, T2, T3, T4, T5, T6, T7, Ret> {
        callBack7 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 八元变体类型
 *
 * Sealed class representing a value that can be one of eight types.
 * 密封类，表示可以是八种类型之一的值。
 */
sealed class Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T1) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T2) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T3) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T4) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T5) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T6) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T7) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}
    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8>(val value: T8) : Variant8<T1, T2, T3, T4, T5, T6, T7, T8>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) = Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(this).if8(callBack)

}

/**
 * 八元变体匹配器
 *
 * Matcher class for pattern matching on Variant8 values.
 * 用于 Variant8 值模式匹配的匹配器类。
 */
data class Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret>(private val value: Variant8<T1, T2, T3, T4, T5, T6, T7, T8>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant8Matcher<T1, T2, T3, T4, T5, T6, T7, T8, Ret> {
        callBack8 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 九元变体类型
 *
 * Sealed class representing a value that can be one of nine types.
 * 密封类，表示可以是九种类型之一的值。
 */
sealed class Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T1) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T2) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T3) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T4) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T5) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T6) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T7) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T8) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}
    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9>(val value: T9) : Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) = Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(this).if9(callBack)

}

/**
 * 九元变体匹配器
 *
 * Matcher class for pattern matching on Variant9 values.
 * 用于 Variant9 值模式匹配的匹配器类。
 */
data class Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret>(private val value: Variant9<T1, T2, T3, T4, T5, T6, T7, T8, T9>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant9Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, Ret> {
        callBack9 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十元变体类型
 *
 * Sealed class representing a value that can be one of ten types.
 * 密封类，表示可以是十种类型之一的值。
 */
sealed class Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T1) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T2) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T3) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T4) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T5) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T6) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T7) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T8) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T9) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>(val value: T10) :
        Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(this).if10(callBack)

}

/**
 * 十元变体匹配器
 *
 * Matcher class for pattern matching on Variant10 values.
 * 用于 Variant10 值模式匹配的匹配器类。
 */
data class Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret>(private val value: Variant10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant10Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, Ret> {
        callBack10 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十一元变体类型
 *
 * Sealed class representing a value that can be one of eleven types.
 * 密封类，表示可以是十一种类型之一的值。
 */
sealed class Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T1) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T2) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T3) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T4) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T5) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T6) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T7) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T8) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T9) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T10) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>(val value: T11) :
        Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(this).if11(callBack)

}

/**
 * 十一元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant11.
 * 用于 Variant11 值流式模式匹配的匹配器类。
 */
data class Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret>(private val value: Variant11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant11Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, Ret> {
        callBack11 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十二元变体类型
 *
 * Sealed class representing a value that can be one of twelve types.
 * 密封类，表示可以是十二种类型之一的值。
 */
sealed class Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T1) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T2) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T3) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T4) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T5) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T6) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T7) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T8) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T9) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T10) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T11) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>(val value: T12) :
        Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(this).if12(callBack)

}

/**
 * 十二元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant12.
 * 用于 Variant12 值流式模式匹配的匹配器类。
 */
data class Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret>(private val value: Variant12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant12Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, Ret> {
        callBack12 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十三元变体类型
 *
 * Sealed class representing a value that can be one of thirteen types.
 * 密封类，表示可以是十三种类型之一的值。
 */
sealed class Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T1) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T2) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T3) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T4) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T5) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T6) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T7) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T8) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T9) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T10) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T11) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T12) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>(val value: T13) :
        Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if12(callBack)

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(this).if13(callBack)

}

/**
 * 十三元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant13.
 * 用于 Variant13 值流式模式匹配的匹配器类。
 */
data class Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret>(private val value: Variant13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack12 = callBack
        return this
    }

    /** 设置 V13 分支的回调 / Sets the callback for the V13 branch */
    fun if13(callBack: (T13) -> Ret): Variant13Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, Ret> {
        callBack13 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十四元变体类型
 *
 * Sealed class representing a value that can be one of fourteen types.
 * 密封类，表示可以是十四种类型之一的值。
 */
sealed class Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T1) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T2) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T3) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T4) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T5) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T6) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T7) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T8) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T9) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T10) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T11) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T12) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T13) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>(val value: T14) :
        Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if12(callBack)

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if13(callBack)

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(this).if14(callBack)

}

/**
 * 十四元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant14.
 * 用于 Variant14 值流式模式匹配的匹配器类。
 */
data class Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret>(private val value: Variant14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack12 = callBack
        return this
    }

    /** 设置 V13 分支的回调 / Sets the callback for the V13 branch */
    fun if13(callBack: (T13) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack13 = callBack
        return this
    }

    /** 设置 V14 分支的回调 / Sets the callback for the V14 branch */
    fun if14(callBack: (T14) -> Ret): Variant14Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, Ret> {
        callBack14 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十五元变体类型
 *
 * Sealed class representing a value that can be one of fifteen types.
 * 密封类，表示可以是十五种类型之一的值。
 */
sealed class Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T1) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T2) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T3) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T4) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T5) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T6) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T7) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T8) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T9) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T10) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T11) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T12) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T13) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T14) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>(val value: T15) :
        Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if12(callBack)

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if13(callBack)

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if14(callBack)

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(this).if15(callBack)

}

/**
 * 十五元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant15.
 * 用于 Variant15 值流式模式匹配的匹配器类。
 */
data class Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret>(private val value: Variant15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack12 = callBack
        return this
    }

    /** 设置 V13 分支的回调 / Sets the callback for the V13 branch */
    fun if13(callBack: (T13) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack13 = callBack
        return this
    }

    /** 设置 V14 分支的回调 / Sets the callback for the V14 branch */
    fun if14(callBack: (T14) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack14 = callBack
        return this
    }

    /** 设置 V15 分支的回调 / Sets the callback for the V15 branch */
    fun if15(callBack: (T15) -> Ret): Variant15Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, Ret> {
        callBack15 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十六元变体类型
 *
 * Sealed class representing a value that can be one of sixteen types.
 * 密封类，表示可以是十六种类型之一的值。
 */
sealed class Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T1) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T2) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T3) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T4) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T5) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T6) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T7) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T8) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T9) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T10) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T11) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T12) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T13) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T14) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T15) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** V16 子类 - 第16种类型的变体 / Represents a value of type T16 */
    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>(val value: T16) :
        Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if12(callBack)

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if13(callBack)

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if14(callBack)

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if15(callBack)

    /** 是否为第16种类型 / Checks if this is type T16 */
    val is16 get() = this is V16

    /** 安全提取第16种类型的值 / Safely extracts value of type T16, or null */
    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(this).if16(callBack)

}

/**
 * 十六元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant16.
 * 用于 Variant16 值流式模式匹配的匹配器类。
 */
data class Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret>(private val value: Variant16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret
    private lateinit var callBack16: (T16) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack12 = callBack
        return this
    }

    /** 设置 V13 分支的回调 / Sets the callback for the V13 branch */
    fun if13(callBack: (T13) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack13 = callBack
        return this
    }

    /** 设置 V14 分支的回调 / Sets the callback for the V14 branch */
    fun if14(callBack: (T14) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack14 = callBack
        return this
    }

    /** 设置 V15 分支的回调 / Sets the callback for the V15 branch */
    fun if15(callBack: (T15) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack15 = callBack
        return this
    }

    /** 设置 V16 分支的回调 / Sets the callback for the V16 branch */
    fun if16(callBack: (T16) -> Ret): Variant16Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, Ret> {
        callBack16 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十七元变体类型
 *
 * Sealed class representing a value that can be one of seventeen types.
 * 密封类，表示可以是十七种类型之一的值。
 */
sealed class Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T1) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T2) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T3) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T4) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T5) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T6) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T7) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T8) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T9) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T10) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T11) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T12) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T13) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T14) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T15) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V16 子类 - 第16种类型的变体 / Represents a value of type T16 */
    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T16) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** V17 子类 - 第17种类型的变体 / Represents a value of type T17 */
    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>(val value: T17) :
        Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if1(
            callBack
        )

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if2(
            callBack
        )

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if3(
            callBack
        )

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if4(
            callBack
        )

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if5(
            callBack
        )

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if6(
            callBack
        )

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if7(
            callBack
        )

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if8(
            callBack
        )

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if9(
            callBack
        )

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if10(
            callBack
        )

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if11(
            callBack
        )

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if12(
            callBack
        )

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if13(
            callBack
        )

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if14(
            callBack
        )

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if15(
            callBack
        )

    /** 是否为第16种类型 / Checks if this is type T16 */
    val is16 get() = this is V16

    /** 安全提取第16种类型的值 / Safely extracts value of type T16, or null */
    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if16(
            callBack
        )

    /** 是否为第17种类型 / Checks if this is type T17 */
    val is17 get() = this is V17

    /** 安全提取第17种类型的值 / Safely extracts value of type T17, or null */
    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(this).if17(
            callBack
        )

}

/**
 * 十七元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant17.
 * 用于 Variant17 值流式模式匹配的匹配器类。
 */
data class Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret>(private val value: Variant17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17>) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret
    private lateinit var callBack16: (T16) -> Ret
    private lateinit var callBack17: (T17) -> Ret

    /** 设置 V1 分支的回调 / Sets the callback for the V1 branch */
    fun if1(callBack: (T1) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack1 = callBack
        return this
    }

    /** 设置 V2 分支的回调 / Sets the callback for the V2 branch */
    fun if2(callBack: (T2) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack2 = callBack
        return this
    }

    /** 设置 V3 分支的回调 / Sets the callback for the V3 branch */
    fun if3(callBack: (T3) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack3 = callBack
        return this
    }

    /** 设置 V4 分支的回调 / Sets the callback for the V4 branch */
    fun if4(callBack: (T4) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack4 = callBack
        return this
    }

    /** 设置 V5 分支的回调 / Sets the callback for the V5 branch */
    fun if5(callBack: (T5) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack5 = callBack
        return this
    }

    /** 设置 V6 分支的回调 / Sets the callback for the V6 branch */
    fun if6(callBack: (T6) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack6 = callBack
        return this
    }

    /** 设置 V7 分支的回调 / Sets the callback for the V7 branch */
    fun if7(callBack: (T7) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack7 = callBack
        return this
    }

    /** 设置 V8 分支的回调 / Sets the callback for the V8 branch */
    fun if8(callBack: (T8) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack8 = callBack
        return this
    }

    /** 设置 V9 分支的回调 / Sets the callback for the V9 branch */
    fun if9(callBack: (T9) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack9 = callBack
        return this
    }

    /** 设置 V10 分支的回调 / Sets the callback for the V10 branch */
    fun if10(callBack: (T10) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack10 = callBack
        return this
    }

    /** 设置 V11 分支的回调 / Sets the callback for the V11 branch */
    fun if11(callBack: (T11) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack11 = callBack
        return this
    }

    /** 设置 V12 分支的回调 / Sets the callback for the V12 branch */
    fun if12(callBack: (T12) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack12 = callBack
        return this
    }

    /** 设置 V13 分支的回调 / Sets the callback for the V13 branch */
    fun if13(callBack: (T13) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack13 = callBack
        return this
    }

    /** 设置 V14 分支的回调 / Sets the callback for the V14 branch */
    fun if14(callBack: (T14) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack14 = callBack
        return this
    }

    /** 设置 V15 分支的回调 / Sets the callback for the V15 branch */
    fun if15(callBack: (T15) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack15 = callBack
        return this
    }

    /** 设置 V16 分支的回调 / Sets the callback for the V16 branch */
    fun if16(callBack: (T16) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack16 = callBack
        return this
    }

    /** 设置 V17 分支的回调 / Sets the callback for the V17 branch */
    fun if17(callBack: (T17) -> Ret): Variant17Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, Ret> {
        callBack17 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Executes matching and returns the result */
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

/**
 * 十八元变体类型
 *
 * Sealed class representing a value that can be one of eighteen types.
 * 密封类，表示可以是十八种类型之一的值。
 */
sealed class Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T1) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T2) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T3) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T4) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T5) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T6) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T7) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T8) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T9) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T10) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T11) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T12) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T13) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T14) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T15) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V16 子类 - 第16种类型的变体 / Represents a value of type T16 */
    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T16) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V17 子类 - 第17种类型的变体 / Represents a value of type T17 */
    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T17) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** V18 子类 - 第18种类型的变体 / Represents a value of type T18 */
    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>(val value: T18) :
        Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if1(
            callBack
        )

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if2(
            callBack
        )

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if3(
            callBack
        )

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if4(
            callBack
        )

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if5(
            callBack
        )

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if6(
            callBack
        )

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if7(
            callBack
        )

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if8(
            callBack
        )

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if9(
            callBack
        )

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if10(
            callBack
        )

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if11(
            callBack
        )

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if12(
            callBack
        )

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if13(
            callBack
        )

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if14(
            callBack
        )

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if15(
            callBack
        )

    /** 是否为第16种类型 / Checks if this is type T16 */
    val is16 get() = this is V16

    /** 安全提取第16种类型的值 / Safely extracts value of type T16, or null */
    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if16(
            callBack
        )

    /** 是否为第17种类型 / Checks if this is type T17 */
    val is17 get() = this is V17

    /** 安全提取第17种类型的值 / Safely extracts value of type T17, or null */
    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if17(
            callBack
        )

    /** 是否为第18种类型 / Checks if this is type T18 */
    val is18 get() = this is V18

    /** 安全提取第18种类型的值 / Safely extracts value of type T18, or null */
    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(this).if18(
            callBack
        )

}

/**
 * 十八元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant18.
 * 用于 Variant18 值流式模式匹配的匹配器类。
 */
data class Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret>(
    private val value: Variant18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18>
) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret
    private lateinit var callBack16: (T16) -> Ret
    private lateinit var callBack17: (T17) -> Ret
    private lateinit var callBack18: (T18) -> Ret

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun if1(callBack: (T1) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack1 = callBack
        return this
    }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun if2(callBack: (T2) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack2 = callBack
        return this
    }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun if3(callBack: (T3) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack3 = callBack
        return this
    }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun if4(callBack: (T4) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack4 = callBack
        return this
    }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun if5(callBack: (T5) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack5 = callBack
        return this
    }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun if6(callBack: (T6) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack6 = callBack
        return this
    }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun if7(callBack: (T7) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack7 = callBack
        return this
    }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun if8(callBack: (T8) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack8 = callBack
        return this
    }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun if9(callBack: (T9) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack9 = callBack
        return this
    }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun if10(callBack: (T10) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack10 = callBack
        return this
    }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun if11(callBack: (T11) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack11 = callBack
        return this
    }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun if12(callBack: (T12) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack12 = callBack
        return this
    }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun if13(callBack: (T13) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack13 = callBack
        return this
    }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun if14(callBack: (T14) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack14 = callBack
        return this
    }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun if15(callBack: (T15) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack15 = callBack
        return this
    }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun if16(callBack: (T16) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack16 = callBack
        return this
    }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun if17(callBack: (T17) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack17 = callBack
        return this
    }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun if18(callBack: (T18) -> Ret): Variant18Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, Ret> {
        callBack18 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Execute match and return result */
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

/**
 * 十九元变体类型
 *
 * Sealed class representing a value that can be one of nineteen types.
 * 密封类，表示可以是十九种类型之一的值。
 */
sealed class Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T1) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T2) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T3) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T4) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T5) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T6) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T7) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T8) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T9) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T10) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T11) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T12) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T13) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T14) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T15) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V16 子类 - 第16种类型的变体 / Represents a value of type T16 */
    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T16) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V17 子类 - 第17种类型的变体 / Represents a value of type T17 */
    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T17) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V18 子类 - 第18种类型的变体 / Represents a value of type T18 */
    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T18) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** V19 子类 - 第19种类型的变体 / Represents a value of type T19 */
    data class V19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>(val value: T19) :
        Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if1(
            callBack
        )

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if2(
            callBack
        )

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if3(
            callBack
        )

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if4(
            callBack
        )

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if5(
            callBack
        )

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if6(
            callBack
        )

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if7(
            callBack
        )

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if8(
            callBack
        )

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if9(
            callBack
        )

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if10(
            callBack
        )

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if11(
            callBack
        )

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if12(
            callBack
        )

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if13(
            callBack
        )

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if14(
            callBack
        )

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if15(
            callBack
        )

    /** 是否为第16种类型 / Checks if this is type T16 */
    val is16 get() = this is V16

    /** 安全提取第16种类型的值 / Safely extracts value of type T16, or null */
    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if16(
            callBack
        )

    /** 是否为第17种类型 / Checks if this is type T17 */
    val is17 get() = this is V17

    /** 安全提取第17种类型的值 / Safely extracts value of type T17, or null */
    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if17(
            callBack
        )

    /** 是否为第18种类型 / Checks if this is type T18 */
    val is18 get() = this is V18

    /** 安全提取第18种类型的值 / Safely extracts value of type T18, or null */
    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if18(
            callBack
        )

    /** 是否为第19种类型 / Checks if this is type T19 */
    val is19 get() = this is V19

    /** 安全提取第19种类型的值 / Safely extracts value of type T19, or null */
    val v19
        get() = when (this) {
            is V19 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第19种类型的模式匹配 / Pattern match for type T19 */
    fun <Ret> if19(callBack: (T19) -> Ret) =
        Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(this).if19(
            callBack
        )

}

/**
 * 十九元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant19.
 * 用于 Variant19 值流式模式匹配的匹配器类。
 */
data class Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret>(
    private val value: Variant19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19>
) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret
    private lateinit var callBack16: (T16) -> Ret
    private lateinit var callBack17: (T17) -> Ret
    private lateinit var callBack18: (T18) -> Ret
    private lateinit var callBack19: (T19) -> Ret

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun if1(callBack: (T1) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack1 = callBack
        return this
    }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun if2(callBack: (T2) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack2 = callBack
        return this
    }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun if3(callBack: (T3) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack3 = callBack
        return this
    }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun if4(callBack: (T4) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack4 = callBack
        return this
    }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun if5(callBack: (T5) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack5 = callBack
        return this
    }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun if6(callBack: (T6) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack6 = callBack
        return this
    }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun if7(callBack: (T7) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack7 = callBack
        return this
    }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun if8(callBack: (T8) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack8 = callBack
        return this
    }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun if9(callBack: (T9) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack9 = callBack
        return this
    }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun if10(callBack: (T10) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack10 = callBack
        return this
    }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun if11(callBack: (T11) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack11 = callBack
        return this
    }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun if12(callBack: (T12) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack12 = callBack
        return this
    }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun if13(callBack: (T13) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack13 = callBack
        return this
    }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun if14(callBack: (T14) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack14 = callBack
        return this
    }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun if15(callBack: (T15) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack15 = callBack
        return this
    }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun if16(callBack: (T16) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack16 = callBack
        return this
    }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun if17(callBack: (T17) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack17 = callBack
        return this
    }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun if18(callBack: (T18) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack18 = callBack
        return this
    }

    /** 第19种类型的模式匹配 / Pattern match for type T19 */
    fun if19(callBack: (T19) -> Ret): Variant19Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, Ret> {
        callBack19 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Execute match and return result */
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

/**
 * 二十元变体类型
 *
 * Sealed class representing a value that can be one of twenty types.
 * 密封类，表示可以是二十种类型之一的值。
 */
sealed class Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {
    /** V1 子类 - 第1种类型的变体 / Represents a value of type T1 */
    data class V1<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T1) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V2 子类 - 第2种类型的变体 / Represents a value of type T2 */
    data class V2<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T2) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V3 子类 - 第3种类型的变体 / Represents a value of type T3 */
    data class V3<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T3) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V4 子类 - 第4种类型的变体 / Represents a value of type T4 */
    data class V4<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T4) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V5 子类 - 第5种类型的变体 / Represents a value of type T5 */
    data class V5<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T5) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V6 子类 - 第6种类型的变体 / Represents a value of type T6 */
    data class V6<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T6) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V7 子类 - 第7种类型的变体 / Represents a value of type T7 */
    data class V7<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T7) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V8 子类 - 第8种类型的变体 / Represents a value of type T8 */
    data class V8<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T8) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V9 子类 - 第9种类型的变体 / Represents a value of type T9 */
    data class V9<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T9) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V10 子类 - 第10种类型的变体 / Represents a value of type T10 */
    data class V10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T10) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V11 子类 - 第11种类型的变体 / Represents a value of type T11 */
    data class V11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T11) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V12 子类 - 第12种类型的变体 / Represents a value of type T12 */
    data class V12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T12) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V13 子类 - 第13种类型的变体 / Represents a value of type T13 */
    data class V13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T13) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V14 子类 - 第14种类型的变体 / Represents a value of type T14 */
    data class V14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T14) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V15 子类 - 第15种类型的变体 / Represents a value of type T15 */
    data class V15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T15) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V16 子类 - 第16种类型的变体 / Represents a value of type T16 */
    data class V16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T16) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V17 子类 - 第17种类型的变体 / Represents a value of type T17 */
    data class V17<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T17) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V18 子类 - 第18种类型的变体 / Represents a value of type T18 */
    data class V18<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T18) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V19 子类 - 第19种类型的变体 / Represents a value of type T19 */
    data class V19<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T19) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** V20 子类 - 第20种类型的变体 / Represents a value of type T20 */
    data class V20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>(val value: T20) :
        Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>() {}

    /** 是否为第1种类型 / Checks if this is type T1 */
    val is1 get() = this is V1

    /** 安全提取第1种类型的值 / Safely extracts value of type T1, or null */
    val v1
        get() = when (this) {
            is V1 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun <Ret> if1(callBack: (T1) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if1(callBack)

    /** 是否为第2种类型 / Checks if this is type T2 */
    val is2 get() = this is V2

    /** 安全提取第2种类型的值 / Safely extracts value of type T2, or null */
    val v2
        get() = when (this) {
            is V2 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun <Ret> if2(callBack: (T2) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if2(callBack)

    /** 是否为第3种类型 / Checks if this is type T3 */
    val is3 get() = this is V3

    /** 安全提取第3种类型的值 / Safely extracts value of type T3, or null */
    val v3
        get() = when (this) {
            is V3 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun <Ret> if3(callBack: (T3) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if3(callBack)

    /** 是否为第4种类型 / Checks if this is type T4 */
    val is4 get() = this is V4

    /** 安全提取第4种类型的值 / Safely extracts value of type T4, or null */
    val v4
        get() = when (this) {
            is V4 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun <Ret> if4(callBack: (T4) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if4(callBack)

    /** 是否为第5种类型 / Checks if this is type T5 */
    val is5 get() = this is V5

    /** 安全提取第5种类型的值 / Safely extracts value of type T5, or null */
    val v5
        get() = when (this) {
            is V5 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun <Ret> if5(callBack: (T5) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if5(callBack)

    /** 是否为第6种类型 / Checks if this is type T6 */
    val is6 get() = this is V6

    /** 安全提取第6种类型的值 / Safely extracts value of type T6, or null */
    val v6
        get() = when (this) {
            is V6 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun <Ret> if6(callBack: (T6) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if6(callBack)

    /** 是否为第7种类型 / Checks if this is type T7 */
    val is7 get() = this is V7

    /** 安全提取第7种类型的值 / Safely extracts value of type T7, or null */
    val v7
        get() = when (this) {
            is V7 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun <Ret> if7(callBack: (T7) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if7(callBack)

    /** 是否为第8种类型 / Checks if this is type T8 */
    val is8 get() = this is V8

    /** 安全提取第8种类型的值 / Safely extracts value of type T8, or null */
    val v8
        get() = when (this) {
            is V8 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun <Ret> if8(callBack: (T8) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if8(callBack)

    /** 是否为第9种类型 / Checks if this is type T9 */
    val is9 get() = this is V9

    /** 安全提取第9种类型的值 / Safely extracts value of type T9, or null */
    val v9
        get() = when (this) {
            is V9 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun <Ret> if9(callBack: (T9) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if9(callBack)

    /** 是否为第10种类型 / Checks if this is type T10 */
    val is10 get() = this is V10

    /** 安全提取第10种类型的值 / Safely extracts value of type T10, or null */
    val v10
        get() = when (this) {
            is V10 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun <Ret> if10(callBack: (T10) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if10(callBack)

    /** 是否为第11种类型 / Checks if this is type T11 */
    val is11 get() = this is V11

    /** 安全提取第11种类型的值 / Safely extracts value of type T11, or null */
    val v11
        get() = when (this) {
            is V11 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun <Ret> if11(callBack: (T11) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if11(callBack)

    /** 是否为第12种类型 / Checks if this is type T12 */
    val is12 get() = this is V12

    /** 安全提取第12种类型的值 / Safely extracts value of type T12, or null */
    val v12
        get() = when (this) {
            is V12 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun <Ret> if12(callBack: (T12) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if12(callBack)

    /** 是否为第13种类型 / Checks if this is type T13 */
    val is13 get() = this is V13

    /** 安全提取第13种类型的值 / Safely extracts value of type T13, or null */
    val v13
        get() = when (this) {
            is V13 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun <Ret> if13(callBack: (T13) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if13(callBack)

    /** 是否为第14种类型 / Checks if this is type T14 */
    val is14 get() = this is V14

    /** 安全提取第14种类型的值 / Safely extracts value of type T14, or null */
    val v14
        get() = when (this) {
            is V14 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun <Ret> if14(callBack: (T14) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if14(callBack)

    /** 是否为第15种类型 / Checks if this is type T15 */
    val is15 get() = this is V15

    /** 安全提取第15种类型的值 / Safely extracts value of type T15, or null */
    val v15
        get() = when (this) {
            is V15 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun <Ret> if15(callBack: (T15) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if15(callBack)

    /** 是否为第16种类型 / Checks if this is type T16 */
    val is16 get() = this is V16

    /** 安全提取第16种类型的值 / Safely extracts value of type T16, or null */
    val v16
        get() = when (this) {
            is V16 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun <Ret> if16(callBack: (T16) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if16(callBack)

    /** 是否为第17种类型 / Checks if this is type T17 */
    val is17 get() = this is V17

    /** 安全提取第17种类型的值 / Safely extracts value of type T17, or null */
    val v17
        get() = when (this) {
            is V17 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun <Ret> if17(callBack: (T17) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if17(callBack)

    /** 是否为第18种类型 / Checks if this is type T18 */
    val is18 get() = this is V18

    /** 安全提取第18种类型的值 / Safely extracts value of type T18, or null */
    val v18
        get() = when (this) {
            is V18 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun <Ret> if18(callBack: (T18) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if18(callBack)

    /** 是否为第19种类型 / Checks if this is type T19 */
    val is19 get() = this is V19

    /** 安全提取第19种类型的值 / Safely extracts value of type T19, or null */
    val v19
        get() = when (this) {
            is V19 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第19种类型的模式匹配 / Pattern match for type T19 */
    fun <Ret> if19(callBack: (T19) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if19(callBack)

    /** 是否为第20种类型 / Checks if this is type T20 */
    val is20 get() = this is V20

    /** 安全提取第20种类型的值 / Safely extracts value of type T20, or null */
    val v20
        get() = when (this) {
            is V20 -> {
                this.value
            }

            else -> {
                null
            }
        }

    /** 第20种类型的模式匹配 / Pattern match for type T20 */
    fun <Ret> if20(callBack: (T20) -> Ret) =
        Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
            this
        ).if20(callBack)

}

/**
 * 二十元变体匹配器
 *
 * Matcher for fluent pattern matching on Variant20.
 * 用于 Variant20 值流式模式匹配的匹配器类。
 */
data class Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret>(
    private val value: Variant20<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20>
) {
    private lateinit var callBack1: (T1) -> Ret
    private lateinit var callBack2: (T2) -> Ret
    private lateinit var callBack3: (T3) -> Ret
    private lateinit var callBack4: (T4) -> Ret
    private lateinit var callBack5: (T5) -> Ret
    private lateinit var callBack6: (T6) -> Ret
    private lateinit var callBack7: (T7) -> Ret
    private lateinit var callBack8: (T8) -> Ret
    private lateinit var callBack9: (T9) -> Ret
    private lateinit var callBack10: (T10) -> Ret
    private lateinit var callBack11: (T11) -> Ret
    private lateinit var callBack12: (T12) -> Ret
    private lateinit var callBack13: (T13) -> Ret
    private lateinit var callBack14: (T14) -> Ret
    private lateinit var callBack15: (T15) -> Ret
    private lateinit var callBack16: (T16) -> Ret
    private lateinit var callBack17: (T17) -> Ret
    private lateinit var callBack18: (T18) -> Ret
    private lateinit var callBack19: (T19) -> Ret
    private lateinit var callBack20: (T20) -> Ret

    /** 第1种类型的模式匹配 / Pattern match for type T1 */
    fun if1(callBack: (T1) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack1 = callBack
        return this
    }

    /** 第2种类型的模式匹配 / Pattern match for type T2 */
    fun if2(callBack: (T2) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack2 = callBack
        return this
    }

    /** 第3种类型的模式匹配 / Pattern match for type T3 */
    fun if3(callBack: (T3) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack3 = callBack
        return this
    }

    /** 第4种类型的模式匹配 / Pattern match for type T4 */
    fun if4(callBack: (T4) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack4 = callBack
        return this
    }

    /** 第5种类型的模式匹配 / Pattern match for type T5 */
    fun if5(callBack: (T5) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack5 = callBack
        return this
    }

    /** 第6种类型的模式匹配 / Pattern match for type T6 */
    fun if6(callBack: (T6) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack6 = callBack
        return this
    }

    /** 第7种类型的模式匹配 / Pattern match for type T7 */
    fun if7(callBack: (T7) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack7 = callBack
        return this
    }

    /** 第8种类型的模式匹配 / Pattern match for type T8 */
    fun if8(callBack: (T8) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack8 = callBack
        return this
    }

    /** 第9种类型的模式匹配 / Pattern match for type T9 */
    fun if9(callBack: (T9) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack9 = callBack
        return this
    }

    /** 第10种类型的模式匹配 / Pattern match for type T10 */
    fun if10(callBack: (T10) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack10 = callBack
        return this
    }

    /** 第11种类型的模式匹配 / Pattern match for type T11 */
    fun if11(callBack: (T11) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack11 = callBack
        return this
    }

    /** 第12种类型的模式匹配 / Pattern match for type T12 */
    fun if12(callBack: (T12) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack12 = callBack
        return this
    }

    /** 第13种类型的模式匹配 / Pattern match for type T13 */
    fun if13(callBack: (T13) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack13 = callBack
        return this
    }

    /** 第14种类型的模式匹配 / Pattern match for type T14 */
    fun if14(callBack: (T14) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack14 = callBack
        return this
    }

    /** 第15种类型的模式匹配 / Pattern match for type T15 */
    fun if15(callBack: (T15) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack15 = callBack
        return this
    }

    /** 第16种类型的模式匹配 / Pattern match for type T16 */
    fun if16(callBack: (T16) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack16 = callBack
        return this
    }

    /** 第17种类型的模式匹配 / Pattern match for type T17 */
    fun if17(callBack: (T17) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack17 = callBack
        return this
    }

    /** 第18种类型的模式匹配 / Pattern match for type T18 */
    fun if18(callBack: (T18) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack18 = callBack
        return this
    }

    /** 第19种类型的模式匹配 / Pattern match for type T19 */
    fun if19(callBack: (T19) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack19 = callBack
        return this
    }

    /** 第20种类型的模式匹配 / Pattern match for type T20 */
    fun if20(callBack: (T20) -> Ret): Variant20Matcher<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, T17, T18, T19, T20, Ret> {
        callBack20 = callBack
        return this
    }

    /** 执行匹配并返回结果 / Execute match and return result */
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
        inline fun <reified T : Any> make(value: T) = Variant(value, T::class)
    }

    inline fun <reified T : Any> isA() = clazz == T::class

    inline fun <reified T : Any> get() = if (isA<T>()) {
        value as T
    } else {
        null
    }

    inline fun <reified T : Any, Ret> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
        val ret = VariantMatcher<Ret>(this)
        return ret.ifIs(callBack)
    }
}

class VariantMatcher<Ret>(private val value: Variant) {
    val callBacks: MutableMap<KClass<*>, (Any) -> Ret> = hashMapOf()

    inline fun <reified T : Any> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
        callBacks[T::class] = { value: Any -> callBack(value as T) }
        return this
    }

    /** 执行匹配并返回结果 / Execute match and return result */
    operator fun invoke() = callBacks[value.clazz]?.let { it -> it(value.value) }
}

fun <T1, T2, Ret> match(value: Variant2<T1, T2>, callBack1: (T1) -> Ret, callBack2: (T2) -> Ret): Ret {
    val matcher = value.if1(callBack1).if2(callBack2)
    return matcher()
}

fun <T1, T2, T3, Ret> match(
    value: Variant3<T1, T2, T3>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3)
    return matcher()
}

fun <T1, T2, T3, T4, Ret> match(
    value: Variant4<T1, T2, T3, T4>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4)
    return matcher()
}

fun <T1, T2, T3, T4, T5, Ret> match(
    value: Variant5<T1, T2, T3, T4, T5>,
    callBack1: (T1) -> Ret,
    callBack2: (T2) -> Ret,
    callBack3: (T3) -> Ret,
    callBack4: (T4) -> Ret,
    callBack5: (T5) -> Ret
): Ret {
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5)
    return matcher()
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
    val matcher = value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6)
    return matcher()
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
        value.if1(callBack1).if2(callBack2).if3(callBack3).if4(callBack4).if5(callBack5).if6(callBack6).if7(callBack7)
    return matcher()
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
            .if8(callBack8)
    return matcher()
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
            .if8(callBack8).if9(callBack9)
    return matcher()
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
            .if8(callBack8).if9(callBack9).if10(callBack10)
    return matcher()
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
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11)
    return matcher()
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
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12)
    return matcher()
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
            .if8(callBack8).if9(callBack9).if10(callBack10).if11(callBack11).if12(callBack12).if13(callBack13)
    return matcher()
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
            .if14(callBack14)
    return matcher()
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
            .if14(callBack14).if15(callBack15)
    return matcher()
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
            .if14(callBack14).if15(callBack15).if16(callBack16)
    return matcher()
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
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17)
    return matcher()
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
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17).if18(callBack18)
    return matcher()
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
            .if14(callBack14).if15(callBack15).if16(callBack16).if17(callBack17).if18(callBack18).if19(callBack19)
    return matcher()
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
            .if20(callBack20)
    return matcher()
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
