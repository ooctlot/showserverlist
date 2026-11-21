package org.octlot.showserverlist.folder;

import net.minecraft.client.multiplayer.ServerData;
import org.jspecify.annotations.Nullable;

/**
 * Where a dragged row would land, together with the shape that shows it: a line between two rows, or a box
 * around the row of the folder that would receive the server.
 */
record DropTarget(Kind kind, @Nullable ServerFolder folder, @Nullable ServerData anchor, boolean after,
                  int x, int y, int width, int height) {
    enum Kind {
        /** Server goes to the end of a folder. */
        INTO_FOLDER,
        /** Server goes next to another server and joins that server's folder (or none). */
        BESIDE_SERVER,
        /** Server leaves its folder and goes to the end of the loose servers. */
        LOOSE_END,
        /** Folder goes in front of {@link #folder()}, or to the end when that is null. */
        FOLDER_SLOT
    }

    static DropTarget intoFolder(ServerFolder folder, int x, int y, int width, int height) {
        return new DropTarget(Kind.INTO_FOLDER, folder, null, true, x, y, width, height);
    }

    static DropTarget besideServer(ServerData anchor, boolean after, int x, int lineY, int width) {
        return new DropTarget(Kind.BESIDE_SERVER, null, anchor, after, x, lineY, width, 0);
    }

    static DropTarget looseEnd(int x, int lineY, int width) {
        return new DropTarget(Kind.LOOSE_END, null, null, true, x, lineY, width, 0);
    }

    static DropTarget folderSlot(@Nullable ServerFolder before, int x, int lineY, int width) {
        return new DropTarget(Kind.FOLDER_SLOT, before, null, false, x, lineY, width, 0);
    }

    boolean isBox() {
        return kind == Kind.INTO_FOLDER;
    }
}
