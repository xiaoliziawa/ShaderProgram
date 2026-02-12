package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import com.lirxowo.shaderprogram.entity.GlassSphereEntity;
import com.lirxowo.shaderprogram.entity.ModEntities;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.List;

public class ClientEvents {

    private static final KeyMapping DISSOLVE_KEY = new KeyMapping(
            "key.shaderprogram.toggle_dissolve",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            "key.categories.shaderprogram"
    );

    private static final KeyMapping TILE_KEY = new KeyMapping(
            "key.shaderprogram.cycle_tile",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.shaderprogram"
    );

    private static final KeyMapping TIME_STOP_KEY = new KeyMapping(
            "key.shaderprogram.toggle_time_stop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "key.categories.shaderprogram"
    );

    @EventBusSubscriber(modid = Shaderprogram.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DISSOLVE_KEY);
            event.register(TILE_KEY);
            event.register(TIME_STOP_KEY);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GLASS_SPHERE.get(), GlassSphereRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:glass_sphere",
                            DefaultVertexFormat.POSITION_TEX),
                    instance -> GlassSphereShader.instance = instance
            );
        }
    }

    @EventBusSubscriber(modid = Shaderprogram.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (DISSOLVE_KEY.consumeClick()) {
                DissolveEffect.toggle();
            }
            while (TILE_KEY.consumeClick()) {
                TileEffect.cycle();
            }
            while (TIME_STOP_KEY.consumeClick()) {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    TimeStopEffect.toggle(mc.player);
                }
            }
            DissolveEffect.tick();
            TimeStopEffect.tick();
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                DissolveEffect.applyUniforms();
                TileEffect.applyUniforms();
                TimeStopEffect.applyPostUniforms();
            }
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                renderGlassSpheres(event);
            }
        }

        private static void renderGlassSpheres(RenderLevelStageEvent event) {
            ShaderInstance shader = GlassSphereShader.instance;
            if (shader == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();

            AABB searchBox = AABB.ofSize(camPos, 128, 128, 128);
            List<GlassSphereEntity> spheres = mc.level.getEntitiesOfClass(
                    GlassSphereEntity.class, searchBox
            );
            if (spheres.isEmpty()) return;

            // 捕获帧缓冲区 -- 在AFTER_WEATHER阶段，水面、半透明方块、
            // 粒子、云层和天气效果都已经绘制完毕。
            GlassSphereShader.captureScene();
            if (GlassSphereShader.getCaptureTexId() == -1) return;

            float screenWidth = mc.getWindow().getWidth();
            float screenHeight = mc.getWindow().getHeight();

            // 设置模型视图矩阵为相机视图矩阵，
            // 与实体渲染内部使用的一致。
            // 在AFTER_WEATHER阶段，模型视图堆栈已被弹出，
            // 所以必须重新应用相机视图矩阵。
            Matrix4f viewMatrix = new Matrix4f().rotation(
                    camera.rotation().conjugate(new Quaternionf())
            );
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            modelViewStack.set(viewMatrix);
            RenderSystem.applyModelViewMatrix();

            // 全新的单位PoseStack -- 与实体渲染使用的相同。
            // 实体位置相对于相机。
            PoseStack poseStack = new PoseStack();
            Quaternionf billboard = mc.getEntityRenderDispatcher().cameraOrientation();

            for (GlassSphereEntity entity : spheres) {
                double dx = entity.getX() - camPos.x;
                double dy = entity.getY() - camPos.y;
                double dz = entity.getZ() - camPos.z;

                poseStack.pushPose();
                poseStack.translate(dx, dy, dz);
                poseStack.mulPose(billboard);

                Matrix4f matrix = poseStack.last().pose();
                float halfSize = 2.5f;

                BufferBuilder builder = Tesselator.getInstance().begin(
                        VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                builder.addVertex(matrix, -halfSize, -halfSize, 0).setUv(0, 0);
                builder.addVertex(matrix,  halfSize, -halfSize, 0).setUv(1, 0);
                builder.addVertex(matrix,  halfSize,  halfSize, 0).setUv(1, 1);
                builder.addVertex(matrix, -halfSize,  halfSize, 0).setUv(0, 1);

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, GlassSphereShader.getCaptureTexId());

                var screenSizeUniform = shader.getUniform("ScreenSize");
                if (screenSizeUniform != null) {
                    screenSizeUniform.set(screenWidth, screenHeight);
                }

                BufferUploader.drawWithShader(builder.buildOrThrow());

                RenderSystem.depthMask(true);
                RenderSystem.disableBlend();

                poseStack.popPose();
            }

            // 恢复模型视图堆栈
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        @SubscribeEvent
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            LivingEntity entity = event.getEntity();
            if (entity.deathTime > 0) {
                MultiBufferSource source = event.getMultiBufferSource();
                if (source instanceof MultiBufferSource.BufferSource bufferSource) {
                    bufferSource.endBatch();
                }

                float progress = Math.min(1.0f, (entity.deathTime + event.getPartialTick()) / 20.0f);
                DissolveEffect.setEntityDissolveProgress(progress);
            }
        }

        @SubscribeEvent
        public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
            LivingEntity entity = event.getEntity();
            if (entity.deathTime > 0) {
                MultiBufferSource source = event.getMultiBufferSource();
                if (source instanceof MultiBufferSource.BufferSource bufferSource) {
                    bufferSource.endBatch();
                }

                DissolveEffect.setEntityDissolveProgress(0.0f);
            }
        }
    }
}
