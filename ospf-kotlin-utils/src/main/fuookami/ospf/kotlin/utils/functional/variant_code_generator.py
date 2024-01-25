import os
import sys


def gen_variantn_generic_parameter(i):
    code = "T1"
    for j in range(1, i):
        code += ", T%d" % (j + 1)
    return code


def gen_variantn_generic_parameter_with_base(i, base):
    code = "T1: %s<T1>" % base
    for j in range(1, i):
        code += ", T%d: %s<T%d>" % (j + 1, base, j + 1)
    return code


def gen_variantn_subclass(i):
    code = ""
    for j in range(i):
        code += \
            """
                data class V%d<%s>(val value: T%d): Variant%d<%s>() {}""" % (
                j + 1, gen_variantn_generic_parameter(i), j + 1, i, gen_variantn_generic_parameter(i))
    return code


def gen_variantn_function(i):
    code = ""
    for j in range(i):
        code += \
            """
                val is%d get() = this is V%d;
            
                val v%d get() = when (this) {
                    is V%d -> { this.value }
                    else -> { null }
                }
            
                fun <Ret> if%d(extractor: Extractor<Ret, T%d>) = Variant%dMatcher<%s, Ret>(this).if%d(callBack)
            """ % (j + 1, j + 1, j + 1, j + 1, j + 1, j + 1, i, gen_variantn_generic_parameter(i), j + 1)
    return code


def gen_variantn_code(i):
    return \
        """
        sealed class Variant%d<%s>() {
        %s
        %s
        }
        """ % (i, gen_variantn_generic_parameter(i), gen_variantn_subclass(i), gen_variantn_function(i))


def gen_variantn_call_back(i):
    code = ""
    for j in range(i):
        code += \
            """
                private lateinit var callBack%d: (T%d) -> Ret;""" % (j + 1, j + 1)
    return code


def gen_variantn_match_call_back(i):
    code = ""
    for j in range(i):
        code += \
            """
                fun if%d(callBack: (T%d) -> Ret): Variant%dMatcher<%s, Ret> {
                    callBack%d = callBack;
                    return this;
                }
            """ % (j + 1, j + 1, i, gen_variantn_generic_parameter(i), j + 1)
    return code


def gen_variantn_call_back_invoke(i):
    code = "is Variant%d.V1 -> { callBack1(value.value) }" % i
    for j in range(1, i):
        code += \
            """
                    is Variant%d.V%d -> { callBack%d(value.value) }""" % (i, j + 1, j + 1)
    return code


def gen_variantn_matcher_code(i):
    code = \
        """
        data class Variant%dMatcher<%s, Ret>(private val value: Variant%d<%s>) {
            %s
            %s
            @Throws(NullPointerException::class)
            operator fun invoke() = when (value) {
                %s
            }
        }
        """ % (i, gen_variantn_generic_parameter(i), i, gen_variantn_generic_parameter(i), gen_variantn_call_back(i),
               gen_variantn_match_call_back(i), gen_variantn_call_back_invoke(i))
    return code


def gen_variantn_match_parameter(i):
    code = ""
    for j in range(i):
        code += ", callBack%d: (T%d) -> Ret" % (j + 1, j + 1)
    return code


def gen_variantn_match(i):
    code = ""
    for j in range(i):
        code += ".if%d(callBack%d)" % (j + 1, j + 1)
    return code


def gen_variantn_match_code(i):
    return """
fun <%s, Ret> match(value: Variant%d<%s>%s): Ret {
    val matcher = value%s;
    return matcher();
}
""" % (gen_variantn_generic_parameter(i), i, gen_variantn_generic_parameter(i), gen_variantn_match_parameter(i),
       gen_variantn_match(i))


def gen_variantn_copy_invoke(i):
    code = "is Variant%d.V1 -> Variant%d.V1(this.value.copy())" % (i, i)
    for j in range(1, i):
        code += \
            """
                is Variant%d.V%d -> Variant%d.V%d(this.value.copy())""" % (i, j + 1, i, j + 1)
    return code


def gen_variantn_copy_code(i):
    return """
@JvmName("Variant%dCopy")
fun <%s> Variant%d<%s>.copy(): Variant%d<%s> = when (this) {
    %s
}
""" % (i, gen_variantn_generic_parameter_with_base(i, "Copyable"), i,
       gen_variantn_generic_parameter(i), i, gen_variantn_generic_parameter(i), gen_variantn_copy_invoke(i))


def gen_variantn_move_invoke(i):
    code = "is Variant%d.V1 -> Variant%d.V1(this.value.move())" % (i, i)
    for j in range(1, i):
        code += \
            """
                is Variant%d.V%d -> Variant%d.V%d(this.value.move())""" % (i, j + 1, i, j + 1)
    return code


def gen_variantn_move_code(i):
    return """
@JvmName("Variant%dMove")
fun <%s> Variant%d<%s>.move(): Variant%d<%s> = when (this) {
    %s
}
""" % (i, gen_variantn_generic_parameter_with_base(i, "Movable"), i, gen_variantn_generic_parameter(i), i,
       gen_variantn_generic_parameter(i), gen_variantn_move_invoke(i))


def gen_variant_code():
    return \
        """
        class Variant(val value: Any, val clazz: KClass<*>) {
            companion object {
                inline fun <reified T: Any> make(value: T) = Variant(value, T::class);
            }
        
            inline fun <reified T: Any> isA() = clazz == T::class;
        
            inline fun <reified T: Any> get() = if (isA<T>()) {
                value as T;
            } else {
                null;
            }
        
            inline fun <reified T: Any, Ret> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
                val ret = VariantMatcher<Ret>(this);
                return ret.ifIs(callBack);
            }
        }
        
        class VariantMatcher<Ret>(private val value: Variant) {
            val callBacks: MutableMap<KClass<*>, (Any) -> Ret> = hashMapOf();
        
            inline fun <reified T: Any> ifIs(noinline callBack: (T) -> Ret): VariantMatcher<Ret> {
                callBacks[T::class] = { value: Any -> callBack(value as T) };
                return this;
            }
        
            operator fun invoke() = callBacks[value.clazz]?.let { it -> it(value.value) };
        }
        """


def gen_code(num):
    code = \
        """package fuookami.ospf.kotlin.utils.functional
        
        import kotlin.reflect.*
        import kotlin.collections.*
        import fuookami.ospf.kotlin.utils.concept.*
        """
    for i in range(2, num + 1):
        code += gen_variantn_code(i)
        code += gen_variantn_matcher_code(i)
    code += gen_variant_code()
    for i in range(2, num + 1):
        code += gen_variantn_match_code(i)
    for i in range(2, num + 1):
        code += gen_variantn_copy_code(i)
    for i in range(2, num + 1):
        code += gen_variantn_move_code(i)
    return code


def main():
    src_path = os.path.abspath("%s/Variant.kt" % sys.path[0])
    code = gen_code(20)
    file = open(src_path, "w")
    file.write(code)
    file.close()


if __name__ == "__main__":
    main()
