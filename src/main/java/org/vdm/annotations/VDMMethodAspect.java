package org.vdm.annotations;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.vdm.overture.RemoteController;

@Aspect
public class VDMMethodAspect {
    private boolean isTheInterpreterExecuting = false;

    @Pointcut("@annotation(vdmMethod)")
    public void callAt(VDMMethod vdmMethod) {
    }

    @Around("callAt(vdmMethod)")
    public Object around(ProceedingJoinPoint proceedingJoinPoint, VDMMethod vdmMethod) throws Throwable {
        System.out.println("Aspect working");
        if (RemoteController.interpreter == null) {
            return proceedingJoinPoint.proceed();
        } else {
            if (isTheInterpreterExecuting) {
                new Exception("Only one VDM Method can be called at a moment in time! VDM Method can not be nested!");
            }
            isTheInterpreterExecuting = true;
            Object result = RemoteController.interpreter.execute(proceedingJoinPoint.toShortString());
            isTheInterpreterExecuting = false;
            return result;
        }
    }
}