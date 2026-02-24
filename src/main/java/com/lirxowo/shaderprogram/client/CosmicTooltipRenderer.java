package com.lirxowo.shaderprogram.client;

import com.lirxowo.shaderprogram.Shaderprogram;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

import java.util.List;

@EventBusSubscriber(modid = Shaderprogram.MODID, value = Dist.CLIENT)
public class CosmicTooltipRenderer {

    private static final int PADDING = 3;
    private static final int Z_LEVEL = 400;

    @SubscribeEvent
    public static void onRenderTooltipPre(RenderTooltipEvent.Pre event) {
        ShaderInstance shader = CosmicTooltipShader.instance;
        if (shader == null) return;

        event.setCanceled(true);

        GuiGraphics graphics = event.getGraphics();
        Font font = event.getFont();
        List<ClientTooltipComponent> components = event.getComponents();
        if (components.isEmpty()) return;

        // Calculate tooltip dimensions (same as vanilla)
        int tooltipWidth = 0;
        int tooltipHeight = components.size() == 1 ? -2 : 0;
        for (ClientTooltipComponent component : components) {
            int w = component.getWidth(font);
            if (w > tooltipWidth) tooltipWidth = w;
            tooltipHeight += component.getHeight();
        }

        // Position tooltip
        ClientTooltipPositioner positioner = event.getTooltipPositioner();
        Vector2ic pos = positioner.positionTooltip(
                event.getScreenWidth(), event.getScreenHeight(),
                event.getX(), event.getY(),
                tooltipWidth, tooltipHeight
        );
        int x = pos.x();
        int y = pos.y();

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();

        // Draw cosmic background
        renderCosmicBackground(graphics, x, y, tooltipWidth, tooltipHeight, shader);

        // Draw border (vanilla style)
        renderBorder(graphics, x, y, tooltipWidth, tooltipHeight);

        // Render text and image components
        poseStack.translate(0.0F, 0.0F, Z_LEVEL);
        int currentY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderText(font, x, currentY, poseStack.last().pose(), graphics.bufferSource());
            currentY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        currentY = y;
        for (int i = 0; i < components.size(); i++) {
            ClientTooltipComponent component = components.get(i);
            component.renderImage(font, x, currentY, graphics);
            currentY += component.getHeight() + (i == 0 ? 2 : 0);
        }

        poseStack.popPose();
    }

    private static void renderCosmicBackground(GuiGraphics graphics, int x, int y,
                                                 int width, int height, ShaderInstance shader) {
        Minecraft mc = Minecraft.getInstance();
        float screenWidth = mc.getWindow().getWidth();
        float screenHeight = mc.getWindow().getHeight();

        CosmicTooltipShader.applyUniforms(screenWidth, screenHeight);

        // Background area with padding
        int bgX = x - PADDING;
        int bgY = y - PADDING;
        int bgW = width + PADDING * 2;
        int bgH = height + PADDING * 2;

        graphics.flush();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        RenderSystem.setShaderTexture(0, mc.getTextureManager()
                .getTexture(InventoryMenu.BLOCK_ATLAS).getId());

        Matrix4f matrix = graphics.pose().last().pose();
        float z = Z_LEVEL;

        BufferBuilder builder = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        // Main background rect
        builder.addVertex(matrix, bgX, bgY, z);
        builder.addVertex(matrix, bgX, bgY + bgH, z);
        builder.addVertex(matrix, bgX + bgW, bgY + bgH, z);
        builder.addVertex(matrix, bgX + bgW, bgY, z);

        // Top edge (1px above)
        builder.addVertex(matrix, bgX, bgY - 1, z);
        builder.addVertex(matrix, bgX, bgY, z);
        builder.addVertex(matrix, bgX + bgW, bgY, z);
        builder.addVertex(matrix, bgX + bgW, bgY - 1, z);

        // Bottom edge (1px below)
        builder.addVertex(matrix, bgX, bgY + bgH, z);
        builder.addVertex(matrix, bgX, bgY + bgH + 1, z);
        builder.addVertex(matrix, bgX + bgW, bgY + bgH + 1, z);
        builder.addVertex(matrix, bgX + bgW, bgY + bgH, z);

        // Left edge (1px left)
        builder.addVertex(matrix, bgX - 1, bgY, z);
        builder.addVertex(matrix, bgX - 1, bgY + bgH, z);
        builder.addVertex(matrix, bgX, bgY + bgH, z);
        builder.addVertex(matrix, bgX, bgY, z);

        // Right edge (1px right)
        builder.addVertex(matrix, bgX + bgW, bgY, z);
        builder.addVertex(matrix, bgX + bgW, bgY + bgH, z);
        builder.addVertex(matrix, bgX + bgW + 1, bgY + bgH, z);
        builder.addVertex(matrix, bgX + bgW + 1, bgY, z);

        BufferUploader.drawWithShader(builder.buildOrThrow());

        RenderSystem.disableBlend();
    }

    private static void renderBorder(GuiGraphics graphics, int x, int y,
                                      int width, int height) {
        int bgX = x - PADDING;
        int bgY = y - PADDING;
        int bgW = width + PADDING * 2;
        int bgH = height + PADDING * 2;

        int borderTop = 0x505000FF;
        int borderBottom = 0x5028007F;

        // Left border
        graphics.fillGradient(bgX, bgY + 1, bgX + 1, bgY + bgH - 1, borderTop, borderBottom, Z_LEVEL);
        // Right border
        graphics.fillGradient(bgX + bgW - 1, bgY + 1, bgX + bgW, bgY + bgH - 1, borderTop, borderBottom, Z_LEVEL);
        // Top border
        graphics.fill(bgX, bgY, bgX + bgW, bgY + 1, Z_LEVEL, borderTop);
        // Bottom border
        graphics.fill(bgX, bgY + bgH - 1, bgX + bgW, bgY + bgH, Z_LEVEL, borderBottom);
    }
}
