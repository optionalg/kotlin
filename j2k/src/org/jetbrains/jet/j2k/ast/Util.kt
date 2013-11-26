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

package org.jetbrains.jet.j2k.ast

fun List<Node>.toKotlin(separator: String, prefix: String = "", suffix: String = ""): String {
    val result = StringBuilder()
    if (size() > 0) {
        result.append(prefix)
        var first = true
        for (i in indices) {
            val elem = get(i)
            val next = if ((i + 1) in indices) get(i + 1) else null
            if (!first && (elem !is WhiteSpace && next !is WhiteSpace)){
                result.append(separator)
            }
            if (elem !is WhiteSpace) {
                first = false
            }
            result.append(elem.toKotlin())
        }
        result.append(suffix)
    }
    return result.toString()
}

fun Collection<Modifier>.toKotlin(separator: String = " "): String {
    val result = StringBuilder()
    for (x in this) {
        result.append(x.name)
        result.append(separator)
    }
    return result.toString()
}

fun String.withPrefix(prefix: String) = if (isEmpty()) "" else prefix + this
fun Expression.withPrefix(prefix: String) = if (isEmpty()) "" else prefix + toKotlin()
