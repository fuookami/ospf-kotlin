package fuookami.ospf.kotlin.utils.meta_programming

enum class NamingSystem {
    /** 蛇式命名法：play_station */
    Underscore {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    },
    /** 大蛇式命名法：PLAY_STATION */
    UpperUnderscore {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    },
    /** 烤肉串命名法：play-station */
    KebabCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    },
    /** 驼峰式命名法：playStation */
    CamelCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            TODO("Not yet implemented")
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            TODO("Not yet implemented")
        }
    },
    /** 大驼峰式命名法：PlayStation */
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
