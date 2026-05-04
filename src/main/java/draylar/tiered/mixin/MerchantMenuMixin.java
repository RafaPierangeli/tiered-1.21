package draylar.tiered.mixin;

import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.MerchantMenu;

@Mixin(MerchantMenu.class)
public abstract class MerchantMenuMixin  {


    @Shadow @Final private Merchant trader;

    // 🌟 UNIQUE: Variável temporária para guardar quem é o jogador fazendo a troca
    @Unique
    private Player tiered$currentPlayer;

    public MerchantMenuMixin(int i, Inventory inventory, Merchant merchant) {
        super();
    }

//    public MerchantMenuMixin(MenuType<?> type, int syncId) {
//        super(type, syncId);
//    }


// 🌟 INJECT: Captura o jogador no exato momento em que ele aperta Shift-Click
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void capturePlayerMixin(Player player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        this.tiered$currentPlayer = player;
    }


    @ModifyVariable(method = "quickMoveStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/MerchantMenu;moveItemStackTo(Lnet/minecraft/world/item/ItemStack;IIZ)Z", ordinal = 0), name = "stack")
    private ItemStack quickMoveMixin(ItemStack stack) {

        if (this.tiered$currentPlayer instanceof ServerPlayer serverPlayer) {

            // 🌟 PASSO 1: O LIMPA-TRILHOS (Sempre executa)
            ModifierUtils.removeItemStackAttribute(stack);

            // 🌟 PASSO 2: APLICAÇÃO (Só executa se a config principal estiver ligada)
            if (ConfigInit.CONFIG.merchantModifier) {
                int merchantLevel = 0;

                if (ConfigInit.CONFIG.merchantLevelScaling && this.trader instanceof Villager villager) {
                    merchantLevel = villager.getVillagerData().level();
                }

                ModifierUtils.setItemStackAttribute(serverPlayer, stack, false, merchantLevel);
            }
        }
        return stack;
    }
}