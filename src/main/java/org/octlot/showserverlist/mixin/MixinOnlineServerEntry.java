package org.octlot.showserverlist.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.octlot.showserverlist.folder.DragController;
import org.octlot.showserverlist.folder.FolderLayout;
import org.octlot.showserverlist.folder.FolderStore;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Vanilla treats the position of a server entry in the list as its position in the server list, which no longer
 * holds once folder rows exist. The move up / move down arrows therefore work on the servers of the same folder.
 */
@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class MixinOnlineServerEntry {
    @Unique
    private static final int GUIDE_COLOR = 0x66FFC850;

    @Shadow
    @Final
    private JoinMultiplayerScreen screen;

    @Unique
    private ServerSelectionList.OnlineServerEntry showserverlist$self() {
        return (ServerSelectionList.OnlineServerEntry) (Object) this;
    }

    @Unique
    private List<ServerSelectionList.OnlineServerEntry> showserverlist$group() {
        return FolderLayout.groupOf(this.screen.serverSelectionList, showserverlist$self());
    }

    /**
     * A press on the text part of a row may turn into a drag. The icon on the left is left alone: vanilla uses it
     * for the join and move up / down buttons.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"))
    private void showserverlist$armDrag(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        ServerSelectionList.OnlineServerEntry entry = showserverlist$self();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= entry.getContentX() + 32) {
            DragController.pressServer(this.screen, entry.getServerData(), event);
        }
    }

    @WrapOperation(
            method = {"extractContent", "keyPressed", "mouseClicked"},
            at = @At(value = "INVOKE", target = "Ljava/util/List;indexOf(Ljava/lang/Object;)I")
    )
    private int showserverlist$indexInGroup(List<?> list, Object entry, Operation<Integer> original) {
        int index = original.call(list, entry);
        if (index == -1 || !FolderStore.get().hasFolders()) {
            return index;
        }
        return showserverlist$group().indexOf(showserverlist$self());
    }

    @WrapOperation(
            method = {"extractContent", "keyPressed", "mouseClicked"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ServerList;size()I")
    )
    private int showserverlist$groupSize(ServerList servers, Operation<Integer> original) {
        if (!FolderStore.get().hasFolders()) {
            return original.call(servers);
        }
        return showserverlist$group().size();
    }

    @Inject(method = "swap", at = @At("HEAD"), cancellable = true)
    private void showserverlist$swapInGroup(int currentIndex, int newIndex, CallbackInfo ci) {
        if (!FolderStore.get().hasFolders()) {
            return;
        }
        ci.cancel();
        List<ServerSelectionList.OnlineServerEntry> group = showserverlist$group();
        if (currentIndex < 0 || newIndex < 0 || currentIndex >= group.size() || newIndex >= group.size()) {
            return;
        }
        ServerSelectionList list = this.screen.serverSelectionList;
        ServerSelectionList.OnlineServerEntry first = group.get(currentIndex);
        ServerSelectionList.OnlineServerEntry second = group.get(newIndex);
        ServerList servers = this.screen.getServers();
        int firstServerIndex = showserverlist$indexOf(servers, first.getServerData());
        int secondServerIndex = showserverlist$indexOf(servers, second.getServerData());
        if (firstServerIndex < 0 || secondServerIndex < 0) {
            return;
        }
        servers.swap(firstServerIndex, secondServerIndex);
        list.swap(list.children().indexOf(first), list.children().indexOf(second));
    }

    @Unique
    private static int showserverlist$indexOf(ServerList servers, ServerData data) {
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i) == data) {
                return i;
            }
        }
        return -1;
    }

    @Inject(method = "extractContent", at = @At("HEAD"))
    private void showserverlist$extractFolderGuide(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a, CallbackInfo ci) {
        if (FolderLayout.isInFolder(showserverlist$self())) {
            ServerSelectionList.Entry entry = showserverlist$self();
            int x = entry.getX() + 9;
            graphics.fill(x, entry.getY(), x + 2, entry.getY() + entry.getHeight(), GUIDE_COLOR);
        }
    }
}
