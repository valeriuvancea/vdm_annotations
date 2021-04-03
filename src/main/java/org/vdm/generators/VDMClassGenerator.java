package org.vdm.generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.squareup.javapoet.TypeName;

import org.apache.commons.io.FileUtils;
import org.overture.interpreter.VDMPP;
import org.overture.interpreter.util.ExitStatus;
import org.vdm.annotations.*;
import org.vdm.overture.VDMTypesHelper;

@SuppressWarnings({ "serial" })
public class VDMClassGenerator extends BaseGenerator {
    private final String generatedMethodComment = "-- The above method was generated. Please do not modify it nor delete this comment!";
    private final String vdmClassName;
    private final String vdmFileName;
    private final String vdmFilePath;
    private final String vdmGeneratedClassesFolder;
    private final Set<String> vdmKeyWordsSet = new HashSet<String>() {
        {
            add("types");
            add("values");
            add("instance variables");
            add("functions");
            add("traces");
        }
    };

    public VDMClassGenerator(String className, List<Method> methods) {
        super(className, methods);
        vdmClassName = packageName.replace(".", "_") + "_" + classNameToAdd;
        vdmKeyWordsSet.add("end " + vdmClassName);
        String classFileName = className.substring(className.lastIndexOf(".") + 1);
        vdmGeneratedClassesFolder = System.getProperty("user.dir").replace("\\", "/") + "/" + "generatedVDMModel/";
        vdmFileName = classFileName + ".vdmpp";
        vdmFilePath = vdmGeneratedClassesFolder + vdmFileName;
    }

    @Override
    public void generate() {
        try {
            File directory = new File(vdmGeneratedClassesFolder);
            File vdmFile = new File(vdmFilePath);

            directory.mkdirs();

            if (!vdmFile.exists()) {
                createNewVDMFile(vdmFile, vdmFilePath);
                VDMOperationProcessor.writeNote("VDN class " + vdmClassName + " generated at " + vdmFilePath);
            } else {
                editExistingFile(vdmFile, vdmFilePath);
                VDMOperationProcessor.writeNote("VDN class " + vdmClassName + " modified at " + vdmFilePath);
            }

            VDMPP vdmParser = new VDMPP();

            if (vdmParser.parse(Arrays.asList(new File[] { vdmFile })) == ExitStatus.EXIT_ERRORS) {
                VDMOperationProcessor.writeError("Found syntax errors in the generated VDM file: " + vdmFileName);
            }

            if (vdmParser.typeCheck() == ExitStatus.EXIT_ERRORS) {
                VDMOperationProcessor.writeError("Found type errors in the generated VDM file: " + vdmFileName);
            }
        } catch (Exception exception) {
            VDMOperationProcessor.writeError(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void createNewVDMFile(File vdmFile, String vdmFilePath) throws Exception {
        vdmFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(vdmFilePath));

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
            writer.write(getOperation(operation));
        }
        writer.write("functions\n");
        writer.write("-- TODO Define functions here\n");
        writer.write("traces\n");
        writer.write("-- TODO Define Combinatorial Test Traces here\n");
        writer.write("end " + vdmClassName);
        writer.close();
    }

    private void editExistingFile(File vdmFile, String vdmFilePath) throws Exception {
        String currentFileContent = FileUtils.readFileToString(vdmFile, "UTF-8");
        int operationsStartIndex = currentFileContent.indexOf("operations");
        Runnable deleteCurrentFileAndCreateANewOne = () -> {
            try {
                VDMOperationProcessor.writeWarning("The file will be deleted and a new one will be created.");
                vdmFile.delete();
                createNewVDMFile(vdmFile, vdmFilePath);
            } catch (Exception exception) {
                VDMOperationProcessor.writeError(exception.getMessage());
                exception.printStackTrace();
            }
        };

        if (operationsStartIndex == -1) {
            VDMOperationProcessor.writeWarning("VDM keyword \"operations\" not found.");
            deleteCurrentFileAndCreateANewOne.run();
        } else {
            Integer operationsEndIndex = null;
            for (String vdmKeyWord : vdmKeyWordsSet) {
                int nextKeyWord = currentFileContent.indexOf(vdmKeyWord);
                if (nextKeyWord != -1 && (operationsEndIndex == null || nextKeyWord < operationsEndIndex)
                        && nextKeyWord > operationsStartIndex) {
                    operationsEndIndex = nextKeyWord;
                }
            }
            if (operationsEndIndex == null) {
                VDMOperationProcessor.writeWarning("VDM file ended unexpected.");
                deleteCurrentFileAndCreateANewOne.run();
            } else {
                String operationsContent = currentFileContent.substring(operationsStartIndex, operationsEndIndex);
                String oldOperationsContent = operationsContent;
                VDMOperationProcessor.writeNote("All previously generated functions will bee deleted");

                /*
                 * Deleting all the operations that start with public GENERATED and end with
                 * `generatedMethodComment`
                 */
                operationsContent = operationsContent.replaceAll("public GENERATED((?!" + generatedMethodComment
                        + ")[^]])*;\\n" + generatedMethodComment + "\\n\\n", "");

                if (operationsContent.equals(oldOperationsContent)) {
                    VDMOperationProcessor.writeWarning(
                            "No generated operations found in already existing vdm file: " + vdmFileName + ".\n"
                                    + "The method regeneration might be wrong. Delete the vdm file in that case.");
                } else {
                    currentFileContent = currentFileContent.replace(oldOperationsContent, operationsContent);
                }

                for (Method operation : methods) {
                    currentFileContent = currentFileContent.replace("operations\n",
                            "operations\n" + getOperation(operation));
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(vdmFilePath));
                writer.write(currentFileContent);
                writer.close();
            }
        }
    }

    private String getOperation(Method operation) throws Exception {
        String result = "";
        boolean isFirstParameter = true;
        Map<String, TypeName> parameters = operation.getParameters();
        String returnType = VDMTypesHelper.getVDMTypeAsStringFromJavaType(operation.getReturnType());
        String preCondition = operation.getPreCondition();
        String postCondition = operation.getPostCondition();
        String operationName = getMethodName(operation);

        result += "public " + operationName + ": ";
        for (Entry<String, TypeName> parameter : parameters.entrySet()) {
            if (!isFirstParameter) {
                result += " * ";
            } else {
                isFirstParameter = false;
            }
            result += "(" + VDMTypesHelper.getVDMTypeAsStringFromJavaType(parameter.getValue()) + ")";
        }
        if (parameters.isEmpty()) {
            result += "()";
        }
        result += " ==> (" + returnType + ")\n";

        isFirstParameter = true;
        result += operationName + " (";
        for (Entry<String, TypeName> parameter : parameters.entrySet()) {
            if (!isFirstParameter) {
                result += ",";
            } else {
                isFirstParameter = false;
            }
            result += parameter.getKey();
        }
        result += ") == is not yet specified";

        if (preCondition != null && !preCondition.isEmpty()) {
            result += "\n";
            result += "pre " + preCondition;
        }

        if (postCondition != null && !postCondition.isEmpty()) {
            result += "\n";
            result += "post " + postCondition;
        }

        result += ";\n";
        result += generatedMethodComment + "\n";
        result += "\n";
        return result;
    }
}
