package com.mycompany.app;

/**
 * Hello world!
 *
 */
public class App {
    public static void main(String[] args) {
        SecuredMethod test = new SecuredMethod();
        test.unlockedMethod();
        System.out.println(test.lockedMethod());
    }
}
