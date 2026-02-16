package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import com.lirxowo.shaderprogram.entity.BlackHoleEntity;
import com.lirxowo.shaderprogram.entity.GlassSphereEntity;
import com.lirxowo.shaderprogram.entity.ModEntities;
import com.lirxowo.shaderprogram.entity.SunEntity;
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
import org.lwjgl.opengl.GL11;

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

    private static final KeyMapping PIXELATE_KEY = new KeyMapping(
            "key.shaderprogram.toggle_pixelate",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_P,
            "key.categories.shaderprogram"
    );

    private static final KeyMapping SEASCAPE_KEY = new KeyMapping(
            "key.shaderprogram.toggle_seascape",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "key.categories.shaderprogram"
    );

    @EventBusSubscriber(modid = Shaderprogram.MODID, value = Dist.CLIENT)
    public static class ModBusEvents {

        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(DISSOLVE_KEY);
            event.register(TILE_KEY);
            event.register(TIME_STOP_KEY);
            event.register(PIXELATE_KEY);
            event.register(SEASCAPE_KEY);
        }

        @SubscribeEvent
        public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.GLASS_SPHERE.get(), GlassSphereRenderer::new);
            event.registerEntityRenderer(ModEntities.BLACK_HOLE.get(), BlackHoleRenderer::new);
            event.registerEntityRenderer(ModEntities.SUN.get(), SunRenderer::new);
        }

        @SubscribeEvent
        public static void onRegisterShaders(RegisterShadersEvent event) throws IOException {
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:glass_sphere",
                            DefaultVertexFormat.POSITION),
                    instance -> GlassSphereShader.instance = instance
            );
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:enchant_glint",
                            DefaultVertexFormat.POSITION_TEX),
                    instance -> EnchantGlintShader.instance = instance
            );
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:synthwave_sky",
                            DefaultVertexFormat.POSITION),
                    instance -> SynthwaveSkyShader.instance = instance
            );
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:black_hole",
                            DefaultVertexFormat.POSITION),
                    instance -> BlackHoleShader.instance = instance
            );
            event.registerShader(
                    new ShaderInstance(event.getResourceProvider(),
                            "shaderprogram:sun",
                            DefaultVertexFormat.POSITION),
                    instance -> SunShader.instance = instance
            );
        }
    }

    @EventBusSubscriber(modid = Shaderprogram.MODID, value = Dist.CLIENT)
    public static class GameBusEvents {

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            while (DISSOLVE_KEY.consumeClick()) {
                SynthwaveEffect.toggle();
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
            while (PIXELATE_KEY.consumeClick()) {
                PixelateEffect.toggle();
            }
            while (SEASCAPE_KEY.consumeClick()) {
                SeascapeEffect.toggle();
            }
            SynthwaveEffect.tick();
            TimeStopEffect.tick();
            PixelateEffect.tick();
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
                SynthwaveEffect.applyUniforms();
                TileEffect.applyUniforms();
                SeascapeEffect.applyUniforms();
                TimeStopEffect.applyPostUniforms();
                PixelateEffect.applyPostUniforms();
                SynthwaveSkyRenderer.render(event);
            }
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_WEATHER) {
                renderGlassSpheres(event);
                renderSuns(event);
                renderBlackHoles(event);
                EnchantGlintRenderer.render(event);
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

            // 相机视图矩阵（仅旋转，无平移）
            Matrix4f viewMatrix = new Matrix4f().rotation(
                    camera.rotation().conjugate(new Quaternionf())
            );
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();

            for (GlassSphereEntity entity : spheres) {
                float dx = (float) (entity.getX() - camPos.x);
                float dy = (float) (entity.getY() - camPos.y);
                float dz = (float) (entity.getZ() - camPos.z);

                // ModelViewMat = viewMatrix * T(dx,dy,dz)
                // 这样顶点数据保持在物体空间（球心在原点），
                // 着色器中 normalize(Position) 即为球体法线
                modelViewStack.set(viewMatrix);
                modelViewStack.translate(dx, dy, dz);
                RenderSystem.applyModelViewMatrix();

                float radius = 2.5f;
                boolean cameraInside = dx * dx + dy * dy + dz * dz < radius * radius;

                BufferBuilder builder = Tesselator.getInstance().begin(
                        VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

                buildSphere(builder, radius, 48, 24);

                RenderSystem.depthMask(true);
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();
                RenderSystem.enableCull();

                if (cameraInside) {
                    // 相机在球内：剔除正面，只渲染内壁（背面）
                    GL11.glCullFace(GL11.GL_FRONT);
                }
                // 相机在球外：默认剔除背面，只渲染外壁（正面）
                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, GlassSphereShader.getCaptureTexId());

                var screenSizeUniform = shader.getUniform("ScreenSize");
                if (screenSizeUniform != null) {
                    screenSizeUniform.set(screenWidth, screenHeight);
                }

                BufferUploader.drawWithShader(builder.buildOrThrow());

                if (cameraInside) {
                    GL11.glCullFace(GL11.GL_BACK); // 恢复默认
                }
            }

            // 恢复模型视图堆栈
            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        /**
         * 生成 UV 球体网格。
         * <p>
         * 球心在原点，顶点坐标即为法线方向乘以半径。
         * 三角形缠绕方向为 CCW（从外部观察），
         * 配合 gl_FrontFacing 可区分内外表面。
         *
         * @param builder  顶点缓冲构建器
         * @param radius   球体半径
         * @param segments 经线分段数（沿赤道方向）
         * @param rings    纬线分段数（从北极到南极）
         */
        private static void buildSphere(BufferBuilder builder, float radius,
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

                    // 单位球面上的四个顶点（同时也是法线方向）
                    float x1 = sinT1 * cosP1, y1 = cosT1, z1 = sinT1 * sinP1;
                    float x2 = sinT1 * cosP2, y2 = cosT1, z2 = sinT1 * sinP2;
                    float x3 = sinT2 * cosP2, y3 = cosT2, z3 = sinT2 * sinP2;
                    float x4 = sinT2 * cosP1, y4 = cosT2, z4 = sinT2 * sinP1;

                    // 三角形 1: v1 -> v2 -> v3 (CCW from outside)
                    builder.addVertex(x1 * radius, y1 * radius, z1 * radius);
                    builder.addVertex(x2 * radius, y2 * radius, z2 * radius);
                    builder.addVertex(x3 * radius, y3 * radius, z3 * radius);

                    // 三角形 2: v1 -> v3 -> v4
                    builder.addVertex(x1 * radius, y1 * radius, z1 * radius);
                    builder.addVertex(x3 * radius, y3 * radius, z3 * radius);
                    builder.addVertex(x4 * radius, y4 * radius, z4 * radius);
                }
            }
        }

        private static void renderBlackHoles(RenderLevelStageEvent event) {
            ShaderInstance shader = BlackHoleShader.instance;
            if (shader == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();

            AABB searchBox = AABB.ofSize(camPos, 256, 256, 256);
            List<BlackHoleEntity> blackHoles = mc.level.getEntitiesOfClass(
                    BlackHoleEntity.class, searchBox
            );
            if (blackHoles.isEmpty()) return;

            BlackHoleShader.captureScene();
            if (BlackHoleShader.getCaptureTexId() == -1) return;

            float screenWidth = mc.getWindow().getWidth();
            float screenHeight = mc.getWindow().getHeight();

            // viewMatrix: 世界空间 → 视空间（纯旋转）
            Matrix4f viewMatrix = new Matrix4f().rotation(
                    camera.rotation().conjugate(new Quaternionf())
            );
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();

            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

            for (BlackHoleEntity entity : blackHoles) {
                // partialTick 插值：消除 20 TPS tick 与 60+ FPS 渲染之间的跳帧
                double ex = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
                double ey = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
                double ez = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;

                float dx = (float) (ex - camPos.x);
                float dy = (float) (ey - camPos.y);
                float dz = (float) (ez - camPos.z);

                // 将实体位置变换到视空间
                float vx = viewMatrix.m00() * dx + viewMatrix.m10() * dy + viewMatrix.m20() * dz;
                float vy = viewMatrix.m01() * dx + viewMatrix.m11() * dy + viewMatrix.m21() * dz;
                float vz = viewMatrix.m02() * dx + viewMatrix.m12() * dy + viewMatrix.m22() * dz;

                // ModelViewMat = 纯平移（无旋转）→ billboard 永远面向相机
                modelViewStack.identity();
                modelViewStack.translate(vx, vy, vz);
                RenderSystem.applyModelViewMatrix();

                float radius = 3.0f;

                BufferBuilder builder = Tesselator.getInstance().begin(
                        VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

                // Billboard 四边形（视空间 XY 平面）
                builder.addVertex(-radius, -radius, 0);
                builder.addVertex( radius, -radius, 0);
                builder.addVertex( radius,  radius, 0);
                builder.addVertex(-radius, -radius, 0);
                builder.addVertex( radius,  radius, 0);
                builder.addVertex(-radius,  radius, 0);

                RenderSystem.depthMask(false);
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.disableCull();

                RenderSystem.setShader(() -> shader);
                RenderSystem.setShaderTexture(0, BlackHoleShader.getCaptureTexId());

                var screenSizeUniform = shader.getUniform("ScreenSize");
                if (screenSizeUniform != null) {
                    screenSizeUniform.set(screenWidth, screenHeight);
                }

                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.enableCull();

            modelViewStack.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }

        private static void renderSuns(RenderLevelStageEvent event) {
            ShaderInstance shader = SunShader.instance;
            if (shader == null) return;

            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            Camera camera = event.getCamera();
            Vec3 camPos = camera.getPosition();

            AABB searchBox = AABB.ofSize(camPos, 128, 128, 128);
            List<SunEntity> suns = mc.level.getEntitiesOfClass(
                    SunEntity.class, searchBox
            );
            if (suns.isEmpty()) return;

            Matrix4f viewMatrix = new Matrix4f().rotation(
                    camera.rotation().conjugate(new Quaternionf())
            );
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();

            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);

            for (SunEntity entity : suns) {
                double ex = entity.xOld + (entity.getX() - entity.xOld) * partialTick;
                double ey = entity.yOld + (entity.getY() - entity.yOld) * partialTick;
                double ez = entity.zOld + (entity.getZ() - entity.zOld) * partialTick;

                float dx = (float) (ex - camPos.x);
                float dy = (float) (ey - camPos.y);
                float dz = (float) (ez - camPos.z);

                float vx = viewMatrix.m00() * dx + viewMatrix.m10() * dy + viewMatrix.m20() * dz;
                float vy = viewMatrix.m01() * dx + viewMatrix.m11() * dy + viewMatrix.m21() * dz;
                float vz = viewMatrix.m02() * dx + viewMatrix.m12() * dy + viewMatrix.m22() * dz;

                modelViewStack.identity();
                modelViewStack.translate(vx, vy, vz);
                RenderSystem.applyModelViewMatrix();

                float radius = 3.0f;

                BufferBuilder builder = Tesselator.getInstance().begin(
                        VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);

                builder.addVertex(-radius, -radius, 0);
                builder.addVertex( radius, -radius, 0);
                builder.addVertex( radius,  radius, 0);
                builder.addVertex(-radius, -radius, 0);
                builder.addVertex( radius,  radius, 0);
                builder.addVertex(-radius,  radius, 0);

                RenderSystem.depthMask(false);
                RenderSystem.enableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                RenderSystem.disableCull();

                RenderSystem.setShader(() -> shader);

                BufferUploader.drawWithShader(builder.buildOrThrow());
            }

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();

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
                SynthwaveEffect.setEntityDissolveProgress(progress);
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

                SynthwaveEffect.setEntityDissolveProgress(0.0f);
            }
        }
    }
}
