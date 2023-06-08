package io.github.ennuil.libzoomer.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.ennuil.libzoomer.api.ZoomInstance;
import io.github.ennuil.libzoomer.api.ZoomOverlay;
import io.github.ennuil.libzoomer.api.ZoomRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.util.Identifier;

@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Unique
    private boolean shouldCancelOverlay = false;

    @Inject(
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/MinecraftClient.getLastFrameDuration()F"
        ),
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V"
    )
    public void injectZoomOverlay(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        this.shouldCancelOverlay = false;
        for (ZoomInstance instance : ZoomRegistry.getZoomInstances()) {
            ZoomOverlay overlay = instance.getZoomOverlay();
            if (overlay != null) {
                overlay.tickBeforeRender();
                if (overlay.getActive()) {
                    this.shouldCancelOverlay = overlay.cancelOverlayRendering() || true;
                    overlay.renderOverlay();
                }
            }
        }
    }

    // Yes, there is a renderOverlay for being frozen...
    @Inject(at = @At("HEAD"), method = "renderSpyglassOverlay", cancellable = true)
    public void cancelSpyglassOverlay(GuiGraphics graphics, float scale, CallbackInfo ci) {
        if (this.shouldCancelOverlay) ci.cancel();
    }

    @Inject(at = @At("HEAD"), method = "renderOverlay", cancellable = true)
    public void cancelOverlay(GuiGraphics graphics, Identifier texture, float opacity, CallbackInfo ci) {
        if (this.shouldCancelOverlay) ci.cancel();
    }

    // ...which is why we set cancelOverlayRender to false before that!
    @Inject(
        at = @At(
            value = "INVOKE",
            target = "net/minecraft/client/network/ClientPlayerEntity.getFrozenTicks()I"
        ),
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;F)V"
    )
    public void disableOverlayCancelling(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        if (this.shouldCancelOverlay) {
            this.shouldCancelOverlay = false;
        }
    }
}
