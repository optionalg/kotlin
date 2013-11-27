import <!PLATFORM_CLASS_MAPPED_TO_KOTLIN!>java.lang.Long<!> as JLong

val longMinValue: Long = (-9223372036854775807).toLong()

val a1: Long = <!INTEGER_OVERFLOW!>JLong.MAX_VALUE + 1<!>
val a2: Long = <!INTEGER_OVERFLOW!>longMinValue - 10<!>

val a3: Long = <!INTEGER_OVERFLOW!><!INTEGER_OVERFLOW!>JLong.MAX_VALUE - 1 + 3<!> - 10<!>