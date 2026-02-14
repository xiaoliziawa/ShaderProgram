package com.lirxowo.shaderprogram.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;

public class SynthwaveSkyRenderer {

    private static final float SKY_RADIUS = 100.0f;
    private static final int SEGMENTS = 64;
    private static final int RINGS = 32;

    private static CloudStatus savedCloudStatus = null;
    private static boolean cloudsHidden = false;

    public static void render(RenderLevelStageEvent event) {
        boolean active = SynthwaveEffect.isEnabled() || SynthwaveEffect.getRadius() > 0.01f;
        Minecraft mc = Minecraft.getInstance();

        // 管理云的显示/隐藏，直接用Minecraft自己的关闭云设置了
        if (active && !cloudsHidden) {
            savedCloudStatus = mc.options.getCloudsType();
            mc.options.cloudStatus().set(CloudStatus.OFF);
            cloudsHidden = true;
        } else if (!active && cloudsHidden) {
            if (savedCloudStatus != null) {
                mc.options.cloudStatus().set(savedCloudStatus);
            }
            cloudsHidden = false;
            savedCloudStatus = null;
        }

        if (!active) return;

        ShaderInstance shader = SynthwaveSkyShader.instance;
        if (shader == null) return;

        Camera camera = event.getCamera();

        // 视图矩阵：仅旋转，天空球始终以相机为中心
        Matrix4f viewMatrix = new Matrix4f().rotation(
                camera.rotation().conjugate(new Quaternionf())
        );

        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(viewMatrix);
        RenderSystem.applyModelViewMatrix();

        // 天空渲染状态：无深度写入，在一切之后
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();

        RenderSystem.setShader(() -> shader);

        if (mc.level != null) {
            var gtUniform = shader.getUniform("GameTime");
            if (gtUniform != null) {
                gtUniform.set((float) (mc.level.getGameTime() % 24000L) / 24000.0f);
            }
        }

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

        buildSkyDome(builder, SKY_RADIUS, SEGMENTS, RINGS);

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);

        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
    }

    private static void buildSkyDome(BufferBuilder builder, float radius,
                                      int segments, int rings) {
        for (int i = 0; i < rings; i++) {
            double theta1 = Math.PI * i / rings;
            double theta2 = Math.PI * (i + 1) / rings;

            float sinT1 = (float) Math.sin(theta1), cosT1 = (float) Math.cos(theta1);
            float sinT2 = (float) Math.sin(theta2), cosT2 = (float) Math.cos(theta2);

            for (int j = 0; j < segments; j++) {
                double phi1 = 2.0 * Math.PI * j / segments;
                double phi2 = 2.0 * Math.PI * (j + 1) / segments;

                float sinP1 = (float) Math.sin(phi1), cosP1 = (float) Math.cos(phi1);
                float sinP2 = (float) Math.sin(phi2), cosP2 = (float) Math.cos(phi2);

                float x1 = sinT1 * cosP1, y1 = cosT1, z1 = sinT1 * sinP1;
                float x2 = sinT1 * cosP2, y2 = cosT1, z2 = sinT1 * sinP2;
                float x3 = sinT2 * cosP2, y3 = cosT2, z3 = sinT2 * sinP2;
                float x4 = sinT2 * cosP1, y4 = cosT2, z4 = sinT2 * sinP1;

                builder.addVertex(x1 * radius, y1 * radius, z1 * radius);
                builder.addVertex(x3 * radius, y3 * radius, z3 * radius);
                builder.addVertex(x2 * radius, y2 * radius, z2 * radius);

                builder.addVertex(x1 * radius, y1 * radius, z1 * radius);
                builder.addVertex(x4 * radius, y4 * radius, z4 * radius);
                builder.addVertex(x3 * radius, y3 * radius, z3 * radius);
            }
        }
    }
}
