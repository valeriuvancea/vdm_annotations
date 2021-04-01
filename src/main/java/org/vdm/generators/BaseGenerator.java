package org.vdm.generators;

import java.util.List;
import org.vdm.annotations.*;

public abstract class BaseGenerator {
    protected String className;
    protected List<Method> methods;
    protected final String classNameToAdd;
    protected final String packageName = "generated.vdm";

    public BaseGenerator(String className, List<Method> methods) {
        this.className = className;
        this.methods = methods;
        this.classNameToAdd = "VDM" + className.substring(className.lastIndexOf(".") + 1);
    }

    public abstract void generate();

    protected String getMethodName(Method method) {
        return method.getName() + "Generated";
    }
}
