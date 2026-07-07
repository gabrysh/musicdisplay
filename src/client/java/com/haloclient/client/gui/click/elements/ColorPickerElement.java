package com.haloclient.client.gui.click.elements;

import com.haloclient.client.gui.click.ClickGUI;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFontManager;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import java.awt.Color;

public class ColorPickerElement {

    private static final float POPUP_WIDTH = 120.0f;
    private static final float POPUP_HEIGHT = 128.0f;

    public static void drawColorPicker(
            @Nullable GuiGraphicsExtractor graphics,
            @Nullable Matrix3x2f pose,
            @Nullable TextureSetup textureSetup,
            @Nullable ScreenRectangle scissor,
            String label, float lx, float ly, float width, int color, int alphaScale, int alpha, float mouseX, float mouseY,
            boolean extractPass
    ) {
        float cardH = 13.0f;
        float cardY = ly - cardH / 2.0f;
        boolean hovered = ClickGUI.isHovered(mouseX, mouseY, lx, cardY, width, cardH);

        float size = 8.0f;
        float rx = lx + width - size - 10.0f;
        float ry = ly - size / 2.0f;

        // Colored square preview
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (color >> 24) & 0xFF;
        int displayColor = ARGB.color((int)(alpha * (a / 255.0f)), r, g, b);

        if (extractPass) {
            if (graphics != null && pose != null && textureSetup != null) {
                // Card background
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        pose,
                        lx, cardY, width, cardH,
                        ARGB.color(hovered ? 185 : 165, 0, 0, 0),
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Colored square preview
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        pose,
                        rx, ry, size + 6, size,
                        displayColor,
                        2.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Label Text
                var font = MsdfFontManager.getFont("productsans-semibold", 6f);
                if (font != null) {
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            font,
                            label,
                            new Matrix3x2f(pose),
                            lx + 4.0f,
                            ly - font.getHeight(6f) / 2f,
                            6f,
                            ARGB.color(255, 244, 244, 245),
                            scissor
                    ));
                }
            }
        }
    }

    public static void drawColorPickerPopup(
            @Nullable GuiGraphicsExtractor graphics,
            @Nullable Matrix3x2f popupPose,
            @Nullable TextureSetup textureSetup,
            @Nullable ScreenRectangle scissor,
            float px, float py, int alpha, float mouseX, float mouseY,
            boolean extractPass,
            ClickGUI gui
    ) {
        float pad = 6.0f;
        float innerW = POPUP_WIDTH - 2 * pad;
        float hueBarW = 8.0f;
        float gap = 6.0f;
        float sbW = innerW - hueBarW - gap;
        float sbH = 65.0f;
        float alphaBarH = 8.0f;
        float hexH = 14.0f;

        // Combobox layout
        float comboX = px + pad;
        float comboY = py + pad;
        float comboW = 90;
        float comboH = 13.0f;
        float gapBetweenComboAndSB = 5.0f;

        float sbX = px + pad;
        float sbY = comboY + comboH + gapBetweenComboAndSB;
        float hueX = sbX + sbW + gap;
        float hueY = sbY;
        float alphaSliderX = sbX;
        float alphaSliderY = sbY + sbH + 5.0f;
        float hexX = sbX;
        float hexY = alphaSliderY + alphaBarH + 5.0f;

        float inputW = 72.0f;
        float btnW = 14.0f;

        // === Combobox ===
        if (extractPass) {
            if (graphics != null && popupPose != null && textureSetup != null) {
                // Combobox background
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        popupPose,
                        comboX, comboY, comboW, comboH,
                        ARGB.color(165, 0, 0, 0),
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Input background
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        popupPose,
                        hexX, hexY, inputW, hexH,
                        gui.hexInputActive ? ARGB.color(185, 0, 0, 0) : ARGB.color(165, 0, 0, 0),
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Copy button background
                float copyBtnX = hexX + inputW + 4.0f;
                boolean copyHovered = ClickGUI.isHovered(mouseX, mouseY, copyBtnX, hexY, btnW, hexH);
                int copyBg = copyHovered ? ARGB.color(185, 0, 0, 0) : ARGB.color(165, 0, 0, 0);
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        popupPose,
                        copyBtnX, hexY, btnW, hexH,
                        copyBg,
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                // Paste button background
                float pasteBtnX = copyBtnX + btnW + 4.0f;
                boolean pasteHovered = ClickGUI.isHovered(mouseX, mouseY, pasteBtnX, hexY, btnW, hexH);
                int pasteBg = pasteHovered ? ARGB.color(185, 0, 0, 0) : ARGB.color(165, 0, 0, 0);
                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        popupPose,
                        pasteBtnX, hexY, btnW, hexH,
                        pasteBg,
                        3.0f,
                        200.0f,
                        0.0f,
                        scissor
                ));

                var itemFont = MsdfFontManager.getFont("productsans-semibold", 6f);
                if (itemFont != null) {
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            itemFont,
                            gui.selectedTarget.getName(),
                            popupPose,
                            comboX + 4.1f,
                            comboY + comboH / 2.0f - itemFont.getHeight(6f) / 2f - 0.7f,
                            6f,
                            ARGB.color(255, 228, 228, 231),
                            scissor
                    ));
                }

                var iconFont = MsdfFontManager.getFont("fluid-regular", 10f);
                if (iconFont != null) {
                    float arrowCenterX = comboX + comboW - 10.0f;
                    float arrowCenterY = comboY + comboH / 2.0f;
                    String arrowChar = "F";
                    float arrowW = iconFont.getWidth(arrowChar, 10f);
                    float arrowH = iconFont.getHeight(10f);
                    float progress = gui.comboboxAnimation.getValue();

                    // Build a pose that rotates around the arrow center
                    Matrix3x2f arrowPose = new Matrix3x2f(popupPose);
                    arrowPose.translate(arrowCenterX, arrowCenterY);
                    arrowPose.rotate((float) (Math.PI / 2.0 + (1.0 - progress) * Math.PI)); // closed=270° (down), open=90° (up)
                    arrowPose.translate(-arrowCenterX, -arrowCenterY);

                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            iconFont,
                            arrowChar,
                            arrowPose,
                            arrowCenterX - arrowW / 2f,
                            arrowCenterY - arrowH / 2f,
                            10f,
                            ARGB.color(255, 161, 161, 170),
                            scissor
                    ));
                }

                // Close button (X) made of 2 rotated lines
                float closeBtnX = px + POPUP_WIDTH - pad - 13.0f;
                float closeBtnY = comboY;
                float closeBtnW = 13.0f;
                float closeBtnH = 13.0f;
                boolean closeHovered = ClickGUI.isHovered(mouseX, mouseY, closeBtnX, closeBtnY, closeBtnW, closeBtnH);
                int closeColor = closeHovered ? ARGB.color(alpha, 255, 255, 255) : ARGB.color(alpha, 161, 161, 170);

                Matrix3x2f closePose1 = new Matrix3x2f(popupPose);
                closePose1.translate(closeBtnX + closeBtnW / 2.0f, closeBtnY + closeBtnH / 2.0f);
                closePose1.rotate((float) Math.toRadians(45));
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        closePose1,
                        -4.0f, -0.5f, 8.0f, 1.0f,
                        closeColor,
                        0.5f,
                        scissor
                ));

                Matrix3x2f closePose2 = new Matrix3x2f(popupPose);
                closePose2.translate(closeBtnX + closeBtnW / 2.0f, closeBtnY + closeBtnH / 2.0f);
                closePose2.rotate((float) Math.toRadians(-45));
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        closePose2,
                        -4.0f, -0.5f, 8.0f, 1.0f,
                        closeColor,
                        0.5f,
                        scissor
                ));

                // === Saturation/Brightness gradient ===
                int hueRGB = Color.HSBtoRGB(gui.cpHue, 1.0f, 1.0f);
                hueRGB = (hueRGB & 0x00FFFFFF) | 0xFF000000;

                // Horizontal: white -> hue color (90 degrees)
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        popupPose,
                        sbX, sbY, sbW, sbH,
                        ARGB.color(alpha, 255, 255, 255), ARGB.color(alpha, (hueRGB >> 16) & 0xFF, (hueRGB >> 8) & 0xFF, hueRGB & 0xFF),
                        4.0f, 0.0f, 90.0f,
                        scissor
                ));

                // Vertical: transparent -> black (0 degrees)
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        popupPose,
                        sbX, sbY, sbW, sbH,
                        ARGB.color(0, 0, 0, 0), ARGB.color(alpha, 0, 0, 0),
                        4.0f, 0.0f, 0.0f,
                        scissor
                ));

                // SB indicator circle
                float indX = sbX + gui.animatedCpSat * sbW;
                float indY = sbY + (1.0f - gui.animatedCpBri) * sbH;
                float circleScale = gui.selectorCircleAnimation.getValue();
                float circleRadius = 4.0f + 1.5f * circleScale;
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        popupPose,
                        indX - circleRadius, indY - circleRadius, circleRadius * 2.0f, circleRadius * 2.0f,
                        ARGB.color(alpha, 255, 255, 255), ARGB.color(alpha, 255, 255, 255),
                        circleRadius , 1.5f,
                        0.0f,
                        scissor
                ));

                // === Hue rainbow bar ===
                final int SEGMENTS = 12;
                final float segHeight = sbH / SEGMENTS;
                for (int i = 0; i < SEGMENTS; i++) {
                    float hue1 = (float) i / SEGMENTS;
                    float hue2 = (float) (i + 1) / SEGMENTS;
                    int rgb1 = Color.HSBtoRGB(hue1, 1f, 1f);
                    int rgb2 = Color.HSBtoRGB(hue2, 1f, 1f);
                    int color1 = ARGB.color(alpha, (rgb1 >> 16) & 255, (rgb1 >> 8) & 255, rgb1 & 255);
                    int color2 = ARGB.color(alpha, (rgb2 >> 16) & 255, (rgb2 >> 8) & 255, rgb2 & 255);
                    float sy = hueY + i * segHeight;

                    float radius = 0.0f;
                    float cornerMask = 0.0f;
                    if (i == 0) {
                        radius = 2.0f;
                        cornerMask = 4.0f; // Round top, sharp bottom (clear p.y > 0)
                    } else if (i == SEGMENTS - 1) {
                        radius = 2.0f;
                        cornerMask = 3.0f; // Round bottom, sharp top (clear p.y < 0)
                    }

                    graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                            HaloRenderPipelines.ROUNDED_BLUR,
                            textureSetup,
                            popupPose,
                            hueX, sy, hueBarW, segHeight + 0.5f,
                            color1, color2,
                            radius,
                            0.0f,
                            0.0f,
                            0.0f,
                            cornerMask,
                            scissor
                    ));
                }

                // Hue indicator (styled like the alpha indicator: solid horizontal bar wrapping the slider)
                float hueIndY = hueY + gui.cpHue * sbH;
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        popupPose,
                        hueX - 1.0f, hueIndY - 1.5f, hueBarW + 2.0f, 3.0f,
                        ARGB.color(alpha, 255, 255, 255),
                        1.5f,
                        scissor
                ));

                // === Alpha slider ===
                int currentRGB = Color.HSBtoRGB(gui.cpHue, gui.cpSat, gui.cpBri);
                int cr = (currentRGB >> 16) & 0xFF;
                int cg = (currentRGB >> 8) & 0xFF;
                int cb = currentRGB & 0xFF;
                int darkColor = ARGB.color(alpha, 20, 20, 22);
                int opaqueColor = ARGB.color(alpha, cr, cg, cb);

                graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_BLUR,
                        textureSetup,
                        popupPose,
                        alphaSliderX, alphaSliderY, innerW, alphaBarH,
                        darkColor, opaqueColor,
                        2.0f,
                        200.0f,
                        0.0f,
                        90.0f,
                        scissor
                ));

                // Alpha indicator
                float alphaIndX = alphaSliderX + gui.cpAlpha * innerW;
                graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                        HaloRenderPipelines.ROUNDED_RECT,
                        popupPose,
                        alphaIndX - 1.5f, alphaSliderY - 1.0f, 3.0f, alphaBarH + 2.0f,
                        ARGB.color(alpha, 255, 255, 255),
                        1.5f,
                        scissor
                ));

                // === Hex input field & Copy/Paste buttons ===
                var font = MsdfFontManager.getFont("productsans-semibold", 6f);
                if (font != null) {
                    String displayText = "#" + gui.hexInputText.toUpperCase();

                    // Selection highlight
                    if (gui.hexInputActive && gui.hasHexSelection()) {
                        int selMin = Math.min(gui.hexSelStart, gui.hexSelEnd);
                        int selMax = Math.max(gui.hexSelStart, gui.hexSelEnd);
                        float hashW = font.getWidth("#", 6.0f);
                        float beforeSelW = selMin > 0 ? font.getWidth(gui.hexInputText.substring(0, selMin), 6.0f) : 0;
                        float selW = font.getWidth(gui.hexInputText.substring(selMin, selMax), 6.0f);
                        float selHighlightX = hexX + 4.0f + hashW + beforeSelW;

                        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                                HaloRenderPipelines.ROUNDED_RECT,
                                popupPose,
                                selHighlightX, hexY + 2.0f, selW, hexH - 4.0f,
                                ARGB.color((int) (alpha * 0.3f), 100, 150, 255),
                                0.0f,
                                scissor
                        ));
                    }

                    // Text
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            font,
                            displayText,
                            new Matrix3x2f(popupPose),
                            hexX + 4.0f,
                            hexY + hexH / 2.0f - font.getHeight(6.0f) / 2f,
                            6.0f,
                            ARGB.color(alpha, 228, 228, 231),
                            scissor
                    ));

                    // Cursor blink
                    if (gui.hexInputActive) {
                        long now = System.currentTimeMillis();
                        if (now - gui.lastBlinkTime > 500) {
                            gui.cursorVisible = !gui.cursorVisible;
                            gui.lastBlinkTime = now;
                        }
                        if (gui.cursorVisible) {
                            float hashW = font.getWidth("#", 6.0f);
                            float beforeCursorW = gui.hexCursor > 0 ? font.getWidth(gui.hexInputText.substring(0, gui.hexCursor), 6.0f) : 0;
                            float cursorDrawX = hexX + 4.0f + hashW + beforeCursorW;

                            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                                    HaloRenderPipelines.ROUNDED_RECT,
                                    popupPose,
                                    cursorDrawX, hexY + 3.0f, 1.0f, hexH - 6.0f,
                                    ARGB.color(alpha, 228, 228, 231),
                                    0.0f,
                                    scissor
                            ));
                        }
                    }
                }

                // Copy/Paste button icons
                var mdIconFont = MsdfFontManager.getFont("materialicons-regular", 7f);
                if (mdIconFont != null) {
                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            mdIconFont,
                            "\uE14D",
                            new Matrix3x2f(popupPose),
                            copyBtnX + btnW / 2.0f - mdIconFont.getWidth("\uE14D", 7f) / 2f,
                            hexY + hexH / 2.0f - mdIconFont.getHeight(7f) / 2f,
                            7f,
                            ARGB.color(alpha, 220, 220, 220),
                            scissor
                    ));

                    graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                            mdIconFont,
                            "\uE14F",
                            new Matrix3x2f(popupPose),
                            pasteBtnX + btnW / 2.0f - mdIconFont.getWidth("\uE14F", 7f) / 2f,
                            hexY + hexH / 2.0f - mdIconFont.getHeight(7f) / 2f,
                            7f,
                            ARGB.color(alpha, 220, 220, 220),
                            scissor
                    ));
                }

                // Draw the dropdown options list at the very end so it overlays the elements below it
                float comboProgress = gui.comboboxAnimation.getValue();
                if (comboProgress > 0.001f) {
                    float listY = comboY + comboH + 1.0f;
                    float optionH = 12.0f;
                    float listH = ClickGUI.ColorTarget.values().length * optionH;
                    float animListH = listH * comboProgress;

                    ScreenRectangle comboBounds = (new ScreenRectangle(
                            (int) comboX, (int) listY,
                            (int) comboW, (int) Math.ceil(animListH)
                    )).transformMaxBounds(popupPose);
                    ScreenRectangle comboScissor = scissor != null ? scissor.intersection(comboBounds) : comboBounds;

                    // Draw list background
                    graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                            HaloRenderPipelines.ROUNDED_BLUR,
                            textureSetup,
                            popupPose,
                            comboX, listY, comboW, animListH,
                            ARGB.color(165, 0, 0, 0),
                            3.0f,
                            200.0f,
                            0.0f,
                            comboScissor
                    ));

                    for (int i = 0; i < ClickGUI.ColorTarget.values().length; i++) {
                        ClickGUI.ColorTarget target = ClickGUI.ColorTarget.values()[i];
                        float optY = listY + i * optionH;
                        boolean optHovered = ClickGUI.isHovered(mouseX, mouseY, comboX, optY, comboW, optionH) && mouseY <= listY + animListH;

                        if (optHovered && gui.comboboxOpen) {
                            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                                    HaloRenderPipelines.ROUNDED_BLUR,
                                    textureSetup,
                                    popupPose,
                                    comboX + 1.0f, optY + 1.0f, comboW - 2.0f, optionH - 2.0f,
                                    ARGB.color(185, 0, 0, 0),
                                    2.0f,
                                    200.0f,
                                    0.0f,
                                    comboScissor
                            ));
                        }

                        int col = (target == gui.selectedTarget) ? ARGB.color(alpha, 255, 255, 255) : ARGB.color(alpha, 161, 161, 170);

                        if (target == gui.selectedTarget) {
                            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                                    HaloRenderPipelines.ROUNDED_BLUR,
                                    textureSetup,
                                    popupPose,
                                    comboX + comboW - 10.0f, optY + optionH / 2.0f - 1.0f, 2.0f, 2.0f,
                                    ARGB.color(alpha, 255, 255, 255),
                                    1.0f,
                                    200.0f,
                                    0.0f,
                                    comboScissor
                            ));
                        }

                        var subFont = MsdfFontManager.getFont("productsans-semibold", 5.5f);
                        if (subFont != null) {
                            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                                    subFont,
                                    target.getName(),
                                    new Matrix3x2f(popupPose),
                                    comboX + 4.0f,
                                    optY + optionH / 2.0f - subFont.getHeight(5.5f) / 2f,
                                    5.5f,
                                    col,
                                    comboScissor
                            ));
                        }
                    }
                }
            }
        }
    }
}
