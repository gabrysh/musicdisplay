package com.haloclient.client;

import com.haloclient.client.render.renderstates.BlurredQuadRenderState;
import com.haloclient.client.render.renderstates.BlurredRoundedRectangleRenderState;
import com.haloclient.client.render.CaptureManager;
import com.haloclient.client.render.HaloRenderPipelines;
import com.haloclient.client.render.animation.Animation;
import com.haloclient.client.render.animation.Easing;
import com.haloclient.client.render.font.FontManager;
import com.haloclient.client.render.font.HaloFontRenderState;
import com.haloclient.client.util.FrameClock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.haloclient.client.module.ModuleManager;
import org.lwjgl.glfw.GLFW;

public class HaloClient implements ClientModInitializer {

    private static final Animation rectAnimation = new Animation(Easing.EASE_IN_OUT_CUBIC, 300L);
    private static float targetValue = 1.0f;
    
    public static HaloClient INSTANCE;
    public ModuleManager moduleManager;

    public static final net.minecraft.client.KeyMapping.Category HALO_CATEGORY = net.minecraft.client.KeyMapping.Category.register(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("halo", "halo"));

    public static final net.minecraft.client.KeyMapping openClickGuiKey = new net.minecraft.client.KeyMapping(
            "key.halo.clickgui",
            org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT,
            HALO_CATEGORY
    );

    public static final net.minecraft.client.KeyMapping openSubsonicSetupKey = new net.minecraft.client.KeyMapping(
            "key.halo.subsonicsetup",
            org.lwjgl.glfw.GLFW.GLFW_KEY_HOME,
            HALO_CATEGORY
    );

    private static void openBrowser(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                new ProcessBuilder("cmd.exe", "/c", "start", url).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", url).start();
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        HaloRenderPipelines.init();
        moduleManager = new ModuleManager();
        com.haloclient.client.gui.click.MusicManager.load();
        com.haloclient.client.gui.click.MusicManager.startPolling();

        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openClickGuiKey.consumeClick()) {
                if (client.screen == null) {
                    client.setScreen(new com.haloclient.client.gui.click.ClickGUI());
                }
            }
            while (openSubsonicSetupKey.consumeClick()) {
                com.haloclient.client.gui.click.SubsonicManager.getInstance().startSetupServer();
                openBrowser("http://127.0.0.1:8889/setup");
            }
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            try {
                com.haloclient.client.gui.click.MusicManager.cleanup();
            } catch (Throwable ignored) {}
        });
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static void renderModules(GuiGraphics graphics, DeltaTracker deltaTracker) {
        FrameClock.update();

        if (INSTANCE != null && INSTANCE.moduleManager != null) {
            INSTANCE.moduleManager.getModules().stream()
                    .filter(com.haloclient.client.module.Module::isEnabled)
                    .forEach(m -> m.onRender(graphics, deltaTracker));
        }
    }
}
