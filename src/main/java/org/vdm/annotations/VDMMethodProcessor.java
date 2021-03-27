package org.vdm.annotations;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.TypeElement;
import javax.lang.model.SourceVersion;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import org.w3c.dom.Document;

import javax.tools.Diagnostic;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import javax.lang.model.element.*;

@SupportedAnnotationTypes("org.vdm.annotations.VDMMethod")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@AutoService(Processor.class)
public class VDMMethodProcessor extends AbstractProcessor {
    private Map<String, List<Method>> classes = new HashMap<>();

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (TypeElement annotation : annotations) {
                Set<?> methods = roundEnv.getElementsAnnotatedWith(annotation);
                methods.stream().forEach(methodObject -> {
                    ExecutableElement method = (ExecutableElement) methodObject;
                    String methodName = method.getSimpleName().toString();
                    Map<String, TypeName> parameters = method.getParameters().stream()
                            .collect(Collectors.toMap(parameter -> parameter.getSimpleName().toString(),
                                    parameter -> TypeName.get(parameter.asType()), (u, v) -> {
                                        throw new IllegalStateException(String.format("Duplicate key %s", u));
                                    }, LinkedHashMap::new));
                    String methodClass = ((PackageElement) method.getEnclosingElement().getEnclosingElement())
                            .getQualifiedName().toString() + "."
                            + method.getEnclosingElement().getSimpleName().toString();
                    TypeName returnType = TypeName.get(method.getReturnType());
                    if (!classes.containsKey(methodClass)) {
                        classes.put(methodClass, new ArrayList<>());
                    }
                    classes.get(methodClass).add(new Method(methodName, parameters, returnType, methodClass));
                });
            }

            for (String classToAdd : classes.keySet()) {
                List<Method> methods = classes.get(classToAdd);
                List<MethodSpec> methodsToAdd = new ArrayList<>();

                for (Method methodToAdd : methods) {
                    MethodSpec.Builder builder = MethodSpec.methodBuilder(methodToAdd.getName())
                            .addModifiers(Modifier.PUBLIC).returns(methodToAdd.getReturnType());

                    Map<String, TypeName> parameters = methodToAdd.getParameters();
                    for (String parameter : parameters.keySet()) {
                        builder.addParameter(parameters.get(parameter), parameter);
                    }
                    String parametersName = parameters.keySet().stream().reduce("", (accumulator, parameter) -> {
                        if (!accumulator.isEmpty()) {
                            accumulator += ", ";
                        }
                        accumulator += parameter;
                        return accumulator;
                    });
                    String statement = "super." + methodToAdd.getName() + "(" + parametersName + ")";
                    if (methodToAdd.getReturnType() != TypeName.get(void.class)) {
                        statement = "return " + statement;
                    }
                    builder.addStatement(statement);
                    methodsToAdd.add(builder.build());
                }
                try {
                    String classNameToAdd = "VDM" + classToAdd.substring(classToAdd.lastIndexOf(".") + 1);
                    String packageName = "generated.vdm.java";
                    TypeSpec classSpec = TypeSpec.classBuilder(classNameToAdd).addModifiers(Modifier.PUBLIC)
                            .addMethods(methodsToAdd).superclass(Class.forName(classToAdd)).build();

                    JavaFile javaFile = JavaFile.builder(packageName, classSpec).build();

                    String javaSourcesPath = null;
                    String path = System.getProperty("user.dir").replace("\\", "/");
                    try {
                        FileInputStream fileIS = new FileInputStream(path + "/pom.xml");
                        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = builderFactory.newDocumentBuilder();
                        Document xmlDocument = builder.parse(fileIS);
                        XPath xPath = XPathFactory.newInstance().newXPath();
                        String expression = "/project/properties/sourcesPath[1]";
                        javaSourcesPath = (String) xPath.compile(expression).evaluate(xmlDocument,
                                XPathConstants.STRING);
                    } catch (Exception exception) {
                        writeWarning("Could not open " + path + "/pom.xml!");
                    }

                    if (javaSourcesPath != null && !javaSourcesPath.isEmpty()) {
                        path += "/" + javaSourcesPath + "/";
                    } else {
                        writeWarning("Din not found sourcesPath in the pom.xml file! The path " + path
                                + " will be used for sources generation");
                    }

                    javaFile.writeTo(Paths.get(path));
                    System.out.println("Java class " + classNameToAdd + " generated at " + path
                            + packageName.replace(".", "/") + "/");
                } catch (Exception e) {
                }
            }
        }
        return false;
    }

    private void writeError(String error) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
    }

    private void writeWarning(String warning) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, warning);
    }
}
