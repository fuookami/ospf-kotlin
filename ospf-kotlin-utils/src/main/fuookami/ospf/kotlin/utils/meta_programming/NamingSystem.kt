package fuookami.ospf.kotlin.utils.meta_programming

import java.util.*

/**
 * 判断字符是否为字母或数字
 *
 * Check if a character is alphanumeric.
 */
private val Char.isAlphaNumber: Boolean get() = isLowerCase() || isUpperCase() || isDigit()

/**
 * 命名系统枚举
 *
 * Enumeration of naming systems for converting between different naming conventions.
 */
enum class NamingSystem {
    /**
     * 蛇形命名法（snake_case）
     *
     * Snake case naming (e.g., play_station).
     */
    SnakeCase {
        /**
         * 将蛇形命名转换为单词序列
         *
         * Convert snake_case name to word sequence.
         *
         * @param name 蛇形命名的名称 / Snake case name
         * @param abbreviations 缩写集合 / Abbreviation set
         * @return 单词序列 / Word sequence
         */
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            if (name.isEmpty()) {
                return emptyList()
            }

            assert(name.all { it.isAlphaNumber || it == '_' })
            return name.split("_").map { it.lowercase(Locale.getDefault()) }
        }

        /**
         * 将单词序列转换为蛇形命名
         *
         * Convert word sequence to snake_case name.
         *
         * @param words 单词序列 / Word sequence
         * @param abbreviations 缩写集合 / Abbreviation set
         * @return 蛇形命名的名称 / Snake case name
         */
        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("_") { it.lowercase(Locale.getDefault()) }
        }
    },

    /**
     * 大写蛇形命名法（UPPER_SNAKE_CASE）
     *
     * Upper snake case naming (e.g., PLAY_STATION).
     */
    UpperSnakeCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return SnakeCase.frontend(name, abbreviations)
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("_")
        }
    },

    /**
     * 短横线命名法（kebab-case）
     *
     * Kebab case naming (e.g., play-station).
     */
    KebabCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            if (name.isEmpty()) {
                return emptyList()
            }

            assert(name.all { it.isAlphaNumber || it == '-' })
            return name.split("-").map { it.lowercase(Locale.getDefault()) }
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("-")
        }
    },

    /**
     * 骆驼命名法（camelCase）
     *
     * Camel case naming (e.g., playStation).
     */
    CamelCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            if (name.isEmpty()) {
                return emptyList()
            }

            assert(name.all { it.isAlphaNumber })
            var p = 0
            var q = 1
            var currentAbbreviation: String? = null
            var alternativeAbbreviation: String? = null
            val words = ArrayList<String>()
            for (i in name.indices) {
                var part = name.substring(p, q)
                var partLower = part.lowercase(Locale.getDefault())

                if (alternativeAbbreviation != null) {
                    if (alternativeAbbreviation != partLower) {
                        if (!alternativeAbbreviation.startsWith(partLower) && currentAbbreviation != null) {
                            // stop traversing and reset
                            words.add(currentAbbreviation)
                            p += currentAbbreviation.length
                            currentAbbreviation = null
                            alternativeAbbreviation = null
                        }
                    } else {
                        val newAlternativeAbbreviation =
                            abbreviations.filter { it != partLower && it.startsWith(partLower) }
                                .minByOrNull { it.length }
                        if (newAlternativeAbbreviation == null) {
                            // stop traversing and reset
                            words.add(alternativeAbbreviation)
                            p += alternativeAbbreviation.length
                            currentAbbreviation = null
                            alternativeAbbreviation = null
                        } else {
                            // refresh and continue traversing
                            currentAbbreviation = alternativeAbbreviation
                            alternativeAbbreviation = newAlternativeAbbreviation
                        }
                    }
                } else {
                    while (true) {
                        part = name.substring(p, q)
                        partLower = part.lowercase(Locale.getDefault())

                        when (val abbreviation = abbreviations.filter { it != partLower && partLower.startsWith(it) }
                            .maxByOrNull { it.length }) {
                            null -> {
                                break
                            }

                            else -> {
                                words.add(abbreviation)
                                p += abbreviation.length
                            }
                        }
                    }

                    if (abbreviations.contains(partLower)) {
                        alternativeAbbreviation = abbreviations.filter { it != partLower && it.startsWith(partLower) }
                            .minByOrNull { it.length }
                        if (alternativeAbbreviation != null) {
                            currentAbbreviation = part
                        } else {
                            words.add(name.substring(p, q).lowercase(Locale.getDefault()))
                            p = i
                        }
                    } else {
                        alternativeAbbreviation =
                            abbreviations.filter { it.startsWith(partLower) }.minByOrNull { it.length }
                        if (alternativeAbbreviation == null && !name[i].isLowerCase()) {
                            if ((q - p) > 1) {
                                words.add(name.substring(p, q).lowercase(Locale.getDefault()))
                            }
                            p = i
                        }
                    }
                }
                q = i + 1
            }
            if (p != q) {
                words.add(name.substring(p, q).lowercase(Locale.getDefault()))
            }
            return words
        }

        /**
         * 将单词序列转换为骆驼命名
         *
         * Convert word sequence to camelCase name.
         *
         * BUG FIX: 原始代码使用 joinToString() 默认逗号分隔。
         * FIX: Original code used joinToString() with default comma separator.
         * 应使用 joinToString("") 空字符串分隔。
         * Should use joinToString("") with empty string separator.
         *
         * @param words 单词序列 / Word sequence
         * @param abbreviations 缩写集合 / Abbreviation set
         * @return 骆驼命名的名称 / Camel case name
         */
        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.mapIndexed { index, word ->
                if (abbreviations.contains(word) && word.length <= 3) {
                    word.uppercase(Locale.getDefault())
                } else if (index != 0) {
                    word.replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(Locale.ROOT)
                        } else {
                            it.toString()
                        }
                    }
                } else {
                    word
                }
            }.joinToString("")  // BUG FIX: 使用空字符串分隔
        }
    },

    /**
     * 帕斯卡命名法（PascalCase）
     *
     * Pascal case naming (e.g., PlayStation).
     */
    PascalCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return CamelCase.frontend(name, abbreviations)
        }

        /**
         * 将单词序列转换为帕斯卡命名
         *
         * Convert word sequence to PascalCase name.
         *
         * BUG FIX: 原始代码使用 joinToString { } 默认逗号分隔。
         * FIX: Original code used joinToString { } with default comma separator.
         * 应使用 joinToString("") { } 空字符串分隔。
         * Should use joinToString("") { } with empty string separator.
         *
         * @param words 单词序列 / Word sequence
         * @param abbreviations 缩写集合 / Abbreviation set
         * @return 帕斯卡命名的名称 / Pascal case name
         */
        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("") { word ->  // BUG FIX: 使用空字符串分隔
                if (abbreviations.contains(word) && word.length <= 3) {
                    word.uppercase(Locale.getDefault())
                } else {
                    word.replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(Locale.ROOT)
                        } else {
                            it.toString()
                        }
                    }
                }
            }
        }
    };

    /**
     * 将给定的名称拆分为单词序列
     *
     * Split the given name to word sequence with this naming system.
     *
     * @param name 给定的名称 / The given name
     * @param abbreviations 缩写集合 / Abbreviation set
     * @return 单词序列 / Word sequence
     */
    abstract fun frontend(name: String, abbreviations: Set<String> = emptySet()): List<String>

    /**
     * 将给定的单词序列合并为对应的命名格式
     *
     * Join the given word sequence to a name corresponding this naming system.
     *
     * @param words 给定的单词序列 / The given word sequence
     * @param abbreviations 缩写集合 / Abbreviation set
     * @return 对应命名格式的名称 / The name corresponding this naming system
     */
    abstract fun backend(words: List<String>, abbreviations: Set<String> = emptySet()): String
}
