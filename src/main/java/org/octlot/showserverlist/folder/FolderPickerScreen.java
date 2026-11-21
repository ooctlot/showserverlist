package org.octlot.showserverlist.folder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ScrollableLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Lets the player choose the folder a server is moved to. The caller decides which screen follows.
 */
public final class FolderPickerScreen extends Screen {
    private static final int BUTTON_WIDTH = 200;

    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private final String serverName;
    private final @Nullable ServerFolder currentFolder;
    private final Consumer<@Nullable ServerFolder> onPick;
    private final Runnable onNewFolder;
    private final Runnable onCancel;
    private ScrollableLayout scrollArea;

    public FolderPickerScreen(String serverName, @Nullable ServerFolder currentFolder, Consumer<@Nullable ServerFolder> onPick, Runnable onNewFolder, Runnable onCancel) {
        super(Component.translatable("showserverlist.folder.picker.title"));
        this.serverName = serverName;
        this.currentFolder = currentFolder;
        this.onPick = onPick;
        this.onNewFolder = onNewFolder;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        layout.setHeaderHeight(40);

        LinearLayout content = LinearLayout.vertical().spacing(4);
        content.addChild(option(Component.translatable("showserverlist.folder.picker.none"), null));
        for (ServerFolder folder : FolderStore.get().folders()) {
            content.addChild(option(Component.literal(folder.name()), folder));
        }
        content.addChild(Button.builder(Component.translatable("showserverlist.folder.picker.new"), button -> onNewFolder.run()).width(BUTTON_WIDTH).build());
        scrollArea = layout.addToContents(new ScrollableLayout(minecraft, content, layout.getContentHeight()));

        layout.addToFooter(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).width(BUTTON_WIDTH).build());
        layout.visitWidgets(this::addRenderableWidget);
        repositionElements();
    }

    private Button option(Component label, @Nullable ServerFolder target) {
        boolean current = target == currentFolder;
        Button button = Button.builder(current ? Component.literal("» ").append(label) : label, b -> onPick.accept(target)).width(BUTTON_WIDTH).build();
        button.active = !current;
        return button;
    }

    @Override
    protected void repositionElements() {
        scrollArea.arrangeElements();
        scrollArea.setMaxHeight(layout.getContentHeight());
        layout.arrangeElements();
        scrollArea.setPosition(width / 2 - scrollArea.getWidth() / 2, layout.getHeaderHeight());
    }

    @Override
    public void onClose() {
        onCancel.run();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 10, -1);
        graphics.centeredText(font, Component.literal(serverName), width / 2, 24, -6250336);
    }
}
