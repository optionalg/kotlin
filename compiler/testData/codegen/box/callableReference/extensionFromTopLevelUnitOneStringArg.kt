class A {
    var result = "Fail"
}

fun A.foo(newResult: String) {
    result = newResult
}

fun box(): String {
    val a = A()
    val x = A::foo
    a.x("OK")
    return a.result
}