package org.octlot.showserverlist.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 2
            )
    )
    public LayoutElement cancelAddChildJoin(LinearLayout instance, LayoutElement layoutElement) {
        if (isInWorld()) {
            return layoutElement;
        }
        return instance.addChild(layoutElement);
    }
    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 3
            )
    )
    public LayoutElement cancelAddChildDirect(LinearLayout instance, LayoutElement layoutElement) {
        if (isInWorld()) {
            return layoutElement;
        }
        return instance.addChild(layoutElement);
    }

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;",
                    ordinal = 4
            )
    )
    public LayoutElement changeAddChildAddServer(LinearLayout instance, LayoutElement layoutElement, @Local(ordinal = 2) LinearLayout linearLayout3) {
        if (isInWorld()) {
            return linearLayout3.addChild(layoutElement);
        }
        return instance.addChild(layoutElement);
    }

    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    public void cancelJoin(ServerData serverData, CallbackInfo ci){
        if (isInWorld()) {
            ci.cancel();
        }
    }


    @Unique
    private boolean isInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }
}
