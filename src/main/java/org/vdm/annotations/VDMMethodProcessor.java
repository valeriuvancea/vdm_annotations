package org.vdm.annotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.TypeName;

import org.vdm.generators.JavaClassGenerator;

import javax.tools.Diagnostic;
import javax.lang.model.element.*;

@SupportedAnnotationTypes("org.vdm.annotations.VDMMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class VDMMethodProcessor extends AbstractProcessor {
    private Map<String, List<Method>> classes = new HashMap<>();
    private RoundEnvironment roundEnv = null;
    public static ProcessingEnvironment processingEnvironment = null;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            this.roundEnv = roundEnv;
            processingEnvironment = processingEnv;
            findClassesFromAnnotations(annotations);
            generateJavaClasses();
        }
        return false;
    }

    private void findClassesFromAnnotations(Set<? extends TypeElement> annotations) {
        for (TypeElement annotation : annotations) {
            Set<?> methods = roundEnv.getElementsAnnotatedWith(annotation);
            methods.stream().forEach(methodObject -> {
                ExecutableElement method = (ExecutableElement) methodObject;
                String methodName = method.getSimpleName().toString();
                Map<String, TypeName> parameters = method.getParameters().stream()
                        .collect(Collectors.toMap(parameter -> parameter.getSimpleName().toString(),
                                parameter -> TypeName.get(parameter.asType()), (key, value) -> {
                                    throw new IllegalStateException(String.format("Duplicate key %s", key));
                                }, LinkedHashMap::new));
                String methodClass = ((PackageElement) method.getEnclosingElement().getEnclosingElement())
                        .getQualifiedName().toString() + "." + method.getEnclosingElement().getSimpleName().toString();
                TypeName returnType = TypeName.get(method.getReturnType());
                if (!classes.containsKey(methodClass)) {
                    classes.put(methodClass, new ArrayList<>());
                }
                classes.get(methodClass).add(new Method(methodName, parameters, returnType, methodClass));
            });
        }
    }

    private void generateJavaClasses() {
        for (String classToAdd : classes.keySet()) {
            new JavaClassGenerator(classToAdd, classes.get(classToAdd)).generate();
        }
    }

    public static void writeNote(String note) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.OTHER, note);
    }

    public static void writeError(String error) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
    }

    public static void writeWarning(String warning) {
        processingEnvironment.getMessager().printMessage(Diagnostic.Kind.WARNING, warning);
    }
}
