package fuookami.ospf.kotlin.utils.meta_programming

import java.util.*

/**
 * 命名系统
 */
enum class NamingSystem {
    /**
     * 蛇式命名法：play_station
     */
    Underscore {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return name.split("_")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("_")
        }
    },

    /**
     * 大蛇式命名法：PLAY_STATION
     */
    UpperUnderscore {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return name.split("_").map { it.lowercase(Locale.getDefault()) }
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("_") { it.uppercase(Locale.getDefault()) }
        }
    },

    /**
     * 烤肉串命名法：play-station
     */
    KebabCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return name.split("-")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("-")
        }
    },

    /**
     * 驼峰式命名法：playStation
     */
    CamelCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    },

    /**
     * 大驼峰式命名法：PlayStation
     */
    PascalCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    };

    abstract fun frontend(name: String, abbreviations: Set<String> = emptySet()): List<String>
    abstract fun backend(words: List<String>, abbreviations: Set<String> = emptySet()): String
}
