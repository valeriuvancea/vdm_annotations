package org.vdm.test;

import org.vdm.annotations.VDMOperation;

public class Adder {
    private int c = 0;

    public Adder(int c) {
        this.c = c;
    }

    @VDMOperation
    public int getC() {
        return c;
    }

    @VDMOperation(postCondition = "RESULT=a+getC()")
    public int add(int a) {
        return a + c;
    }
}
