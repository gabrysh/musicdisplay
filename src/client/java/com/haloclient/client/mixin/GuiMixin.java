package com.haloclient.client.mixin;

import com.haloclient.client.HaloClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class GuiMixin {

 

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void halo$onAfterExtractRenderState(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HaloClient.renderModules(graphics, deltaTracker);
    }

}
