package org.octlot.showserverlist;

import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.jspecify.annotations.Nullable;

/**
 * Remembers the scroll position and selected server of the multiplayer list for the current game session,
 * so the list does not jump back to the top whenever its screen is re-created.
 */
public final class ServerListMemory {
    private static double scrollAmount;
    private static @Nullable String selectedIp;

    private ServerListMemory() {
    }

    public static void save(ServerSelectionList list) {
        scrollAmount = list.scrollAmount();
        selectedIp = list.getSelected() instanceof ServerSelectionList.OnlineServerEntry entry
                ? entry.getServerData().ip
                : null;
    }

    public static void restore(ServerSelectionList list) {
        if (selectedIp != null) {
            for (ServerSelectionList.Entry entry : list.children()) {
                if (entry instanceof ServerSelectionList.OnlineServerEntry online && selectedIp.equals(online.getServerData().ip)) {
                    list.setSelected(entry);
                    break;
                }
            }
        }
        list.setScrollAmount(scrollAmount);
    }
}
