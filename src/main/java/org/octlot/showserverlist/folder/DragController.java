package org.octlot.showserverlist.folder;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Drag and drop for the multiplayer list: servers can be moved into / out of folders and re-ordered, folders can
 * be re-ordered.
 * <p>
 * A row only <em>starts</em> a drag - the row reports its press via {@link #pressServer} / {@link #pressFolder}.
 * Everything after that (moving, dropping, cancelling, drawing) is driven by screen events, so a plain click on a row
 * keeps behaving as before. While a folder is dragged all folders are shown collapsed to make them easy to
 * re-order; their stored state is not touched.
 */
public final class DragController {
    private static final int START_DISTANCE = 4;
    private static final int SCROLL_ZONE = 28;
    private static final double MAX_SCROLL_PER_TICK = 10;
    private static final long FLASH_MILLIS = 700;

    private static @Nullable DragController active;

    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;

    private boolean pressed;
    private int pressButton;
    private double pressX;
    private double pressY;
    private boolean pressWasDoubleClick;
    /** The row that was pressed: exactly one of {@code server} / {@code folder} is set while {@code pressed}. */
    private @Nullable ServerData server;
    private @Nullable ServerFolder folder;
    private @Nullable FolderEntry folderRow;

    private boolean dragging;
    private double mouseX;
    private double mouseY;
    private @Nullable DropTarget target;

    private @Nullable ServerData flashServer;
    private @Nullable ServerFolder flashFolder;
    private long flashStart;

    private DragController(JoinMultiplayerScreen screen) {
        this.screen = screen;
        this.list = screen.serverSelectionList;
    }

    /** Called whenever the multiplayer screen is (re-)initialised. */
    public static void attach(JoinMultiplayerScreen screen) {
        DragController controller = new DragController(screen);
        active = controller;
        ScreenMouseEvents.allowMouseDrag(screen).register((s, event, dx, dy) -> !controller.onDrag(event));
        ScreenMouseEvents.allowMouseRelease(screen).register((s, event) -> !controller.onRelease(event));
        ScreenKeyboardEvents.allowKeyPress(screen).register((s, event) -> !controller.onKey(event));
        ScreenEvents.afterTick(screen).register(s -> controller.tick());
        ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, delta) -> controller.extract(graphics, mouseX, mouseY));
        ScreenEvents.remove(screen).register(s -> controller.abort());
    }

    /** While a folder is dragged every folder is laid out collapsed. */
    public static boolean isFolderDragActive() {
        return active != null && active.dragging && active.folder != null;
    }

    public static void pressServer(JoinMultiplayerScreen screen, ServerData server, MouseButtonEvent event) {
        if (active != null && active.screen == screen) {
            active.beginPress(event, false, server, null, null);
        }
    }

    public static void pressFolder(JoinMultiplayerScreen screen, FolderEntry row, MouseButtonEvent event, boolean doubleClick) {
        if (active != null && active.screen == screen) {
            active.beginPress(event, doubleClick, null, row.folder(), row);
        }
    }

    private void beginPress(MouseButtonEvent event, boolean doubleClick, @Nullable ServerData server, @Nullable ServerFolder folder, @Nullable FolderEntry row) {
        this.pressed = true;
        this.pressButton = event.button();
        this.pressX = event.x();
        this.pressY = event.y();
        this.pressWasDoubleClick = doubleClick;
        this.server = server;
        this.folder = folder;
        this.folderRow = row;
        this.dragging = false;
        this.target = null;
    }

    // ---------------------------------------------------------------- input

    /** @return whether the event was consumed */
    private boolean onDrag(MouseButtonEvent event) {
        if (!pressed || event.button() != pressButton) {
            return false;
        }
        mouseX = event.x();
        mouseY = event.y();
        if (!dragging) {
            if (Math.hypot(mouseX - pressX, mouseY - pressY) < START_DISTANCE) {
                return false;
            }
            dragging = true;
            if (folder != null) {
                refreshKeepingRowInPlace(folder);
            }
        }
        return true;
    }

    /** @return whether the event was consumed */
    private boolean onRelease(MouseButtonEvent event) {
        if (!pressed || event.button() != pressButton) {
            return false;
        }
        boolean wasDragging = dragging;
        DropTarget dropTarget = wasDragging ? resolve(event.x(), event.y()) : null;
        FolderEntry clickedFolder = !wasDragging && !pressWasDoubleClick ? folderRow : null;
        if (wasDragging) {
            drop(dropTarget);
        }
        clearPress();
        if (clickedFolder != null) {
            clickedFolder.toggle();
        }
        return wasDragging;
    }

    private boolean onKey(KeyEvent event) {
        if (dragging && event.isEscape()) {
            cancel();
            return true;
        }
        return false;
    }

    private void tick() {
        if (!dragging) {
            return;
        }
        double top = list.getY();
        double bottom = list.getBottom();
        double speed = 0;
        if (mouseY < top + SCROLL_ZONE) {
            speed = -scrollSpeed(top + SCROLL_ZONE - mouseY);
        } else if (mouseY > bottom - SCROLL_ZONE) {
            speed = scrollSpeed(mouseY - (bottom - SCROLL_ZONE));
        }
        if (speed != 0) {
            list.setScrollAmount(list.scrollAmount() + speed);
        }
    }

    private static double scrollSpeed(double depthIntoZone) {
        return 1 + Math.min(1, depthIntoZone / SCROLL_ZONE) * (MAX_SCROLL_PER_TICK - 1);
    }

    private void cancel() {
        ServerFolder draggedFolder = folder;
        dragging = false;
        clearPress();
        if (draggedFolder != null) {
            refreshKeepingRowInPlace(draggedFolder);
        }
    }

    /** The screen goes away (or is rebuilt) while something is held. */
    private void abort() {
        if (dragging && folder != null) {
            list.refreshEntries();
        }
        dragging = false;
        clearPress();
    }

    private void clearPress() {
        pressed = false;
        server = null;
        folder = null;
        folderRow = null;
        target = null;
    }

    // ---------------------------------------------------------------- drop

    private void drop(@Nullable DropTarget dropTarget) {
        ServerData draggedServer = server;
        ServerFolder draggedFolder = folder;
        dragging = false;
        if (draggedFolder != null) {
            if (dropTarget != null) {
                FolderStore.get().moveBefore(draggedFolder, dropTarget.folder());
                markDropped(null, draggedFolder);
            }
            refreshKeepingRowInPlace(draggedFolder);
        } else if (draggedServer != null && dropTarget != null) {
            dropServer(draggedServer, dropTarget);
            markDropped(draggedServer, null);
        }
    }

    private void dropServer(ServerData moved, DropTarget dropTarget) {
        FolderStore store = FolderStore.get();
        ServerList servers = screen.getServers();
        ServerData anchor;
        boolean after = true;
        switch (dropTarget.kind()) {
            case INTO_FOLDER -> {
                store.assign(moved.ip, dropTarget.folder());
                anchor = lastServerOf(servers, dropTarget.folder(), moved);
            }
            case BESIDE_SERVER -> {
                anchor = dropTarget.anchor();
                after = dropTarget.after();
                store.assign(moved.ip, store.folderOf(anchor.ip));
            }
            default -> {
                store.assign(moved.ip, null);
                anchor = lastServerOf(servers, null, moved);
            }
        }
        moveNextTo(servers, moved, anchor, after);
        list.updateOnlineServers(servers);
    }

    /** The last server of a folder (or of the loose servers when {@code folder} is null) in list order. */
    private static @Nullable ServerData lastServerOf(ServerList servers, @Nullable ServerFolder folder, ServerData exclude) {
        FolderStore store = FolderStore.get();
        for (int i = servers.size() - 1; i >= 0; i--) {
            ServerData candidate = servers.get(i);
            if (candidate != exclude && store.folderOf(candidate.ip) == folder) {
                return candidate;
            }
        }
        return null;
    }

    /** Re-inserts {@code moved} right before / after {@code anchor}, or at the very end without an anchor. */
    private static void moveNextTo(ServerList servers, ServerData moved, @Nullable ServerData anchor, boolean after) {
        List<ServerData> all = servers.serverList;
        int from = indexOf(all, moved);
        if (from < 0) {
            return;
        }
        all.remove(from);
        int anchorIndex = anchor == null ? -1 : indexOf(all, anchor);
        all.add(anchorIndex < 0 ? all.size() : anchorIndex + (after ? 1 : 0), moved);
        servers.save();
    }

    private static int indexOf(List<ServerData> servers, ServerData wanted) {
        for (int i = 0; i < servers.size(); i++) {
            if (servers.get(i) == wanted) {
                return i;
            }
        }
        return -1;
    }

    private void markDropped(@Nullable ServerData droppedServer, @Nullable ServerFolder droppedFolder) {
        flashServer = droppedServer;
        flashFolder = droppedFolder;
        flashStart = Util.getMillis();
    }

    /** Rebuilds the list and scrolls so that the row of {@code keep} stays where it was on screen. */
    private void refreshKeepingRowInPlace(ServerFolder keep) {
        int before = rowTop(keep);
        list.refreshEntries();
        int after = rowTop(keep);
        if (before >= 0 && after >= 0) {
            list.setScrollAmount(list.scrollAmount() + after - before);
        }
    }

    private int rowTop(ServerFolder wanted) {
        for (ServerSelectionList.Entry entry : list.children()) {
            if (entry instanceof FolderEntry row && row.folder() == wanted) {
                return row.getY();
            }
        }
        return -1;
    }

    // ---------------------------------------------------------------- where would it land

    private @Nullable DropTarget resolve(double x, double y) {
        if (!list.isMouseOver(x, y)) {
            return null;
        }
        return folder != null ? resolveFolderSlot(y) : resolveServerTarget(y);
    }

    private @Nullable DropTarget resolveServerTarget(double y) {
        List<ServerSelectionList.Entry> rows = list.children();
        if (rows.isEmpty() || y < rows.get(0).getY()) {
            return null;
        }
        for (ServerSelectionList.Entry row : rows) {
            if (y < row.getY() || y >= row.getY() + row.getHeight()) {
                continue;
            }
            if (row instanceof FolderEntry folderRow) {
                return DropTarget.intoFolder(folderRow.folder(), folderRow.getContentX(), folderRow.getContentY(), folderRow.getContentWidth(), folderRow.getContentHeight());
            }
            if (row instanceof ServerSelectionList.OnlineServerEntry serverRow) {
                if (serverRow.getServerData() == server) {
                    return null;
                }
                boolean after = y >= row.getY() + row.getHeight() / 2.0;
                return DropTarget.besideServer(serverRow.getServerData(), after, row.getContentX(), after ? row.getY() + row.getHeight() : row.getY(), row.getContentWidth());
            }
            break;
        }
        return looseEnd(rows);
    }

    /** Anything below the servers (the LAN section, empty space) means "no folder, at the end". */
    private static DropTarget looseEnd(List<ServerSelectionList.Entry> rows) {
        for (ServerSelectionList.Entry row : rows) {
            if (!(row instanceof FolderEntry) && !(row instanceof ServerSelectionList.OnlineServerEntry)) {
                return DropTarget.looseEnd(row.getContentX(), row.getY(), row.getContentWidth());
            }
        }
        ServerSelectionList.Entry last = rows.get(rows.size() - 1);
        return DropTarget.looseEnd(last.getContentX(), last.getY() + last.getHeight(), last.getContentWidth());
    }

    private @Nullable DropTarget resolveFolderSlot(double y) {
        FolderEntry last = null;
        for (ServerSelectionList.Entry row : list.children()) {
            if (!(row instanceof FolderEntry folderRow)) {
                continue;
            }
            last = folderRow;
            if (y < folderRow.getY() + folderRow.getHeight() / 2.0) {
                return folderSlot(folderRow.folder(), folderRow, folderRow.getY());
            }
        }
        return last == null ? null : folderSlot(null, last, last.getY() + last.getHeight());
    }

    /** A folder slot, unless dropping there would not change anything. */
    private @Nullable DropTarget folderSlot(@Nullable ServerFolder before, FolderEntry row, int lineY) {
        List<ServerFolder> order = FolderStore.get().folders();
        int from = order.indexOf(folder);
        int to = before == null ? order.size() : order.indexOf(before);
        if (to == from || to == from + 1) {
            return null;
        }
        return DropTarget.folderSlot(before, row.getContentX(), lineY, row.getContentWidth());
    }

    // ---------------------------------------------------------------- drawing

    private void extract(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (dragging) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            target = resolve(mouseX, mouseY);
        }
        graphics.enableScissor(list.getX(), list.getY(), list.getRight(), list.getBottom());
        if (dragging) {
            ServerSelectionList.Entry source = rowOf(server, folder);
            if (source != null) {
                DragRenderer.dimSource(graphics, source);
            }
            if (target != null) {
                DragRenderer.dropIndicator(graphics, target);
            }
        } else if (flashStart != 0) {
            long elapsed = Util.getMillis() - flashStart;
            ServerSelectionList.Entry row = elapsed < FLASH_MILLIS ? rowOf(flashServer, flashFolder) : null;
            if (row != null) {
                DragRenderer.dropFlash(graphics, row, 1f - (float) elapsed / FLASH_MILLIS);
            } else {
                flashStart = 0;
            }
        }
        graphics.disableScissor();
        if (dragging) {
            graphics.nextStratum();
            DragRenderer.ghost(graphics, Minecraft.getInstance().font, labelOfDragged(), folder != null, mouseX, mouseY);
            graphics.requestCursor(target == null && !list.isMouseOver(mouseX, mouseY) ? CursorTypes.NOT_ALLOWED : CursorTypes.RESIZE_ALL);
        }
    }

    private String labelOfDragged() {
        if (folder != null) {
            return folder.name();
        }
        if (server == null) {
            return "";
        }
        return server.name == null || server.name.isBlank() ? server.ip : server.name;
    }

    private ServerSelectionList.@Nullable Entry rowOf(@Nullable ServerData wantedServer, @Nullable ServerFolder wantedFolder) {
        for (ServerSelectionList.Entry entry : list.children()) {
            if (wantedServer != null && entry instanceof ServerSelectionList.OnlineServerEntry row && row.getServerData() == wantedServer) {
                return entry;
            }
            if (wantedFolder != null && entry instanceof FolderEntry row && row.folder() == wantedFolder) {
                return entry;
            }
        }
        return null;
    }
}
