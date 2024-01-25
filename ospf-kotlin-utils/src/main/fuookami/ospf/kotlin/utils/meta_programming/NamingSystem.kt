package fuookami.ospf.kotlin.utils.meta_programming

import java.util.*

private val Char.isAlphaNumber: Boolean get() = isLowerCase() || isUpperCase() || isDigit()

enum class NamingSystem {
    /**
     * e.g. play_station
     */
    SnakeCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            if (name.isEmpty()) {
                return emptyList()
            }

            assert(name.all { it.isAlphaNumber || it == '_' })
            return name.split("_").map { it.lowercase(Locale.getDefault()) }
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString("_") { it.lowercase(Locale.getDefault()) }
        }
    },

    /**
     * e.g. PLAY_STATION
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
     * e.g. play-station
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
     * e.g. playStation
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
            }.joinToString()
        }
    },

    /**
     * e.g. PlayStation
     */
    PascalCase {
        override fun frontend(name: String, abbreviations: Set<String>): List<String> {
            return CamelCase.frontend(name, abbreviations)
        }

        override fun backend(words: List<String>, abbreviations: Set<String>): String {
            return words.joinToString { word ->
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
     * split the given name to word sequence with this naming system
     *
     * @param name              the given name
     * @param abbreviations     abbreviation set
     * @return                  word sequence
     */
    abstract fun frontend(name: String, abbreviations: Set<String> = emptySet()): List<String>

    /**
     * join the given word sequence to a name corresponding this naming system
     *
     * @param words             the given word sequence
     * @param abbreviations     abbreviation set
     * @return                  the name corresponding this naming system
     */
    abstract fun backend(words: List<String>, abbreviations: Set<String> = emptySet()): String
}
