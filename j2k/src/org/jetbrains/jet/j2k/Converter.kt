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

package org.jetbrains.jet.j2k

import com.google.common.collect.ImmutableMap
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.intellij.psi.*
import org.jetbrains.jet.j2k.ast.*
import org.jetbrains.jet.j2k.ast.types.ClassType
import org.jetbrains.jet.j2k.ast.types.EmptyType
import org.jetbrains.jet.j2k.ast.types.Type
import org.jetbrains.jet.j2k.visitors.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import java.util.*
import com.intellij.psi.CommonClassNames.*
import org.jetbrains.jet.lang.types.expressions.OperatorConventions.*
import com.intellij.psi.util.PsiUtil
import com.intellij.openapi.project.Project

public class Converter(val project: Project, val settings: ConverterSettings) {

    private var classIdentifiersSet: MutableSet<String> = Sets.newHashSet()!!

    private val dispatcher: Dispatcher = Dispatcher(this)

    public var methodReturnType: PsiType? = null
        private set

    public fun setClassIdentifiers(identifiers: MutableSet<String>) {
        classIdentifiersSet = identifiers
    }

    public fun getClassIdentifiers(): Set<String> {
        return Collections.unmodifiableSet(classIdentifiersSet)
    }

    public fun clearClassIdentifiers() {
        classIdentifiersSet.clear()
    }

    public fun elementToKotlin(element: PsiElement): String {
        val kElement = topElementToElement(element)
        return kElement?.toKotlin() ?: ""
    }

    public fun topElementToElement(element: PsiElement?): Node? = when(element) {
        is PsiJavaFile -> fileToFile(element)
        is PsiClass -> classToClass(element)
        is PsiMethod -> methodToFunction(element)
        is PsiField -> fieldToField(element, element.getContainingClass())
        is PsiStatement -> statementToStatement(element)
        is PsiExpression -> expressionToExpression(element)
        is PsiComment -> Comment(element.getText()!!)
        is PsiWhiteSpace -> WhiteSpace(element.getText()!!)
        is PsiImportList -> importsToImportList(element)
        is PsiImportStatementBase -> importToImport(element)
        else -> null
    }

    public fun fileToFile(javaFile: PsiJavaFile): File {
        val body = ArrayList<Node>()
        for (element in javaFile.getChildren()) {
            if (element !is PsiImportStatementBase) {
                val node = topElementToElement(element)
                if (node != null) {
                    body.add(node)
                }
            }
        }
        return File(quoteKeywords(javaFile.getPackageName()), body, createMainFunction(javaFile))
    }

    public fun anonymousClassToAnonymousClass(anonymousClass: PsiAnonymousClass): AnonymousClass {
        return AnonymousClass(this, getMembers(anonymousClass))
    }

    private fun getMembers(psiClass: PsiClass): MutableList<Node> {
        val members = ArrayList<Node>()
        val lbraceOffset = psiClass.getLBrace()?.getTextRange()?.getStartOffset() ?: 0
        for (e : PsiElement? in psiClass.getChildren()) {
            val isDocComment = e?.getTextRange()?.getStartOffset() ?: 0 < lbraceOffset
            if (isDocComment) continue
            val converted = memberToMember(e, psiClass)
            if (converted != null) members.add(converted)
        }
        return members
    }

    private fun getDocComments(element: PsiElement): List<Node> {
        val comments = ArrayList<Node>()
        val textOffset = element.getTextOffset()
        for (e : PsiElement? in element.getChildren()) {
            if (e is PsiComment && e.getTextRange()?.getStartOffset() ?: 0 < textOffset) {
                comments.add(Comment(e.getText()!!));
            }
        }
        return comments
    }

    private fun memberToMember(e: PsiElement?, containingClass: PsiClass): Node? = when(e) {
        is PsiMethod -> methodToFunction(e, true)
        is PsiField -> fieldToField(e, containingClass)
        is PsiClass -> classToClass(e)
        is PsiClassInitializer -> initializerToInitializer(e)
        is PsiComment -> Comment(e.getText()!!)
        else -> null
    }

    private fun classToClass(psiClass: PsiClass): Class {
        val modifiers: Set<Modifier> = modifiersListToModifiersSet(psiClass.getModifierList())
        val fields: List<Field> = fieldsToFieldList(psiClass.getFields(), psiClass)
        val typeParameters: List<Element> = elementsToElementList(psiClass.getTypeParameters())
        val implementsTypes: List<Type> = typesToNotNullableTypeList(psiClass.getImplementsListTypes())
        val extendsTypes: List<Type> = typesToNotNullableTypeList(psiClass.getExtendsListTypes())
        val name: Identifier = Identifier(psiClass.getName()!!)
        val baseClassParams = ArrayList<Expression>()
        val members = getMembers(psiClass)
        val docComments = getDocComments(psiClass)
        val visitor: SuperVisitor = SuperVisitor()
        psiClass.accept(visitor)
        val resolvedSuperCallParameters = visitor.resolvedSuperCallParameters
        if (resolvedSuperCallParameters.size() == 1) {
            val psiExpressionList = resolvedSuperCallParameters.iterator().next()
            baseClassParams.addAll(expressionsToExpressionList(psiExpressionList.getExpressions()))
        }

        if (!psiClass.isEnum() && !psiClass.isInterface() && psiClass.getConstructors().size > 1 &&
        getPrimaryConstructorForThisCase(psiClass) == null) {
            val finalOrWithEmptyInitializer: List<Field> = getFinalOrWithEmptyInitializer(fields)
            val initializers = HashMap<String, String>()
            for (m in members) {
                if (m is Constructor) {
                    if (!m.isPrimary) {
                        for (fo in finalOrWithEmptyInitializer) {
                            val init = getDefaultInitializer(fo)
                            initializers.put(fo.identifier.toKotlin(), init)
                        }
                        val newStatements = ArrayList<Element>()
                        for (s in m.block!!.statements) {
                            var isRemoved: Boolean = false
                            if (s is AssignmentExpression) {
                                val assignee = s.left
                                if (assignee is CallChainExpression) {
                                    for (fo : Field in finalOrWithEmptyInitializer) {
                                        val id: String = fo.identifier.toKotlin()
                                        if (assignee.identifier.toKotlin().endsWith("." + id)) {
                                            initializers.put(id, s.right.toKotlin())
                                            isRemoved = true
                                        }

                                    }
                                }

                            }

                            if (!isRemoved) {
                                newStatements.add(s)
                            }

                        }
                        newStatements.add(0, DummyStringExpression("val __ = " + createPrimaryConstructorInvocation(name.toKotlin(), finalOrWithEmptyInitializer, initializers)))
                        m.block = Block(newStatements)
                    }
                }
            }
            members.add(Constructor(this, Identifier.EMPTY_IDENTIFIER, arrayListOf(), Collections.emptySet<Modifier>(),
                                    ClassType(name, Collections.emptyList<Element>(), false, this),
                                    Collections.emptyList<Element>(),
                                    ParameterList(createParametersFromFields(finalOrWithEmptyInitializer)),
                                    Block(createInitStatementsFromFields(finalOrWithEmptyInitializer)),
                                    true))
        }

        if (psiClass.isInterface()) {
            return Trait(this, name, docComments, modifiers, typeParameters, extendsTypes, Collections.emptyList<Expression>(), implementsTypes, members)
        }

        if (psiClass.isEnum()) {
            return Enum(this, name, docComments, modifiers, typeParameters, Collections.emptyList<Type>(), Collections.emptyList<Expression>(), implementsTypes, members)
        }

        return Class(this, name, docComments, modifiers, typeParameters, extendsTypes, baseClassParams, implementsTypes, members)
    }

    private fun initializerToInitializer(i: PsiClassInitializer): Initializer {
        return Initializer(blockToBlock(i.getBody(), true), modifiersListToModifiersSet(i.getModifierList()))
    }

    private fun fieldsToFieldList(fields: Array<PsiField>, psiClass: PsiClass): List<Field> {
        return fields.map { fieldToField(it, psiClass) }
    }

    private fun fieldToField(field: PsiField, psiClass: PsiClass?): Field {
        val modifiers = modifiersListToModifiersSet(field.getModifierList())
        val docComments = getDocComments(field)
        if (field is PsiEnumConstant) {
            return EnumConstant(Identifier(field.getName()!!),
                                docComments,
                                modifiers,
                                typeToType(field.getType()),
                                elementToElement(field.getArgumentList()))
        }

        var kType = typeToType(field.getType(), isAnnotatedAsNotNull(field.getModifierList()))
        if (field.hasModifierProperty(PsiModifier.FINAL) && isDefinitelyNotNull(field.getInitializer())) {
            kType = kType.convertedToNotNull();
        }

        return Field(Identifier(field.getName()!!),
                     docComments,
                     modifiers,
                     kType,
                     expressionToExpression(field.getInitializer(), field.getType()),
                     countWritingAccesses(field, psiClass))
    }

    private fun methodToFunction(method: PsiMethod): Function {
        return methodToFunction(method, true)
    }

    private fun methodToFunction(method: PsiMethod, notEmpty: Boolean): Function {
        if (diretlyOverridesMethodFromObject(method)) {
            dispatcher.expressionVisitor = ExpressionVisitorForDirectObjectInheritors(this)
        }
        else {
            dispatcher.expressionVisitor = ExpressionVisitor(this)
        }
        methodReturnType = method.getReturnType()
        val identifier: Identifier = Identifier(method.getName())
        val returnType: Type = typeToType(method.getReturnType(), isAnnotatedAsNotNull(method.getModifierList()))
        val body = blockToBlock(method.getBody(), notEmpty)

        val params: Element = createFunctionParameters(method)
        val typeParameters = elementsToElementList(method.getTypeParameters())
        val modifiers = modifiersListToModifiersSet(method.getModifierList())
        val docComments = getDocComments(method)
        if (isOverride(method)) {
            modifiers.add(Modifier.OVERRIDE)
        }

        val containingClass = method.getContainingClass()
        if (containingClass != null && containingClass.isInterface()) {
            modifiers.remove(Modifier.ABSTRACT)
        }

        if (isNotOpenMethod(method)) {
            modifiers.add(Modifier.NOT_OPEN)
        }

        if (method.isConstructor()) {
            return Constructor(this, identifier, docComments, modifiers, returnType, typeParameters, params,
                               Block(removeEmpty(body.statements), false), isConstructorPrimary(method))
        }

        return Function(this, identifier, docComments, modifiers, returnType, typeParameters, params, body)
    }

    private fun createFunctionParameters(method: PsiMethod): ParameterList {
        val result = ArrayList<Parameter>()
        for (parameter : PsiParameter? in method.getParameterList().getParameters()) {
            result.add(Parameter(Identifier(parameter?.getName()!!),
                                 typeToType(parameter?.getType(),
                                            isAnnotatedAsNotNull(parameter?.getModifierList())),
                                 isReadOnly(parameter, method.getBody())))
        }
        return ParameterList(result)
    }

    public fun blockToBlock(block: PsiCodeBlock?, notEmpty: Boolean): Block {
        if (block == null)
            return Block.EMPTY_BLOCK

        return Block(removeEmpty(statementsToStatementList(block.getChildren())), notEmpty)
    }

    public fun blockToBlock(block: PsiCodeBlock?): Block {
        return blockToBlock(block, true)
    }

    public fun statementsToStatementList(statements: Array<PsiElement>): List<Element> {
        return statements.filterNot { it is PsiWhiteSpace }.map { statementToStatement(it) }
    }

    public fun statementsToStatementList(statements: List<PsiElement>): List<Element> {
        return statements.filterNot { it is PsiWhiteSpace }.map { statementToStatement(it) }
    }

    public fun statementToStatement(s: PsiElement?): Element {
        if (s == null)
            return Statement.EMPTY_STATEMENT

        val statementVisitor: StatementVisitor = StatementVisitor(this)
        s.accept(statementVisitor)
        return statementVisitor.getResult()
    }

    public fun expressionsToExpressionList(expressions: Array<PsiExpression>): List<Expression> {
        val result = ArrayList<Expression>()
        for (e : PsiExpression? in expressions)
            result.add(expressionToExpression(e))
        return result
    }

    public fun expressionToExpression(e: PsiExpression?): Expression {
        if (e == null)
            return Expression.EMPTY_EXPRESSION

        val expressionVisitor: ExpressionVisitor = dispatcher.expressionVisitor
        e.accept(expressionVisitor)
        return expressionVisitor.getResult()
    }

    public fun elementToElement(e: PsiElement?): Element {
        if (e == null)
            return Element.EMPTY_ELEMENT

        val elementVisitor: ElementVisitor = ElementVisitor(this)
        e.accept(elementVisitor)
        return elementVisitor.getResult()
    }

    public fun elementsToElementList(elements: Array<out PsiElement?>): List<Element> {
        val result = ArrayList<Element>()
        for (element in elements) {
            result.add(elementToElement(element))
        }
        return result
    }

    public fun typeElementToTypeElement(element: PsiTypeElement?): TypeElement {
        return TypeElement(if (element == null)
                               EmptyType()
                           else
                               typeToType(element.getType()))
    }

    public fun typeToType(`type`: PsiType?): Type {
        if (`type` == null)
            return EmptyType()

        val typeVisitor: TypeVisitor = TypeVisitor(this)
        `type`.accept<Type>(typeVisitor)
        return typeVisitor.getResult()
    }

    public fun typesToTypeList(types: Array<PsiType>): List<Type> {
        return types.map { typeToType(it) }
    }

    public fun typeToType(`type`: PsiType?, notNull: Boolean): Type {
        val result: Type = typeToType(`type`)
        if (notNull) {
            return result.convertedToNotNull()
        }

        return result
    }

    private fun typesToNotNullableTypeList(types: Array<out PsiType?>): List<Type> {
        val result = ArrayList<Type>()
        for (aType in types) {
            result.add(typeToType(aType).convertedToNotNull())
        }
        return result
    }

    public fun parametersToParameterList(parameters: Array<PsiParameter>): List<Parameter?> {
        return parameters.map { parameterToParameter(it) }
    }

    public fun parameterToParameter(parameter: PsiParameter, forceNotNull: Boolean = false): Parameter {
        return Parameter(Identifier(parameter.getName()!!),
                         typeToType(parameter.getType(),
                                    forceNotNull || isAnnotatedAsNotNull(parameter.getModifierList())), true)
    }

    public fun argumentsToExpressionList(expression: PsiCallExpression): List<Expression> {
        val argumentList: PsiExpressionList? = expression.getArgumentList()
        val arguments: Array<PsiExpression> = (if (argumentList != null)
            argumentList.getExpressions()
        else
            PsiExpression.EMPTY_ARRAY)
        val result = ArrayList<Expression>()
        val resolved: PsiMethod? = expression.resolveMethod()
        val expectedTypes = ArrayList<PsiType?>()
        if (resolved != null) {
            for (p : PsiParameter? in resolved.getParameterList().getParameters())
                expectedTypes.add(p?.getType())
        }

        if (arguments.size == expectedTypes.size()) {
            for (i in 0..expectedTypes.size() - 1) result.add(expressionToExpression(arguments[i], expectedTypes.get(i)))
        }
        else {
            for (argument : PsiExpression? in arguments) {
                result.add(expressionToExpression(argument))
            }
        }
        return result
    }

    public fun expressionToExpression(argument: PsiExpression?, expectedType: PsiType?): Expression {
        if (argument == null)
            return Identifier.EMPTY_IDENTIFIER

        var expression: Expression = expressionToExpression(argument)
        val actualType: PsiType? = argument.getType()
        val isPrimitiveTypeOrNull: Boolean = actualType == null || actualType is PsiPrimitiveType
        if (isPrimitiveTypeOrNull && expression.isNullable()) {
            expression = BangBangExpression(expression)
        }
        else if (expectedType is PsiPrimitiveType && actualType is PsiClassType) {
            if (PsiPrimitiveType.getUnboxedType(actualType) == expectedType) {
                expression = BangBangExpression(expression)
            }
        }

        if (actualType != null) {
            if (isConversionNeeded(actualType, expectedType) && !(expression is LiteralExpression))
            {
                val conversion: String? = PRIMITIVE_TYPE_CONVERSIONS.get(expectedType?.getCanonicalText())
                if (conversion != null) {
                    expression = MethodCallExpression.build(expression, conversion)
                }
            }

        }

        return expression
    }

    public fun quoteKeywords(packageName: String): String {
        return packageName.split("\\.").map { Identifier(it).toKotlin() }.makeString(".")
    }

    private fun getFinalOrWithEmptyInitializer(fields: List<Field>): List<Field> {
        val result = ArrayList<Field>()
        for (f : Field in fields)
            if (f.isVal() || f.initializer.toKotlin().isEmpty()) {
                result.add(f)
            }

        return result
    }

    private fun createParametersFromFields(fields: List<Field>): List<Parameter> {
        return fields.map { Parameter(Identifier("_" + it.identifier.name), it.`type`, true) }
    }

    private fun createInitStatementsFromFields(fields: List<Field>): List<Element> {
        val result = ArrayList<Element>()
        for (f : Field in fields) {
            val identifierToKotlin: String? = f.identifier.toKotlin()
            result.add(DummyStringExpression(identifierToKotlin + " = " + "_" + identifierToKotlin))
        }
        return result
    }

    private fun createPrimaryConstructorInvocation(s: String, fields: List<Field>, initializers: Map<String, String>): String {
        return s + "(" + fields.map { initializers[it.identifier.toKotlin()] }.makeString(", ") + ")"
    }

    private fun removeEmpty(statements: List<Element>): List<Element> {
        return statements.filterNot {
            it == Statement.EMPTY_STATEMENT ||
            it == Expression.EMPTY_EXPRESSION ||
            it == Element.EMPTY_ELEMENT
        }
    }

    public fun identifierToIdentifier(identifier: PsiIdentifier?): Identifier {
        if (identifier == null)
            return Identifier.EMPTY_IDENTIFIER

        return Identifier(identifier.getText()!!)
    }

    public fun modifiersListToModifiersSet(modifierList: PsiModifierList?): MutableSet<Modifier> {
        val modifiersSet: HashSet<Modifier> = hashSetOf()
        if (modifierList != null) {
            if (modifierList.hasExplicitModifier(PsiModifier.ABSTRACT))
                modifiersSet.add(Modifier.ABSTRACT)

            if (modifierList.hasModifierProperty(PsiModifier.FINAL))
                modifiersSet.add(Modifier.FINAL)

            if (modifierList.hasModifierProperty(PsiModifier.STATIC))
                modifiersSet.add(Modifier.STATIC)

            if (modifierList.hasExplicitModifier(PsiModifier.PUBLIC))
                modifiersSet.add(Modifier.PUBLIC)

            if (modifierList.hasExplicitModifier(PsiModifier.PROTECTED))
                modifiersSet.add(Modifier.PROTECTED)

            if (modifierList.hasExplicitModifier(PsiModifier.PACKAGE_LOCAL))
                modifiersSet.add(Modifier.INTERNAL)

            if (modifierList.hasExplicitModifier(PsiModifier.PRIVATE))
                modifiersSet.add(Modifier.PRIVATE)
        }

        return modifiersSet
    }

    private fun isConversionNeeded(actual: PsiType?, expected: PsiType?): Boolean {
        if (actual == null || expected == null) {
            return false
        }

        val typeMap = HashMap<String, String>()
        typeMap.put(JAVA_LANG_BYTE, "byte")
        typeMap.put(JAVA_LANG_SHORT, "short")
        typeMap.put(JAVA_LANG_INTEGER, "int")
        typeMap.put(JAVA_LANG_LONG, "long")
        typeMap.put(JAVA_LANG_FLOAT, "float")
        typeMap.put(JAVA_LANG_DOUBLE, "double")
        typeMap.put(JAVA_LANG_CHARACTER, "char")
        val expectedStr: String? = expected.getCanonicalText()
        val actualStr: String? = actual.getCanonicalText()
        val o1: Boolean = expectedStr == typeMap[actualStr]
        val o2: Boolean = actualStr == typeMap[expectedStr]
        return actualStr != expectedStr && (!(o1 xor o2))
    }
}

public val NOT_NULL_ANNOTATIONS: Set<String> = ImmutableSet.of<String>("org.jetbrains.annotations.NotNull", "com.sun.istack.internal.NotNull", "javax.annotation.Nonnull")!!
public val PRIMITIVE_TYPE_CONVERSIONS: Map<String, String> = ImmutableMap.builder<String, String>()
?.put("byte", BYTE.asString())
?.put("short", SHORT.asString())
?.put("int", INT.asString())
?.put("long", LONG.asString())
?.put("float", FLOAT.asString())
?.put("double", DOUBLE.asString())
?.put("char", CHAR.asString())
?.put(JAVA_LANG_BYTE, BYTE.asString())
?.put(JAVA_LANG_SHORT, SHORT.asString())
?.put(JAVA_LANG_INTEGER, INT.asString())
?.put(JAVA_LANG_LONG, LONG.asString())
?.put(JAVA_LANG_FLOAT, FLOAT.asString())
?.put(JAVA_LANG_DOUBLE, DOUBLE.asString())
?.put(JAVA_LANG_CHARACTER, CHAR.asString())
?.build()!!


public fun countWritingAccesses(element: PsiElement?, container: PsiElement?): Int {
    var counter: Int = 0
    if (container != null) {
        val visitor: ReferenceCollector = ReferenceCollector()
        container.accept(visitor)
        for (e : PsiReferenceExpression in visitor.getCollectedReferences())
            if (e.isReferenceTo(element) && PsiUtil.isAccessedForWriting(e)) {
                counter++
            }
    }

    return counter
}

open class ReferenceCollector() : JavaRecursiveElementVisitor() {
    private val myCollectedReferences = ArrayList<PsiReferenceExpression>()

    public open fun getCollectedReferences(): List<PsiReferenceExpression> {
        return myCollectedReferences
    }

    public override fun visitReferenceExpression(expression: PsiReferenceExpression?) {
        super.visitReferenceExpression(expression)
        if (expression != null) {
            myCollectedReferences.add(expression)
        }
    }
}

public fun isReadOnly(element: PsiElement?, container: PsiElement?): Boolean {
    return countWritingAccesses(element, container) == 0
}

public fun isAnnotatedAsNotNull(modifierList: PsiModifierList?): Boolean {
    if (modifierList != null) {
        val annotations: Array<PsiAnnotation> = modifierList.getAnnotations()
        for (a : PsiAnnotation in annotations) {
            val qualifiedName: String? = a.getQualifiedName()
            if (qualifiedName != null && NOT_NULL_ANNOTATIONS.contains(qualifiedName)) {
                return true
            }
        }
    }
    return false
}

public fun isDefinitelyNotNull(element: PsiElement?): Boolean = when(element) {
    is PsiLiteralExpression -> element.getValue() != null
    is PsiNewExpression -> true
    else -> false
}

public fun getDefaultInitializer(f: Field): String {
    if (f.`type`.nullable) {
        return "null"
    }
    else {
        val typeToKotlin = f.`type`.toKotlin()
        if (typeToKotlin.equals("Boolean"))
            return "false"

        if (typeToKotlin.equals("Char"))
            return "' '"

        if (typeToKotlin.equals("Double"))
            return "0." + OperatorConventions.DOUBLE + "()"

        if (typeToKotlin.equals("Float"))
            return "0." + OperatorConventions.FLOAT + "()"

        return "0"
    }
}