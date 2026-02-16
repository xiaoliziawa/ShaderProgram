package com.lirxowo.shaderprogram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;

public class SeascapeEffect {

    private static boolean enabled = false;

    public static void toggle() {
        enabled = !enabled;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void applyUniforms() {
        ShaderInstance shader = GameRenderer.getRendertypeTranslucentShader();
        setFloatUniform(shader, "SeascapeEnabled", enabled ? 1.0f : 0.0f);

        if (enabled) {
            Minecraft mc = Minecraft.getInstance();
            float camX = 0f, camZ = 0f;
            var camPos = mc.gameRenderer.getMainCamera().getPosition();
            camX = (float) camPos.x;
            camZ = (float) camPos.z;
            setFloatUniform(shader, "CameraPosX", camX);
            setFloatUniform(shader, "CameraPosZ", camZ);
        }
    }

    private static void setFloatUniform(@Nullable ShaderInstance shader, String name, float value) {
        if (shader == null) return;
        var uniform = shader.getUniform(name);
        if (uniform != null) {
            uniform.set(value);
        }
    }
}
