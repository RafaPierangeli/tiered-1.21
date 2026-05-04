package draylar.tiered.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.Tiered;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;


@Mixin(Item.class)
public class ItemMixin {


// 🌟 CORREÇÃO: Removemos o 'World world' dos parâmetros, pois a Mojang tirou ele na 1.21.11
    @Inject(method = "onCraftedBy", at = @At("TAIL"))
    private void onCraftByPlayerMixin(ItemStack stack, Player player, CallbackInfo info) {
        // 🌟 CORREÇÃO: Pegamos o mundo diretamente do jogador para manter a sua lógica original funcionando!
        Level world = player.level();

        if (!world.isClientSide() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(player, stack, false);
        }
    }


// Os outros métodos continuam intactos, pois não sofreram alterações na 1.21.11
    @Inject(method = "onCraftedPostProcess", at = @At("TAIL"))
    private void onCraftMixin(ItemStack stack, Level world, CallbackInfo info) {
        if (!world.isClientSide() && !stack.isEmpty() && ConfigInit.CONFIG.craftingModifier) {
            ModifierUtils.setItemStackAttribute(null, stack, false);
        }
    }


    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void getItemBarStepMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            info.setReturnValue(Math.round(13.0f - (float) stack.getDamageValue() * 13.0f / (float) stack.getMaxDamage()));
        }
    }


    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void getItemBarColorMixin(ItemStack stack, CallbackInfoReturnable<Integer> info) {
        if (stack.get(Tiered.TIER) != null && stack.get(Tiered.TIER).durable() > 0f) {
            float f = Math.max(0.0f, ((float) stack.getMaxDamage() - (float) stack.getDamageValue()) / (float) stack.getMaxDamage());
            info.setReturnValue(Mth.hsvToRgb(f / 3.0f, 1.0f, 1.0f));
        }
    }
}