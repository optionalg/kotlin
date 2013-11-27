val a1: Int = <!INTEGER_OVERFLOW!>Integer.MAX_VALUE + 1<!>
val a2: Int = Integer.MIN_VALUE - 1
val a3: Int = <!INTEGER_OVERFLOW!><!INTEGER_OVERFLOW!>Integer.MAX_VALUE + 1<!> - 10<!>
val a4: Int = <!INTEGER_OVERFLOW!>Integer.MAX_VALUE + 1<!> + 10