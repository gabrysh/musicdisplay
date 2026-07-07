package com.haloclient.client.render;

import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.system.MemoryStack;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.lwjgl.nanovg.NanoSVG.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

public final class NVGImageRenderer {

        private static final Pattern SVG_VIEW_BOX = Pattern.compile(
                "viewBox\\s*=\\s*\"[\\d.]+\\s+[\\d.]+\\s+([\\d.]+)\\s+([\\d.]+)\""
        );

        /** Returns the current NanoVG context (lazy-initialized). */
        private static long VG() { return NVGRenderer.getContext(); }

        private ByteBuffer imageData;
        private ByteBuffer rgbaData;
        private int imageWidth;
        private int imageHeight;
        private final int imageFlags;
        private int imageHandle = -1;

        public NVGImageRenderer(final InputStream inputStream, final int flags) {
                this.imageData = IOUtility.ioResourceToByteBuffer(inputStream, 512 * 1024);
                this.imageFlags = flags;
                // Don't call nvgCreateImageMem here — VG context may not exist yet.
        }

        public NVGImageRenderer(final InputStream inputStream) {
                this(inputStream, NVGRenderer.NVG_IMAGE_GENERATE_MIPMAPS);
        }

        private NVGImageRenderer(final ByteBuffer rgbaData, final int width, final int height, final int flags) {
                this.rgbaData = rgbaData;
                this.imageWidth = width;
                this.imageHeight = height;
                this.imageFlags = flags;
        }

        /**
         * Loads an SVG resource, rasterizes it with NanoSVG, and prepares it for NanoVG rendering.
         *
         * @param inputStream SVG file stream
         * @param rasterSize  longest side of the rasterized bitmap in pixels
         */
        public static NVGImageRenderer fromSvg(final InputStream inputStream, final int rasterSize) {
                return fromSvg(inputStream, rasterSize, NVGRenderer.NVG_IMAGE_GENERATE_MIPMAPS);
        }

        public static NVGImageRenderer fromSvg(final InputStream inputStream, final int rasterSize, final int flags) {
                final ByteBuffer svgData = IOUtility.ioResourceToByteBuffer(inputStream, 64 * 1024);
                if (svgData == null) {
                        return null;
                }

                final float[] viewBox = parseSvgViewBox(svgData);
                final float srcW = viewBox[0];
                final float srcH = viewBox[1];
                if (srcW <= 0.0f || srcH <= 0.0f) {
                        return null;
                }

                // nsvgParse expects a null-terminated C string
                final ByteBuffer ntSvg = memAlloc(svgData.remaining() + 1);
                ntSvg.put(svgData);
                ntSvg.put((byte) 0);
                ntSvg.flip();

                try (MemoryStack stack = MemoryStack.stackPush()) {
                        final NSVGImage svg = nsvgParse(ntSvg, stack.ASCII("px"), 96.0f);
                        if (svg == null) {
                                return null;
                        }

                        try {

                                final float scale = rasterSize / Math.max(srcW, srcH);
                                final int rasterW = Math.max(1, (int) Math.ceil(srcW * scale));
                                final int rasterH = Math.max(1, (int) Math.ceil(srcH * scale));

                                final long rasterizer = nsvgCreateRasterizer();
                                if (rasterizer == 0L) {
                                        return null;
                                }

                                final ByteBuffer pixels = memAlloc(rasterW * rasterH * 4);
                                nsvgRasterize(rasterizer, svg, 0.0f, 0.0f, scale, pixels, rasterW, rasterH, rasterW * 4);
                                nsvgDeleteRasterizer(rasterizer);

                                return new NVGImageRenderer(pixels, rasterW, rasterH, flags);
                        } finally {
                                nsvgDelete(svg);
                        }
                } finally {
                        memFree(ntSvg);
                }
        }

        /** Creates the NVG image handle lazily when the context is ready. */
        private int handle() {
                if (imageHandle != -1) {
                        return imageHandle;
                }

                final long vg = VG();
                if (vg == 0) {
                        return -1;
                }

                if (rgbaData != null) {
                        imageHandle = nvgCreateImageRGBA(vg, imageWidth, imageHeight, imageFlags, rgbaData);
                        memFree(rgbaData);
                        rgbaData = null;
                } else if (imageData != null) {
                        imageHandle = nvgCreateImageMem(vg, imageFlags, imageData);
                        // nvgCreateImageMem copies the data to the GPU — release the
                        // native ByteBuffer immediately to avoid holding 512KB+ per image
                        // in off-heap memory forever.  This was a major memory leak.
                        imageData = null;
                }

                return imageHandle;
        }

        private static float[] parseSvgViewBox(final ByteBuffer svgData) {
                final String text = StandardCharsets.UTF_8.decode(svgData.duplicate()).toString();
                final Matcher matcher = SVG_VIEW_BOX.matcher(text);
                if (matcher.find()) {
                        return new float[]{
                                Float.parseFloat(matcher.group(1)),
                                Float.parseFloat(matcher.group(2))
                        };
                }
                return new float[]{512.0f, 512.0f};
        }

        public void drawImage(final float x, final float y, final float width, final float height) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;

                nvgBeginPath(VG);
                nvgRect(VG, x, y, width, height);
                nvgImagePattern(VG, x, y, width, height, 0, h, 1, NVGRenderer.NVG_PAINT);
                nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                nvgFill(VG);
                nvgClosePath(VG);
        }

        public void drawImage(final float x, final float y, final float width, final float height,
                        final int colorOverlay) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;

                nvgBeginPath(VG);
                nvgRect(VG, x, y, width, height);
                nvgImagePattern(VG, x, y, width, height, 0, h, 1, NVGRenderer.NVG_PAINT);
                NVGRenderer.applyColor(colorOverlay, NVGRenderer.NVG_COLOR_1);
                NVGRenderer.NVG_PAINT.innerColor(NVGRenderer.NVG_COLOR_1);

                nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                nvgFill(VG);
                nvgClosePath(VG);
        }

        public void drawImageRounded(final float x, final float y, final float width, final float height,
                        final float radius) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;

                nvgBeginPath(VG);
                nvgRoundedRect(VG, x, y, width, height, radius);
                nvgImagePattern(VG, x, y, width, height, 0, h, 1, NVGRenderer.NVG_PAINT);
                nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                nvgFill(VG);
                nvgClosePath(VG);
        }

        /**
         * Scales the image so its height exactly fills {@code height}, keeping aspect ratio.
         * The result is centered horizontally within {@code [x, x+width]}.
         * Ideal for portrait mob textures (transparent sides).
         */
        public void drawImageFitHeightRounded(final float x, final float y, final float width, final float height, final float radius) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer hb = stack.mallocInt(1);
                    nvgImageSize(VG, h, w, hb);
                    float imgW = w.get(0);
                    float imgH = hb.get(0);
                    if (imgH <= 0) return;

                    float scale = height / imgH;
                    float drawW = imgW * scale;
                    float drawH = height;
                    float drawX = x + (width - drawW) / 2f;

                    nvgImagePattern(VG, drawX, y, drawW, drawH, 0, h, 1, NVGRenderer.NVG_PAINT);
                    nvgBeginPath(VG);
                    nvgRoundedRect(VG, x, y, width, height, radius);
                    nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                    nvgFill(VG);
                    nvgClosePath(VG);
                }
        }

        public void drawImageFitRounded(final float x, final float y, final float width, final float height, final float radius) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer hb = stack.mallocInt(1);
                    nvgImageSize(VG, h, w, hb);
                    float imgW = w.get(0);
                    float imgH = hb.get(0);

                    float scale = Math.min(width / imgW, height / imgH);
                    float drawW = imgW * scale;
                    float drawH = imgH * scale;
                    float drawX = x + (width - drawW) / 2f;
                    float drawY = y + (height - drawH) / 2f;

                    nvgImagePattern(VG, drawX, drawY, drawW, drawH, 0, h, 1, NVGRenderer.NVG_PAINT);
                    nvgBeginPath(VG);
                    nvgRoundedRect(VG, x, y, width, height, radius);
                    nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                    nvgFill(VG);
                    nvgClosePath(VG);
                }
        }

        public void drawImageCoverRounded(final float x, final float y, final float width, final float height, final float radius) {
                final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return;
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    IntBuffer w = stack.mallocInt(1);
                    IntBuffer hb = stack.mallocInt(1);
                    nvgImageSize(VG, h, w, hb);
                    float imgW = w.get(0);
                    float imgH = hb.get(0);

                    float scale = Math.max(width / imgW, height / imgH);
                    float drawW = imgW * scale;
                    float drawH = imgH * scale;
                    float drawX = x + (width - drawW) / 2f;
                    float drawY = y + (height - drawH) / 2f;

                    nvgImagePattern(VG, drawX, drawY, drawW, drawH, 0, h, 1, NVGRenderer.NVG_PAINT);
                    nvgBeginPath(VG);
                    nvgRoundedRect(VG, x, y, width, height, radius);
                    nvgFillPaint(VG, NVGRenderer.NVG_PAINT);
                    nvgFill(VG);
                    nvgClosePath(VG);
                }
        }

        public int getWidth() {
            final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return 0;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer hb = stack.mallocInt(1);
                nvgImageSize(VG, h, w, hb);
                return w.get(0);
            }
        }

        public int getHeight() {
            final long VG = VG(); int h = handle(); if (VG == 0 || h < 0) return 0;
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer hb = stack.mallocInt(1);
                nvgImageSize(VG, h, w, hb);
                return hb.get(0);
            }
        }
}
