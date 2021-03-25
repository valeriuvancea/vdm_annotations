package com.mycompany.app;

public class SecuredMethod {

    @Secured(isLocked = true)
    public String lockedMethod() {
        System.out.println("locked");
        return "asd";
    }

    @Secured(isLocked = false)
    public void unlockedMethod() {
        System.out.println("unlocked");
    }
}