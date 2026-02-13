package com.lirxowo.shaderprogram.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import java.util.Map;

public class EnchantGlintRenderer {

    private static final float INFLATE = 0.002f;
    private static final double MAX_DISTANCE_SQ = 64.0 * 64.0;
    private static final ResourceLocation GLINT_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");

    public static void render(RenderLevelStageEvent event) {
        ShaderInstance shader = EnchantGlintShader.instance;
        if (shader == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Map<BlockPos, ItemEnchantments> enchantedBlocks = EnchantedBlockClientStore.getAll();
        if (enchantedBlocks.isEmpty()) return;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        // Quick check: any blocks in range?
        boolean hasVisible = false;
        for (BlockPos pos : enchantedBlocks.keySet()) {
            double dx = pos.getX() + 0.5 - camPos.x;
            double dy = pos.getY() + 0.5 - camPos.y;
            double dz = pos.getZ() + 0.5 - camPos.z;
            if (dx * dx + dy * dy + dz * dz <= MAX_DISTANCE_SQ) {
                hasVisible = true;
                break;
            }
        }
        if (!hasVisible) return;

        // Set up render state: additive blending, depth test on, depth write off, no cull
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.depthMask(false);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();

        // Bind vanilla enchantment glint texture with GL_REPEAT wrapping
        int texId = mc.getTextureManager().getTexture(GLINT_TEXTURE).getId();
        RenderSystem.setShaderTexture(0, texId);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);

        // Camera view matrix (rotation only)
        Matrix4f viewMatrix = new Matrix4f().rotation(
                camera.rotation().conjugate(new Quaternionf())
        );
        Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
        modelViewStack.pushMatrix();
        modelViewStack.set(viewMatrix);
        RenderSystem.applyModelViewMatrix();

        RenderSystem.setShader(() -> shader);

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_TEX);

        int count = 0;
        for (Map.Entry<BlockPos, ItemEnchantments> entry : enchantedBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            float dx = (float) (pos.getX() - camPos.x);
            float dy = (float) (pos.getY() - camPos.y);
            float dz = (float) (pos.getZ() - camPos.z);

            if ((double) (dx * dx + dy * dy + dz * dz) > MAX_DISTANCE_SQ) continue;

            buildCube(builder,
                    dx - INFLATE, dy - INFLATE, dz - INFLATE,
                    dx + 1 + INFLATE, dy + 1 + INFLATE, dz + 1 + INFLATE);
            count++;
        }

        if (count > 0) {
            BufferUploader.drawWithShader(builder.buildOrThrow());
        }

        // Restore state
        modelViewStack.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Build an axis-aligned cube with per-face UV coordinates (0-1).
     * Each face's UV is derived from the two axes that vary on that face,
     * so the glint pattern stays fixed to the block surface.
     */
    private static void buildCube(BufferBuilder builder,
                                   float x0, float y0, float z0,
                                   float x1, float y1, float z1) {
        // Bottom face (y = y0) — UV from XZ
        builder.addVertex(x0, y0, z1).setUv(0f, 1f);
        builder.addVertex(x1, y0, z1).setUv(1f, 1f);
        builder.addVertex(x1, y0, z0).setUv(1f, 0f);
        builder.addVertex(x0, y0, z1).setUv(0f, 1f);
        builder.addVertex(x1, y0, z0).setUv(1f, 0f);
        builder.addVertex(x0, y0, z0).setUv(0f, 0f);

        // Top face (y = y1) — UV from XZ
        builder.addVertex(x0, y1, z0).setUv(0f, 0f);
        builder.addVertex(x1, y1, z0).setUv(1f, 0f);
        builder.addVertex(x1, y1, z1).setUv(1f, 1f);
        builder.addVertex(x0, y1, z0).setUv(0f, 0f);
        builder.addVertex(x1, y1, z1).setUv(1f, 1f);
        builder.addVertex(x0, y1, z1).setUv(0f, 1f);

        // North face (z = z0) — UV from XY
        builder.addVertex(x1, y0, z0).setUv(0f, 0f);
        builder.addVertex(x1, y1, z0).setUv(0f, 1f);
        builder.addVertex(x0, y1, z0).setUv(1f, 1f);
        builder.addVertex(x1, y0, z0).setUv(0f, 0f);
        builder.addVertex(x0, y1, z0).setUv(1f, 1f);
        builder.addVertex(x0, y0, z0).setUv(1f, 0f);

        // South face (z = z1) — UV from XY
        builder.addVertex(x0, y0, z1).setUv(0f, 0f);
        builder.addVertex(x0, y1, z1).setUv(0f, 1f);
        builder.addVertex(x1, y1, z1).setUv(1f, 1f);
        builder.addVertex(x0, y0, z1).setUv(0f, 0f);
        builder.addVertex(x1, y1, z1).setUv(1f, 1f);
        builder.addVertex(x1, y0, z1).setUv(1f, 0f);

        // West face (x = x0) — UV from ZY
        builder.addVertex(x0, y0, z0).setUv(0f, 0f);
        builder.addVertex(x0, y1, z0).setUv(0f, 1f);
        builder.addVertex(x0, y1, z1).setUv(1f, 1f);
        builder.addVertex(x0, y0, z0).setUv(0f, 0f);
        builder.addVertex(x0, y1, z1).setUv(1f, 1f);
        builder.addVertex(x0, y0, z1).setUv(1f, 0f);

        // East face (x = x1) — UV from ZY
        builder.addVertex(x1, y0, z1).setUv(0f, 0f);
        builder.addVertex(x1, y1, z1).setUv(0f, 1f);
        builder.addVertex(x1, y1, z0).setUv(1f, 1f);
        builder.addVertex(x1, y0, z1).setUv(0f, 0f);
        builder.addVertex(x1, y1, z0).setUv(1f, 1f);
        builder.addVertex(x1, y0, z0).setUv(1f, 0f);
    }
}
