package com.haloclient.client.gui.click;

import com.haloclient.client.HaloClient;
import com.haloclient.client.module.Category;
import com.haloclient.client.module.Module;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.FontManager;
import com.haloclient.client.render.font.HaloFontRenderState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import java.util.ArrayList;
import java.util.List;

public class Panel {
    public Category category;
    public float x, y, width, height;
    public boolean dragging;
    public float dragX, dragY;
    public boolean extended = true;
    public List<ModuleButton> buttons = new ArrayList<>();

    public Panel(Category category, float x, float y, float width, float height) {
        this.category = category;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        float offset = 0;
        for (Module module : HaloClient.INSTANCE.getModuleManager().getModulesByCategory(category)) {
            buttons.add(new ModuleButton(module, this, offset));
            offset += height;
        }
    }

    public void drawScreen(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (dragging) {
            x = mouseX - dragX;
            y = mouseY - dragY;
        }

        var textureSetup = CaptureManager.getCaptureTextureSetup();
        int headerColor = ARGB.color(200, 20, 20, 20);

        // Draw header
        graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_BLUR,
                textureSetup,
                new Matrix3x2f(graphics.pose()),
                x, y, width, height,
                headerColor,
                5.0f,
                10.0f,
                0.0f,
                graphics.scissorStack.peek()
        ));

        // Draw category name
        var font = FontManager.getFont("productsans-bold.ttf", 18f);
        if (font != null) {
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    font,
                    category.getName(),
                    new Matrix3x2f(graphics.pose()),
                    x + 5, y + (height - font.getHeight()) / 2f + 2,
                    ARGB.color(255, 255, 255, 255),
                    graphics.scissorStack.peek()
            ));
        }

        if (extended) {
            for (ModuleButton button : buttons) {
                button.drawScreen(graphics, mouseX, mouseY, delta);
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY)) {
            if (button == 0) {
                dragging = true;
                dragX = (float) (mouseX - x);
                dragY = (float) (mouseY - y);
            } else if (button == 1) {
                extended = !extended;
            }
        }
        
        if (extended) {
            for (ModuleButton mb : buttons) {
                mb.mouseClicked(mouseX, mouseY, button);
            }
        }
    }

    public void mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
    }

    public boolean isHovered(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
