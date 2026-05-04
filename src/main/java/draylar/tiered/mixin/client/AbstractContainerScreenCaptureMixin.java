package draylar.tiered.mixin.client;

import draylar.tiered.client.TooltipContextHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO(Ravel): can not resolve target class HandledScreen
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenCaptureMixin {

    // TODO(Ravel): Could not determine a single target
    @Shadow @Nullable protected Slot hoveredSlot;

    // TODO(Ravel): no target class
// 🌟 PASSO 1: Limpa o porta-malas no início de cada frame (Garante que não vaze memória)
    @Inject(method = "render", at = @At("HEAD"))
    private void clearOnRender(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TooltipContextHolder.currentStack = ItemStack.EMPTY;
    }

    // TODO(Ravel): no target class
// 🌟 PASSO 2: Guarda o item EXATO que o Minecraft focou (Sem matemática manual!)
    @Inject(method = "renderTooltip", at = @At("HEAD"))
    private void captureTooltipItem(GuiGraphics context, int x, int y, CallbackInfo ci) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            TooltipContextHolder.currentStack = this.hoveredSlot.getItem();
        }
    }
}