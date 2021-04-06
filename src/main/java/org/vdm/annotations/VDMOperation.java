package org.vdm.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VDMOperation {
    public String preCondition() default "";

    public String postCondition() default "";
}