package org.octlot.showserverlist.folder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The folder related dialogs of the multiplayer screen. Every flow ends by returning to the (re-created)
 * multiplayer screen, which rebuilds the list from the {@link FolderStore}.
 */
public final class FolderActions {
    private FolderActions() {
    }

    public static void createFolder(JoinMultiplayerScreen screen) {
        FolderStore store = FolderStore.get();
        open(new FolderNameScreen(
                Component.translatable("showserverlist.folder.name.new_title"),
                "",
                name -> store.isNameAvailable(name, null),
                name -> {
                    store.create(name);
                    returnTo(screen);
                },
                () -> returnTo(screen)
        ));
    }

    public static void renameFolder(JoinMultiplayerScreen screen, ServerFolder folder) {
        FolderStore store = FolderStore.get();
        open(new FolderNameScreen(
                Component.translatable("showserverlist.folder.name.rename_title"),
                folder.name(),
                name -> store.isNameAvailable(name, folder),
                name -> {
                    store.rename(folder, name);
                    returnTo(screen);
                },
                () -> returnTo(screen)
        ));
    }

    public static void deleteFolder(JoinMultiplayerScreen screen, ServerFolder folder) {
        open(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        FolderStore.get().delete(folder);
                    }
                    returnTo(screen);
                },
                Component.translatable("showserverlist.folder.delete.title"),
                Component.translatable("showserverlist.folder.delete.message", folder.name()),
                Component.translatable("showserverlist.folder.delete"),
                CommonComponents.GUI_CANCEL
        ));
    }

    public static void moveServer(JoinMultiplayerScreen screen, ServerData server) {
        FolderStore store = FolderStore.get();
        FolderPickerScreen[] picker = new FolderPickerScreen[1];
        picker[0] = new FolderPickerScreen(
                server.name,
                store.folderOf(server.ip),
                target -> {
                    store.assign(server.ip, target);
                    returnTo(screen);
                },
                () -> open(new FolderNameScreen(
                        Component.translatable("showserverlist.folder.name.new_title"),
                        "",
                        name -> store.isNameAvailable(name, null),
                        name -> {
                            store.assign(server.ip, store.create(name));
                            returnTo(screen);
                        },
                        () -> open(picker[0])
                )),
                () -> returnTo(screen)
        );
        open(picker[0]);
    }

    /**
     * The multiplayer screen is not re-created when a dialog closes, so its list has to be rebuilt by hand
     * (vanilla does the same before returning from the edit / add / delete dialogs).
     */
    private static void returnTo(JoinMultiplayerScreen screen) {
        screen.serverSelectionList.refreshEntries();
        open(screen);
    }

    private static void open(Screen screen) {
        Minecraft.getInstance().gui.setScreen(screen);
    }
}
