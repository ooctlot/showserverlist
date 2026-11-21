package org.octlot.showserverlist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.octlot.showserverlist.folder.FolderLayout;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Collection;

@Mixin(ServerSelectionList.class)
public abstract class MixinServerSelectionList {
    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @WrapOperation(
            method = "refreshEntries",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/multiplayer/ServerSelectionList;replaceEntries(Ljava/util/Collection;)V"
            )
    )
    private void showserverlist$arrangeFolders(ServerSelectionList instance, Collection<ServerSelectionList.Entry> entries, Operation<Void> original) {
        ServerSelectionList.Entry previouslySelected = instance.getSelected();
        original.call(instance, FolderLayout.arrange(this.screen, instance, entries));
        FolderLayout.applyHeights(instance);
        FolderLayout.reselectFolder(instance, previouslySelected);
    }
}
