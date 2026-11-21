package org.octlot.showserverlist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import org.octlot.showserverlist.folder.DragController;

public class ShowServerListClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof JoinMultiplayerScreen multiplayerScreen) {
                DragController.attach(multiplayerScreen);
            }
        });
    }
}
