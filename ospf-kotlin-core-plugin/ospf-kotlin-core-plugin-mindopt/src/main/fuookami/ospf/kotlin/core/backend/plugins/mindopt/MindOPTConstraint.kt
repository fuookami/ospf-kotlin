package fuookami.ospf.kotlin.core.backend.plugins.mindopt

import com.alibaba.damo.mindopt.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

enum class MindOPTConstraintSign {
    GreaterEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.GREATER_EQUAL
        }
    },
    Equal {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.EQUAL
        }
    },
    LessEqual {
        override fun toMindOPTConstraintSign(): Char {
            return MDO.LESS_EQUAL
        }
    };

    companion object {
        operator fun invoke(sign: Sign): MindOPTConstraintSign {
            return when (sign) {
                Sign.GreaterEqual -> {
                    GreaterEqual
                }

                Sign.Equal -> {
                    Equal
                }

                Sign.LessEqual -> {
                    LessEqual
                }
            }
        }
    }

    abstract fun toMindOPTConstraintSign(): Char
}
