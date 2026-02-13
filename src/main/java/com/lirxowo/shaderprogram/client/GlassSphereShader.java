package com.lirxowo.shaderprogram.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

/**
 * 玻璃球体着色器管理类。
 * <p>
 * 负责两件事：
 * <ol>
 *   <li>持有 {@code glass_sphere} 着色器实例（在 ClientEvents 中注册）</li>
 *   <li>每帧将当前帧缓冲区截图到一张 GPU 纹理，供着色器做折射采样</li>
 * </ol>
 * <p>
 * 工作流程：在 AFTER_WEATHER 渲染阶段调用 {@link #captureScene()} 截取画面，
 * 然后渲染玻璃球时将截图纹理绑定到 Sampler0，片段着色器通过它实现透镜折射效果。
 */
public class GlassSphereShader {

    /** 已编译的着色器程序实例，由 RegisterShadersEvent 回调赋值 */
    @Nullable
    public static ShaderInstance instance;

    /** GPU 上截图纹理的句柄，-1 表示尚未创建 */
    private static int captureTexId = -1;

    /** 上次创建纹理时的窗口尺寸，用于检测窗口 resize */
    private static int lastWidth = -1;
    private static int lastHeight = -1;

    /**
     * @return 截图纹理的 OpenGL ID，未创建时返回 -1
     */
    public static int getCaptureTexId() {
        return captureTexId;
    }

    /**
     * 将当前帧缓冲区内容复制到一张 GPU 纹理中。
     * <p>
     * 在 AFTER_WEATHER 阶段调用，此时天空、方块、实体、半透明物体、
     * 粒子、云层和天气效果都已绘制完毕，截取到的画面最为完整。
     * <p>
     * 若窗口尺寸发生变化，会销毁旧纹理并重新分配匹配大小的新纹理。
     * 数据始终留在 GPU 上（glCopyTexSubImage2D），不经过 CPU，开销较小。
     */
    public static void captureScene() {
        Minecraft mc = Minecraft.getInstance();
        int width = mc.getWindow().getWidth();
        int height = mc.getWindow().getHeight();

        if (width <= 0 || height <= 0) return;

        // 首次调用或窗口尺寸变化时，（重新）创建纹理
        if (captureTexId == -1 || width != lastWidth || height != lastHeight) {
            // 释放旧纹理，防止显存泄漏
            if (captureTexId != -1) {
                glDeleteTextures(captureTexId);
            }

            // glGenTextures: 在 GPU 上分配一个新纹理对象，返回其句柄 ID
            captureTexId = glGenTextures();

            // glBindTexture: 将该纹理绑定为当前操作目标，
            // 后续所有 glTex* 调用都作用于此纹理
            glBindTexture(GL_TEXTURE_2D, captureTexId);

            // glTexImage2D: 分配纹理存储空间（不填充数据）
            //   GL_RGB8   — GPU 内部格式：每通道 8bit，RGB 三通道
            //   null      — 不传入像素数据，仅分配空间，后续用 glCopyTexSubImage2D 填充
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB8, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, (ByteBuffer) null);

            // 纹理过滤：LINEAR = 线性插值，采样落在像素之间时混合相邻4像素，画面平滑
            // 若用 NEAREST 则会看到锯齿状像素块
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);  // 缩小时
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);  // 放大时

            // 纹理环绕：CLAMP_TO_EDGE = UV 超出 [0,1] 时钳制到边缘像素颜色
            // 折射采样可能越界，用 clamp 避免出现错误的重复图案
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);  // S = 水平(U)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);  // T = 垂直(V)

            lastWidth = width;
            lastHeight = height;
        }

        // glCopyTexSubImage2D: 将当前帧缓冲区的像素直接复制到已绑定纹理中
        // 数据全程留在 GPU 上，比 glReadPixels + glTexSubImage2D 快得多
        //   参数: (target, mipLevel, texOffsetX, texOffsetY, fbReadX, fbReadY, width, height)
        glBindTexture(GL_TEXTURE_2D, captureTexId);
        glCopyTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height);

        // 解绑纹理，恢复干净状态，避免后续渲染意外使用此纹理
        glBindTexture(GL_TEXTURE_2D, 0);
    }
}
