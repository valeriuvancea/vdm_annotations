package org.vdm.test;

import java.util.ArrayList;
import java.util.List;

import org.vdm.annotations.VDMOperation;

public class Test {
    public void init() {
        System.out.println(this.bestFunction(true, 3, new ArrayList<Integer>() {
            {
                add(3);
                add(4);
            }
        }));
        System.out.println(this.bestFunction2323(true, 2, new ArrayList<Integer>() {
            {
                add(3);
                add(5);
            }
        }));
    }

    @VDMOperation(preCondition = "c=true", postCondition = "e=3")
    public String bestFunction(boolean c, int e, List<Integer> test) {
        return bestFunction2323(c, e, test);
    }

    @VDMOperation(preCondition = "c=true", postCondition = "e=2")
    public String bestFunction2323(boolean c, int e, List<Integer> test) {
        System.out.println(c);
        System.out.println(e);
        System.out.println(test);
        if (c) {
            return bestFunction2323(false, e, test);
        } else {
            return "asdw";
        }
    }
}
