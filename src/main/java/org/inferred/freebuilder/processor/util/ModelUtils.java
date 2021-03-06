/*
 * Copyright 2014 Google Inc. All rights reserved.
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
package org.inferred.freebuilder.processor.util;

import static javax.lang.model.util.ElementFilter.methodsIn;

import com.google.common.base.Optional;

import java.lang.annotation.Annotation;
import java.util.Map.Entry;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.util.SimpleElementVisitor6;
import javax.lang.model.util.SimpleTypeVisitor6;
import javax.lang.model.util.Types;

/**
 * Utility methods for the javax.lang.model package.
 */
public class ModelUtils {

  /**
   * Returns an {@link AnnotationMirror} for the annotation of type {@code annotationClass} on
   * {@code element}, or {@link Optional#absent()} if no such annotation exists.
   */
  public static Optional<AnnotationMirror> findAnnotationMirror(
      Element element, Class<? extends Annotation> annotationClass) {
    return findAnnotationMirror(element, Shading.unshadedName(annotationClass.getName()));
  }

  /**
   * Returns an {@link AnnotationMirror} for the annotation of type {@code annotationClass} on
   * {@code element}, or {@link Optional#absent()} if no such annotation exists.
   */
  public static Optional<AnnotationMirror> findAnnotationMirror(
      Element element, QualifiedName annotationClass) {
    return findAnnotationMirror(element, Shading.unshadedName(annotationClass.toString()));
  }

  /**
   * Returns an {@link AnnotationMirror} for the annotation of type {@code annotationClassName} on
   * {@code element}, or {@link Optional#absent()} if no such annotation exists.
   */
  public static Optional<AnnotationMirror> findAnnotationMirror(
      Element element, String annotationClassName) {
    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      TypeElement annotationTypeElement =
          (TypeElement) (annotationMirror.getAnnotationType().asElement());
      if (annotationTypeElement.getQualifiedName().contentEquals(annotationClassName)) {
        return Optional.of(annotationMirror);
      }
    }
    return Optional.absent();
  }

  public static Optional<AnnotationValue> findProperty(
      AnnotationMirror annotation, String propertyName) {
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> element
        : annotation.getElementValues().entrySet()) {
      if (element.getKey().getSimpleName().contentEquals(propertyName)) {
        return Optional.<AnnotationValue>of(element.getValue());
      }
    }
    return Optional.absent();
  }

  /** Returns {@code element} as a {@link TypeElement}, if it is one. */
  public static Optional<TypeElement> maybeType(Element element) {
    return TYPE_ELEMENT_VISITOR.visit(element);
  }

  /** Returns {@code type} as a {@link DeclaredType}, if it is one. */
  public static Optional<DeclaredType> maybeDeclared(TypeMirror type) {
    return DECLARED_TYPE_VISITOR.visit(type);
  }

  public static Optional<TypeVariable> maybeVariable(TypeMirror type) {
    return TYPE_VARIABLE_VISITOR.visit(type);
  }

  /** Returns the {@link TypeElement} corresponding to {@code type}, if there is one. */
  public static Optional<TypeElement> maybeAsTypeElement(TypeMirror type) {
    Optional<DeclaredType> declaredType = maybeDeclared(type);
    if (declaredType.isPresent()) {
      return maybeType(declaredType.get().asElement());
    } else {
      return Optional.absent();
    }
  }

  /** Returns the {@link TypeElement} corresponding to {@code type}. */
  public static TypeElement asElement(DeclaredType type) {
    return maybeType(type.asElement()).get();
  }

  /** Applies unboxing conversion to {@code mirror}, if it can be unboxed. */
  public static Optional<TypeMirror> maybeUnbox(TypeMirror mirror, Types types) {
    try {
      return Optional.<TypeMirror>of(types.unboxedType(mirror));
    } catch (IllegalArgumentException e) {
      return Optional.absent();
    }
  }

  /** Returns whether {@code type} overrides method {@code methodName(params)}. */
  public static boolean overrides(
      TypeElement type, Types types, String methodName, TypeMirror... params) {
    for (ExecutableElement method : methodsIn(type.getEnclosedElements())) {
      if (signatureMatches(method, types, methodName, params)) {
        return true;
      }
    }
    return false;
  }

  private static boolean signatureMatches(
      ExecutableElement method, Types types, String name, TypeMirror... params) {
    if (!method.getSimpleName().contentEquals(name)) {
      return false;
    }
    if (method.getParameters().size() != params.length) {
      return false;
    }
    for (int i = 0; i < params.length; ++i) {
      if (!types.isSameType(params[i], method.getParameters().get(i).asType())) {
        return false;
      }
    }
    return true;
  }

  private static final SimpleElementVisitor6<Optional<TypeElement>, ?> TYPE_ELEMENT_VISITOR =
      new SimpleElementVisitor6<Optional<TypeElement>, Void>() {

        @Override
        public Optional<TypeElement> visitType(TypeElement e, Void p) {
          return Optional.of(e);
        }

        @Override
        protected Optional<TypeElement> defaultAction(Element e, Void p) {
          return Optional.absent();
        }
      };

  private static final SimpleTypeVisitor6<Optional<DeclaredType>, ?> DECLARED_TYPE_VISITOR =
      new SimpleTypeVisitor6<Optional<DeclaredType>, Void>() {

        @Override
        public Optional<DeclaredType> visitDeclared(DeclaredType t, Void p) {
          return Optional.of(t);
        }

        @Override
        protected Optional<DeclaredType> defaultAction(TypeMirror e, Void p) {
          return Optional.absent();
        }
      };

  private static final SimpleTypeVisitor6<Optional<TypeVariable>, ?> TYPE_VARIABLE_VISITOR =
      new SimpleTypeVisitor6<Optional<TypeVariable>, Void>() {

        @Override
        public Optional<TypeVariable> visitTypeVariable(TypeVariable t, Void p) {
          return Optional.of(t);
        }

        @Override
        protected Optional<TypeVariable> defaultAction(TypeMirror e, Void p) {
          return Optional.absent();
        }
      };

  /**
   * Determines the return type of {@code method}, if called on an instance of type {@code type}.
   *
   * <p>For instance, in this example, myY.getProperty() returns List&lt;T&gt;, not T:<pre><code>
   *    interface X&lt;T&gt; {
   *      T getProperty();
   *    }
   *    &#64;FreeBuilder interface Y&lt;T&gt; extends X&lt;List&lt;T&gt;&gt; { }</code></pre>
   *
   * <p>(Unfortunately, a bug in Eclipse prevents us handling these cases correctly at the moment.
   * javac works fine.)
   */
  public static TypeMirror getReturnType(TypeElement type, ExecutableElement method, Types types) {
    try {
      ExecutableType executableType = (ExecutableType)
          types.asMemberOf((DeclaredType) type.asType(), method);
      return executableType.getReturnType();
    } catch (IllegalArgumentException e) {
      // Eclipse incorrectly throws an IllegalArgumentException here:
      //    "element is not valid for the containing declared type"
      // As a workaround for the common case, fall back to the declared return type.
      return method.getReturnType();
    }
  }

}
