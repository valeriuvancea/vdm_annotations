package org.vdm.generators;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.squareup.javapoet.TypeName;
import org.overture.parser.lex.LexTokenReader;
import org.overture.parser.syntax.StatementReader;
import org.vdm.annotations.*;
import org.vdm.overture.VDMTypesHelper;
import org.apache.commons.io.FileUtils;
import org.overture.ast.lex.Dialect;
import org.overture.ast.lex.VDMToken;
import org.overture.interpreter.VDMPP;
import org.overture.interpreter.util.ExitStatus;

import static java.util.stream.Collectors.toList;

@SuppressWarnings({ "serial" })
public class VDMClassGenerator extends BaseGenerator {
    private final String vdmInterfaceName;
    private final String vdmInterfaceFileName;
    private final String vdmInterfaceFilePath;
    private final String vdmClassName;
    private final String vdmClassFileName;
    private final String vdmClassFilePath;
    private final String vdmGeneratedClassesFolder;
    private final String vdmOperationAccess = "public";
    private final String vdmInterfaceObjectName = "javaObject";
    public static final String vdmClassOperationPrefix = "GENERATED_";

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
        vdmClassName = className.substring(className.lastIndexOf(".") + 1);
        vdmKeyWordsSet.add("end " + vdmClassName);
        vdmGeneratedClassesFolder = System.getProperty("user.dir").replace("\\", "/") + "/" + "generatedVDMModel/";
        vdmClassFileName = vdmClassName + ".vdmpp";
        vdmClassFilePath = vdmGeneratedClassesFolder + vdmClassFileName;
        vdmInterfaceName = packageName.replace(".", "_") + "_" + classNameToAdd;
        vdmInterfaceFileName = "I" + vdmClassFileName;
        vdmInterfaceFilePath = vdmGeneratedClassesFolder + vdmInterfaceFileName;
    }

    @Override
    public void generate() {
        try {
            new File(vdmGeneratedClassesFolder).mkdirs();

            File vdmInterfaceFile = new File(vdmInterfaceFilePath);
            if (vdmInterfaceFile.exists()) {
                vdmInterfaceFile.delete();
            }
            createNewVDMFile(vdmInterfaceFile, vdmInterfaceFilePath, true);
            VDMOperationProcessor.writeNote("VDM class " + vdmInterfaceName + " generated at " + vdmInterfaceFilePath);

            File vdmClassFile = new File(vdmClassFilePath);

            if (!vdmClassFile.exists()) {
                createNewVDMFile(vdmClassFile, vdmClassFilePath, false);
                VDMOperationProcessor.writeNote("VDM class " + vdmClassName + " generated at " + vdmClassFilePath);
            } else {
                editExistingVDMClass(vdmClassFile, vdmClassFilePath);
                VDMOperationProcessor.writeNote("VDM class " + vdmClassName + " edited at " + vdmClassFilePath);
            }

            VDMPP vdmParser = new VDMPP();

            if (vdmParser
                    .parse(Arrays.asList(new File[] { vdmInterfaceFile, vdmClassFile })) == ExitStatus.EXIT_ERRORS) {
                VDMOperationProcessor
                        .writeError("Found syntax errors in the generated VDM classes for the Java class " + className);
            }

            if (vdmParser.typeCheck() == ExitStatus.EXIT_ERRORS) {
                VDMOperationProcessor
                        .writeError("Found type errors in the generated VDM classes for the Java class " + className);
            }

        } catch (Exception exception) {
            VDMOperationProcessor.writeError(exception.getMessage());
            exception.printStackTrace();
        }
    }

    private void createNewVDMFile(File vdmFile, String vdmFilePath, boolean isInterface) throws Exception {
        vdmFile.createNewFile();
        BufferedWriter writer = new BufferedWriter(new FileWriter(vdmFilePath));

        writer.write("class " + (isInterface ? vdmInterfaceName : vdmClassName) + "\n");
        writer.write("types\n");
        writer.write("-- TODO Define types here\n");
        writer.write("values\n");
        writer.write("-- TODO Define values here\n");
        writer.write("instance variables\n");
        if (isInterface) {
            writer.write("-- TODO Define instance variables here\n");
        } else {
            writer.write(
                    "\t" + vdmInterfaceObjectName + ": " + vdmInterfaceName + " := new " + vdmInterfaceName + "();\n");
        }
        writer.write("operations\n");
        for (Method operation : methods) {
            writer.write(getVDMOperation(operation, isInterface));
        }
        writer.write("functions\n");
        writer.write("-- TODO Define functions here\n");
        writer.write("traces\n");
        writer.write("-- TODO Define Combinatorial Test Traces here\n");
        writer.write("end " + (isInterface ? vdmInterfaceName : vdmClassName));
        writer.close();
    }

    private void editExistingVDMClass(File vdmFile, String vdmFilePath) throws Exception {
        String currentFileContent = FileUtils.readFileToString(vdmFile, "UTF-8");
        int operationsStartIndex = currentFileContent.indexOf("operations");
        Runnable deleteCurrentFileAndCreateANewOne = () -> {
            try {
                VDMOperationProcessor
                        .writeWarning("The file " + vdmFilePath + " will be deleted and a new one will be created.");
                vdmFile.delete();
                createNewVDMFile(vdmFile, vdmFilePath, false);
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
                VDMOperationProcessor
                        .writeWarning("VDM file ended unexpected. Could not found the 'operations' token.");
                deleteCurrentFileAndCreateANewOne.run();
            } else {
                String operationsContent = currentFileContent.substring(operationsStartIndex, operationsEndIndex);
                String oldOperationsContent = operationsContent;
                Set<Method> foundMethods = new HashSet<>();

                // edit existing methods
                String vdmDefinitionRegex = vdmOperationAccess + " " + vdmClassOperationPrefix
                        + "[^:]*:((?!==>)[^]])*==>((?!==)[^]])*==";
                Matcher matcher = Pattern.compile(vdmDefinitionRegex).matcher(oldOperationsContent);
                while (matcher.find()) {
                    LexTokenReader lexTokenReader = new LexTokenReader(oldOperationsContent.substring(matcher.end()),
                            Dialect.VDM_PP);
                    StatementReader stmt = new StatementReader(lexTokenReader);

                    stmt.readStatement();
                    if (lexTokenReader.getLast().is(VDMToken.PRE) || lexTokenReader.getLast().is(VDMToken.POST)) {
                        lexTokenReader.nextToken();
                        if (lexTokenReader.getLast().is(VDMToken.PRE) || lexTokenReader.getLast().is(VDMToken.POST)) {
                            lexTokenReader.nextToken();
                        }
                    }

                    lexTokenReader.nextToken();

                    String operationString = oldOperationsContent.substring(matcher.start(),
                            matcher.end() + lexTokenReader.getLast().location.getStartOffset());

                    Optional<Method> foundMethod = methods.stream()
                            .filter(method -> getVDMClassOperationNameWithAccess(method).equals(oldOperationsContent
                                    .substring(matcher.start(), oldOperationsContent.indexOf(":", matcher.start()))))
                            .findFirst();

                    if (foundMethod.isPresent()) {
                        Method method = foundMethod.get();
                        String editedOperationString = operationString;

                        foundMethods.add(method);
                        // replace definition
                        editedOperationString = editedOperationString.replaceFirst(vdmDefinitionRegex,
                                getVDMOperationDefinition(method, false));
                        // replace superclass execution
                        editedOperationString = editedOperationString.replaceFirst(
                                getVDMSuperClassOperationExecutionWithoutParameters(method) + "\\(.*\\);?",
                                getVDMSuperClassOperationExecution(method));
                        operationsContent = operationsContent.replace(operationString, editedOperationString);
                    } else {
                        // delete generated method that is not part of the original class anymore, or it
                        // doesn't have the annotation anymore
                        operationsContent = operationsContent.replace(operationString, "");
                    }
                }

                // add new methods
                for (Method method : methods.stream().filter(method -> !foundMethods.contains(method))
                        .collect(toList())) {
                    if (!operationsContent.endsWith("\n")) {
                        operationsContent += "\n\n";
                    } else if (!operationsContent.endsWith("\n\n")) {
                        operationsContent += "\n";
                    }
                    operationsContent += getVDMOperation(method, false);
                }

                BufferedWriter writer = new BufferedWriter(new FileWriter(vdmFilePath));
                writer.write(currentFileContent.replace(oldOperationsContent, operationsContent));
                writer.close();
            }
        }
    }

    private String getVDMOperation(Method method, boolean isForInterface) throws Exception {
        String result = "";
        String preCondition = method.getPreCondition();
        String postCondition = method.getPostCondition();

        result += getVDMOperationDefinition(method, isForInterface);
        result += isForInterface ? "is not yet specified" : getVDMClassOperationBody(method);

        if (isForInterface) {
            if (preCondition != null && !preCondition.isEmpty()) {
                result += "\n";
                result += "pre " + preCondition;
            }

            if (postCondition != null && !postCondition.isEmpty()) {
                result += "\n";
                result += "post " + postCondition;
            }
        }

        result += ";\n\n";
        return result;
    }

    String getVDMOperationDefinition(Method method, boolean isForInterface) throws Exception {
        String definition = "";

        definition += (isForInterface ? getVDMInterfaceOperationNameWithAccess(method)
                : getVDMClassOperationNameWithAccess(method)) + ":";
        definition += getVDMParametersTypes(method);
        definition += " ==> (" + VDMTypesHelper.getVDMTypeAsStringFromJavaType(method.getReturnType()) + ")\n";
        definition += (isForInterface ? getVDMInterfaceOperationName(method) : getVDMClassOperationName(method))
                + getVDMParametersNames(method) + " == ";

        return definition;
    }

    String getVDMSuperClassOperationExecution(Method method) {
        return getVDMSuperClassOperationExecutionWithoutParameters(method) + getVDMParametersNames(method) + ";";
    }

    String getVDMClassOperationBody(Method method) {
        String vdmOperationBody = "";

        vdmOperationBody += "(\n\t" + "--Extra VDM expressions can be added to this operation";
        vdmOperationBody += "\n\t" + getVDMSuperClassOperationExecution(method);
        vdmOperationBody += "\n)";

        return vdmOperationBody;
    }

    String getVDMSuperClassOperationExecutionWithoutParameters(Method method) {
        return vdmInterfaceObjectName + "." + getMethodName(method);
    }

    String getVDMInterfaceOperationNameWithAccess(Method method) {
        return vdmOperationAccess + " " + getVDMInterfaceOperationName(method);
    }

    String getVDMInterfaceOperationName(Method method) {
        return getMethodName(method);
    }

    String getVDMClassOperationNameWithAccess(Method method) {
        return vdmOperationAccess + " " + getVDMClassOperationName(method);
    }

    String getVDMClassOperationName(Method method) {
        return vdmClassOperationPrefix + method.getName();
    }

    String getVDMParametersTypes(Method method) throws Exception {
        boolean isFirstParameter = true;
        String result = "";

        for (Entry<String, TypeName> parameter : method.getParameters().entrySet()) {
            if (!isFirstParameter) {
                result += " * ";
            } else {
                isFirstParameter = false;
            }
            result += "(" + VDMTypesHelper.getVDMTypeAsStringFromJavaType(parameter.getValue()) + ")";
        }

        if (result.isEmpty()) {
            result += "()";
        }

        return result;
    }

    String getVDMParametersNames(Method method) {
        boolean isFirstParameter = true;
        String parametersString = "(";

        for (Entry<String, TypeName> parameter : method.getParameters().entrySet()) {
            if (!isFirstParameter) {
                parametersString += ",";
            } else {
                isFirstParameter = false;
            }
            parametersString += parameter.getKey();
        }

        parametersString += ")";

        return parametersString;
    }
}
