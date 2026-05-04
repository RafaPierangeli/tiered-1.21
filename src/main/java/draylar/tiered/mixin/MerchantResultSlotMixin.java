package draylar.tiered.mixin;

import draylar.tiered.api.ModifierUtils;
import draylar.tiered.config.ConfigInit;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.Merchant;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// TODO(Ravel): can not resolve target class TradeOutputSlot
@Mixin(MerchantResultSlot.class)
public abstract class MerchantResultSlotMixin {

    // TODO(Ravel): Could not determine a single target
    @Shadow @Final private Merchant merchant;

    // TODO(Ravel): no target class
// 🌟 MUDANÇA CRÍTICA: Mudamos de "HEAD" para "RETURN".
    // Agora nós esperamos o Minecraft e o Mod original fazerem toda a bagunça deles.
    // Quando eles terminarem, nós entramos e damos a palavra final!
    @Inject(method = "onTake", at = @At("RETURN"))
    private void onTakeItemMixin(Player player, ItemStack stack, CallbackInfo ci) {

        if (player instanceof ServerPlayer serverPlayer) {

            // 🌟 PASSO 1: O LIMPA-TRILHOS (Sempre executa)
            // Arrancamos qualquer Tier genérico que o evento de Crafting tenha colocado milissegundos atrás.
            ModifierUtils.removeItemStackAttribute(stack);

            // 🌟 PASSO 2: APLICAÇÃO (Só executa se a config principal estiver ligada)
            if (ConfigInit.CONFIG.merchantModifier) {
                int merchantLevel = 0;

                // Verifica se o escalonamento por nível está ligado
                if (ConfigInit.CONFIG.merchantLevelScaling && this.merchant instanceof Villager villager) {
                    merchantLevel = villager.getVillagerData().level();
                }

                ModifierUtils.setItemStackAttribute(serverPlayer, stack, false, merchantLevel);
            }

            serverPlayer.connection.send(new ClientboundContainerSetSlotPacket(
                    -1,                                   // containerId
                    serverPlayer.containerMenu.incrementStateId(), // stateId (equivalente ao nextRevision)
                    -1,                                   // slot
                    stack                                 // item
            ));
        }
    }
}