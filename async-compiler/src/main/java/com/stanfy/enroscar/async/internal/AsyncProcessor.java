package com.stanfy.enroscar.async.internal;

import com.stanfy.enroscar.async.Async;
import com.stanfy.enroscar.async.Load;
import com.stanfy.enroscar.async.Send;
import com.stanfy.enroscar.async.rx.RxLoad;
import com.stanfy.enroscar.async.rx.RxSend;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import static javax.tools.Diagnostic.Kind.ERROR;

/**
 * @author Roman Mazur - Stanfy (http://stanfy.com)
 */
public final class AsyncProcessor extends AbstractProcessor {

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return new HashSet<>(Arrays.asList(
        Load.class.getCanonicalName(),
        Send.class.getCanonicalName(),
        RxLoad.class.getCanonicalName(),
        RxSend.class.getCanonicalName()
    ));
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(final Set<? extends TypeElement> annotations,
                         final RoundEnvironment roundEnv) {
    Map<TypeElement, List<MethodData>> classMethods =
        new LinkedHashMap<>();
    collectAndValidate(classMethods, Load.class, roundEnv);
    collectAndValidate(classMethods, Send.class, roundEnv);
    collectAndValidate(classMethods, RxLoad.class, roundEnv);
    collectAndValidate(classMethods, RxSend.class, roundEnv);

    for (Map.Entry<TypeElement, List<MethodData>> e : classMethods.entrySet()) {
      generateCode(e.getKey(), e.getValue());
    }

    return false;
  }

  private void collectAndValidate(final Map<TypeElement, List<MethodData>> classMethods,
                                  final Class<? extends Annotation> annotation,
                                  final RoundEnvironment roundEnv) {
    for (Element m : roundEnv.getElementsAnnotatedWith(annotation)) {
      Element encl = m.getEnclosingElement();
      if (!(m instanceof ExecutableElement)) {
        // this can happen in case of compilation errors
        // just skip it
        continue;
      }
      if (!(encl instanceof TypeElement)) {
        throw new IllegalStateException(m + " annotated with @" + annotation.getSimpleName()
            + " in " + encl + ". Enclosing element is not a type.");
      }

      ExecutableElement method = (ExecutableElement) m;

      TypeElement type = (TypeElement) encl;

      String returnType = GenUtils.getReturnType(method);

      TypeSupport operatorTypeSupport = null;
      if (returnType.startsWith(Async.class.getName().concat("<"))) {
        operatorTypeSupport = TypeSupport.ASYNC;
      } else if (returnType.startsWith(TypeSupport.RX_OBSERVABLE_CLASS.concat("<"))) {
        operatorTypeSupport = TypeSupport.RX;
      }

      if (operatorTypeSupport == null) {
        error(method, "Method annotated with @" + annotation.getSimpleName()
            + " must return either Async<T> or rx.Observable<T>");
        continue;
      }

      TypeSupport loaderDescriptionTypeSupport = operatorTypeSupport;
      if (annotation == RxLoad.class || annotation == RxSend.class) {
        loaderDescriptionTypeSupport = TypeSupport.RX;
      }

      List<MethodData> methods = classMethods.get(type);
      if (methods == null) {
        methods = new ArrayList<>();
        classMethods.put(type, methods);
      }
      methods.add(new MethodData(method, operatorTypeSupport, loaderDescriptionTypeSupport));
    }
  }

  private void generateCode(final TypeElement baseType, final List<MethodData> methods) {
    new LoaderDescriptionGenerator(processingEnv, baseType, methods).generateCode();
    new OperatorGenerator(processingEnv, baseType, methods).generateCode();
  }

  private void error(final Element element, final String message) {
    processingEnv.getMessager().printMessage(ERROR, message, element);
  }

}
