package com.haloclient.client.gui.click;

import com.haloclient.client.module.Module;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.FontManager;
import com.haloclient.client.render.font.HaloFontRenderState;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

public class ModuleButton {
    public Module module;
    public Panel parent;
    public float offset;

    public ModuleButton(Module module, Panel parent, float offset) {
        this.module = module;
        this.parent = parent;
        this.offset = offset;
    }

    public void drawScreen(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        float x = parent.x;
        float y = parent.y + parent.height + offset;
        float width = parent.width;
        float height = parent.height;

        int color = module.isEnabled() ? ARGB.color(200, 50, 150, 255) : ARGB.color(150, 30, 30, 30);
        if (isHovered(mouseX, mouseY)) {
            color = ARGB.color(220, 70, 170, 255); // Slightly brighter on hover
        }

        var textureSetup = CaptureManager.getCaptureTextureSetup();
        graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_BLUR,
                textureSetup,
                new Matrix3x2f(graphics.pose()),
                x, y, width, height,
                color,
                2.0f,
                5.0f,
                0.0f,
                graphics.scissorStack.peek()
        ));

        var font = FontManager.getFont("productsans-bold.ttf", 16f);
        if (font != null) {
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    module.getName(),
                    new Matrix3x2f(graphics.pose()),
                    x + 5, y + (height - font.getHeight()) / 2f + 1,
                    ARGB.color(255, 255, 255, 255),
                    graphics.scissorStack.peek()
            ));
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY) && button == 0) {
            module.toggle();
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        float x = parent.x;
        float y = parent.y + parent.height + offset;
        return mouseX >= x && mouseX <= x + parent.width && mouseY >= y && mouseY <= y + parent.height;
    }
}
