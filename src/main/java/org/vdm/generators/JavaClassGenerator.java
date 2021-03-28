package org.vdm.generators;

import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import javax.lang.model.element.Modifier;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.overture.interpreter.values.Value;
import org.overture.interpreter.values.VoidValue;
import org.vdm.annotations.*;
import org.vdm.overture.VDMTypesHelper;
import org.w3c.dom.Document;

public class JavaClassGenerator implements IGenerator {
    private String className;
    private List<Method> methods;

    public JavaClassGenerator(String className, List<Method> methods) {
        this.className = className;
        this.methods = methods;
    }

    public void generate() {
        try {
            List<MethodSpec> methodsToAdd = generateMethodsForJavaClass(methods);
            String classNameToAdd = "VDM" + className.substring(className.lastIndexOf(".") + 1);
            String packageName = "generated.vdm";
            TypeSpec classSpec = TypeSpec.classBuilder(classNameToAdd).addModifiers(Modifier.PUBLIC)
                    .addMethods(methodsToAdd).superclass(Class.forName(className)).build();

            String path = System.getProperty("user.dir").replace("\\", "/");
            String javaSourcesPath = getJavaSourcePathFromPomXML(path);

            if (javaSourcesPath != null && !javaSourcesPath.isEmpty()) {
                path += "/" + javaSourcesPath + "/";
            } else {
                VDMMethodProcessor.writeWarning("Din not found sourcesPath in the pom.xml file! The path " + path
                        + " will be used for sources generation");
            }

            JavaFile javaFile = JavaFile.builder(packageName, classSpec).build();
            javaFile.writeTo(Paths.get(path));
            VDMMethodProcessor.writeNote(
                    "Java class " + classNameToAdd + " generated at " + path + packageName.replace(".", "/") + "/");
        } catch (Exception e) {
            VDMMethodProcessor.writeError(e.getMessage());
        }
    }

    private List<MethodSpec> generateMethodsForJavaClass(List<Method> methods) throws Exception {
        List<MethodSpec> methodsToAdd = new ArrayList<>();
        for (Method methodToAdd : methods) {
            MethodSpec.Builder builder = MethodSpec.methodBuilder(methodToAdd.getName()).addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(Value.class));

            Map<String, TypeName> parameters = methodToAdd.getParameters();
            for (String parameter : parameters.keySet()) {
                builder.addParameter(VDMTypesHelper.getVDMTypeNameFromJavaType(parameters.get(parameter)), parameter);
            }

            List<Object> statementTypes = new ArrayList<>();

            String parametersName = parameters.keySet().stream().reduce("", (accumulator, parameter) -> {
                if (!accumulator.isEmpty()) {
                    accumulator += ", ";
                }
                accumulator += "$T.getJavaValueFromVDMValue(" + parameter + ", \""
                        + parameters.get(parameter).toString() + "\")";
                statementTypes.add(VDMTypesHelper.class);
                return accumulator;
            });

            String statement = "super." + methodToAdd.getName() + "(" + parametersName + ")";
            if (methodToAdd.getReturnType() != TypeName.get(void.class)) {
                statementTypes.add(0, VDMTypesHelper.class);
                statement = "return $T.getVDMValueFromJavaValue(" + statement + ")";
            }

            builder.addStatement(statement, statementTypes.toArray());

            if (methodToAdd.getReturnType() == TypeName.get(void.class)) {
                builder.addStatement("return new $T()", VoidValue.class);
            }

            builder.addException(Exception.class);
            methodsToAdd.add(builder.build());
        }
        return methodsToAdd;
    }

    private String getJavaSourcePathFromPomXML(String path) {
        String javaSourcesPath = null;
        try {
            FileInputStream fileIS = new FileInputStream(path + "/pom.xml");
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(fileIS);
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/project/properties/sourcesPath[1]";
            javaSourcesPath = (String) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.STRING);
        } catch (Exception exception) {
            VDMMethodProcessor.writeWarning("Could not open " + path + "/pom.xml!");
        }
        return javaSourcesPath;
    }
}
