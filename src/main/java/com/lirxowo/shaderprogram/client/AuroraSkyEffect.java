package com.lirxowo.shaderprogram.client;

public class AuroraSkyEffect {
    private static boolean enabled = false;

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
