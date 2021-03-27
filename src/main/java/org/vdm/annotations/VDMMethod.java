package org.vdm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VDMMethod {
    public enum VDMMethodType {
        OPERATION, FUNCTION
    }

    String preCondition = null;
    String postCondition = null;
    VDMMethodType methodType = VDMMethodType.OPERATION;
}