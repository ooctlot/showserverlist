package org.octlot.showserverlist.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {
    @Shadow
    private Button selectButton;
    @Unique
    private Button directConnectionButton;

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 3
            )
    )
    public LayoutElement showserverlist$redirectCreateDirectConnectButton(LinearLayout instance, LayoutElement child) {
        if (isInWorld()) {
            this.directConnectionButton = (Button) child;
        }
        return instance.addChild(child);
    }
    @Unique
    private boolean isInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }
    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    public void showserverlist$cancelJoin(ServerData data, CallbackInfo ci){
        if (isInWorld()) {
            ci.cancel();
        }
    }
    @Inject(method = "init", at = @At("TAIL"))
    public void showserverlist$onInit(CallbackInfo ci){
        if (isInWorld()) {
            this.selectButton.active = false;
            this.directConnectionButton.active = false;
        }
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    public void showserverlist$onSelectedChange(CallbackInfo ci){
        if (isInWorld()) {
            this.selectButton.active = false;
            this.directConnectionButton.active = false;
        }
    }
}
