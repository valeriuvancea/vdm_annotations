package org.vdm.test;

import org.vdm.annotations.VDMOperation;

public class Adder {
    @VDMOperation(postCondition = "RESULT=a+b")
    public int add(int a, int b) {
        return a + b;
    }
}
