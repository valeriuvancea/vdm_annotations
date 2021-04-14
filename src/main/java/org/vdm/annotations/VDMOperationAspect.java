package org.vdm.annotations;

import java.util.HashMap;
import java.util.Map;

import com.squareup.javapoet.TypeName;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.vdm.generators.BaseGenerator;
import org.vdm.generators.VDMClassGenerator;
import org.vdm.overture.RemoteController;
import org.vdm.overture.VDMTypesHelper;
import org.aspectj.lang.reflect.MethodSignature;

@Aspect
public class VDMOperationAspect {
    private static Map<Object, String> vdmObjects = new HashMap<>();
    private static boolean isTheCurrentMethodCalledFromAnotherOne = false;
    private static String lastCalledMethodName = "";

    @Pointcut("@annotation(vdmMethod)")
    public void callAt(VDMOperation vdmMethod) {
    }

    @Around("callAt(vdmMethod)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, VDMOperation vdmMethod) throws Throwable {
        Object result = null;
        String fullMethodName = proceedingJoinPoint.toShortString();
        fullMethodName = fullMethodName.substring(fullMethodName.indexOf("(") + 1, fullMethodName.length() - 1);
        if (proceedingJoinPoint.getKind() != "method-execution") {
            boolean isMethodCalledFromAVDMJavaInterface = VDMJavaInterface.class
                    .isAssignableFrom(proceedingJoinPoint.getSourceLocation().getWithinType());

            if (RemoteController.interpreter == null || isMethodCalledFromAVDMJavaInterface || isTheCurrentMethodCalledFromAnotherOne) {
                if (RemoteController.interpreter != null && isTheCurrentMethodCalledFromAnotherOne) {
                    System.out.println("NOTE: Annotated method " + fullMethodName
                            + " called inside from another annotated method " + lastCalledMethodName
                            + ". The method will be executed directly in Java without verifying properties defined in VDM.");
                }

                isTheCurrentMethodCalledFromAnotherOne = true;
                lastCalledMethodName = fullMethodName;
                result = proceedingJoinPoint.proceed();
            } else {
                Object caller = proceedingJoinPoint.getTarget();
                String vdmObjectName = "";

                if (!vdmObjects.containsKey(caller)) {
                    String vdmClassName = caller.getClass().getSimpleName();
                    vdmObjectName = vdmClassName + caller.hashCode();
                    RemoteController.interpreter.create(vdmObjectName, "new " + vdmClassName + "()");
                    vdmObjects.put(caller, vdmObjectName);
                    RemoteController.interpreter.execute(vdmObjectName + "." + BaseGenerator.setJavaObjectMethodName
                            + "(\"" + vdmObjectName + "\")");
                    System.out.println("created vdm object " + vdmObjectName + " of class " + vdmClassName);
                } else {
                    vdmObjectName = vdmObjects.get(caller);
                }

                String methodName = VDMClassGenerator.vdmClassOperationPrefix
                        + fullMethodName.substring(fullMethodName.indexOf(".") + 1, fullMethodName.indexOf("("));
                String lineToExecute = vdmObjectName + "." + methodName + "(";
                boolean isFirstArgument = true;

                for (Object argument : proceedingJoinPoint.getArgs()) {
                    if (!isFirstArgument) {
                        lineToExecute += ",";
                    } else {
                        isFirstArgument = false;
                    }
                    lineToExecute += VDMTypesHelper.getVDMStringFromJavaValue(argument);
                }
                lineToExecute += ")";

                lastCalledMethodName = fullMethodName;
                result = RemoteController.interpreter.execute(lineToExecute);
                System.out.println("Executed " + lineToExecute + " and got the result " + result);

                Signature signature = proceedingJoinPoint.getSignature();
                Class<?> returnType = ((MethodSignature) signature).getReturnType();
                if (returnType != void.class) {
                    result = VDMTypesHelper.getJavaValueFromVDMString(result.toString(),
                            TypeName.get(returnType).toString());
                }
            }
        } else {
            result = proceedingJoinPoint.proceed();
        }
        return result;

    }

    @After("callAt(vdmMethod)")
    public void after(JoinPoint joinPoint, VDMOperation vdmMethod) throws Throwable {
        isTheCurrentMethodCalledFromAnotherOne = false;
    }

    @SuppressWarnings({ "unchecked" })
    public static <T> T getObjectWithVdmName(String name) throws Exception {
        for (Map.Entry<Object, String> entry : vdmObjects.entrySet()) {
            if (name.equals(entry.getValue())) {
                return (T) entry.getKey();
            }
        }
        throw new Exception("Vdm object named " + name + " was not found!");
    }
}