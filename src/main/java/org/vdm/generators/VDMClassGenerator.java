package org.vdm.generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.squareup.javapoet.TypeName;

import org.vdm.annotations.*;
import org.vdm.overture.VDMTypesHelper;

public class VDMClassGenerator extends BaseGenerator {
    private final String generatedMethodComment = "-- This method is auto-generated. Please do not modify it!\n";

    public VDMClassGenerator(String className, List<Method> methods) {
        super(className, methods);
    }

    @Override
    public void generate() {
        try {
            String basePath = System.getProperty("user.dir").replace("\\", "/") + "/" + "generatedVDMModel/";
            File directory = new File(basePath);
            String classFileName = className.substring(className.lastIndexOf(".") + 1);

            if (!directory.exists()) {
                directory.mkdir();
            }

            String vdmFileName = basePath + classFileName + ".vdmpp";
            File vdmFile = new File(vdmFileName);

            if (!vdmFile.exists()) {
                vdmFile.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(vdmFileName));
                String vdmClassName = packageName.replace(".", "_") + "_" + classNameToAdd;
                writer.write("class " + vdmClassName + "\n");
                writer.write("types\n");
                writer.write("-- TODO Define types here\n");
                writer.write("values\n");
                writer.write("-- TODO Define values here\n");
                writer.write("instance variables\n");
                writer.write("-- TODO Define instance variables here\n");
                writer.write("operations\n");
                writer.write("-- TODO Define operations here\n");
                for (Method operation : methods) {
                    writeOperation(writer, operation);
                }
                writer.write("functions\n");
                writer.write("-- TODO Define functions here\n");
                writer.write("traces\n");
                writer.write("-- TODO Define Combinatorial Test Traces here\n");
                writer.write("end " + vdmClassName);
                writer.close();
            }
        } catch (Exception exception) {
            VDMOperationProcessor.writeError(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void writeOperation(BufferedWriter writer, Method operation) throws Exception {
        boolean isFirstParameter = true;
        Map<String, TypeName> parameters = operation.getParameters();
        String returnType = VDMTypesHelper.getVDMTypeAsStringFromJavaType(operation.getReturnType());
        String preCondition = operation.getPreCondition();
        String postCondition = operation.getPostCondition();
        String operationName = getMethodName(operation);

        writer.write(generatedMethodComment);
        writer.write("public " + operationName + ": ");
        for (Entry<String, TypeName> parameter : parameters.entrySet()) {
            if (!isFirstParameter) {
                writer.write(" * ");
            } else {
                isFirstParameter = false;
            }
            writer.write("(" + VDMTypesHelper.getVDMTypeAsStringFromJavaType(parameter.getValue()) + ")");
        }
        if (parameters.isEmpty()) {
            writer.write("()");
        }
        writer.write(" ==> (" + returnType + ")\n");

        isFirstParameter = true;
        writer.write(operationName + " (");
        for (Entry<String, TypeName> parameter : parameters.entrySet()) {
            if (!isFirstParameter) {
                writer.write(",");
            } else {
                isFirstParameter = false;
            }
            writer.write(parameter.getKey());
        }
        writer.write(") == is not yet specified");

        if (preCondition != null && !preCondition.isEmpty()) {
            writer.newLine();
            writer.write("pre " + preCondition);
        }

        if (postCondition != null && !postCondition.isEmpty()) {
            writer.newLine();
            writer.write("post " + postCondition);
        }

        writer.write(";\n");
        writer.newLine();
    }
}
