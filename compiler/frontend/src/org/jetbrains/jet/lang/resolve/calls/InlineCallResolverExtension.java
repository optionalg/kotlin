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

package org.jetbrains.jet.lang.resolve.calls;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.calls.context.BasicCallResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.model.DefaultValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedValueArgument;
import org.jetbrains.jet.lang.resolve.calls.model.VarargValueArgument;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExtensionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.HashSet;
import java.util.Set;

public class InlineCallResolverExtension implements CallResolverExtension {

    private final SimpleFunctionDescriptor descriptor;

    private final Set<CallableDescriptor> inlinableParameters = new HashSet<CallableDescriptor>();

    private final boolean isEffectivelyPublicApiFunction;

    public InlineCallResolverExtension(@NotNull SimpleFunctionDescriptor descriptor) {
        assert descriptor.isInline() : "This extension should be created only for inline functions but not " + descriptor;
        this.descriptor = descriptor;
        this.isEffectivelyPublicApiFunction = isEffectivelyPublicApi(descriptor);

        for (ValueParameterDescriptor param : descriptor.getValueParameters()) {
            if (isInlinableParameter(param)) {
                inlinableParameters.add(param);
            }
        }

        //add extension receiver as inlinable
        ReceiverParameterDescriptor receiverParameter = descriptor.getReceiverParameter();
        if (receiverParameter != null) {
            if (isInlinableParameter(receiverParameter)) {
                inlinableParameters.add(receiverParameter);
            }
        }
    }

    @Override
    public <F extends CallableDescriptor> void run(
            @NotNull ResolvedCall<F> resolvedCall, @NotNull BasicCallResolutionContext context
    ) {
        JetExpression expression = context.call.getCalleeExpression();
        if (expression == null) {
            return;
        }

        //checking that only invoke or inlinable extension called on function parameter
        CallableDescriptor targetDescriptor = resolvedCall.getResultingDescriptor();
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getThisObject(), expression);
        checkCallWithReceiver(context, targetDescriptor, resolvedCall.getReceiverArgument(), expression);

        if (inlinableParameters.contains(targetDescriptor)) {
            if (!couldAccessVariable(expression)) {
                context.trace.report(Errors.USAGE_IS_NOT_INLINABLE.on(expression, expression, descriptor));
            }
        }

        for (ResolvedValueArgument value : resolvedCall.getValueArguments().values()) {
            if (!(value instanceof DefaultValueArgument)) {
                for (ValueArgument argument : value.getArguments()) {
                    checkValueParameter(context, targetDescriptor, argument, value instanceof VarargValueArgument);
                }
            }
        }

        checkVisibility(targetDescriptor, expression, context);
    }

    private static boolean couldAccessVariable(JetExpression expression) {
        PsiElement parent = expression.getParent();
        while (parent != null) {
            if (parent instanceof JetValueArgument ||
                parent instanceof JetUnaryExpression ||
                parent instanceof JetBinaryExpression ||
                parent instanceof JetDotQualifiedExpression ||
                parent instanceof JetCallExpression) {
                //check that it's in inlineable call would be in resolve call of parent
                return true;
            }
            else if (parent instanceof JetParenthesizedExpression || parent instanceof JetBinaryExpressionWithTypeRHS) {
                parent = parent.getParent();
            }
            else if (parent instanceof JetPrefixExpression) {
                boolean isLabeled = JetPsiUtil.isLabeledExpression((JetPrefixExpression) parent);
                if (!isLabeled) {
                    return false;
                }
                parent = parent.getParent();
            }
            else {
                return false;
            }
        }
        return false;
    }

    private void checkValueParameter(BasicCallResolutionContext context, CallableDescriptor targetDescriptor, ValueArgument argument, boolean isVararg) {
        JetExpression jetExpression = argument.getArgumentExpression();
        if (jetExpression == null) {
            return;
        }
        CallableDescriptor varDescriptor = getDescriptor(context, jetExpression);

        if (varDescriptor != null && inlinableParameters.contains(varDescriptor)) {
            checkFunctionCall(context, targetDescriptor, jetExpression, isVararg);
        }
    }

    private void checkCallWithReceiver(
            @NotNull BasicCallResolutionContext context,
            @NotNull CallableDescriptor targetDescriptor,
            @NotNull ReceiverValue receiver,
            @Nullable JetExpression expression
    ) {
        if (!receiver.exists()) return;

        CallableDescriptor varDescriptor = null;
        JetExpression receiverExpression = null;
        if (receiver instanceof ExpressionReceiver) {
            receiverExpression = ((ExpressionReceiver) receiver).getExpression();
            varDescriptor = getDescriptor(context, receiverExpression);
        }
        else if (receiver instanceof ExtensionReceiver) {
            ExtensionReceiver extensionReceiver = (ExtensionReceiver) receiver;
            CallableDescriptor extension = extensionReceiver.getDeclarationDescriptor();

            varDescriptor = extension.getReceiverParameter();
            assert varDescriptor != null : "Extension should have receiverParameterDescriptor: " + extension;

            receiverExpression = expression;
        }

        if (inlinableParameters.contains(varDescriptor)) {
            //check that it's invoke or inlinable extension
            checkFunctionCall(context, targetDescriptor, receiverExpression, false);
        }
    }

    @Nullable
    private static CallableDescriptor getDescriptor(
            @NotNull BasicCallResolutionContext context,
            @NotNull JetExpression expression
    ) {
        ResolvedCall<? extends CallableDescriptor> thisCall = context.trace.get(BindingContext.RESOLVED_CALL, expression);
        return thisCall != null ? thisCall.getResultingDescriptor() : null;
    }

    private void checkFunctionCall(
            BasicCallResolutionContext context,
            CallableDescriptor targetDescriptor,
            JetExpression receiverExpresssion,
            boolean isVararg
    ) {
        boolean inlinableCall = isInvokeOrInlineExtension(targetDescriptor);
        if (!inlinableCall || isVararg) {
            context.trace.report(Errors.USAGE_IS_NOT_INLINABLE.on(receiverExpresssion, receiverExpresssion, descriptor));
        }
    }

    private static boolean isInlinableParameter(@NotNull CallableDescriptor descriptor) {
        JetType type = descriptor.getReturnType();
        return type != null &&
               KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(type) &&
               !type.isNullable() &&
               !InlineUtil.hasNoinlineAnnotation(descriptor);
    }

    private static boolean isInvokeOrInlineExtension(@NotNull CallableDescriptor descriptor) {
        return descriptor.getName().asString().equals("invoke") /*TODO check type*/ ||
               //or inline extension /*TODO*/
               (descriptor instanceof SimpleFunctionDescriptor) && ((SimpleFunctionDescriptor) descriptor).isInline();
    }

    private void checkVisibility(@NotNull CallableDescriptor declarationDescriptor, @NotNull JetElement expression, @NotNull BasicCallResolutionContext context){
        if (isEffectivelyPublicApiFunction && !isEffectivelyPublicApi(declarationDescriptor) && declarationDescriptor.getVisibility() != Visibilities.LOCAL) {
            context.trace.report(Errors.INVISIBLE_MEMBER_FROM_INLINE.on(expression, declarationDescriptor, descriptor));
        }
    }

    private static boolean isEffectivelyPublicApi(DeclarationDescriptorWithVisibility descriptor) {
        DeclarationDescriptorWithVisibility parent = descriptor;
        while (parent != null) {
            if (!parent.getVisibility().isPublicAPI()) {
                return false;
            }
            parent = DescriptorUtils.getParentOfType(parent, DeclarationDescriptorWithVisibility.class);
        }
        return true;
    }
}
