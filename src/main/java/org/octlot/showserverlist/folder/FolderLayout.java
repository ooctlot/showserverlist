package org.octlot.showserverlist.folder;

import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Decides how the entries of the multiplayer list are grouped: folders first (each followed by its servers
 * unless collapsed), then the servers without folder, then everything else (LAN servers).
 */
public final class FolderLayout {
    /** Horizontal offset of servers that live inside a folder. */
    public static final int INDENT = 14;

    private FolderLayout() {
    }

    public static List<ServerSelectionList.Entry> arrange(JoinMultiplayerScreen screen, ServerSelectionList list, Collection<ServerSelectionList.Entry> vanilla) {
        FolderStore store = FolderStore.get();
        List<ServerSelectionList.OnlineServerEntry> servers = new ArrayList<>();
        List<ServerSelectionList.Entry> others = new ArrayList<>();
        for (ServerSelectionList.Entry entry : vanilla) {
            if (entry instanceof ServerSelectionList.OnlineServerEntry server) {
                servers.add(server);
            } else {
                others.add(entry);
            }
        }
        store.prune(servers.stream().map(server -> server.getServerData().ip).toList());
        if (!store.hasFolders()) {
            return new ArrayList<>(vanilla);
        }

        Map<ServerFolder, List<ServerSelectionList.OnlineServerEntry>> inFolder = new LinkedHashMap<>();
        for (ServerFolder folder : store.folders()) {
            inFolder.put(folder, new ArrayList<>());
        }
        List<ServerSelectionList.OnlineServerEntry> loose = new ArrayList<>();
        for (ServerSelectionList.OnlineServerEntry server : servers) {
            ServerFolder folder = store.folderOf(server.getServerData().ip);
            (folder == null ? loose : inFolder.get(folder)).add(server);
        }

        List<ServerSelectionList.Entry> arranged = new ArrayList<>();
        inFolder.forEach((folder, members) -> {
            boolean collapsed = folder.collapsed() || DragController.isFolderDragActive();
            arranged.add(new FolderEntry(screen, list, folder, members.size(), collapsed));
            if (!collapsed) {
                arranged.addAll(members);
            }
        });
        arranged.addAll(loose);
        arranged.addAll(others);
        return arranged;
    }

    /** Folder rows are lower than server rows. */
    public static void applyHeights(ServerSelectionList list) {
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof FolderEntry) {
                entry.setHeight(FolderEntry.HEIGHT);
            }
        }
        list.setScrollAmount(list.scrollAmount());
    }

    /** Vanilla only restores the selection of server rows, so keep a selected folder row selected as well. */
    public static void reselectFolder(ServerSelectionList list, ServerSelectionList.@Nullable Entry previouslySelected) {
        if (!(previouslySelected instanceof FolderEntry previous)) {
            return;
        }
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof FolderEntry folderEntry && folderEntry.folder() == previous.folder()) {
                list.setSelected(entry);
                return;
            }
        }
    }

    public static boolean isInFolder(Object entry) {
        return entry instanceof ServerSelectionList.OnlineServerEntry server && FolderStore.get().folderOf(server.getServerData().ip) != null;
    }

    public static int indentOf(Object entry) {
        return isInFolder(entry) ? INDENT : 0;
    }

    /** All visible servers that share the folder (or the top level) with the given one, in display order. */
    public static List<ServerSelectionList.OnlineServerEntry> groupOf(ServerSelectionList list, ServerSelectionList.OnlineServerEntry entry) {
        String key = groupKey(entry);
        List<ServerSelectionList.OnlineServerEntry> group = new ArrayList<>();
        for (ServerSelectionList.Entry candidate : list.children()) {
            if (candidate instanceof ServerSelectionList.OnlineServerEntry server && Objects.equals(groupKey(server), key)) {
                group.add(server);
            }
        }
        return group;
    }

    private static @Nullable String groupKey(ServerSelectionList.OnlineServerEntry entry) {
        ServerFolder folder = FolderStore.get().folderOf(entry.getServerData().ip);
        return folder == null ? null : folder.id();
    }
}
