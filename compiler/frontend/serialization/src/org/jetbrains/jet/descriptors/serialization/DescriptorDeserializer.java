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

package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl;
import org.jetbrains.jet.lang.types.Variance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.descriptors.serialization.ProtoBuf.*;

public class DescriptorDeserializer {
    private final DeclarationDescriptor containingDeclaration;
    private final NameResolver nameResolver;
    private final TypeDeserializer typeDeserializer;

    public DescriptorDeserializer(
            @NotNull TypeDeserializer typeDeserializer,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        this.typeDeserializer = typeDeserializer;
        this.containingDeclaration = containingDeclaration;
        this.nameResolver = nameResolver;
    }

    public DescriptorDeserializer(
            @Nullable DescriptorDeserializer parent,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull NameResolver nameResolver
    ) {
        this(
                new TypeDeserializer(parent == null ? null : parent.typeDeserializer, nameResolver),
                containingDeclaration,
                nameResolver
        );
    }

    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    public NameResolver getNameResolver() {
        return nameResolver;
    }

    @NotNull
    public TypeDeserializer getTypeDeserializer() {
        return typeDeserializer;
    }

    @NotNull
    private DescriptorDeserializer createChildDeserializer(@NotNull DeclarationDescriptor descriptor) {
        return new DescriptorDeserializer(this, descriptor, nameResolver);
    }

    @NotNull
    public FunctionDescriptor loadFunction(@NotNull Callable proto) {
        // TODO: assert function flag

        SimpleFunctionDescriptorImpl function = new SimpleFunctionDescriptorImpl(
                containingDeclaration,
                // TODO: annotations
                Collections.<AnnotationDescriptor>emptyList(),
                nameResolver.getName(proto.getName()),
                // TODO: kind
                CallableMemberDescriptor.Kind.DECLARATION
        );
        DescriptorDeserializer local = new DescriptorDeserializer(this, function, nameResolver);
        function.initialize(
                local.typeDeserializer.typeOrNull(proto.hasReceiverType() ? proto.getReceiverType() : null),
                // TODO: expectedThisObject
                null,
                local.typeParameters(proto.getTypeParametersList()),
                local.valueParameters(proto.getValueParametersList()),
                local.typeDeserializer.type(proto.getReturnType()),
                // TODO: modality
                Modality.OPEN,
                // TODO: visibility
                Visibilities.PUBLIC,
                // TODO: inline
                false

        );
        return function;
    }

    @NotNull
    public List<TypeParameterDescriptor> typeParameters(@NotNull List<TypeParameter> protos) {
        List<TypeParameterDescriptorImpl> result = new ArrayList<TypeParameterDescriptorImpl>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            TypeParameter proto = protos.get(i);
            TypeParameterDescriptorImpl descriptor = typeParameter(proto, i);
            result.add(descriptor);
        }
        // Account for circular bounds:
        for (int i = 0; i < protos.size(); i++) {
            TypeParameter proto = protos.get(i);
            TypeParameterDescriptorImpl descriptor = result.get(i);
            addTypeParameterBounds(proto, descriptor);
        }
        //noinspection unchecked
        return (List) result;
    }

    private TypeParameterDescriptorImpl typeParameter(TypeParameter proto, int index) {
        int id = proto.getId();
        TypeParameterDescriptorImpl descriptor = TypeParameterDescriptorImpl.createForFurtherModification(
                containingDeclaration,
                // TODO
                Collections.<AnnotationDescriptor>emptyList(),
                proto.getReified(),
                variance(proto.getVariance()),
                nameResolver.getName(proto.getName()),
                index);
        typeDeserializer.registerTypeParameter(id, descriptor);
        return descriptor;
    }

    private void addTypeParameterBounds(TypeParameter proto, TypeParameterDescriptorImpl descriptor) {
        if (proto.getUpperBoundsCount() == 0) {
            descriptor.addDefaultUpperBound();
        }
        else {
            for (Type upperBound : proto.getUpperBoundsList()) {
                descriptor.addUpperBound(typeDeserializer.type(upperBound));
            }
        }
        descriptor.setInitialized();
    }

    private static Variance variance(TypeParameter.Variance proto) {
        switch (proto) {
            case IN:
                return Variance.IN_VARIANCE;
            case OUT:
                return Variance.OUT_VARIANCE;
            case INV:
                return Variance.INVARIANT;
        }
        throw new IllegalStateException("Unknown projection: " + proto);
    }

    @NotNull
    private List<ValueParameterDescriptor> valueParameters(@NotNull List<Callable.ValueParameter> protos) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>(protos.size());
        for (int i = 0; i < protos.size(); i++) {
            Callable.ValueParameter proto = protos.get(i);
            result.add(valueParameter(proto, i));
        }
        return result;
    }

    private ValueParameterDescriptor valueParameter(Callable.ValueParameter proto, int index) {
        return new ValueParameterDescriptorImpl(
                containingDeclaration,
                index,
                // TODO
                Collections.<AnnotationDescriptor>emptyList(),
                nameResolver.getName(proto.getName()),
                typeDeserializer.type(proto.getType()),
                // TODO: declaresDefaultValue
                false,
                typeDeserializer.typeOrNull(proto.hasVarargElementType() ? proto.getVarargElementType() : null));
    }
}