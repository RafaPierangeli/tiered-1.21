package draylar.tiered.mixin.client;

import draylar.tiered.client.TooltipContextHolder;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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

    @Shadow @Nullable protected Slot hoveredSlot;

// 🌟 PASSO 1: Limpa o porta-malas no início de cada frame (Garante que não vaze memória)
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void clearOnRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        TooltipContextHolder.currentStack = ItemStack.EMPTY;
    }

    // TODO(Ravel): no target class
// 🌟 PASSO 2: Guarda o item EXATO que o Minecraft focou (Sem matemática manual!)
    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void captureTooltipItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            TooltipContextHolder.currentStack = this.hoveredSlot.getItem();
        }
    }
}