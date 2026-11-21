package org.octlot.showserverlist.folder;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/**
 * A folder header row in the multiplayer list. Clicking it expands or collapses the folder, dragging it re-orders
 * the folders (see {@link DragController}).
 */
public final class FolderEntry extends ServerSelectionList.Entry {
    public static final int HEIGHT = 28;

    private static final Identifier CHEVRON_RIGHT = sprite("chevron_right");
    private static final Identifier CHEVRON_DOWN = sprite("chevron_down");
    private static final Identifier FOLDER_CLOSED = sprite("folder_closed");
    private static final Identifier FOLDER_OPEN = sprite("folder_open");
    private static final Identifier PENCIL = sprite("pencil");
    private static final Identifier PENCIL_HIGHLIGHTED = sprite("pencil_highlighted");
    private static final Identifier TRASH = sprite("trash");
    private static final Identifier TRASH_HIGHLIGHTED = sprite("trash_highlighted");

    private static final int COLOR_BAND = 0x25FFFFFF;
    private static final int COLOR_BAND_HOVERED = 0x40FFFFFF;
    private static final int COLOR_COUNT = 0xFF808080;
    private static final int ACTION_ICON_SIZE = 10;
    private static final int ACTION_HIT_SIZE = 14;

    private final JoinMultiplayerScreen screen;
    private final ServerSelectionList list;
    private final ServerFolder folder;
    private final int serverCount;
    /** What is shown; differs from the stored state while a folder is being dragged. */
    private final boolean collapsed;
    private final Minecraft minecraft = Minecraft.getInstance();

    public FolderEntry(JoinMultiplayerScreen screen, ServerSelectionList list, ServerFolder folder, int serverCount, boolean collapsed) {
        this.screen = screen;
        this.list = list;
        this.folder = folder;
        this.serverCount = serverCount;
        this.collapsed = collapsed;
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath("showserverlist", name);
    }

    public ServerFolder folder() {
        return folder;
    }

    @Override
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float a) {
        Font font = minecraft.font;
        int x = getContentX();
        int y = getContentY();
        int mid = getContentYMiddle();

        graphics.fill(x, y, x + getContentWidth(), y + getContentHeight(), hovered ? COLOR_BAND_HOVERED : COLOR_BAND);

        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, collapsed ? CHEVRON_RIGHT : CHEVRON_DOWN, x + 4, mid - 4, 8, 8);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, collapsed ? FOLDER_CLOSED : FOLDER_OPEN, x + 16, mid - 8, 16, 16);

        int nameX = x + 38;
        graphics.text(font, folder.name(), nameX, mid - 4, -1);
        graphics.text(font, "(" + serverCount + ")", nameX + font.width(folder.name()) + 6, mid - 4, COLOR_COUNT);

        if (hovered || list.getSelected() == this) {
            extractAction(graphics, mouseX, mouseY, renameX(), PENCIL, PENCIL_HIGHLIGHTED, "showserverlist.folder.rename");
            extractAction(graphics, mouseX, mouseY, deleteX(), TRASH, TRASH_HIGHLIGHTED, "showserverlist.folder.delete");
        }
    }

    private void extractAction(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, Identifier sprite, Identifier highlighted, String tooltipKey) {
        boolean over = isOverAction(mouseX, mouseY, x);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, over ? highlighted : sprite, x, getContentYMiddle() - ACTION_ICON_SIZE / 2, ACTION_ICON_SIZE, ACTION_ICON_SIZE);
        if (over) {
            graphics.setTooltipForNextFrame(Component.translatable(tooltipKey), mouseX, mouseY);
        }
    }

    private int deleteX() {
        return getContentRight() - ACTION_ICON_SIZE - 6;
    }

    private int renameX() {
        return deleteX() - ACTION_ICON_SIZE - 8;
    }

    private boolean isOverAction(int mouseX, int mouseY, int iconX) {
        int pad = (ACTION_HIT_SIZE - ACTION_ICON_SIZE) / 2;
        int top = getContentYMiddle() - ACTION_HIT_SIZE / 2;
        return mouseX >= iconX - pad && mouseX < iconX - pad + ACTION_HIT_SIZE && mouseY >= top && mouseY < top + ACTION_HIT_SIZE;
    }

    /**
     * The rename / delete icons act right away. Anywhere else the press only arms a click or a drag; the folder
     * is toggled when the button is released without the mouse having been dragged.
     */
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseClicked(event, doubleClick);
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        if (isOverAction(mouseX, mouseY, renameX())) {
            FolderActions.renameFolder(screen, folder);
        } else if (isOverAction(mouseX, mouseY, deleteX())) {
            FolderActions.deleteFolder(screen, folder);
        } else {
            DragController.pressFolder(screen, this, event, doubleClick);
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isSelection()) {
            toggle();
            return true;
        }
        if (event.hasShiftDown() && (event.isUp() || event.isDown())) {
            if (FolderStore.get().move(folder, event.isDown() ? 1 : -1)) {
                refreshAndReselect();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    public void toggle() {
        FolderStore.get().toggleCollapsed(folder);
        refreshAndReselect();
    }

    private void refreshAndReselect() {
        list.refreshEntries();
        FolderLayout.reselectFolder(list, this);
    }

    @Override
    protected boolean matches(ServerSelectionList.Entry other) {
        return other instanceof FolderEntry entry && entry.folder == folder;
    }

    @Override
    public void join() {
        toggle();
    }

    @Override
    public Component getNarration() {
        return Component.translatable(
                "showserverlist.folder.narration",
                folder.name(),
                serverCount,
                Component.translatable(collapsed ? "showserverlist.folder.state.collapsed" : "showserverlist.folder.state.expanded")
        );
    }
}
