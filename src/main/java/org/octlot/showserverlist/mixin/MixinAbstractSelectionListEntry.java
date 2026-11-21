package org.octlot.showserverlist.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.octlot.showserverlist.folder.FolderLayout;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Indents the content of servers that live inside a folder. Everything a row draws or hit-tests is based on
 * {@code getContentX()} / {@code getContentWidth()}, so rendering and clicks stay consistent.
 */
@Mixin(targets = "net.minecraft.client.gui.components.AbstractSelectionList$Entry")
public abstract class MixinAbstractSelectionListEntry {
    @ModifyReturnValue(method = "getContentX", at = @At("RETURN"))
    private int showserverlist$indentContentX(int original) {
        return original + FolderLayout.indentOf(this);
    }

    @ModifyReturnValue(method = "getContentWidth", at = @At("RETURN"))
    private int showserverlist$indentContentWidth(int original) {
        return original - FolderLayout.indentOf(this);
    }
}
