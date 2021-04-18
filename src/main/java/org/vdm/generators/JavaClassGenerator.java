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
            methodsToAdd.add(0, generateJavaObjectSetterForJavaClass());
            TypeSpec classSpec = TypeSpec.classBuilder(classNameToAdd).addModifiers(Modifier.PUBLIC)
                    .addMethods(methodsToAdd).addSuperinterface(VDMJavaInterface.class).build();
            String path = System.getProperty("user.dir").replace("\\", "/") + "/";
            String javaSourcesPath = getJavaSourcePathFromPomXML(path);
            String generatedJavaSourcesFolder = packageName.replace(".", "/") + "/";

            if (javaSourcesPath != null && !javaSourcesPath.isEmpty()) {
                path += javaSourcesPath + "/";
            } else {
                VDMOperationProcessor.writeWarning("Din not found sourcesPath in the pom.xml file! The path " + path
                        + generatedJavaSourcesFolder + " will be used for Java sources generation");
            }

            path += generatedJavaSourcesFolder;

            File directory = new File(path);
            directory.mkdirs();

            String filePath = path + classNameToAdd + ".java";
            File javaFile = new File(filePath);
            String javaFileContent = JavaFile.builder(packageName, classSpec).build().toString();

            if (javaFile.exists()) {
                javaFile.delete();
            }

            javaFile.createNewFile();
            FileWriter writer = new FileWriter(filePath);
            writer.write(javaFileContent.replace("implements VDMJavaInterface {",
                    "implements VDMJavaInterface {\n" + "\tprivate " + className + " javaObject;\n"));
            writer.close();
            VDMOperationProcessor.writeNote("Java class " + classNameToAdd + " generated at " + filePath);

        } catch (Exception exception) {
            VDMOperationProcessor.writeError(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private MethodSpec generateJavaObjectSetterForJavaClass() {
        return MethodSpec.methodBuilder(setJavaObjectMethodName).addModifiers(Modifier.PUBLIC)
                .addParameter(Value.class, "vdmObjectName")
                .addStatement("String vdmObjectNameString = vdmObjectName.toString()")
                .addStatement(
                        "javaObject = $T.getObjectWithVdmName(vdmObjectNameString.substring(1, vdmObjectNameString.length() - 1))",
                        VDMOperationAspect.class)
                .addStatement("return new $T()", VoidValue.class).returns(Value.class).addException(Exception.class)
                .build();
    }

    private List<MethodSpec> generateMethodsForJavaClass(List<Method> methods) throws Exception {
        List<MethodSpec> methodsToAdd = new ArrayList<>();
        for (Method methodToAdd : methods) {
            MethodSpec.Builder builder = MethodSpec.methodBuilder(methodToAdd.getName()).addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.get(Value.class));

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

            String statement = "javaObject." + methodToAdd.getName() + "(" + parametersName + ")";
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
