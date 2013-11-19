// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

inline fun inlineFun(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(<!USAGE_IS_NOT_INLINABLE!>s<!>, b)
    subNoInline(<!USAGE_IS_NOT_INLINABLE!>s<!>, b)
}

inline fun Function1<Int, Unit>.inlineExt(s: (p: Int) -> Unit, noinline b: (p: Int) -> Unit) {
    subInline(<!USAGE_IS_NOT_INLINABLE!>this<!>, <!USAGE_IS_NOT_INLINABLE!>s<!>, b)
    subNoInline(<!USAGE_IS_NOT_INLINABLE!>this<!>, <!USAGE_IS_NOT_INLINABLE!>s<!>, b)
}


<!NOTHING_TO_INLINE!>inline fun subInline(vararg s: (p: Int) -> Unit) {}<!>

fun subNoInline(vararg s: (p: Int) -> Unit) {}