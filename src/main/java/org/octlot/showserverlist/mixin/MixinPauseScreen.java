package org.octlot.showserverlist.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class MixinPauseScreen {
    @Inject(
            method = "createPauseMenu",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", shift = At.Shift.AFTER, ordinal = 3)
    )
    public void onInit(CallbackInfo ci, @Local(name = "iconButtonRow") LinearLayout iconButtonRow) {
        Screen currentScreen = (Screen) (Object) this;
        SpriteIconButton button = SpriteIconButton.builder(
                Component.literal("Server List"),
                (_) -> Minecraft.getInstance().gui.setScreen(new JoinMultiplayerScreen(currentScreen)),
                true
        ).width(20).sprite(Identifier.fromNamespaceAndPath("showserverlist", "servers"), 16, 16).withTootip().build();
        iconButtonRow.addChild(button);
    }
}
