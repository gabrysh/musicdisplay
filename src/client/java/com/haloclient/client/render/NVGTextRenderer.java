package com.haloclient.client.render;

import com.google.common.collect.Lists;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.nanovg.NanoVG.*;

public final class NVGTextRenderer {

    private static final int DEFAULT_ALIGNMENT = NVG_ALIGN_CENTER | NVG_ALIGN_LEFT;
    public static boolean blockTextRendering;

    /** Returns the current NanoVG context (lazy-initialized). */
    private static long VG() { return NVGRenderer.getContext(); }

    private final String name;
    private final ByteBuffer fontData;
    private boolean fontRegistered;
    private long fontRegisteredVg;

    public NVGTextRenderer(final String name, final InputStream inputStream) {
        this.name = name;
        this.fontData = IOUtility.ioResourceToByteBuffer(inputStream, 512 * 1024);
        // Don't call nvgCreateFontMem here — VG context may not exist yet.
        // Font will be registered lazily on first use.
    }

    /** Registers the font with the NanoVG context if not done yet. */
    private void ensureFont() {
        long vg = VG();
        if (vg == 0) return;
        if (fontRegistered && fontRegisteredVg == vg) return;
        if (fontData != null) {
            int fontId = nvgCreateFontMem(vg, this.name, this.fontData, false);
            if (fontId == -1) {
                System.err.println("[Halo/NVG] CRITICAL: Failed to register font '" + this.name + "' (nvgCreateFontMem returned -1)");
            } else {
                System.out.println("[Halo/NVG] Font '" + this.name + "' registered with id " + fontId);
            }
            fontRegistered = true;
            fontRegisteredVg = vg;
        }
    }

    public List<String> wrapStringToWidth(String text, float width, float size) {
        float i = 0;
        StringBuilder stringBuilder = new StringBuilder();
        List<String> textList = new ArrayList<>();
        List<String> copyList = Lists.newArrayList(text);

        for (int j = 0; j < copyList.size() && j < 1024; ++j) {
            String part = copyList.get(j);

            boolean flag = false;
            if (text.contains("\n")) {
                int newlineIndex = text.indexOf('\n');
                String s1 = text.substring(newlineIndex + 1);
                text = text.substring(0, newlineIndex + 1);
                copyList.add(j + 1, s1);
                flag = true;
            }

            String s5 = part.endsWith("\n") ? part.substring(0, part.length() - 1) : part;
            float i1 = this.getStringWidth(s5, size);
            String leftOver = s5;

            if (i + i1 > width) {
                String s2 = this.trimStringToWidth(part, width - i, size);
                String s3 = s2.length() < part.length() ? part.substring(s2.length()) : null;

                if (s3 != null) {
                    int l = s2.lastIndexOf(' ');
                    if (l >= 0 && this.getStringWidth(part.substring(0, l), size) > 0) {
                        s2 = part.substring(0, l);
                        s3 = part.substring(l);
                    } else if (i > 0 && !part.contains(" ")) {
                        s2 = "";
                        s3 = part;
                    }

                    if (!s3.isEmpty() && s3.charAt(0) == ' ') {
                        s2 += ' ';
                        s3 = s3.substring(1);
                    }

                    copyList.add(j + 1, s3);
                }

                i1 = this.getStringWidth(s2, size);
                leftOver = s2;
                flag = true;
            }

            if (i + i1 <= width) {
                i += i1;
                stringBuilder.append(leftOver);
            } else {
                flag = true;
            }

            if (flag) {
                textList.add(stringBuilder.toString());
                i = 0;
                stringBuilder = new StringBuilder();
            }
        }

        textList.add(stringBuilder.toString());
        return textList;
    }


    public String trimStringToWidth(String text, float width, float size) {
        StringBuilder stringBuilder = new StringBuilder();
        float f = 0.0F;
        int i = 0;
        int j = 1;
        boolean flag = false;
        boolean flag1 = false;

        for (int k = i; k >= 0 && k < text.length() && f < width; k += j) {
            char character = text.charAt(k);
            float stringWidth = this.getStringWidth(String.valueOf(character), size);

            if (character == COLOR_INVOKER) {
                stringBuilder.append(character);
                if (k != text.length() - 1) {
                    stringBuilder.append(text.charAt(k + 1));
                }
                k++;
                continue;
            }

            if (flag) {
                flag = false;

                if (character != 108 && character != 76) {
                    if (character == 114 || character == 82) {
                        flag1 = false;
                    }
                } else {
                    flag1 = true;
                }
            } else if (stringWidth < 0.0F) {
                flag = true;
            } else {
                f += stringWidth;

                if (flag1) {
                    ++f;
                }
            }

            if (f > width) {
                break;
            }

            stringBuilder.append(character);
        }

        return stringBuilder.toString();
    }

    public float drawStringWithShadow(final String text, final float x, final float y, final float size,
            final int color) {
        drawString(text, x + 0.5F, y + 0.5F, size, color, true, DEFAULT_ALIGNMENT, 0);
        return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT, 0);
    }

    public float drawBlurredStringWithShadow(final String text, final float x, final float y, final float size,
            final int color, final float blur) {
        drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT, blur);
        drawString(text, x + 0.5F, y + 0.5F, size, color, true, DEFAULT_ALIGNMENT, 0);
        return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT, 0);
    }

    public float drawString(final String text, final float x, final float y, final float size, final int color) {
        return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT, 0);
    }

    public float drawBlurredString(final String text, final float x, final float y, final float size, final int color,
            final float blur) {
        return drawString(text, x, y, size, color, false, DEFAULT_ALIGNMENT, blur);
    }

    public void drawGradientString(final String text, final float x, final float y, final float size, final int color1,
            final int color2, final boolean shadow) {
        drawGradientString(text, x, y, size, color1, color2, shadow, 0);
    }

    public void drawBlurredGradientString(final String text, final float x, final float y, final float size,
            final int color1, final int color2, final float blur) {
        drawGradientString(text, x, y, size, color1, color2, false, blur);
    }

    public void drawGradientString(final String text, final float x, final float y, final float size, final int color1,
            final int color2, final boolean shadow, final float blur) {
        if (blockTextRendering) {
            return;
        }

        float offset = 0;

        for (int i = 0; i < text.length(); i++) {
            final char c = text.charAt(i);
            final String character = String.valueOf(c);
            final float characterWidth = getStringWidth(character, size);

            final int color = ColorUtility.interpolateColorsBackAndForth(10, i * 15, color1, color2);

            if (blur > 0) {
                drawString(character, x + offset, y, size, color, false, DEFAULT_ALIGNMENT, blur);
            }

            if (shadow) {
                drawStringWithShadow(character, x + offset, y, size, color);
            } else {
                drawString(character, x + offset, y, size, color);
            }

            offset += characterWidth;
        }
    }

    public void drawGradientString(final String text, final float x, final float y, final float size, final int color1,
            final int color2) {
        drawGradientString(text, x, y, size, color1, color2, false, 0);
    }

    public void drawGradientStringWithShadow(final String text, final float x, final float y, final float size,
            final int color1, final int color2) {
        drawGradientString(text, x, y, size, color1, color2, true, 0);
    }

    public float drawString(final String text, float x, final float y, final float size, final int color,
            final boolean shadow, final int alignment) {
        return drawString(text, x, y, size, color, shadow, alignment, 0);
    }

    public float drawString(final String text, float x, final float y, final float size, final int color,
            final boolean shadow, final int alignment, final float blur) {
        if (blockTextRendering) {
            return x;
        }

        ensureFont();
        final long VG = VG();
        if (VG == 0) return x;

        nvgBeginPath(VG);
        nvgFontFace(VG, this.name);
        nvgFontSize(VG, size);
        nvgTextAlign(VG, alignment);
        nvgFontBlur(VG, blur);

        boolean underline = false;
        boolean strikethrough = false;

        NVGRenderer.applyColor(shadow ? ColorUtility.getShadowColor(color) : color, NVGRenderer.NVG_COLOR_1);
        nvgFillColor(VG, NVGRenderer.NVG_COLOR_1);

        StringBuilder currentSegment = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            final char character = text.charAt(i);
            if (character == COLOR_INVOKER && text.length() > i + 1) {
                if (!currentSegment.isEmpty()) {
                    drawStringSegment(currentSegment.toString(), x, y, size, underline, strikethrough);
                    x += nvgTextBounds(VG, 0, 0, currentSegment.toString(), (FloatBuffer) null);
                    currentSegment.setLength(0);
                }
                int index = getColorCodeCharacter(Character.toLowerCase(text.charAt(i + 1)));
                if (index >= 0) {
                    if (index < 16) {
                        nvgFontFace(VG, name);
                        underline = strikethrough = false;
                        int colorCode = COLOR_CODES[index];
                        if (shadow) {
                            colorCode = ColorUtility.getShadowColor(colorCode);
                        }
                        NVGRenderer.applyColor(ColorUtility.applyOpacity(colorCode, color >> 24 & 0xFF),
                                NVGRenderer.NVG_COLOR_2);
                        nvgFillColor(VG, NVGRenderer.NVG_COLOR_2);
                    } else {
                        switch (index) {
                            case 17 -> nvgFontFace(VG, "productsans-bold");
                            case 18 -> strikethrough = true;
                            case 19 -> underline = true;
                            case 20 -> nvgFontFace(VG, "productsans-italic");
                            case 21 -> { // reset
                                nvgFontFace(VG, name);
                                underline = strikethrough = false;
                                nvgFillColor(VG, NVGRenderer.NVG_COLOR_1);
                            }
                        }
                    }
                }
                i++;
            } else {
                currentSegment.append(character);
            }
        }
        if (!currentSegment.isEmpty()) {
            drawStringSegment(currentSegment.toString(), x, y, size, underline, strikethrough);
            x += nvgTextBounds(VG, 0, 0, currentSegment.toString(), (FloatBuffer) null);
        }

        nvgClosePath(VG);
        return x;
    }

    private int getColorCodeCharacter(char lowerCase) {
        return lowerCase < 128 ? CHAR_TO_INDEX[lowerCase] : -1;
    }

    private void drawStringSegment(String segment, float x, float y, float size, boolean underline,
            boolean strikethrough) {
        final long VG = VG();
        nvgText(VG, x, y, segment);

        final float width = nvgTextBounds(VG, 0, 0, segment, (FloatBuffer) null);

        if (strikethrough) {
            final float strikeY = y - (size * 0.25F);
            nvgBeginPath(VG);
            nvgMoveTo(VG, x, strikeY);
            nvgLineTo(VG, x + width, strikeY + 0.5F);
            nvgFill(VG);
            nvgClosePath(VG);
        }

        if (underline) {
            nvgBeginPath(VG);
            nvgMoveTo(VG, x, y);
            nvgLineTo(VG, x + width, y + 0.5F);
            nvgFill(VG);
            nvgClosePath(VG);
        }
    }

    public float getStringWidth(final String text, final float size) {
        ensureFont();
        final long VG = VG();
        if (VG == 0) return 0;

        nvgFontFace(VG, name);
        nvgFontSize(VG, size);

        StringBuilder currentSegment = new StringBuilder();
        float width = 0.0F;
        final int length = text.length();

        for (int i = 0; i < length; i++) {
            final char character = text.charAt(i);
            if (character == COLOR_INVOKER && i < length - 1) {
                i++;
            } else {
                currentSegment.append(character);
            }

            if ((character == COLOR_INVOKER && i < length - 1) || i == length - 1) {
                if (!currentSegment.isEmpty()) {
                    width += nvgTextBounds(VG, 0, 0, currentSegment.toString(), (FloatBuffer) null);
                    currentSegment.setLength(0);
                }
            }
        }

        return width;
    }

    public float getStringHeight(final String text, final float size) {
        ensureFont();
        final long VG = VG();
        if (VG == 0) return 0;

        nvgFontFace(VG, name);
        nvgFontSize(VG, size);

        try (final MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer bounds = stack.mallocFloat(4);

            nvgTextBounds(VG, 0, 0, text, bounds);

            return bounds.get(3) - bounds.get(1);
        }
    }

    private static final int[] COLOR_CODES = new int[32];
    private static final char COLOR_INVOKER = '\247';
    private static final byte[] CHAR_TO_INDEX = new byte[128];

    private static void initColorCodes() {
        for (int i = 0; i < 32; ++i) {
            final int amplifier = (i >> 3 & 1) * 85;
            int red = (i >> 2 & 1) * 170 + amplifier;
            int green = (i >> 1 & 1) * 170 + amplifier;
            int blue = (i & 1) * 170 + amplifier;
            if (i == 6) {
                red += 85;
            }
            if (i >= 16) {
                red /= 4;
                green /= 4;
                blue /= 4;
            }
            COLOR_CODES[i] = (red & 255) << 16 | (green & 255) << 8 | blue & 255;
        }

        final String colorCodeCharacters = "0123456789abcdefklmnor";

        for (int i = 0; i < 128; i++)
            CHAR_TO_INDEX[i] = -1;
        for (int i = 0; i < colorCodeCharacters.length(); i++) {
            char c = colorCodeCharacters.charAt(i);
            CHAR_TO_INDEX[c] = (byte) i;
            CHAR_TO_INDEX[Character.toLowerCase(c)] = (byte) i;
        }
    }

    static {
        initColorCodes();
    }
}
