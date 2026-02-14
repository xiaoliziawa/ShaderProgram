package com.lirxowo.shaderprogram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * 黑洞着色器管理类。
 * <p>
 * 持有 black_hole 着色器实例，并独立维护一份场景捕获纹理，
 * 供引力透镜效果采样背景画面。
 */
public class BlackHoleShader {

    @Nullable
    public static ShaderInstance instance;

    private static int captureTexId = -1;
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    public static int getCaptureTexId() {
        return captureTexId;
    }

    /**
     * 将当前帧缓冲区内容复制到 GPU 纹理。
     * 在 AFTER_WEATHER 阶段调用，此时所有场景内容都已绘制完毕。
     */
    public static void captureScene() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (width <= 0 || height <= 0) return;

        if (captureTexId == -1 || width != lastWidth || height != lastHeight) {
            if (captureTexId != -1) {
                glDeleteTextures(captureTexId);
            }

            captureTexId = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, captureTexId);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

            lastWidth = width;
            lastHeight = height;
        }

        glBindTexture(GL_TEXTURE_2D, captureTexId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
