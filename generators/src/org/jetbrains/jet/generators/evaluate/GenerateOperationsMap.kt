/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.generators.evaluate

import org.jetbrains.jet.di.GeneratorsFileUtil
import java.io.File
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.jet.utils.Printer
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import java.util.Collections
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.JetType

val DEST_FILE: File = File("compiler/frontend/src/org/jetbrains/jet/lang/evaluate/OperationsMapGenerated.kt")
private val EXCLUDED_FUNCTIONS = listOf("rangeTo", "hashCode", "inc", "dec")

fun main(args: Array<String>) {
    GeneratorsFileUtil.writeFileIfContentChanged(DEST_FILE, generate())
}

fun generate(): String {
    val sb = StringBuilder()
    val p = Printer(sb)
    p.println(FileUtil.loadFile(File("injector-generator/copyright.txt")))
    p.println("package org.jetbrains.jet.lang.evaluate")
    p.println()
    p.println("import java.math.BigInteger")
    p.println()
    p.println("/** This file is generated by org.jetbrains.jet.generators.evaluate:generate(). DO NOT MODIFY MANUALLY */")
    p.println()

    val unaryOperationsMap = arrayListOf<Pair<String, List<JetType>>>()
    val binaryOperationsMap = arrayListOf<Pair<String, List<JetType>>>()

    val builtIns = KotlinBuiltIns.getInstance()
    [suppress("UNCHECKED_CAST")]
    val allPrimitiveTypes = builtIns.getBuiltInsPackage().getMemberScope().getAllDescriptors()
            .filter { it is ClassDescriptor && builtIns.isPrimitiveType(it.getDefaultType()) } as List<ClassDescriptor>

    for (descriptor in allPrimitiveTypes + builtIns.getString()) {
        [suppress("UNCHECKED_CAST")]
        val functions = descriptor.getMemberScope(Collections.emptyList()).getAllDescriptors()
                .filter { it is FunctionDescriptor && !EXCLUDED_FUNCTIONS.contains(it.getName().asString()) } as List<FunctionDescriptor>

        for (function in functions) {
            val parametersTypes = function.getParametersTypes()

            when (parametersTypes.size) {
                1 -> unaryOperationsMap.add(function.getName().asString() to parametersTypes)
                2 -> binaryOperationsMap.add(function.getName().asString() to parametersTypes)
                else -> throw IllegalStateException("Couldn't add following method from builtins to operations map: ${function.getName()} in class ${descriptor.getName()}")
            }
        }
    }

    p.println("val emptyFun: Function2<BigInteger, BigInteger, BigInteger> = { a, b -> BigInteger(\"0\") }")
    p.println()
    p.println("private val unaryOperations = hashMapOf<UnaryOperationKey<*>, (Any?) -> Any>(")
    p.pushIndent()

    val unaryOperationsMapIterator = unaryOperationsMap.iterator()
    while (unaryOperationsMapIterator.hasNext()) {
        val (funcName, parameters) = unaryOperationsMapIterator.next()
        p.print(
                "unaryOperationKey(",
                parameters.map { it.asSrting() }.makeString(", "),
                ", ",
                "\"$funcName\"",
                ", { a -> a.${funcName}() })"
        )
        if (unaryOperationsMapIterator.hasNext()) {
            p.printWithNoIndent(", ")
        }
        p.println()
    }
    p.popIndent()
    p.println(")")

    p.println()


    p.println("private val binaryOperations = hashMapOf<BinaryOperationKey<*, *>, Pair<Function2<Any?, Any?, Any>, Function2<BigInteger, BigInteger, BigInteger>>>(")
    p.pushIndent()

    val binaryOperationsMapIterator = binaryOperationsMap.iterator()
    while (binaryOperationsMapIterator.hasNext()) {
        val (funcName, parameters) = binaryOperationsMapIterator.next()
        p.print(
                "binaryOperationKey(",
                parameters.map { it.asSrting() }.makeString(", "),
                ", ",
                "\"$funcName\"",
                ", Pair({ a, b -> a.${funcName}(b) }, ",
                renderCheckOperation(funcName, parameters),
                " ))"
        )
        if (binaryOperationsMapIterator.hasNext()) {
            p.printWithNoIndent(", ")
        }
        p.println()
    }
    p.popIndent()
    p.println(")")

    return sb.toString()
}

fun renderCheckOperation(name: String, params: List<JetType>): String {
    val isAllParamsIntegers = params.fold(true) { a, b -> a && b.isIntegerType() }
    if (!isAllParamsIntegers) {
        return "emptyFun"
    }

    return when(name) {
        "plus" -> "{ a, b -> a.add(b) }"
        "minus" -> "{ a, b -> a.subtract(b) }"
        "mod" -> "{ a, b -> a.mod(b) }"
        "div" -> "{ a, b -> a.divide(b) }"
        "times" -> "{ a, b -> a.multiply(b) }"
        else -> "emptyFun"
    }
}

private fun JetType.isIntegerType(): Boolean {
    val builtIns = KotlinBuiltIns.getInstance()
    return this.equals(builtIns.getIntType()) ||
            this.equals(builtIns.getShortType()) ||
            this.equals(builtIns.getByteType()) ||
            this.equals(builtIns.getLongType())
}


private fun FunctionDescriptor.getParametersTypes(): List<JetType> {
    val list = arrayListOf(getExpectedThisObject()!!.getType())
    getValueParameters().map { it.getType() }.forEach {
        list.add(TypeUtils.makeNotNullable(it))
    }
    return list
}

private fun JetType.asSrting(): String = getConstructor().getDeclarationDescriptor()!!.getName().asString().toUpperCase()
