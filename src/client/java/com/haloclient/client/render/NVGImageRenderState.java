package com.haloclient.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL33C;

import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;

public record NVGImageRenderState(
    NVGImageRenderer image,
    Matrix3x2fc pose,
    float x, float y, float width, float height,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public NVGImageRenderState(
        final NVGImageRenderer image,
        final Matrix3x2fc pose,
        final float x, final float y, final float width, final float height,
        @Nullable final ScreenRectangle scissorArea
    ) {
        this(image, pose, x, y, width, height, scissorArea, getBounds(x, y, width, height, pose, scissorArea));
    }

    @Override
    public void buildVertices(final VertexConsumer vertexConsumer) {
        HaloVertexConsumer hv = (HaloVertexConsumer) vertexConsumer;
        vertexConsumer.addVertexWith2DPose(pose, 0, 0).setColor(0).setUv(0, 0);
        hv.setCustomData(0, 0, 0, 0);
        hv.setColor2(0);
        hv.setShadowProps(0, 0, 0, 0);

        if (NVGRenderer.beginFrame()) {
            try {
                GL33C.glDisable(GL_DEPTH_TEST);
                image.drawImageFitRounded(x, y, width, height, 0.0f);
            } catch (Throwable t) {
                System.err.println("Error rendering NVGImageRenderState: " + t.getMessage());
                t.printStackTrace();
            } finally {
                NVGRenderer.endFrame(true);
            }
        }
    }

    @Nullable
    private static ScreenRectangle getBounds(
        final float x, final float y, final float width, final float height,
        final Matrix3x2fc pose, @Nullable final ScreenRectangle scissorArea
    ) {
        ScreenRectangle rectBounds = new ScreenRectangle((int) x, (int) y, (int) width, (int) height).transformMaxBounds(pose);
        return scissorArea != null ? scissorArea.intersection(rectBounds) : rectBounds;
    }

    @Override public RenderPipeline pipeline() { return HaloRenderPipelines.ROUNDED_RECT; }
    @Override public TextureSetup textureSetup() { return CaptureManager.getCaptureTextureSetup(); }
    @Override public Matrix3x2fc pose() { return pose; }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { return bounds; }
}
