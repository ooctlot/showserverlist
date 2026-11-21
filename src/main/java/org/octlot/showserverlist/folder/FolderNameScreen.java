package org.octlot.showserverlist.folder;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Small dialog to enter the name of a new folder or to rename one. The caller decides which screen follows.
 */
public final class FolderNameScreen extends Screen {
    private static final Component LABEL = Component.translatable("showserverlist.folder.name.label");
    private static final Component HINT = Component.translatable("showserverlist.folder.name.hint");
    private static final Component TAKEN = Component.translatable("showserverlist.folder.name.taken");

    private final String initialName;
    private final Predicate<String> nameAvailable;
    private final Consumer<String> onDone;
    private final Runnable onCancel;
    private EditBox nameEdit;
    private Button doneButton;

    public FolderNameScreen(Component title, String initialName, Predicate<String> nameAvailable, Consumer<String> onDone, Runnable onCancel) {
        super(title);
        this.initialName = initialName;
        this.nameAvailable = nameAvailable;
        this.onDone = onDone;
        this.onCancel = onCancel;
    }

    @Override
    protected void init() {
        nameEdit = new EditBox(font, width / 2 - 100, 66, 200, 20, LABEL);
        nameEdit.setMaxLength(FolderStore.MAX_NAME_LENGTH);
        nameEdit.setHint(HINT);
        nameEdit.setValue(initialName);
        nameEdit.setResponder(value -> updateDoneButton());
        addRenderableWidget(nameEdit);
        doneButton = addRenderableWidget(
                Button.builder(CommonComponents.GUI_DONE, button -> submit()).bounds(width / 2 - 100, height / 4 + 96 + 18, 200, 20).build()
        );
        addRenderableWidget(
                Button.builder(CommonComponents.GUI_CANCEL, button -> onClose()).bounds(width / 2 - 100, height / 4 + 120 + 18, 200, 20).build()
        );
        updateDoneButton();
    }

    @Override
    protected void setInitialFocus() {
        setInitialFocus(nameEdit);
    }

    @Override
    public void resize(int width, int height) {
        String value = nameEdit.getValue();
        init(width, height);
        nameEdit.setValue(value);
    }

    private void updateDoneButton() {
        doneButton.active = nameAvailable.test(nameEdit.getValue());
    }

    private void submit() {
        if (doneButton.active) {
            onDone.accept(nameEdit.getValue().trim());
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isConfirmation() && doneButton.active) {
            submit();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        onCancel.run();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(font, title, width / 2, 17, -1);
        graphics.text(font, LABEL, width / 2 - 100 + 1, 53, -6250336);
        String value = nameEdit.getValue();
        if (!value.isBlank() && !nameAvailable.test(value)) {
            graphics.text(font, TAKEN, width / 2 - 100 + 1, 90, 0xFFFF5555);
        }
    }
}
