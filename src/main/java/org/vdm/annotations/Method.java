package org.vdm.annotations;

import java.util.Map;

import com.squareup.javapoet.TypeName;

public class Method {
    private String name;
    private Map<String, TypeName> parameters;
    private TypeName returnType;
    private String className;

    public Method(String name, Map<String, TypeName> parameters, TypeName returnType, String className) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public Map<String, TypeName> getParameters() {
        return parameters;
    }

    public TypeName getReturnType() {
        return returnType;
    }

    public String getClassName() {
        return className;
    }
}
