package org.vdm.generators;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
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

public class JavaClassGenerator extends BaseGenerator {
    public JavaClassGenerator(String className, List<Method> methods) {
        super(className, methods);
    }

    @Override
    public void generate() {
        try {
            List<MethodSpec> methodsToAdd = generateMethodsForJavaClass(methods);
            TypeSpec classSpec = TypeSpec.classBuilder(classNameToAdd).addModifiers(Modifier.PUBLIC)
                    .addMethods(methodsToAdd).addSuperinterface(VDMJavaInterface.class).build();

            String path = System.getProperty("user.dir").replace("\\", "/") + "/";
            String javaSourcesPath = getJavaSourcePathFromPomXML(path);

            if (javaSourcesPath != null && !javaSourcesPath.isEmpty()) {
                path += javaSourcesPath + "/";
            } else {
                VDMOperationProcessor.writeWarning("Din not found sourcesPath in the pom.xml file! The path " + path
                        + " will be used for sources generation");
            }
            path += packageName.replace(".", "/") + "/";

            File directory = new File(path);
            directory.mkdirs();

            String filePath = path + classNameToAdd + ".java";
            File javaFile = new File(filePath);
            String javaFileContent = JavaFile.builder(packageName, classSpec).build().toString()
                    .replace("class " + classNameToAdd, "class " + classNameToAdd + " extends " + className);

            if (!javaFile.exists()) {
                javaFile.createNewFile();
                FileWriter writer = new FileWriter(filePath);
                writer.write(javaFileContent);
                writer.close();
                VDMOperationProcessor.writeNote(
                        "Java class " + classNameToAdd + " generated at " + path + packageName.replace(".", "/") + "/");
            }
        } catch (Exception exception) {
            VDMOperationProcessor.writeError(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private List<MethodSpec> generateMethodsForJavaClass(List<Method> methods) throws Exception {
        List<MethodSpec> methodsToAdd = new ArrayList<>();
        for (Method methodToAdd : methods) {
            MethodSpec.Builder builder = MethodSpec.methodBuilder(getMethodName(methodToAdd))
                    .addModifiers(Modifier.PUBLIC).returns(TypeName.get(Value.class));

            Map<String, TypeName> parameters = methodToAdd.getParameters();
            for (String parameter : parameters.keySet()) {
                builder.addParameter(Value.class, parameter);
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
            VDMOperationProcessor.writeWarning("Could not open " + path + "/pom.xml!");
        }
        return javaSourcesPath;
    }
}
