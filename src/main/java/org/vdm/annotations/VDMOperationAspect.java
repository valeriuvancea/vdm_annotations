package org.vdm.annotations;

import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.vdm.generators.BaseGenerator;
import org.vdm.overture.RemoteController;
import org.vdm.overture.VDMTypesHelper;

@Aspect
public class VDMOperationAspect {
    private Map<Object, String> vdmObjects = new HashMap<>();
    private static int callStackLevel = 0;
    private static String lastCalledMethodName = "";

    @Pointcut("@annotation(vdmMethod)")
    public void callAt(VDMOperation vdmMethod) {
    }

    @Around("callAt(vdmMethod)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, VDMOperation vdmMethod) throws Throwable {
        String fullMethodName = proceedingJoinPoint.toShortString();
        Object result = null;
        fullMethodName = fullMethodName.substring(fullMethodName.indexOf("(") + 1, fullMethodName.length() - 1);

        if (RemoteController.interpreter == null || proceedingJoinPoint.getTarget() instanceof VDMJavaInterface) {
            if (proceedingJoinPoint.getTarget() instanceof VDMJavaInterface && RemoteController.interpreter != null
                    && callStackLevel > 0 && callStackLevel % 2 == 1) {
                System.out.println("NOTE: Annotated method " + fullMethodName
                        + " called inside from another annotated method " + lastCalledMethodName
                        + ". The method will be executed directly in Java without verifying properties defined in VDM.");
            }

            callStackLevel++;
            result = proceedingJoinPoint.proceed();
        } else {
            Object caller = proceedingJoinPoint.getTarget();
            String vdmObjectName = "";
            callStackLevel = 0;

            if (!vdmObjects.containsKey(caller)) {
                vdmObjectName = "vdmObject" + caller.hashCode();
                String vdmClassName = BaseGenerator.packageName.replace(".", "_") + "_" + BaseGenerator.classPrefix
                        + caller.getClass().getSimpleName();
                RemoteController.interpreter.create(vdmObjectName, "new " + vdmClassName + "()");
                System.out.println("created vdm object " + vdmObjectName + " of class " + vdmClassName);
                vdmObjects.put(caller, vdmObjectName);
            } else {
                vdmObjectName = vdmObjects.get(caller);
            }

            String methodName = BaseGenerator.methodsSuffix
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

            System.out.println("Executed " + lineToExecute);
            result = RemoteController.interpreter.execute(lineToExecute);
        }
        lastCalledMethodName = fullMethodName;
        return result;
    }
}