package com.haloclient.client.gui.click;

import com.haloclient.client.render.*;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.render.font.MsdfFont;
import com.haloclient.client.render.font.MsdfFontManager;
import com.haloclient.client.render.renderstates.BlurredLiquidGlassRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.renderstates.ImageRenderState;
import com.haloclient.client.render.renderstates.RoundedRectangleRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;

/**
 * Samodzielny widget HUD (music display) — odseparowany od ekranu ClickGUI.
 */
public final class MusicDisplayOverlay {

    public static final float WIDTH = 175;
    public static final float HEIGHT = 44;

    private static final float PADDING = 6.0f;
    private static final float COVER_SIZE = 32.0f;
    private static final float TEXT_SIZE = 9.0f;
    private static final float ARTISTTEXT_SIZE = 7.0f;
    private static final float TITLE_SIZE = 9.0f;
    private static final float TIME_SIZE = 7.0f;
    private static final float PROGRESS_HEIGHT = 3.0f;
    private static final float DEFAULT_BLUR = 15.0f;
    private static final float DEFAULT_BLOOM = 3.0f;

    private static float relativeX = Float.NaN;
    private static float relativeY = Float.NaN;
    private static boolean visible;
    public static int backgroundColor = ARGB.color(100, 12, 12, 12);
    public static int titleColor = ARGB.color(255, 255, 255, 255);
    public static int artistColor = ARGB.color(145, 255, 255, 255);
    public static int timeColor = ARGB.color(145, 255, 255, 255);
    public static int progressColor = ARGB.color(220, 255, 255, 255);
    private static float targetUserScale = 1.0f;
    private static final Animation userScaleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 150);
    private static final Animation posXAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180);
    private static final Animation posYAnimation = new Animation(Easing.EASE_OUT_CUBIC, 180);
    private static boolean showControls = false;
    private static boolean showNextSong = true;
    public enum BackgroundType {
        GAUSSIAN("Gaussian"),
        LIQUID_GLASS("Liquid Glass");

        private final String name;
        BackgroundType(String name) { this.name = name; }
        public String getName() { return name; }
    }

    private static BackgroundType backgroundType = BackgroundType.GAUSSIAN;
    private static float blurStrength = DEFAULT_BLUR;
    private static float bloomStrength = DEFAULT_BLOOM;

    private static final Animation scaleAnimation = new Animation(Easing.EASE_OUT_CUBIC, 250);
    private static final Animation progressAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private static final Animation heightAnimation = new Animation(Easing.EASE_OUT_CUBIC, 200);
    private static boolean animatingOut;
    private static boolean dragging = false;
    private static boolean draggingVolume = false;
    private static float dragOffsetX = 0.0f;
    private static float dragOffsetY = 0.0f;

    private static ImageManager.CachedImage spotifyLogoImage;
    private static NVGImageRenderer settingsIcon;
    private static ImageManager.CachedImage albumArtImage;
    private static String albumArtPath = "";
    private static long albumArtLastModified = 0L;
    private static MsdfFont msdfTitleFont;
    private static MsdfFont msdfArtistFont;
    private static MsdfFont msdfTimeFont;
    private static MsdfFont fluidFont;

    static {
        scaleAnimation.setStartValue(0.0f);
        userScaleAnimation.setStartValue(1.0f);
        heightAnimation.setStartValue(44.0f);
    }

    private MusicDisplayOverlay() {
    }

    public static boolean isVisible() {
        return visible;
    }

    public static void setVisible(boolean value) {
        if (value && !visible) {
            // Opening: scale in
            visible = true;
            animatingOut = false;
            scaleAnimation.setStartValue(scaleAnimation.getValue());
            scaleAnimation.reset();
        } else if (!value && visible) {
            // Closing: start scale out animation
            animatingOut = true;
            scaleAnimation.setStartValue(scaleAnimation.getValue());
            scaleAnimation.reset();
        }
    }

    public static boolean isShowControls() {
        return showControls;
    }

    public static void setShowControls(boolean value) {
        showControls = value;
    }

    public static boolean isShowNextSong() {
        return showNextSong;
    }

    public static void setShowNextSong(boolean value) {
        showNextSong = value;
    }

    public static float getBlurStrength() {
        return blurStrength;
    }

    public static void setBlurStrength(float value) {
        blurStrength = clamp(value, 0.0f, 30.0f);
    }

    public static float getBloomStrength() {
        return bloomStrength;
    }

    public static void setBloomStrength(float value) {
        bloomStrength = clamp(value, 0.0f, 20.0f);
    }

    public static BackgroundType getBackgroundType() {
        return backgroundType;
    }

    public static void setBackgroundType(BackgroundType value) {
        backgroundType = value;
    }

    public static void render(GuiGraphicsExtractor graphics) {
        if (!visible) {
            return;
        }

        // Run scale animation
        scaleAnimation.run(animatingOut ? 0.0f : 1.0f);
        float scale = scaleAnimation.getValue();

        // If scale out animation finished, hide fully
        if (animatingOut && scale <= 0.001f) {
            visible = false;
            animatingOut = false;
            return;
        }
        if (scale <= 0.001f) {
            scale = 0.001f;
        }

        // Run height animation
        heightAnimation.run(showControls ? 58.0f : 44.0f);
        float currentHeight = heightAnimation.getValue();

        float x = resolveX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
        float y = resolveY(Minecraft.getInstance().getWindow().getGuiScaledHeight());

        // Center of the overlay for scale pivot
        float centerX = x + WIDTH / 2.0f;
        float centerY = y + currentHeight / 2.0f;

        var textureSetup = CaptureManager.getCaptureTextureSetup();
        userScaleAnimation.run(targetUserScale);
        float totalScale = scale * userScaleAnimation.getValue();
        // Apply scale transform around the overlay center
        var pose = new Matrix3x2f(graphics.pose());
        pose.translate(centerX, centerY)
            .scale(totalScale, totalScale)
            .translate(-centerX, -centerY);
        var scissor = graphics.scissorStack.peek();

        int r = (backgroundColor >> 16) & 0xFF;
        int g = (backgroundColor >> 8) & 0xFF;
        int b = backgroundColor & 0xFF;
        int maxAlpha = (backgroundColor >> 24) & 0xFF;
        int dynamicAlpha = (int) (maxAlpha * scale);

        if (backgroundType == BackgroundType.LIQUID_GLASS) {
            graphics.guiRenderState.addGuiElement(new BlurredLiquidGlassRoundedRectangleRenderState(
                    HaloRenderPipelines.LIQUID_GLASS,
                    textureSetup,
                    pose,
                    x, y, WIDTH, currentHeight,
                    ARGB.color(dynamicAlpha, r, g, b),
                    9.0f,
                    blurStrength,
                    bloomStrength,
                    scissor
            ));
        } else {
            graphics.guiRenderState.addGuiElement(new BlurredRoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_BLUR,
                    textureSetup,
                    pose,
                    x, y, WIDTH, currentHeight,
                    ARGB.color(dynamicAlpha, r, g, b),
                    9.0f,
                    blurStrength,
                    bloomStrength,
                    scissor
            ));
        }

        loadAssets();
        SpotifyManager.MediaStatus status = SpotifyManager.getStatus();
        boolean isConfigured = SpotifyManager.isConfigured();
        String title = isConfigured ? (status.hasMedia() ? status.title() : "No media playing") : "Spotify not connected";
        String artist = isConfigured ? (status.hasMedia() && !status.artist().isBlank() ? status.artist() : "Spotify API") : "Setup in ClickGUI";
        ImageManager.CachedImage currentAlbumArt = isConfigured ? getAlbumArt(status.artworkPath()) : null;
        float progress = isConfigured ? status.progress() : 0.0f;
        float textX = x + PADDING + COVER_SIZE + 7.0f;
        float textWidth = WIDTH - textX + x - PADDING;
        final float drawScale = scale;

        // --- MSDF title rendering (GPU pipeline — always crisp) ---
        if (msdfTitleFont != null) {
            int titleAlpha = (int) (255 * drawScale);
            String trimmedTitle = trimToWidthMsdf(msdfTitleFont, title, textWidth, TITLE_SIZE);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    msdfTitleFont,
                    trimmedTitle,
                    pose,
                    textX,
                    y + 7.0f,
                    TITLE_SIZE,
                    ARGB.color((int) (((titleColor >> 24) & 0xFF) * drawScale), (titleColor >> 16) & 0xFF, (titleColor >> 8) & 0xFF, titleColor & 0xFF),
                    scissor
            ));
        }

        // --- MSDF artist rendering (GPU pipeline — always crisp) ---
        if (msdfArtistFont != null) {
            int artA = (artistColor >> 24) & 0xFF;
            int artR = (artistColor >> 16) & 0xFF;
            int artG = (artistColor >> 8) & 0xFF;
            int artB = artistColor & 0xFF;
            int artistAlpha = (int) (artA * drawScale);
            float artistMaxWidth = textWidth;
            if (isConfigured && status.hasMedia()) {
                int posMin = (int) status.positionSeconds() / 60;
                int posSec = (int) status.positionSeconds() % 60;
                int durMin = (int) status.durationSeconds() / 60;
                int durSec = (int) status.durationSeconds() % 60;
                String timeText = String.format("%d:%02d / %d:%02d", posMin, posSec, durMin, durSec);
                float timeWidth = msdfArtistFont.getWidth(timeText, TIME_SIZE);
                artistMaxWidth = textWidth - timeWidth - 8.0f;
            }
            String trimmedArtist = trimToWidthMsdf(msdfArtistFont, artist, artistMaxWidth, ARTISTTEXT_SIZE);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    msdfArtistFont,
                    trimmedArtist,
                    pose,
                    textX,
                    y + 18.0f,
                    ARTISTTEXT_SIZE,
                    ARGB.color(artistAlpha, artR, artG, artB),
                    scissor
            ));
        }

        // --- MSDF time rendering (GPU pipeline — always crisp) ---
        if (msdfArtistFont != null && isConfigured && status.hasMedia()) {
            int posMin = (int) status.positionSeconds() / 60;
            int posSec = (int) status.positionSeconds() % 60;
            int durMin = (int) status.durationSeconds() / 60;
            int durSec = (int) status.durationSeconds() % 60;
            String timeText = String.format("%d:%02d / %d:%02d", posMin, posSec, durMin, durSec);
            
            int tA = (timeColor >> 24) & 0xFF;
            int tR = (timeColor >> 16) & 0xFF;
            int tG = (timeColor >> 8) & 0xFF;
            int tB = timeColor & 0xFF;
            int timeAlpha = (int) (tA * drawScale);
            float timeWidth = msdfArtistFont.getWidth(timeText, TIME_SIZE);
            float timeX = textX + textWidth - timeWidth;
            float timeY = y + 44.0f - PADDING - PROGRESS_HEIGHT - TIME_SIZE - 10.0f;
            
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    msdfArtistFont,
                    timeText,
                    pose,
                    timeX,
                    timeY,
                    TIME_SIZE,
                    ARGB.color(timeAlpha, tR, tG, tB),
                    scissor
            ));
        }

        // --- Progress bar (GPU pipeline with smooth animation) ---
        float progressX = textX;
        float progressY = y + 44.0f - PADDING - PROGRESS_HEIGHT - 5.0f;

        // Animate progress for smooth expansion
        progressAnimation.run(progress);
        float animatedProgress = progressAnimation.getValue();
        float filledWidth = Math.max(PROGRESS_HEIGHT, textWidth * animatedProgress);

        int bgAlpha = (int) (65 * drawScale);

        // Progress bar background
        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                progressX, progressY, textWidth, PROGRESS_HEIGHT,
                ARGB.color(bgAlpha, 255, 255, 255),
                1.5f,
                scissor
        ));

        // Progress bar fill
        int pA = (progressColor >> 24) & 0xFF;
        int pR = (progressColor >> 16) & 0xFF;
        int pG = (progressColor >> 8) & 0xFF;
        int pB = progressColor & 0xFF;
        int fillAlpha = (int) (pA * drawScale);

        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                progressX, progressY, filledWidth, PROGRESS_HEIGHT,
                ARGB.color(fillAlpha, pR, pG, pB),
                1.5f,
                scissor
        ));

        // --- Album art / Spotify logo rendering (GPU pipeline — always crisp) ---
        
        // Background placeholder
        graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                HaloRenderPipelines.ROUNDED_RECT,
                pose,
                x + PADDING,
                y + PADDING,
                COVER_SIZE,
                COVER_SIZE,
                ARGB.color((int) (55 * drawScale), 255, 255, 255),
                6.0f,
                scissor
        ));

        if (currentAlbumArt != null) {
            graphics.guiRenderState.addGuiElement(new ImageRenderState(
                    HaloRenderPipelines.IMAGE,
                    currentAlbumArt.textureSetup(),
                    pose,
                    x + PADDING,
                    y + PADDING,
                    COVER_SIZE,
                    COVER_SIZE,
                    ARGB.color((int) (255 * drawScale), 255, 255, 255),
                    6.0f,
                    ImageRenderState.ScaleMode.FILL,
                    currentAlbumArt.width(),
                    currentAlbumArt.height(),
                    scissor
            ));
        } else if (spotifyLogoImage != null) {
            float iconSize = 19.0f;
            float iconX = x + PADDING + (COVER_SIZE - iconSize) / 2.0f;
            float iconY = y + PADDING + (COVER_SIZE - iconSize) / 2.0f;
            graphics.guiRenderState.addGuiElement(new ImageRenderState(
                    HaloRenderPipelines.IMAGE,
                    spotifyLogoImage.textureSetup(),
                    pose,
                    iconX,
                    iconY,
                    iconSize,
                    iconSize,
                    ARGB.color((int) (210 * drawScale), 255, 255, 255),
                    0.0f,
                    ImageRenderState.ScaleMode.FIT,
                    spotifyLogoImage.width(),
                    spotifyLogoImage.height(),
                    scissor
            ));
        }

        // --- Render Spotify Controls (GPU MSDF Pipeline) ---
        float controlsAlphaProgress = (heightAnimation.getValue() - 44.0f) / (58.0f - 44.0f);
        if (controlsAlphaProgress > 0.01f && fluidFont != null) {
            int controlAlpha = (int) (255 * drawScale * controlsAlphaProgress);
            
            double rawMouseX = Minecraft.getInstance().mouseHandler.xpos();
            double rawMouseY = Minecraft.getInstance().mouseHandler.ypos();
            double winScale = Minecraft.getInstance().getWindow().getGuiScale();
            double currentGuiMX = rawMouseX / winScale;
            double currentGuiMY = rawMouseY / winScale;
            
            totalScale = scale * userScaleAnimation.getValue();
            float pivotX = x + WIDTH / 2.0f;
            float pivotY = y + heightAnimation.getValue() / 2.0f;
            float localMX = (float) ((currentGuiMX - pivotX) / totalScale + WIDTH / 2.0f);
            float localMY = (float) ((currentGuiMY - pivotY) / totalScale + heightAnimation.getValue() / 2.0f);

            boolean chatOpen = Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.ChatScreen;
            
            // 1. Speaker Icon (E)
            boolean speakerHovered = chatOpen && localMX >= 6.0f && localMX <= 17.0f && localMY >= 40.0f && localMY <= 58.0f;
            int speakerColor = speakerHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    "E",
                    pose,
                    x + 8.0f,
                    y + 40.0f,
                    20.0f,
                    speakerColor,
                    scissor
            ));

            // 2. Volume Slider (starts at x+18, width=30)
            float sliderX = x + 18.0f;
            float sliderY = y + 46.5f;
            float sliderW = 30.0f;
            float sliderH = 2.0f;
            float volPct = isConfigured ? (status.volumePercent() / 100.0f) : 0.5f;

            // Slider bg
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    sliderX, sliderY, sliderW, sliderH,
                    ARGB.color((int) (65 * drawScale * controlsAlphaProgress), 255, 255, 255),
                    1.0f,
                    scissor
            ));

            // Slider fill
            float fillW = sliderW * volPct;
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    sliderX, sliderY, fillW, sliderH,
                    ARGB.color(controlAlpha, 255, 255, 255),
                    1.0f,
                    scissor
            ));

            // Slider knob
            boolean sliderHovered = chatOpen && localMX >= 18.0f && localMX <= 48.0f && localMY >= 40.0f && localMY <= 58.0f;
            float knobRadius = (sliderHovered || draggingVolume) ? 2.5f : 1.5f;
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    pose,
                    sliderX + fillW - knobRadius, sliderY + sliderH / 2.0f - knobRadius, knobRadius * 2.0f, knobRadius * 2.0f,
                    ARGB.color(controlAlpha, 255, 255, 255),
                    knobRadius,
                    scissor
            ));

            // 3. Middle playback: Prev (H), Play/Pause (B/A), Next (G)
            float midX = x + WIDTH / 2.0f + 7;
            
            boolean prevHovered = chatOpen && localMX >= 73.0f && localMX <= 87.0f && localMY >= 40.0f && localMY <= 58.0f;
            int prevColor = prevHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    "H",
                    pose,
                    midX - 16.0f - 1.5f,
                    y + 41.5f,
                    16.0f,
                    prevColor,
                    scissor
            ));

            boolean playHovered = chatOpen && localMX >= 88.0f && localMX <= 101.0f && localMY >= 40.0f && localMY <= 58.0f;
            int playColor = playHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255);
            String playChar = (isConfigured && status.isPlaying()) ? "A" : "B";
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    playChar,
                    pose,
                    midX - 4.5f,
                    y + 41.5f,
                    16.0f,
                    playColor,
                    scissor
            ));

            boolean nextHovered = chatOpen && localMX >= 102.0f && localMX <= 116.0f && localMY >= 40.0f && localMY <= 58.0f;
            int nextColor = nextHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255);
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    "G",
                    pose,
                    midX + 16.0f - 8.5f,
                    y + 41.5f,
                    16.0f,
                    nextColor,
                    scissor
            ));

            // 4. Right: Shuffle (C) and Like (D)
            boolean shuffleHovered = chatOpen && localMX >= 147.0f && localMX <= 159.0f && localMY >= 40.0f && localMY <= 58.0f;
            boolean shuffleActive = isConfigured && status.shuffleState();
            int shuffleColor = shuffleActive ? ARGB.color(controlAlpha, 29, 185, 84) : (shuffleHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255));
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    "C",
                    pose,
                    x + WIDTH - PADDING - 18.0f ,
                    y + 41.5f,
                    16.0f,
                    shuffleColor,
                    scissor
            ));

            boolean likeHovered = chatOpen && localMX >= 160.0f && localMX <= 172.0f && localMY >= 40.0f && localMY <= 58.0f;
            boolean likeActive = isConfigured && status.liked();
            int likeColor = likeActive ? ARGB.color(controlAlpha, 29, 185, 84) : (likeHovered ? ARGB.color(controlAlpha, 255, 255, 255) : ARGB.color((int)(controlAlpha * 0.7f), 255, 255, 255));
            graphics.guiRenderState.addGuiElement(new HaloFontRenderState(
                    fluidFont,
                    "D",
                    pose,
                    x + WIDTH - PADDING - 7f,
                    y + 41.5f,
                    16.0f,
                    likeColor,
                    scissor
            ));
        }

        if (dragging) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
            float screenCenterX = screenWidth / 2.0f;
            float screenCenterY = screenHeight / 2.0f;

            var screenPose = new Matrix3x2f(graphics.pose());

            float overlayCenterX = x + WIDTH / 2.0f;
            float overlayCenterY = y + currentHeight / 2.0f;

            int vertLineCol = Math.abs(overlayCenterX - screenCenterX) < 0.01f ? ARGB.color(180, 29, 185, 84) : ARGB.color(100, 255, 255, 255);
            int horizLineCol = Math.abs(overlayCenterY - screenCenterY) < 0.01f ? ARGB.color(180, 29, 185, 84) : ARGB.color(100, 255, 255, 255);

            // Vertical center line
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    screenPose,
                    screenCenterX - 0.5f, 0.0f, 1.0f, screenHeight,
                    vertLineCol,
                    0.0f,
                    scissor
            ));

            // Horizontal center line
            graphics.guiRenderState.addGuiElement(new RoundedRectangleRenderState(
                    HaloRenderPipelines.ROUNDED_RECT,
                    screenPose,
                    0.0f, screenCenterY - 0.5f, screenWidth, 1.0f,
                    horizLineCol,
                    0.0f,
                    scissor
            ));
        }
    }



    /**
     * Trims text to fit within the given width using MSDF font metrics.
     */
    private static String trimToWidthMsdf(MsdfFont font, String text, float width, float size) {
        if (font.getWidth(text, size) <= width) {
            return text;
        }
        String suffix = "...";
        float suffixWidth = font.getWidth(suffix, size);
        float maxWidth = Math.max(0.0f, width - suffixWidth);
        StringBuilder sb = new StringBuilder();
        float currentWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float charWidth = font.getWidth(ch, size);
            if (currentWidth + charWidth > maxWidth) break;
            currentWidth += charWidth;
            sb.append(text.charAt(i));
        }
        return sb + suffix;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ImageManager.CachedImage getAlbumArt(String path) {
        if (path == null || path.isBlank()) {
            albumArtImage = null;
            albumArtPath = "";
            albumArtLastModified = 0L;
            return null;
        }
        java.io.File file = new java.io.File(path);
        long lastMod = file.exists() ? file.lastModified() : 0L;
        if (path.equals(albumArtPath) && lastMod == albumArtLastModified && albumArtImage != null) {
            return albumArtImage;
        }
        try {
            if (file.exists()) {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                ImageManager.evict("native:" + path);
                albumArtImage = ImageManager.fromBytes(path, bytes);
                albumArtPath = path;
                albumArtLastModified = lastMod;
                return albumArtImage;
            }
        } catch (Exception ignored) {
        }
        albumArtImage = null;
        albumArtPath = "";
        albumArtLastModified = 0L;
        return null;
    }

    private static float resolveX(int screenWidth) {
        float target = Float.isNaN(relativeX) ? (screenWidth - WIDTH) / 2.0f : relativeX * (screenWidth - WIDTH);
        if (dragging) {
            posXAnimation.setValue(target);
            posXAnimation.setStartValue(target);
        }
        if (posXAnimation.getValue() == 0.0f && posXAnimation.getStartValue() == 0.0f) {
            posXAnimation.setStartValue(target);
        }
        posXAnimation.run(target);
        return posXAnimation.getValue();
    }

    private static float resolveY(int screenHeight) {
        float currentH = heightAnimation.getValue();
        float target = Float.isNaN(relativeY) ? 20.0f : relativeY * (screenHeight - currentH);
        if (dragging) {
            posYAnimation.setValue(target);
            posYAnimation.setStartValue(target);
        }
        if (posYAnimation.getValue() == 0.0f && posYAnimation.getStartValue() == 0.0f) {
            posYAnimation.setStartValue(target);
        }
        posYAnimation.run(target);
        return posYAnimation.getValue();
    }

    private static void loadAssets() {
        if (spotifyLogoImage == null) {
            try {
                Identifier id = Identifier.fromNamespaceAndPath("halo", "spotify-white-icon.png");
                spotifyLogoImage = ImageManager.fromIdentifier(id);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (settingsIcon == null) {
            try {
                Identifier id = Identifier.fromNamespaceAndPath("halo", "icons8-settings-20.png");
                settingsIcon = new NVGImageRenderer(
                        Minecraft.getInstance().getResourceManager().open(id)
                );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



        // Load MSDF fonts for crisp text rendering
        if (msdfTitleFont == null) {
            try {
                msdfTitleFont = MsdfFontManager.getFont("inter-bold", TITLE_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (msdfTimeFont == null) {
            try {
                msdfTimeFont = MsdfFontManager.getFont("inter-regular", TIME_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (msdfArtistFont == null) {
            try {
                msdfArtistFont = MsdfFontManager.getFont("inter-semibold", TEXT_SIZE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (fluidFont == null) {
            try {
                fluidFont = MsdfFontManager.getFont("fluid-regular", 9.0f);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean onMouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (button == 0) {
            float x = resolveX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
            float y = resolveY(Minecraft.getInstance().getWindow().getGuiScaledHeight());

            float scale = scaleAnimation.getValue();
            float currentScale = userScaleAnimation.getValue();
            float totalScale = scale * currentScale;
            float currentW = WIDTH * totalScale;
            float currentH = heightAnimation.getValue() * totalScale;
            float currentX = x + (WIDTH - currentW) / 2.0f;
            float currentY = y + (heightAnimation.getValue() - currentH) / 2.0f;

            if (mouseX >= currentX && mouseX <= currentX + currentW && mouseY >= currentY && mouseY <= currentY + currentH) {
                float pivotX = x + WIDTH / 2.0f;
                float pivotY = y + heightAnimation.getValue() / 2.0f;
                float localMX = (float) ((mouseX - pivotX) / totalScale + WIDTH / 2.0f);
                float localMY = (float) ((mouseY - pivotY) / totalScale + heightAnimation.getValue() / 2.0f);

                // If controls are shown, check if clicking controls row (localMY >= 40.0)
                if (showControls && heightAnimation.getValue() > 44.0f && localMY >= 40.0f) {
                    SpotifyManager.MediaStatus status = SpotifyManager.getStatus();
                    boolean isConfigured = SpotifyManager.isConfigured();

                    // Left controls: Speaker icon (E) / Volume Slider
                    if (isConfigured && localMX >= 18.0f && localMX <= 48.0f && localMY >= 40.0f && localMY <= 58.0f) {
                        draggingVolume = true;
                        float pct = clamp((localMX - 18.0f) / 30.0f, 0.0f, 1.0f);
                        SpotifyManager.getInstance().setVolume((int) (pct * 100));
                        return true;
                    }
                    if (isConfigured && localMX >= 6.0f && localMX <= 17.0f && localMY >= 40.0f && localMY <= 58.0f) {
                        SpotifyManager.getInstance().setVolume(status.volumePercent() > 0 ? 0 : 50);
                        return true;
                    }

                    // Middle playback controls
                    if (isConfigured) {
                        if (localMX >= 88.0f && localMX <= 101.0f && localMY >= 40.0f && localMY <= 58.0f) {
                            SpotifyManager.getInstance().togglePlayPause();
                            return true;
                        }
                        if (localMX >= 73.0f && localMX <= 87.0f && localMY >= 40.0f && localMY <= 58.0f) {
                            SpotifyManager.getInstance().previous();
                            return true;
                        }
                        if (localMX >= 102.0f && localMX <= 116.0f && localMY >= 40.0f && localMY <= 58.0f) {
                            SpotifyManager.getInstance().next();
                            return true;
                        }
                    }

                    // Right controls: Shuffle / Like
                    if (isConfigured) {
                        if (localMX >= 160.0f && localMX <= 172.0f && localMY >= 40.0f && localMY <= 58.0f) {
                            SpotifyManager.getInstance().toggleLike();
                            return true;
                        }
                        if (localMX >= 147.0f && localMX <= 159.0f && localMY >= 40.0f && localMY <= 58.0f) {
                            SpotifyManager.getInstance().toggleShuffle(!status.shuffleState());
                            return true;
                        }
                    }
                    
                    return true;
                }

                dragging = true;
                dragOffsetX = (float) (mouseX - x);
                dragOffsetY = (float) (mouseY - y);
                return true;
            }
        }
        return false;
    }

    public static boolean onMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingVolume && button == 0) {
            float x = resolveX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
            float totalScale = scaleAnimation.getValue() * userScaleAnimation.getValue();
            float pivotX = x + WIDTH / 2.0f;
            float localMX = (float) ((mouseX - pivotX) / totalScale + WIDTH / 2.0f);
            float pct = clamp((localMX - 18.0f) / 30.0f, 0.0f, 1.0f);
            SpotifyManager.getInstance().setVolume((int) (pct * 100));
            return true;
        }
        if (dragging && button == 0) {
            int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
            int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

            float targetX = (float) (mouseX - dragOffsetX);
            float targetY = (float) (mouseY - dragOffsetY);

            float currentScale = userScaleAnimation.getValue();
            float currentW = WIDTH * currentScale;
            float currentH = heightAnimation.getValue() * currentScale;

            float minX = (currentW - WIDTH) / 2.0f;
            float maxX = screenWidth - (WIDTH + currentW) / 2.0f;
            float minY = (currentH - heightAnimation.getValue()) / 2.0f;
            float maxY = screenHeight - (heightAnimation.getValue() + currentH) / 2.0f;

            if (minX > maxX) {
                targetX = (screenWidth - WIDTH) / 2.0f;
            } else {
                targetX = clamp(targetX, minX, maxX);
            }

            if (minY > maxY) {
                targetY = (screenHeight - heightAnimation.getValue()) / 2.0f;
            } else {
                targetY = clamp(targetY, minY, maxY);
            }

            float centerX = screenWidth / 2.0f;
            float centerY = screenHeight / 2.0f;
            float overlayCenterX = targetX + WIDTH / 2.0f;
            float overlayCenterY = targetY + heightAnimation.getValue() / 2.0f;

            float snapThreshold = 6.0f;

            if (Math.abs(overlayCenterX - centerX) < snapThreshold) {
                targetX = centerX - WIDTH / 2.0f;
            }
            if (Math.abs(overlayCenterY - centerY) < snapThreshold) {
                targetY = centerY - heightAnimation.getValue() / 2.0f;
            }

            relativeX = screenWidth > WIDTH ? targetX / (screenWidth - WIDTH) : 0.5f;
            relativeY = screenHeight > heightAnimation.getValue() ? targetY / (screenHeight - heightAnimation.getValue()) : 0.05f;
            return true;
        }
        return false;
    }

    public static boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (draggingVolume && button == 0) {
            draggingVolume = false;
            return true;
        }
        if (dragging && button == 0) {
            dragging = false;
            return true;
        }
        return false;
    }

    public static boolean onMouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!visible) return false;
        float x = resolveX(Minecraft.getInstance().getWindow().getGuiScaledWidth());
        float y = resolveY(Minecraft.getInstance().getWindow().getGuiScaledHeight());

        float currentScale = userScaleAnimation.getValue();
        float currentW = WIDTH * currentScale;
        float currentH = heightAnimation.getValue() * currentScale;
        float currentX = x + (WIDTH - currentW) / 2.0f;
        float currentY = y + (heightAnimation.getValue() - currentH) / 2.0f;

        if (mouseX >= currentX && mouseX <= currentX + currentW && mouseY >= currentY && mouseY <= currentY + currentH) {
            targetUserScale = clamp(targetUserScale + (float) scrollY * 0.05f, 0.4f, 2.5f);
            return true;
        }
        return false;
    }
}

