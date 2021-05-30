package org.vdm.annotations;

import java.util.Map;

import javax.lang.model.element.Element;

import com.squareup.javapoet.TypeName;

public class Method {
    private String name;
    private Map<String, TypeName> parameters;
    private TypeName returnType;
    private String className;
    private String preCondition;
    private String postCondition;
    private Element errorsAndWarningsTarget;

    public Method(String name, Map<String, TypeName> parameters, TypeName returnType, String className,
            String preCondition, String postCondition, Element errorsAndWarningsTarget) {
        this.name = name;
        this.parameters = parameters;
        this.returnType = returnType;
        this.className = className;
        this.preCondition = preCondition;
        this.postCondition = postCondition;
        this.errorsAndWarningsTarget = errorsAndWarningsTarget;
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

    public String getPreCondition() {
        return preCondition;
    }

    public String getPostCondition() {
        return postCondition;
    }

    public Element getErrorsAndWarningsTarget() {
        return errorsAndWarningsTarget;
    }
}
