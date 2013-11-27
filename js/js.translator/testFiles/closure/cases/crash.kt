package foo

fun run<T>(f: () -> T) = f()

fun boo(i: Int = 0): Int = if (i == 0) i else boo(i - 1)

fun box(): String {
    fun baz() = 1
    fun boo(i: Int = 0): Int = if (i == 0) i else boo(i - 1)

    run {
        run {
        }
    }

    fun bar(s: String? = null): String {
        if (s != null) return s

        bar("OK")
        return run {
            bar("OK")
        }
    }

    return bar()

    //return "OK"
}
//
//class A {
//    fun qqq() {
//        fun www() {}
//    }
//}