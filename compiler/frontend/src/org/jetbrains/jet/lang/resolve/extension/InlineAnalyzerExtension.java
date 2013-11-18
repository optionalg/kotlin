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

package org.jetbrains.jet.lang.resolve.extension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.FunctionAnalyzerExtension;

import java.util.List;

public class InlineAnalyzerExtension implements FunctionAnalyzerExtension.AnalyzerExtension {

    public static final InlineAnalyzerExtension INSTANCE = new InlineAnalyzerExtension();

    private InlineAnalyzerExtension() {

    }

    @Override
    public void process(
            @NotNull final FunctionDescriptor descriptor, @NotNull JetNamedFunction function, @NotNull final BindingTrace trace
    ) {
        assert descriptor instanceof SimpleFunctionDescriptor && ((SimpleFunctionDescriptor) descriptor).isInline() :
                "This method should be invoced on inline function: " + descriptor;

        checkDefaults(descriptor, function, trace);

        JetVisitorVoid visitor = new JetVisitorVoid() {

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                super.visitJetElement(element);
                element.acceptChildren(this);
            }

            @Override
            public void visitClass(@NotNull JetClass klass) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(klass, klass, descriptor));
            }

            @Override
            public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(declaration, declaration, descriptor));
            }

            @Override
            public void visitNamedFunction(@NotNull JetNamedFunction function) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(function, function, descriptor));
            }
        };

        function.acceptChildren(visitor);
    }

    private static void checkDefaults(
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetFunction function,
            @NotNull BindingTrace trace
    ) {
        List<JetParameter> parameters = function.getValueParameters();
        for (JetParameter parameter : parameters) {
            if (parameter.getDefaultValue() != null) {
                trace.report(Errors.NOT_YET_SUPPORTED_IN_INLINE.on(parameter, parameter, functionDescriptor));
            }
        }
    }
}
