package org.octlot.showserverlist.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.octlot.showserverlist.ServerListMemory;
import org.octlot.showserverlist.folder.FolderActions;
import org.octlot.showserverlist.folder.FolderEntry;
import org.octlot.showserverlist.folder.FolderStore;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public class MixinJoinMultiplayerScreen {
    @Shadow
    private Button joinButton;
    @Shadow
    protected ServerSelectionList serverSelectionList;
    @Shadow
    private ServerData editingServer;
    @Unique
    private SpriteIconButton newFolderButton;
    @Unique
    private SpriteIconButton moveToFolderButton;

    @Unique
    private boolean isInWorld() {
        return Minecraft.getInstance().player != null && Minecraft.getInstance().level != null;
    }

    /**
     * Joining from a screen opened inside a world leaves that world, like the disconnect button of the pause menu
     * does (including the question about a draft report). If the connection fails the player lands in a server
     * list in front of the title screen: the pause menu this list was opened from no longer has a world.
     */
    @Inject(method = "join", at = @At("HEAD"), cancellable = true)
    public void showserverlist$joinFromWorld(ServerData data, CallbackInfo ci){
        if (!isInWorld()) {
            return;
        }
        ci.cancel();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.getReportingContext().draftReportHandled(
                minecraft,
                (JoinMultiplayerScreen) (Object) this,
                () -> ConnectScreen.startConnecting(
                        new JoinMultiplayerScreen(new TitleScreen()), minecraft, ServerAddress.parseString(data.ip), data, false, null
                ),
                true
        );
    }

    @Inject(method = "init", at = @At("TAIL"))
    public void showserverlist$onInit(CallbackInfo ci){
        ServerListMemory.restore(this.serverSelectionList);
        showserverlist$addFolderButtons();
    }

    @Unique
    private void showserverlist$addFolderButtons() {
        JoinMultiplayerScreen screen = (JoinMultiplayerScreen) (Object) this;
        this.newFolderButton = screen.addRenderableWidget(
                SpriteIconButton.builder(
                        Component.translatable("showserverlist.folder.new"),
                        button -> FolderActions.createFolder(screen),
                        true
                ).width(20).sprite(Identifier.fromNamespaceAndPath("showserverlist", "folder_add"), 16, 16).withTootip().build()
        );
        this.moveToFolderButton = screen.addRenderableWidget(
                SpriteIconButton.builder(
                        Component.translatable("showserverlist.folder.move"),
                        button -> {
                            if (this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry) {
                                FolderActions.moveServer(screen, entry.getServerData());
                            }
                        },
                        true
                ).width(20).sprite(Identifier.fromNamespaceAndPath("showserverlist", "folder_move"), 16, 16).withTootip().build()
        );
        showserverlist$positionFolderButtons();
        showserverlist$updateFolderButtons();
    }

    /** The screen is kept (and only repositioned) when the window is resized or a dialog returns to it. */
    @Inject(method = "repositionElements", at = @At("TAIL"))
    public void showserverlist$onReposition(CallbackInfo ci){
        showserverlist$positionFolderButtons();
    }

    @Unique
    private void showserverlist$positionFolderButtons() {
        if (this.newFolderButton == null || this.moveToFolderButton == null) {
            return;
        }
        JoinMultiplayerScreen screen = (JoinMultiplayerScreen) (Object) this;
        int y = 6;
        int x = screen.width - 28;
        this.newFolderButton.setPosition(x, y);
        this.moveToFolderButton.setPosition(x - 24, y);
    }

    @Unique
    private void showserverlist$updateFolderButtons() {
        if (this.moveToFolderButton != null) {
            this.moveToFolderButton.active = this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry;
        }
    }

    @Inject(method = "editServerCallback", at = @At("HEAD"))
    public void showserverlist$keepFolderOnEdit(boolean result, CallbackInfo ci){
        if (result && this.serverSelectionList.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry) {
            FolderStore.get().migrateServer(entry.getServerData().ip, this.editingServer.ip);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    public void showserverlist$rememberListState(CallbackInfo ci){
        ServerListMemory.save(this.serverSelectionList);
    }

    @Inject(method = "onSelectedChange", at = @At("TAIL"))
    public void showserverlist$onSelectedChange(CallbackInfo ci){
        showserverlist$updateFolderButtons();
        if (this.serverSelectionList.getSelected() instanceof FolderEntry) {
            this.joinButton.active = false;
        }
    }
}
