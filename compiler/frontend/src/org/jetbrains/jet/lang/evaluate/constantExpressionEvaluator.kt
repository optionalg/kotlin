package org.jetbrains.jet.lang.evaluate

import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant
import org.jetbrains.jet.lang.resolve.name.Name

fun evaluateBinaryExpression(firstCompileTimeConstant: CompileTimeConstant<*>, secondCompileTimeConstant: CompileTimeConstant<*>, functionName: Name): Any? {
    val compileTimeTypeFirst = getCompileTimeType(firstCompileTimeConstant)
    val compileTimeTypeSecond = getCompileTimeType(secondCompileTimeConstant)
    if (compileTimeTypeFirst == null || compileTimeTypeSecond == null) {
        return null
    }

    val first = firstCompileTimeConstant.getValue()
    val second = secondCompileTimeConstant.getValue()

    val function = binaryOperations[BinaryOperation(compileTimeTypeFirst, compileTimeTypeSecond, functionName)]
    if (function != null)  {
        return function(first, second)
    }

    return null
}

fun getCompileTimeType(c: CompileTimeConstant<*>): CompileTimeType<out Any>? = when (c.getValue()) {
    is Int -> INT
    is Byte -> BYTE
    is Short -> SHORT
    is Long -> LONG
    is Double -> DOUBLE
    is Float -> FLOAT
    is Char -> CHAR
    is Boolean -> BOOLEAN
    is String -> STRING
    else -> null
}

private class CompileTimeType<T>

private val BYTE = CompileTimeType<Byte>()
private val SHORT = CompileTimeType<Short>()
private val INT = CompileTimeType<Int>()
private val LONG = CompileTimeType<Long>()
private val DOUBLE = CompileTimeType<Double>()
private val FLOAT = CompileTimeType<Float>()
private val CHAR = CompileTimeType<Char>()
private val BOOLEAN = CompileTimeType<Boolean>()
private val STRING = CompileTimeType<String>()

private fun <A, B> bOp(a: CompileTimeType<A>, b: CompileTimeType<B>, functionNameAsString: String, f: (A, B) -> Any) = BinaryOperation(a, b, Name.identifier(functionNameAsString)) to f  as Function2<Any?, Any?, Any>

private data class BinaryOperation<A, B>(val f: CompileTimeType<out A>, val s: CompileTimeType<out B>, val functionName: Name)

private val binaryOperations = hashMapOf<BinaryOperation<*, *>, (Any?, Any?) -> Any>(
        // String
        bOp(STRING, STRING, "plus", { a, b -> a + b }),
        bOp(STRING, BYTE,   "plus", { a, b -> a + b }),
        bOp(STRING, SHORT,  "plus", { a, b -> a + b }),
        bOp(STRING, INT,    "plus", { a, b -> a + b }),
        bOp(STRING, LONG,   "plus", { a, b -> a + b }),
        bOp(STRING, DOUBLE, "plus", { a, b -> a + b }),
        bOp(STRING, FLOAT,  "plus", { a, b -> a + b }),
        bOp(STRING, CHAR,   "plus", { a, b -> a + b }),

        // Byte
        bOp(BYTE, DOUBLE, "plus", { a, b -> a + b }),
        bOp(BYTE, FLOAT,  "plus", { a, b -> a + b }),
        bOp(BYTE, LONG,   "plus", { a, b -> a + b }),
        bOp(BYTE, INT,    "plus", { a, b -> a + b }),
        bOp(BYTE, SHORT,  "plus", { a, b -> a + b }),
        bOp(BYTE, BYTE,   "plus", { a, b -> a + b }),
        bOp(BYTE, CHAR,   "plus", { a, b -> a + b }),
        bOp(BYTE, DOUBLE, "minus", { a, b -> a - b }),
        bOp(BYTE, FLOAT,  "minus", { a, b -> a - b }),
        bOp(BYTE, LONG,   "minus", { a, b -> a - b }),
        bOp(BYTE, INT,    "minus", { a, b -> a - b }),
        bOp(BYTE, SHORT,  "minus", { a, b -> a - b }),
        bOp(BYTE, BYTE,   "minus", { a, b -> a - b }),
        bOp(BYTE, CHAR,   "minus", { a, b -> a - b }),
        bOp(BYTE, DOUBLE, "times", { a, b -> a * b }),
        bOp(BYTE, FLOAT,  "times", { a, b -> a * b }),
        bOp(BYTE, LONG,   "times", { a, b -> a * b }),
        bOp(BYTE, INT,    "times", { a, b -> a * b }),
        bOp(BYTE, SHORT,  "times", { a, b -> a * b }),
        bOp(BYTE, BYTE,   "times", { a, b -> a * b }),
        bOp(BYTE, CHAR,   "times", { a, b -> a * b }),
        bOp(BYTE, DOUBLE,  "div", { a, b -> a / b }),
        bOp(BYTE, FLOAT,   "div", { a, b -> a / b }),
        bOp(BYTE, LONG,    "div", { a, b -> a / b }),
        bOp(BYTE, INT,     "div", { a, b -> a / b }),
        bOp(BYTE, SHORT,   "div", { a, b -> a / b }),
        bOp(BYTE, BYTE,    "div", { a, b -> a / b }),
        bOp(BYTE, CHAR,    "div", { a, b -> a / b }),
        bOp(BYTE, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(BYTE, FLOAT,   "mod", { a, b -> a % b }),
        bOp(BYTE, LONG,    "mod", { a, b -> a % b }),
        bOp(BYTE, INT,     "mod", { a, b -> a % b }),
        bOp(BYTE, SHORT,   "mod", { a, b -> a % b }),
        bOp(BYTE, BYTE,    "mod", { a, b -> a % b }),
        bOp(BYTE, CHAR,    "mod", { a, b -> a % b }),

        // Short
        bOp(SHORT, DOUBLE, "plus", { a, b -> a + b }),
        bOp(SHORT, FLOAT,  "plus", { a, b -> a + b }),
        bOp(SHORT, LONG,   "plus", { a, b -> a + b }),
        bOp(SHORT, INT,    "plus", { a, b -> a + b }),
        bOp(SHORT, SHORT,  "plus", { a, b -> a + b }),
        bOp(SHORT, BYTE,   "plus", { a, b -> a + b }),
        bOp(SHORT, CHAR,   "plus", { a, b -> a + b }),
        bOp(SHORT, DOUBLE, "minus", { a, b -> a - b }),
        bOp(SHORT, FLOAT,  "minus", { a, b -> a - b }),
        bOp(SHORT, LONG,   "minus", { a, b -> a - b }),
        bOp(SHORT, INT,    "minus", { a, b -> a - b }),
        bOp(SHORT, SHORT,  "minus", { a, b -> a - b }),
        bOp(SHORT, BYTE,   "minus", { a, b -> a - b }),
        bOp(SHORT, CHAR,   "minus", { a, b -> a - b }),
        bOp(SHORT, DOUBLE, "times", { a, b -> a * b }),
        bOp(SHORT, FLOAT,  "times", { a, b -> a * b }),
        bOp(SHORT, LONG,   "times", { a, b -> a * b }),
        bOp(SHORT, INT,    "times", { a, b -> a * b }),
        bOp(SHORT, SHORT,  "times", { a, b -> a * b }),
        bOp(SHORT, BYTE,   "times", { a, b -> a * b }),
        bOp(SHORT, CHAR,   "times", { a, b -> a * b }),
        bOp(SHORT, DOUBLE,  "div", { a, b -> a / b }),
        bOp(SHORT, FLOAT,   "div", { a, b -> a / b }),
        bOp(SHORT, LONG,    "div", { a, b -> a / b }),
        bOp(SHORT, INT,     "div", { a, b -> a / b }),
        bOp(SHORT, SHORT,   "div", { a, b -> a / b }),
        bOp(SHORT, BYTE,    "div", { a, b -> a / b }),
        bOp(SHORT, CHAR,    "div", { a, b -> a / b }),
        bOp(SHORT, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(SHORT, FLOAT,   "mod", { a, b -> a % b }),
        bOp(SHORT, LONG,    "mod", { a, b -> a % b }),
        bOp(SHORT, INT,     "mod", { a, b -> a % b }),
        bOp(SHORT, SHORT,   "mod", { a, b -> a % b }),
        bOp(SHORT, BYTE,    "mod", { a, b -> a % b }),
        bOp(SHORT, CHAR,    "mod", { a, b -> a % b }),

        // Int
        bOp(INT, DOUBLE, "plus", { a, b -> a + b }),
        bOp(INT, FLOAT,  "plus", { a, b -> a + b }),
        bOp(INT, LONG,   "plus", { a, b -> a + b }),
        bOp(INT, INT,    "plus", { a, b -> a + b }),
        bOp(INT, SHORT,  "plus", { a, b -> a + b }),
        bOp(INT, BYTE,   "plus", { a, b -> a + b }),
        bOp(INT, CHAR,   "plus", { a, b -> a + b }),
        bOp(INT, DOUBLE, "minus", { a, b -> a - b }),
        bOp(INT, FLOAT,  "minus", { a, b -> a - b }),
        bOp(INT, LONG,   "minus", { a, b -> a - b }),
        bOp(INT, INT,    "minus", { a, b -> a - b }),
        bOp(INT, SHORT,  "minus", { a, b -> a - b }),
        bOp(INT, BYTE,   "minus", { a, b -> a - b }),
        bOp(INT, CHAR,   "minus", { a, b -> a - b }),
        bOp(INT, DOUBLE, "times", { a, b -> a * b }),
        bOp(INT, FLOAT,  "times", { a, b -> a * b }),
        bOp(INT, LONG,   "times", { a, b -> a * b }),
        bOp(INT, INT,    "times", { a, b -> a * b }),
        bOp(INT, SHORT,  "times", { a, b -> a * b }),
        bOp(INT, BYTE,   "times", { a, b -> a * b }),
        bOp(INT, CHAR,   "times", { a, b -> a * b }),
        bOp(INT, DOUBLE,  "div", { a, b -> a / b }),
        bOp(INT, FLOAT,   "div", { a, b -> a / b }),
        bOp(INT, LONG,    "div", { a, b -> a / b }),
        bOp(INT, INT,     "div", { a, b -> a / b }),
        bOp(INT, SHORT,   "div", { a, b -> a / b }),
        bOp(INT, BYTE,    "div", { a, b -> a / b }),
        bOp(INT, CHAR,    "div", { a, b -> a / b }),
        bOp(INT, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(INT, FLOAT,   "mod", { a, b -> a % b }),
        bOp(INT, LONG,    "mod", { a, b -> a % b }),
        bOp(INT, INT,     "mod", { a, b -> a % b }),
        bOp(INT, SHORT,   "mod", { a, b -> a % b }),
        bOp(INT, BYTE,    "mod", { a, b -> a % b }),
        bOp(INT, CHAR,    "mod", { a, b -> a % b }),

        // Long
        bOp(LONG, DOUBLE, "plus", { a, b -> a + b }),
        bOp(LONG, FLOAT,  "plus", { a, b -> a + b }),
        bOp(LONG, LONG,   "plus", { a, b -> a + b }),
        bOp(LONG, INT,    "plus", { a, b -> a + b }),
        bOp(LONG, SHORT,  "plus", { a, b -> a + b }),
        bOp(LONG, BYTE,   "plus", { a, b -> a + b }),
        bOp(LONG, CHAR,   "plus", { a, b -> a + b }),
        bOp(LONG, DOUBLE, "minus", { a, b -> a - b }),
        bOp(LONG, FLOAT,  "minus", { a, b -> a - b }),
        bOp(LONG, LONG,   "minus", { a, b -> a - b }),
        bOp(LONG, INT,    "minus", { a, b -> a - b }),
        bOp(LONG, SHORT,  "minus", { a, b -> a - b }),
        bOp(LONG, BYTE,   "minus", { a, b -> a - b }),
        bOp(LONG, CHAR,   "minus", { a, b -> a - b }),
        bOp(LONG, DOUBLE, "times", { a, b -> a * b }),
        bOp(LONG, FLOAT,  "times", { a, b -> a * b }),
        bOp(LONG, LONG,   "times", { a, b -> a * b }),
        bOp(LONG, INT,    "times", { a, b -> a * b }),
        bOp(LONG, SHORT,  "times", { a, b -> a * b }),
        bOp(LONG, BYTE,   "times", { a, b -> a * b }),
        bOp(LONG, CHAR,   "times", { a, b -> a * b }),
        bOp(LONG, DOUBLE,  "div", { a, b -> a / b }),
        bOp(LONG, FLOAT,   "div", { a, b -> a / b }),
        bOp(LONG, LONG,    "div", { a, b -> a / b }),
        bOp(LONG, INT,     "div", { a, b -> a / b }),
        bOp(LONG, SHORT,   "div", { a, b -> a / b }),
        bOp(LONG, BYTE,    "div", { a, b -> a / b }),
        bOp(LONG, CHAR,    "div", { a, b -> a / b }),
        bOp(LONG, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(LONG, FLOAT,   "mod", { a, b -> a % b }),
        bOp(LONG, LONG,    "mod", { a, b -> a % b }),
        bOp(LONG, INT,     "mod", { a, b -> a % b }),
        bOp(LONG, SHORT,   "mod", { a, b -> a % b }),
        bOp(LONG, BYTE,    "mod", { a, b -> a % b }),
        bOp(LONG, CHAR,    "mod", { a, b -> a % b }),

        // Double
        bOp(DOUBLE, DOUBLE, "plus", { a, b -> a + b }),
        bOp(DOUBLE, FLOAT,  "plus", { a, b -> a + b }),
        bOp(DOUBLE, LONG,   "plus", { a, b -> a + b }),
        bOp(DOUBLE, INT,    "plus", { a, b -> a + b }),
        bOp(DOUBLE, SHORT,  "plus", { a, b -> a + b }),
        bOp(DOUBLE, BYTE,   "plus", { a, b -> a + b }),
        bOp(DOUBLE, CHAR,   "plus", { a, b -> a + b }),
        bOp(DOUBLE, DOUBLE, "minus", { a, b -> a - b }),
        bOp(DOUBLE, FLOAT,  "minus", { a, b -> a - b }),
        bOp(DOUBLE, LONG,   "minus", { a, b -> a - b }),
        bOp(DOUBLE, INT,    "minus", { a, b -> a - b }),
        bOp(DOUBLE, SHORT,  "minus", { a, b -> a - b }),
        bOp(DOUBLE, BYTE,   "minus", { a, b -> a - b }),
        bOp(DOUBLE, CHAR,   "minus", { a, b -> a - b }),
        bOp(DOUBLE, DOUBLE, "times", { a, b -> a * b }),
        bOp(DOUBLE, FLOAT,  "times", { a, b -> a * b }),
        bOp(DOUBLE, LONG,   "times", { a, b -> a * b }),
        bOp(DOUBLE, INT,    "times", { a, b -> a * b }),
        bOp(DOUBLE, SHORT,  "times", { a, b -> a * b }),
        bOp(DOUBLE, BYTE,   "times", { a, b -> a * b }),
        bOp(DOUBLE, CHAR,   "times", { a, b -> a * b }),
        bOp(DOUBLE, DOUBLE,  "div", { a, b -> a / b }),
        bOp(DOUBLE, FLOAT,   "div", { a, b -> a / b }),
        bOp(DOUBLE, LONG,    "div", { a, b -> a / b }),
        bOp(DOUBLE, INT,     "div", { a, b -> a / b }),
        bOp(DOUBLE, SHORT,   "div", { a, b -> a / b }),
        bOp(DOUBLE, BYTE,    "div", { a, b -> a / b }),
        bOp(DOUBLE, CHAR,    "div", { a, b -> a / b }),
        bOp(DOUBLE, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(DOUBLE, FLOAT,   "mod", { a, b -> a % b }),
        bOp(DOUBLE, LONG,    "mod", { a, b -> a % b }),
        bOp(DOUBLE, INT,     "mod", { a, b -> a % b }),
        bOp(DOUBLE, SHORT,   "mod", { a, b -> a % b }),
        bOp(DOUBLE, BYTE,    "mod", { a, b -> a % b }),

        // Float
        bOp(FLOAT, DOUBLE, "plus", { a, b -> a + b }),
        bOp(FLOAT, FLOAT,  "plus", { a, b -> a + b }),
        bOp(FLOAT, LONG,   "plus", { a, b -> a + b }),
        bOp(FLOAT, INT,    "plus", { a, b -> a + b }),
        bOp(FLOAT, SHORT,  "plus", { a, b -> a + b }),
        bOp(FLOAT, BYTE,   "plus", { a, b -> a + b }),
        bOp(FLOAT, CHAR,   "plus", { a, b -> a + b }),
        bOp(FLOAT, DOUBLE, "minus", { a, b -> a - b }),
        bOp(FLOAT, FLOAT,  "minus", { a, b -> a - b }),
        bOp(FLOAT, LONG,   "minus", { a, b -> a - b }),
        bOp(FLOAT, INT,    "minus", { a, b -> a - b }),
        bOp(FLOAT, SHORT,  "minus", { a, b -> a - b }),
        bOp(FLOAT, BYTE,   "minus", { a, b -> a - b }),
        bOp(FLOAT, CHAR,   "minus", { a, b -> a - b }),
        bOp(FLOAT, DOUBLE, "times", { a, b -> a * b }),
        bOp(FLOAT, FLOAT,  "times", { a, b -> a * b }),
        bOp(FLOAT, LONG,   "times", { a, b -> a * b }),
        bOp(FLOAT, INT,    "times", { a, b -> a * b }),
        bOp(FLOAT, SHORT,  "times", { a, b -> a * b }),
        bOp(FLOAT, BYTE,   "times", { a, b -> a * b }),
        bOp(FLOAT, CHAR,   "times", { a, b -> a * b }),
        bOp(FLOAT, DOUBLE,  "div", { a, b -> a / b }),
        bOp(FLOAT, FLOAT,   "div", { a, b -> a / b }),
        bOp(FLOAT, LONG,    "div", { a, b -> a / b }),
        bOp(FLOAT, INT,     "div", { a, b -> a / b }),
        bOp(FLOAT, SHORT,   "div", { a, b -> a / b }),
        bOp(FLOAT, BYTE,    "div", { a, b -> a / b }),
        bOp(FLOAT, CHAR,    "div", { a, b -> a / b }),
        bOp(FLOAT, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(FLOAT, FLOAT,   "mod", { a, b -> a % b }),
        bOp(FLOAT, LONG,    "mod", { a, b -> a % b }),
        bOp(FLOAT, INT,     "mod", { a, b -> a % b }),
        bOp(FLOAT, SHORT,   "mod", { a, b -> a % b }),
        bOp(FLOAT, BYTE,    "mod", { a, b -> a % b }),
        bOp(FLOAT, CHAR,    "mod", { a, b -> a % b }),

        // Char
        bOp(CHAR, DOUBLE, "plus", { a, b -> a + b }),
        bOp(CHAR, FLOAT,  "plus", { a, b -> a + b }),
        bOp(CHAR, LONG,   "plus", { a, b -> a + b }),
        bOp(CHAR, INT,    "plus", { a, b -> a + b }),
        bOp(CHAR, SHORT,  "plus", { a, b -> a + b }),
        bOp(CHAR, BYTE,   "plus", { a, b -> a + b }),
        bOp(CHAR, DOUBLE, "minus", { a, b -> a - b }),
        bOp(CHAR, FLOAT,  "minus", { a, b -> a - b }),
        bOp(CHAR, LONG,   "minus", { a, b -> a - b }),
        bOp(CHAR, INT,    "minus", { a, b -> a - b }),
        bOp(CHAR, SHORT,  "minus", { a, b -> a - b }),
        bOp(CHAR, BYTE,   "minus", { a, b -> a - b }),
        bOp(CHAR, CHAR,   "minus", { a, b -> a - b }),
        bOp(CHAR, DOUBLE, "times", { a, b -> a * b }),
        bOp(CHAR, FLOAT,  "times", { a, b -> a * b }),
        bOp(CHAR, LONG,   "times", { a, b -> a * b }),
        bOp(CHAR, INT,    "times", { a, b -> a * b }),
        bOp(CHAR, SHORT,  "times", { a, b -> a * b }),
        bOp(CHAR, BYTE,   "times", { a, b -> a * b }),
        bOp(CHAR, DOUBLE,  "div", { a, b -> a / b }),
        bOp(CHAR, FLOAT,   "div", { a, b -> a / b }),
        bOp(CHAR, LONG,    "div", { a, b -> a / b }),
        bOp(CHAR, INT,     "div", { a, b -> a / b }),
        bOp(CHAR, SHORT,   "div", { a, b -> a / b }),
        bOp(CHAR, BYTE,    "div", { a, b -> a / b }),
        bOp(CHAR, DOUBLE,  "mod", { a, b -> a % b }),
        bOp(CHAR, FLOAT,   "mod", { a, b -> a % b }),
        bOp(CHAR, LONG,    "mod", { a, b -> a % b }),
        bOp(CHAR, INT,     "mod", { a, b -> a % b }),
        bOp(CHAR, SHORT,   "mod", { a, b -> a % b }),
        bOp(CHAR, BYTE,    "mod", { a, b -> a % b })
)

