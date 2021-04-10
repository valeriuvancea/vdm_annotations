package org.vdm.test;

import org.vdm.annotations.VDMOperation;

public class Adder {
    @VDMOperation(postCondition = "a=5")
    public int add(int x, int y) {
        return x + y;
    }
}
