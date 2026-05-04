package draylar.tiered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.CraftingMenu;

// TODO(Ravel): can not resolve target class CraftingScreenHandler
@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {


    // TODO(Ravel): no target class
    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    public void quickMoveMixin(Player player, int slot, CallbackInfoReturnable<ItemStack> info) {


        if (slot == 0 && player.getInventory().getFreeSlot() == -1) {
            info.setReturnValue(ItemStack.EMPTY);
        }
    }
}