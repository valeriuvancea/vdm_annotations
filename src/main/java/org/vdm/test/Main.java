package org.vdm.test;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        Test test = new Test();
        test.bestFunction(true, 2, new ArrayList<Integer>() {
            {
                add(3);
                add(4);
            }
        });
    }
}
