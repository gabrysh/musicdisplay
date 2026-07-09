package com.haloclient.client.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.CaptureManager;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.jspecify.annotations.Nullable;
import org.lwjgl.opengl.GL33C;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;

public record NVGRenderState(
    Runnable drawCall,
    Matrix3x2fc pose,
    @Nullable ScreenRectangle scissorArea,
    @Nullable ScreenRectangle bounds
) implements GuiElementRenderState {

    public NVGRenderState(Runnable drawCall, Matrix3x2fc pose, @Nullable ScreenRectangle scissorArea) {
        this(drawCall, pose, scissorArea, null);
    }

    @Override
    public void buildVertices(VertexConsumer vertexConsumer) {
        System.out.println("NVGRenderState: buildVertices called");
        // Add a dummy vertex to ensure the engine doesn't cull this element as empty.
        // We must provide ALL elements expected by the ROUNDED_RECT pipeline (Position, Color, UV, CustomData, Color2, ShadowProps).
        HaloVertexConsumer hv = (HaloVertexConsumer) vertexConsumer;
        vertexConsumer.addVertexWith2DPose(pose, 0, 0).setColor(0).setUv(0, 0);
        hv.setCustomData(0, 0, 0, 0);
        hv.setColor2(0);
        hv.setShadowProps(0, 0, 0, 0);

        if (NVGRenderer.beginFrame()) {
            try {
                System.out.println("NVGRenderState: Drawing spotify logo");
                GL33C.glDisable(GL_DEPTH_TEST);
                drawCall.run();
            } catch (Throwable t) {
                System.err.println("Error rendering NVGRenderState: " + t.getMessage());
                t.printStackTrace();
            } finally {
                // We use endFrame(true) to ensure the correct viewport and FBO are set up.
                // Since GLUtility now restores the previous FBO, this is safe.
                NVGRenderer.endFrame(true);
            }
        }
    }

    @Override public RenderPipeline pipeline() { return HaloRenderPipelines.ROUNDED_RECT; }
    @Override public TextureSetup textureSetup() { return CaptureManager.getCaptureTextureSetup(); }
    @Nullable @Override public ScreenRectangle scissorArea() { return scissorArea; }
    @Nullable @Override public ScreenRectangle bounds() { 
        return new net.minecraft.client.gui.navigation.ScreenRectangle(0, 0, 4000, 4000); 
    }
}
