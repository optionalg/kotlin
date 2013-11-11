fun Array<Int>.test(): Array<Int> {
    val func = {(i:Int) -> this}
    return func(1)
}

fun Array<Int>.test2(): Array<Int> {
    class Z() {
        fun run(): Array<Int> {
            this@test2
        }
    }
    return Z().run()
}

fun Array<String>.test(): Array<String> {
    val func = {(i:Int) -> this}
    return func(1)
}


fun Array<String>.test2() : Array<String> {
    class Z2() {
        fun run(): Array<String> {
            this@test2
        }
    }
    return Z2().run()
}

fun box() : String {
    val array = Array<String>(2, { i -> "${i}" })
    if (array != array.test()) return "fail 1"
    if (array != array.test2()) return "fail 2"

    val array2 = Array<Int>(2, { i -> i})
    if (array2 != array2.test()) return "fail 3"
    if (array2 != array2.test2()) return "fail 4"

    return "OK"
}