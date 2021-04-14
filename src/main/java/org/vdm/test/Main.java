package org.vdm.test;

public class Main {
    public static void main(String[] args) {
        Adder adder = new Adder(5);
        System.out.println(adder.add(3));
        System.out.println(adder.add(1));
        Test test = new Test();
        test.init();
    }
}
