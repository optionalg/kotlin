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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaClassResolver;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaSubModule;
import org.jetbrains.jet.lang.resolve.java.resolver.*;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.storage.LockBasedStorageManager;

import javax.inject.Inject;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class JavaDescriptorResolver implements DependencyClassByQualifiedNameResolver {
    private static final boolean LAZY = true;

    public static final Name JAVA_ROOT = Name.special("<java_root>");

    private JavaClassResolver classResolver;
    private JavaNamespaceResolver namespaceResolver;
    private JavaClassFinder javaClassFinder;
    private ExternalAnnotationResolver externalAnnotationResolver;
    private LazyJavaSubModule subModule;
    private ExternalSignatureResolver externalSignatureResolver;
    private ErrorReporter errorReporter;
    private MethodSignatureChecker signatureChecker;
    private JavaResolverCache javaResolverCache;

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @Inject
    public void setNamespaceResolver(JavaNamespaceResolver namespaceResolver) {
        this.namespaceResolver = namespaceResolver;
    }

    @Inject
    public void setJavaClassFinder(JavaClassFinder javaClassFinder) {
        this.javaClassFinder = javaClassFinder;
    }

    @Inject
    public void setExternalAnnotationResolver(ExternalAnnotationResolver externalAnnotationResolver) {
        this.externalAnnotationResolver = externalAnnotationResolver;
    }

    @Inject
    public void setExternalSignatureResolver(ExternalSignatureResolver externalSignatureResolver) {
        this.externalSignatureResolver = externalSignatureResolver;
    }

    @Inject
    public void setErrorReporter(ErrorReporter errorReporter) {
        this.errorReporter = errorReporter;
    }

    @Inject
    public void setSignatureChecker(MethodSignatureChecker signatureChecker) {
        this.signatureChecker = signatureChecker;
    }

    @Inject
    public void setJavaResolverCache(JavaResolverCache javaResolverCache) {
        this.javaResolverCache = javaResolverCache;
    }

    @NotNull
    private LazyJavaSubModule getSubModule() {
        if (subModule == null) {
            subModule = new LazyJavaSubModule(
                    new GlobalJavaResolverContext(
                            new LockBasedStorageManager(),
                            javaClassFinder,
                            new LazyJavaClassResolver() {
                                @Override
                                public ClassDescriptor resolveClass(JavaClass aClass) {
                                    return null;
                                }

                                @Override
                                public ClassDescriptor resolveClassByFqName(FqName name) {
                                    return javaResolverCache.getClassResolvedFromSource(name);
                                }
                            },
                            externalAnnotationResolver,
                            externalSignatureResolver,
                            errorReporter,
                            signatureChecker,
                            javaResolverCache
                    ),
                    new ModuleDescriptorImpl(Name.special("<java module>"), Collections.<ImportPath>emptyList(), PlatformToKotlinClassMap.EMPTY)
            );
        }
        return subModule;
    }

    @Nullable
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        if (LAZY) {
            return getSubModule().getClass(qualifiedName);
        }
        return classResolver.resolveClass(qualifiedName, searchRule);
    }

    @Override
    public ClassDescriptor resolveClass(@NotNull FqName qualifiedName) {
        return classResolver.resolveClass(qualifiedName, IGNORE_KOTLIN_SOURCES);
    }

    @Nullable
    public NamespaceDescriptor resolveNamespace(@NotNull FqName qualifiedName, @NotNull DescriptorSearchRule searchRule) {
        if (LAZY) {
            return getSubModule().getPackageFragment(qualifiedName);
        }
        return namespaceResolver.resolveNamespace(qualifiedName, searchRule);
    }

    @Nullable
    public JetScope getJavaPackageScope(@NotNull NamespaceDescriptor namespaceDescriptor) {
        if (LAZY) {
            NamespaceDescriptor packageFragment = getSubModule().getPackageFragment(DescriptorUtils.getFQName(namespaceDescriptor).toSafe());
            if (packageFragment == null) {
                return null;
            }
            return packageFragment.getMemberScope();
        }
        return namespaceResolver.getJavaPackageScopeForExistingNamespaceDescriptor(namespaceDescriptor);
    }
}
