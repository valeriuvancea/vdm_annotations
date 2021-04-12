package org.vdm.annotations;

import java.util.HashMap;
import java.util.Map;

import com.squareup.javapoet.TypeName;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.*;
import org.vdm.generators.VDMClassGenerator;
import org.vdm.overture.RemoteController;
import org.vdm.overture.VDMTypesHelper;
import org.aspectj.lang.reflect.MethodSignature;

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
                String vdmClassName = caller.getClass().getSimpleName();
                vdmObjectName = vdmClassName + caller.hashCode();
                RemoteController.interpreter.create(vdmObjectName, "new " + vdmClassName + "()");
                System.out.println("created vdm object " + vdmObjectName + " of class " + vdmClassName);
                vdmObjects.put(caller, vdmObjectName);
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

            result = RemoteController.interpreter.execute(lineToExecute);
            System.out.println("Executed " + lineToExecute + " and got the result " + result);

            Signature signature =  proceedingJoinPoint.getSignature();
            Class<?> returnType = ((MethodSignature) signature).getReturnType();
            if (returnType != void.class) {
                result = VDMTypesHelper.getJavaValueFromVDMString(result.toString(), TypeName.get(returnType).toString());
            }
        }
        lastCalledMethodName = fullMethodName;
        return result;
    }
}