package org.vdm.test;

import java.util.List;

import org.vdm.annotations.VDMOperation;

public class Test {
    @VDMOperation(preCondition = "c=true", postCondition = "e=2")
    public void bestFunction(boolean c, int e, List<Integer> test) {
        System.out.println(c);
        System.out.println(e);
        System.out.println(test);
    }

    @VDMOperation(preCondition = "c=true", postCondition = "e=2")
    public String bestFunction2323(boolean c, int e, List<Integer> test) {
        System.out.println(c);
        System.out.println(e);
        System.out.println(test);
        return "asdw";
    }
}
