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

    @Pointcut("@annotation(vdmMethod)")
    public void callAt(VDMOperation vdmMethod) {
    }

    @Around("callAt(vdmMethod)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, VDMOperation vdmMethod) throws Throwable {
        if (RemoteController.interpreter == null || proceedingJoinPoint.getTarget() instanceof VDMJavaInterface) {
            return proceedingJoinPoint.proceed();
        } else {
            Object caller = proceedingJoinPoint.getTarget();
            String vdmObjectName = "";

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

            String fullMethodName = proceedingJoinPoint.toShortString().substring("call(".length());
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

            System.out.println("Executing " + lineToExecute);
            Object result = RemoteController.interpreter.execute(lineToExecute);
            return result;
        }
    }
}