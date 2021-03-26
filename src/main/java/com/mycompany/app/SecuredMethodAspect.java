package com.mycompany.app;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;

@Aspect
public class SecuredMethodAspect {
    @Pointcut("@annotation(secured)")
    public void callAt(Secured secured) {
    }

    @Around("callAt(secured)")
    public Object around(ProceedingJoinPoint pjp, Secured secured) throws Throwable {
        return secured.isLocked() ? "Method locked! Can not execute!" : pjp.proceed();
    }
}