package com.haloclient.client.gui.click.elements;

import com.haloclient.client.gui.click.ClickGUI;
import com.haloclient.client.gui.click.MusicDisplayOverlay;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFontManager;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public class GlassStyleComboBoxElement {

    public static void drawGlassStyleComboBox(
            @Nullable GuiGraphics graphics,
            @Nullable Matrix3x2f pose,
            @Nullable TextureSetup textureSetup,
            @Nullable ScreenRectangle scissor,
            float rightColX, float colWidth, float y, int alpha, float mouseX, float mouseY,
            boolean extractPass,
            ClickGUI gui
    ) {
        float styleLy = y + 99.0f;
        float styleCardH = 10.0f;
        float styleCardY = styleLy - styleCardH / 2.0f;
        float styleRectW = 55.0f;
        float styleRectX = rightColX + colWidth - styleRectW - 3.0f;
        boolean styleHovered = ClickGUI.isHovered(mouseX, mouseY, styleRectX, styleCardY, styleRectW, styleCardH);
        int styleRectBg = styleHovered ? ARGB.color(200, 0, 0, 0) : ARGB.color(185, 0, 0, 0);

        if (extractPass) {
            if (graphics != null && pose != null && textureSetup != null) {
                // Combobox background
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        pose,
                        styleRectX, styleCardY, styleRectW, styleCardH,
                        styleRectBg,
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Label Text
                var font = MsdfFontManager.getFont("productsans-semibold", 6f);
                if (font != null) {
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            font,
                            "Glass Style",
                            new Matrix3x2f(pose),
                            rightColX + 4.0f,
                            styleLy - font.getHeight(6f) / 2f,
                            6f,
                            ARGB.color(255, 244, 244, 245),
                            scissor
                    ));

                    // Selected value text
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            font,
                            MusicDisplayOverlay.getBackgroundType().getName(),
                            new Matrix3x2f(pose),
                            styleRectX + 4.0f,
                            styleLy - font.getHeight(5.5f) / 2f,
                            5.5f,
                            ARGB.color(255, 228, 228, 231),
                            scissor
                    ));
                }

                // Dropdown arrow — rotate based on animation progress
                var iconFont = MsdfFontManager.getFont("fluid-regular", 8f);
                if (iconFont != null) {
                    float arrowCenterX = styleRectX + styleRectW - 6.0f;
                    float arrowCenterY = styleLy;
                    String arrowChar = "F";
                    float arrowW = iconFont.getWidth(arrowChar, 8f);
                    float arrowH = iconFont.getHeight(8f);
                    float progress = gui.bgStyleComboAnimation.getValue();

                    // Build a pose that rotates around the arrow center
                    Matrix3x2f arrowPose = new Matrix3x2f(pose);
                    arrowPose.translate(arrowCenterX, arrowCenterY);
                    arrowPose.rotate((float) (Math.PI / 2.0 + (1.0 - progress) * Math.PI)); // closed=270° (down), open=90° (up)
                    arrowPose.translate(-arrowCenterX, -arrowCenterY);

                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            iconFont,
                            arrowChar,
                            arrowPose,
                            arrowCenterX - arrowW / 2f,
                            arrowCenterY - arrowH / 2f,
                            8f,
                            ARGB.color(255, 161, 161, 170),
                            scissor
                    ));
                }

                // Glass Style Dropdown rendering
                float bgComboProgress = gui.bgStyleComboAnimation.getValue();
                if (bgComboProgress > 0.001f) {
                    float styleListY = styleCardY + styleCardH + 1.0f;
                    float styleOptionH = 12.0f;
                    float styleListH = MusicDisplayOverlay.BackgroundType.values().length * styleOptionH;
                    float styleAnimListH = styleListH * bgComboProgress;

                    // Compute a scissor rectangle for the animated dropdown area
                    ScreenRectangle dropdownBounds = (new ScreenRectangle(
                            (int) styleRectX, (int) styleListY,
                            (int) styleRectW, (int) Math.ceil(styleAnimListH)
                    )).transformMaxBounds(pose);
                    ScreenRectangle dropdownScissor = scissor != null ? scissor.intersection(dropdownBounds) : dropdownBounds;

                    // List Background — uses the dropdown scissor
                    graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                            HaloRenderPipelines.ROUNDED_BLUR,
                            textureSetup,
                            pose,
                            styleRectX, styleListY, styleRectW - 0, styleAnimListH,
                            ARGB.color(185, 0, 0, 0),
                            3.0f,
                            200.0f,
                            0.0f,
                            dropdownScissor
                    ));

                    for (int i = 0; i < MusicDisplayOverlay.BackgroundType.values().length; i++) {
                        MusicDisplayOverlay.BackgroundType type = MusicDisplayOverlay.BackgroundType.values()[i];
                        float optY = styleListY + i * styleOptionH;
                        boolean optHovered = ClickGUI.isHovered(mouseX, mouseY, styleRectX, optY, styleRectW, styleOptionH) && mouseY <= styleListY + styleAnimListH;

                        if (optHovered && gui.bgStyleComboOpen) {
                            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                                    HaloRenderPipelines.ROUNDED_BLUR,
                                    textureSetup,
                                    pose,
                                    styleRectX + 1.0f, optY + 1.0f, styleRectW - 2.0f, styleOptionH - 2.0f,
                                    ARGB.color(200, 0, 0, 0),
                                    2.0f,
                                    200.0f,
                                    0.0f,
                                    dropdownScissor
                            ));
                        }

                        int col = (type == MusicDisplayOverlay.getBackgroundType()) ? ARGB.color(255, 255, 255, 255) : ARGB.color(255, 161, 161, 170);
                        if (type == MusicDisplayOverlay.getBackgroundType()) {
                            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                                    HaloRenderPipelines.ROUNDED_BLUR,
                                    textureSetup,
                                    pose,
                                    styleRectX + styleRectW - 8.0f, optY + styleOptionH / 2.0f - 1.0f, 2.0f, 2.0f,
                                    ARGB.color(255, 255, 255, 255),
                                    1.0f,
                                    200.0f,
                                    0.0f,
                                    dropdownScissor
                            ));
                        }

                        var itemFont = MsdfFontManager.getFont("productsans-semibold", 5.5f);
                        if (itemFont != null) {
                            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                                    itemFont,
                                    type.getName(),
                                    new Matrix3x2f(pose),
                                    styleRectX + 4.0f,
                                    optY + styleOptionH / 2.0f + 0.2f - itemFont.getHeight(5.5f) / 2f,
                                    5.5f,
                                    col,
                                    dropdownScissor
                            ));
                        }
                    }
                }
            }
        }
    }
}
