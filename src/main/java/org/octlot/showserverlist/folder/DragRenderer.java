package org.octlot.showserverlist.folder;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Everything that is drawn on top of the list while a row is dragged or has just been dropped. */
final class DragRenderer {
    static final int ACCENT = 0xFFFFC850;

    private static final int ACCENT_RGB = 0xFFC850;
    private static final int SOURCE_DIM = 0xA0000000;
    private static final int BOX_FILL = 0x30FFC850;
    private static final int GHOST_BACKGROUND = 0xE8181818;
    private static final int GHOST_SHADOW = 0x60000000;
    private static final int GHOST_HEIGHT = 20;
    private static final int GHOST_MAX_TEXT_WIDTH = 150;
    private static final int MAX_FLASH_ALPHA = 0x70;
    private static final Identifier FOLDER_ICON = Identifier.fromNamespaceAndPath("showserverlist", "folder_closed");

    private DragRenderer() {
    }

    /** Darkens the row the dragged item came from, so it reads as "picked up". */
    static void dimSource(GuiGraphicsExtractor graphics, ServerSelectionList.Entry row) {
        graphics.fill(row.getContentX(), row.getContentY(), row.getContentRight(), row.getContentBottom(), SOURCE_DIM);
    }

    static void dropIndicator(GuiGraphicsExtractor graphics, DropTarget target) {
        int x = target.x();
        int y = target.y();
        int width = target.width();
        if (target.isBox()) {
            graphics.fill(x, y, x + width, y + target.height(), BOX_FILL);
            graphics.outline(x, y, width, target.height(), ACCENT);
            return;
        }
        graphics.fill(x, y - 1, x + width, y + 1, ACCENT);
        graphics.fill(x - 1, y - 2, x + 2, y + 2, ACCENT);
        graphics.fill(x + width - 2, y - 2, x + width + 1, y + 2, ACCENT);
    }

    /** A small label that follows the mouse. */
    static void ghost(GuiGraphicsExtractor graphics, Font font, String label, boolean folder, int mouseX, int mouseY) {
        String text = font.plainSubstrByWidth(label, GHOST_MAX_TEXT_WIDTH);
        int iconSpace = folder ? 20 : 0;
        int width = font.width(text) + 16 + iconSpace;
        int x = Math.max(2, Math.min(mouseX + 12, graphics.guiWidth() - width - 2));
        int y = Math.max(2, Math.min(mouseY - GHOST_HEIGHT / 2, graphics.guiHeight() - GHOST_HEIGHT - 2));
        graphics.fill(x + 2, y + 2, x + width + 2, y + GHOST_HEIGHT + 2, GHOST_SHADOW);
        graphics.fill(x, y, x + width, y + GHOST_HEIGHT, GHOST_BACKGROUND);
        graphics.outline(x, y, width, GHOST_HEIGHT, ACCENT);
        if (folder) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, FOLDER_ICON, x + 5, y + 2, 16, 16);
        }
        graphics.text(font, text, x + 8 + iconSpace, y + 6, -1);
    }

    /** Briefly lights up the row that was just dropped. {@code strength} fades from 1 to 0. */
    static void dropFlash(GuiGraphicsExtractor graphics, ServerSelectionList.Entry row, float strength) {
        int alpha = (int) (Math.max(0f, Math.min(1f, strength)) * MAX_FLASH_ALPHA);
        graphics.fill(row.getContentX(), row.getContentY(), row.getContentRight(), row.getContentBottom(), (alpha << 24) | ACCENT_RGB);
    }
}
