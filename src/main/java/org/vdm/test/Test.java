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
        }, "asdf", MessageField.CERT_CA));
        System.out.println(a(new byte[]{2,3})[0] + " " + a(new byte[]{2,3})[1]);
    }

    @VDMOperation(preCondition = "c=true", postCondition = "e=3")
    public String bestFunction(boolean c, int e, List<Integer> test) {
        return bestFunction2323(c, e, test, "asd", MessageField.TYPE);
    }

    @VDMOperation(preCondition = "c=true", postCondition = "e=2")
    public String bestFunction2323(boolean c, int e, List<Integer> test, String t, MessageField a) {
        System.out.println(c);
        System.out.println(e);
        System.out.println(test);
        System.out.println(t);
        System.out.println(a);
        if (c) {
            return bestFunction2323(false, e, test, t, a);
        } else {
            return "asdw";
        }
    }

    @VDMOperation
    public byte[] a(byte[]c) {
        return c;
    }
}
