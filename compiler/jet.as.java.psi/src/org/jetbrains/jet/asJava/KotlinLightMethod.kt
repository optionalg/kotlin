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

package org.jetbrains.jet.asJava

import com.intellij.psi.impl.light.LightMethod
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.resolve.java.jetAsJava.JetPsiMethodWrapper
import com.intellij.psi.PsiParameterList
import com.intellij.psi.impl.light.LightParameterListBuilder
import org.jetbrains.jet.plugin.JetLanguage
import com.intellij.psi.impl.light.LightParameter
import kotlin.properties.Delegates

public class KotlinLightMethod(manager: PsiManager, val method: PsiMethod, val jetDeclaration: JetDeclaration, containingClass: PsiClass):
        LightMethod(manager, method, containingClass), JetPsiMethodWrapper {

    override fun getNavigationElement() : PsiElement = jetDeclaration
    override fun getOriginalElement() : PsiElement = jetDeclaration
    override fun getOrigin(): JetDeclaration? = jetDeclaration

    override fun getParent(): PsiElement? = getContainingClass()

    private val paramsList by Delegates.lazy {
        val parameterBuilder = LightParameterListBuilder(getManager(), JetLanguage.INSTANCE)

        for ((index, parameter) in method.getParameterList().getParameters().withIndices()) {
            val lightParameter = LightParameter(parameter.getName() ?: "p$index", parameter.getType(), this, JetLanguage.INSTANCE)
            parameterBuilder.addParameter(lightParameter)
        }

        parameterBuilder
    }

    override fun setName(name: String): PsiElement? {
        (jetDeclaration as PsiNamedElement).setName(name)
        return this
    }

    public override fun delete() {
        if (jetDeclaration.isValid()) {
            jetDeclaration.delete()
        }
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        if (another is JetPsiMethodWrapper && getOrigin() == another.getOrigin())
            return true

        return super<LightMethod>.isEquivalentTo(another)
    }

    override fun getParameterList(): PsiParameterList = paramsList

    override fun copy(): PsiElement? {
        return KotlinLightMethod(getManager()!!, method, jetDeclaration, getContainingClass()!!)
    }
}
